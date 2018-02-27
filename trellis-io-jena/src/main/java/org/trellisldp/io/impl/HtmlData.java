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
package org.trellisldp.io.impl;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
public class HtmlData {

    private static final Set<IRI> titleCandidates = new HashSet<>(asList(SKOS.prefLabel, RDFS.label, DC.title));

    private final List<Triple> triples;
    private final IRI subject;
    private final NamespaceService namespaceService;
    private final Map<String, String> properties;

    /**
     * Create an HTML Data object.
     *
     * @param namespaceService the namespace service
     * @param subject the subject
     * @param triples the triples
     * @param properties additional properties for static resources
     */
    public HtmlData(final NamespaceService namespaceService, final IRI subject, final List<Triple> triples,
            final Map<String, String> properties) {
        this.namespaceService = namespaceService;
        this.subject = subject;
        this.triples = triples;
        this.properties = properties;
    }

    /**
     * Get the triples.
     *
     * @return the labelled triples
     */
    public List<LabelledTriple> getTriples() {
        return triples.stream().map(labelTriple)
            .sorted(sortSubjects.thenComparing(sortPredicates).thenComparing(sortObjects)).collect(toList());
    }

    /**
     * Get any CSS document URLs.
     *
     * @return a list of any CSS documents
     */
    public List<String> getCss() {
        return stream(properties.getOrDefault("css", "").split(","))
            .map(String::trim).filter(x -> x.length() > 0).collect(toList());
    }

    /**
     * Get a Icon URL.
     *
     * @return the location of an icon, if one exists
     */
    public String getIcon() {
        return properties.get("icon");
    }

    /**
     * Get a list of javascript document URLs.
     *
     * @return a list of JS documents
     */
    public List<String> getJs() {
        return stream(properties.getOrDefault("js", "").split(","))
            .map(String::trim).filter(x -> x.length() > 0).collect(toList());
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
        return ofNullable(subject).map(IRI::getIRIString).orElse("");
    }

    private Function<Triple, LabelledTriple> labelTriple = triple -> {
        final String pred = triple.getPredicate().getIRIString();
        if (triple.getObject() instanceof IRI) {
            return new LabelledTriple(triple, getLabel(pred), getLabel(((IRI) triple.getObject()).getIRIString()));
        } else if (triple.getObject() instanceof Literal) {
            return new LabelledTriple(triple, getLabel(pred), ((Literal) triple.getObject()).getLexicalForm());
        }
        return new LabelledTriple(triple, getLabel(pred), triple.getObject().ntriplesString());
    };

    private String getLabel(final String iri) {
        final int lastHash = iri.lastIndexOf('#');
        String namespace = null;
        final String qname;
        if (lastHash != -1) {
            namespace = iri.substring(0, lastHash + 1);
            qname = iri.substring(lastHash + 1);
        } else {
            final int lastSlash = iri.lastIndexOf('/');
            if (lastSlash != -1) {
                namespace = iri.substring(0, lastSlash + 1);
                qname = iri.substring(lastSlash + 1);
            } else {
                qname = "";
            }
        }
        if (nonNull(namespaceService)) {
            return ofNullable(namespace).flatMap(namespaceService::getPrefix).map(pre -> pre + ":" + qname)
                .orElse(iri);
        }
        return iri;
    }

    private static final Comparator<LabelledTriple> sortSubjects = (q1, q2) ->
        q1.getSubject().compareTo(q2.getSubject());

    private static final Comparator<LabelledTriple> sortPredicates = (q1, q2) ->
        q1.getPredicate().compareTo(q2.getPredicate());

    private static final Comparator<LabelledTriple> sortObjects = (q1, q2) ->
        q1.getObject().compareTo(q2.getObject());
}
