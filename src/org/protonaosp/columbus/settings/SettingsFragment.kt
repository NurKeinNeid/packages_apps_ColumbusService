/*
 * Copyright (C) 2020 The Proton AOSP Project
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

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.widget.LabeledSeekBarPreference
import com.android.settings.widget.SeekBarPreference
import com.android.settingslib.widget.MainSwitchPreference
import org.protonaosp.columbus.PREFS_NAME
import org.protonaosp.columbus.R
import org.protonaosp.columbus.getAction
import org.protonaosp.columbus.getActionName
import org.protonaosp.columbus.getAllowScreenOff
import org.protonaosp.columbus.getDePrefs
import org.protonaosp.columbus.getEnabled
import org.protonaosp.columbus.getLaunchActionAppName
import org.protonaosp.columbus.getSensitivity

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var prefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = PREFS_NAME

        prefs = context!!.getDePrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        updateUi()
    }

    private fun updateUi() {
        // Enabled
        val enabled = prefs.getEnabled(context)
        findPreference<MainSwitchPreference>(getString(R.string.pref_key_enabled))?.apply {
            setChecked(enabled)
        }

        // Sensitivity value
        findPreference<LabeledSeekBarPreference>(getString(R.string.pref_key_sensitivity))?.apply {
            progress = prefs.getSensitivity(context)
            setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS)
        }

        // Action value and summary
        val key_action = getString(R.string.pref_key_action)
        val action_value = prefs.getAction(context)
        findPreference<ListPreference>(key_action)?.apply {
            value = action_value
            summary = prefs.getActionName(context)
        }

        val launch_app: Preference? =
            findPreference<Preference>(getString(R.string.pref_key_launch_app))
        launch_app?.apply {
            val appname = prefs.getLaunchActionAppName(context)
            if (appname != getString(R.string.launch_app_default)) {
                summary = appname
            } else {
                summary = getString(R.string.launch_app_select_summary)
            }
        }
        launch_app?.setVisible(enabled && action_value == getString(R.string.action_launch_value))

        // Screen state based on action
        findPreference<SwitchPreferenceCompat>(getString(R.string.pref_key_allow_screen_off))
            ?.apply {
                val screenForced =
                    prefs.getBoolean(
                        getString(R.string.pref_key_allow_screen_off_action_forced),
                        false,
                    )
                setEnabled(!screenForced)
                if (screenForced) {
                    setSummary(getString(R.string.setting_screen_off_blocked_summary))
                    setPersistent(false)
                    setChecked(false)
                } else {
                    setSummary(getString(R.string.setting_screen_off_summary))
                    setPersistent(true)
                    setChecked(prefs.getAllowScreenOff(context))
                }
            }
    }
}
