/*
 * The gemma-mda project
 *
 * Copyright (c) 2013 University of British Columbia
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

package gemma.gsec.acl.domain;

import gemma.gsec.model.Securable;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link ObjectIdentity}.
 *
 * @author Paul
 * @version $Id: AclObjectIdentity.java,v 1.1 2013/09/14 16:55:18 paul Exp $
 */
public class AclObjectIdentity implements ObjectIdentity {

    private static final long serialVersionUID = -6715898560226971244L;

    // of this objectidentity
    private Long id;

    // of the entity
    private Long identifier;

    private String type;

    /**
     * Spring Security canonical {@code acl_object_identity.object_id_class} FK to {@code acl_class.id}.
     * The {@link #type} field is the human-readable class name derived from this FK via a Hibernate
     * formula at load time (see AclObjectIdentity.hbm.xml). Application code reads {@link #getType()};
     * this field exists so Hibernate's hbm2ddl generates the canonical column for Spring Security's
     * stock JdbcMutableAclService to read/write through JDBC.
     */
    private Long objectIdClass;

    private AclSid ownerSid;

    private AclObjectIdentity parentObject;

    private Set<AclEntry> entries = new HashSet<>();

    private boolean entriesInheriting = false;

    @SuppressWarnings("unused")
    public AclObjectIdentity() {
    }

    public AclObjectIdentity( Class<? extends Securable> javaType, Long identifier ) {
        Assert.notNull( javaType, "The java type must be non-null" );
        Assert.notNull( identifier, "The identifier must be non-null" );
        this.type = javaType.getName();
        this.identifier = identifier;
    }

    /**
     * If the object is a proxy, its superclass must be the correct one to give the OBJECT_CLASS (type) in the ACL
     * tables. For polymorphic classes this will be a problem if the method returns the superclass, so use fetch all
     * properties. Example: PhenotypeAssociationDao. See chapter 19.1.3 of the Hibernate documentation for an
     * explanation about polymorphic classes and proxies.
     * <p>
     * Renovations Phase 2: {@link ClassUtils#getUserClass(Class)} only unwraps CGLIB proxies (whose
     * class names contain {@code "$$"}); Hibernate proxies use {@code "$HibernateProxy$"} (single
     * {@code $}) and slip through unchanged, leaving the OID with a synthetic type like
     * {@code ExperimentalDesign$HibernateProxy$hm1xnISy} that doesn't match the canonical class name
     * Spring Security's BasicLookupStrategy compares against {@code acl_class.class}. Use
     * {@link org.hibernate.Hibernate#getClass(Object)} which handles HibernateProxy and then falls
     * back to ClassUtils for CGLIB-only stacks.
     */
    public AclObjectIdentity( Securable object ) {
        Assert.notNull( object.getId(), "ID is required to be non-null" );
        // Renovations Phase 2: avoid initializing the proxy just to learn its class.
        // org.hibernate.Hibernate.getClass(object) eagerly initializes HibernateProxies, which
        // throws LazyInitializationException when callers (e.g. AclTestUtils.checkDeleteEEAcls)
        // construct an OID for a detached or post-remove entity. The persistent class is known
        // from the proxy's LazyInitializer without touching the session.
        Class<?> hibernateClass;
        if ( object instanceof org.hibernate.proxy.HibernateProxy ) {
            hibernateClass = ( ( org.hibernate.proxy.HibernateProxy ) object )
                    .getHibernateLazyInitializer().getPersistentClass();
        } else {
            hibernateClass = object.getClass();
        }
        Class<?> typeClass = ClassUtils.getUserClass( hibernateClass );
        type = typeClass.getName();
        this.identifier = object.getId();

    }

    public AclObjectIdentity( String type, Long identifier ) {
        this.type = type;
        this.identifier = identifier;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    @Override
    public Long getIdentifier() {
        return this.identifier;
    }

    @SuppressWarnings("unused")
    public void setIdentifier( Long identifier ) {
        this.identifier = identifier;
    }

    public Set<AclEntry> getEntries() {
        return entries;
    }

    @SuppressWarnings("unused")
    public void setEntries( Set<AclEntry> entries ) {
        this.entries = entries;
    }

    public boolean getEntriesInheriting() {
        return entriesInheriting;
    }

    public void setEntriesInheriting( boolean entriesInheriting ) {
        this.entriesInheriting = entriesInheriting;
    }

    public Long getObjectIdClass() {
        return objectIdClass;
    }

    public void setObjectIdClass( Long objectIdClass ) {
        this.objectIdClass = objectIdClass;
    }

    public AclSid getOwnerSid() {
        return ownerSid;
    }

    /**
     * Set the owner from an {@link AclSid} entity (Hibernate-side).
     */
    public void setOwnerSid( AclSid ownerSid ) {
        this.ownerSid = ownerSid;
    }

    /**
     * Set the owner from a Spring Security {@link Sid}. The legacy gsec {@code AclImpl} accepts
     * a Spring sid through Spring's {@code OwnershipAcl} interface; this overload bridges that
     * back to the Hibernate-mapped entity. Phase B of the gsec absorption split the Sid hierarchy
     * cleanly: gsec entities are no longer {@code Sid} implementations, so the conversion has to
     * happen here. The legacy {@code AclImpl}/{@code AclDaoImpl} stack is no longer wired in
     * production (the {@code aclService} bean is now {@code JdbcMutableAclService}); this path
     * remains for {@code BaseDatabaseTest}-based unit tests that still exercise the legacy stack.
     */
    public void setOwnerSidFromSpring( Sid ownerSid ) {
        if ( ownerSid == null ) {
            this.ownerSid = null;
        } else if ( ownerSid instanceof AclSid ) {
            // Defensive: callers shouldn't pass AclSid as a Sid (Phase B decoupled them) but if
            // they do, accept it.
            this.ownerSid = ( AclSid ) ownerSid;
        } else if ( ownerSid instanceof PrincipalSid ) {
            this.ownerSid = new AclPrincipalSid( ( ( PrincipalSid ) ownerSid ).getPrincipal() );
        } else if ( ownerSid instanceof GrantedAuthoritySid ) {
            this.ownerSid = new AclGrantedAuthoritySid( ( ( GrantedAuthoritySid ) ownerSid ).getGrantedAuthority() );
        } else {
            throw new IllegalArgumentException( "Unsupported Sid type: " + ownerSid.getClass().getName() );
        }
    }

    public AclObjectIdentity getParentObject() {
        return parentObject;
    }

    public void setParentObject( AclObjectIdentity parentObject ) {
        assert parentObject != this && !this.equals( parentObject );
        this.parentObject = parentObject;
    }

    /**
     * Important so caching operates properly.
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        return Objects.hash( type, identifier );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == null ) return false;
        if ( o == this ) return true;
        if ( !( o instanceof ObjectIdentity ) ) return false;
        ObjectIdentity oi = ( ObjectIdentity ) o;
        // Null-tolerant comparison: under Hibernate 6 bytecode enhancement, setPropertyValues may
        // invoke setParentObject (which calls equals) before the type/identifier fields are populated
        // on a freshly constructed instance. Guard with Objects.equals so we don't NPE in that window.
        return Objects.equals( this.type, oi.getType() )
                && Objects.equals( this.identifier, oi.getIdentifier() );
    }

    @Override
    public String toString() {
        return String.format( "%s[Type: %s; Identifier: %s]", this.getClass().getName(), this.type, this.identifier );
    }
}
