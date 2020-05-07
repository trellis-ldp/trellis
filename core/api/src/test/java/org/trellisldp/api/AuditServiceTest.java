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
package org.trellisldp.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuditServiceTest {

    @Test
    void testNullAuditService() {
        final AuditService svc = new NoopAuditService();
        assertTrue(svc.creation(null, null).isEmpty(), "Audit triples were generated for a create event");
        assertTrue(svc.deletion(null, null).isEmpty(), "Audit triples were generated for a delete event");
        assertTrue(svc.update(null, null).isEmpty(), "Audit triples were generated for an update event");
    }
}
