/*
 * The gemma-mda project
 *
 * Copyright (c) 2013 University of British Columbia
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
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Paul
 * @version $Id: AclDao.java,v 1.1 2013/09/14 16:55:19 paul Exp $
 */
public interface AclDao {

    AclObjectIdentity createObjectIdentity( String type, Serializable identifier, AclSid sid, boolean entriesInheriting );

    void delete( AclObjectIdentity objectIdentity, boolean deleteChildren );

    void delete( AclSid sid );

    @Nullable
    AclObjectIdentity find( ObjectIdentity oid );

    @Nullable
    AclObjectIdentity find( ObjectIdentity objectIdentity, Session session );

    @Nullable
    AclSid find( Sid sid );

    @Nullable
    AclSid find( Sid sid, Session session );

    List<AclObjectIdentity> findChildren( AclObjectIdentity parentIdentity );

    AclSid findOrCreate( AclSid sid );

    void update( MutableAcl acl );

    /**
     * Read ACLs for the given object identities.
     */
    Map<AclObjectIdentity, Acl> readAclsById( List<AclObjectIdentity> objects );

    /**
     * Read ACLs for the given object identities from the provided Hibernate session.
     */
    Map<AclObjectIdentity, Acl> readAclsById( List<AclObjectIdentity> objectIdentities, Session session );

    /**
     * Open a session to be used by {@link #readAclsById(List, Session)}.
     */
    Session openSession();
}
