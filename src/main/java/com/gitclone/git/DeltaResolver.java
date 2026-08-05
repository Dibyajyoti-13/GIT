package com.gitclone.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility to resolve delta-compressed Git objects.
 */
public class DeltaResolver {

    /**
     * Applies delta commands on top of base bytes and returns reconstructed object content.
     *
     * @param baseBytes Decompressed base object bytes
     * @param deltaInstructions Raw decompressed delta payload containing instructions
     * @return Reconstructed object bytes
     * @throws IOException if instruction parsing fails
     */
    public static byte[] applyDelta(byte[] baseBytes, byte[] deltaInstructions) throws IOException {
        int idx = 0;

        // 1. Parse base object size (variable-length int)
        long baseSize = 0;
        int shift = 0;
        while (true) {
            byte b = deltaInstructions[idx++];
            baseSize |= (long) (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                break;
            }
        }

        // 2. Parse target object size (variable-length int)
        long targetSize = 0;
        shift = 0;
        while (true) {
            byte b = deltaInstructions[idx++];
            targetSize |= (long) (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                break;
            }
        }

        ByteArrayOutputStream target = new ByteArrayOutputStream((int) targetSize);

        // 3. Process delta commands
        while (idx < deltaInstructions.length) {
            int cmd = deltaInstructions[idx++] & 0xFF;

            if ((cmd & 0x80) != 0) {
                // COPY Command
                long offset = 0;
                long size = 0;

                // Read offset (up to 4 bytes)
                if ((cmd & 0x01) != 0) offset |= (deltaInstructions[idx++] & 0xFF);
                if ((cmd & 0x02) != 0) offset |= (long) (deltaInstructions[idx++] & 0xFF) << 8;
                if ((cmd & 0x04) != 0) offset |= (long) (deltaInstructions[idx++] & 0xFF) << 16;
                if ((cmd & 0x08) != 0) offset |= (long) (deltaInstructions[idx++] & 0xFF) << 24;

                // Read size (up to 3 bytes)
                if ((cmd & 0x10) != 0) size |= (deltaInstructions[idx++] & 0xFF);
                if ((cmd & 0x20) != 0) size |= (long) (deltaInstructions[idx++] & 0xFF) << 8;
                if ((cmd & 0x40) != 0) size |= (long) (deltaInstructions[idx++] & 0xFF) << 16;

                if (size == 0) {
                    size = 0x10000; // 64KB default if size flag is omitted
                }

                // Copy bytes from base
                if (offset + size > baseBytes.length) {
                    throw new IOException("Delta offset/size exceeds base object size. Base size: " + baseBytes.length + ", Offset: " + offset + ", Size: " + size);
                }
                target.write(baseBytes, (int) offset, (int) size);

            } else if (cmd > 0) {
                // INSERT Command (cmd specifies size)
                if (idx + cmd > deltaInstructions.length) {
                    throw new IOException("Insert command size exceeds remaining instruction bytes");
                }
                target.write(deltaInstructions, idx, cmd);
                idx += cmd;
            } else {
                throw new IOException("Invalid delta command byte 0");
            }
        }

        byte[] result = target.toByteArray();
        if (result.length != targetSize) {
            throw new IOException("Reconstructed size mismatch. Expected: " + targetSize + ", Got: " + result.length);
        }
        return result;
    }
}
