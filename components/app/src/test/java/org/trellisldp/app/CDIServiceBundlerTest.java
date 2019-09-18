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

import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.trellisldp.api.*;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.io.NoopProfileCache;
import org.trellisldp.rdfa.DefaultRdfaWriterService;

@ExtendWith(WeldJunit5Extension.class)
public class CDIServiceBundlerTest {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                       .beanClasses(
                                           BaseServiceBundler.class,
                                           CDIConstraintServices.class,
                                           DefaultIdentifierService.class,
                                           DefaultRdfaWriterService.class,
                                           DefaultTimemapGenerator.class,
                                           FileBinaryService.class,
                                           FileMementoService.class,
                                           JenaIOService.class,
                                           LdpConstraintService.class,
                                           NoopAuditService.class,
                                           NoopEventService.class,
                                           NoopNamespaceService.class,
                                           NoopProfileCache.class,
                                           NoopResourceService.class)
                                       .alternatives(NoopAuditService.class,
                                           NoopEventService.class,
                                           NoopNamespaceService.class,
                                           NoopResourceService.class));

    @Inject
    private ServiceBundler serviceBundler;

    @Test
    public void testResourceService() {
        assertNotNull(serviceBundler.getResourceService());
    }

    @Test
    public void testBinaryService() {
        assertNotNull(serviceBundler.getBinaryService());
    }

    @Test
    public void testAuditService() {
        assertNotNull(serviceBundler.getAuditService());
    }

    @Test
    public void testTimemapGenerator() {
        assertNotNull(serviceBundler.getTimemapGenerator());
    }

    @Test
    public void testIOService() {
        assertNotNull(serviceBundler.getIOService());
    }

    @Test
    public void testConstraintServices() {
        int counter = 0;
        for (final ConstraintService c : serviceBundler.getConstraintServices()) {
            counter++;
        }
        assertEquals(1, counter);
    }

    @Test
    public void testMementoService() {
        assertNotNull(serviceBundler.getMementoService());
    }

    @Test
    public void testEventService() {
        assertNotNull(serviceBundler.getEventService());
    }
}
