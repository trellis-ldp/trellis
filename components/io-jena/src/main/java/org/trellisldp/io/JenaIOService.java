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
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.RDFFormat.JSONLD_COMPACT_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_EXPAND_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_FLATTEN_FLAT;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.apache.jena.update.UpdateAction.execute;
import static org.apache.jena.update.UpdateFactory.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.JSONLD.compacted_flattened;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.JSONLD.expanded_flattened;
import static org.trellisldp.vocabulary.JSONLD.flattened;
import static org.trellisldp.vocabulary.JSONLD.getNamespace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

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
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * An IOService implemented using Jena.
 *
 * @author acoburn
 */
public class JenaIOService implements IOService {

    public static final String IO_JSONLD_PROFILES = "trellis.io.jsonld.profiles";
    public static final String IO_JSONLD_DOMAINS = "trellis.io.jsonld.domains";

    private static final Logger LOGGER = getLogger(JenaIOService.class);

    private static final JenaRDF rdf = new JenaRDF();

    private static final Map<IRI, RDFFormat> JSONLD_FORMATS = unmodifiableMap(Stream.of(
                new SimpleEntry<>(compacted, JSONLD_COMPACT_FLAT),
                new SimpleEntry<>(flattened, JSONLD_FLATTEN_FLAT),
                new SimpleEntry<>(expanded, JSONLD_EXPAND_FLAT),
                new SimpleEntry<>(compacted_flattened, JSONLD_FLATTEN_FLAT),
                new SimpleEntry<>(expanded_flattened, JSONLD_FLATTEN_FLAT))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    private final NamespaceService nsService;
    private final CacheService<String, String> cache;
    private final RDFaWriterService htmlSerializer;
    private final Set<String> whitelist;
    private final Set<String> whitelistDomains;

    private final List<RDFSyntax> readable;
    private final List<RDFSyntax> writable;
    private final List<RDFSyntax> updatable;

    /**
     * Create a serialization service.
     */
    public JenaIOService() {
        this(null);
    }

    /**
     * Create a serialization service.
     * @param namespaceService the namespace service
     */
    public JenaIOService(final NamespaceService namespaceService) {
        this(namespaceService, null);
    }

    /**
     * Create a serialization service.
     *
     * @param namespaceService the namespace service
     * @param htmlSerializer the HTML serializer service
     */
    public JenaIOService(final NamespaceService namespaceService, final RDFaWriterService htmlSerializer) {
        this(namespaceService, htmlSerializer, null);
    }

    /**
     * Create a serialization service.
     *
     * @param namespaceService the namespace service
     * @param htmlSerializer the HTML serializer service
     * @param cache a cache for custom JSON-LD profile resolution
     */
    @Inject
    public JenaIOService(final NamespaceService namespaceService,
            final RDFaWriterService htmlSerializer,
            @Named("TrellisProfileCache") final CacheService<String, String> cache) {
        this(namespaceService, htmlSerializer, cache,
                ConfigurationProvider.getConfiguration().getOrDefault(IO_JSONLD_PROFILES, ""),
                ConfigurationProvider.getConfiguration().getOrDefault(IO_JSONLD_DOMAINS, ""));
    }

    /**
     * Create a serialization service.
     *
     * @param namespaceService the namespace service
     * @param htmlSerializer the HTML serializer service
     * @param cache a cache for custom JSON-LD profile resolution
     * @param whitelist a whitelist of JSON-LD profiles
     * @param whitelistDomains a whitelist of JSON-LD profile domains
     */
    public JenaIOService(final NamespaceService namespaceService, final RDFaWriterService htmlSerializer,
            final CacheService<String, String> cache, final String whitelist, final String whitelistDomains) {
        this(namespaceService, htmlSerializer, cache, intoSet(whitelist), intoSet(whitelistDomains));
    }


    /**
     * Create a serialization service.
     *
     * @param namespaceService the namespace service
     * @param htmlSerializer the HTML serializer service
     * @param cache a cache for custom JSON-LD profile resolution
     * @param whitelist a whitelist of JSON-LD profiles
     * @param whitelistDomains a whitelist of JSON-LD profile domains
     */
    public JenaIOService(final NamespaceService namespaceService, final RDFaWriterService htmlSerializer,
            final CacheService<String, String> cache, final Set<String> whitelist, final Set<String> whitelistDomains) {
        this.nsService = namespaceService;
        this.htmlSerializer = htmlSerializer;
        this.cache = cache;
        this.whitelist = whitelist;
        this.whitelistDomains = whitelistDomains;

        final List<RDFSyntax> reads = new ArrayList<>(asList(TURTLE, RDFSyntax.JSONLD, NTRIPLES));
        if (nonNull(htmlSerializer)) {
            reads.add(RDFA);
        }
        this.readable = unmodifiableList(reads);
        this.updatable = unmodifiableList(asList(SPARQL_UPDATE));
        this.writable = unmodifiableList(asList(TURTLE, RDFSyntax.JSONLD, NTRIPLES));
    }

    @Override
    public List<RDFSyntax> supportedReadSyntaxes() {
        return readable;
    }

    @Override
    public List<RDFSyntax> supportedWriteSyntaxes() {
        return writable;
    }

    @Override
    public List<RDFSyntax> supportedUpdateSyntaxes() {
        return updatable;
    }

    @Override
    public void write(final Stream<? extends Triple> triples, final OutputStream output, final RDFSyntax syntax,
            final IRI... profiles) {
        requireNonNull(triples, "The triples stream may not be null!");
        requireNonNull(output, "The output stream may not be null!");
        requireNonNull(syntax, "The RDF syntax value may not be null!");

        try {
            if (RDFA.equals(syntax)) {
                writeHTML(triples, output, profiles.length > 0 ? profiles[0].getIRIString() : null);
            } else {
                final Lang lang = rdf.asJenaLang(syntax).orElseThrow(() ->
                        new RuntimeTrellisException("Invalid content type: " + syntax.mediaType()));

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
            throw new RuntimeTrellisException(ex);
        }
    }

    private void writeHTML(final Stream<? extends Triple> triples, final OutputStream output, final String subject) {
        if (nonNull(htmlSerializer)) {
            htmlSerializer.write(triples, output, subject);
        } else {
            write(triples, output, TURTLE);
        }
    }

    private Boolean canUseCustomJsonLdProfile(final String profile) {
        return nonNull(profile) && nonNull(cache);
    }

    private void writeJsonLd(final OutputStream output, final DatasetGraph graph, final IRI... profiles) {
        final String profile = getCustomJsonLdProfile(profiles);
        final RDFFormat format = canUseCustomJsonLdProfile(profile) ? JSONLD_COMPACT_FLAT : getJsonLdProfile(profiles);
        final WriterDatasetRIOT writer = RDFDataMgr.createDatasetWriter(format);
        final PrefixMap pm = RiotLib.prefixMap(graph);
        final String base = null;
        final JsonLDWriteContext ctx = new JsonLDWriteContext();
        if (canUseCustomJsonLdProfile(profile)) {
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
            if (!profile.startsWith(getNamespace())) {
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
    public Stream<? extends Triple> read(final InputStream input, final RDFSyntax syntax, final String base) {
        requireNonNull(input, "The input stream may not be null!");
        requireNonNull(syntax, "The syntax value may not be null!");

        try {
            final org.apache.jena.graph.Graph graph = createDefaultGraph();
            final Lang lang = rdf.asJenaLang(syntax).orElseThrow(() ->
                    new RuntimeTrellisException("Unsupported RDF Syntax: " + syntax.mediaType()));

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
            throw new RuntimeTrellisException(ex);
        }
    }

    @Override
    public void update(final Graph graph, final String update, final RDFSyntax syntax, final String base) {
        requireNonNull(graph, "The input graph may not be null");
        requireNonNull(update, "The update command may not be null");
        requireNonNull(syntax, "The RDF syntax may not be null");
        if (!SPARQL_UPDATE.equals(syntax)) {
            throw new RuntimeTrellisException("The syntax " + syntax + " is not supported for updates.");
        }

        try {
            final org.apache.jena.graph.Graph g = rdf.asJenaGraph(graph);
            execute(create(update, base), g);
        } catch (final UpdateException | QueryParseException ex) {
            throw new RuntimeTrellisException(ex);
        }
    }

    private static Set<String> intoSet(final String property) {
        return stream(property.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(toSet());
    }

    private static IRI mergeProfiles(final IRI... profiles) {
        Boolean isExpanded = true;
        Boolean isFlattened = false;

        for (final IRI uri : profiles) {
            if (compacted_flattened.equals(uri) || expanded_flattened.equals(uri)) {
                return uri;
            }

            if (flattened.equals(uri)) {
                isFlattened = true;
            } else if (compacted.equals(uri)) {
                isExpanded = false;
            } else if (expanded.equals(uri)) {
                isExpanded = true;
            }
        }
        if (isFlattened) {
            return isExpanded ? expanded_flattened : compacted_flattened;
        }
        return isExpanded ? expanded : compacted;
    }

    private static RDFFormat getJsonLdProfile(final IRI... profiles) {
        return of(mergeProfiles(profiles)).map(JSONLD_FORMATS::get).orElse(JSONLD_EXPAND_FLAT);
    }
}
