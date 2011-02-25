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
package ubic.gemma.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.auditAndSecurity.Organization;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.PersonService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementService;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.measurement.UnitService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;

/**
 * Persister for ubic.gemma.model.common package classes.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class CommonPersister extends AbstractPersister {

    @Autowired
    protected AuditTrailService auditTrailService;

    @Autowired
    protected BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    protected ContactService contactService;

    @Autowired
    protected DatabaseEntryService databaseEntryService;

    protected Person defaultOwner;

    @Autowired
    protected ExternalDatabaseService externalDatabaseService;

    @Autowired
    protected LocalFileService localFileService;

    @Autowired
    protected MeasurementService measurementService;
    @Autowired
    protected PersonService personService;

    // protected OntologyEntryService ontologyEntryService;

    @Autowired
    protected ProtocolService protocolService;

    @Autowired
    protected QuantitationTypeService quantitationTypeService;

    protected Map<Object, ExternalDatabase> seenDatabases = new HashMap<Object, ExternalDatabase>();

    @Autowired
    protected UnitService unitService;

    Map<Object, QuantitationType> quantitationTypeCache = new HashMap<Object, QuantitationType>();

    public CommonPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    // protected TermRelationshipService termRelationshipService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
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
            return super.persist( ( Collection ) entity );
        } else if ( entity instanceof BibliographicReference ) {
            return persistBibliographicReference( ( BibliographicReference ) entity );
        }
        throw new UnsupportedOperationException( "Don't know how to persist a " + entity.getClass().getName() );
    }

    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        throw new UnsupportedOperationException( "Don't know how to persistOrUpdate a " + entity.getClass().getName() );
    }

    /**
     * @param auditTrailService The auditTrailService to set.
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param contactService The contactService to set.
     */
    public void setContactService( ContactService contactService ) {
        this.contactService = contactService;
    }

    /**
     * @param databaseEntryService The databaseEntryService to set.
     */
    public void setDatabaseEntryService( DatabaseEntryService databaseEntryService ) {
        this.databaseEntryService = databaseEntryService;
    }

    /**
     * @param defaultOwner The defaultOwner to set.
     */
    public void setDefaultOwner( Person defaultOwner ) {
        this.defaultOwner = defaultOwner;
    }

    /**
     * @param externalDatabaseService The externalDatabaseService to set.
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param localFileService The localFileService to set.
     */
    public void setLocalFileService( LocalFileService localFileService ) {
        this.localFileService = localFileService;
    }

    /**
     * @param measurementService The measurementService to set.
     */
    public void setMeasurementService( MeasurementService measurementService ) {
        this.measurementService = measurementService;
    }

    /**
     * @param personService The personService to set.
     */
    public void setPersonService( PersonService personService ) {
        this.personService = personService;
    }

    /**
     * @param protocolService The protocolService to set.
     */
    public void setProtocolService( ProtocolService protocolService ) {
        this.protocolService = protocolService;
    }

    /**
     * @param quantitationTypeService The quantitationTypeService to set.
     */
    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    /**
     * @param unitService the unitService to set
     */
    public void setUnitService( UnitService unitService ) {
        this.unitService = unitService;
    }

    /**
     * For clearing the cache.
     */
    protected void clearCommonCache() {
        this.quantitationTypeCache.clear();
    }

    /**
     * @param databaseEntry
     */
    protected void fillInDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( !isTransient( databaseEntry ) ) return;
        if ( databaseEntry == null ) return;
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = persistExternalDatabase( tempExternalDb );
        databaseEntry.setExternalDatabase( persistedDb );

        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    /**
     * @param protocol
     */
    protected void fillInProtocol( Protocol protocol ) {
        if ( !isTransient( protocol ) ) return;
        if ( protocol == null ) {
            log.warn( "Null protocol" );
            return;
        }

    }

    /**
     * @param protocolApplication
     */
    protected void fillInProtocolApplication( ProtocolApplication protocolApplication ) {
        if ( !isTransient( protocolApplication ) ) return;
        if ( protocolApplication == null ) return;

        log.debug( "Filling in protocolApplication" );

        Protocol protocol = protocolApplication.getProtocol();
        if ( protocol == null )
            throw new IllegalStateException( "Must have protocol associated with ProtocolApplication" );

        if ( protocol.getName() == null ) throw new IllegalStateException( "Protocol must have a name" );

        protocolApplication.setProtocol( persistProtocol( protocol ) );

        for ( Person performer : protocolApplication.getPerformers() ) {
            log.debug( "Filling in performer" );
            performer = personService.findOrCreate( performer );
        }

    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    protected void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personService.findByFullName( "nobody", "nobody" );

        assert matchingPersons.size() == 1 : "Found " + matchingPersons.size() + " contacts matching 'nobody'";

        defaultOwner = matchingPersons.iterator().next();

        if ( defaultOwner == null ) throw new NullPointerException( "Default Person 'nobody' not found in database." );
    }

    /**
     * @param entity
     * @return
     */
    protected AuditTrail persistAuditTrail( AuditTrail entity ) {
        if ( entity == null ) return null;
        if ( !this.isTransient( entity ) ) return entity;

        for ( AuditEvent event : entity.getEvents() ) {
            event.setPerformer( ( User ) persistPerson( event.getPerformer() ) );
        }

        // events are persisted by composition.
        return auditTrailService.create( entity );
    }

    /**
     * @param reference
     * @return
     */
    protected Object persistBibliographicReference( BibliographicReference reference ) {
        fillInDatabaseEntry( reference.getPubAccession() );
        final BibliographicReference perReference = this.bibliographicReferenceService.findOrCreate( reference );

        // thaw - this is necessary to avoid lazy exceptions later, but perhaps could be done more elegantly!
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( perReference );
                return null;
            }
        } );

        return perReference;
    }

    /**
     * @param designProvider
     */
    protected Contact persistContact( Contact contact ) {
        if ( contact == null ) return null;
        return this.contactService.findOrCreate( contact );
    }

    /**
     * @param database
     */
    protected ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {

        if ( database == null ) return null;
        if ( !isTransient( database ) ) return database;

        String name = database.getName();

        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }

        database = externalDatabaseService.findOrCreate( database );
        seenDatabases.put( database.getName(), database );
        return database;
    }

    /**
     * @param file
     */
    protected LocalFile persistLocalFile( LocalFile file ) {
        if ( file == null ) return null;
        if ( !isTransient( file ) ) return file;
        return localFileService.findOrCreate( file );
    }

    /**
     * Unlike many entities, measurements are 'unique' - there is no 'findOrCreate' method.
     * 
     * @param measurement
     * @return
     */
    protected Measurement persistMeasurement( Measurement measurement ) {

        if ( measurement.getUnit() != null ) {
            measurement.setUnit( persistUnit( measurement.getUnit() ) );
        }

        return measurementService.create( measurement );
    }

    /**
     * @param
     */
    protected Person persistPerson( Person person ) {
        if ( person == null ) return null;
        return this.personService.findOrCreate( person );
    }

    protected Protocol persistProtocol( Protocol protocol ) {
        if ( protocol == null ) return protocol;
        fillInProtocol( protocol );
        return protocolService.findOrCreate( protocol );
    }

    /**
     * @param qType
     */
    protected QuantitationType persistQuantitationType( QuantitationType qType ) {
        if ( qType == null ) return null;
        if ( !isTransient( qType ) ) return qType;

        int key = 0;
        if ( qType.getName() == null ) throw new IllegalArgumentException( "QuantitationType must have a name" );
        key = qType.getName().hashCode();
        if ( qType.getDescription() != null ) key += qType.getDescription().hashCode();

        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        /*
         * Note: we use 'create' here instead of 'findOrCreate' because we don't want quantitation types shared across
         * experiments.
         */
        QuantitationType qt = quantitationTypeService.create( qType );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    protected Unit persistUnit( Unit unit ) {
        if ( unit == null ) return null;
        if ( !isTransient( unit ) ) return unit;
        return this.unitService.findOrCreate( unit );
    }

    //
    // /**
    // * @param user
    // * @return
    // */
    // protected User persistUser( User user ) {
    // if ( user == null ) return null;
    //
    // User existingUser = this.userService.findByUserName( user.getUserName() );
    //
    // if ( existingUser == null ) {
    // log.warn( "No such user '" + user.getUserName() + "' exists" );
    // for ( Organization affiliation : user.getAffiliations() ) {
    // affiliation = persistOrganization( affiliation );
    // }
    // try {
    // return userService.create( user );
    // } catch ( Exception e ) {
    // throw new RuntimeException( e );
    // }
    // }
    //
    // for ( Organization affiliation : existingUser.getAffiliations() ) {
    // affiliation = persistOrganization( affiliation );
    // }
    // return existingUser;
    // }

    /**
     * @param affiliation
     * @return
     */
    private Organization persistOrganization( Organization affiliation ) {
        if ( affiliation == null ) return null;
        if ( !isTransient( affiliation ) ) return affiliation;
        affiliation.setParent( persistOrganization( affiliation.getParent() ) );
        return ( Organization ) persistContact( affiliation );
    }

}
