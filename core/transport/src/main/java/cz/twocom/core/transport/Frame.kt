package cz.twocom.core.transport

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Minimal versioned wire frame: 1-byte type, 1-byte version, 4-byte big-endian length,
 * length-prefixed payload — every field bounds-checked on read (invariant B.8: never trust
 * the wire). A full Protobuf migration per doc/10_API_Spec.md is a tracked follow-up; this
 * is the smallest structure that lets HELLO/DATA framing be validated instead of raw bytes.
 */
object FrameType {
    const val HELLO: Int = 1
    const val DATA: Int = 2
}

const val WIRE_PROTOCOL_VERSION: Int = 2
private const val MAX_FRAME_PAYLOAD_BYTES = 1 shl 20 // 1 MiB — generous for text + Signal envelope overhead

class FrameFormatException(message: String) : Exception(message)

data class Frame(val type: Int, val version: Int, val payload: ByteArray)

fun writeFrame(out: OutputStream, type: Int, payload: ByteArray) {
    require(payload.size <= MAX_FRAME_PAYLOAD_BYTES) { "Frame payload too large: ${payload.size}" }
    val d = DataOutputStream(out)
    d.writeByte(type)
    d.writeByte(WIRE_PROTOCOL_VERSION)
    d.writeInt(payload.size)
    d.write(payload)
    d.flush()
}

/** Reads exactly one frame, validating type/version/length before trusting the payload. */
fun readFrame(input: InputStream): Frame {
    val d = DataInputStream(input)
    try {
        val type = d.readUnsignedByte()
        if (type != FrameType.HELLO && type != FrameType.DATA) {
            throw FrameFormatException("Unknown frame type $type")
        }
        val version = d.readUnsignedByte()
        if (version != WIRE_PROTOCOL_VERSION) {
            throw FrameFormatException("Unsupported wire protocol version $version")
        }
        val length = d.readInt()
        if (length < 0 || length > MAX_FRAME_PAYLOAD_BYTES) {
            throw FrameFormatException("Invalid frame length $length")
        }
        val payload = ByteArray(length)
        d.readFully(payload)
        return Frame(type, version, payload)
    } catch (e: EOFException) {
        throw FrameFormatException("Connection closed mid-frame")
    }
}
