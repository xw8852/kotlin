/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import jetbrains.buildServer.messages.serviceMessages.Message
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageParserCallback
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLogger
import org.jetbrains.kotlin.gradle.utils.clearAnsiColor
import java.text.ParseException

class TeamCityMessageCommonClient(
    private val log: Logger,
    private val progressLogger: ProgressLogger
) : ServiceMessageParserCallback {

    private val errors = mutableListOf<String>()

    private val stackTraceProcessor = TeamCityMessageStackTraceProcessor()

    override fun parseException(e: ParseException, text: String) {
        log.error("Failed to parse test process messages: \"$text\"", e)
    }

    override fun serviceMessage(message: ServiceMessage) {
        when (message) {
            is Message -> printMessage(message.text, LogType.byValueOrNull(message.attributes["type"]))
        }
    }

    internal fun testFailedMessage(): String {
        return errors
            .joinToString("\n")
    }

    private fun printMessage(text: String, type: LogType?) {
        val value = text.trimEnd()
        progressLogger.progress(value)

        val inStackTrace = stackTraceProcessor.process(text) { line, logType ->
            log.processLogMessage(line, logType)
            errors.add(line.clearAnsiColor())
        }

        if (inStackTrace) return

        if (type?.isErrorLike() == true) {
            errors.add(value.clearAnsiColor())
        }

        type?.let { log.processLogMessage(value, it) }
    }

    override fun regularText(text: String) {
        printMessage(text, null)
    }
}