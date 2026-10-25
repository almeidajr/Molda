package com.almeidajr.molda.testing

import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateRepository
import kotlinx.coroutines.CompletableDeferred

/** Updates without GitHub: every check answers [result], which a test may change later. */
class FakeUpdateRepository(@Volatile var result: UpdateCheck = UpdateCheck.UpToDate) :
    UpdateRepository {
    /** When set, every check waits for it to complete, so tests can observe the pending state. */
    @Volatile var checkGate: CompletableDeferred<Unit>? = null

    override suspend fun check(): UpdateCheck {
        checkGate?.await()
        return result
    }
}
