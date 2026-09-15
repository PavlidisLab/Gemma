/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.security.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link AuditedOnError} declarations on a
 * single method. Authored by hand rather than auto-generated so that the
 * pointcut in {@link AuditedAspect} can target it by its concrete type.
 *
 * <p>Callers should never write {@code @AuditedOnErrors} directly — write
 * multiple {@link AuditedOnError} annotations on the same method and let the
 * compiler wrap them in this container:
 *
 * <pre>{@code
 *   @AuditedOnError(value = FailedFooEvent.class, exception = FooException.class)
 *   @AuditedOnError(value = FailedBarEvent.class, exception = BarException.class)
 *   public void doWork( ExpressionExperiment ee ) { ... }
 * }</pre>
 *
 * <p>See {@link AuditedOnError} for the "most-specific match" dispatch rule
 * applied when multiple declarations could fire for a given throwable.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface AuditedOnErrors {
    AuditedOnError[] value();
}
