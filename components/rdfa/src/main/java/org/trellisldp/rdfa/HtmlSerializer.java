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
package org.trellisldp.rdfa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;

/**
 * An RDFa (HTML) serialization service.
 */
public class HtmlSerializer implements RDFaWriterService {

    private static final MustacheFactory mf = new DefaultMustacheFactory();

    public static final String HTML_TEMPLATE = "trellis.html.template";
    public static final String HTML_CSS = "trellis.html.css";
    public static final String HTML_ICON = "trellis.html.icon";
    public static final String HTML_JS = "trellis.html.js";

    private final NamespaceService namespaceService;
    private final Mustache template;
    private final List<String> css;
    private final List<String> js;
    private final String icon;

    /**
     * Create an HTML Serializer object.
     *
     * @param namespaceService a namespace service
     */
    @Inject
    public HtmlSerializer(final NamespaceService namespaceService) {
        this(namespaceService, getConfiguration().get(HTML_TEMPLATE),
                getConfiguration().get(HTML_CSS), getConfiguration().get(HTML_JS),
                getConfiguration().get(HTML_ICON));
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
    public HtmlSerializer(final NamespaceService namespaceService,
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
    public HtmlSerializer(final NamespaceService namespaceService,
            final String template, final List<String> css,
            final List<String> js, final String icon) {
        this.namespaceService = namespaceService;
        final String templatePath = ofNullable(template).orElse("org/trellisldp/rdfa/resource.mustache");
        final File tpl = new File(templatePath);
        this.template = tpl.exists() ? mf.compile(templatePath) : mf.compile(getReader(templatePath), templatePath);
        this.css = ofNullable(css).orElseGet(Collections::emptyList);
        this.js = ofNullable(js).orElseGet(Collections::emptyList);
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
    public void write(final Stream<? extends Triple> triples, final OutputStream out, final String subject) {
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
        return stream(property.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(toList());
    }
}
