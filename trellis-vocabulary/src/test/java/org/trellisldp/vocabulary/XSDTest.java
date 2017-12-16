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
package org.trellisldp.vocabulary;

import static org.apache.jena.vocabulary.XSD.NS;
import static org.apache.jena.vocabulary.XSD.dateTime;
import static org.apache.jena.vocabulary.XSD.xstring;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author acoburn
 */
public class XSDTest {

    public String namespace() {
        return "http://www.w3.org/2001/XMLSchema#";
    }

    @Test
    public void testVocabulary() {
        assertEquals(namespace() + "dateTime", XSD.dateTime.getIRIString());
        assertEquals(namespace() + "string", XSD.string_.getIRIString());
        assertEquals(dateTime.getURI(), XSD.dateTime.getIRIString());
        assertEquals(xstring.getURI(), XSD.string_.getIRIString());
    }

    @Test
    public void checkUri() {
        assertEquals(namespace(), XSD.URI);
        assertEquals(NS, XSD.URI);
    }
}
