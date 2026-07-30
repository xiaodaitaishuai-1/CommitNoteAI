package com.commitnoteai.vcs

import javax.swing.Timer

object CommitMessageTypewriter {
    private const val delayMs = 24
    private const val chunkSize = 2

    fun start(target: String, updateText: (String) -> Unit, onCompleted: () -> Unit) {
        var state = TypewriterText.State(target = target, visibleLength = 0)
        updateText(state.visibleText)
        if (state.isComplete) {
            onCompleted()
            return
        }

        val timer = Timer(delayMs, null)
        timer.addActionListener {
            state = TypewriterText.step(state, chunkSize)
            updateText(state.visibleText)
            if (state.isComplete) {
                timer.stop()
                onCompleted()
            }
        }
        timer.start()
    }
}
