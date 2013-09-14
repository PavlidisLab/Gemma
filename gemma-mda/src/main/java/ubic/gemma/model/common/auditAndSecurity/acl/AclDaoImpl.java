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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * We have our own implementation of the AclDao in part because of deadlock problems caused by the default JDBC-based
 * spring security DAO. As documented here:
 * <p>
 * {@link http://www.ericsimmerman.com/2012/06/resolving-spring-security-acl-deadlock.html}:
 * </p>
 * <blockquote> The default JDBCTemplate based implementation of Spring Security ACLs removes and recreates the entire
 * ACL for each update. That requires both deletes and inserts into the same table within the same JPA transaction and
 * is a recipe for deadlock when using the default MySQL transaction isolation level of REPEATABLE_READ. </blockquote>
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class AclDaoImpl implements AclDao {

    private static Log log = LogFactory.getLog( AclDaoImpl.class );

    @Autowired
    private AclAuthorizationStrategy aclAuthorizationStrategy;

    @Autowired
    private AclCache aclCache;

    /*
     * Used for fetching ACLs. 50 is the value used by the default spring implementation.
     */
    private int batchSize = 50;

    // @Autowired - this causes some kind of circular reference problem during startup; fillin manually
    private SessionFactory sessionFactory;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.acl.AclDao#createObjectIdentity(java.lang.String,
     * java.io.Serializable, org.springframework.security.acls.model.Sid, java.lang.Boolean)
     */
    @Override
    public AclObjectIdentity createObjectIdentity( String type, Serializable identifier, Sid sid,
            Boolean entitiesInheriting ) {
        AclObjectIdentity aoi = new AclObjectIdentity( type, identifier );
        aoi.setOwnerSid( sid );
        aoi.setEntriesInheriting( entitiesInheriting );
        this.getSessionFactory().getCurrentSession().save( aoi );
        return aoi;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.acl.AclDao#delete(org.springframework.security.acls.model.ObjectIdentity
     * , boolean)
     */
    @Override
    public void delete( ObjectIdentity objectIdentity, boolean deleteChildren ) {
        if ( deleteChildren ) {
            List<ObjectIdentity> c = findChildren( objectIdentity );
            for ( ObjectIdentity cc : c ) {
                delete( cc, true );
            }
        }

        // cascades to entries.
        assert objectIdentity instanceof AclObjectIdentity;

        // must do this first...
        this.evictFromCache( objectIdentity );

        this.getSessionFactory().getCurrentSession().delete( objectIdentity );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.acl.AclDao#delete(org.springframework.security.acls.model.Sid)
     */
    @Override
    public void delete( Sid sid ) {

        Sid toDelete = this.find( sid );

        if ( toDelete == null ) {
            log.warn( "No such sid: " + sid );
            return;
        }

        // delete any objectidentities owned
        Session session = this.getSessionFactory().getCurrentSession();
        List<?> ownedOis = session
                .createQuery( "select s from AclObjectIdentity oi join oi.ownerSid s where s = :sid  " )
                .setParameter( "sid", toDelete ).list();

        if ( !ownedOis.isEmpty() ) {
            for ( Object oi : ownedOis ) {
                this.evictFromCache( ( ObjectIdentity ) oi );
                session.delete( oi );
            }
        }

        // delete any aclentries referring to this sid
        List<?> entries = session.createQuery( "select e from AclEntry e where e.sid = :sid" )
                .setParameter( "sid", toDelete ).list();

        for ( Object e : entries ) {
            session.delete( e );
        }

        assert toDelete != null;
        session.delete( toDelete );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.acl.AclDao#find(org.springframework.security.acls.model.ObjectIdentity)
     */
    @Override
    public AclObjectIdentity find( ObjectIdentity oid ) {
        List<?> r = this.getSessionFactory().getCurrentSession()
                .createQuery( "from AclObjectIdentity where type=:t and identifier=:i" )
                .setParameter( "t", oid.getType() ).setParameter( "i", oid.getIdentifier() ).list();
        if ( r.isEmpty() ) return null;

        return ( AclObjectIdentity ) r.get( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.acl.AclDao#find(org.springframework.security.acls.model.Sid)
     */
    @Override
    public AclSid find( Sid sid ) {
        if ( sid instanceof AclPrincipalSid ) {
            AclPrincipalSid p = ( AclPrincipalSid ) sid;
            List<?> r = this.getSessionFactory().getCurrentSession()
                    .createQuery( "from AclPrincipalSid where principal = :p" ).setParameter( "p", p.getPrincipal() )
                    .list();
            if ( !r.isEmpty() ) {
                return ( AclSid ) r.get( 0 );
            }
        } else {
            AclGrantedAuthoritySid g = ( AclGrantedAuthoritySid ) sid;
            List<?> r = this.getSessionFactory().getCurrentSession()
                    .createQuery( "from AclGrantedAuthoritySid where grantedAuthority = :g" )
                    .setParameter( "g", g.getGrantedAuthority() ).list();
            if ( !r.isEmpty() ) {
                return ( AclSid ) r.get( 0 );
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.acl.AclDao#findChildren(org.springframework.security.acls.model.
     * ObjectIdentity)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity ) {

        Assert.notNull( parentIdentity, "ParentIdentity cannot be null" );

        ObjectIdentity oi = this.find( parentIdentity );

        if ( oi == null ) {
            throw new EntityNotFoundException( "Failed to find: " + parentIdentity );
        }

        List<?> list = this.getSessionFactory().getCurrentSession()
                .createQuery( "from AclObjectIdentity o where o.parentObject = :po" )
                .setParameter( "po", parentIdentity ).list();

        return ( List<ObjectIdentity> ) list;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.acl.AclDao#findOrCreate(org.springframework.security.acls.model.Sid)
     */
    @Override
    public AclSid findOrCreate( Sid sid ) {
        AclSid fsid = this.find( sid );

        if ( fsid != null ) return fsid;

        assert sid instanceof AclSid;
        this.getSessionFactory().getCurrentSession().save( sid );

        return ( AclSid ) sid;

    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.jdbc.LookupStrategy#readAclsById(java.util.List, java.util.List)
     * 
     * Note that the objects here are not persistent - they need to be populated from the db.
     * 
     * argument sids is ignored by this implementation
     */
    @Override
    public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects, List<Sid> sids ) {

        Assert.isTrue( batchSize >= 1, "BatchSize must be >= 1" );
        Assert.notEmpty( objects, "Objects to lookup required" );

        Map<ObjectIdentity, Acl> result = new HashMap<ObjectIdentity, Acl>();

        Set<ObjectIdentity> currentBatchToLoad = new HashSet<ObjectIdentity>();

        for ( int i = 0; i < objects.size(); i++ ) {
            final ObjectIdentity oid = objects.get( i );
            boolean aclFound = false;

            // Check we don't already have this ACL in the results
            if ( result.containsKey( oid ) ) {
                aclFound = true;
            }

            // Check cache for the present ACL entry
            if ( !aclFound ) {
                Acl acl = aclCache.getFromCache( oid );
                // Ensure any cached element supports all the requested SIDs
                // (they should always, as our base impl doesn't filter on SID)
                if ( acl != null ) {

                    result.put( acl.getObjectIdentity(), acl );
                    aclFound = true;

                }
            }

            if ( !aclFound ) {
                currentBatchToLoad.add( oid );
            }

            if ( ( currentBatchToLoad.size() == this.batchSize ) || ( ( i + 1 ) == objects.size() ) ) {
                if ( currentBatchToLoad.size() > 0 ) {
                    Map<ObjectIdentity, Acl> loadedBatch = loadAcls( currentBatchToLoad );

                    // Add loaded batch (all elements 100% initialized) to results
                    result.putAll( loadedBatch );

                    assert result.size() >= loadedBatch.size();

                    // Add the loaded batch to the cache
                    for ( Acl loadedAcl : loadedBatch.values() ) {
                        aclCache.putInCache( ( MutableAcl ) loadedAcl );
                    }

                    currentBatchToLoad.clear();
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.acl.AclDao#setSessionFactory(org.hibernate.SessionFactory)
     */
    @Override
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.acl.AclDao#update(org.springframework.security.acls.model.MutableAcl)
     * 
     * 
     * This is an important method, and one that causes problems in the default JDBC-based service from spring-security.
     */
    @Override
    public void update( MutableAcl acl ) {
        if ( log.isTraceEnabled() )
            log.trace( ">>>>>>>>>> starting database update of acl for: " + acl.getObjectIdentity() );
        /*
         * This fixes problems with premature commits causing IDs to be erased on some entities.
         */
        this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.COMMIT );

        AclObjectIdentity aclObjectIdentity = convert( acl );

        // the ObjectIdentity might already be in the session.
        aclObjectIdentity = ( AclObjectIdentity ) this.getSessionFactory().getCurrentSession()
                .merge( aclObjectIdentity );

        if ( acl.getParentAcl() != null ) {
            update( ( MutableAcl ) acl.getParentAcl() );
            this.getSessionFactory().getCurrentSession().evict( acl.getParentAcl() );
            aclObjectIdentity.setParentObject( convert( ( MutableAcl ) acl.getParentAcl() ) );
        } else {
            // basically impossible to go from non-null to null, but just in case ...
            aclObjectIdentity.setParentObject( null );
        }

        this.getSessionFactory().getCurrentSession().update( aclObjectIdentity );

        evictFromCache( aclObjectIdentity );

        // children are left out, no big deal. Eviction more important.
        this.aclCache.putInCache( convertToAcl( aclObjectIdentity ) );

        if ( log.isTraceEnabled() )
            log.trace( " >>>>>>>>>> Done with database update of acl for: " + acl.getObjectIdentity() );
    }

    /**
     * @param acl
     * @return synched-up and partly updated AclObjectIdentity
     */
    private AclObjectIdentity convert( MutableAcl acl ) {
        assert acl instanceof AclImpl;
        assert acl.getObjectIdentity() instanceof AclObjectIdentity;

        // these come back with the ace_order filled in.
        List<AccessControlEntry> entriesFromAcl = acl.getEntries();

        // repopulate the ID of the SIDs.
        log.trace( "Preparing to update " + entriesFromAcl.size() + " aces" );
        for ( AccessControlEntry ace : entriesFromAcl ) {
            log.trace( ace );
            AclSid sid = ( AclSid ) ace.getSid();
            if ( sid.getId() == null ) {
                AclEntry aace = ( AclEntry ) ace;
                aace.setSid( this.findOrCreate( sid ) );
            }
        }

        // synched up with the ACL, partly
        AclObjectIdentity aclObjectIdentity = ( AclObjectIdentity ) acl.getObjectIdentity();

        if ( aclObjectIdentity.getOwnerSid().getId() == null ) {
            aclObjectIdentity.setOwnerSid( this.find( acl.getOwner() ) );
        }

        assert aclObjectIdentity.getOwnerSid() != null;

        /*
         * Update the collection of entries.
         */
        Collection<AclEntry> entriesToUpdate = aclObjectIdentity.getEntries();
        entriesToUpdate.clear();
        for ( AccessControlEntry accessControlEntry : entriesFromAcl ) {
            entriesToUpdate.add( ( AclEntry ) accessControlEntry );
        }

        return aclObjectIdentity;
    }

    /**
     * Does not check the cache;
     * 
     * @param oi
     * @return
     */
    private MutableAcl convertToAcl( AclObjectIdentity oi ) {
        if ( oi == null ) return null;

        MutableAcl e = new AclImpl( oi, aclAuthorizationStrategy, ( AclImpl ) convertToAcl( oi.getParentObject() ) );

        return e;
    }

    /**
     * ... including children, recursively.
     * 
     * @param aclObjectIdentity
     */
    private void evictFromCache( ObjectIdentity aclObjectIdentity ) {
        Assert.notNull( aclObjectIdentity, "aclObjectIdentity cannot be null" );

        this.aclCache.evictFromCache( aclObjectIdentity );
        for ( ObjectIdentity c : this.findChildren( aclObjectIdentity ) ) {
            evictFromCache( c );
        }
    }

    /**
     * Looks up a batch of <code>ObjectIdentity</code>s directly from the database, when we only know the type and
     * object's id (not the objectIdentity PK).
     * <p>
     * The caller is responsible for optimization issues, such as selecting the identities to lookup, ensuring the cache
     * doesn't contain them already, and adding the returned elements to the cache etc.
     * <p>
     * This is required to return fully valid <code>Acl</code>s, including properly-configured parent ACLs.
     * 
     * @param objectIdentities a batch of OIs to fetch ACLs for.
     */
    private Map<ObjectIdentity, Acl> loadAcls( final Collection<ObjectIdentity> objectIdentities ) {

        final Map<Serializable, Acl> results = new HashMap<Serializable, Acl>();

        // possibly has no entries yet, so left outer join?
        StringBuilder buf = new StringBuilder( "select o from AclObjectIdentity o left outer join o.entries e  where " );

        for ( int i = 1; i <= objectIdentities.size(); i++ ) {
            buf.append( " (o.identifier=? and o.type=?)" );
            if ( i < objectIdentities.size() ) {
                buf.append( " or" );
            }
        }

        buf.append( " order by o.identifier asc, e.aceOrder asc" );

        /*
         * We do not add a clause for the sids! That would require a more complex caching scheme.
         */

        Session session = this.getSessionFactory().getCurrentSession();

        Query query = session.createQuery( buf.toString() ).setReadOnly( true );
        int i = 0; // 1 is for jdbc...so we have to subtract 1 than used in BasicLookupStrategy
        for ( ObjectIdentity oi : objectIdentities ) {
            query.setLong( ( 2 * i ), ( Long ) oi.getIdentifier() );
            query.setString( ( 2 * i ) + 1, oi.getType() );
            i++;
        }

        List<?> queryR = query.list();

        Set<Long> parentIdsToLookup = new HashSet<>();
        for ( Object o : queryR ) {

            AclObjectIdentity oi = ( AclObjectIdentity ) o;

            AclImpl parentAcl = null;
            AclObjectIdentity parentObjectIdentity = oi.getParentObject();

            if ( parentObjectIdentity != null ) {

                if ( results.containsKey( parentObjectIdentity.getId() ) ) {
                    // it is possible cache contains the parent, without yet containing the child
                    continue;
                }

                // try to find parent in the cache
                MutableAcl cachedParent = aclCache.getFromCache( parentObjectIdentity.getId() );

                if ( cachedParent == null ) {

                    parentIdsToLookup.add( new Long( parentObjectIdentity.getId() ) );

                    parentAcl = new AclImpl( parentObjectIdentity, aclAuthorizationStrategy, /* parent acl */null );

                    parentAcl.getEntries().addAll( AclEntry.convert( new ArrayList<>( oi.getEntries() ), parentAcl ) );
                } else {
                    parentAcl = ( AclImpl ) cachedParent;

                    // Pop into the acls map, so our convert method doesn't
                    // need to deal with an unsynchronized AclCache, even though it might not be used directly.

                    results.put( cachedParent.getId(), cachedParent );
                }

                assert parentAcl != null;
            }

            Acl acl = new AclImpl( oi, aclAuthorizationStrategy, parentAcl );

            results.put( oi.getId(), acl );

        }

        // Lookup the parents
        if ( parentIdsToLookup.size() > 0 ) {
            loadAcls( results, parentIdsToLookup );
        }

        Map<ObjectIdentity, Acl> resultMap = new HashMap<ObjectIdentity, Acl>();
        for ( Acl inputAcl : results.values() ) {
            resultMap.put( inputAcl.getObjectIdentity(), inputAcl );
        }
        return resultMap;
    }

    /**
     * Load ACLs when we know the primary key of the objectIdentity. Recursively fill in the parentAcls.
     * 
     * @param acls the starting set of acls.
     * @param objectIdentityIds primary keys of the object identities to be fetched. If these yield acls that are the
     *        parents of the given acls, they will be populated.
     */
    private void loadAcls( final Map<Serializable, Acl> acls, final Set<Long> objectIdentityIds ) {
        Assert.notNull( acls, "ACLs are required" );
        Assert.notEmpty( objectIdentityIds, "Items to find now required" );

        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "from AclObjectIdentity where id in (:ids)" ).setParameterList( "ids", objectIdentityIds );

        List<?> queryR = query.list();

        Set<Long> parentsToLookup = new HashSet<>();
        for ( Object o : queryR ) {
            AclObjectIdentity oi = ( AclObjectIdentity ) o;

            if ( oi.getParentObject() != null ) {
                assert !oi.getParentObject().getId().equals( oi.getId() );
                parentsToLookup.add( oi.getParentObject().getId() );
            }

            Acl acl = new AclImpl( oi, aclAuthorizationStrategy, /* parentacl, to be filled in later */null );
            acls.put( oi.getId(), acl );
        }

        /*
         * Fill in the parent Acls for the acls that need them.
         */
        for ( Long oiid : objectIdentityIds ) {
            MutableAcl acl = ( MutableAcl ) acls.get( oiid );

            if ( acl.getParentAcl() != null ) {
                // we already did it.
                continue;
            }

            ObjectIdentity oi = acl.getObjectIdentity();
            AclObjectIdentity aoi = ( AclObjectIdentity ) oi;

            if ( aoi.getParentObject() != null ) {
                assert acls.containsKey( aoi.getParentObject().getId() );
                Acl parentAcl = acls.get( aoi.getParentObject().getId() );
                assert !acl.equals( parentAcl );
                acl.setParent( parentAcl );
            }
        }

        // Recurse; Lookup the parents of the newly fetched Acls.
        if ( parentsToLookup.size() > 0 ) {
            loadAcls( acls, parentsToLookup );
        }
    }
}
