package cn.ts.exception

/**
 * 业务异常
 * @author tomsean
 */
open class BizException(msg: String? = "panic error") : RuntimeException(msg)