/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.security.interceptor;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import ubic.gemma.persistence.CrudUtils;

/**
 * Pointcut to identify CRUD operations on Auditables. Used for auditing.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.security.interceptor.AddOrRemoveFromACLInterceptor
 */
public class CrudPointcut extends StaticMethodMatcherPointcut {

    protected static Log log = LogFactory.getLog( CrudPointcut.class.getName() );

    /**
     * Checks if methods are CRUD operations. This should intercept methods called from services - any class and method
     * that is wired to the serviceSecurityInterceptor
     * 
     * @param method the candidate method
     * @param targetClass the target class (may be <code>null</code>, in which case the candidate class must be taken to
     *        be the method's declaring (ignored by the CrudPointcut)
     * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
     * @see org.acegisecurity.intercept.method.aopalliance.MethodSecurityInterceptor
     */
    public boolean matches( Method method, Class targetClass ) {
        return CrudUtils.methodIsCrud( method );
    }

}
