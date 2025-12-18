package cn.ts.exception

/**
 *
 * @author tomsean
 */
class ForbiddenException (msg: String = "无权限") : BizException(msg)