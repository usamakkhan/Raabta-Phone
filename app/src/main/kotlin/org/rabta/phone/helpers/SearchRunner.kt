package org.rabta.phone.classic.helpers

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coalesces quick keystrokes, filters off the UI thread, and discards stale results. */
class SearchRunner(private val owner: LifecycleOwner) {
    private var job: Job? = null
    fun <T> submit(produce: () -> T, display: (T) -> Unit) {
        job?.cancel()
        job = owner.lifecycleScope.launch {
            delay(70)
            val result = withContext(Dispatchers.Default) { produce() }
            display(result)
        }
    }
    fun cancel() { job?.cancel() }
}
