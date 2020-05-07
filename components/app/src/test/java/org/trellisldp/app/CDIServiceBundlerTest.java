/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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
package org.trellisldp.app;

import static java.util.stream.StreamSupport.stream;
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
class CDIServiceBundlerTest {

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
                                           NoopProfileCache.class,
                                           TestServices.class)
                                       .alternatives(FileMementoService.class));

    @Inject
    private ServiceBundler serviceBundler;

    @Test
    void testResourceService() {
        assertNotNull(serviceBundler.getResourceService());
    }

    @Test
    void testBinaryService() {
        assertNotNull(serviceBundler.getBinaryService());
    }

    @Test
    void testAuditService() {
        assertNotNull(serviceBundler.getAuditService());
    }

    @Test
    void testTimemapGenerator() {
        assertNotNull(serviceBundler.getTimemapGenerator());
    }

    @Test
    void testIOService() {
        assertNotNull(serviceBundler.getIOService());
    }

    @Test
    void testConstraintServices() {
        assertEquals(1, stream(serviceBundler.getConstraintServices().spliterator(), false).count());
    }

    @Test
    void testMementoService() {
        assertNotNull(serviceBundler.getMementoService());
    }

    @Test
    void testEventService() {
        assertNotNull(serviceBundler.getEventService());
    }
}
