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
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;

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
    private DatabaseEntryDao databaseEntryDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
        } else if ( entity instanceof Characteristic ) {
            // Characteristic is always cascaded from its owning entity (Investigation,
            // BioMaterial, FactorValue, etc. all declare cascade="all" on their
            // characteristics collection in HBM). Nothing to do here.
            return null;
        } else {
            return super.doPersist( entity, xdbCache );
        }
    }

    /**
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. Used by in-tree callers
     * ({@link GenomePersister}, {@link EeWriteServiceImpl},
     * {@link ArrayDesignPersister}, and {@link #persistBibliographicReference})
     * to dedupe ExternalDatabase lookups within one persist call.
     */
    protected void fillInDatabaseEntry( DatabaseEntry databaseEntry, Map<String, ExternalDatabase> externalDbCache ) {
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
     * Phase 3 persister-retirement scaffold: takes a per-call
     * {@code Map<String, ExternalDatabase>}. Used by GenomePersister to resolve
     * BioSequence.sequenceDatabaseEntry (the only in-tree caller).
     */
    protected DatabaseEntry persistDatabaseEntry( DatabaseEntry entity, Map<String, ExternalDatabase> externalDbCache ) {
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
    protected BibliographicReference persistBibliographicReference( BibliographicReference reference, Map<String, ExternalDatabase> externalDbCache ) {
        // BK is the pubAccession (a DatabaseEntry); resolve its ExternalDatabase first
        // so the BK lookup can match by accession string. BibliographicReference has
        // no static BusinessKey.find — the DAO-level find() queries by pubAccession.accession.
        this.fillInDatabaseEntry( reference.getPubAccession(), externalDbCache );
        BibliographicReference existing = bibliographicReferenceDao.find( reference );
        return existing != null ? existing : bibliographicReferenceDao.create( reference );
    }

}
