/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test the no-op notification service.
 */
@ExtendWith(MockitoExtension.class)
class NoopNotificationServiceTest {

    @Mock
    private Notification mockNotification;

    @Test
    void testNoopNotificationSvc() {
        try {
            final NotificationService svc = new NoopNotificationService();
            svc.emit(mockNotification);
        } catch (final Exception ex) {
            fail("Notification serialization failed! " + ex.getMessage());
        }
    }
}
