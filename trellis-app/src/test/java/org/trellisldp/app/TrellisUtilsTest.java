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
package org.trellisldp.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.io.JenaIOService.IO_HTML_CSS;
import static org.trellisldp.io.JenaIOService.IO_HTML_ICON;
import static org.trellisldp.io.JenaIOService.IO_HTML_JS;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.app.config.NotificationsConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.kafka.KafkaPublisher;

/**
 * @author acoburn
 */
public class TrellisUtilsTest {

    @Mock
    private Environment mockEnv;

    @Mock
    private LifecycleEnvironment mockLifecycle;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockEnv.lifecycle()).thenReturn(mockLifecycle);
    }

    @Test
    public void testGetAssetConfigurations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        final Map<String, String> assets = TrellisUtils.getAssetConfiguration(config);
        assertEquals(4L, assets.size());
        assertEquals("http://example.org/image.icon", assets.get(IO_HTML_ICON));
        assertEquals("http://example.org/styles1.css,http://example.org/styles2.css", assets.get(IO_HTML_CSS));
        assertEquals("http://example.org/scripts1.js,http://example.org/scripts2.js", assets.get(IO_HTML_JS));
    }

    @Test
    public void testGetWebacConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertTrue(TrellisUtils.getWebacConfiguration(config).isPresent());

        config.getAuth().getWebac().setEnabled(false);

        assertFalse(TrellisUtils.getWebacConfiguration(config).isPresent());
    }

    @Test
    public void testGetCORSConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertTrue(TrellisUtils.getCorsConfiguration(config).isPresent());

        config.getCors().setEnabled(false);

        assertFalse(TrellisUtils.getCorsConfiguration(config).isPresent());
    }

    @Test
    public void testGetRDFConnection() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        config.setResources("http://localhost/sparql");

        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        config.setResources("https://localhost/sparql");
        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        final File dir = new File(new File(getClass().getResource("/data").toURI()), "resources");
        config.setResources(dir.getAbsolutePath());
        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());
    }



    @Test
    public void testGetAuthFilters() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        final Optional<List<AuthFilter>> filters = TrellisUtils.getAuthFilters(config);
        assertTrue(filters.isPresent());
        filters.ifPresent(f -> assertEquals(3L, f.size()));

        config.getAuth().getAnon().setEnabled(false);
        config.getAuth().getBasic().setEnabled(false);
        config.getAuth().getJwt().setEnabled(false);

        assertFalse(TrellisUtils.getAuthFilters(config).isPresent());
    }

    @Test
    public void testEventServiceNone() throws Exception {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.setConnectionString("localhost");
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.NONE);
        final EventService svc = TrellisUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc);
        assertTrue(svc instanceof NoopEventService);
    }

    @Test
    public void testEventServiceDisabled() throws Exception {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        c.setEnabled(false);
        c.setType(NotificationsConfiguration.Type.KAFKA);
        final EventService svc = TrellisUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc);
        assertTrue(svc instanceof NoopEventService);
    }

    @Test
    public void testEventServiceKafka() throws Exception {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.KAFKA);
        final EventService svc = TrellisUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc);
        assertTrue(svc instanceof KafkaPublisher);
    }

    @Test
    public void testGetKafkaProps() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("some.other", "value");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        final Properties p = TrellisUtils.getKafkaProperties(c);
        assertEquals("all", p.getProperty("acks"));
        assertEquals("value", p.getProperty("some.other"));
        assertEquals("org.apache.kafka.common.serialization.StringSerializer", p.getProperty("key.serializer"));
        assertEquals("localhost:9092", p.getProperty("bootstrap.servers"));
    }

    @Test
    public void testEventServiceJms() throws Exception {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.setConnectionString("tcp://localhost:61616");
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.JMS);
        assertThrows(JMSException.class, () -> TrellisUtils.getNotificationService(c, mockEnv));
    }

    @Test
    public void testGetJmsFactory() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.setConnectionString("localhost:61616");

        final ActiveMQConnectionFactory factory1 = TrellisUtils.getJmsFactory(c);
        assertNull(factory1.getUserName());
        assertNull(factory1.getPassword());
        assertEquals("localhost:61616", factory1.getBrokerURL());

        c.set("password", "pass");
        final ActiveMQConnectionFactory factory2 = TrellisUtils.getJmsFactory(c);
        assertNull(factory2.getUserName());
        assertNull(factory2.getPassword());
        assertEquals("localhost:61616", factory2.getBrokerURL());

        c.set("username", "user");
        final ActiveMQConnectionFactory factory3 = TrellisUtils.getJmsFactory(c);
        assertEquals("user", factory3.getUserName());
        assertEquals("pass", factory3.getPassword());
        assertEquals("localhost:61616", factory3.getBrokerURL());
    }
}
