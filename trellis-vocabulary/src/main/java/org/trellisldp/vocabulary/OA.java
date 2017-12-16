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
 * RDF Terms from the OA Vocabulary
 *
 * @see <a href="https://www.w3.org/ns/oa">OA Vocabulary</a>
 *
 * @author acoburn
 */
public final class OA extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/ns/oa#";

    /* Classes */
    public static final IRI Annotation = createIRI(URI + "Annotation");
    public static final IRI Choice = createIRI(URI + "Choice");
    public static final IRI CssSelector = createIRI(URI + "CssSelector");
    public static final IRI CssStyle = createIRI(URI + "CssStyle");
    public static final IRI DataPositionSelector = createIRI(URI + "DataPositionSelector");
    public static final IRI Direction = createIRI(URI + "Direction");
    public static final IRI FragmentSelector = createIRI(URI + "FragmentSelector");
    public static final IRI HttpRequestState = createIRI(URI + "HttpRequestState");
    public static final IRI Motivation = createIRI(URI + "Motivation");
    public static final IRI RangeSelector = createIRI(URI + "RangeSelector");
    public static final IRI ResourceSelection = createIRI(URI + "ResourceSelection");
    public static final IRI Selector = createIRI(URI + "Selector");
    public static final IRI SpecificResource = createIRI(URI + "SpecificResource");
    public static final IRI State = createIRI(URI + "State");
    public static final IRI Style = createIRI(URI + "Style");
    public static final IRI SvgSelector = createIRI(URI + "SvgSelector");
    public static final IRI TextPositionSelector = createIRI(URI + "TextPositionSelector");
    public static final IRI TextQuoteSelector = createIRI(URI + "TextQuoteSelector");
    public static final IRI TextualBody = createIRI(URI + "TextualBody");
    public static final IRI TimeState = createIRI(URI + "TimeState");
    public static final IRI XPathSelector = createIRI(URI + "XPathSelector");

    /* Properties */
    public static final IRI annotationService = createIRI(URI + "annotationService");
    public static final IRI bodyValue = createIRI(URI + "bodyValue");
    public static final IRI cachedSource = createIRI(URI + "cachedSource");
    public static final IRI canonical = createIRI(URI + "canonical");
    public static final IRI end = createIRI(URI + "end");
    public static final IRI exact = createIRI(URI + "exact");
    public static final IRI hasBody = createIRI(URI + "hasBody");
    public static final IRI hasEndSelector = createIRI(URI + "hasEndSelector");
    public static final IRI hasPurpose = createIRI(URI + "hasPurpose");
    public static final IRI hasScope = createIRI(URI + "hasScope");
    public static final IRI hasSelector = createIRI(URI + "hasSelector");
    public static final IRI hasSource = createIRI(URI + "hasSource");
    public static final IRI hasStartSelector = createIRI(URI + "hasStartSelector");
    public static final IRI hasState = createIRI(URI + "hasState");
    public static final IRI hasTarget = createIRI(URI + "hasTarget");
    public static final IRI motivatedBy = createIRI(URI + "motivatedBy");
    public static final IRI prefix = createIRI(URI + "prefix");
    public static final IRI processingLanguage = createIRI(URI + "processingLanguage");
    public static final IRI refinedBy = createIRI(URI + "refinedBy");
    public static final IRI renderedVia = createIRI(URI + "renderedVia");
    public static final IRI sourceDate = createIRI(URI + "sourceDate");
    public static final IRI sourceDateEnd = createIRI(URI + "sourceDateEnd");
    public static final IRI sourceDateStart = createIRI(URI + "sourceDateStart");
    public static final IRI start = createIRI(URI + "start");
    public static final IRI styleClass = createIRI(URI + "styleClass");
    public static final IRI styledBy = createIRI(URI + "styledBy");
    public static final IRI suffix = createIRI(URI + "suffix");
    public static final IRI textDirection = createIRI(URI + "textDirection");
    public static final IRI via = createIRI(URI + "via");

    /* Named Individuals */
    public static final IRI assessing = createIRI(URI + "assessing");
    public static final IRI bookmarking = createIRI(URI + "bookmarking");
    public static final IRI classifying = createIRI(URI + "classifying");
    public static final IRI commenting = createIRI(URI + "commenting");
    public static final IRI describing = createIRI(URI + "describing");
    public static final IRI editing = createIRI(URI + "editing");
    public static final IRI highlighting = createIRI(URI + "highlighting");
    public static final IRI identifying = createIRI(URI + "identifying");
    public static final IRI linking = createIRI(URI + "linking");
    public static final IRI ltrDirection = createIRI(URI + "ltrDirection");
    public static final IRI moderating = createIRI(URI + "moderating");
    public static final IRI questioning = createIRI(URI + "questioning");
    public static final IRI replying = createIRI(URI + "replying");
    public static final IRI rtlDirection = createIRI(URI + "rtlDirection");
    public static final IRI tagging = createIRI(URI + "tagging");

    /* Prefer-related Classes */
    public static final IRI PreferContainedDescriptions = createIRI(URI + "PreferContainedDescriptions");
    public static final IRI PreferContainedIRIs = createIRI(URI + "PreferContainedIRIs");

    private OA() {
        super();
    }
}
