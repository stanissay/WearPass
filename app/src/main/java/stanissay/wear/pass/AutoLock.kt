/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import stanissay.wear.pass.utils.DataConstants
import java.util.concurrent.Executor

class AutoLock(private val onLock: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var since = 0L
    private val task = Runnable { onLock() }

    fun onBackground(timeoutMs: Long = DataConstants.AUTO_LOCK_MS) {
        if (since != 0L) return
        since = SystemClock.elapsedRealtime()
        handler.postDelayed(task, timeoutMs)
    }

    fun onForeground(timeoutMs: Long = DataConstants.AUTO_LOCK_MS) {
        handler.removeCallbacks(task)
        val expired = since != 0L && SystemClock.elapsedRealtime() - since >= timeoutMs
        since = 0L
        if (expired) onLock()
    }

    fun lockIfExpired(timeoutMs: Long = DataConstants.AUTO_LOCK_MS) {
        if (since != 0L && SystemClock.elapsedRealtime() - since >= timeoutMs) onLock()
    }
}

class BodyLock(context: Context, private val onLock: () -> Unit) {
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)

    private var active = false
    private var sensorRegistered = false
    private var watcher: KeyguardWatcher? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.firstOrNull() == 0f) onLock()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (active) return
        active = true

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
        sensorRegistered = sensor != null &&
                sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        watcher = runCatching {
            KeyguardWatcher(keyguard, executor) { onLock() }.also { it.start() }
        }.getOrNull()
    }

    fun stop() {
        if (!active) return
        active = false
        if (sensorRegistered) sensorManager?.unregisterListener(sensorListener)
        sensorRegistered = false
        watcher?.let { runCatching { it.stop() } }
        watcher = null
    }
}

private class KeyguardWatcher(
    private val keyguard: KeyguardManager,
    private val executor: Executor,
    private val onLocked: () -> Unit,
) {
    private val listener = KeyguardManager.KeyguardLockedStateListener { locked ->
        if (locked) onLocked()
    }
    fun start() = keyguard.addKeyguardLockedStateListener(executor, listener)
    fun stop() = keyguard.removeKeyguardLockedStateListener(listener)
}