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
package ubic.gemma.core.security.acl.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

/**
 * Hibernate-mapped abstract base for {@code acl_sid} rows. Phase B of the gsec absorption
 * decoupled this hierarchy from Spring Security's {@link org.springframework.security.acls.model.Sid}
 * interface: there is now exactly one {@code Sid} hierarchy at runtime (Spring's stock
 * {@link org.springframework.security.acls.domain.PrincipalSid} /
 * {@link org.springframework.security.acls.domain.GrantedAuthoritySid}). This class and its
 * subclasses exist purely as JPA entity types backing HQL queries against {@code acl_sid}
 * (e.g. {@code AclQueryUtils} subqueries). They are NEVER constructed for use in the security
 * path. Callers that need a {@code Sid} from one of these entities call {@link #toSid()}.
 *
 * @author Paul
 */
@Entity
@Table(name = "acl_sid")
@Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "principal", discriminatorType = DiscriminatorType.INTEGER, columnDefinition = "BIT")
@DiscriminatorValue("-1")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public abstract class AclSid implements java.io.Serializable {

    private static final long serialVersionUID = -3256613712125656321L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;

    /**
     * The {@code sid} column on {@code acl_sid}. Holds the principal name for
     * {@link AclPrincipalSid} rows (discriminator {@code principal=1}) and the granted-authority
     * name for {@link AclGrantedAuthoritySid} rows (discriminator {@code principal=0}). Lives on
     * the root because Hibernate's single-table inheritance + per-subclass duplicate-column
     * mapping with {@code insertable=false, updatable=false} on BOTH subclasses caused one of
     * the two fields to read back NULL — breaking the ACL check that compares the loaded sid
     * value against the current user's principal name. With the field on the root, every
     * AclSid row loads with a populated {@code sid} regardless of subclass; the semantic
     * accessors {@link AclPrincipalSid#getPrincipal()} and
     * {@link AclGrantedAuthoritySid#getGrantedAuthority()} delegate here.
     */
    @Column(name = "sid", nullable = false, columnDefinition = "VARCHAR(255)")
    private String sid;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /** Package-private accessor for subclasses' semantic getters. */
    String getSid() {
        return sid;
    }

    /** Package-private mutator for subclasses' semantic setters. */
    void setSid( String sid ) {
        this.sid = sid;
    }

    /**
     * Convert this Hibernate-mapped row to a Spring Security
     * {@link org.springframework.security.acls.model.Sid} for use in the security path
     * (ACL equality checks, owner comparisons, ACE construction).
     */
    public abstract org.springframework.security.acls.model.Sid toSid();
}
