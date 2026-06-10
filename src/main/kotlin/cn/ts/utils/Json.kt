package cn.ts.utils

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

object Json {

    /**
     * 暴露给同模块内 inline 函数访问
     * (Kotlin 规则: public inline fun 只能引用 public/internal 成员,且在调用方模块需要可达)
     * 实际上是 public 的最小要求
     */
    val objectMapper = jacksonObjectMapper()

    fun toJson(obj: Any): String = objectMapper.writeValueAsString(obj)

    fun <T> fromJson(json: String, clazz: Class<T>): T =
        objectMapper.readValue(json, clazz)

    fun <T> fromJson(json: String, typeRef: TypeReference<T>): T =
        objectMapper.readValue(json, typeRef)

    fun <T> convert(obj: Any, clazz: Class<T>): T =
        objectMapper.convertValue(obj, clazz)

    fun <T> convert(obj: Any, typeRef: TypeReference<T>): T =
        objectMapper.convertValue(obj, typeRef)

    fun parse(json: String): JsonNode = objectMapper.readTree(json)

    fun <T> fromNode(node: JsonNode, clazz: Class<T>): T =
        objectMapper.treeToValue(node, clazz)

}

fun Any.toJson(): String = Json.toJson(this)

inline fun <reified T> String.toObject(): T = Json.fromJson(this, object : TypeReference<T>() {})

fun String.toJsonNode(): JsonNode = Json.parse(this)

inline fun <reified T> JsonNode.toObject(): T = Json.fromNode(this, T::class.java)

inline fun <reified T> Any.convertObject(): T =
    Json.convert(this, object : TypeReference<T>() {})
