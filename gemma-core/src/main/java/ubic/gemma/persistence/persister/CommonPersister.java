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
package ubic.gemma.persistence.persister;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persister for ubic.gemma.model.common package classes.
 * <p>
 * Persister-shrink S2a: lifted out of the {@code AbstractPersister} inheritance chain
 * into a concrete {@code @Component}. Carries the bits the rest of the chain (still
 * inheriting via {@code GenomePersister → ArrayDesignPersister → RelationshipPersister
 * → PersisterHelperImpl}) needs to keep working: the {@link SessionFactory} autowire,
 * the public {@code persist(T)} / {@code persistOrUpdate(T)} / {@code persist(Collection)}
 * entry points with their {@link FlushMode#MANUAL} window, the cascade helper
 * {@code doPersist(Collection&lt;T&gt;, Map)}, the polymorphic {@code doPersist} terminator
 * (throws {@link UnsupportedOperationException}), and the User/Characteristic instanceof
 * arm. Subsequent S2 sub-steps will peel each upper layer off and rewire it through
 * {@code @Autowired CommonPersister common;} instead.
 * <p>
 * Phase 3 persister retirement: methods here are being rewired to delegate to
 * {@code BusinessKey.find(Session, T)} (or DAO-level {@code find} where it already
 * wraps BusinessKey) followed by a direct {@code session.persist} / {@code dao.create}
 * on miss. The aim is to make each {@code persistXxx} a thin two-line "lookup by
 * business key, else create" so the whole persister can eventually be deleted in favour
 * of either the DAO {@code findOrCreate} call or a JPA cascade declared in the parent's
 * HBM mapping.
 *
 * @author pavlidis
 */
@Component("commonPersister")
public class CommonPersister {

    /**
     * Shared logger for all persisters. Kept on the (still-extant) {@code AbstractPersister}
     * symbol so the in-flight S2b–S2d subclasses can continue to use
     * {@code AbstractPersister.log} without touching every call site in the same commit.
     */
    protected static final Log log = LogFactory.getLog( CommonPersister.class.getName() );

    /**
     * Size if batch to report when persisting multiple entities with {@link #doPersist(Collection, Map)}.
     */
    protected static final int REPORT_BATCH_SIZE = 100;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Transactional
    public <T extends Identifiable> T persist( T entity ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            CommonPersister.log.trace( String.format( "Persisting a %s.", formatEntity( entity ) ) );
            T persistedEntity = doPersist( entity, new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    @Transactional
    public <T extends Identifiable> T persistOrUpdate( T entity ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            CommonPersister.log.trace( String.format( "Persisting or updating a %s.", formatEntity( entity ) ) );
            T persistedEntity = doPersistOrUpdate( entity, new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    @Transactional
    public <T extends Identifiable> List<T> persist( Collection<T> col ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            CommonPersister.log.trace( String.format( "Persisting a collection of %d entities.", col.size() ) );
            List<T> result = doPersist( col, new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Polymorphic dispatch entry point on the persister chain. Subclasses
     * ({@code GenomePersister}, {@code ArrayDesignPersister}, {@code RelationshipPersister},
     * {@code PersisterHelperImpl}) override and {@code super.doPersist(...)}-chain back to
     * here for the User and Characteristic arms; anything not handled terminates with
     * {@link UnsupportedOperationException}.
     */
    @OverridingMethodsMustInvokeSuper
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
        } else if ( entity instanceof Characteristic ) {
            // Characteristic is always cascaded from its owning entity (Investigation,
            // BioMaterial, FactorValue, etc. all declare cascade="all" on their
            // characteristics collection in HBM). Nothing to do here.
            return null;
        }
        throw new UnsupportedOperationException( String.format( "Don't know how to persist a %s.", formatEntity( entity ) ) );
    }

    protected final <T extends Identifiable> Set<T> doPersist( Set<T> entities, Map<String, ExternalDatabase> xdbCache ) {
        Set<T> result = new HashSet<>( entities.size() );
        int i = 0;
        for ( T entity : entities ) {
            result.add( this.doPersist( entity, xdbCache ) );
            if ( i++ % REPORT_BATCH_SIZE == 0 ) {
                CommonPersister.log.debug( String.format( "Persisted %d/%d entities.", result.size(), entities.size() ) );
            }
        }
        return result;
    }

    protected final <T extends Identifiable> List<T> doPersist( Collection<T> entities, Map<String, ExternalDatabase> xdbCache ) {
        List<T> result = new ArrayList<>( entities.size() );
        int i = 0;
        for ( T entity : entities ) {
            result.add( this.doPersist( entity, xdbCache ) );
            if ( i++ % REPORT_BATCH_SIZE == 0 ) {
                CommonPersister.log.debug( String.format( "Persisted %d/%d entities.", result.size(), entities.size() ) );
            }
        }
        return result;
    }

    @OverridingMethodsMustInvokeSuper
    protected <T extends Identifiable> T doPersistOrUpdate( T entity, Map<String, ExternalDatabase> xdbCache ) {
        throw new UnsupportedOperationException( String.format( "Don't know how to persist or update a %s.", formatEntity( entity ) ) );
    }

    /**
     * Persister-shrink S2 typed dispatch: handles the User / Characteristic arms
     * formerly carried by the {@code doPersist} override. Returns {@code null} for
     * Characteristic (cascade-only signal) and for any entity type this persister
     * does not recognise; callers must fall through to the next typed bean
     * ({@code GenomePersister.doGenome}, etc.) on null.
     */
    @Nullable
    public <T extends Identifiable> T doCommon( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
        } else if ( entity instanceof Characteristic ) {
            // Cascaded from the owning entity; null signals "handled, nothing to do".
            return null;
        }
        return null;
    }

    /**
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. Used by in-tree callers
     * ({@link GenomePersister}, {@link EeWriteServiceImpl},
     * {@link ArrayDesignPersister}, and {@link #persistBibliographicReference})
     * to dedupe ExternalDatabase lookups within one persist call.
     */
    public void fillInDatabaseEntry( DatabaseEntry databaseEntry, Map<String, ExternalDatabase> externalDbCache ) {
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb, externalDbCache );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    /**
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. New callers in the persister chain
     * (and in EE/AD/Genome write services) pass their own per-call map.
     */
    public ExternalDatabase persistExternalDatabase( ExternalDatabase database, Map<String, ExternalDatabase> externalDbCache ) {
        String name = database.getName();
        if ( name != null && externalDbCache.containsKey( name ) ) {
            return externalDbCache.get( name );
        }
        // ExternalDatabase has no static BusinessKey.find — DAO-level find()
        // resolves by name (a single-property business key).
        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );
        ExternalDatabase resolved = existingDatabase != null ? existingDatabase : externalDatabaseDao.create( database );
        if ( name != null ) {
            externalDbCache.put( name, resolved );
        }
        return resolved;
    }

    /**
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. Used by GenomePersister to resolve
     * BioSequence.sequenceDatabaseEntry (the only in-tree caller).
     */
    public DatabaseEntry persistDatabaseEntry( DatabaseEntry entity, Map<String, ExternalDatabase> externalDbCache ) {
        if ( entity.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( String.format( "DatabaseEntry %s must have an associated external database.", entity ) );
        }
        // Resolve the ExternalDatabase first (BK lookup, cached), then persist the
        // entry itself. DatabaseEntry has no business key — accession is per-entry
        // and only unique within an external database, so we always create.
        entity.setExternalDatabase( this.persistExternalDatabase( entity.getExternalDatabase(), externalDbCache ) );
        return databaseEntryDao.create( entity );
    }

    /**
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. Used by
     * {@link EeWriteServiceImpl} to resolve primaryPublication and
     * otherRelevantPublications within one EE-graph persist. External
     * (top-level) callers go through
     * {@code BibliographicReferenceService.findOrCreate} which resolves the XDB
     * via a fresh map per call.
     */
    public BibliographicReference persistBibliographicReference( BibliographicReference reference, Map<String, ExternalDatabase> externalDbCache ) {
        // BK is the pubAccession (a DatabaseEntry); resolve its ExternalDatabase first
        // so the BK lookup can match by accession string. BibliographicReference has
        // no static BusinessKey.find — the DAO-level find() queries by pubAccession.accession.
        this.fillInDatabaseEntry( reference.getPubAccession(), externalDbCache );
        BibliographicReference existing = bibliographicReferenceDao.find( reference );
        return existing != null ? existing : bibliographicReferenceDao.create( reference );
    }

    private String formatEntity( Object entity ) {
        Class<?> elementClass = Hibernate.getClass( entity );
        // Hibernate 6: getIdentifier() throws TransientObjectException for any entity not in the
        // session (HB 5 returned null). Branch on contains() first so a transient entity reports
        // cleanly instead of throwing out of a debug-log helper. For detached entities (have an id
        // but aren't in this session) we lose the "with ID N" detail — Hibernate 6 has no
        // dependency-free way to read the id off a detached entity short of reflection on the
        // @Id-annotated field, which isn't worth the complexity for a log string.
        org.hibernate.Session session = sessionFactory.getCurrentSession();
        if ( !session.contains( entity ) ) {
            return String.format( "transient or detached %s entity", elementClass.getSimpleName() );
        }
        Object id = session.getIdentifier( entity );
        return String.format( "persistent %s entity with ID %s", elementClass.getSimpleName(), id );
    }

}
