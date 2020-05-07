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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AppUtilsTest {

    @Test
    void testPrintBanner() {
        assertDoesNotThrow(() -> AppUtils.printBanner("Trellis Application", "org/trellisldp/app/banner.txt"));
    }

    @Test
    void testPrintBannerRelative() {
        assertDoesNotThrow(() -> AppUtils.printBanner("Trellis Application Relative",
                    "./org/trellisldp/app/banner.txt"));
    }

    @Test
    void testPrintBannerUnknownResource() {
        assertDoesNotThrow(() -> AppUtils.printBanner("Non-existent resource", "non-existent-resource.txt"));
    }
}
