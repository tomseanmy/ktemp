package cn.ts.utils

import java.time.*
import cn.ts.utils.DateUtil.DEFAULT_ZONE

/**
 *
 * @author tomsean
 */
object DateUtil {
    val DEFAULT_ZONE: ZoneOffset = ZoneOffset.of("+8")
}

fun LocalDateTime.toInstant(): Instant = this.toInstant(DEFAULT_ZONE)

fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, DEFAULT_ZONE)

/**
 * 计算两个日期相差天数
 * 23.5 小时 = 0 day
 * @param other
 * @return 相差天数
 */
infix fun LocalDateTime.diffDay(other: LocalDateTime): Long {
    val duration: Duration = Duration.between(this, other)
    return duration.toDays()
}

/**
 * 计算差值
 */
infix fun LocalDateTime.subtract(other: LocalDateTime): Duration {
    return Duration.between(this, other)
}