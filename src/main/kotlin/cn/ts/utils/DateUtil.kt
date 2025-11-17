package cn.ts.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/**
 * 时间工具类
 * @author tomsean
 */

object DateUtil {
    val DEFAULT_ZONE: TimeZone by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        _defaultZeon ?: TimeZone.of("Asia/Shanghai")
    }

    private var _defaultZeon: TimeZone? = null

    /**
     * 设置默认时区
     */
    fun setDefaultZone(zone: TimeZone) {
        if (_defaultZeon != null) {
            throw IllegalStateException("Default zone has been set")
        }
        _defaultZeon = zone
    }

    val YYYYMMDD_HHMMSS = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }

    val YYYYMMDD_HHMM = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
    }

    val YYYYMMDD = LocalDate.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
    }
    val YYYYMM = LocalDate.Format {
        year()
        char('-')
        monthNumber()
    }
}

infix fun LocalDateTime.diff(other: LocalDateTime): Duration {
    return this.toInstant(DateUtil.DEFAULT_ZONE) - other.toInstant(DateUtil.DEFAULT_ZONE)
}

/**
 * 获取当前时间
 */
fun LocalDateTime.Companion.now(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(DateUtil.DEFAULT_ZONE)
}

/**
 * 时间加减
 */
fun LocalDateTime.plus(value: Int, unit: DateTimeUnit): LocalDateTime {
    val curInstant = this.toInstant(DateUtil.DEFAULT_ZONE)
    return curInstant.plus(value, unit, DateUtil.DEFAULT_ZONE).toLocalDateTime(DateUtil.DEFAULT_ZONE)
}

/**
 * 获取当前日期的开始时间
 */
fun LocalDate.atStartOfDay(): LocalDateTime {
    return this.atTime(0, 0, 0, 0)
}

/**
 * 获取当前日期的结束时间
 */
fun LocalDate.atEndOfDay(): LocalDateTime {
    return this.atTime(23, 59, 59, 999999999)
}

/**
 * 获取当前日期
 */
fun LocalDate.Companion.now(): LocalDate {
    return LocalDateTime.now().date
}

/**
 * 时间转时间戳
 */
fun LocalDateTime.toTimestamp(): Long {
    return this.toInstant(DateUtil.DEFAULT_ZONE).toEpochMilliseconds()
}

/**
 * 时间戳转时间
 */
fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(DateUtil.DEFAULT_ZONE)
}

/**
 * 时间字符串转时间
 */
fun String.parseLocalDateTime(format: DateTimeFormat<LocalDateTime> = DateUtil.YYYYMMDD_HHMMSS): LocalDateTime? {
    return format.parseOrNull(this)
}

/**
 * 时间字符串转时间
 */
fun String.parseLocalDate(format: DateTimeFormat<LocalDate> = DateUtil.YYYYMMDD): LocalDate? {
    return format.parseOrNull(this)
}

/**
 * 时间转字符串
 */
fun LocalDateTime.format(format: DateTimeFormat<LocalDateTime> = DateUtil.YYYYMMDD_HHMMSS): String {
    return format.format(this)
}

/**
 * 时间转字符串
 */
fun LocalDate.format(format: DateTimeFormat<LocalDate> = DateUtil.YYYYMMDD): String {
    return format.format(this)
}