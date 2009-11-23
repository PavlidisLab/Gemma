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
package ubic.gemma.security.authorization.acl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Pointcut to narrow methods looked at for ACL permissions modifications.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.security.authorization.acl.AclAdvice
 */
@Aspect
public class AclPointcut {

    protected static Log log = LogFactory.getLog( AclPointcut.class.getName() );

    /**
     * Test whether a method requires ACL add or delete. This includes 'update' methods that might cascade to a child
     * object that has been added (and which needs an object identity added)
     */
    @Pointcut("ubic.gemma.util.SystemArchitectureAspect.modifier()")
    public void requiresAclAction() {
    }

}
