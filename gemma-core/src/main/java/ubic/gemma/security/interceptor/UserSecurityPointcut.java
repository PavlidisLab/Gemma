/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import org.springframework.aop.support.StaticMethodMatcherPointcut;

import ubic.gemma.model.common.auditAndSecurity.UserService;

/**
 * Matches UserService calls to 'update' or 'remove'/'delete' etc.
 * 
 * @author paul
 * @version $Id$
 */
public class UserSecurityPointcut extends StaticMethodMatcherPointcut {

    public boolean matches( Method method, Class targetClass ) {
        if ( UserService.class.isAssignableFrom( targetClass ) ) {
            if ( method.getName().equals( "update" ) || method.getName().equals( "remove" )
                    || method.getName().equals( "create" ) || method.getName().contains( "save" )
                    || method.getName().equals( "addRole" ) || method.getName().equals( "delete" ) ) {
                return true;
            }
            return false;
        }
        return false;
    }

}
