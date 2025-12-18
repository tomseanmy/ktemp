package cn.ts.exception

/**
 * 认证异常
 * @author tomsean
 */
class AuthenticationException (msg: String = "err_auth_failed") : BizException(msg)