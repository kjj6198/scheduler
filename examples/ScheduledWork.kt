package kalan.todo.works

import kalan.todo.scheduler.*

class TestWork : Scheduler(), Workable {
  override fun work(arg: Arg) {
		println("Hello world")
  }
}

fun main() {
	val worker = TestWork()
  val ts = System.currentTimeMillis().plus(1000).toDouble() // execute one second later
  val item = SchedulerItem(TestWork::class.qualifiedName!!, "10")
	item.at = ts
	worker.performAt(ts, "10")
}