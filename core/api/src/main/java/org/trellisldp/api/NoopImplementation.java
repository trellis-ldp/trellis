package org.trellisldp.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;

/**
 * Marks a no-op implementation of a Trellis component.
 * 
 * Such implementations are expected to avoid persisting any data beyond the lifetime of the running application.
 */
@Stereotype
@ApplicationScoped
@Alternative
@Retention(RUNTIME)
@Target(TYPE)
public @interface NoopImplementation {}
