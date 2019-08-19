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
package org.trellisldp.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.AgentService;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class DefaultAgentServiceTest {

    @Test
    public void testAgent() {
        final AgentService service = new DefaultAgentService();

        assertEquals("user:acoburn", service.asAgent("user:acoburn").getIRIString(), "Unexpected acoburn agent IRI!");
        assertEquals("user:foo/bar", service.asAgent("user:foo/bar").getIRIString(), "Unexpected foo/bar agent IRI!");
        assertEquals(Trellis.AnonymousAgent, service.asAgent(null), "null agent isn't anonymous!");
        assertEquals(Trellis.AnonymousAgent, service.asAgent(""), "blank agent isn't anonymous!");
    }
}
