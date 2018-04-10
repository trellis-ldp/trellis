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
package org.trellisldp.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;

import java.io.File;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test OSGi provisioning.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OSGiTest {

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
        final String jenaVersion = cm.getProperty("jena.version");

        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .version(cm.getProperty("karaf.version")).type("zip"))
                .unpackDirectory(new File("build", "exam"))
                .useDeployFolder(false),
            logLevel(LogLevel.INFO),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(),

            features(maven().groupId("org.apache.karaf.features").artifactId("standard")
                        .versionAsInProject().classifier("features").type("xml"), "scr"),
            features(maven().groupId("org.apache.jena").artifactId("jena-osgi-features")
                        .version(jenaVersion).classifier("features").type("xml"), "xerces"),
            features(maven().groupId("org.trellisldp").artifactId("trellis-karaf")
                        .type("xml").classifier("features").versionAsInProject(),
                        "trellis-io-jena"),

            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)
       };
    }

    @Test
    public void testCoreInstallation() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("commons-rdf-jena")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-io-jena")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-api")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-vocabulary")));
    }

    @Test
    public void testAuditAndTriplestoreInstallation() throws Exception {
        // test these two together because trellis-triplestore depends on trellis-audit
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-audit")));
        featuresService.installFeature("trellis-audit");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-audit")));
        featuresService.uninstallFeature("trellis-audit");
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-audit")));

        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-triplestore")));
        featuresService.installFeature("trellis-triplestore");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-triplestore")));
    }

    @Test
    public void testHttpInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-http")));
        featuresService.installFeature("trellis-http");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-http")));
    }

    @Test
    public void testIdInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-id")));
        featuresService.installFeature("trellis-id");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-id")));
    }

    @Test
    public void testWebacInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-webac")));
        featuresService.installFeature("trellis-webac");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-webac")));
    }

    @Test
    public void testAgentInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-agent")));
        featuresService.installFeature("trellis-agent");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-agent")));
    }

    @Test
    public void testFileInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-file")));
        featuresService.installFeature("trellis-file");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-file")));
    }

    @Test
    public void testConstraintsInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-constraint-rules")));
        featuresService.installFeature("trellis-constraint-rules");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-constraint-rules")));
    }

    @Test
    public void testEventSerializationInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-event-serialization"))) {
            featuresService.installFeature("trellis-event-serialization");
        }
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-event-serialization")));
    }

    @Test
    public void testKafkaInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-kafka")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-kafka");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-kafka")));
    }

    @Test
    public void testAmqpInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-amqp")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-amqp");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-amqp")));
    }

    @Test
    public void testJmsInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-jms")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-jms");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-jms")));
    }

    @Test
    public void testNamespacesInstallation() throws Exception {
        assertFalse(featuresService.isInstalled(featuresService.getFeature("trellis-namespaces")));
        featuresService.installFeature("trellis-namespaces");
        assertTrue(featuresService.isInstalled(featuresService.getFeature("trellis-namespaces")));
    }

    protected <T> T getOsgiService(final Class<T> type, final String filter, final long timeout) {
        try {
            final ServiceTracker<?, T> tracker = new ServiceTracker<>(bundleContext,
                    createFilter("(&(" + OBJECTCLASS + "=" + type.getName() + ")" + filter + ")"), null);
            tracker.open(true);
            final T svc = tracker.waitForService(timeout);
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + filter);
            }
            return svc;
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
