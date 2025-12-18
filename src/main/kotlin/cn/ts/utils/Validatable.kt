package cn.ts.utils

import io.ktor.server.plugins.requestvalidation.ValidationResult

/**
 * 验证实体
 * @author tomsean
 */
interface Validatable {
    fun validate(): ValidationResult
}