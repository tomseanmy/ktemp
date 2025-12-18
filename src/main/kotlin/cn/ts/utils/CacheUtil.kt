package cn.ts.utils

import cn.ts.utils.CacheUtil.CACHE_KEY
import cn.ts.utils.CacheUtil.LOCK_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object CacheUtil {
    val log: Logger = LoggerFactory.getLogger("CacheUtil.kt")
    val map = mutableMapOf<List<String>, suspend () -> Pair<*, Duration?>>()
    val CACHE_KEY = "cache"
    val LOCK_KEY = "_lock"
}
/**
 * 缓存工具，如果未命中缓存，则执行block
 * ```example
 *     cache("key") {
 *         "some str"
 *     }
 * ```
 * @author tomsean
 */
suspend inline fun <reified T> cache(
    vararg key: String,
    duration: Duration = 10.seconds.toJavaDuration(),
    noinline block: suspend () -> Pair<T, Duration?>
): T {
    CacheUtil.map[key.toList()] = block
    try {
        val value = block()
        Redis.lock(CACHE_KEY, LOCK_KEY, *key) {
            Redis.set<T>(listOf(CACHE_KEY, *key).joinToString(":"), value.first, value.second ?: duration)
        }
        return value.first
    } catch (e: Exception) {
        // 可选：记录日志
        CacheUtil.log.error("Failed to execute block", e)
        throw e
    }
}

/**
 * 刷新缓存
 */
suspend fun refreshCache(
    vararg key: String,
) {
    if (CacheUtil.map.containsKey(key.toList())) {
        val block = CacheUtil.map[key.toList()]!!
        val value = block()
        Redis.lock(CACHE_KEY, LOCK_KEY, *key) {
            Redis.set(listOf(CACHE_KEY, *key).joinToString(":"), value.first, value.second ?: 10.seconds.toJavaDuration())
        }
    } else {
        throw IllegalArgumentException("No cache block found for key: ${key.joinToString(":")}")
    }
}

/**
 * 删除缓存
 * @author tomsean
 */
fun invalidCache(vararg key: String) {
    Redis.lock(CACHE_KEY, LOCK_KEY, *key) {
        Redis.delete(listOf(CACHE_KEY, *key).joinToString(":"))
    }
}