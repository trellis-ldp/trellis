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

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.processor.idempotent.kafka.KafkaIdempotentRepository;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

public class CamelKafkaProducerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelKafkaProducerTest.class);

    private KafkaIdempotentRepository kafkaIdempotentRepository;

    private volatile ProducerTemplate template;

    private static ThreadLocal<ModelCamelContext> threadCamelContext = new ThreadLocal<>();

    private volatile ModelCamelContext context;

    private static final RDF rdf = new SimpleRDF();

    private final Instant time = now();

    @Mock
    private Event mockEvent;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockEvent.getTarget()).thenReturn(of(rdf.createIRI("trellis:repository/resource")));
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getTargetTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
    }

    @Test
    public void testCamelKafkaProducer() throws Exception {
        context = (ModelCamelContext) createCamelContext();
        threadCamelContext.set(context);
        template = context.createProducerTemplate();
        context.addRoutes(createRouteBuilder());
        context.start();
        template.start();
        final EventService svc = new CamelProducer(template, "direct:in");
        svc.emit(mockEvent);
    }


    private JndiRegistry createRegistry() throws Exception {
        final JndiRegistry jndi = new JndiRegistry(createJndiContext());

        kafkaIdempotentRepository = new KafkaIdempotentRepository("test-topic", "localhost:9094");
        jndi.bind("kafkaIdempotentRepository", kafkaIdempotentRepository);

        return jndi;
    }

    private Context createJndiContext() throws Exception {
        final Properties properties = new Properties();

        final InputStream in = getClass().getClassLoader().getResourceAsStream("jndi.properties");
        if (in != null) {
            LOGGER.debug("Using jndi.properties from classpath root");
            properties.load(in);
        } else {
            properties.put("java.naming.factory.initial", "org.apache.camel.util.jndi.CamelInitialContextFactory");
        }
        return new InitialContext(new Hashtable<>(properties));
    }

    private CamelContext createCamelContext() throws Exception {
        return new DefaultCamelContext(createRegistry());
    }

    private RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in").to("mock:before").idempotentConsumer(header("id")).messageIdRepositoryRef(
                        "kafkaIdempotentRepository").to("mock:out").end();
            }
        };
    }
}
