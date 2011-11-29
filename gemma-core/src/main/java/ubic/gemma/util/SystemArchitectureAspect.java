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
package ubic.gemma.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * General-purpose pointcuts to recognize CRUD operations etc.
 * 
 * @author paul
 * @version $Id$
 */
@Aspect
public class SystemArchitectureAspect {

    /**
     * Encompasses the 'web' packages
     */
    @Pointcut("within(ubic.gemma.web..*)")
    public void inWebLayer() {
    }

    /**
     * Encompasses the 'model' packages
     */
    @Pointcut("within(ubic.gemma.model..*)")
    public void inModelLayer() {
    }

    /**
     * A entity service method.
     */
    @Pointcut("execution(public * ubic.gemma.model..*.*Service.*(..)) || execution(public * ubic.gemma.association..*.*Service.*(..)) || execution(public * ubic.gemma.web.services.rest.*Service.*(..))")
    public void serviceMethod() {
    }

    /**
     * Methods that create new objects in the persistent store
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.serviceMethod() && (execution(* save(..)) || execution(* create(..)) || execution(* findOrCreate(..)))")
    public void creator() {
    }

    /**
     * Methods that update items in the persistent store
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.serviceMethod() && execution(* update(..))")
    public void updater() {
    }

    /**
     * Methods that delete items in the persistent store
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.serviceMethod() && (execution(* remove(..)) || execution(* delete(..)))")
    public void deleter() {
    }

    /**
     * Methods that load (read) from the persistent store
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.serviceMethod() && (execution(* load(..)) || execution(* loadAll(..)) || execution(* read(..)))")
    public void loader() {
    }

    /**
     * Create, delete or update methods
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.creator() || ubic.gemma.util.SystemArchitectureAspect.updater() || ubic.gemma.util.SystemArchitectureAspect.deleter()")
    public void modifier() {
    }

    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.deleter() ||ubic.gemma.util.SystemArchitectureAspect.loader() || ubic.gemma.util.SystemArchitectureAspect.creator() || ubic.gemma.util.SystemArchitectureAspect.updater() || ubic.gemma.util.SystemArchitectureAspect.deleter()")
    public void crud() {

    }

}
