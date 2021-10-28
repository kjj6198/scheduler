package kalan.todo.works

/*
 * Static Cron job will register here, it will only register into redis queue
 * when start up, once the job is removed in redis, it won't execute again until
 * server restart.
 */
open class CronWorkEntry {

  @CronSchedule("00 18 * * 1-5")
  class NotifyMeCheck: Scheduler(), Workable {
    override fun work(args: Arg) {
			// will run at 18:00 in every monday-friday
    }
  }
}