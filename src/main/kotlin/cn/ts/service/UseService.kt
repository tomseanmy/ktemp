package cn.ts.service

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 使用注入
 * @author tomsean
 */
class UseService : KoinComponent {
    val greeting: Greeting by inject()
}