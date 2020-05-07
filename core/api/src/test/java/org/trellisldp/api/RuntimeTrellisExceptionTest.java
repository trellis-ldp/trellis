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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class RuntimeTrellisExceptionTest {

    @Test
    void testException1() {
        final RuntimeException ex = new RuntimeTrellisException();
        assertNull(ex.getMessage(), "Message was not null");
    }

    @Test
    void testException2() {
        final String msg = "the cause";
        final RuntimeException ex = new RuntimeTrellisException(msg);
        assertEquals(msg, ex.getMessage(), "Unexpected message");
    }

    @Test
    void testException3() {
        final Throwable cause = new Throwable("an error");
        final RuntimeException ex = new RuntimeTrellisException(cause);
        assertEquals(cause, ex.getCause(), "Unexpected exception cause");
    }

    @Test
    void testException4() {
        final Throwable cause = new Throwable("an error");
        final String msg = "The message";
        final RuntimeException ex = new RuntimeTrellisException(msg, cause);
        assertEquals(cause, ex.getCause(), "Unexpected exception cause");
        assertEquals(msg, ex.getMessage(), "Unexpected exception message");
    }
}
