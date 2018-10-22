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
package org.trellisldp.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class DefaultIdentifierServiceTest {

    @Test
    public void testSupplier() {
        final String prefix = "trellis:data/";
        final Supplier<String> supplier = new DefaultIdentifierService().getSupplier(prefix);
        final String id1 = supplier.get();
        final String id2 = supplier.get();

        assertTrue(id1.startsWith(prefix), "Generated id has wrong prefix!");
        assertTrue(id2.startsWith(prefix), "Generated id has wrong prefix!");
        assertNotEquals(id1, id2, "Generated ids shouldn't match!");
    }

    @Test
    public void testGenerator() {
        final String prefix1 = "http://example.org/";
        final String prefix2 = "trellis:data/a/b/c/";
        final IdentifierService svc = new DefaultIdentifierService();
        final Supplier<String> gen1 = svc.getSupplier(prefix1);
        final Supplier<String> gen2 = svc.getSupplier(prefix2);

        final String id1 = gen1.get();
        final String id2 = gen2.get();

        assertTrue(id1.startsWith(prefix1), "Generated id has wrong prefix!");
        assertNotEquals(prefix1, id1, "Generated id shouldn't equal prefix!");
        assertTrue(id2.startsWith(prefix2), "Generated id has wrong prefix!");
        assertNotEquals(prefix2, id2, "Generated id shouldn't equal prefix!");
    }

    @Test
    public void testGenerator2() {
        final IdentifierService svc = new DefaultIdentifierService();
        final Supplier<String> gen = svc.getSupplier("", 4, 2);

        final String id = gen.get();

        final String[] parts = id.split("/");
        assertEquals(5L, parts.length, "hierarchical supplier has wrong number of levels!");
        assertEquals(parts[0], parts[4].substring(0, 2), "hierarchical supplier has wrong first section!");
        assertEquals(parts[1], parts[4].substring(2, 4), "hierarchical supplier has wrong second section!");
        assertEquals(parts[2], parts[4].substring(4, 6), "hierarchical supplier has wrong third section!");
        assertEquals(parts[3], parts[4].substring(6, 8), "hierarchical supplier has wrong fourth section!");
    }

    @Test
    public void testSupplier2() {
        final Supplier<String> supplier = new DefaultIdentifierService().getSupplier();
        final String id1 = supplier.get();
        final String id2 = supplier.get();

        assertNotEquals(id1, id2, "Suppliers shouldn't be equal!");
    }
}
