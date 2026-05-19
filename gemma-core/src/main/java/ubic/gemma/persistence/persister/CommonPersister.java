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

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.*;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;

import java.util.Map;

/**
 * Persister for ubic.gemma.model.common package classes.
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
public abstract class CommonPersister extends AbstractPersister {

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
        } else if ( entity instanceof QuantitationType ) {
            return ( T ) this.persistQuantitationType( ( QuantitationType ) entity, caches );
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

    /**
     * Phase 3 persister-retirement scaffold: takes a local {@code Map<String, ExternalDatabase>}
     * instead of pulling the cache out of the {@link Caches} value object. New callers in
     * the persister chain (and in EE/AD/Genome write services) pass their own per-call map;
     * the legacy {@link #persistExternalDatabase(ExternalDatabase, Caches)} below delegates here
     * to preserve the {@link #fillInDatabaseEntry} / {@link #persistDatabaseEntry} paths until
     * those are lifted too.
     */
    protected ExternalDatabase persistExternalDatabase( ExternalDatabase database, Map<String, ExternalDatabase> externalDbCache ) {
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
     * In-CommonPersister convenience: pulls the per-call Map out of the
     * {@link Caches} container for the {@link #fillInDatabaseEntry} /
     * {@link #persistDatabaseEntry} paths. External callers (GenomePersister,
     * ArrayDesignPersister, EeWriteServiceImpl) call
     * {@link #persistExternalDatabase(ExternalDatabase, Map)} directly with
     * their own per-call map; this overload is intentionally private.
     */
    private ExternalDatabase persistExternalDatabase( ExternalDatabase database, Caches caches ) {
        return this.persistExternalDatabase( database, caches.getExternalDatabaseCache() );
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

    private Object persistBibliographicReference( BibliographicReference reference, Caches caches ) {
        // BK is the pubAccession (a DatabaseEntry); resolve its ExternalDatabase first
        // so the BK lookup can match by accession string. BibliographicReference has
        // no static BusinessKey.find — the DAO-level find() queries by pubAccession.accession.
        this.fillInDatabaseEntry( reference.getPubAccession(), caches );
        BibliographicReference existing = bibliographicReferenceDao.find( reference );
        return existing != null ? existing : bibliographicReferenceDao.create( reference );
    }

}
