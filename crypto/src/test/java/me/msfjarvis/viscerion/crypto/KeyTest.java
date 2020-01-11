/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeyTest {
    private static final String TEST_KEY = "Lr6H7NLgVC44JOe0t8P1X5dPm0QbF8JnBQjLdbWUiNk=";

    @Test
    public void generating_key_from_base64() throws KeyFormatException {
        final Key key = Key.fromBase64(TEST_KEY);
        assertEquals(TEST_KEY, key.toBase64());
        assertEquals(key.toBase64().length(), Key.Format.BASE64.getLength());
    }
}
