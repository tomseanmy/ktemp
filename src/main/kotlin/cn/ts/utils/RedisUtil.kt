package cn.ts.utils

import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.*
import kotlinx.coroutines.async
import org.redisson.api.RList
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Redis 工具 (基于 Jackson 序列化)
 * @author tomsean
 */
object Redis {

    @PublishedApi
    internal val log = LoggerFactory.getLogger(Redis::class.java)

    lateinit var client: RedissonClient

    /**
     * 读取
     */
    inline operator fun <reified T> get(key: String): T? {
        return runCatching {
            client.getBucket<T>(key, jacksonCodec<T>()).get()
        }.onFailure { log.error("Redis get failed: key=$key", it) }.getOrNull()
    }

    /**
     * 写入 (无过期时间)
     */
    inline operator fun <reified T> set(key: String, value: T) {
        set(key, value, null)
    }

    /**
     * 写入 (可指定过期时间)
     */
    inline fun <reified T> set(key: String, value: T, duration: Duration?) {
        runCatching {
            val bucket = client.getBucket<T>(key, jacksonCodec<T>())
            if (duration != null) bucket.set(value, duration) else bucket.set(value)
        }.onFailure { log.error("Redis set failed: key=$key", it) }
    }

    /**
     * 批量删除
     */
    fun batchDelete(keys: List<String>): List<Boolean> = runBlocking(Dispatchers.IO) {
        keys.map { key ->
            async {
                runCatching {
                    client.keys.deleteAsync(key)
                    true
                }.onFailure { log.error("Failed to delete key: $key", it) }
                    .getOrDefault(false)
            }
        }.awaitAll()
    }

    /**
     * 删除
     */
    fun delete(vararg key: String): Long = client.keys.delete(*key)

    /**
     * 按前缀删除
     */
    fun deletePrefix(prefix: String): Long = client.keys.deleteByPattern("$prefix*")

    fun lock(key: String, block: () -> Unit) = lock(listOf(key), block)

    fun lock(vararg key: String, block: () -> Unit) = lock(key.toList(), block)

    fun lock(key: List<String>, block: () -> Unit) {
        val lo = client.getLock(key.joinToString(":"))
        try {
            lo.lock()
            block()
        } finally {
            lo.unlock()
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
