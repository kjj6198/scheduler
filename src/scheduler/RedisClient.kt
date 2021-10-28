package kalan.todo.scheduler

import kalan.todo.config.Env
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Transaction
import redis.clients.jedis.exceptions.JedisConnectionException
import java.net.SocketException

class RedisClient {
  val logger = LoggerFactory.getLogger(RedisClient::class.java)
  var config: JedisPoolConfig = makeJedisPoolConfig()
  private val pool = makeJedisPool()

  companion object {
    private val redisClient = RedisClient()

    // prevent default thread access to the same pool
    // it'll cause Socket closed exception
    @Synchronized fun getInstance(): Jedis {
      return redisClient.pool.resource
    }

    fun withTx(block: (tx: Transaction) -> Unit) {
      val c = getInstance()
      val tx = c.multi()

      block(tx)
      tx.close()
      c.close()
    }

    fun withRedisPool(block: (client: Jedis) -> Unit) {
      try {
        val c = getInstance()
        block(c)
        c.close()
      } catch(e: JedisConnectionException) {
        redisClient.logger.error("Redis connection failed: ${e.message}")
      } catch(e: SocketException) {
        redisClient.logger.warn(
          "Socket closed unexpectedly, it always means you're performing operation after closing connection," + "\n" +
            "or you're accessing the same pool in different thread."
        )
      }
    }
  }

  constructor() {
    logger.info("Start Redis client")
  }

  fun makeJedisPoolConfig(): JedisPoolConfig {
    config = JedisPoolConfig()
    config.maxTotal = 10
    config.maxWaitMillis = 20000
    config.maxIdle = 5
    config.testWhileIdle = true
    config.minEvictableIdleTimeMillis = 60000
    config.timeBetweenEvictionRunsMillis = 60000
    config.numTestsPerEvictionRun = -1
    return config
  }

  fun makeJedisPool(): JedisPool {
    val env = Env.getInstance()
    return if (env.get("REDIS_PASSWORD") != "") {
      JedisPool(
        config,
        env.get("REDIS_HOST"),
        env.get("REDIS_PORT").toInt(10),
        2000,
        env.get("REDIS_PASSWORD")
      )
    } else {
      JedisPool(
        config,
        env.get("REDIS_HOST"),
        env.get("REDIS_PORT").toInt(10)
      )
    }

  }
}