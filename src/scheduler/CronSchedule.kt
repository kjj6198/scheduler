package kalan.todo.scheduler


@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class CronSchedule(val cronExpression: String) {}
