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
package org.trellisldp.http.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class WantDigestTest {

    @Test
    public void testWantDigest1() {
        final WantDigest wantDigest = new WantDigest("md5, sha-1");
        assertEquals(2, wantDigest.getAlgorithms().size());
        assertEquals("MD5", wantDigest.getAlgorithms().get(0));
        assertEquals("SHA-1", wantDigest.getAlgorithms().get(1));
    }

    @Test
    public void testWantDigest2() {
        final WantDigest wantDigest = new WantDigest("sha-1, md5");
        assertEquals(2, wantDigest.getAlgorithms().size());
        assertEquals("SHA-1", wantDigest.getAlgorithms().get(0));
        assertEquals("MD5", wantDigest.getAlgorithms().get(1));
    }

    @Test
    public void testWantDigest3() {
        final WantDigest wantDigest = new WantDigest("sha-1;q=0.3, md5;q=1");
        assertEquals(2, wantDigest.getAlgorithms().size());
        assertEquals("MD5", wantDigest.getAlgorithms().get(0));
        assertEquals("SHA-1", wantDigest.getAlgorithms().get(1));
    }

    @Test
    public void testWantDigest4() {
        final WantDigest wantDigest = new WantDigest(null);
        assertTrue(wantDigest.getAlgorithms().isEmpty());
    }

    @Test
    public void testWantDigest5() {
        final WantDigest wantDigest = new WantDigest("sha-1;q=0.3, md5;q=blah");
        assertEquals(2, wantDigest.getAlgorithms().size());
        assertEquals("SHA-1", wantDigest.getAlgorithms().get(0));
        assertEquals("MD5", wantDigest.getAlgorithms().get(1));
    }

    @Test
    public void testWantDigest6() {
        final WantDigest wantDigest = new WantDigest("sha-1;q=0.3, md5;p=1.0");
        assertEquals(2, wantDigest.getAlgorithms().size());
        assertEquals("SHA-1", wantDigest.getAlgorithms().get(0));
        assertEquals("MD5", wantDigest.getAlgorithms().get(1));
    }
}
