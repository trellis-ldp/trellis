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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

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
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.NamespaceService;

/**
 * @author acoburn
 */
public class HtmlSerializer {

    private static final MustacheFactory mf = new DefaultMustacheFactory();

    private final Mustache template;
    private final NamespaceService namespaceService;
    private final Map<String, String> properties;

    /**
     * Create a ResourceView object.
     *
     * @param namespaceService a namespace service
     * @param template the template name
     * @param properties additional HTML-related properties, e.g. URLs for icon, css, js
     */
    public HtmlSerializer(final NamespaceService namespaceService, final String template,
            final Map<String, String> properties) {
        this.namespaceService = namespaceService;
        final File tpl = new File(template);
        if (tpl.exists()) {
            this.template = mf.compile(template);
        } else {
            this.template = mf.compile(getReader(template), template);
        }
        this.properties = properties;
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
     * @param out the output stream
     * @param triples the triples
     * @param subject the subject
     */
    public void write(final OutputStream out, final Stream<? extends Triple> triples, final IRI subject) {
        final Writer writer = new OutputStreamWriter(out, UTF_8);
        try {
            template
                .execute(writer, new HtmlData(namespaceService, subject, triples.collect(toList()), properties))
                .flush();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
