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
package org.trellisldp.cdi;

import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.trellisldp.app.ConstraintServices;
import org.trellisldp.constraint.LdpConstraintService;

@ExtendWith(WeldJunit5Extension.class)
class CDIConstraintServicesTest {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                       .beanClasses(
                                           CDIConstraintServices.class,
                                           LdpConstraintService.class));

    @Inject
    private ConstraintServices constraintServices;

    @Test
    void testConstraintServices() {
        assertEquals(1, stream(constraintServices.spliterator(), false).count());
    }
}
