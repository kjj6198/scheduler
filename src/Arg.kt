package kalan.todo.scheduler

import java.lang.ClassCastException

data class Arg(val value: Any) {
  fun getString(): String? {
    return try {
      value as String
    } catch(e: TypeCastException) {
      null
    } catch(e: ClassCastException) {
      null
    }
  }

  fun getString(default: String): String {
    return getString() ?: default
  }

  fun getInt(): Int? {
    return try {
      value as Int
    } catch(e: TypeCastException) {
      null
    } catch(e: ClassCastException) {
      null
    }
  }

  fun getInt(default: Int): Int {
    return getInt() ?: default
  }

  fun <K, V>getMap(): Map<K, V>? {
    return try {
      value as Map<K, V>
    } catch(e: TypeCastException) {
      null
    } catch(e: ClassCastException) {
      null
    }
  }
}

