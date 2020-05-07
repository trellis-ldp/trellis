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
    private static final String URI = "http://www.w3.org/2001/XMLSchema#";

    /* DataTypes */
    public static final IRI anyURI = createIRI(getNamespace() + "anyURI");
    public static final IRI base64Binary = createIRI(getNamespace() + "base64Binary");
    public static final IRI boolean_ = createIRI(getNamespace() + "boolean");
    public static final IRI byte_ = createIRI(getNamespace() + "byte");
    public static final IRI date = createIRI(getNamespace() + "date");
    public static final IRI dateTime = createIRI(getNamespace() + "dateTime");
    public static final IRI dateTimeStamp = createIRI(getNamespace() + "dateTimeStamp");
    public static final IRI dayTimeDuration = createIRI(getNamespace() + "dayTimeDuration");
    public static final IRI decimal = createIRI(getNamespace() + "decimal");
    public static final IRI double_ = createIRI(getNamespace() + "double");
    public static final IRI duration = createIRI(getNamespace() + "duration");
    public static final IRI float_ = createIRI(getNamespace() + "float");
    public static final IRI gDay = createIRI(getNamespace() + "gDay");
    public static final IRI gMonth = createIRI(getNamespace() + "gMonth");
    public static final IRI gMonthDay = createIRI(getNamespace() + "gMonthDay");
    public static final IRI gYear = createIRI(getNamespace() + "gYear");
    public static final IRI gYearMonth = createIRI(getNamespace() + "gYearMonth");
    public static final IRI hexBinary = createIRI(getNamespace() + "hexBinary");
    public static final IRI integer = createIRI(getNamespace() + "integer");
    public static final IRI int_ = createIRI(getNamespace() + "int");
    public static final IRI language = createIRI(getNamespace() + "language");
    public static final IRI long_ = createIRI(getNamespace() + "long");
    public static final IRI negativeInteger = createIRI(getNamespace() + "negativeInteger");
    public static final IRI nonNegativeInteger = createIRI(getNamespace() + "nonNegativeInteger");
    public static final IRI nonPositiveInteger = createIRI(getNamespace() + "nonPositiveInteger");
    public static final IRI normalizedString = createIRI(getNamespace() + "normalizedString");
    public static final IRI positiveInteger = createIRI(getNamespace() + "positiveInteger");
    public static final IRI short_ = createIRI(getNamespace() + "short");
    public static final IRI string_ = createIRI(getNamespace() + "string");
    public static final IRI time = createIRI(getNamespace() + "time");
    public static final IRI token = createIRI(getNamespace() + "token");
    public static final IRI unsignedByte = createIRI(getNamespace() + "unsignedByte");
    public static final IRI unsignedInt = createIRI(getNamespace() + "unsignedInt");
    public static final IRI unsignedLong = createIRI(getNamespace() + "unsignedLong");
    public static final IRI unsignedShort = createIRI(getNamespace() + "unsignedShort");
    public static final IRI yearMonthDuration = createIRI(getNamespace() + "yearMonthDuration");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private XSD() {
        // prevent instantiation
    }
}
