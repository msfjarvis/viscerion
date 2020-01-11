package me.msfjarvis.viscerion.crypto;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class KeyTest {
    private static final String TEST_KEY = "Lr6H7NLgVC44JOe0t8P1X5dPm0QbF8JnBQjLdbWUiNk=";

    @Test
    public void generating_key_from_base64() throws KeyFormatException {
        final Key key = Key.fromBase64(TEST_KEY);
        assertEquals(TEST_KEY, key.toBase64());
        assertEquals(key.toBase64().length(), Key.Format.BASE64.getLength());
    }
}
