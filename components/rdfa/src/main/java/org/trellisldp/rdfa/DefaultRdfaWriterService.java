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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    private static final MustacheFactory mf = new DefaultMustacheFactory();

    /** The configuration key controlling the HTML template to use. */
    public static final String CONFIG_RDFA_TEMPLATE = "trellis.rdfa.template";

    /** The configuration key controlling the CSS URLs to use. */
    public static final String CONFIG_RDFA_CSS = "trellis.rdfa.css";

    /** The configuration key controlling the web icon to use. */
    public static final String CONFIG_RDFA_ICON = "trellis.rdfa.icon";

    /** The configuration key controlling the JS URLs to use. */
    public static final String CONFIG_RDFA_JS = "trellis.rdfa.js";

    private Mustache template;

    @Inject
    @ConfigProperty(name = CONFIG_RDFA_TEMPLATE,
                    defaultValue = "/org/trellisldp/rdfa/resource.mustache")
    String templateLocation;

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
        LOGGER.info("Using RDFa writer template: {}", templateLocation);
        final File file = new File(templateLocation);
        template = file.exists() ? mf.compile(templateLocation) :
            mf.compile(getClasspathReader(templateLocation), templateLocation);
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

    static Reader getClasspathReader(final String template) {
        final InputStream is = getClasspathResource(template);
        if (is != null) {
            return new InputStreamReader(is, UTF_8);
        }
        LOGGER.warn("Unable to load RDFa writer template from [{}], falling back to default", template);
        return getDefaultTemplateReader();
    }

    static Reader getDefaultTemplateReader() {
        return new StringReader("<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><title>{{title}}</title></head>\n" +
            "<body><h1>{{title}}</h1>\n" +
            "<main>\n" +
            "{{#triples}}\n" +
            "<p>\n" +
            "{{subject}} <a href=\"{{predicate}}\">{{predicateLabel}}</a>\n" +
            "{{#objectIsIRI}}\n" +
            "<a href=\"{{object}}\">{{objectLabel}}</a>\n" +
            "{{/objectIsIRI}}\n" +
            "{{^objectIsIRI}}\n" +
            "{{objectLabel}}\n" +
            "{{/objectIsIRI}}\n" +
            "</p>\n" +
            "{{/triples}}\n" +
            "</main></body></html>\n");
    }

    static InputStream getClasspathResource(final String location) {
        final InputStream is = DefaultRdfaWriterService.class.getResourceAsStream(location);
        if (is == null) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        }
        return is;
    }
}
