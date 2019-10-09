/*
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

import static java.util.Objects.requireNonNull;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.trellisldp.api.*;

/**
 * A microprofile-based event service.
 */
@ApplicationScoped
public class ReactiveEventService implements EventService {

    public static final String REACTIVE_DESTINATION = "trellis";

    private final ActivityStreamService serializer;
    private final PublishSubject<Message<String>> subject = PublishSubject.create();

    /**
     * Create a new Reactive Stream Event Service with a no-op serializer.
     *
     * <p>Note: this is used by CDI proxies and should not be invoked directly.
     */
    public ReactiveEventService() {
        this(new NoopActivityStreamService());
    }

    /**
     * Create a new Reactive Stream Event Service.
     * @param serializer the event serializer
     */
    @Inject
    public ReactiveEventService(final ActivityStreamService serializer) {
        this.serializer = requireNonNull(serializer, "serializer may not be null!");
    }

    @Override
    public void emit(final Event event) {
        serializer.serialize(event).map(Message::of).ifPresent(subject::onNext);
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
