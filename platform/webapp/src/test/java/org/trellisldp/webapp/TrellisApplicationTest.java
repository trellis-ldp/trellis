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
package org.trellisldp.webapp;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.text.RandomStringGenerator;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(PER_CLASS)
public class TrellisApplicationTest extends JerseyTest {

    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('a', 'z').build();

    @Override
    protected Application configure() {
        return new TrellisApplication();
    }

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
        final String id = "-" + generator.generate(5);
        System.setProperty("trellis.rdf.location", System.getProperty("trellis.rdf.location") + id);
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
        System.clearProperty("trellis.rdf.location");
    }

    @Test
    public void testSimple() {
        final Response res = target().request().get();
        assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response code!");
    }
}
