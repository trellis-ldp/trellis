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
package org.trellisldp.webac;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import org.apache.commons.rdf.api.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
public class WrappedGraphTest {

    @Mock
    private Graph mockGraph;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        doThrow(new IOException()).when(mockGraph).close();
    }

    @Test
    public void testCloseGraphError() {
        assertThrows(RuntimeTrellisException.class, () -> {
            try (final WrappedGraph graph = WrappedGraph.wrap(mockGraph)) {
                // nothing here
            }
        }, "IOException not called when graph is closed!");
    }
}
