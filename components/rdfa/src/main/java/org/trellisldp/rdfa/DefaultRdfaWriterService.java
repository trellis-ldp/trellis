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
package org.trellisldp.rdfa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Triple;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;

/**
 * An RDFa (HTML) serialization service.
 */
@ApplicationScoped
public class DefaultRdfaWriterService implements RDFaWriterService {

    private static final Logger LOGGER = getLogger(DefaultRdfaWriterService.class);

    /** The configuration key controlling the HTML template to use. */
    public static final String CONFIG_RDFA_TEMPLATE = "trellis.rdfa.template";

    /** The configuration key controlling the CSS URLs to use. */
    public static final String CONFIG_RDFA_CSS = "trellis.rdfa.css";

    /** The configuration key controlling the web icon to use. */
    public static final String CONFIG_RDFA_ICON = "trellis.rdfa.icon";

    /** The configuration key controlling the JS URLs to use. */
    public static final String CONFIG_RDFA_JS = "trellis.rdfa.js";

    private MustacheFactory mf;
    private Mustache template;

    @Inject
    @ConfigProperty(name = CONFIG_RDFA_TEMPLATE)
    Optional<String> templateLocation;

    @Inject
    @ConfigProperty(name = CONFIG_RDFA_ICON,
                    defaultValue = "//www.trellisldp.org/assets/img/trellis.png")
    Optional<String> icon;

    @Inject
    @ConfigProperty(name = CONFIG_RDFA_CSS,
                    defaultValue = "//www.trellisldp.org/assets/css/trellis.css")
    Optional<String[]> css;

    @Inject
    @ConfigProperty(name = CONFIG_RDFA_JS)
    Optional<String[]> js;

    @Inject
    NamespaceService namespaceService;

    @PostConstruct
    void init() {
        final String resource = templateLocation.orElse("org/trellisldp/rdfa/resource.mustache");
        LOGGER.info("Using RDFa writer template: {}", resource);
        mf = new DefaultMustacheFactory();
        template = mf.compile(resource);
    }

    /**
     * Send the content to an output stream.
     *
     * @param triples the triples
     * @param out the output stream
     * @param subject the subject
     */
    @Override
    public void write(final Stream<Triple> triples, final OutputStream out, final String subject) {
        final Writer writer = new OutputStreamWriter(out, UTF_8);
        try {
            template
                .execute(writer, new HtmlData(namespaceService, subject, triples.collect(toList()),
                            css.map(List::of).orElseGet(() -> List.of("//www.trellisldp.org/assets/css/trellis.css")),
                            js.map(List::of).orElseGet(Collections::emptyList),
                            icon.orElse(null)))
                .flush();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
