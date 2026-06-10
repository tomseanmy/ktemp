package cn.ts.tables

import cn.ts.model.Id
import cn.ts.model.Page
import cn.ts.model.Pager
import cn.ts.utils.Uuid
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

/**
 * ID列
 * @author tomsean
 */
fun Table.identity(name: String): Column<Id> = registerColumn(name, VarCharColumnType(50, null))

/**
 * 表主键为uuid
 * @author tomsean
 */
abstract class Bases(name: String = "", columnName: String = "id") : IdTable<String>(name)    {
    final override val id = varchar(columnName, 32).clientDefault { Uuid.randomUUID() }.entityId()
    final override val primaryKey = PrimaryKey(id)
}


/**
 * 数据表
 * @author tomsean
 */
abstract class Datas(name: String = ""): Bases(name) {
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val createdBy = identity("created_by").clientDefault { "SYSTEM" }
    val deleted = bool("deleted").default(false)
}


/**
 * 树结构
 * @author tomsean
 */
abstract class Trees(tableName: String = ""): Datas(tableName) {
    val parentId = identity("parent_id").nullable()
    val left = long("left").default(0)
    val right = long("right").default(0)
}


/**
 * 通过ID查询
 */
fun Bases.find(id: Id): ResultRow? {
    val table = this
    return selectAll().where { table.id.eq(id) }.firstOrNull()
}

/**
 * 判断ID是否存在
 */
fun Bases.exists(id: Id): Boolean {
    val table = this
    return table.select(table.id).where { table.id.eq(id) }.limit(1).firstOrNull() != null
}

fun Bases.existsWhere(block: () -> Op<Boolean>): Boolean {
    val table = this
    return table.selectAll().where(block).limit(1).firstOrNull() != null
}

/**
 * 通过ID查询
 */
fun Datas.find(id: Id, deleted: Boolean = false): ResultRow? {
    val table = this
    return selectAll().where { table.id.eq(id) and table.deleted.eq(deleted) }.firstOrNull()
}

/**
 * 判断ID是否存在
 */
fun Datas.exists(id: Id, deleted: Boolean = false): Boolean {
    val table = this
    return table.select(table.id).where { table.id.eq(id) and table.deleted.eq(deleted) }.limit(1).firstOrNull() != null
}


/**
 * 删除
 * @param ids 节点ID列表
 */
fun Datas.delete(ids: List<Id>): Int {
    val table = this
    return table.update(where = { table.id inList ids }) {
        it[table.deleted] = true
    }
}


/**
 * 查询所有子代（一级）
 * @param id 节点ID
 * @return 子代节点列表
 * @author tomsean
 */
fun Trees.children(id: Id): List<ResultRow> {
    val table = this
    val node = find(id) ?: return emptyList()
    val nodeLeft = node[table.left]
    val nodeRight = node[table.right]
    return selectAll().where {
        (table.left greater nodeLeft) and
                (table.right less nodeRight) and
                (table.parentId eq id) and
                (table.deleted eq false)
    }.toList()
}

/**
 * 查询所有后代
 * @param id 节点ID
 * @return 后代节点列表
 * @author tomsean
 */
fun Trees.descendants(id: Id): List<ResultRow> {
    val table = this
    val node = find(id) ?: return emptyList()
    val nodeLeft = node[table.left]
    val nodeRight = node[table.right]
    return selectAll().where {
        (table.left greater nodeLeft) and
                (table.right less nodeRight) and
                (table.deleted eq false)
    }.orderBy(table.left).toList()
}

/**
 * 查询所有祖先
 * @param id 节点ID
 * @return 祖先节点列表
 * @author tomsean
 */
fun Trees.ancestors(id: Id): List<ResultRow> {
    val table = this
    val node = find(id) ?: return emptyList()
    val nodeLeft = node[table.left]
    val nodeRight = node[table.right]
    return selectAll().where {
        (table.left less nodeLeft) and
                (table.right greater nodeRight) and
                (table.deleted eq false)
    }.orderBy(table.left).toList()
}

/**
 * 保存
 * @param parentId 父节点ID
 * @param body 节点数据
 * @return 节点ID
 * @author tomsean
 */
fun Trees.save(parentId: Id? = null, body: Trees.(InsertStatement<EntityID<Id>>) -> Unit = {}): Id {
    val table = this
    val (newLeft, newRight) = if (parentId != null) {
        val parent = find(parentId) ?: throw IllegalArgumentException("父节点不存在: $parentId")
        val pr = parent[table.right]
        table.update(where = { table.right greaterEq pr }) {
            it.update(table.right, table.right + 2L)
        }
        table.update(where = { table.left greaterEq pr }) {
            it.update(table.left, table.left + 2L)
        }
        pr to pr + 1L
    } else {
        val maxRight = selectAll().orderBy(table.right, SortOrder.DESC).limit(1)
            .firstOrNull()?.get(table.right) ?: 0L
        (maxRight + 1L) to (maxRight + 2L)
    }
    val id = table.insertAndGetId {
        if (parentId != null) it[table.parentId] = parentId
        it[table.left] = newLeft
        it[table.right] = newRight
        body(it)
    }
    return id.value
}

/**
 * 重建树结构
 * @author tomsean
 */
fun Trees.rebuild() {
    val table = this
    val nodes = selectAll().where { table.deleted eq false }
        .orderBy(table.left to SortOrder.ASC, table.createdAt to SortOrder.ASC)
        .toList()
    val children = mutableMapOf<Id?, MutableList<Id>>()
    for (node in nodes) {
        val pid: Id? = node[table.parentId]
        children.getOrPut(pid) { mutableListOf() }.add(node[table.id].value)
    }
    var counter = 0L
    fun traverse(parentId: Id?) {
        for (childId in children[parentId] ?: return) {
            counter++
            val leftVal = counter
            traverse(childId)
            counter++
            table.update(where = { table.id eq childId }) {
                it[table.left] = leftVal
                it[table.right] = counter
            }
        }
    }
    traverse(null)
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