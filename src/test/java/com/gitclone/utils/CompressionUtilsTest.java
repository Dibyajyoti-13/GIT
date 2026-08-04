package com.gitclone.utils;

import org.junit.jupiter.api.Test;
import java.util.zip.DataFormatException;
import static org.junit.jupiter.api.Assertions.*;

public class CompressionUtilsTest {

    @Test
    public void testCompressDecompress() throws DataFormatException {
        String original = "This is a test string to verify zlib compression and decompression.";
        byte[] originalBytes = original.getBytes();

        byte[] compressed = CompressionUtils.compress(originalBytes);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);

        byte[] decompressed = CompressionUtils.decompress(compressed);
        assertArrayEquals(originalBytes, decompressed);
        assertEquals(original, new String(decompressed));
    }
}
