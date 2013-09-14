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
package ubic.gemma.model.common.auditAndSecurity.acl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;

/**
 * @author Paul
 * @version $Id$
 */
public interface AclDao extends LookupStrategy {

    public AclObjectIdentity createObjectIdentity( String type, Serializable identifier, Sid sid, Boolean true1 );

    public void delete( ObjectIdentity objectIdentity, boolean deleteChildren );

    public void delete( Sid sid );

    public AclObjectIdentity find( ObjectIdentity oid );

    public AclSid find( Sid sid );

    public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity );

    public AclSid findOrCreate( Sid sid );

    public void setSessionFactory( SessionFactory sessionFactory );

    public void update( MutableAcl acl );

}
