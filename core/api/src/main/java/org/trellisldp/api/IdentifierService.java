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

import java.util.function.Supplier;

/**
 * The IdentifierService provides a mechanism for creating new identifiers.
 *
 * @author acoburn
 */
public interface IdentifierService {

    /**
     * Get a Supplier that generates Strings with the provided prefix.
     *
     * @param prefix the prefix
     * @param hierarchy the levels of hierarchy to add
     * @param length the length of each level of hierarchy
     * @return a String Supplier
     */
    Supplier<String> getSupplier(String prefix, int hierarchy, int length);

    /**
     * Get a Supplier that generates Strings with the provided prefix.
     *
     * @param prefix the prefix
     * @return a String Supplier
     */
    Supplier<String> getSupplier(String prefix);

    /**
     * Get a Supplier that generates Strings with the provided prefix.
     *
     * @return a String Supplier
     */
    Supplier<String> getSupplier();
}
