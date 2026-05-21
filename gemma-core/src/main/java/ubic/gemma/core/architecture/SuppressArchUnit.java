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
 */
package ubic.gemma.core.architecture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Targeted opt-out marker for ArchUnit production-code rules.
 *
 * <p>Use sparingly to suppress a single, named ArchUnit rule on a specific
 * field, constructor, method, or class where the rule's invariant has an
 * intentional, documented exception in production code. The {@link #value()}
 * attribute names the rule being suppressed so a future suppression does not
 * blanket-allow other rule violations on the same target.
 *
 * <p>Every use site MUST also carry a Javadoc explanation of WHY the
 * suppression is justified. If the reason isn't worth a Javadoc, the
 * suppression isn't worth landing.
 *
 * <p>Known rule names:
 * <ul>
 *   <li>{@code "AutowireImpl"} — suppresses
 *       {@code AutowireImplRuleTest#autowired_fields_must_not_be_impl_typed}.
 *       Reserved for {@code @Lazy @Autowired ImplType} fields used to break
 *       Spring DI cycles, where the {@code Impl} reference is required to
 *       reach a method that isn't on the interface and the {@code @Lazy}
 *       proxy keeps the cycle from being eager.</li>
 * </ul>
 *
 * <p>Retention is {@code RUNTIME} so ArchUnit (which reads compiled bytecode)
 * can see the annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE })
public @interface SuppressArchUnit {

    /**
     * Name of the ArchUnit rule being suppressed. See the class Javadoc for
     * the registry of known names. New names must be added to the registry
     * (and the corresponding rule must check for that exact name) before
     * landing — otherwise the suppression silently fails to take effect.
     */
    String value();
}
