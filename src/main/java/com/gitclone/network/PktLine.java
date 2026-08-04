package com.gitclone.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles formatting and parsing of Git's pkt-line protocol format.
 */
public class PktLine {

    public static final byte[] FLUSH = "0000".getBytes(StandardCharsets.UTF_8);
    public static final byte[] DELIM = "0001".getBytes(StandardCharsets.UTF_8);

    /**
     * Formats a String into a pkt-line format: 4 hex length prefix followed by string payload.
     */
    public static byte[] format(String payload) {
        if (payload == null) {
            return FLUSH;
        }
        return format(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Formats a raw byte array into a pkt-line format.
     */
    public static byte[] format(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return FLUSH;
        }
        int totalLength = payload.length + 4;
        if (totalLength > 65520) {
            throw new IllegalArgumentException("Payload too large for pkt-line format: " + payload.length);
        }
        String hexLength = String.format("%04x", totalLength);
        byte[] formatted = new byte[totalLength];
        System.arraycopy(hexLength.getBytes(StandardCharsets.UTF_8), 0, formatted, 0, 4);
        System.arraycopy(payload, 0, formatted, 4, payload.length);
        return formatted;
    }

    /**
     * Returns the 4-byte representation of a Flush packet: "0000".
     */
    public static byte[] formatFlush() {
        return FLUSH;
    }

    /**
     * Parses a byte array stream containing sequential pkt-lines.
     * Returns a list of the parsed payloads. Flush packets (0000) are returned as null or empty.
     * Let's represent flush packets as empty byte arrays.
     *
     * @param stream Raw byte stream containing pkt-lines
     * @return List of parsed packet payloads (excluding length prefixes)
     */
    public static List<byte[]> parse(byte[] stream) {
        List<byte[]> list = new ArrayList<>();
        int i = 0;
        while (i + 4 <= stream.length) {
            String hexLength = new String(stream, i, 4, StandardCharsets.UTF_8);
            int totalLength = Integer.parseInt(hexLength, 16);
            if (totalLength == 0) {
                // Flush packet
                list.add(new byte[0]); // empty represents FLUSH
                i += 4;
            } else if (totalLength == 1) {
                // Delim packet
                list.add(new byte[]{1}); // represents DELIM
                i += 4;
            } else {
                if (i + totalLength > stream.length) {
                    throw new IllegalArgumentException("Incomplete pkt-line stream. Expected length: " + totalLength + ", but remaining bytes: " + (stream.length - i));
                }
                int payloadLength = totalLength - 4;
                byte[] payload = new byte[payloadLength];
                System.arraycopy(stream, i + 4, payload, 0, payloadLength);
                list.add(payload);
                i += totalLength;
            }
        }
        return list;
    }
}
