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
package org.trellisldp.vocabulary;

import static org.apache.jena.vocabulary.XSD.NS;
import static org.apache.jena.vocabulary.XSD.dateTime;
import static org.apache.jena.vocabulary.XSD.xstring;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * @author acoburn
 */
class XSDTest {

    String namespace() {
        return "http://www.w3.org/2001/XMLSchema#";
    }

    @Test
    void testVocabulary() {
        assertEquals(namespace() + "dateTime", XSD.dateTime.getIRIString(), "xsd:dateTime IRIs don't match");
        assertEquals(namespace() + "string", XSD.string_.getIRIString(), "xsd:string IRIs don't match!");
        assertEquals(dateTime.getURI(), XSD.dateTime.getIRIString(), "xsd:dateTime IRI doesn't match Jena's value!");
        assertEquals(xstring.getURI(), XSD.string_.getIRIString(), "xsd:string IRI doesn't match Jena's value!");
    }

    @Test
    void checkUri() {
        assertEquals(namespace(), XSD.getNamespace(), "Namespace IRIs don't match!");
        assertEquals(NS, XSD.getNamespace(), "Namespace IRI doesn't match Jena's value!");
    }
}
