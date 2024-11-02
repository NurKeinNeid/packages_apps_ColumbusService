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

package org.protonaosp.columbus.sensors

enum class MessageType(val id: Int) {
    RECOGNIZER_START(100),
    RECOGNIZER_STOP(101),
    SENSITIVITY_UPDATE(200),
    GESTURE_DETECTED(300),
    GATE_START(1),
    GATE_STOP(2),
    HIGH_IMU_ODR_START(3),
    HIGH_IMU_ODR_STOP(4),
    ML_PREDICTION_START(5),
    ML_PREDICTION_STOP(6),
    SINGLE_TAP(7),
    DOUBLE_TAP(8),
}
