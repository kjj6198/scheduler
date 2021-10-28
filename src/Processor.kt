package kalan.todo.scheduler

import kalan.todo.config.Env
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.full.createInstance

class WorkFailureException(override val message: String?) : Exception()

class Processor {
  @Volatile
  private var done = false
  private val logger = LoggerFactory.getLogger("Processor")
  var processThread: Thread? = null

  fun stop() {
    done = true
    processThread?.interrupt()
  }

  fun processWork(routine: Boolean?, item: SchedulerItem) {
    try {
      val worker = Class.forName(item.klass).kotlin.createInstance() as Workable
      when (routine) {
        true -> {
          val nextExecutionTime = CronScheduler.getNextExecutionTs(item.cronExpression!!)
          item.at = nextExecutionTime
          Scheduler().schedule(Date(nextExecutionTime.toLong()), item, "cron_schedule")

          if (Env.getInstance().get("ENV") == "production") {
            try {
              worker.work(Arg(item.args))
            } catch (e: Exception) {
              logger.error("Unhandle exception in $worker: " + e.message + "\n" + e.stackTraceToString())
              throw WorkFailureException("worker: $worker failed to execute. Try to push into schedule queue again")
            }
          } else {
            logger.info("Cron Job: $item is executing in development mode.")
          }
        }
        null -> {
          if (Env.getInstance().get("ENV") == "production") {
            worker.work(Arg(item.args))
          } else {
            logger.info("Job: $item is executing in development mode.")
          }
        }
      }
    } catch(ex: Exception) {
      when (ex) {
        is ClassNotFoundException -> logger.error("Can not found class for ${item.klass}")
        is ClassCastException -> logger.error("Can not cast class for ${item.klass}. Please make sure to inherit Scheduler and Workable")
        is WorkFailureException -> logger.error(ex.message)
        else -> throw ex
      }
    }
  }


  fun start() {
    processThread = thread(start = true, name = "processor") {
      try {
        while(!done) {
          RedisClient.withRedisPool { client ->
            client.brpop("queue#todo", "2")?.forEach {
              if (it != "queue#todo") {
                val item = Json.decodeFromString<SchedulerItem>(it)
                processWork(item.routine, item)
              }
            }
          }
          Thread.sleep(1000)
        }
      } catch(e: InterruptedException) {
        logger.warn("Processor has been interrupted")
      }
    }

  }
}