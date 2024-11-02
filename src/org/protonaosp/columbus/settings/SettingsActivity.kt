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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import org.protonaosp.columbus.ColumbusService
import org.protonaosp.columbus.TAG

class SettingsActivity : CollapsingToolbarBaseActivity() {
    private var columbusService: ColumbusService? = null

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as ColumbusService.Binder
                columbusService = binder.getService()
                columbusService?.isSettingsActivityOnTop = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                columbusService?.isSettingsActivityOnTop = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        columbusService?.isSettingsActivityOnTop = true

        fragmentManager
            .beginTransaction()
            .replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                SettingsFragment(),
                TAG,
            )
            .commit()
    }

    override fun onResume() {
        super.onResume()
        Intent(this, ColumbusService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        columbusService?.isSettingsActivityOnTop = true
    }

    override fun onPause() {
        super.onPause()
        columbusService?.isSettingsActivityOnTop = false
        unbindService(connection)
    }
}
