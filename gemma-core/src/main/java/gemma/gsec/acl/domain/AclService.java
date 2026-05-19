/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gemma.gsec.acl.domain;

import org.hibernate.Session;
import org.springframework.security.acls.model.*;

/**
 * @author paul
 * @version $Id: AclService.java,v 1.1 2013/09/14 16:55:19 paul Exp $
 */
public interface AclService extends MutableAclService {

    /**
     * Read ACLs from a specific Hibernate session.
     */
    Acl readAclById( ObjectIdentity objectIdentity, Session session ) throws NotFoundException;

    /**
     * Open an Hibernate session for use with {@link #readAclById(ObjectIdentity, Session)}
     */
    Session openSession();

    /**
     * Remove an {@link Sid} and all associated ACEs.
     */
    @SuppressWarnings("unused")
    void deleteSid( Sid sid );
}
