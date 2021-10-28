package kalan.todo.scheduler

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.ZoneId
import java.time.ZonedDateTime

object CronScheduler {
  private var cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))

  fun getNextExecutionTs(cronExpression: String): Double {
    val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
    val executionTime = ExecutionTime.forCron(cronParser.parse(cronExpression))
    val nextExecutionTime = executionTime.nextExecution(now).get()
    return nextExecutionTime.toEpochSecond().times(1000).toDouble()
  }
}