package org.jetbrains.kotlinconf

import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun launchAndCatch(
        context: CoroutineContext,
        onError: (Throwable) -> Unit,
        function: suspend () -> Unit
): Finallizable {
    val ret = Finallizable()
    launch(context) {
        try {
            function()
        } catch (e: Throwable) {
            onError(e)
        } finally {
            ret.onFinally?.invoke()
        }
    }
    return ret
}

class Finallizable {
    var onFinally: (() -> Unit)? = null

    infix fun finally(f: () -> Unit) {
        onFinally = f
    }
}