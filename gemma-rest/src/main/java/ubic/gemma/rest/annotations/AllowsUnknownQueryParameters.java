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
package ubic.gemma.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exempts a resource method (or every method of a resource class) from the unknown-query-parameter rejection
 * performed by {@code UnknownQueryParameterFilter}.
 * <p>
 * That filter rejects any query parameter the matched resource method cannot bind, deriving the accepted set from
 * the method's own {@code @QueryParam} declarations. A handful of endpoints read the query string as a whole rather
 * than through declared parameters — the 302 pass-through aliases forward every parameter they are given to the
 * endpoint they redirect to — so for them there is no set of declared names to check against and every parameter is
 * meaningful. Annotate those.
 * <p>
 * This is an opt-out from a check, not a licence to ignore input: a method carrying this annotation is asserting
 * that it reads the query string itself.
 *
 * @author gemma
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowsUnknownQueryParameters {
}
