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

import static org.eclipse.microprofile.reactive.messaging.Message.of;
import static org.slf4j.LoggerFactory.getLogger;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.trellisldp.api.*;

/**
 * A MicroProfile-based notification service.
 */
@ApplicationScoped
public class ReactiveNotificationService implements NotificationService {

    private static final Logger LOGGER = getLogger(ReactiveNotificationService.class);

    public static final String REACTIVE_DESTINATION = "trellis";

    private final PublishSubject<Message<String>> subject = PublishSubject.create();

    @Inject
    NotificationSerializationService serializer;

    @Inject
    Event<Notification> trellisNotification;

    @Override
    public void emit(final Notification notification) {
        LOGGER.debug("Sending message to reactive destination: {}", notification.getIdentifier());
        trellisNotification.fireAsync(notification);
        subject.onNext(of(serializer.serialize(notification)));
    }

    /**
     * Send the notification to the reactive stream destination.
     * @return the flowable stream of messages
     */
    @Outgoing(REACTIVE_DESTINATION)
    public Flowable<Message<String>> publish() {
        return subject.toFlowable(BackpressureStrategy.BUFFER);
    }
}
