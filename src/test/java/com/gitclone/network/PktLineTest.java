package com.gitclone.network;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PktLineTest {

    @Test
    public void testFormatString() {
        String payload = "want a94a8fe5ccb19ba61c4c0873d391e987982fbbd3\n";
        byte[] formatted = PktLine.format(payload);
        
        // expected prefix length is payload length (46) + 4 = 50. In hex: "0032"
        String expectedPrefix = "0032";
        String actualPrefix = new String(formatted, 0, 4, StandardCharsets.UTF_8);
        assertEquals(expectedPrefix, actualPrefix);
        
        String actualPayload = new String(formatted, 4, formatted.length - 4, StandardCharsets.UTF_8);
        assertEquals(payload, actualPayload);
    }

    @Test
    public void testFormatFlush() {
        byte[] flush = PktLine.formatFlush();
        assertArrayEquals("0000".getBytes(StandardCharsets.UTF_8), flush);
    }

    @Test
    public void testParseStream() {
        // Prepare combined stream of:
        // 1. "000btest\n12" -> 11 bytes: length 11 ("000b"), payload "test\n12" (7 bytes)
        // 2. "0000" -> Flush
        // 3. "000a123456" -> 10 bytes: length 10 ("000a"), payload "123456" (6 bytes)
        byte[] stream = "000btest\n120000000a123456".getBytes(StandardCharsets.UTF_8);
        
        List<byte[]> parsed = PktLine.parse(stream);
        assertEquals(3, parsed.size());
        
        assertEquals("test\n12", new String(parsed.get(0), StandardCharsets.UTF_8));
        assertEquals(0, parsed.get(1).length); // Flush represents empty byte array
        assertEquals("123456", new String(parsed.get(2), StandardCharsets.UTF_8));
    }
}
