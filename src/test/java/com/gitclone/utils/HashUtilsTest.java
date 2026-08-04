package com.gitclone.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HashUtilsTest {

    @Test
    public void testSha1Hex() {
        String testInput = "test";
        // SHA-1 of "test" is a94a8fe5ccb19ba61c4c0873d391e987982fbbd3
        String expectedSha1 = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
        String actualSha1 = HashUtils.sha1Hex(testInput.getBytes());
        assertEquals(expectedSha1, actualSha1);
    }

    @Test
    public void testHexConversion() {
        String hex = "01020304050a0f10ff";
        byte[] bytes = HashUtils.hexToBytes(hex);
        String resultHex = HashUtils.bytesToHex(bytes);
        assertEquals(hex, resultHex);
    }
}
