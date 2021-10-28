## Simple kotlin scheduler

**THIS IS ONLY FOR EXPERIMENT**

A simple kotlin scheduler using redis.

## Features

1. A kotiln class defined cronjob entry (static, run when application start)

```kotlin
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
```

2. application-time defined cronjob

```kotlin

import kalan.todo.scheduler.*

// defined your job in work function
class TestWork : Scheduler(), Workable {
  override fun work(arg: Arg) {
    println("Hello world")
  }
}

// will register a cronjob
fun main() {
	val worker = TestWork()
	worker.performCron("00 18 * * 1-5", "10")
}
```

3. a delay job

```kotlin

import kalan.todo.scheduler.*

// defined your job in work function
class TodoWork : Scheduler(), Workable {
  override fun work(arg: Arg) {
    println("Time to complete your todo!")
  }
}

// will register a cronjob
fun main() {
	val worker = TestWork()
	val ts = System.currentTimeMillis().plus(1000).toDouble()
	val item = SchedulerItem(TestWork::class.qualifiedName!!, "10")
	item.at = ts
	worker.performAt(ts, "10") // will execute 1 sec later
}
```

## TODOs

This todo may not be implemented anyway.

- [ ] Better API and better naming
- [ ] Re-organize the class structure
- [ ] Simplify the code implementation
- [ ] More robust and error-prune implementation