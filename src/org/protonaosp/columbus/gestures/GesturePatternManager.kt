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

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.protonaosp.columbus.TAG
import java.util.UUID

class GesturePatternManager(private val context: Context, private val prefs: SharedPreferences) {
    private val patterns = mutableMapOf<String, GesturePattern>()
    private var currentSequence = mutableListOf<GesturePattern.TapEvent>()
    private var lastTapTime: Long = 0

    init {
        loadPatterns()
    }

    private fun loadPatterns() {
        patterns.clear()
        val patternsStr = prefs.getString(PREFS_KEY_PATTERNS, "")
        if (!patternsStr.isNullOrEmpty()) {
            patternsStr.split(";").forEach { patternStr ->
                GesturePattern.fromString(patternStr)?.let { pattern ->
                    patterns[pattern.id] = pattern
                }
            }
        }
        Log.d(TAG, "Loaded ${patterns.size} custom gesture patterns")
    }

    private fun savePatterns() {
        val patternsStr = patterns.values.joinToString(";") { GesturePattern.toString(it) }
        prefs.edit().putString(PREFS_KEY_PATTERNS, patternsStr).apply()
    }

    fun addPattern(name: String, pattern: List<GesturePattern.TapEvent>, actionKey: String): GesturePattern {
        val id = UUID.randomUUID().toString()
        val gesturePattern = GesturePattern(id, name, pattern, actionKey)
        patterns[id] = gesturePattern
        savePatterns()
        return gesturePattern
    }

    fun removePattern(id: String) {
        patterns.remove(id)
        savePatterns()
    }

    fun getPatterns(): List<GesturePattern> = patterns.values.toList()

    fun onTapEvent(type: Int, timestamp: Long): String? {
        // Clear sequence if too much time has passed
        if (timestamp - lastTapTime > MAX_SEQUENCE_TIMEOUT) {
            currentSequence.clear()
        }

        // Add new tap to sequence
        currentSequence.add(GesturePattern.TapEvent(type))
        lastTapTime = timestamp

        // Check if sequence matches any pattern
        patterns.values.forEach { pattern ->
            if (isPatternMatch(pattern)) {
                currentSequence.clear()
                return pattern.actionKey
            }
        }

        // Clear sequence if it's too long
        if (currentSequence.size > MAX_SEQUENCE_LENGTH) {
            currentSequence.clear()
        }

        return null
    }

    private fun isPatternMatch(pattern: GesturePattern): Boolean {
        if (currentSequence.size != pattern.pattern.size) return false

        // Check if tap types match in sequence
        return currentSequence.zip(pattern.pattern).all { (current, expected) ->
            current.type == expected.type
        }
    }

    companion object {
        private const val PREFS_KEY_PATTERNS = "custom_gesture_patterns"
        private const val MAX_SEQUENCE_LENGTH = 5
        private const val MAX_SEQUENCE_TIMEOUT = 2000L // 2 seconds
    }
} 
