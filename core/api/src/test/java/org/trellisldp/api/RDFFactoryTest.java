/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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
package org.trellisldp.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;

class RDFFactoryTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testGetInstance() {
        assertNotNull(rdf, "RDF instance is null!");
    }

    @Test
    @EnabledOnJre(JAVA_8)
    void testGetService() {
        assertTrue(RDFFactory.findFirst(RDF.class).isPresent());
        assertFalse(RDFFactory.findFirst(String.class).isPresent());
    }
}
