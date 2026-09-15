/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.core.security.acl.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Hibernate-mapped {@code acl_entry} row. Phase B of the gsec absorption decoupled the Sid
 * hierarchy: this entry's stored {@link #sid} field is a JPA entity ({@link AclSid}) that does
 * NOT implement Spring Security's {@code Sid} interface. {@link #getSid()} converts to a Spring
 * sid at the boundary via {@link AclSid#toSid()} so callers that talk to the
 * {@link AccessControlEntry} contract see the stock Spring type. {@link #getSidEntity()} exposes
 * the entity for HQL-side use.
 *
 * @author paul
 */
@Entity
@Table(name = "acl_entry")
@Access(AccessType.FIELD)
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AclEntry implements AccessControlEntry, Comparable<AclEntry> {

    private static final PermissionFactory permissionFactory = new DefaultPermissionFactory();

    private static final long serialVersionUID = -4697361841061166973L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sid", nullable = false, columnDefinition = "BIGINT")
    private AclSid sid;

    @Column(name = "granting", nullable = false, columnDefinition = "BIT")
    private boolean granting;

    @Column(name = "mask", nullable = false, columnDefinition = "INTEGER")
    private int mask;

    @Transient
    private Acl acl;

    @Column(name = "ace_order", nullable = false, columnDefinition = "INTEGER")
    private int aceOrder;

    @SuppressWarnings("unused")
    public AclEntry() {

    }

    public AclEntry( Acl acl, AclSid sid, Permission permission, boolean granting ) {
        Assert.notNull( acl, "Acl required" );
        Assert.notNull( sid, "Sid required" );
        Assert.notNull( permission, "Permission required" );
        this.acl = acl;
        this.sid = sid;
        this.mask = permission.getMask();
        this.granting = granting;
    }


    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public Permission getPermission() {
        return permissionFactory.buildFromMask( mask );
    }

    public void setPermission( Permission permission ) {
        this.mask = permission.getMask();
    }

    /**
     * Returns the stored sid as a Spring Security {@link Sid} (built via
     * {@link AclSid#toSid()}). After Phase B of the gsec absorption, callers that exercise the
     * {@code AccessControlEntry} contract see exactly one Sid type (Spring's stock).
     */
    @Override
    public Sid getSid() {
        return this.sid == null ? null : this.sid.toSid();
    }

    /**
     * Returns the underlying Hibernate-mapped {@code acl_sid} entity. For HQL-internal use only;
     * external callers should use {@link #getSid()} which yields a Spring-typed sid.
     */
    public AclSid getSidEntity() {
        return this.sid;
    }

    public void setSid( AclSid sid ) {
        this.sid = sid;
    }

    @Override
    public boolean isGranting() {
        return this.granting;
    }

    public void setGranting( boolean granting ) {
        this.granting = granting;
    }

    public int getMask() {
        return mask;
    }

    public void setMask( int mask ) {
        this.mask = mask;
    }

    @Override
    public Acl getAcl() {
        return this.acl;
    }

    @SuppressWarnings("unused")
    public int getAceOrder() {
        return aceOrder;
    }

    public void setAceOrder( int aceOrder ) {
        this.aceOrder = aceOrder;
    }

    @Override
    public int compareTo( AclEntry other ) {
        return aceOrder - other.aceOrder;
    }

    /**
     * Note that this does not use the ID, to avoid getting duplicate entries.
     */
    @Override
    final public int hashCode() {
        return Objects.hash( granting, mask, sid );
    }

    @Override
    final public boolean equals( Object obj ) {
        //  Note that this does not use the ID, to avoid getting duplicate entries.
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        AclEntry other = ( AclEntry ) obj;

        if ( granting != other.granting ) {
            return false;
        }

        if ( mask != other.mask ) {
            return false;
        }

        if ( sid == null ) {
            return other.sid == null;
        } else return sid.equals( other.sid );
    }

    @Override
    public String toString() {
        return String.format( "AclEntry[id: %d; granting: %s; sid: %s; permission: %s; ]",
            this.id, this.granting, this.sid, permissionFactory.buildFromMask( mask ) );
    }
}
