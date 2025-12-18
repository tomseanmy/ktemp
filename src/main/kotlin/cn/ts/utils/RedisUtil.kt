package cn.ts.utils

import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.*
import kotlinx.coroutines.async
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.redisson.api.RList
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.StringCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Redis工具
 * @author tomsean
 */
object Redis {

    private val log = LoggerFactory.getLogger(Redis::class.java)

    lateinit var client: RedissonClient

    /**
     *
     */
    inline operator fun <reified T> get(key: String): T? {
        try {
            val bucket = client.getBucket<String>(key, kotlinxCodec<T>())
            if (!bucket.isExists) return null
            val valueStr = bucket.get()
            if (T::class.isPrimitive()) {
                return valueStr as T
            }
            return json.decodeFromString(valueStr)
        } catch (e: Exception) {
            throw e
        }
    }

    inline operator fun <reified T> set(key: String, value: T) {
        val valueStr = if (value::class.isPrimitive()) (value).toString() else json.encodeToString(value)
        try {
            client.getBucket<String>(key, StringCodec()).set(valueStr)
        } catch (e: Exception) {
            throw e
        }
    }

    inline fun <reified T> set(key: String, value: T, duration: Duration? = null) {

        try {
            val bucket = client.getBucket<String>(key, kotlinxCodec<T>())
            val valueStr = if (T::class.isPrimitive()) (value).toString() else json.encodeToString(value)
            if (duration != null) {
                bucket.set(valueStr, duration)
                return
            }
            bucket.set(valueStr)
        } catch (e: Exception) {
            throw e
        }
    }

    inline fun <reified T : Any> hash(key: String): RMap<String, T> = client.getMap(key, kotlinxCodec<T>())

    inline fun <reified T : Any> list(key: String): RList<T> = client.getList(key, kotlinxCodec<T>())

    fun batchDelete(keys: List<String>) = runBlocking(Dispatchers.IO) {
        keys.map { key ->
            async {
                try {
                    client.keys.deleteAsync(key)
                    true
                } catch (e: Exception) {
                    // 可选：记录日志
                    log.error("Failed to delete key: $key", e)
                    false
                }
            }
        }
    }

    fun delete(vararg key: String): Long {
        try {
            return client.keys.delete(*key)
        } catch (e: Exception) {
            throw e
        }
    }

    fun deletePrefix(prefix: String): Long {
        try {
            return client.keys.deleteByPattern("$prefix*")
        } catch (e: Exception) {
            throw e
        }
    }

    fun lock(key: String, block: () -> Unit) {
        lock(listOf(key), block)
    }

    fun lock(vararg key: String, block: () -> Unit) {
        lock(key.toList(), block)
    }

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

class KotlinxCodec<T>(
    private val serializer: KSerializer<T>,
) : BaseCodec() {
    // 编码器：Object -> ByteBuf
    private val encoder: Encoder = Encoder { `in` ->
        val byteBuf = ByteBufAllocator.DEFAULT.buffer()
        try {
            // 强制转换并序列化
            @Suppress("UNCHECKED_CAST")
            val str = json.encodeToString(serializer, `in` as T)
            byteBuf.writeCharSequence(str, StandardCharsets.UTF_8)
            byteBuf
        } catch (e: Exception) {
            byteBuf.release()
            throw e
        }
    }

    // 解码器：ByteBuf -> Object
    private val decoder: Decoder<Any> = Decoder { buf, _ ->
        val str = buf.toString(StandardCharsets.UTF_8)
        json.decodeFromString(serializer, str)
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}

// 辅助函数：快速生成 Codec
inline fun <reified T> kotlinxCodec(): KotlinxCodec<T> {
    return KotlinxCodec(serializer<T>())
}