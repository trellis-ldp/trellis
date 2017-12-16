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
package org.trellisldp.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA_HTML;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.RDFFormat.JSONLD_COMPACT_FLAT;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.apache.jena.update.UpdateAction.execute;
import static org.apache.jena.update.UpdateFactory.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.io.impl.IOUtils.getJsonLdProfile;
import static org.trellisldp.vocabulary.JSONLD.URI;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.update.UpdateException;
import org.slf4j.Logger;

import org.trellisldp.api.CacheService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RuntimeRepositoryException;
import org.trellisldp.io.impl.HtmlSerializer;

/**
 * An IOService implemented using Jena
 *
 * @author acoburn
 */
public class JenaIOService implements IOService {

    private static final Logger LOGGER = getLogger(JenaIOService.class);

    private static final JenaRDF rdf = new JenaRDF();

    private static final Map<String, String> defaultProperties = unmodifiableMap(of(
        new SimpleEntry<>("icon", "//s3.amazonaws.com/www.trellisldp.org/assets/img/trellis.png"),
        new SimpleEntry<>("css", "//s3.amazonaws.com/www.trellisldp.org/assets/css/trellis.css"))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    private final Set<String> whitelist;
    private final Set<String> whitelistDomains;
    private final CacheService<String, String> cache;

    private final NamespaceService nsService;
    private final HtmlSerializer htmlSerializer;

    /**
     * Create a serialization service
     * @param namespaceService the namespace service
     */
    public JenaIOService(final NamespaceService namespaceService) {
        this(namespaceService, defaultProperties);
    }

    /**
     * Create a serialization service
     * @param namespaceService the namespace service
     * @param properties additional properties for the HTML view
     */
    public JenaIOService(final NamespaceService namespaceService, final Map<String, String> properties) {
        this(namespaceService, properties, emptySet(), emptySet(), null);
    }

    /**
     * Create a serialization service
     * @param namespaceService the namespace service
     * @param properties additional properties for the HTML view
     * @param whitelist a whitelist of JSON-LD profiles
     * @param whitelistDomains a whitelist of domains for use with JSON-LD profiles
     * @param cache a cache for custom JSON-LD profile resolution
     */
    public JenaIOService(final NamespaceService namespaceService, final Map<String, String> properties,
            final Set<String> whitelist, final Set<String> whitelistDomains, final CacheService<String, String> cache) {
        this.nsService = namespaceService;
        this.htmlSerializer = new HtmlSerializer(namespaceService,
                properties.getOrDefault("template", "org/trellisldp/io/resource.mustache"), properties);
        this.whitelist = unmodifiableSet(whitelist);
        this.whitelistDomains = unmodifiableSet(whitelistDomains);
        this.cache = cache;
    }

    @Override
    public void write(final Stream<? extends Triple> triples, final OutputStream output, final RDFSyntax syntax,
            final IRI... profiles) {
        requireNonNull(triples, "The triples stream may not be null!");
        requireNonNull(output, "The output stream may not be null!");
        requireNonNull(syntax, "The RDF syntax value may not be null!");

        try {
            if (RDFA_HTML.equals(syntax)) {
                htmlSerializer.write(output, triples, profiles.length > 0 ? profiles[0] : null);
            } else {
                final Lang lang = rdf.asJenaLang(syntax).orElseThrow(() ->
                        new RuntimeRepositoryException("Invalid content type: " + syntax.mediaType));

                final RDFFormat format = defaultSerialization(lang);

                if (nonNull(format)) {
                    LOGGER.debug("Writing stream-based RDF: {}", format);
                    final StreamRDF stream = getWriterStream(output, format);
                    stream.start();
                    ofNullable(nsService).ifPresent(svc -> svc.getNamespaces().forEach(stream::prefix));
                    triples.map(rdf::asJenaTriple).forEachOrdered(stream::triple);
                    stream.finish();
                } else {
                    LOGGER.debug("Writing buffered RDF: {}", lang);
                    final org.apache.jena.graph.Graph graph = createDefaultGraph();
                    ofNullable(nsService).map(NamespaceService::getNamespaces)
                        .ifPresent(graph.getPrefixMapping()::setNsPrefixes);
                    triples.map(rdf::asJenaTriple).forEachOrdered(graph::add);
                    if (JSONLD.equals(lang)) {
                        writeJsonLd(output, DatasetGraphFactory.create(graph), profiles);
                    } else {
                        RDFDataMgr.write(output, graph, lang);
                    }
                }
            }
        } catch (final AtlasException ex) {
            throw new RuntimeRepositoryException(ex);
        }
    }

    private void writeJsonLd(final OutputStream output, final DatasetGraph graph, final IRI... profiles) {
        final String profile = getCustomJsonLdProfile(profiles);
        final RDFFormat format = nonNull(profile) && nonNull(cache) ? JSONLD_COMPACT_FLAT : getJsonLdProfile(profiles);
        final WriterDatasetRIOT writer = RDFDataMgr.createDatasetWriter(format);
        final PrefixMap pm = RiotLib.prefixMap(graph);
        final String base = null;
        final JsonLDWriteContext ctx = new JsonLDWriteContext();
        if (nonNull(profile) && nonNull(cache)) {
            LOGGER.debug("Setting JSON-LD context with profile: {}", profile);
            final String c = cache.get(profile, p -> {
                try (final TypedInputStream res = HttpOp.execHttpGet(profile)) {
                    return IOUtils.toString(res.getInputStream(), UTF_8);
                } catch (final IOException | HttpException ex) {
                    LOGGER.warn("Error fetching profile {}: {}", p, ex.getMessage());
                    return null;
                }
            });
            if (nonNull(c)) {
                ctx.setJsonLDContext(c);
                ctx.setJsonLDContextSubstitution("\"" + profile + "\"");
            }
        }
        writer.write(output, graph, pm, base, ctx);
    }

    private String getCustomJsonLdProfile(final IRI... profiles) {
        for (final IRI p : profiles) {
            final String profile = p.getIRIString();
            if (!profile.startsWith(URI)) {
                if (whitelist.contains(profile)) {
                    return profile;
                }
                for (final String domain : whitelistDomains) {
                    if (profile.startsWith(domain)) {
                        return profile;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Stream<? extends Triple> read(final InputStream input, final String base, final RDFSyntax syntax) {
        requireNonNull(input, "The input stream may not be null!");
        requireNonNull(syntax, "The syntax value may not be null!");

        try {
            final org.apache.jena.graph.Graph graph = createDefaultGraph();
            final Lang lang = rdf.asJenaLang(syntax).orElseThrow(() ->
                    new RuntimeRepositoryException("Unsupported RDF Syntax: " + syntax.mediaType));

            RDFParser.source(input).lang(lang).base(base).parse(graph);

            // Check the graph for any new namespace definitions
            if (nonNull(nsService)) {
                final Set<String> namespaces = nsService.getNamespaces().entrySet().stream().map(Map.Entry::getValue)
                    .collect(toSet());
                graph.getPrefixMapping().getNsPrefixMap().forEach((prefix, namespace) -> {
                    if (!namespaces.contains(namespace)) {
                        LOGGER.debug("Setting prefix ({}) for namespace {}", prefix, namespace);
                        nsService.setPrefix(prefix, namespace);
                    }
                });
            }
            return rdf.asGraph(graph).stream();
        } catch (final RiotException | AtlasException ex) {
            throw new RuntimeRepositoryException(ex);
        }
    }

    @Override
    public void update(final Graph graph, final String update, final String base) {
        requireNonNull(graph, "The input graph may not be null");
        requireNonNull(update, "The update command may not be null");
        try {
            execute(create(update, base), rdf.asJenaGraph(graph));
        } catch (final UpdateException | QueryParseException ex) {
            throw new RuntimeRepositoryException(ex);
        }
    }
}
