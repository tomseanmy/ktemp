package cn.ts.utils

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
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
inline fun <reified T : Any> T.copy(block: DeepCopyContext<T>.() -> Unit): T {
    val mapper = Json.objectMapper
    // A. 把原对象转成可变的 ObjectNode (Jackson 的 JsonObject 等价物)
    val mergedNode: ObjectNode = mapper.valueToTree<JsonNode>(this) as ObjectNode

    // B. 执行用户的配置 Block,收集变更
    val context = DeepCopyContext<T>()
    context.block()

    // C. 应用 patch
    context.edits.forEach { (key, value) ->
        mergedNode.set(key, value)
    }

    // D. 反序列化回对象
    return mapper.treeToValue(mergedNode, T::class.java)
}

/**
 * 这个类负责收集用户的修改,并自动处理序列化
 */
class DeepCopyContext<T : Any> {
    // 存储修改:Key 是属性名,Value 是已经序列化好的 JsonNode
    val edits = mutableMapOf<String, JsonNode>()

    /**
     * 语法支持:User::name set "新值"
     * @param V 属性的类型
     * @param value 用户传入的新值 (纯 Kotlin 对象)
     */
    infix fun <V> KProperty1<T, V>.set(value: V) {
        // 在这里,自动将用户传入的纯对象 value 转换为 JsonNode
        // 这一步对用户是完全透明的
        edits[this.name] = Json.objectMapper.valueToTree<JsonNode>(value)
    }
}
