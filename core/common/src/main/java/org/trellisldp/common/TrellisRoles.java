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
package org.trellisldp.common;

/**
 * A collection of security roles for use with Trellis.
 */
public final class TrellisRoles {

    /** A user role for Trellis. */
    public static final String USER = "USER";

    /** An admin role for Trellis. */
    public static final String ADMIN = "ADMIN";

    /** An owner role for Trellis. */
    public static final String OWNER = "OWNER";

    private TrellisRoles() {
        // Prevent instantiation.
    }
}
