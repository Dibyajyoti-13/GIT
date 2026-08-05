package com.gitclone.git;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class DeltaResolverTest {

    @Test
    public void testApplyDeltaCopyAndInsert() throws IOException {
        String baseStr = "abcdefghijklmnopqrstuvwxyz";
        byte[] baseBytes = baseStr.getBytes();

        // Target: "defgXYZ" (Length 7)
        // 1. Base size 26 -> 0x1a
        // 2. Target size 7 -> 0x07
        // 3. COPY command: offset 3, size 4. 
        //    cmd = 0x80 | 0x01 (offset byte 1) | 0x10 (size byte 1) = 0x91.
        //    offset = 0x03, size = 0x04
        // 4. INSERT command: size 3. cmd = 0x03.
        //    insert bytes = 'X', 'Y', 'Z'
        byte[] deltaInstructions = {
                0x1a, // Base size 26
                0x07, // Target size 7
                (byte) 0x91, // COPY command (cmd=0x91)
                0x03, // Offset 3
                0x04, // Size 4
                0x03, // INSERT command (cmd=3)
                'X', 'Y', 'Z'
        };

        byte[] result = DeltaResolver.applyDelta(baseBytes, deltaInstructions);
        assertNotNull(result);
        assertEquals("defgXYZ", new String(result));
    }
}
