package cn.ts.tables

import cn.ts.utils.Uuid
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

/**
 * 表主键为uuid
 * @author tomsean
 */
abstract class SIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name)    {
    final override val id = varchar(columnName, 32).clientDefault { Uuid.randomUUID() }.entityId()
    final override val primaryKey = PrimaryKey(id)
}


/**
 * 数据表
 * @author tomsean
 */
abstract class DataTable(name: String = ""): SIdTable(name) {
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val createdBy = identity("created_by").clientDefault { "SYSTEM" }
    val deleted = bool("deleted").default(false)
}
