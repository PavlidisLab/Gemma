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

package ubic.gemma.model.common.auditAndSecurity.acl;

import java.io.Serializable;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.util.FieldUtils;
import org.springframework.util.Assert;

/**
 * based on the spring-security AclCache.
 * 
 * @author Paul
 * @version $Id$
 */
public class AclCache implements org.springframework.security.acls.model.AclCache {

    private Ehcache cache;
    // private AuditLogger auditLogger;
    private AclAuthorizationStrategy aclAuthorizationStrategy;

    public AclCache( Ehcache cache ) {
        Assert.notNull( cache, "Cache required" );
        this.cache = cache;
    }

    @Override
    public void clearCache() {
        cache.removeAll();
    }

    @Override
    public void evictFromCache( ObjectIdentity objectIdentity ) {
        Assert.notNull( objectIdentity, "ObjectIdentity required" );

        MutableAcl acl = getFromCache( objectIdentity );

        if ( acl != null ) {
            cache.remove( acl.getId() );
            cache.remove( acl.getObjectIdentity() );
        }
    }

    @Override
    public void evictFromCache( Serializable pk ) {
        Assert.notNull( pk, "Primary key (identifier) required" );

        MutableAcl acl = getFromCache( pk );

        if ( acl != null ) {
            cache.remove( acl.getId() );
            cache.remove( acl.getObjectIdentity() );
        }
    }

    @Override
    public MutableAcl getFromCache( ObjectIdentity objectIdentity ) {
        Assert.notNull( objectIdentity, "ObjectIdentity required" );

        Element element = null;

        try {
            element = cache.get( objectIdentity );
        } catch ( CacheException ignored ) {
        }

        if ( element == null ) {
            return null;
        }

        return initializeTransientFields( ( MutableAcl ) element.getValue() );
    }

    @Override
    public MutableAcl getFromCache( Serializable pk ) {
        Assert.notNull( pk, "Primary key (identifier) required" );

        Element element = null;

        try {
            element = cache.get( pk );
        } catch ( CacheException ignored ) {
        }

        if ( element == null ) {
            return null;
        }

        return initializeTransientFields( ( MutableAcl ) element.getValue() );
    }

    @Override
    public void putInCache( MutableAcl acl ) {
        Assert.notNull( acl, "Acl required" );
        Assert.notNull( acl.getObjectIdentity(), "ObjectIdentity required" );
        Assert.notNull( acl.getId(), "ID required" );

        if ( this.aclAuthorizationStrategy == null ) {
            this.aclAuthorizationStrategy = ( AclAuthorizationStrategy ) FieldUtils.getProtectedFieldValue(
                    "aclAuthorizationStrategy", acl );

        }

        if ( ( acl.getParentAcl() != null ) && ( acl.getParentAcl() instanceof MutableAcl ) ) {
            putInCache( ( MutableAcl ) acl.getParentAcl() );
        }

        cache.put( new Element( acl.getObjectIdentity(), acl ) );
        cache.put( new Element( acl.getId(), acl ) );
    }

    private MutableAcl initializeTransientFields( MutableAcl value ) {

        FieldUtils.setProtectedFieldValue( "aclAuthorizationStrategy", value, this.aclAuthorizationStrategy );

        if ( value.getParentAcl() != null ) {
            initializeTransientFields( ( MutableAcl ) value.getParentAcl() );
        }
        return value;
    }

}
