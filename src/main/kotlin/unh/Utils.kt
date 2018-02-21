package unh

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking

fun <A, B>List<A>.pmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async(CommonPool) { f(it) } }.map { it.await() }
}

fun <A>Collection<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
    map { async(CommonPool) { f(it) } }.forEach { it.await() }
}

//fun <T, K, R> Grouping<T, K>.pfold(
