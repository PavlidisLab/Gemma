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
package ubic.gemma.core.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.transaction.annotation.Transactional;

/**
 * General-purpose pointcuts to recognize CRUD operations etc.
 * <p>
 * For help with expressions see <a href="http://static.springsource.org/spring/docs/2.5.x/reference/aop.html#6.2.3.4">Chapter 6. Aspect Oriented Programming with Spring</a>.
 *
 * @author paul
 */
@Aspect
public class Pointcuts {

    /**
     * Matches stuff within Gemma package.
     */
    @Pointcut("within(ubic.gemma..*)")
    public void inGemma() {
    }

    @Pointcut("execution(public * *(..))")
    public void anyPublicMethod() {
    }

    /**
     * A DAO method, public and within a class annotated with {@link org.springframework.stereotype.Repository}.
     */
    @Pointcut("inGemma() && @target(org.springframework.stereotype.Repository) && anyPublicMethod()")
    public void daoMethod() {
    }

    /**
     * CRUD-like method that modifies the database (i.e. not a read operation).
     */
    @Pointcut("creator() || updater() || deleter()")
    public void modifier() {
    }

    /**
     * Methods that load (read) from the persistent store
     */
    @Pointcut("daoMethod() && (execution(* load*(..)) || execution(* find*(..)) || execution(* read*(..)))")
    public void loader() {
    }

    /**
     * Methods that create new objects in the persistent store
     */
    @Pointcut("daoMethod() && (execution(* save*(..)) || execution(* create*(..)) || execution(* findOrCreate*(..)) || execution(* persist*(..)) || execution(* add*(..)))")
    public void creator() {
    }

    /**
     * Methods that update items in the persistent store
     */
    @Pointcut("daoMethod() && execution(* update*(..))")
    public void updater() {
    }

    /**
     * Methods that remove items in the persistent store
     */
    @Pointcut("daoMethod() && (execution(* remove*(..)) || execution(* delete*(..)))")
    public void deleter() {
    }

    /**
     * A service method, public and within a class annotated with {@link org.springframework.stereotype.Service}.
     *<p>
     * Using @target makes a proxy out of everything, which causes problems if services aren't implementing
     * interfaces -- seems for InitializingBeans in particular. @within doesn't work, at least for the ACLs.
     */
    @Pointcut("inGemma() && @target(org.springframework.stereotype.Service) && anyPublicMethod()")
    public void serviceMethod() {
    }

    /**
     * A service method with arguments.
     */
    @Pointcut("serviceMethod() && (execution(* *(*)) || execution(* *(*,..)))")
    public void serviceMethodWithArg() {
    }
}
