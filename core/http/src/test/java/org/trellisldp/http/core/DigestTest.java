/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.http.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class DigestTest {

    @Test
    public void testDigest() {
        final Digest d = Digest.valueOf("md5=HUXZLQLMuI/KZ5KDcJPcOA==");
        assertEquals("md5", d.getAlgorithm(), "check algorithm name");
        assertEquals("HUXZLQLMuI/KZ5KDcJPcOA==", d.getDigest(), "check digest value");
    }

    @Test
    public void testDigest2() {
        final Digest d = new Digest("md5", "HUXZLQLMuI/KZ5KDcJPcOA==");
        assertEquals("md5", d.getAlgorithm(), "Check md5 algorithm name");
        assertEquals("HUXZLQLMuI/KZ5KDcJPcOA==", d.getDigest(), "Check digest value");
    }

    @Test
    public void testInvalidDigest() {
        assertNull(Digest.valueOf("blah"), "Check parsing invalid digest");
    }
}
