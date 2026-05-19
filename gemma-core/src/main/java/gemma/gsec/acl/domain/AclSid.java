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
public abstract class AclSid implements java.io.Serializable {

    private static final long serialVersionUID = -3256613712125656321L;
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * Convert this Hibernate-mapped row to a Spring Security
     * {@link org.springframework.security.acls.model.Sid} for use in the security path
     * (ACL equality checks, owner comparisons, ACE construction).
     */
    public abstract org.springframework.security.acls.model.Sid toSid();
}
