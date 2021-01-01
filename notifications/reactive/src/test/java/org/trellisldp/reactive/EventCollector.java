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
package org.trellisldp.reactive;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;

import org.trellisldp.api.Event;

@ApplicationScoped
public class EventCollector {

    private final List<Event> events = new CopyOnWriteArrayList<>();

    public void sink(@ObservesAsync final Event event) {
        events.add(event);
    }

    public List<Event> getResults() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}
