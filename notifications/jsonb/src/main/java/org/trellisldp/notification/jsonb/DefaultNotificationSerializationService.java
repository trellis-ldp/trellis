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
package org.trellisldp.notification.jsonb;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.trellisldp.api.Notification;
import org.trellisldp.api.NotificationSerializationService;

/**
 * An {@link NotificationSerializationService} that serializes an {@link Notification} object
 * into an ActivityStream-compliant JSON string.
 *
 * @author acoburn
 */
@ApplicationScoped
public class DefaultNotificationSerializationService implements NotificationSerializationService {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public String serialize(final Notification notification) {
        return jsonb.toJson(ActivityStreamMessage.from(notification));
    }
}
