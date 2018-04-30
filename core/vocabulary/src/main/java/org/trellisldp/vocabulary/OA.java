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
 * RDF Terms from the OA Vocabulary.
 *
 * @see <a href="https://www.w3.org/ns/oa">OA Vocabulary</a>
 *
 * @author acoburn
 */
public final class OA {

    /* Namespace */
    public static final String NS = "http://www.w3.org/ns/oa#";

    /* Classes */
    public static final IRI Annotation = createIRI(NS + "Annotation");
    public static final IRI Choice = createIRI(NS + "Choice");
    public static final IRI CssSelector = createIRI(NS + "CssSelector");
    public static final IRI CssStyle = createIRI(NS + "CssStyle");
    public static final IRI DataPositionSelector = createIRI(NS + "DataPositionSelector");
    public static final IRI Direction = createIRI(NS + "Direction");
    public static final IRI FragmentSelector = createIRI(NS + "FragmentSelector");
    public static final IRI HttpRequestState = createIRI(NS + "HttpRequestState");
    public static final IRI Motivation = createIRI(NS + "Motivation");
    public static final IRI RangeSelector = createIRI(NS + "RangeSelector");
    public static final IRI ResourceSelection = createIRI(NS + "ResourceSelection");
    public static final IRI Selector = createIRI(NS + "Selector");
    public static final IRI SpecificResource = createIRI(NS + "SpecificResource");
    public static final IRI State = createIRI(NS + "State");
    public static final IRI Style = createIRI(NS + "Style");
    public static final IRI SvgSelector = createIRI(NS + "SvgSelector");
    public static final IRI TextPositionSelector = createIRI(NS + "TextPositionSelector");
    public static final IRI TextQuoteSelector = createIRI(NS + "TextQuoteSelector");
    public static final IRI TextualBody = createIRI(NS + "TextualBody");
    public static final IRI TimeState = createIRI(NS + "TimeState");
    public static final IRI XPathSelector = createIRI(NS + "XPathSelector");

    /* Properties */
    public static final IRI annotationService = createIRI(NS + "annotationService");
    public static final IRI bodyValue = createIRI(NS + "bodyValue");
    public static final IRI cachedSource = createIRI(NS + "cachedSource");
    public static final IRI canonical = createIRI(NS + "canonical");
    public static final IRI end = createIRI(NS + "end");
    public static final IRI exact = createIRI(NS + "exact");
    public static final IRI hasBody = createIRI(NS + "hasBody");
    public static final IRI hasEndSelector = createIRI(NS + "hasEndSelector");
    public static final IRI hasPurpose = createIRI(NS + "hasPurpose");
    public static final IRI hasScope = createIRI(NS + "hasScope");
    public static final IRI hasSelector = createIRI(NS + "hasSelector");
    public static final IRI hasSource = createIRI(NS + "hasSource");
    public static final IRI hasStartSelector = createIRI(NS + "hasStartSelector");
    public static final IRI hasState = createIRI(NS + "hasState");
    public static final IRI hasTarget = createIRI(NS + "hasTarget");
    public static final IRI motivatedBy = createIRI(NS + "motivatedBy");
    public static final IRI prefix = createIRI(NS + "prefix");
    public static final IRI processingLanguage = createIRI(NS + "processingLanguage");
    public static final IRI refinedBy = createIRI(NS + "refinedBy");
    public static final IRI renderedVia = createIRI(NS + "renderedVia");
    public static final IRI sourceDate = createIRI(NS + "sourceDate");
    public static final IRI sourceDateEnd = createIRI(NS + "sourceDateEnd");
    public static final IRI sourceDateStart = createIRI(NS + "sourceDateStart");
    public static final IRI start = createIRI(NS + "start");
    public static final IRI styleClass = createIRI(NS + "styleClass");
    public static final IRI styledBy = createIRI(NS + "styledBy");
    public static final IRI suffix = createIRI(NS + "suffix");
    public static final IRI textDirection = createIRI(NS + "textDirection");
    public static final IRI via = createIRI(NS + "via");

    /* Named Individuals */
    public static final IRI assessing = createIRI(NS + "assessing");
    public static final IRI bookmarking = createIRI(NS + "bookmarking");
    public static final IRI classifying = createIRI(NS + "classifying");
    public static final IRI commenting = createIRI(NS + "commenting");
    public static final IRI describing = createIRI(NS + "describing");
    public static final IRI editing = createIRI(NS + "editing");
    public static final IRI highlighting = createIRI(NS + "highlighting");
    public static final IRI identifying = createIRI(NS + "identifying");
    public static final IRI linking = createIRI(NS + "linking");
    public static final IRI ltrDirection = createIRI(NS + "ltrDirection");
    public static final IRI moderating = createIRI(NS + "moderating");
    public static final IRI questioning = createIRI(NS + "questioning");
    public static final IRI replying = createIRI(NS + "replying");
    public static final IRI rtlDirection = createIRI(NS + "rtlDirection");
    public static final IRI tagging = createIRI(NS + "tagging");

    /* Prefer-related Classes */
    public static final IRI PreferContainedDescriptions = createIRI(NS + "PreferContainedDescriptions");
    public static final IRI PreferContainedIRIs = createIRI(NS + "PreferContainedIRIs");

    private OA() {
        // prevent instantiation
    }
}
