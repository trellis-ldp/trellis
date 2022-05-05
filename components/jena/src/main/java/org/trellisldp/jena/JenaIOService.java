/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.jena;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.RDFFormat.JSONLD10_COMPACT_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD10_EXPAND_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD10_FLATTEN_FLAT;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.apache.jena.update.UpdateAction.execute;
import static org.apache.jena.update.UpdateFactory.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.JSONLD.flattened;
import static org.trellisldp.vocabulary.JSONLD.getNamespace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.commonsrdf.JenaCommonsRDF;
import org.apache.jena.http.HttpOp;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.LangJSONLD10;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.update.UpdateException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.CacheService.TrellisProfileCache;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.TrellisRuntimeException;
import org.trellisldp.vocabulary.Trellis;

/**
 * An IOService implemented using Jena.
 *
 * @author acoburn
 */
@ApplicationScoped
public class JenaIOService implements IOService {

    /** The configuration key listing valid JSON-LD profile documents. */
    public static final String CONFIG_IO_JSONLD_PROFILES = "trellis.jena.jsonld-profiles";

    /** The configuration key listing valid JSON-LD profile domains. */
    public static final String CONFIG_IO_JSONLD_DOMAINS = "trellis.jena.jsonld-domains";

    /** The configuration key controling whether to use relative IRIs for Turtle serializations. */
    public static final String CONFIG_IO_RELATIVE_IRIS = "trellis.jena.relative-iris";

    private static final Logger LOGGER = getLogger(JenaIOService.class);
    private static final Map<IRI, RDFFormat> JSONLD_FORMATS = Map.of(
                compacted, JSONLD10_COMPACT_FLAT,
                flattened, JSONLD10_FLATTEN_FLAT,
                expanded, JSONLD10_EXPAND_FLAT);

    private final List<RDFSyntax> readable = List.of(TURTLE, RDFSyntax.JSONLD, NTRIPLES, RDFA);
    private final List<RDFSyntax> writable = List.of(TURTLE, RDFSyntax.JSONLD, NTRIPLES);
    private final List<RDFSyntax> updatable = List.of(SPARQL_UPDATE);

    private Set<String> allowedContexts;
    private Set<String> allowedContextDomains;
    private boolean relativeIRIs;

    @Inject
    NamespaceService namespaceService;

    @Inject
    RDFaWriterService htmlSerializer;

    @Inject
    @TrellisProfileCache
    CacheService<String, String> cache;

    @Inject
    @ConfigProperty(name = CONFIG_IO_JSONLD_DOMAINS)
    Optional<String[]> allowedDomainsConfig;

    @Inject
    @ConfigProperty(name = CONFIG_IO_JSONLD_PROFILES)
    Optional<String[]> allowedContextsConfig;

    @Inject
    @ConfigProperty(name = CONFIG_IO_RELATIVE_IRIS)
    Optional<Boolean> relativeIriConfig;

    @PostConstruct
    void init() {
        allowedContexts = allowedContextsConfig.map(Set::of).orElseGet(Collections::emptySet);
        allowedContextDomains = allowedDomainsConfig.map(Set::of).orElseGet(Collections::emptySet);
        relativeIRIs = relativeIriConfig.orElse(Boolean.FALSE);

        // Use JSON-LD 1.0 parser
        final var jsonldParser = new ReaderRIOTFactoryJSONLD10();
        RDFParserRegistry.registerLangTriples(Lang.JSONLD, jsonldParser);
        RDFParserRegistry.registerLangQuads(Lang.JSONLD, jsonldParser);
        RDFWriterRegistry.register(Lang.JSONLD, RDFFormat.JSONLD10);

        LOGGER.info("Using Jena IO Service");
    }

    /**
     * Create a new Jena-based IOService.
     * @return an IOService instance
     */
    public static IOService newJenaIOService() {
        final JenaIOService svc = new JenaIOService();
        svc.cache = new NoopProfileCache();
        svc.namespaceService = new NoopNamespaceService();
        svc.allowedDomainsConfig = Optional.empty();
        svc.allowedContextsConfig = Optional.empty();
        svc.relativeIriConfig = Optional.empty();
        svc.init();
        return svc;
    }

    @Override
    public List<RDFSyntax> supportedReadSyntaxes() {
        if (htmlSerializer != null) {
            return readable;
        }
        return readable.stream().filter(format -> !RDFA.equals(format)).collect(toList());
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
    public void write(final Stream<Triple> triples, final OutputStream output, final RDFSyntax syntax,
            final String baseUrl, final IRI... profiles) {
        requireNonNull(triples, "The triples stream may not be null!");
        requireNonNull(output, "The output stream may not be null!");
        requireNonNull(syntax, "The RDF syntax value may not be null!");

        try {
            if (RDFA.equals(syntax)) {
                writeHTML(triples, output, baseUrl);
            } else {
                final Lang lang = JenaCommonsRDF.toJena(syntax).orElseThrow(() ->
                        new TrellisRuntimeException("Invalid content type: " + syntax.mediaType()));

                final RDFFormat format = defaultSerialization(lang);

                if (format != null) {
                    LOGGER.debug("Writing stream-based RDF: {}", format);
                    final StreamRDF stream = getWriterStream(output, format, null);
                    stream.start();
                    namespaceService.getNamespaces().forEach(stream::prefix);
                    if (shouldUseRelativeIRIs(relativeIRIs, profiles)) {
                        stream.base(baseUrl);
                    }
                    triples.map(JenaCommonsRDF::toJena).forEachOrdered(stream::triple);
                    stream.finish();
                } else {
                    LOGGER.debug("Writing buffered RDF: {}", lang);
                    final org.apache.jena.graph.Graph graph = createDefaultGraph();
                    graph.getPrefixMapping().setNsPrefixes(namespaceService.getNamespaces());
                    triples.map(JenaCommonsRDF::toJena).forEachOrdered(graph::add);
                    if (JSONLD.equals(lang)) {
                        writeJsonLd(output, DatasetGraphFactory.create(graph), profiles);
                    } else {
                        RDFDataMgr.write(output, graph, lang);
                    }
                }
            }
        } catch (final AtlasException ex) {
            throw new TrellisRuntimeException(ex);
        }
    }

    @Override
    public Stream<Triple> read(final InputStream input, final RDFSyntax syntax, final String base) {
        requireNonNull(input, "The input stream may not be null!");
        requireNonNull(syntax, "The syntax value may not be null!");

        try {
            final org.apache.jena.graph.Graph graph = createDefaultGraph();
            final Lang lang = JenaCommonsRDF.toJena(syntax).orElseThrow(() ->
                    new TrellisRuntimeException("Unsupported RDF Syntax: " + syntax.mediaType()));

            RDFParser.source(input).lang(lang).base(base).parse(graph);

            // Check the graph for any new namespace definitions
            final Set<String> namespaces = new HashSet<>(namespaceService.getNamespaces().values());
            graph.getPrefixMapping().getNsPrefixMap().forEach((prefix, namespace) -> {
                if (shouldAddNamespace(namespaces, namespace, base)) {
                    LOGGER.debug("Setting prefix ({}) for namespace {}", prefix, namespace);
                    namespaceService.setPrefix(prefix, namespace);
                }
            });
            return JenaCommonsRDF.fromJena(graph).stream().map(Triple.class::cast);
        } catch (final RiotException | AtlasException | IllegalArgumentException ex) {
            throw new TrellisRuntimeException(ex);
        }
    }

    @Override
    public void update(final Graph graph, final String update, final RDFSyntax syntax, final String base) {
        requireNonNull(graph, "The input graph may not be null");
        requireNonNull(update, "The update command may not be null");
        requireNonNull(syntax, "The RDF syntax may not be null");
        if (!SPARQL_UPDATE.equals(syntax)) {
            throw new TrellisRuntimeException("The syntax " + syntax + " is not supported for updates.");
        }

        try {
            final org.apache.jena.graph.Graph g = JenaCommonsRDF.toJena(graph);
            execute(create(update, base), g);
        } catch (final UpdateException | QueryParseException ex) {
            throw new TrellisRuntimeException(ex);
        }
    }

    private void writeHTML(final Stream<Triple> triples, final OutputStream output, final String context) {
        if (htmlSerializer != null) {
            htmlSerializer.write(triples, output, context);
        } else {
            write(triples, output, TURTLE, context);
        }
    }

    private boolean canUseCustomJsonLdProfile(final String profile) {
        return profile != null;
    }

    private void writeJsonLd(final OutputStream output, final DatasetGraph graph, final IRI... profiles) {
        final String profile = getCustomJsonLdProfile(profiles);
        final RDFFormat format = canUseCustomJsonLdProfile(profile) ? JSONLD10_COMPACT_FLAT
            : getJsonLdProfile(profiles);
        final JsonLDWriteContext ctx = new JsonLDWriteContext();
        if (canUseCustomJsonLdProfile(profile)) {
            LOGGER.debug("Setting JSON-LD context with profile: {}", profile);
            final String c = cache.get(profile, p -> {
                try (final TypedInputStream res = HttpOp.httpGet(profile)) {
                    return IOUtils.toString(res.getInputStream(), UTF_8);
                } catch (final IOException | HttpException ex) {
                    LOGGER.warn("Error fetching profile {}: {}", p, ex.getMessage());
                    return null;
                }
            });
            if (c != null) {
                ctx.setJsonLDContext(c);
                ctx.setJsonLDContextSubstitution("\"" + profile + "\"");
            }
        }
        RDFWriter.create().format(format).context(ctx).source(graph).output(output);
    }

    private String getCustomJsonLdProfile(final IRI... profiles) {
        for (final IRI p : profiles) {
            final String profile = p.getIRIString();
            if (!profile.startsWith(getNamespace())) {
                if (allowedContexts.contains(profile)) {
                    return profile;
                }
                for (final String domain : allowedContextDomains) {
                    if (profile.startsWith(domain)) {
                        return profile;
                    }
                }
            }
        }
        return null;
    }

    static IRI mergeProfiles(final IRI... profiles) {
        for (final IRI uri : profiles) {
            if (flattened.equals(uri)) {
                return flattened;
            } else if (compacted.equals(uri)) {
                return compacted;
            } else if (expanded.equals(uri)) {
                return expanded;
            }
        }
        return compacted;
    }

    static RDFFormat getJsonLdProfile(final IRI... profiles) {
        return JSONLD_FORMATS.get(mergeProfiles(profiles));
    }

    static boolean shouldAddNamespace(final Set<String> namespaces, final String namespace, final String base) {
        if (!namespaces.contains(namespace) && base != null) {
            try {
                final URL url1 = new URL(namespace);
                final URL url2 = new URL(base);
                return !url1.getProtocol().equals(url2.getProtocol()) ||
                    !url1.getHost().equals(url2.getHost()) ||
                    url1.getPort() != url2.getPort();
            } catch (final MalformedURLException ex) {
                LOGGER.debug("Skipping malformed URL: {}", ex.getMessage());
            }
        }
        return false;
    }

    static boolean shouldUseRelativeIRIs(final boolean defaultValue, final IRI... profiles) {
        for (final IRI profile : profiles) {
            if (Trellis.SerializationRelative.equals(profile)) {
                return true;
            } else if (Trellis.SerializationAbsolute.equals(profile)) {
                return false;
            }
        }
        return defaultValue;
    }

    private static class ReaderRIOTFactoryJSONLD10 implements ReaderRIOTFactory {
        @Override
        public ReaderRIOT create(final Lang language, final ParserProfile profile) {
            // force the use of jsonld-java (i.e., JSON-LD 1.0)
            return new LangJSONLD10(language, profile, profile.getErrorHandler());
        }
    }
}
