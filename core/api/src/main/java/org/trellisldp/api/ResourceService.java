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
package org.trellisldp.api;

import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;

/**
 * The ResourceService provides methods for creating, retrieving and manipulating Trellis resources.
 *
 * @implSpec Implementations should take care to provide any initialization needed, in a constructor or methods
 *           managed by an external framework (e.g. using {@code PostConstruct}). This may or may not include
 *           actions in external systems like databases, which may be better managed elsewhere.
 */
public interface ResourceService extends RetrievalService<Resource> {

    /**
     * Create a resource in the server.
     *
     * @implSpec the default implementation of this method is to proxy create requests to the {@link #replace} method.
     * @param metadata metadata for the new resource
     * @param dataset the dataset to be persisted
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data were
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    default CompletionStage<Void> create(Metadata metadata, Dataset dataset) {
        return replace(metadata, dataset);
    }

    /**
     * Replace a resource in the server.
     *
     * @param metadata metadata for the resource
     * @param dataset the dataset to be persisted
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data
     * were successfully stored in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    CompletionStage<Void> replace(Metadata metadata, Dataset dataset);

    /**
     * Delete a resource from the server.
     *
     * @param metadata metadata for the resource
     * @return a new completion stage that, when the stage completes normally, indicates that the resource
     * was successfully deleted from the corresponding persistence layer. In the case of an unsuccessful delete
     * operation, the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    CompletionStage<Void> delete(Metadata metadata);

    /**
     * @param identifier the identifier under which to persist a dataset
     * @param dataset a dataset to persist
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data
     * were successfully stored in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    CompletionStage<Void> add(IRI identifier, Dataset dataset);

    /**
     * Skolemize a blank node.
     *
     * @param term the RDF term
     * @return a skolemized node, if a blank node; otherwise the original term
     */
    default RDFTerm skolemize(final RDFTerm term) {
        if (term instanceof BlankNode) {
            return RDFFactory.getInstance().createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) term).uniqueReference());
        }
        return term;
    }

    /**
     * Un-skolemize a blank node.
     *
     * @param term the RDF term
     * @return a blank node, if a previously-skolemized node; otherwise the original term
     */
    default RDFTerm unskolemize(final RDFTerm term) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(TRELLIS_BNODE_PREFIX)) {
                return RDFFactory.getInstance().createBlankNode(iri.substring(TRELLIS_BNODE_PREFIX.length()));
            }
        }
        return term;
    }

    /**
     * Return an "internal" representation of an RDF term.
     *
     * @param <T> the type of RDF term
     * @param term the RDF term
     * @param baseUrl the base URL of the domain
     * @return the "internal" RDF term
     */
    default <T extends RDFTerm> T toInternal(final T term, final String baseUrl) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(baseUrl)) {
                @SuppressWarnings("unchecked")
                final T t = (T) RDFFactory.getInstance()
                                    .createIRI(TRELLIS_DATA_PREFIX + iri.substring(baseUrl.length()));
                return t;
            }
        }
        return term;
    }

    /**
     * Return an "external" representation of an RDF term.
     *
     * @param <T> the type of RDF term
     * @param term the RDF term
     * @param baseUrl the base URL of the domain
     * @return the "external" RDF term
     */
    default <T extends RDFTerm> T toExternal(final T term, final String baseUrl) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(TRELLIS_DATA_PREFIX)) {
                @SuppressWarnings("unchecked")
                final T t = (T) RDFFactory.getInstance()
                                    .createIRI(baseUrl + iri.substring(TRELLIS_DATA_PREFIX.length()));
                return t;
            }
        }
        return term;
    }

    /**
     * Update the modification date of the provided resource.
     *
     * @apiNote In general, when this method is called, it can be expected that the target resource
     *          will already exist. In cases where that resource does not already exist, an implementation
     *          may choose to throw an exception, log an error or simply do nothing.
     * @param identifier the identifier of the resource
     * @return a new completion stage that, when the stage completes normally, indicates that the
     *         identified resource has been updated with a new modification date.
     */
    CompletionStage<Void> touch(IRI identifier);

    /**
     * Return a collection of interaction models supported by this Resource Service.
     *
     * @return a set of supported interaction models
     */
    Set<IRI> supportedInteractionModels();

    /**
     * An identifier generator.
     *
     * @return a new identifier
     */
    String generateIdentifier();
}
