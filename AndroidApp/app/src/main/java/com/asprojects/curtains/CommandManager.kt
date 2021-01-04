package com.asprojects.curtains

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer


enum class Commands (val command : Byte){
    CONNECT(1),
    SYNC_FROM(2),
    SYNC_CHECK(3),
    MANUAL_RUN(10),
    MANUAL_STOP(11),
    SET_ENABLED(20),
    SET_OVERRIDE_ENABLED(30),
    SET_OVERRIDE_TIME(31),
    SET_OVERRIDE_LENGTH(32),
    SET_DAY_ENABLED(40),
    SET_DAY_TIME(41),
    SET_DAY_LENGTH(42)
}

val PROTOCOL_VERSION : Byte  = 1
val BASE_MESSAGE_SIZE : Byte = 1 + 1 + 1 // LENGTH + VERSION + COMMAND

var socket : Socket = Socket()
var timeout = 500 // ms

fun Boolean.toByte() : Byte = if (this) 1 else 0
fun Byte.toBoolean() : Boolean = this == 1.toByte()

fun sendCommandToggleEnabled (enabled: Boolean) {
    val buffer_length = BASE_MESSAGE_SIZE + 1
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_ENABLED.command)
    buffer.put(enabled.toByte())

    transmit(buffer)

    sync_check()
}

fun sendCommandSetNextDayOverrideEnabled (enabled: Boolean) {
    val buffer_length = BASE_MESSAGE_SIZE + 1
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_OVERRIDE_ENABLED.command)
    buffer.put(enabled.toByte())

    transmit(buffer)

    sync_check()
}

fun sendCommandSetNextDayOverrideTime (time: Int) {
    val buffer_length = BASE_MESSAGE_SIZE + 4
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_OVERRIDE_TIME.command)
    buffer.putInt(time)

    transmit(buffer)

    sync_check()
}

fun sendCommandSetNextDayOverrideLength (length: Int) {
    val buffer_length = BASE_MESSAGE_SIZE + 4
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_OVERRIDE_LENGTH.command)
    buffer.putInt(length)

    transmit(buffer)

    sync_check()
}

fun sendCommandDayEnabled(index: Int, enabled: Boolean) {
    val buffer_length = BASE_MESSAGE_SIZE + 2
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_DAY_ENABLED.command)
    buffer.put(index.toByte())
    buffer.put(enabled.toByte())

    transmit(buffer)

    sync_check()
}

fun sendCommandDayTime(index: Int, time: Int) {
    val buffer_length = BASE_MESSAGE_SIZE + 5
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_DAY_TIME.command)
    buffer.put(index.toByte())
    buffer.putInt(time)

    transmit(buffer)

    sync_check()
}

fun sendCommandDayLength(index: Int, length: Int) {
    val buffer_length = BASE_MESSAGE_SIZE + 5
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SET_DAY_LENGTH.command)
    buffer.put(index.toByte())
    buffer.putInt(length)

    transmit(buffer)

    sync_check()
}

fun reconnect () {
    try {
        socket.close()
        socket = Socket()
        socket.connect(InetSocketAddress(InetAddress.getByName(getIP()), getIPPort().toInt()), timeout)
        socket.soTimeout = timeout

        val buffer_length = BASE_MESSAGE_SIZE.toInt()
        var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

        buffer.put(buffer_length.toByte())
        buffer.put(PROTOCOL_VERSION)
        buffer.put(Commands.CONNECT.command)

        var result = transmit(buffer, true)

        connection_status_bool = result.first

        if (connection_status_bool) sync_check()
    }
    catch (e : Exception) {
        connection_status_bool = false
    }

    updateConnectionAndSyncStatus()
}

fun sync_to () {
    sendCommandSetNextDayOverrideEnabled(getNextDayOverrideEnabled())
    sendCommandSetNextDayOverrideTime(getNextDayOverrideTime())
    sendCommandSetNextDayOverrideLength(getNextDayOverrideLength())

    for (i in 0..6) {
        sendCommandDayEnabled(i, getDayEnabled(i))
        sendCommandDayTime(i, getDayTime(i))
        sendCommandDayLength(i, getDayLength(i))
    }

    // Send enabled last
    sendCommandToggleEnabled(getIsEnabled())

    sync_check()
}

fun sync_from () {
    val buffer_length = BASE_MESSAGE_SIZE.toInt()
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SYNC_FROM.command)

    var pair = transmit(buffer)

    if (pair.first) {
        var response = pair.second
        if (response.limit() == 73) {
            setIsEnabled(response.get().toBoolean())
            setNextDayOverrideEnabled(response.get().toBoolean())
            setNextDayOverrideTime(response.getInt())
            setNextDayOverrideLength(response.getInt())

            for (i in 0..6) {
                setDayEnabled(i, response.get().toBoolean())
                setDayTime(i, response.getInt())
                setDayLength(i, response.getInt())
            }
        }
    }

    sync_check()
}

fun sync_check() {
    val buffer_length = BASE_MESSAGE_SIZE.toInt()
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.SYNC_CHECK.command)

    var pair = transmit(buffer)

    sync_status_bool = false

    if (pair.first) {
        var response = pair.second
        if (response.limit() == 73) {
            var synced = true
            synced = synced && getIsEnabled() == response.get().toBoolean()
            synced = synced && getNextDayOverrideEnabled() == response.get().toBoolean()
            synced = synced && getNextDayOverrideTime() == response.getInt()
            synced = synced && getNextDayOverrideLength() == response.getInt()

            for (i in 0..6) {
                synced = synced && getDayEnabled(i) == response.get().toBoolean()
                synced = synced && getDayTime(i) == response.getInt()
                synced = synced && getDayLength(i) == response.getInt()
            }

            sync_status_bool = synced
        }
    }

    updateConnectionAndSyncStatus()
}

fun manual_run () {
    val buffer_length = BASE_MESSAGE_SIZE.toInt()
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.MANUAL_RUN.command)

    transmit(buffer)
}

fun manual_stop () {
    val buffer_length = BASE_MESSAGE_SIZE.toInt()
    var buffer: ByteBuffer = ByteBuffer.allocate(buffer_length)

    buffer.put(buffer_length.toByte())
    buffer.put(PROTOCOL_VERSION)
    buffer.put(Commands.MANUAL_STOP.command)

    transmit(buffer)
}

const val READ_TIMEOUT = 2000;

fun transmit (buffer : ByteBuffer, connecting : Boolean = false) : Pair<Boolean, ByteBuffer> {
    if (!connection_status_bool && !connecting) {
        reconnect()
        if (!connection_status_bool) {
            return Pair(false, ByteBuffer.allocate(0))
        }
    }

    if (connection_status_bool || connecting) {
        try {
            socket.getOutputStream().write(buffer.array())
            socket.getOutputStream().flush()

            var base_time = System.currentTimeMillis();
            while (socket.getInputStream().available() <= 0) {
                if (System.currentTimeMillis() - base_time > READ_TIMEOUT) {
                    return Pair(false, ByteBuffer.allocate(0))
                }
            }
            // First byte will be the length of the message (including itself)
            var expected_bytes = socket.getInputStream().read() - BASE_MESSAGE_SIZE

            base_time = System.currentTimeMillis();
            while (socket.getInputStream().available() < expected_bytes + 2) {
                if (System.currentTimeMillis() - base_time > READ_TIMEOUT) {
                    return Pair(false, ByteBuffer.allocate(0))
                }
            }

            if (socket.getInputStream().read().toByte() != buffer.get(1)) {
                // protocol versions don't match
                return Pair(false, ByteBuffer.allocate(0))
            }

            if (socket.getInputStream().read().toByte() != buffer.get(2)) {
                // response has a different command
                return Pair(false, ByteBuffer.allocate(0))
            }

            var read_bytes = 0
            var array: ByteArray = ByteArray(expected_bytes)
            while (read_bytes < expected_bytes) {
                read_bytes += socket.getInputStream().read(array, read_bytes, expected_bytes - read_bytes)
            }

            var response: ByteBuffer = ByteBuffer.wrap(array)
            return Pair(true, response)
        }
        catch (e: Exception) {
            connection_status_bool = false
            return Pair(false, ByteBuffer.allocate(0))
        }
    }

    return Pair(true, ByteBuffer.allocate(0))
}