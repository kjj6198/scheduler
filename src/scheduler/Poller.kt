package kalan.todo.scheduler

import kalan.todo.utils.generateSafeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.security.SecureRandom
import java.util.*
import kotlin.concurrent.thread

class Poller {
  val serializer = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  var done = false
  val logger = LoggerFactory.getLogger("poller")
  var pollerThread: Thread? = null
  val queue = listOf("schedule", "cron_schedule")

  fun start() {
    pollerThread = thread(start = true, name = "Poller") {
      try {
        while(!done) {
          enqueueJobs()
          Thread.sleep(1000)
        }
      } catch(e: InterruptedException) {
        done = true
        logger.warn("Poller Thread has been interrupted.")
      }
    }
  }

  fun stop() {
    done = true
    pollerThread?.interrupt()
  }

  fun enqueueJobs() {
    queue.forEach { queueName ->
      try {
        val now = Date(System.currentTimeMillis()).time.toDouble()
        RedisClient.withRedisPool { client ->
          val jobs = client.zrangeByScore(queueName, 0.0, now, 0, 100)
          jobs.forEach {
            if (client.zrem(queueName, it) == 1L) {
              val item = serializer.decodeFromString<SchedulerItem>(it)
              item.enqueueAt = System.currentTimeMillis().toDouble()
              item.at = null
              val tx = client.multi()
              tx.lpush("queue#todo", serializer.encodeToString(item))
              tx.exec()
              logger.info("enqueue $it into queue:todo")
            }
          }
        }
      } catch (e: Exception) {
        logger.error(e.message)
      }
    }
  }
}