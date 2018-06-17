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
package org.trellisldp.camel.producer;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ServiceLoader;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;

/**
 * A Camel message producer.
 */
public class CamelProducer implements EventService {

    private volatile ProducerTemplate template;

    private String endpointUri;

    private static final Logger LOGGER = getLogger(CamelProducer.class);

    // TODO - JDK9 ServiceLoader::findFirst
    private static ActivityStreamService service = ServiceLoader.load(ActivityStreamService.class).iterator().next();

    /**
     * Create a new CamelProducer.
     *
     * @param template the template
     * @param endpointUri the endpointUri
     */
    public CamelProducer(final ProducerTemplate template, final String endpointUri) {
        requireNonNull(template, "Camel producer may not be null!");

        this.template = template;
        this.endpointUri = endpointUri;
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        service.serialize(event).ifPresent(message -> {
            template.sendBodyAndHeader(
                    endpointUri, message, "id", event.getTarget().map(IRI::getIRIString).orElse(null));
        });
    }
}

