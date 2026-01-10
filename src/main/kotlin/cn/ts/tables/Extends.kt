package cn.ts.tables

import cn.ts.model.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 *
 * @author tomsean
 */

fun Table.identity(name: String): Column<Id> = registerColumn(name, VarCharColumnType(50, null))

/**
 * 通过ID查询
 */
fun SIdTable.findById(id: Id): ResultRow? {
    val table = this
    return selectAll().where { table.id.eq(id) }.firstOrNull()
}

/**
 * 判断是否存在
 */
fun SIdTable.exists(block: () -> Op<Boolean>): Boolean {
    return select(id).where(block).limit(1).firstOrNull() != null
}


/**
 * 分页查询
 * @author tomsean
 */
fun <T> Query.page(pager: Pager = Pager.DEFAULT, transform: (ResultRow) -> T): Page<T> {
    //查询总数量
    val total = this.copy().count()
    //查询结果集
    val list = this.limit(pager.size).offset(pager.offset).map { transform(it) }
    return Page(pager.page, pager.size, total, list)
}