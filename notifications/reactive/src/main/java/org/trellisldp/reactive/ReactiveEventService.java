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
package org.trellisldp.reactive;

import static org.eclipse.microprofile.reactive.messaging.Message.of;
import static org.slf4j.LoggerFactory.getLogger;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.trellisldp.api.*;

/**
 * A MicroProfile-based event service.
 */
@ApplicationScoped
public class ReactiveEventService implements EventService {

    private static final Logger LOGGER = getLogger(ReactiveEventService.class);

    public static final String REACTIVE_DESTINATION = "trellis";

    private final PublishSubject<Message<String>> subject = PublishSubject.create();

    @Inject
    EventSerializationService serializer;

    @Inject
    javax.enterprise.event.Event<Event> trellisEvent;

    @Override
    public void emit(final Event event) {
        LOGGER.debug("Sending message to reactive destination: {}", event.getIdentifier());
        trellisEvent.fireAsync(event);
        subject.onNext(of(serializer.serialize(event)));
    }

    /**
     * Send the event to the reactive stream destination.
     * @return the flowable stream of messages
     */
    @Outgoing(REACTIVE_DESTINATION)
    public Flowable<Message<String>> publish() {
        return subject.toFlowable(BackpressureStrategy.BUFFER);
    }
}
