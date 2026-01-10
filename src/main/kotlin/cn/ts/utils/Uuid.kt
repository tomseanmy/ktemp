package cn.ts.utils

/**
 *     DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *            Version 2, December 2004
 *
 *  Copyright (C) 2024 0xShamil
 *
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 */

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import kotlinx.coroutines.channels.produce
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * UUIDv7 generator with coroutine-friendly APIs.
 *
 * Notes:
 * - 48-bit unix epoch millis, 4-bit version (7), 12-bit rand_a (here used as a monotonic sequence),
 *   2-bit variant (RFC 4122), 62-bit rand_b.
 * - Monotonic sequence guarantees ordering for calls within the same millisecond.
 * - Thread- and coroutine-safe (atomics, no blocking locks).
 */
object Uuid {
    private val rng = SecureRandom()

    // Monotonic state across threads/coroutines
    private val lastMillis = AtomicLong(Long.MIN_VALUE)
    private val lastSeq12 = AtomicInteger(-1)

    /**
     * Generate a UUIDv7 string.
     * Default is 32-char hex without dashes to fit varchar(32) columns.
     */
    fun randomUUID(compact: Boolean = true): String {
        val bytes = nextBytesV7()
        val msb = readLongBE(bytes, 0)
        val lsb = readLongBE(bytes, 8)
        val uuid = UUID(msb, lsb)
        return if (compact) uuidToHex32(uuid) else uuid.toString()
    }

    /**
     * Suspend-friendly variant returning a string.
     */
    suspend fun randomUUIDSuspend(compact: Boolean = true): String {
        return randomUUID(compact)
    }

    /**
     * Generate a UUIDv7 as java.util.UUID object.
     */
    fun randomUUIDObject(): UUID {
        val bytes = nextBytesV7()
        return UUID(readLongBE(bytes, 0), readLongBE(bytes, 8))
    }

    /**
     * Cold Flow producing [count] UUIDv7 values lazily on collection.
     */
    fun flow(count: Long, compact: Boolean = true): Flow<String> = flow {
        var i = 0L
        while (i < count) {
            emit(randomUUID(compact))
            i++
            // Cooperative scheduling for very tight loops in coroutine contexts
            if ((i and 0xFF) == 0L) yield()
        }
    }

    /**
     * Hot channel producing UUIDv7 strings until the scope is cancelled.
     * Typical usage: scope.uuidV7Channel(capacity = 256). Receive and consume.
     */
    fun CoroutineScope.uuidV7Channel(
        capacity: Int = 128,
        compact: Boolean = true
    ): ReceiveChannel<String> = produce(capacity = capacity) {
        while (isActive) {
            send(randomUUID(compact))
        }
    }

    // Build 16 bytes of UUIDv7, filling randomness first then setting fixed fields.
    private fun nextBytesV7(): ByteArray {
        val out = ByteArray(16)
        rng.nextBytes(out)

        val now = System.currentTimeMillis()
        val last = lastMillis.get()
        val ts = if (now >= last) now else last // clamp to guarantee monotonic time

        // Write 48-bit big-endian unix millis
        out[0] = ((ts ushr 40) and 0xFF).toByte()
        out[1] = ((ts ushr 32) and 0xFF).toByte()
        out[2] = ((ts ushr 24) and 0xFF).toByte()
        out[3] = ((ts ushr 16) and 0xFF).toByte()
        out[4] = ((ts ushr 8) and 0xFF).toByte()
        out[5] = (ts and 0xFF).toByte()

        // 12-bit rand_a replaced with a monotonic sequence for same-ms ordering
        val seq = nextSeq(ts)

        // Version (7) high nibble of byte 6, low nibble is seq[11:8]
        out[6] = (((0x7 shl 4) or ((seq ushr 8) and 0x0F))).toByte()
        // Low 8 bits of seq to byte 7
        out[7] = (seq and 0xFF).toByte()

        // Set RFC 4122 variant: 10xxxxxx in the high bits of byte 8
        out[8] = ((out[8].toInt() and 0x3F) or 0x80).toByte()

        return out
    }

    // CAS loop for 12-bit monotonic counter keyed by millisecond
    private fun nextSeq(ts: Long): Int {
        while (true) {
            val lastTs = lastMillis.get()
            val lastSeq = lastSeq12.get()

            if (ts != lastTs) {
                // New millisecond: randomize start to keep entropy
                val seeded = rng.nextInt(1 shl 12)
                if (lastMillis.compareAndSet(lastTs, ts)) {
                    lastSeq12.set(seeded)
                    return seeded
                }
            } else {
                val next = (lastSeq + 1) and 0x0FFF
                if (lastSeq12.compareAndSet(lastSeq, next)) {
                    return next
                }
            }
            // Retry on contention
        }
    }

    // Read 8 bytes as big-endian long
    private fun readLongBE(buf: ByteArray, off: Int): Long {
        var v = 0L
        var i = 0
        while (i < 8) {
            v = (v shl 8) or (buf[off + i].toLong() and 0xFFL)
            i++
        }
        return v
    }

    // Convert UUID to 32-char lowercase hex without dashes
    private fun uuidToHex32(u: UUID): String {
        val msb = u.mostSignificantBits
        val lsb = u.leastSignificantBits
        val chars = CharArray(32)

        // Write 16 bytes to hex chars
        writeLongAsHex(msb, chars, 0)
        writeLongAsHex(lsb, chars, 16)
        return String(chars)
    }

    private val HEX = charArrayOf(
        '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
    )

    private fun writeLongAsHex(value: Long, dst: CharArray, charOffset: Int) {
        var v = value
        var i = 0
        // Extract 8 bytes big-endian
        var b7 = (v and 0xFFL).toInt(); v = v ushr 8
        var b6 = (v and 0xFFL).toInt(); v = v ushr 8
        var b5 = (v and 0xFFL).toInt(); v = v ushr 8
        var b4 = (v and 0xFFL).toInt(); v = v ushr 8
        var b3 = (v and 0xFFL).toInt(); v = v ushr 8
        var b2 = (v and 0xFFL).toInt(); v = v ushr 8
        var b1 = (v and 0xFFL).toInt(); v = v ushr 8
        var b0 = (v and 0xFFL).toInt()

        // write as hex for bytes b0..b7
        i = charOffset
        i = writeByteHex(b0, dst, i)
        i = writeByteHex(b1, dst, i)
        i = writeByteHex(b2, dst, i)
        i = writeByteHex(b3, dst, i)
        i = writeByteHex(b4, dst, i)
        i = writeByteHex(b5, dst, i)
        i = writeByteHex(b6, dst, i)
        writeByteHex(b7, dst, i)
    }

    private fun writeByteHex(b: Int, dst: CharArray, off: Int): Int {
        dst[off] = HEX[(b ushr 4) and 0xF]
        dst[off + 1] = HEX[b and 0xF]
        return off + 2
    }
}
