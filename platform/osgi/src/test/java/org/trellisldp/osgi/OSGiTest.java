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
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

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
        final String karafVersion = cm.getProperty("karaf.version");

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
                        .version(karafVersion).classifier("features").type("xml"), "scr"),
            features(maven().groupId("org.apache.karaf.features").artifactId("spring-legacy")
                        .version(karafVersion).classifier("features").type("xml"), "spring"),
            features(maven().groupId("org.apache.jena").artifactId("jena-osgi-features")
                        .version(jenaVersion).classifier("features").type("xml")),
            features(maven().groupId("org.trellisldp").artifactId("trellis-karaf")
                        .type("xml").classifier("features").versionAsInProject(),
                        "trellis-jena"),

            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories",
                    "https://repo1.maven.org/maven2@id=central"),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),

            new VMOption("--add-opens"),
            new VMOption("java.base/java.security=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.net=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.lang=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.util=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
            new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED")
       };
    }

    @Test
    public void testCoreInstallation() throws Exception {
        assertTrue("trellis-jena not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jena")));
        assertTrue("trellis-api not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-api")));
        assertTrue("trellis-vocabulary not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-vocabulary")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testAuditInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-audit"))) {
            featuresService.installFeature("trellis-audit");
        }
        assertTrue("trellis-audit not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-audit")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testJdbcInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-jdbc"))) {
            featuresService.installFeature("trellis-jdbc");
        }
        assertTrue("trellis-jdbc not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jdbc")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testTriplestoreInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-triplestore"))) {
            featuresService.installFeature("trellis-triplestore");
        }
        assertTrue("trellis-triplestore not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-triplestore")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testHttpInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-http"))) {
            featuresService.installFeature("trellis-http");
        }
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
        assertTrue("trellis-webac not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-webac")));
        featuresService.uninstallFeature("trellis-webac");
        featuresService.uninstallFeature("trellis-triplestore");
        if (featuresService.isInstalled(featuresService.getFeature("trellis-audit"))) {
            featuresService.uninstallFeature("trellis-audit");
        }
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
    public void testConstraintInstallation() throws Exception {
        assertFalse("trellis-constraint already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-constraint")));
        featuresService.installFeature("trellis-constraint");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-constraint not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-constraint")));
    }

    @Test
    public void testNotificationSerializationInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-notification-jackson"))) {
            featuresService.installFeature("trellis-notification-jackson");
        }
        checkTrellisBundlesAreActive();
        assertTrue("trellis-notification-jackson not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-notification-jackson")));
    }

    @Test
    public void testJwtAuthAuthInstallation() throws Exception {
        assertFalse("trellis-jwt already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jwt")));
        featuresService.installFeature("trellis-jwt");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-jwt not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-jwt")));
    }

    @Test
    public void testNamespaceInstallation() throws Exception {
        assertFalse("trellis-namespace already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-namespace")));
        featuresService.installFeature("trellis-namespace");
        checkTrellisBundlesAreActive();
        assertTrue("trellis-namespace not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-namespace")));
    }

    @Test
    public void testWebdavInstallation() throws Exception {
        assertFalse("trellis-webdav already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-webdav")));
        featuresService.installFeature("trellis-webdav");
        assertTrue("trellis-webdav not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-webdav")));
    }

    @Test
    public void testNotificationJsonbInstallation() throws Exception {
        assertFalse("trellis-notification-jsonb already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-notification-jsonb")));
        featuresService.installFeature("trellis-notification-jsonb");
        assertTrue("trellis-notification-jsonb not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-notification-jsonb")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testAppInstallation() throws Exception {
        if (!featuresService.isInstalled(featuresService.getFeature("trellis-app"))) {
            featuresService.installFeature("trellis-app");
        }
        assertTrue("trellis-app not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-app")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testCdiInstallation() throws Exception {
        assertFalse("trellis-cdi already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-cdi")));
        featuresService.installFeature("trellis-cdi");
        assertTrue("trellis-cdi not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-cdi")));
        checkTrellisBundlesAreActive();
    }

    @Test
    public void testCacheInstallation() throws Exception {
        assertFalse("trellis-cache already installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-cache")));
        featuresService.installFeature("trellis-cache");
        assertTrue("trellis-cache not installed!",
                featuresService.isInstalled(featuresService.getFeature("trellis-cache")));
        checkTrellisBundlesAreActive();
    }

    private void checkTrellisBundlesAreActive() {
        stream(bundleContext.getBundles()).filter(b -> b.getSymbolicName().startsWith("org.trellisldp")).forEach(b ->
                    assertEquals("Bundle " + b.getSymbolicName() + " is not active!", ACTIVE, b.getState()));
    }
}
