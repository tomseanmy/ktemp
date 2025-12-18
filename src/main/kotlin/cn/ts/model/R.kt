package cn.ts.model

import kotlinx.serialization.*

/**
 * 结果集
 * @author tomsean
 */
@Serializable
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