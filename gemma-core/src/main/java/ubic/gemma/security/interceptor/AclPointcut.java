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

/**
 * Pointcut to narrow methods looked at for ACL permissions modifications.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.security.interceptor.AclAdvice
 */
public class AclPointcut extends StaticMethodMatcherPointcut {

    protected static Log log = LogFactory.getLog( AclPointcut.class.getName() );

    /*
     * (non-Javadoc)
     * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
     */
    public boolean matches( Method method, Class targetClass ) {
        return methodTriggersACLAction( method, targetClass );
    }

    /**
     * Test whether a method requires ACL permissions to be added.
     * 
     * @param m
     * @return
     */
    private boolean methodsTriggersACLAddition( Method m ) {
        return m.getName().equals( "create" ) || m.getName().equals( "save" ) || m.getName().equals( "findOrCreate" )
                || m.getName().equals( "update" );
    }

    /**
     * Test whether a method requires any ACL action at all.
     * 
     * @param m
     * @return
     */
    private boolean methodTriggersACLAction( Method m, Class targetClass ) {
        return methodsTriggersACLAddition( m ) || methodTriggersACLDelete( m );
    }

    /**
     * Test whether a method requires ACL permissions to be deleted.
     * 
     * @param m
     * @return
     */
    private boolean methodTriggersACLDelete( Method m ) {
        return m.getName().equals( "remove" ) || m.getName().equals( "delete" );
    }

}
