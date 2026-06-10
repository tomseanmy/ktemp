package cn.ts.utils

import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.future.asDeferred
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Redis 工具 (基于 Jackson 序列化,真非阻塞协程)
 *
 * 走 Redisson 的 `*Async` API (内部是 Netty 事件循环 + Lua 脚本 + Pub/Sub),
 * 拿到 `RFuture` 后用 `kotlinx-coroutines-jdk8` 的 `CompletionStage.await()`
 * 挂起协程等回调 —— 整个等待期间不占任何调度器线程。
 *
 * @author tomsean
 */
object Redis {

    @PublishedApi
    internal val log = LoggerFactory.getLogger(Redis::class.java)

    lateinit var client: RedissonClient

    /**
     * 读取
     */
    suspend fun <T> get(key: String, codec: JacksonCodec<T>): T? = try {
        client.getBucket<T>(key, codec).getAsync().asDeferred().await()
    } catch (e: Exception) {
        log.error("Redis get failed: key=$key", e)
        null
    }

    /**
     * 写入 (无过期时间)
     */
    suspend fun <T> set(key: String, value: T, codec: JacksonCodec<T>) {
        set(key, value, null, codec)
    }

    /**
     * 写入 (可指定过期时间)
     */
    suspend fun <T> set(key: String, value: T, duration: Duration?, codec: JacksonCodec<T>) {
        try {
            val bucket = client.getBucket<T>(key, codec)
            val future = if (duration != null) bucket.setAsync(value, duration) else bucket.setAsync(value)
            future.asDeferred().await()
        } catch (e: Exception) {
            log.error("Redis set failed: key=$key", e)
        }
    }

    /**
     * 删除一个或多个 key,返回实际删除的条数
     */
    suspend fun delete(vararg key: String): Long = try {
        client.keys.deleteAsync(*key).asDeferred().await()
    } catch (e: Exception) {
        log.error("Redis delete failed: keys=${key.toList()}", e)
        0L
    }

    /**
     * 协程安全的分布式锁 —— `RLockAsync.lockAsync()` 走 Netty 事件循环,
     * 等待期间不占 Kotlin 调度器线程
     */
    suspend fun lock(key: String, block: suspend () -> Unit) = lock(listOf(key), block)

    suspend fun lock(vararg key: String, block: suspend () -> Unit) = lock(key.toList(), block)

    suspend fun lock(key: List<String>, block: suspend () -> Unit) {
        val lo = client.getLock(key.joinToString(":"))
        try {
            lo.lockAsync().asDeferred().await()
            block()
        } finally {
            runCatching { lo.unlockAsync().asDeferred().await() }
                .onFailure { log.error("Redis unlock failed: key=${key.joinToString(":")}", it) }
        }
    }
}

/**
 * Jackson 实现的 Redisson Codec
 *
 * 相比原 KotlinxCodec 的改进:
 * 1. 缓存 `ObjectWriter` / `ObjectReader` 而不是每次重新构造 (性能)
 * 2. `TypeReference` 精确保留泛型信息 (支持 `List<User>` 这种嵌套泛型)
 * 3. 资源管理更稳健 (ByteBuf 异常时也会 release)
 */
class JacksonCodec<T>(typeRef: TypeReference<T>) : BaseCodec() {

    private val mapper = Json.objectMapper
    private val resolvedType = mapper.typeFactory.constructType(typeRef)
    private val writer = mapper.writerFor(resolvedType)
    private val reader = mapper.readerFor(resolvedType)

    private val encoder = Encoder { `in` ->
        val buf = ByteBufAllocator.DEFAULT.buffer()
        try {
            val str = writer.writeValueAsString(`in`)
            buf.writeCharSequence(str, StandardCharsets.UTF_8)
            buf
        } catch (e: Exception) {
            buf.release()
            throw e
        }
    }

    private val decoder = Decoder<Any> { buf, _ ->
        val str = buf.toString(StandardCharsets.UTF_8)
        reader.readValue(str)
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}

/**
 * 构造一个 [JacksonCodec]
 *
 * 利用 inline reified 捕获泛型 [T] 的真实类型(包括嵌套泛型),传给 Jackson 的 [TypeReference]
 */
inline fun <reified T> jacksonCodec(): JacksonCodec<T> = JacksonCodec(object : TypeReference<T>() {})

/**
 * reified 便捷入口 —— 跟 `get(key, jacksonCodec<T>())` 等价
 */
suspend inline fun <reified T> Redis.get(key: String): T? = Redis.get(key, jacksonCodec<T>())

/**
 * reified 便捷入口 —— 跟 `set(key, value, null, jacksonCodec<T>())` 等价
 */
suspend inline fun <reified T> Redis.set(key: String, value: T) =
    Redis.set(key, value, null, jacksonCodec<T>())

/**
 * reified 便捷入口 —— 跟 `set(key, value, duration, jacksonCodec<T>())` 等价
 */
suspend inline fun <reified T> Redis.set(key: String, value: T, duration: Duration) =
    Redis.set(key, value, duration, jacksonCodec<T>())
