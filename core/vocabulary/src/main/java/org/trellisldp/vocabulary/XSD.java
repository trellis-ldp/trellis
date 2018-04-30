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

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the XML Schema Datatype Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema Part 2</a>
 *
 * @author acoburn
 */
public final class XSD {

    /* Namespace */
    public static final String NS = "http://www.w3.org/2001/XMLSchema#";

    /* DataTypes */
    public static final IRI anyNS = createIRI(NS + "anyNS");
    public static final IRI base64Binary = createIRI(NS + "base64Binary");
    public static final IRI boolean_ = createIRI(NS + "boolean");
    public static final IRI byte_ = createIRI(NS + "byte");
    public static final IRI date = createIRI(NS + "date");
    public static final IRI dateTime = createIRI(NS + "dateTime");
    public static final IRI dateTimeStamp = createIRI(NS + "dateTimeStamp");
    public static final IRI dayTimeDuration = createIRI(NS + "dayTimeDuration");
    public static final IRI decimal = createIRI(NS + "decimal");
    public static final IRI double_ = createIRI(NS + "double");
    public static final IRI duration = createIRI(NS + "duration");
    public static final IRI float_ = createIRI(NS + "float");
    public static final IRI gDay = createIRI(NS + "gDay");
    public static final IRI gMonth = createIRI(NS + "gMonth");
    public static final IRI gMonthDay = createIRI(NS + "gMonthDay");
    public static final IRI gYear = createIRI(NS + "gYear");
    public static final IRI gYearMonth = createIRI(NS + "gYearMonth");
    public static final IRI hexBinary = createIRI(NS + "hexBinary");
    public static final IRI integer = createIRI(NS + "integer");
    public static final IRI int_ = createIRI(NS + "int");
    public static final IRI language = createIRI(NS + "language");
    public static final IRI long_ = createIRI(NS + "long");
    public static final IRI negativeInteger = createIRI(NS + "negativeInteger");
    public static final IRI nonNegativeInteger = createIRI(NS + "nonNegativeInteger");
    public static final IRI nonPositiveInteger = createIRI(NS + "nonPositiveInteger");
    public static final IRI normalizedString = createIRI(NS + "normalizedString");
    public static final IRI positiveInteger = createIRI(NS + "positiveInteger");
    public static final IRI short_ = createIRI(NS + "short");
    public static final IRI string_ = createIRI(NS + "string");
    public static final IRI time = createIRI(NS + "time");
    public static final IRI token = createIRI(NS + "token");
    public static final IRI unsignedByte = createIRI(NS + "unsignedByte");
    public static final IRI unsignedInt = createIRI(NS + "unsignedInt");
    public static final IRI unsignedLong = createIRI(NS + "unsignedLong");
    public static final IRI unsignedShort = createIRI(NS + "unsignedShort");
    public static final IRI yearMonthDuration = createIRI(NS + "yearMonthDuration");

    private XSD() {
        // prevent instantiation
    }
}
