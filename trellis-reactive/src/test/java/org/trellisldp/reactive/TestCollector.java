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
package org.trellisldp.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TestCollector {

    private final List<String> list = new CopyOnWriteArrayList<>();

    @Incoming(ReactiveNotificationService.REACTIVE_DESTINATION)
    public void sink(final String message) {
        list.add(message);
    }

    public List<String> getResults() {
        return list;
    }

    public void clear() {
        list.clear();
    }
}
