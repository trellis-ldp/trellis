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
package org.trellisldp.api;

/**
 * Base class for all trellis unchecked exceptions.
 *
 * @author acoburn
 */
public class RuntimeRepositoryException extends RuntimeException {

    private static final long serialVersionUID = 8046489554418284257L;

    /**
     * Create a new RuntimeRepositoryException
     */
    public RuntimeRepositoryException() {
        super();
    }

    /**
     * Create a new RuntimeRepositoryException with a custom message
     * @param message the message
     */
    public RuntimeRepositoryException(final String message) {
        super(message);
    }

    /**
     * Create a new RuntimeRepositoryException with a custom message and known cause
     * @param message the message
     * @param cause the cause
     */
    public RuntimeRepositoryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new RuntimeRepositoryException with a known cause
     * @param cause the cause
     */
    public RuntimeRepositoryException(final Throwable cause) {
        super(cause);
    }
}
