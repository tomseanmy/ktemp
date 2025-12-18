package cn.ts.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * 深度拷贝，得到一个新的对象，并支持属性的修改
 * ```example
 *     val newUser = User(123L, "张三", "12345678901").copy {
 *         User::name set "李四"
 *         User::phone set "12345678901"
 *     }
 * ```
 * @author tomsean
 */
inline fun <reified T> T.copy(block: DeepCopyContext<T>.() -> Unit): T {
    val strategy = json.serializersModule.serializer<T>()
    val originalJson = json.encodeToJsonElement(strategy, this).jsonObject

    // B. 执行用户的配置 Block，收集变更
    val context = DeepCopyContext<T>()
    context.block()
    // C. 合并变更 (Patch)
    // 将原有的 JSON 转为可变 Map，并用收集到的新 JSON 覆盖
    val mergedMap = originalJson.toMutableMap()
    context.edits.forEach { (key, value) ->
        mergedMap[key] = value
    }
    // D. 反序列化回对象
    return json.decodeFromJsonElement(strategy, JsonObject(mergedMap))
}

/**
 * 这个类负责收集用户的修改，并自动处理序列化
 */
class DeepCopyContext<T>() {
    // 存储修改：Key 是属性名，Value 是已经序列化好的 JsonElement
    val edits = mutableMapOf<String, JsonElement>()

    /**
     * 语法支持：User::name set "新值"
     * @param V 属性的类型
     * @param value 用户传入的新值 (纯 Kotlin 对象)
     */
    inline infix fun <reified V> KProperty1<T, V>.set(value: V) {
        // 在这里，自动将用户传入的纯对象 value 转换为 JsonElement
        // 这一步对用户是完全透明的
        val encodedValue = json.encodeToJsonElement(value)
        edits[this.name] = encodedValue
    }
}
// 预定义的 Kotlin 基础类型类引用
val basicTypes = setOf(
    Int::class, Long::class, Short::class, Byte::class,
    Float::class, Double::class,
    Boolean::class,
    Char::class,
    String::class,
)

/**
 * 检查传入的 kClass 是否是 Kotlin 的基础类型
 */
fun KClass<*>.isPrimitive(): Boolean {
    // 检查传入的 kClass 是否在基础类型集合中
    return this in basicTypes
}

inline fun <reified T> T.toJsonString(): String {
    if (T::class.isPrimitive()) throw IllegalArgumentException("${T::class.simpleName} is not a primitive type")
    return json.encodeToString(this)
}