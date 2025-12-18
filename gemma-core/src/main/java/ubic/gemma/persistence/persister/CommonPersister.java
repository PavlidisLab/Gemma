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
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.PersonDao;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
import ubic.gemma.persistence.service.common.protocol.ProtocolDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;

import java.util.Collection;
import java.util.Map;

/**
 * Persister for ubic.gemma.model.common package classes.
 *
 * @author pavlidis
 */
public abstract class CommonPersister extends AbstractPersister {

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ProtocolDao protocolDao;

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
            // return persistUser( ( User ) entity );
        } else if ( entity instanceof Person ) {
            return ( T ) this.persistPerson( ( Person ) entity );
        } else if ( entity instanceof Contact ) {
            return ( T ) this.persistContact( ( Contact ) entity );
        } else if ( entity instanceof Unit ) {
            return ( T ) this.persistUnit( ( Unit ) entity );
        } else if ( entity instanceof QuantitationType ) {
            return ( T ) this.persistQuantitationType( ( QuantitationType ) entity, caches );
        } else if ( entity instanceof ExternalDatabase ) {
            return ( T ) this.persistExternalDatabase( ( ExternalDatabase ) entity, caches );
        } else if ( entity instanceof Protocol ) {
            return ( T ) this.persistProtocol( ( Protocol ) entity );
        } else if ( entity instanceof Characteristic ) {
            return null; // cascade
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
        for ( AuditEvent event : entity.getEvents() ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            // any more
            // event.setPerformer( ( User ) persistPerson( event.getPerformer() ) );
            assert event.getPerformer() != null;
        }

        // events are persisted by composition.
        return auditTrailDao.create( entity );
    }

    protected Contact persistContact( Contact contact ) {
        return this.contactDao.findOrCreate( contact );
    }

    protected ExternalDatabase persistExternalDatabase( ExternalDatabase database, Caches caches ) {
        Map<String, ExternalDatabase> seenDatabases = caches.getExternalDatabaseCache();

        String name = database.getName();

        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }

        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );

        // don't use findOrCreate to avoid flush.
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
        entity.setExternalDatabase( this.persistExternalDatabase( entity.getExternalDatabase(), caches ) );
        return databaseEntryDao.create( entity );
    }


    protected Protocol persistProtocol( Protocol protocol ) {
        // I changed this to create instead of findOrCreate because in
        // practice protocols are not shared; we use them to store information about analyses we run. PP2017
        return protocolDao.create( protocol );
    }

    protected QuantitationType persistQuantitationType( QuantitationType qType, Caches caches ) {
        /*
         * this cache is dangerous if run for multiple experiment loadings. For this reason we clear the cache
         * before persisting each experiment.
         */
        int key;
        if ( qType.getName() == null )
            throw new IllegalArgumentException( "QuantitationType must have a name" );
        key = qType.getName().hashCode();
        if ( qType.getDescription() != null )
            key += qType.getDescription().hashCode();

        Map<Integer, QuantitationType> quantitationTypeCache = caches.getQuantitationTypeCache();

        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        /*
         * Note: we use 'create' here instead of 'findOrCreate' because we don't want quantitation types shared across
         * experiments.
         */
        QuantitationType qt = quantitationTypeDao.create( qType );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    protected Unit persistUnit( Unit unit ) {
        return this.unitDao.findOrCreate( unit );
    }

    private Object persistBibliographicReference( BibliographicReference reference, Caches caches ) {
        this.fillInDatabaseEntry( reference.getPubAccession(), caches );
        return this.bibliographicReferenceDao.findOrCreate( reference );
    }

    private Person persistPerson( Person person ) {
        return this.personDao.findOrCreate( person );
    }

}
