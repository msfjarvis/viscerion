/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

public class KeyTest {
    private static final String TEST_KEY = "Lr6H7NLgVC44JOe0t8P1X5dPm0QbF8JnBQjLdbWUiNk=";

    @Test
    public void generating_key_from_base64() throws KeyFormatException {
        final Key key = Key.fromBase64(TEST_KEY);
        assertEquals(TEST_KEY, key.toBase64());
        assertEquals(key.toBase64().length(), Key.Format.BASE64.getLength());
    }

    @Test(expected = KeyFormatException.class)
    public void throws_when_key_is_invalid() throws KeyFormatException {
        Key.fromBase64(TEST_KEY + "invalid_data");
    }

    @Test
    public void throws_key_format_exception_with_length_type() {
        KeyFormatException exc = null;
        try {
            Key.fromBase64(TEST_KEY + "invalid_data");
        } catch (KeyFormatException kfe) {
            exc = kfe;
        }
        assertNotNull(exc);
        assertEquals(KeyFormatException.Type.LENGTH, exc.getType());
    }

    @Test
    @Ignore("I'm generating bad base64 because I don't understand this math")
    public void throw_key_format_exception_with_contents_type() {
        KeyFormatException exc = null;
        try {
            Key.fromBase64("ca4OBkHIlUG8okG7DriV5gZETb+DAE7jqn5K0+WMnIo=");
        } catch (KeyFormatException kfe) {
            exc = kfe;
        }
        assertNotNull(exc);
        assertEquals(KeyFormatException.Type.CONTENTS, exc.getType());
    }

    @Test
    public void hashCode_works_as_expected() throws KeyFormatException {
        assertEquals(67, Key.fromBase64(TEST_KEY).hashCode());
    }
}
