package cn.ts.model

/**
 * 结果集
 * @author tomsean
 *
 * 注意: Jackson 默认序列化时会包含 null 字段 (例如 d=null 会输出 "d":null),
 * 而原 kotlinx 默认会省略 null 字段。如果前端依赖字段缺失来判定 null,
 * 在本类上加 `@JsonInclude(JsonInclude.Include.NON_NULL)` 即可恢复旧行为。
 */
open class R<T>(
    val c: Int = 0,
    val m: String? = null,
    val d: T? = null,
) {
    companion object {
        fun ok(): R<String> {
            return R(c = 0)
        }

        inline fun <reified T> ok(msg: String? = null, data: T? = null): R<T> {
            return R(c = 0, m = msg, d = data)
        }

        inline fun <reified T> err(code: Int = 1, msg: String? = null, data: T? = null): R<T> {
            return R(c = code, m = msg, d = data)
        }
    }
}