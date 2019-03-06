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
package org.trellisldp.osgi;

import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.osgi.framework.Bundle.ACTIVE;
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
import org.trellisldp.api.RuntimeTrellisException;

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
        final String activemqVersion = cm.getProperty("activemq.version");
        final String springFeatureVersion = cm.getProperty("spring.version");

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
            features(maven().groupId("org.apache.activemq").artifactId("activemq-karaf")
                        .version(activemqVersion).classifier("features").type("xml")),
            features(maven().groupId("org.apache.jena").artifactId("jena-osgi-features")
                        .version(jenaVersion).classifier("features").type("xml")),
            // TODO -- with a new version of ActiveMQ, it should be possible to user `versionAsInProject()`
            features(maven().groupId("org.apache.karaf.features").artifactId("spring")
                        .version(springFeatureVersion).classifier("features").type("xml"), "spring"),
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
        assertTrue("commons-rdf-jena not installed!",
                featuresService.isInstalled(featuresService.getFeature("commons-rdf-jena")));
        assertTrue("trellis-io-jena not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-io-jena")));
        assertTrue("trellis-api not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-api")));
        assertTrue("trellis-vocabulary not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-vocabulary")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testAuditAndTriplestoreInstallation() throws Exception {
        // test these two together because trellis-triplestore depends on trellis-audit
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-audit"))) {
            featuresService.installFeature("trellis-audit");
        }
        assertTrue("trellis-audit not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-audit")));

        if (!featuresService.isInstalled(featuresService.getFeature("trellis-triplestore"))) {
            featuresService.installFeature("trellis-triplestore");
        }
        assertTrue("trellis-triplestore not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-triplestore")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testHttpInstallation() throws Exception {
        assertFalse("trellis-http already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-http")));
        featuresService.installFeature("trellis-http");
        assertTrue("trellis-http not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-http")));
    }

    @Test
    public void testRDFaInstallation() throws Exception {
        featuresService.installFeature("trellis-rdfa");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-rdfa not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-rdfa")));
    }

    @Test
    public void testWebacInstallation() throws Exception {
        assertFalse("trellis-webac already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-webac")));
        featuresService.installFeature("trellis-triplestore");
        featuresService.installFeature("trellis-webac");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-webac not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-webac")));
        featuresService.uninstallFeature("trellis-webac");
        featuresService.uninstallFeature("trellis-triplestore");
        if (featuresService.isInstalled(featuresService.getFeature("trellis-audit"))) {
            featuresService.uninstallFeature("trellis-audit");
        }
    }

    @Test
    public void testAgentInstallation() throws Exception {
        assertFalse("trellis-agent already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-agent")));
        featuresService.installFeature("trellis-agent");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-agent not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-agent")));
    }

    @Test
    public void testFileInstallation() throws Exception {
        assertFalse("trellis-file already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-file")));
        featuresService.installFeature("trellis-file");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-file not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-file")));
    }

    @Test
    public void testConstraintsInstallation() throws Exception {
        assertFalse("trellis-constraint-rules already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-constraint-rules")));
        featuresService.installFeature("trellis-constraint-rules");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-constraint-rules not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-constraint-rules")));
    }

    @Test
    public void testEventSerializationInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-event-serialization"))) {
            featuresService.installFeature("trellis-event-serialization");
        }
        checkTrellisBundlesAreActive();
        assertTrue("trellis-event-serialization not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-event-serialization")));
    }

    @Test
    public void testKafkaInstallation() throws Exception {
        assertFalse("trellis-kafka already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-kafka")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-kafka");
        assertTrue("trellis-kafka not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-kafka")));
    }

    @Test
    public void testAmqpInstallation() throws Exception {
        assertFalse("trellis-amqp already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-amqp")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-amqp");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-amqp not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-amqp")));
    }

    @Test
    public void testJmsInstallation() throws Exception {
        assertFalse("trellis-jms already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jms")));
        featuresService.installFeature("trellis-event-serialization");
        featuresService.installFeature("trellis-jms");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-jms not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jms")));
    }

    @Test
    public void testOAuthAuthInstallation() throws Exception {
        assertFalse("trellis-auth-oauth already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-auth-oauth")));
        featuresService.installFeature("trellis-auth-oauth");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-auth-oauth not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-auth-oauth")));
    }

    @Test
    public void testBasicAuthInstallation() throws Exception {
        assertFalse("trellis-auth-basic already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-auth-basic")));
        featuresService.installFeature("trellis-auth-basic");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-auth-basic not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-auth-basic")));
    }

    @Test
    public void testNamespacesInstallation() throws Exception {
        assertFalse("trellis-namespace already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-namespaces")));
        featuresService.installFeature("trellis-namespaces");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-namespace not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-namespaces")));
    }

    private void checkTrellisBundlesAreActive() {
        stream(bundleContext.getBundles()).filter(b -> b.getSymbolicName().startsWith("org.trellisldp")).forEach(b ->
                    assertEquals("Bundle " + b.getSymbolicName() + " is not active!", ACTIVE, b.getState()));
    }

    protected <T> T getOsgiService(final Class<T> type, final String filter, final long timeout) {
        try {
            final ServiceTracker<?, T> tracker = new ServiceTracker<>(bundleContext,
                    createFilter("(&(" + OBJECTCLASS + "=" + type.getName() + ")" + filter + ")"), null);
            tracker.open(true);
            final T svc = tracker.waitForService(timeout);
            if (svc == null) {
                throw new RuntimeTrellisException("Gave up waiting for service " + filter);
            }
            return svc;
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeTrellisException(e);
        }
    }
}
