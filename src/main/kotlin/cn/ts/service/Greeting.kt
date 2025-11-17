package cn.ts.service

/**
 * Greeting service
 * @author tomsean
 */
interface Greeting {
    /**
     * Greeting
     */
    suspend fun say(msg: String)
}