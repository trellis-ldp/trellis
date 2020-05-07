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
package org.trellisldp.rdfa;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
class HtmlData {

    private static final Set<IRI> titleCandidates = new HashSet<>(asList(SKOS.prefLabel, RDFS.label, DC.title));

    private static final Comparator<LabelledTriple> sortSubjects = comparing(LabelledTriple::getSubject);

    private static final Comparator<LabelledTriple> sortPredicates = comparing(LabelledTriple::getPredicate);

    private static final Comparator<LabelledTriple> sortObjects = comparing(LabelledTriple::getObject);

    private final List<Triple> triples;
    private final String subject;
    private final PrefixMapping prefixMapping = PrefixMapping.Factory.create();
    private final List<String> css;
    private final List<String> js;
    private final String icon;

    /**
     * Create an HTML Data object.
     *
     * @param namespaceService the namespace service
     * @param subject the subject
     * @param triples the triples
     * @param css the stylesheets
     * @param js the javascripts
     * @param icon the icon
     */
    public HtmlData(final NamespaceService namespaceService, final String subject, final List<Triple> triples,
            final List<String> css, final List<String> js, final String icon) {
        this.css = requireNonNull(css, "The CSS list may not be null!");
        this.js = requireNonNull(js, "The JS list may not be null!");
        this.triples = triples;
        this.subject = subject != null ? subject : "";
        this.icon = icon != null ? icon : "//www.trellisldp.org/assets/img/trellis.png";
        this.prefixMapping.setNsPrefixes(namespaceService.getNamespaces());
    }

    /**
     * Get the triples.
     *
     * @return the labelled triples
     */
    public List<LabelledTriple> getTriples() {
        return triples.stream().map(this::labelTriple)
            .sorted(sortSubjects.thenComparing(sortPredicates).thenComparing(sortObjects)).collect(toList());
    }

    /**
     * Get any CSS document URLs.
     *
     * @return a list of any CSS documents
     */
    public List<String> getCss() {
        return css;
    }

    /**
     * Get a Icon URL.
     *
     * @return the location of an icon, if one exists
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get a list of javascript document URLs.
     *
     * @return a list of JS documents
     */
    public List<String> getJs() {
        return js;
    }

    /**
     * Get the title.
     *
     * @return a title for the resource
     */
    public String getTitle() {
        final Map<IRI, List<String>> titles = triples.stream()
            .filter(triple -> titleCandidates.contains(triple.getPredicate()))
            .filter(triple -> triple.getObject() instanceof Literal)
            .collect(groupingBy(Triple::getPredicate, mapping(triple ->
                            ((Literal) triple.getObject()).getLexicalForm(), toList())));
        return titleCandidates.stream().filter(titles::containsKey)
            .map(titles::get).flatMap(List::stream).findFirst()
            .orElseGet(this::getSubject);
    }

    private String getSubject() {
        return subject;
    }

    private String getLabel(final String iri) {
        final String label = prefixMapping.qnameFor(iri);
        if (label != null) {
            return label;
        }
        return iri;
    }

    private LabelledTriple labelTriple(final Triple triple) {
        final String pred = triple.getPredicate().getIRIString();
        if (triple.getObject() instanceof IRI) {
            return new LabelledTriple(triple, getLabel(pred), getLabel(((IRI) triple.getObject()).getIRIString()));
        } else if (triple.getObject() instanceof Literal) {
            return new LabelledTriple(triple, getLabel(pred), ((Literal) triple.getObject()).getLexicalForm());
        }
        return new LabelledTriple(triple, getLabel(pred), triple.getObject().ntriplesString());
    }
}
