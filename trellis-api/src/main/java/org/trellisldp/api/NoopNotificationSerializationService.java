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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.apache.commons.rdf.api.IRI;

/**
 * For use when notification serialization is not desired.
 */
@NoopImplementation
public class NoopNotificationSerializationService implements NotificationSerializationService {
    private static final String AS_NAMESPACE = "https://www.w3.org/ns/activitystreams#";

    @Override
    public String serialize(final Notification notification) {
        return "{" +
            "\n  \"@context\": \"https://www.w3.org/ns/activitystreams\"" +
            ",\n  \"id\": \"" + notification.getIdentifier().getIRIString() + "\"" +
            getTypesAsJsonFragment(notification) +
            notification.getObject().map(obj -> ",\n  \"object\": \"" + obj.getIRIString() + "\"").orElse("") +
            "\n}";
    }

    private String getTypesAsJsonFragment(final Notification notification) {
        final List<String> types = notification.getTypes().stream().map(IRI::getIRIString)
            .map(type -> type.startsWith(AS_NAMESPACE) ? type.substring(AS_NAMESPACE.length()) : type)
            .map(type -> "\"" + type + "\"").collect(toList());
        if (types.isEmpty()) {
            return "";
        } else if (types.size() == 1) {
            return ",\n  \"type\": " + types.get(0);
        }
        return ",\n  \"type\": [" + String.join(",", types) + "]";
    }
}
