package com.gitclone.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Utility functions for compression and decompression using zlib (java.util.zip).
 */
public class CompressionUtils {

    /**
     * Compresses the input bytes using standard zlib deflate algorithm.
     *
     * @param input Data bytes to compress
     * @return Compressed byte array
     */
    public static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            deflater.end();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Decompression failed due to stream IO error", e);
        }
    }

    /**
     * Decompresses standard zlib-compressed input bytes.
     *
     * @param input Compressed data bytes
     * @return Decompressed byte array
     * @throws DataFormatException if data format is invalid
     */
    public static byte[] decompress(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            inflater.end();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Decompression failed due to stream IO error", e);
        }
    }
}
