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

package stanissay.wear.pass.pin

import stanissay.wear.pass.utils.SafeResult

class SafeLogic(private val length: Int) {
    var cursor: Int? = null
        private set
    private var direction = 0
    private val digits = StringBuilder()
    val count get() = digits.length
    val isComplete get() = digits.length >= length
    val awaitingLast
        get() = !isComplete && digits.length == length - 1 && cursor != null

    fun step(dir: Int): SafeResult {
        require(dir == 1 || dir == -1)
        if (isComplete) return SafeResult.Ignored

        val c = cursor
        if (c == null) {
            cursor = if (dir > 0) 0 else 9
            direction = dir
            return SafeResult.Moved
        }
        if (dir == direction) {
            cursor = (c + dir + 10) % 10
            return SafeResult.Moved
        }

        direction = dir
        digits.append(c)
        cursor = (c + dir + 10) % 10
        return if (isComplete) SafeResult.Completed else SafeResult.Committed
    }

    fun commitCurrent(): SafeResult {
        val c = cursor
        if (isComplete || digits.length != length - 1 || c == null) return SafeResult.Ignored
        digits.append(c)
        return SafeResult.Completed
    }

    fun commitByDwell(): SafeResult = if (awaitingLast) commitCurrent() else SafeResult.Ignored

    fun clear() {
        digits.setLength(0)
        cursor = null
        direction = 0
    }

    fun code(): CharArray = digits.toString().toCharArray()
}