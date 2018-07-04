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
package org.trellisldp.amqp;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.time.Instant;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class AmqpPublisherTest {

    private static final RDF rdf = new SimpleRDF();

    private final String exchangeName = "exchange";

    private final String queueName = "queue";

    private final Instant time = now();

    @Mock
    private Channel mockChannel;

    @Mock
    private Event mockEvent;

    @BeforeEach
    public void setUp() throws IOException {
        initMocks(this);
        when(mockEvent.getTarget()).thenReturn(of(rdf.createIRI("trellis:data/resource")));
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getTargetTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        doNothing().when(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    public void testAmqp() throws IOException {
        final EventService svc = new AmqpPublisher(mockChannel, exchangeName, queueName);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    public void testAmqpConfiguration() throws IOException {
        final EventService svc = new AmqpPublisher(mockChannel);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    public void testError() throws IOException {
        doThrow(IOException.class).when(mockChannel).basicPublish(eq(exchangeName), eq(queueName),
                anyBoolean(), anyBoolean(), any(BasicProperties.class), any(byte[].class));

        final EventService svc = new AmqpPublisher(mockChannel, exchangeName, queueName, true, true);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }
}
