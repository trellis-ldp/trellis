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

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the XML Schema Datatype Vocabulary
 *
 * @see <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema Part 2</a>
 *
 * @author acoburn
 */
public final class XSD extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/2001/XMLSchema#";

    /* DataTypes */
    public static final IRI anyURI = createIRI(URI + "anyURI");
    public static final IRI base64Binary = createIRI(URI + "base64Binary");
    public static final IRI boolean_ = createIRI(URI + "boolean");
    public static final IRI byte_ = createIRI(URI + "byte");
    public static final IRI date = createIRI(URI + "date");
    public static final IRI dateTime = createIRI(URI + "dateTime");
    public static final IRI dateTimeStamp = createIRI(URI + "dateTimeStamp");
    public static final IRI dayTimeDuration = createIRI(URI + "dayTimeDuration");
    public static final IRI decimal = createIRI(URI + "decimal");
    public static final IRI double_ = createIRI(URI + "double");
    public static final IRI duration = createIRI(URI + "duration");
    public static final IRI float_ = createIRI(URI + "float");
    public static final IRI gDay = createIRI(URI + "gDay");
    public static final IRI gMonth = createIRI(URI + "gMonth");
    public static final IRI gMonthDay = createIRI(URI + "gMonthDay");
    public static final IRI gYear = createIRI(URI + "gYear");
    public static final IRI gYearMonth = createIRI(URI + "gYearMonth");
    public static final IRI hexBinary = createIRI(URI + "hexBinary");
    public static final IRI integer = createIRI(URI + "integer");
    public static final IRI int_ = createIRI(URI + "int");
    public static final IRI language = createIRI(URI + "language");
    public static final IRI long_ = createIRI(URI + "long");
    public static final IRI negativeInteger = createIRI(URI + "negativeInteger");
    public static final IRI nonNegativeInteger = createIRI(URI + "nonNegativeInteger");
    public static final IRI nonPositiveInteger = createIRI(URI + "nonPositiveInteger");
    public static final IRI normalizedString = createIRI(URI + "normalizedString");
    public static final IRI positiveInteger = createIRI(URI + "positiveInteger");
    public static final IRI short_ = createIRI(URI + "short");
    public static final IRI string_ = createIRI(URI + "string");
    public static final IRI time = createIRI(URI + "time");
    public static final IRI token = createIRI(URI + "token");
    public static final IRI unsignedByte = createIRI(URI + "unsignedByte");
    public static final IRI unsignedInt = createIRI(URI + "unsignedInt");
    public static final IRI unsignedLong = createIRI(URI + "unsignedLong");
    public static final IRI unsignedShort = createIRI(URI + "unsignedShort");
    public static final IRI yearMonthDuration = createIRI(URI + "yearMonthDuration");

    private XSD() {
        super();
    }
}
