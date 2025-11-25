package cn.ts.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

/**
 * Id类型
 * 在后端使用long类型作为ID的类型，但前端long类型会丢失精度，所以序列化时需要转为String类型
 * @author tomsean
 */
typealias Id = @Serializable(with = LongAsStringSerializer::class) Long

/**
 * LocalDateTime类型
 * 在后端使用LocalDateTime类型，但kotlinx serialization无法序列化LocalDateTime类型，所以需要自定义序列化器
 * @author tomsean
 */
typealias LDT = @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime

/**
 * LocalDateTime序列化器
 * @author tomsean
 */
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTimeString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }
}