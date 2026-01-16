package cn.ts.tables

import cn.ts.utils.Uuid
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * 表主键为uuid
 * @author tomsean
 */
abstract class SIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name)    {
    final override val id = varchar(columnName, 32).clientDefault { Uuid.randomUUID() }.entityId()
    final override val primaryKey = PrimaryKey(id)
}