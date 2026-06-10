package cn.ts.model

import cn.ts.utils.Json
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.core.SortOrder
import tools.jackson.databind.node.ObjectNode

/**
 * 分页结果封装
 */
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
    val sortsNode: ObjectNode? = this.request.queryParameters["sorts"]?.let { sortsStr ->
        runCatching { Json.parse(sortsStr) as? ObjectNode }.getOrNull()
    }
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
        sorts = sortsNode?.properties()?.asSequence()?.associate { (key, node) ->
            key to SortOrder.valueOf(node.asText())
        } ?: emptyMap()
    )
}
