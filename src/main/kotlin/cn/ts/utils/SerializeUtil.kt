package cn.ts.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    allowSpecialFloatingPointValues = true
}

/**
 *
 * @author tomsean
 */
object SerializeUtil

typealias Id = @Serializable(with = IdSerialize::class) Long

class IdSerialize: KSerializer<Id> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IdString", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Id) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Id {
        return decoder.decodeString().toLong()
    }
}


/**
 * 时间序列化
 */
class LocalDateSerialize: KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

/**
 * 时间序列化
 */
class LocalDateTimeSerialize: KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTimeString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

/**
 * BigDecimal序列化
 */
class BigDecimalSerialize: KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimalString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

fun String.isJson(): Boolean {
    return try {
        json.parseToJsonElement(this)
        true
    } catch (_: Exception) {
        false
    }
}