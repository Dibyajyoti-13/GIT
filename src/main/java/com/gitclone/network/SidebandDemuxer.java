package com.gitclone.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Demultiplexes Git's sideband stream.
 */
public class SidebandDemuxer {
    private static final Logger logger = LoggerFactory.getLogger(SidebandDemuxer.class);

    /**
     * Decodes the sideband stream and writes Channel 1 data into the packfile stream.
     *
     * @param rawResponse The raw HTTP response bytes (composed of pkt-lines)
     * @param packfileOut Target OutputStream to write raw packfile bytes
     * @throws IOException if parsing fails or channel 3 error is received
     */
    public static void demux(byte[] rawResponse, OutputStream packfileOut) throws IOException {
        List<byte[]> packets = PktLine.parse(rawResponse);
        for (byte[] packet : packets) {
            if (packet.length == 0) {
                // Flush packet
                continue;
            }

            int channel = packet[0] & 0xFF;
            int payloadLen = packet.length - 1;
            if (payloadLen <= 0) {
                continue;
            }

            switch (channel) {
                case 1:
                    // Channel 1: Packfile Data
                    packfileOut.write(packet, 1, payloadLen);
                    break;
                case 2:
                    // Channel 2: Progress (Stderr Text)
                    String progressMsg = new String(packet, 1, payloadLen, StandardCharsets.UTF_8).trim();
                    logger.info("[Remote Progress] {}", progressMsg);
                    break;
                case 3:
                    // Channel 3: Error
                    String errMsg = new String(packet, 1, payloadLen, StandardCharsets.UTF_8).trim();
                    logger.error("[Remote Error] {}", errMsg);
                    throw new IOException("Git remote protocol error: " + errMsg);
                default:
                    logger.warn("Unknown sideband channel received: {}", channel);
                    break;
            }
        }
    }
}
