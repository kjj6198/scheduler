package kalan.todo.scheduler

import kalan.todo.works.CronWorkEntry
import org.slf4j.LoggerFactory
import sun.misc.Signal
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.system.exitProcess


class Manager() {
  private val logger = LoggerFactory.getLogger("Manager")
  private val count = 3
  private val workers = ArrayList<Processor>()
  private var poller: Poller? = null

  init {
    Signal.handle(Signal("INT")) {
      logger.warn("Receive SIGINT, gracefully shutdown starts...")
      workers.forEach { it.stop() }
      poller?.stop()
      exitProcess(0)
    }

    poller = Poller()
    poller?.start()
    repeat(count) {
      workers.add(Processor())
    }
  }

  fun start() {
    workers.forEach {
      it.start()
    }

    CronWorkEntry::class.nestedClasses.forEach {
      val annotation = it.findAnnotation<CronSchedule>()
      if (annotation != null) {
        val worker = it.createInstance() as Scheduler
        worker.performCron(annotation.cronExpression,"${CronWorkEntry::class.qualifiedName!!}$${it.simpleName!!}", "", true)
      }
    }
  }

  fun removeByJid(jid: String) {
    val client = RedisClient.getInstance()

  }
}
