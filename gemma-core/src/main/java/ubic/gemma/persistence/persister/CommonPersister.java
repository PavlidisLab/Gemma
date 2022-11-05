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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persister for ubic.gemma.model.common package classes.
 *
 * @author pavlidis
 */
abstract public class CommonPersister extends AbstractPersister {

    private final Map<Object, ExternalDatabase> seenDatabases = new ConcurrentHashMap<>();
    private final Map<Object, QuantitationType> quantitationTypeCache = new ConcurrentHashMap<>();

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
    public Object persist( Object entity ) {

        if ( entity instanceof AuditTrail ) {
            return this.persistAuditTrail( ( AuditTrail ) entity );
        } else if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
            // return persistUser( ( User ) entity );
        } else if ( entity instanceof Person ) {
            return this.persistPerson( ( Person ) entity );
        } else if ( entity instanceof Contact ) {
            return this.persistContact( ( Contact ) entity );
        } else if ( entity instanceof Unit ) {
            return this.persistUnit( ( Unit ) entity );
        } else if ( entity instanceof QuantitationType ) {
            return this.persistQuantitationType( ( QuantitationType ) entity );
        } else if ( entity instanceof ExternalDatabase ) {
            return this.persistExternalDatabase( ( ExternalDatabase ) entity );
        } else if ( entity instanceof Protocol ) {
            return this.persistProtocol( ( Protocol ) entity );
        } else if ( entity instanceof Characteristic ) {
            return null; // cascade
        } else if ( entity instanceof Collection ) {
            return super.persist( ( Collection<?> ) entity );
        } else if ( entity instanceof BibliographicReference ) {
            return this.persistBibliographicReference( ( BibliographicReference ) entity );
        } else if ( entity instanceof DatabaseEntry ) {
            return this.persistDatabaseEntry( ( DatabaseEntry ) entity );
        }
        throw new UnsupportedOperationException( "Don't know how to persist a " + entity.getClass().getName() );
    }


    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null )
            return null;
        throw new UnsupportedOperationException( "Don't know how to persistOrUpdate a " + entity.getClass().getName() );
    }

    /**
     * For clearing the cache.
     */
    void clearCommonCache() {
        this.quantitationTypeCache.clear();
    }

    void fillInDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( !this.isTransient( databaseEntry ) )
            return;
        if ( databaseEntry == null )
            return;
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    AuditTrail persistAuditTrail( AuditTrail entity ) {
        if ( entity == null )
            return null;
        if ( !this.isTransient( entity ) )
            return entity;

        for ( AuditEvent event : entity.getEvents() ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            // any more
            // event.setPerformer( ( User ) persistPerson( event.getPerformer() ) );
            assert event.getPerformer() != null && !this.isTransient( event.getPerformer() );
        }

        // events are persisted by composition.
        auditTrailDao.create( entity );

        return entity;
    }

    Contact persistContact( Contact contact ) {
        if ( contact == null )
            return null;
        return this.contactDao.findOrCreate( contact );
    }

    ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {

        if ( database == null )
            return null;
        if ( !this.isTransient( database ) )
            return database;

        String name = database.getName();

        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }

        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );

        // don't use findOrCreate to avoid flush.
        if ( existingDatabase == null ) {
            externalDatabaseDao.create( database );
        } else {
            database = existingDatabase;
        }

        seenDatabases.put( database.getName(), database );
        return database;
    }

    private DatabaseEntry persistDatabaseEntry( DatabaseEntry entity ) {
        if ( isTransient( entity.getExternalDatabase() ) ) {
            entity.setExternalDatabase( this.persistExternalDatabase( entity.getExternalDatabase() ) );
        }
        databaseEntryDao.create( entity );
        return entity;
    }


    Protocol persistProtocol( Protocol protocol ) {
        if ( protocol == null )
            return null;
        this.fillInProtocol( protocol );
        // I changed this to create instead of findOrCreate because in
        // practice protocols are not shared; we use them to store information about analyses we run. PP2017
        protocolDao.create( protocol );
        return protocol;
    }

    QuantitationType persistQuantitationType( QuantitationType qType ) {
        if ( qType == null )
            return null;
        if ( !this.isTransient( qType ) )
            return qType;

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

        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        /*
         * Note: we use 'create' here instead of 'findOrCreate' because we don't want quantitation types shared across
         * experiments.
         */
        quantitationTypeDao.create( qType );
        quantitationTypeCache.put( key, qType );
        return qType;
    }

    Unit persistUnit( Unit unit ) {
        if ( unit == null )
            return null;
        if ( !this.isTransient( unit ) )
            return unit;
        return this.unitDao.findOrCreate( unit );
    }

    private void fillInProtocol( Protocol protocol ) {
        if ( !this.isTransient( protocol ) )
            return;
        if ( protocol == null ) {
            AbstractPersister.log.warn( "Null protocol" );
        }

    }

    private Object persistBibliographicReference( BibliographicReference reference ) {
        this.fillInDatabaseEntry( reference.getPubAccession() );
        return this.bibliographicReferenceDao.findOrCreate( reference );
    }

    private Person persistPerson( Person person ) {
        if ( person == null )
            return null;
        return this.personDao.findOrCreate( person );
    }

}
