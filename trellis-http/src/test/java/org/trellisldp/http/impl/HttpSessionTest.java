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
package org.trellisldp.http.impl;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class HttpSessionTest {

    @Test
    public void testHttpSession() {
        final Instant time = now();
        final Session session = new HttpSession();
        assertEquals(Trellis.AnonymousUser, session.getAgent());
        assertFalse(session.getDelegatedBy().isPresent());
        assertTrue(session.getIdentifier().getIRIString().startsWith("trellis:session/"));
        final Session session2 = new HttpSession();
        assertNotEquals(session.getIdentifier(), session2.getIdentifier());
        assertFalse(session.getCreated().isBefore(time));
        assertFalse(session.getCreated().isAfter(session2.getCreated()));
    }
}
