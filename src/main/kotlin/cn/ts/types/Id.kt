package cn.ts.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer

/**
 * Id类型
 * @author tomsean
 */
typealias Id = @Serializable(with = LongAsStringSerializer::class) Long
