package com.commitnoteai.vcs

object TypewriterText {
    data class State(
        val target: String,
        val visibleLength: Int,
    ) {
        val visibleText: String
            get() = target.take(visibleLength)

        val isComplete: Boolean
            get() = visibleLength >= target.length
    }

    fun step(state: State, chunkSize: Int): State {
        val nextLength = (state.visibleLength + chunkSize.coerceAtLeast(1)).coerceAtMost(state.target.length)
        return state.copy(visibleLength = nextLength)
    }
}
