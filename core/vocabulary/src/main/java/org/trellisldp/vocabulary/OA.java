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
 * RDF Terms from the OA Vocabulary.
 *
 * @see <a href="https://www.w3.org/ns/oa">OA Vocabulary</a>
 *
 * @author acoburn
 */
public final class OA {

    /* Namespace */
    private static final String URI = "http://www.w3.org/ns/oa#";

    /* Classes */
    public static final IRI Annotation = createIRI(getNamespace() + "Annotation");
    public static final IRI Choice = createIRI(getNamespace() + "Choice");
    public static final IRI CssSelector = createIRI(getNamespace() + "CssSelector");
    public static final IRI CssStyle = createIRI(getNamespace() + "CssStyle");
    public static final IRI DataPositionSelector = createIRI(getNamespace() + "DataPositionSelector");
    public static final IRI Direction = createIRI(getNamespace() + "Direction");
    public static final IRI FragmentSelector = createIRI(getNamespace() + "FragmentSelector");
    public static final IRI HttpRequestState = createIRI(getNamespace() + "HttpRequestState");
    public static final IRI Motivation = createIRI(getNamespace() + "Motivation");
    public static final IRI RangeSelector = createIRI(getNamespace() + "RangeSelector");
    public static final IRI ResourceSelection = createIRI(getNamespace() + "ResourceSelection");
    public static final IRI Selector = createIRI(getNamespace() + "Selector");
    public static final IRI SpecificResource = createIRI(getNamespace() + "SpecificResource");
    public static final IRI State = createIRI(getNamespace() + "State");
    public static final IRI Style = createIRI(getNamespace() + "Style");
    public static final IRI SvgSelector = createIRI(getNamespace() + "SvgSelector");
    public static final IRI TextPositionSelector = createIRI(getNamespace() + "TextPositionSelector");
    public static final IRI TextQuoteSelector = createIRI(getNamespace() + "TextQuoteSelector");
    public static final IRI TextualBody = createIRI(getNamespace() + "TextualBody");
    public static final IRI TimeState = createIRI(getNamespace() + "TimeState");
    public static final IRI XPathSelector = createIRI(getNamespace() + "XPathSelector");

    /* Properties */
    public static final IRI annotationService = createIRI(getNamespace() + "annotationService");
    public static final IRI bodyValue = createIRI(getNamespace() + "bodyValue");
    public static final IRI cachedSource = createIRI(getNamespace() + "cachedSource");
    public static final IRI canonical = createIRI(getNamespace() + "canonical");
    public static final IRI end = createIRI(getNamespace() + "end");
    public static final IRI exact = createIRI(getNamespace() + "exact");
    public static final IRI hasBody = createIRI(getNamespace() + "hasBody");
    public static final IRI hasEndSelector = createIRI(getNamespace() + "hasEndSelector");
    public static final IRI hasPurpose = createIRI(getNamespace() + "hasPurpose");
    public static final IRI hasScope = createIRI(getNamespace() + "hasScope");
    public static final IRI hasSelector = createIRI(getNamespace() + "hasSelector");
    public static final IRI hasSource = createIRI(getNamespace() + "hasSource");
    public static final IRI hasStartSelector = createIRI(getNamespace() + "hasStartSelector");
    public static final IRI hasState = createIRI(getNamespace() + "hasState");
    public static final IRI hasTarget = createIRI(getNamespace() + "hasTarget");
    public static final IRI motivatedBy = createIRI(getNamespace() + "motivatedBy");
    public static final IRI prefix = createIRI(getNamespace() + "prefix");
    public static final IRI processingLanguage = createIRI(getNamespace() + "processingLanguage");
    public static final IRI refinedBy = createIRI(getNamespace() + "refinedBy");
    public static final IRI renderedVia = createIRI(getNamespace() + "renderedVia");
    public static final IRI sourceDate = createIRI(getNamespace() + "sourceDate");
    public static final IRI sourceDateEnd = createIRI(getNamespace() + "sourceDateEnd");
    public static final IRI sourceDateStart = createIRI(getNamespace() + "sourceDateStart");
    public static final IRI start = createIRI(getNamespace() + "start");
    public static final IRI styleClass = createIRI(getNamespace() + "styleClass");
    public static final IRI styledBy = createIRI(getNamespace() + "styledBy");
    public static final IRI suffix = createIRI(getNamespace() + "suffix");
    public static final IRI textDirection = createIRI(getNamespace() + "textDirection");
    public static final IRI via = createIRI(getNamespace() + "via");

    /* Named Individuals */
    public static final IRI assessing = createIRI(getNamespace() + "assessing");
    public static final IRI bookmarking = createIRI(getNamespace() + "bookmarking");
    public static final IRI classifying = createIRI(getNamespace() + "classifying");
    public static final IRI commenting = createIRI(getNamespace() + "commenting");
    public static final IRI describing = createIRI(getNamespace() + "describing");
    public static final IRI editing = createIRI(getNamespace() + "editing");
    public static final IRI highlighting = createIRI(getNamespace() + "highlighting");
    public static final IRI identifying = createIRI(getNamespace() + "identifying");
    public static final IRI linking = createIRI(getNamespace() + "linking");
    public static final IRI ltrDirection = createIRI(getNamespace() + "ltrDirection");
    public static final IRI moderating = createIRI(getNamespace() + "moderating");
    public static final IRI questioning = createIRI(getNamespace() + "questioning");
    public static final IRI replying = createIRI(getNamespace() + "replying");
    public static final IRI rtlDirection = createIRI(getNamespace() + "rtlDirection");
    public static final IRI tagging = createIRI(getNamespace() + "tagging");

    /* Prefer-related Classes */
    public static final IRI PreferContainedDescriptions = createIRI(getNamespace() + "PreferContainedDescriptions");
    public static final IRI PreferContainedIRIs = createIRI(getNamespace() + "PreferContainedIRIs");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private OA() {
        // prevent instantiation
    }
}
