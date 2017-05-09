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
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.PersonDao;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.common.description.LocalFileDao;
import ubic.gemma.model.common.auditAndSecurity.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.persistence.service.common.measurement.MeasurementDao;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.persistence.service.common.protocol.ProtocolDao;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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

    // FIXME should use an expiring cache (but this is probably not a big deal)
    private final Map<Object, ExternalDatabase> seenDatabases = new ConcurrentHashMap<>();
    // FIXME should use an expiring cache (not a huge amount of data)
    private final Map<Object, QuantitationType> quantitationTypeCache = new ConcurrentHashMap<>();

    Person defaultOwner;

    @Autowired
    LocalFileDao localFileDao;

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private MeasurementDao measurementDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ProtocolDao protocolDao;

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Autowired
    private UnitDao unitDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {

        if ( entity instanceof AuditTrail ) {
            return persistAuditTrail( ( AuditTrail ) entity );
        } else if ( entity instanceof User ) {
            throw new UnsupportedOperationException( "Don't persist users via this class; use the UserManager (core)" );
            // return persistUser( ( User ) entity );
        } else if ( entity instanceof Person ) {
            return persistPerson( ( Person ) entity );
        } else if ( entity instanceof Contact ) {
            return persistContact( ( Contact ) entity );
        } else if ( entity instanceof Unit ) {
            return persistUnit( ( Unit ) entity );
        } else if ( entity instanceof QuantitationType ) {
            return persistQuantitationType( ( QuantitationType ) entity );
        } else if ( entity instanceof ExternalDatabase ) {
            return persistExternalDatabase( ( ExternalDatabase ) entity );
        } else if ( entity instanceof LocalFile ) {
            return persistLocalFile( ( LocalFile ) entity );
        } else if ( entity instanceof Protocol ) {
            return persistProtocol( ( Protocol ) entity );
        } else if ( entity instanceof VocabCharacteristic ) {
            return null; // cascade
        } else if ( entity instanceof Characteristic ) {
            return null; // cascade
        } else if ( entity instanceof Collection ) {
            return super.persist( ( Collection<?> ) entity );
        } else if ( entity instanceof BibliographicReference ) {
            return persistBibliographicReference( ( BibliographicReference ) entity );
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
     * @param defaultOwner The defaultOwner to set.
     */
    public void setDefaultOwner( Person defaultOwner ) {
        this.defaultOwner = defaultOwner;
    }

    /**
     * For clearing the cache.
     */
    void clearCommonCache() {
        this.quantitationTypeCache.clear();
    }

    void fillInDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( !isTransient( databaseEntry ) )
            return;
        if ( databaseEntry == null )
            return;
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = persistExternalDatabase( tempExternalDb );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    private void fillInProtocol( Protocol protocol ) {
        if ( !isTransient( protocol ) )
            return;
        if ( protocol == null ) {
            log.warn( "Null protocol" );
        }

    }

    void fillInProtocolApplication( ProtocolApplication protocolApplication ) {
        if ( !isTransient( protocolApplication ) )
            return;
        if ( protocolApplication == null )
            return;

        log.debug( "Filling in protocolApplication" );

        Protocol protocol = protocolApplication.getProtocol();
        if ( protocol == null )
            throw new IllegalStateException( "Must have protocol associated with ProtocolApplication" );

        if ( protocol.getName() == null )
            throw new IllegalStateException( "Protocol must have a name" );

        protocolApplication.setProtocol( persistProtocol( protocol ) );

        for ( Person performer : protocolApplication.getPerformers() ) {
            log.debug( "Filling in performer" );
            personDao.findOrCreate( performer );
        }

    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    protected void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personDao.findByFullName( "nobody", "nobody" );

        assert matchingPersons.size() == 1 : "Found " + matchingPersons.size() + " contacts matching 'nobody'";

        defaultOwner = matchingPersons.iterator().next();

        if ( defaultOwner == null )
            throw new NullPointerException( "Default Person 'nobody' not found in database." );
    }

    AuditTrail persistAuditTrail( AuditTrail entity ) {
        if ( entity == null )
            return null;
        if ( !isTransient( entity ) )
            return entity;

        for ( AuditEvent event : entity.getEvents() ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            // any more
            // event.setPerformer( ( User ) persistPerson( event.getPerformer() ) );
            assert event.getPerformer() != null && !isTransient( event.getPerformer() );
        }

        // events are persisted by composition.
        return auditTrailDao.create( entity );
    }

    private Object persistBibliographicReference( BibliographicReference reference ) {
        fillInDatabaseEntry( reference.getPubAccession() );
        return this.bibliographicReferenceDao.findOrCreate( reference );
    }

    Contact persistContact( Contact contact ) {
        if ( contact == null )
            return null;
        return this.contactDao.findOrCreate( contact );
    }

    ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {

        if ( database == null )
            return null;
        if ( !isTransient( database ) )
            return database;

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

    LocalFile persistLocalFile( LocalFile file ) {
        return persistLocalFile( file, false );
    }

    LocalFile persistLocalFile( LocalFile file, boolean forceNew ) {
        if ( file == null )
            return null;
        if ( !isTransient( file ) )
            return file;
        if ( forceNew )
            return localFileDao.create( file );
        file.setId( null ); // in case of retry.
        return localFileDao.findOrCreate( file );
    }

    /**
     * Unlike many entities, measurements are 'unique' - there is no 'findOrCreate' method.
     */
    protected Measurement persistMeasurement( Measurement measurement ) {

        if ( measurement.getUnit() != null ) {
            measurement.setUnit( persistUnit( measurement.getUnit() ) );
        }

        return measurementDao.create( measurement );
    }

    private Person persistPerson( Person person ) {
        if ( person == null )
            return null;
        return this.personDao.findOrCreate( person );
    }

    Protocol persistProtocol( Protocol protocol ) {
        if ( protocol == null )
            return protocol;
        fillInProtocol( protocol );
        return protocolDao.findOrCreate( protocol );
    }

    QuantitationType persistQuantitationType( QuantitationType qType ) {
        if ( qType == null )
            return null;
        if ( !isTransient( qType ) )
            return qType;

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
        QuantitationType qt = quantitationTypeDao.create( qType );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    Unit persistUnit( Unit unit ) {
        if ( unit == null )
            return null;
        if ( !isTransient( unit ) )
            return unit;
        return this.unitDao.findOrCreate( unit );
    }

}
