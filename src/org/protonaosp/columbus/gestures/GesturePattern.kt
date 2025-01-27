/*
 * Copyright (C) 2025 DerpFest AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.protonaosp.columbus.gestures

data class GesturePattern(
    val id: String,
    val name: String,
    val pattern: List<TapEvent>,
    val actionKey: String,
    val timeoutMs: Long = 1000 // Maximum time between taps
) {
    data class TapEvent(
        val type: Int, // 1 for single tap, 2 for double tap
        val maxDelayMs: Long = 500 // Maximum delay after this tap
    )

    companion object {
        const val SINGLE_TAP = 1
        const val DOUBLE_TAP = 2

        // Convert pattern to/from string for storage
        fun toString(pattern: GesturePattern): String {
            return "${pattern.id}|${pattern.name}|${pattern.actionKey}|${pattern.timeoutMs}|" +
                pattern.pattern.joinToString(",") { "${it.type}:${it.maxDelayMs}" }
        }

        fun fromString(str: String): GesturePattern? {
            try {
                val parts = str.split("|")
                if (parts.size != 5) return null

                val tapEvents = parts[4].split(",").map {
                    val (type, delay) = it.split(":")
                    TapEvent(type.toInt(), delay.toLong())
                }

                return GesturePattern(
                    id = parts[0],
                    name = parts[1],
                    actionKey = parts[2],
                    timeoutMs = parts[3].toLong(),
                    pattern = tapEvents
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
} 
