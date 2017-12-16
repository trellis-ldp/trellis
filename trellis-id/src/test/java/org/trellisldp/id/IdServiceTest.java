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
package org.trellisldp.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.trellisldp.api.IdentifierService;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class IdServiceTest {

    @Test
    public void testSupplier() {
        final String prefix = "trellis:repository/";
        final Supplier<String> supplier = new UUIDGenerator().getSupplier(prefix);
        final String id1 = supplier.get();
        final String id2 = supplier.get();

        assertTrue(id1.startsWith(prefix));
        assertTrue(id2.startsWith(prefix));
        assertFalse(id1.equals(id2));
    }

    @Test
    public void testGenerator() {
        final String prefix1 = "http://example.org/";
        final String prefix2 = "trellis:repository/a/b/c/";
        final IdentifierService svc = new UUIDGenerator();
        final Supplier<String> gen1 = svc.getSupplier(prefix1);
        final Supplier<String> gen2 = svc.getSupplier(prefix2);

        final String id1 = gen1.get();
        final String id2 = gen2.get();

        assertTrue(id1.startsWith(prefix1));
        assertFalse(id1.equals(prefix1));
        assertTrue(id2.startsWith(prefix2));
        assertFalse(id2.equals(prefix2));
    }

    @Test
    public void testGenerator2() {
        final IdentifierService svc = new UUIDGenerator();
        final Supplier<String> gen = svc.getSupplier("", 4, 2);

        final String id = gen.get();

        final String[] parts = id.split("/");
        assertEquals(5L, parts.length);
        assertEquals(parts[0], parts[4].substring(0, 2));
        assertEquals(parts[1], parts[4].substring(2, 4));
        assertEquals(parts[2], parts[4].substring(4, 6));
        assertEquals(parts[3], parts[4].substring(6, 8));
    }

    @Test
    public void testSupplier2() {
        final Supplier<String> supplier = new UUIDGenerator().getSupplier();
        final String id1 = supplier.get();
        final String id2 = supplier.get();

        assertFalse(id1.equals(id2));
    }
}
