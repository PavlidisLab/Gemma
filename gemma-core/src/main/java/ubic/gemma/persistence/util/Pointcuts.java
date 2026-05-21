/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.persistence.util;

import org.aspectj.lang.annotation.Pointcut;
import ubic.gemma.persistence.retry.Retryable;

/**
 * General-purpose pointcuts to recognize CRUD operations etc.
 * <p>
 * For help with expressions see <a href="http://static.springsource.org/spring/docs/2.5.x/reference/aop.html#6.2.3.4">Chapter 6. Aspect Oriented Programming with Spring</a>.
 *
 * @author paul
 */
public class Pointcuts {

    /**
     * Matches stuff within Gemma package.
     */
    @Pointcut("within(ubic.gemma..*)")
    public void inGemma() {
    }

    /**
     * A public method.
     */
    @Pointcut("execution(public * *(..))")
    public void anyPublicMethod() {
    }

    /**
     * A public method defined in a service.
     */
    @Pointcut("inGemma() && @target(org.springframework.stereotype.Service) && anyPublicMethod()")
    public void serviceMethod() {
    }

    /**
     * A transactional method, public and annotated with {@link org.springframework.transaction.annotation.Transactional}.
     */
    @Pointcut("inGemma() && (@within(org.springframework.transaction.annotation.Transactional) ||  @annotation(org.springframework.transaction.annotation.Transactional)) && anyPublicMethod()")
    public void transactionalMethod() {
    }

    /**
     * A method that can be retried, public and annotated with {@link Retryable}.
     */
    @Pointcut("inGemma() && @annotation(ubic.gemma.persistence.retry.Retryable) && anyPublicMethod()")
    public void retryableMethod() {
    }

    /**
     * A retryable or transactional service method.
     */
    @Pointcut("retryableMethod() || (serviceMethod() && transactionalMethod())")
    public void retryableOrTransactionalServiceMethod() {
    }
}
