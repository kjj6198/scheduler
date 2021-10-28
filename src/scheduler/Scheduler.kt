package kalan.todo.scheduler

import kalan.todo.utils.generateSafeToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import kotlin.reflect.full.createInstance

@Serializable
data class SchedulerItem(
  val klass: String,
  val args: String,
  var cronExpression: String? = null,
  var jid: String? = null,
  var routine: Boolean? = null,
  var enqueueAt: Double? = null,
  var at: Double? = null
)

open class Scheduler {
  private var score: Double? = null
  private var jid: String? = null

  companion object {
    private val serializer = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }
    private val logger = LoggerFactory.getLogger("Scheduler")
    fun remove(jid: String, score: Double) {
      RedisClient.withRedisPool { client ->
        val messages = client.zrangeByScore("schedule", score, score, 0, 100)
        for (msg in messages) {
          val item = serializer.decodeFromString<SchedulerItem>(msg)
          if (item.jid == jid) {
            client.zrem("schedule", msg)
          }
        }
      }
    }

    fun remove(jid: String, score: DateTime) {
      remove(jid, score.millis.toDouble())
    }
  }

  fun makeRoutineJob(cronExpression: String, message: SchedulerItem, fromProgram: Boolean): String {
    if (fromProgram) {
      RedisClient.withRedisPool { client ->
        val cronItems = client
          .zrangeByScore(
            "cron_schedule",
            "0",
            "inf",
            0,
            100
          )

        cronItems.forEach {
          val item = serializer.decodeFromString<SchedulerItem>(it)
          if (item.klass == message.klass) {
            client.zrem("cron_schedule", it)
          }
        }
      }
    }

    return makeRoutineJob(cronExpression, message)
  }

  fun makeRoutineJob(cronExpression: String, message: SchedulerItem): String {
    try {
      val nextExecutionTime = CronScheduler.getNextExecutionTs(cronExpression)
      RedisClient.withRedisPool { client ->
        message.routine = true
        message.cronExpression = cronExpression
        message.at = nextExecutionTime
        message.jid = message.jid ?: generateSafeToken()
        score = message.at
        jid = message.jid

        val str = serializer.encodeToString(message)

        client.zadd("cron_schedule", mutableMapOf(str to nextExecutionTime))
        logger.info("Put $message into cron_schedule")
      }
      return message.jid!!
    } catch(e: Exception) {
      logger.error("Can not parse cron expression: $cronExpression, treat it as normal schedule job.")
      return schedule(System.currentTimeMillis().plus(1000).toDouble(), message)
    }
  }

  fun schedule(time: Date, message: SchedulerItem, scheduleName: String = "schedule"): String {
    RedisClient.withRedisPool { client ->
      val ts = time.time.toDouble()

      message.at = ts
      message.jid = generateSafeToken()

      score = message.at
      jid = message.jid

      val str = serializer.encodeToString(message)
      // TODO: move this operation into other class
      client.zadd(scheduleName, mutableMapOf(str to ts))
      logger.info("Put $message into schedule")
    }

    return message.jid ?: ""
  }

  fun schedule(ts: Double, message: SchedulerItem): String {
    val date = Date(ts.toLong())
    return schedule(date, message)
  }

  fun schedule(ts: Double, kclass: String, args: String): String {
    return schedule(ts, SchedulerItem(kclass, args))
  }

  fun performCron(cronExpression: String, args: String): String {
    val worker = this::class.createInstance()
    return worker.makeRoutineJob(cronExpression, SchedulerItem(this::class.qualifiedName!!, args))
  }

  fun performCron(cronExpression: String, kclass: String, args: String): String {
    val worker = Class.forName(kclass).kotlin.createInstance() as Scheduler
    return worker.makeRoutineJob(cronExpression, SchedulerItem(kclass, args))
  }

  fun performCron(cronExpression: String, kclass: String, args: String, fromProgram: Boolean): String {
    val worker = Class.forName(kclass).kotlin.createInstance() as Scheduler
    return worker.makeRoutineJob(cronExpression, SchedulerItem(kclass, args), fromProgram)
  }

  fun performAt(at: Double, args: String): String {
    val worker = this::class.createInstance()
    return worker.schedule(at, this::class.qualifiedName!!, args)
  }

  fun performAt(at: DateTime, args: String): String {
    val worker = this::class.createInstance()
    return worker.schedule(at.millis.toDouble(), this::class.qualifiedName!!, args)
  }

  // maybe move to other place?
  fun delete() {
    RedisClient.withRedisPool { client ->
      if (jid != null && score != null) {
        val messages = client.zrangeByScore("schedule", score!!, score!!, 0, 1)
        for (msg in messages) {
          val item = serializer.decodeFromString<SchedulerItem>(msg)
          if (item.jid === jid) {
            client.zrem("schedule", msg)
          }
        }
      }
    }
  }

  fun modify(scheduleAt: Date, message: SchedulerItem): String {
    delete()
    return schedule(scheduleAt, message)
  }
}