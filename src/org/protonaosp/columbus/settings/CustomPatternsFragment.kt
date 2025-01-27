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

package org.protonaosp.columbus.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.protonaosp.columbus.R
import org.protonaosp.columbus.gestures.GesturePattern
import org.protonaosp.columbus.gestures.GesturePatternManager

class CustomPatternsFragment : Fragment() {
    private lateinit var patternManager: GesturePatternManager
    private lateinit var patternsContainer: LinearLayout
    private var isRecording = false
    private val recordedPattern = mutableListOf<GesturePattern.TapEvent>()
    private var recordStartTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_custom_patterns, container, false)
        
        patternManager = GesturePatternManager(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        
        patternsContainer = view.findViewById(R.id.patterns_container)
        val addButton = view.findViewById<Button>(R.id.add_pattern)
        addButton.setOnClickListener { showAddPatternDialog() }
        
        updatePatternsList()
        return view
    }

    private fun updatePatternsList() {
        patternsContainer.removeAllViews()
        val patterns = patternManager.getPatterns()
        
        if (patterns.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.setText(R.string.custom_patterns_empty)
            emptyText.setPadding(32, 32, 32, 32)
            patternsContainer.addView(emptyText)
            return
        }

        patterns.forEach { pattern ->
            val patternView = createPatternView(pattern)
            patternsContainer.addView(patternView)
        }
    }

    private fun createPatternView(pattern: GesturePattern): View {
        val view = layoutInflater.inflate(R.layout.item_pattern, null)
        view.findViewById<TextView>(R.id.pattern_name).text = pattern.name
        view.findViewById<TextView>(R.id.pattern_sequence).text = 
            pattern.pattern.joinToString(" → ") { 
                if (it.type == GesturePattern.SINGLE_TAP) "Single" else "Double" 
            }
        
        view.findViewById<View>(R.id.delete_pattern).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.custom_patterns_delete_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    patternManager.removePattern(pattern.id)
                    updatePatternsList()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        return view
    }

    private fun showAddPatternDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pattern, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.pattern_name)
        val recordButton = dialogView.findViewById<Button>(R.id.record_pattern)
        val sequenceText = dialogView.findViewById<TextView>(R.id.sequence_text)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.custom_patterns_add)
            .setView(dialogView)
            .setPositiveButton(R.string.custom_patterns_save, null)
            .setNegativeButton(R.string.custom_patterns_cancel) { _, _ ->
                isRecording = false
                recordedPattern.clear()
            }
            .create()

        recordButton.setOnClickListener {
            if (!isRecording) {
                isRecording = true
                recordedPattern.clear()
                recordStartTime = System.currentTimeMillis()
                recordButton.setText(R.string.custom_patterns_recording)
                sequenceText.setText(R.string.custom_patterns_tap_to_record)
            }
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameInput.text.toString()
                if (name.isBlank()) {
                    nameInput.error = "Name required"
                    return@setOnClickListener
                }
                if (recordedPattern.isEmpty()) {
                    sequenceText.error = "Pattern required"
                    return@setOnClickListener
                }
                
                patternManager.addPattern(name, recordedPattern, "screenshot") // Default action
                updatePatternsList()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun onTapDetected(type: Int) {
        if (isRecording) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - recordStartTime > GesturePatternManager.MAX_SEQUENCE_TIMEOUT) {
                isRecording = false
                recordedPattern.clear()
                return
            }
            
            recordedPattern.add(GesturePattern.TapEvent(type))
            // Update UI to show recorded pattern
            view?.findViewById<TextView>(R.id.sequence_text)?.text = 
                recordedPattern.joinToString(" → ") { 
                    if (it.type == GesturePattern.SINGLE_TAP) "Single" else "Double" 
                }
        }
    }
} 
