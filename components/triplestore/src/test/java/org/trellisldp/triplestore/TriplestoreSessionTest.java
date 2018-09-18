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
package org.trellisldp.triplestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class TriplestoreSessionTest {

    @Test
    public void testSimpleSession() {
        final Session s1 = new SimpleSession(Trellis.AdministratorAgent);
        final Session s2 = new SimpleSession(Trellis.AdministratorAgent);
        assertNotEquals(s1.getIdentifier(), s2.getIdentifier(), "Identifiers should be unique!");
        assertEquals(s1.getAgent(), s2.getAgent(), "Agents should be equal!");
        assertFalse(s1.getCreated().isAfter(s2.getCreated()), "Dates should be sequential!");
        assertFalse(s1.getDelegatedBy().isPresent(), "Unexpected delegation value!");
    }
}
