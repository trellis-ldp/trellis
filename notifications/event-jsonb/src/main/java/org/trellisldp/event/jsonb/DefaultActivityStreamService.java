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
package org.trellisldp.event.jsonb;

import static java.util.Optional.of;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;

/**
 * An {@link ActivityStreamService} that serializes an {@link Event} object
 * into an ActivityStream-compliant JSON string.
 *
 * @author acoburn
 */
@ApplicationScoped
public class DefaultActivityStreamService implements ActivityStreamService {

    private static Jsonb jsonb = JsonbBuilder.create();

    @Override
    public Optional<String> serialize(final Event event) {
        return of(jsonb.toJson(ActivityStreamMessage.from(event)));
    }
}
