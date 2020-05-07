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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.rdf.api.Triple;
import org.eclipse.microprofile.config.Config;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.api.RDFaWriterService;

/**
 * An RDFa (HTML) serialization service.
 */
@ApplicationScoped
public class DefaultRdfaWriterService implements RDFaWriterService {

    private static final MustacheFactory mf = new DefaultMustacheFactory();

    /** The configuration key controlling the HTML template to use. */
    public static final String CONFIG_RDFA_TEMPLATE = "trellis.rdfa.template";

    /** The configuration key controlling the CSS URLs to use. */
    public static final String CONFIG_RDFA_CSS = "trellis.rdfa.css";

    /** The configuration key controlling the web icon to use. */
    public static final String CONFIG_RDFA_ICON = "trellis.rdfa.icon";

    /** The configuration key controlling the JS URLs to use. */
    public static final String CONFIG_RDFA_JS = "trellis.rdfa.js";

    private final NamespaceService namespaceService;
    private final Mustache template;
    private final List<String> css;
    private final List<String> js;
    private final String icon;

    /**
     * Create an HTML Serializer object.
     */
    public DefaultRdfaWriterService() {
        this(new NoopNamespaceService());
    }

    /**
     * Create an HTML Serializer object.
     *
     * @param namespaceService a namespace service
     */
    @Inject
    public DefaultRdfaWriterService(final NamespaceService namespaceService) {
        this(namespaceService, getConfig());
    }

    private DefaultRdfaWriterService(final NamespaceService namespaceService, final Config config) {
        this(namespaceService, config.getOptionalValue(CONFIG_RDFA_TEMPLATE, String.class).orElse(null),
                config.getOptionalValue(CONFIG_RDFA_CSS, String.class)
                    .orElse("//www.trellisldp.org/assets/css/trellis.css"),
                config.getOptionalValue(CONFIG_RDFA_JS, String.class).orElse(""),
                config.getOptionalValue(CONFIG_RDFA_ICON, String.class)
                    .orElse("//www.trellisldp.org/assets/img/trellis.png"));
    }

    /**
     * Create an HTML Serializer object.
     *
     * @param namespaceService a namespace service
     * @param template the template location
     * @param css the css to use (comma-delimited for multiple css documents)
     * @param js the js to use (comma-delimited for multiple js documents)
     * @param icon an icon, may be {@code null}
     */
    public DefaultRdfaWriterService(final NamespaceService namespaceService,
            final String template, final String css,
            final String js, final String icon) {
        this(namespaceService, template, intoList(css), intoList(js), icon);
    }

    /**
     * Create an HTML Serializer object.
     *
     * @param namespaceService a namespace service
     * @param template the template location
     * @param css the css to use (comma-delimited for multiple css documents)
     * @param js the js to use (comma-delimited for multiple js documents)
     * @param icon an icon, may be {@code null}
     */
    public DefaultRdfaWriterService(final NamespaceService namespaceService,
            final String template, final List<String> css,
            final List<String> js, final String icon) {
        this.namespaceService = requireNonNull(namespaceService, "NamespaceService may not be null!");
        final String templatePath = template != null ? template : "org/trellisldp/rdfa/resource.mustache";
        final File tpl = new File(templatePath);
        this.template = tpl.exists() ? mf.compile(templatePath) : mf.compile(getReader(templatePath), templatePath);
        this.css = css != null ? css : emptyList();
        this.js = js != null ? js : emptyList();
        this.icon = icon;
    }

    private Reader getReader(final String template) {
        if (template.startsWith("/")) {
            return new InputStreamReader(getClass().getResourceAsStream(template), UTF_8);
        }
        return new InputStreamReader(getClass().getResourceAsStream("/" + template), UTF_8);
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
                .execute(writer, new HtmlData(namespaceService, subject, triples.collect(toList()), css, js, icon))
                .flush();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static List<String> intoList(final String property) {
        if (property != null) {
            return stream(property.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(toList());
        }
        return emptyList();
    }
}
