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
package ubic.gemma.loader.util.persister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.auditAndSecurity.Organization;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.PersonService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementService;
import ubic.gemma.model.common.protocol.Hardware;
import ubic.gemma.model.common.protocol.HardwareApplication;
import ubic.gemma.model.common.protocol.HardwareService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.common.protocol.Software;
import ubic.gemma.model.common.protocol.SoftwareApplication;
import ubic.gemma.model.common.protocol.SoftwareService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;

/**
 * Persister for ubic.gemma.model.common package classes.
 * 
 * @spring.property name="protocolService" ref="protocolService"
 * @spring.property name="softwareService" ref="softwareService"
 * @spring.property name="hardwareService" ref="hardwareService"
 * @spring.property name="ontologyEntryService" ref="ontologyEntryService"
 * @spring.property name="personService" ref="personService"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="localFileService" ref="localFileService"
 * @spring.property name="databaseEntryService" ref="databaseEntryService"
 * @spring.property name="contactService" ref="contactService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @spring.property name="measurementService" ref="measurementService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class CommonPersister extends AbstractPersister {

    protected AuditTrailService auditTrailService;

    protected ContactService contactService;

    protected DatabaseEntryService databaseEntryService;

    protected Person defaultOwner;

    protected ExternalDatabaseService externalDatabaseService;

    protected HardwareService hardwareService;

    protected LocalFileService localFileService;

    protected MeasurementService measurementService;

    protected OntologyEntryService ontologyEntryService;

    protected PersonService personService;

    protected UserService userService;

    protected ProtocolService protocolService;

    protected QuantitationTypeService quantitationTypeService;

    protected Map<Object, ExternalDatabase> seenDatabases = new HashMap<Object, ExternalDatabase>();

    protected SoftwareService softwareService;

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
            return persistUser( ( User ) entity );
        } else if ( entity instanceof Person ) {
            return persistPerson( ( Person ) entity );
        } else if ( entity instanceof Contact ) {
            return persistContact( ( Contact ) entity );
        } else if ( entity instanceof Hardware ) {
            return persistHardware( ( Hardware ) entity );
        } else if ( entity instanceof QuantitationType ) {
            return persistQuantitationType( ( QuantitationType ) entity );
        } else if ( entity instanceof ExternalDatabase ) {
            return persistExternalDatabase( ( ExternalDatabase ) entity );
        } else if ( entity instanceof OntologyEntry ) {
            return persistOntologyEntry( ( OntologyEntry ) entity );
        } else if ( entity instanceof DatabaseEntry ) {
            return persistDatabaseEntry( ( DatabaseEntry ) entity );
        } else if ( entity instanceof LocalFile ) {
            return persistLocalFile( ( LocalFile ) entity );
        } else if ( entity instanceof Software ) {
            return persistSoftware( ( Software ) entity );
        } else if ( entity instanceof Protocol ) {
            return null;
        } else if ( entity instanceof Collection ) {
            return super.persist( ( Collection ) entity );
        }
        throw new UnsupportedOperationException( "Don't know how to persist a " + entity.getClass().getName() );
    }

    /**
     * @param auditTrailService The auditTrailService to set.
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
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
     * @param hardwareService The hardwareService to set.
     */
    public void setHardwareService( HardwareService hardwareService ) {
        this.hardwareService = hardwareService;
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
     * @param ontologyEntryService The ontologyEntryService to set.
     */
    public void setOntologyEntryService( OntologyEntryService ontologyEntryService ) {
        this.ontologyEntryService = ontologyEntryService;
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
     * @param softwareService The softwareService to set.
     */
    public void setSoftwareService( SoftwareService softwareService ) {
        this.softwareService = softwareService;
    }

    /**
     * Fill in the categoryTerm and valueTerm associations of a
     * 
     * @param Characteristics Collection of Characteristics
     */
    protected void fillInOntologyEntries( Collection<Characteristic> Characteristics ) {
        for ( Characteristic Characteristic : Characteristics ) {
            persistOntologyEntry( Characteristic.getCategoryTerm() );
            persistOntologyEntry( Characteristic.getValueTerm() );
        }
    }

    /**
     * @param protocol
     */
    protected void fillInProtocol( Protocol protocol ) {
        if ( protocol == null ) {
            log.warn( "Null protocol" );
            return;
        }

        OntologyEntry type = protocol.getType();
        persistOntologyEntry( type );
        protocol.setType( type );

        for ( Software software : protocol.getSoftwareUsed() ) {
            software = persistSoftware( software );
        }

        for ( Hardware hardware : protocol.getHardwares() ) {
            hardware = persistHardware( hardware );
        }
    }

    /**
     * @param protocolApplication
     */

    protected void fillInProtocolApplication( ProtocolApplication protocolApplication ) {
        if ( protocolApplication == null ) return;

        log.debug( "Filling in protocolApplication" );

        Protocol protocol = protocolApplication.getProtocol();
        if ( protocol == null )
            throw new IllegalStateException( "Must have protocol associated with ProtocolApplication" );

        if ( protocol.getName() == null ) throw new IllegalStateException( "Protocol must have a name" );

        fillInProtocol( protocol );
        protocolApplication.setProtocol( protocolService.findOrCreate( protocol ) );

        for ( Person performer : protocolApplication.getPerformers() ) {
            log.debug( "Filling in performer" );
            performer = personService.findOrCreate( performer );
        }

        for ( SoftwareApplication softwareApplication : protocolApplication.getSoftwareApplications() ) {
            Software software = softwareApplication.getSoftware();
            if ( software == null )
                throw new IllegalStateException( "Must have software associated with SoftwareApplication" );

            OntologyEntry type = software.getType();
            persistOntologyEntry( type );
            software.setType( type );

            softwareApplication.setSoftware( softwareService.findOrCreate( software ) );

        }

        for ( HardwareApplication HardwareApplication : protocolApplication.getHardwareApplications() ) {
            Hardware hardware = HardwareApplication.getHardware();
            if ( hardware == null )
                throw new IllegalStateException( "Must have hardware associated with HardwareApplication" );

            OntologyEntry type = hardware.getType();
            persistOntologyEntry( type );
            hardware.setType( type );

            HardwareApplication.setHardware( hardwareService.findOrCreate( hardware ) );
        }
    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    @SuppressWarnings("unchecked")
    protected void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personService.findByFullName( "nobody", "nobody", "nobody" );

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
     * @param designProvider
     */
    protected Contact persistContact( Contact contact ) {
        if ( contact == null ) return null;
        return this.contactService.findOrCreate( contact );
    }

    /**
     * @param
     */
    protected Person persistPerson( Person person ) {
        if ( person == null ) return null;
        for ( Organization affiliation : person.getAffiliations() ) {
            affiliation = persistOrganization( affiliation );
        }
        return this.personService.findOrCreate( person );
    }

    /**
     * @param user
     * @return
     */
    protected User persistUser( User user ) {
        if ( user == null ) return null;

        // never create users from scratch this way -- only find them.
        User existingUser = this.userService.findByUserName( user.getUserName() );

        if ( existingUser == null ) {
            log.warn( "No such user '" + user.getUserName() + "' exists" );
            return null;
        }

        for ( Organization affiliation : existingUser.getAffiliations() ) {
            affiliation = persistOrganization( affiliation );
        }
        return existingUser;
    }

    /**
     * @param affiliation
     * @return
     */
    private Organization persistOrganization( Organization affiliation ) {
        // FIXME This is just to get us back in the saddle.
        log.warn( "Not persisting organization!!" );
        return null;
    }

    /**
     * @param databaseEntry
     * @return
     */
    protected DatabaseEntry persistDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) return null;
        databaseEntry.setExternalDatabase( persistExternalDatabase( databaseEntry.getExternalDatabase() ) );
        DatabaseEntry nde = databaseEntryService.findOrCreate( databaseEntry );
        log.debug( "Persisted " + nde );
        return nde;
    }

    /**
     * @param database
     */
    protected ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {

        if ( database == null ) return null;
        if ( !isTransient( database ) ) return database;

        String name = database.getName(); // FIXME make sure this is the right business key to use.

        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }

        log.debug( "Loading or creating " + name );
        database = externalDatabaseService.findOrCreate( database );
        seenDatabases.put( database.getName(), database );
        return database;
    }

    /**
     * @param hardware
     * @return
     */

    protected Hardware persistHardware( Hardware hardware ) {

        if ( hardware == null ) return null;
        if ( !isTransient( hardware ) ) return hardware;

        if ( hardware.getSoftwares() != null && hardware.getSoftwares().size() > 0 ) {
            for ( Software software : hardware.getSoftwares() ) {
                software = persistSoftware( software );
            }
        }

        hardware.setType( persistOntologyEntry( hardware.getType() ) );

        if ( hardware.getHardwareManufacturers() != null && hardware.getHardwareManufacturers().size() > 0 ) {
            for ( Contact manufacturer : hardware.getHardwareManufacturers() ) {
                manufacturer = persistContact( manufacturer );
            }
        }

        return hardwareService.findOrCreate( hardware );
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
        return measurementService.create( measurement );
    }

    /**
     * Ontology entr
     * 
     * @param ontologyEntry
     */

    protected OntologyEntry persistOntologyEntry( OntologyEntry ontologyEntry ) {
        if ( ontologyEntry == null ) return null;
        if ( !isTransient( ontologyEntry ) ) {
            return ontologyEntry;
        }

        ontologyEntry.setExternalDatabase( this.persistExternalDatabase( ontologyEntry.getExternalDatabase() ) );

        for ( OntologyEntry associatedOntologyEntry : ontologyEntry.getAssociations() ) {
            associatedOntologyEntry = persistOntologyEntry( associatedOntologyEntry );
        }

        ontologyEntry = ontologyEntryService.findOrCreate( ontologyEntry );
        return ontologyEntry;
    }

    Map<Object, QuantitationType> quantitationTypeCache = new HashMap<Object, QuantitationType>();

    /**
     * @param entity
     */
    protected QuantitationType persistQuantitationType( QuantitationType entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;

        String key = entity.getName() + " " + entity.getDescription();
        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        QuantitationType qt = quantitationTypeService.findOrCreate( entity );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    /**
     * @param software
     * @return
     */
    protected Software persistSoftware( Software software ) {
        if ( software == null ) return null;
        if ( !isTransient( software ) ) return software;

        Collection<Software> components = software.getSoftwareComponents();

        if ( components != null && components.size() > 0 ) {
            for ( Software component : components ) {
                component = persistSoftware( component );
            }
        }

        if ( software.getSoftwareManufacturers() != null && software.getSoftwareManufacturers().size() > 0 ) {
            for ( Contact manufacturer : software.getSoftwareManufacturers() ) {
                manufacturer = persistContact( manufacturer );
            }
        }

        software.setHardware( persistHardware( software.getHardware() ) );

        return softwareService.findOrCreate( software );

    }

    /**
     * @param userService The userService to set.
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

}
