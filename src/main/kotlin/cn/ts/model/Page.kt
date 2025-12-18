package cn.ts.model

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder

/**
 * 分页结果封装
 */
@Serializable
open class Page<T>(
    val page: Int,
    val size: Int,
    val total: Long,
    val list: List<T>
) {
    fun <K> map(transform: (T) -> K): Page<K> {
        return Page(page, size, total, list.map(transform))
    }
}

/**
 * 分页器
 * @author tomsean
 */
@Serializable
open class Pager(
    val page: Int = 1,
    val size: Int = 10,
    val sorts: Map<String, SortOrder> = emptyMap()
) {
    val offset: Long
        get() {
            return ((page - 1) * size).toLong()
        }

    companion object {
        val DEFAULT = Pager()
    }
}

/**
 * 获取分页器
 */
fun ApplicationCall.usePager(): Pager {
    val page = this.request.queryParameters["page"]?.toIntOrNull() ?: 1
    val size = this.request.queryParameters["size"]?.toIntOrNull() ?: 10
    val sorts: JsonObject? = this.request.queryParameters["sorts"]?.let { sortsStr -> Json.parseToJsonElement(sortsStr) as? JsonObject }
    return Pager(
        page = when {
            page < 1 -> 1
            page > 1000 -> 1000
            else -> page
        },
        size = when {
            size < 0 -> 10
            size > 10000 -> 10000
            else -> size
        },
        sorts = sorts?.map {
            it.key to SortOrder.valueOf(it.value.jsonPrimitive.content)
        }?.toMap() ?: emptyMap()
    )
}
