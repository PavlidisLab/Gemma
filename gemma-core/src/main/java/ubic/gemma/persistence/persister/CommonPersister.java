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

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.*;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Map;

/**
 * Persister for ubic.gemma.model.common package classes.
 * <p>
 * Phase 3 persister retirement: methods here are being rewired to delegate to
 * {@link BusinessKey#find(Session, Object)} (or DAO-level {@code find} where it already
 * wraps BusinessKey) followed by a direct {@code session.persist} / {@code dao.create}
 * on miss. The aim is to make each {@code persistXxx} a thin two-line "lookup by
 * business key, else create" so the whole persister can eventually be deleted in favour
 * of either the DAO {@code findOrCreate} call or a JPA cascade declared in the parent's
 * HBM mapping.
 *
 * @author pavlidis
 */
public abstract class CommonPersister extends AbstractPersister {

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Autowired
    private UnitDao unitDao;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof AuditTrail ) {
            return ( T ) this.persistAuditTrail( ( AuditTrail ) entity );
        } else if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
        } else if ( entity instanceof Unit ) {
            return ( T ) this.persistUnit( ( Unit ) entity );
        } else if ( entity instanceof QuantitationType ) {
            return ( T ) this.persistQuantitationType( ( QuantitationType ) entity, caches );
        } else if ( entity instanceof ExternalDatabase ) {
            return ( T ) this.persistExternalDatabase( ( ExternalDatabase ) entity, caches );
        } else if ( entity instanceof Characteristic ) {
            // Characteristic is always cascaded from its owning entity (Investigation,
            // BioMaterial, FactorValue, etc. all declare cascade="all" on their
            // characteristics collection in HBM). Nothing to do here.
            return null;
        } else if ( entity instanceof BibliographicReference ) {
            return ( T ) this.persistBibliographicReference( ( BibliographicReference ) entity, caches );
        } else if ( entity instanceof DatabaseEntry ) {
            return ( T ) this.persistDatabaseEntry( ( DatabaseEntry ) entity, caches );
        } else {
            return super.doPersist( entity, caches );
        }
    }

    protected void fillInDatabaseEntry( DatabaseEntry databaseEntry, Caches caches ) {
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb, caches );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    protected AuditTrail persistAuditTrail( AuditTrail entity ) {
        // AuditTrail has no business key; events are persisted by composition.
        // Most callers reach AuditTrail via the Auditable parent's cascade=all and
        // never invoke this directly — preserved for the few that do.
        for ( AuditEvent event : entity.getEvents() ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            assert event.getPerformer() != null;
        }
        return auditTrailDao.create( entity );
    }

    protected ExternalDatabase persistExternalDatabase( ExternalDatabase database, Caches caches ) {
        Map<String, ExternalDatabase> seenDatabases = caches.getExternalDatabaseCache();
        String name = database.getName();
        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }
        // ExternalDatabase has no static BusinessKey.find — DAO-level find()
        // resolves by name (a single-property business key).
        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );
        if ( existingDatabase == null ) {
            database = externalDatabaseDao.create( database );
        } else {
            database = existingDatabase;
        }
        seenDatabases.put( database.getName(), database );
        return database;
    }

    private DatabaseEntry persistDatabaseEntry( DatabaseEntry entity, Caches caches ) {
        if ( entity.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( String.format( "DatabaseEntry %s must have an associated external database.", entity ) );
        }
        // Resolve the ExternalDatabase first (BK lookup, cached), then persist the
        // entry itself. DatabaseEntry has no business key — accession is per-entry
        // and only unique within an external database, so we always create.
        entity.setExternalDatabase( this.persistExternalDatabase( entity.getExternalDatabase(), caches ) );
        return databaseEntryDao.create( entity );
    }

    protected QuantitationType persistQuantitationType( QuantitationType qType, Caches caches ) {
        // QTs are per-experiment — we deliberately do NOT find-or-create across
        // experiments, only within one (via the cache, which the caller clears
        // between experiments). The cache key matches BusinessKey.matches semantics
        // for QuantitationType ((name, description)).
        if ( qType.getName() == null )
            throw new IllegalArgumentException( "QuantitationType must have a name" );
        int key = qType.getName().hashCode();
        if ( qType.getDescription() != null )
            key += qType.getDescription().hashCode();

        Map<Integer, QuantitationType> quantitationTypeCache = caches.getQuantitationTypeCache();
        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        QuantitationType qt = quantitationTypeDao.create( qType );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    protected Unit persistUnit( Unit unit ) {
        // Unit has a static BusinessKey.find — bypass the DAO-level wrapper so the
        // intent is visible at the call site.
        Session session = getSessionFactory().getCurrentSession();
        Unit existing = BusinessKey.find( session, unit );
        return existing != null ? existing : unitDao.create( unit );
    }

    private Object persistBibliographicReference( BibliographicReference reference, Caches caches ) {
        // BK is the pubAccession (a DatabaseEntry); resolve its ExternalDatabase first
        // so the BK lookup can match by accession string. BibliographicReference has
        // no static BusinessKey.find — the DAO-level find() queries by pubAccession.accession.
        this.fillInDatabaseEntry( reference.getPubAccession(), caches );
        BibliographicReference existing = bibliographicReferenceDao.find( reference );
        return existing != null ? existing : bibliographicReferenceDao.create( reference );
    }

}
