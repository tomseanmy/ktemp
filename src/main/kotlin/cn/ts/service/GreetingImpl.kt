package cn.ts.service

/**
 *
 * @author tomsean
 */
class GreetingImpl : Greeting {
    override suspend fun say(msg: String) {
        println("say: $msg")
    }
}