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
package edu.columbia.gemma.loader.loaderutils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.BioAssayData.DesignElementDimension;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactService;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonService;
import edu.columbia.gemma.common.description.Characteristic;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseEntryService;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileService;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryService;
import edu.columbia.gemma.common.protocol.Hardware;
import edu.columbia.gemma.common.protocol.HardwareApplication;
import edu.columbia.gemma.common.protocol.HardwareService;
import edu.columbia.gemma.common.protocol.Protocol;
import edu.columbia.gemma.common.protocol.ProtocolApplication;
import edu.columbia.gemma.common.protocol.ProtocolService;
import edu.columbia.gemma.common.protocol.Software;
import edu.columbia.gemma.common.protocol.SoftwareApplication;
import edu.columbia.gemma.common.protocol.SoftwareService;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssay.BioAssayService;
import edu.columbia.gemma.expression.bioAssayData.BioAssayDimension;
import edu.columbia.gemma.expression.bioAssayData.BioAssayDimensionService;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService;
import edu.columbia.gemma.expression.bioAssayData.QuantitationTypeDimensionService;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.BioMaterialService;
import edu.columbia.gemma.expression.biomaterial.Compound;
import edu.columbia.gemma.expression.biomaterial.CompoundService;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.designElement.ReporterService;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentSubSet;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.expression.experiment.FactorValueService;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonService;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.biosequence.BioSequenceService;
import edu.columbia.gemma.genome.gene.GeneService;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="persisterHelper"
 * @spring.property name="ontologyEntryService" ref="ontologyEntryService"
 * @spring.property name="personService" ref="personService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioMaterialService" ref="bioMaterialService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="protocolService" ref="protocolService"
 * @spring.property name="softwareService" ref="softwareService"
 * @spring.property name="hardwareService" ref="hardwareService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="localFileService" ref="localFileService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name="compoundService" ref="compoundService"
 * @spring.property name="databaseEntryService" ref="databaseEntryService"
 * @spring.property name="contactService" ref="contactService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="factorValueService" ref="factorValueService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="reporterService" ref="reporterService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @spring.property name="quantitationTypeDimensionService" ref="quantitationTypeDimensionService"
 */
public class PersisterHelper implements Persister {
    private static Log log = LogFactory.getLog( PersisterHelper.class.getName() );

    private ArrayDesignService arrayDesignService;

    private BioAssayService bioAssayService;

    private BioMaterialService bioMaterialService;

    private BioSequenceService bioSequenceService;

    private CompoundService compoundService;

    private ContactService contactService;

    private DatabaseEntryService databaseEntryService;

    private Person defaultOwner = null;

    private ExpressionExperimentService expressionExperimentService;

    private ExternalDatabaseService externalDatabaseService;

    private FactorValueService factorValueService;

    private GeneService geneService;

    private HardwareService hardwareService;

    private LocalFileService localFileService;

    private OntologyEntryService ontologyEntryService;

    private PersonService personService;

    private ProtocolService protocolService;

    private QuantitationTypeService quantitationTypeService;

    private SoftwareService softwareService;

    private TaxonService taxonService;

    private CompositeSequenceService compositeSequenceService;

    private ReporterService reporterService;

    private DesignElementDataVectorService designElementDataVectorService;

    private BioAssayDimensionService bioAssayDimensionService;

    private QuantitationTypeDimensionService quantitationTypeDimensionService;

    private Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

    private boolean firstBioSequence = true;

    /*
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public Collection<Object> persist( Collection<Object> col ) {
        if ( defaultOwner == null ) initializeDefaultOwner();
        try {
            log.debug( "Entering + " + this.getClass().getName() + ".create() with " + col.size() + " objects." );
            for ( Object entity : col ) {
                persist( entity );
            }
        } catch ( Exception e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        }
        return col;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(edu.columbia.gemma.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    public Object persist( Object entity ) {

        if ( entity == null ) return null;

        log.debug( "Persisting " + entity.getClass().getName() + " " + entity );
        if ( entity instanceof ExpressionExperiment ) {
            return persistExpressionExperiment( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof ArrayDesign ) {
            return persistArrayDesign( ( ArrayDesign ) entity );
        } else if ( entity instanceof BioSequence ) {
            if ( firstBioSequence )
                log.warn( "*** Attempt to directly persist a BioSequence "
                        + "*** BioSequence are only persisted by association to other objects." );
            firstBioSequence = false;
            return null;
            // deal with in cascade from array design? Do nothing, probably.
        } else if ( entity instanceof Protocol ) {
            return null;
        } else if ( entity instanceof CompositeSequence ) {
            // if ( firstCompositeSequence )
            // log.warn( "*** Attempt to directly persist a CompositeSequence "
            // + "*** CompositeSequences are only persisted by association to other objects." );
            // firstCompositeSequence = false;
            // return null;
            return persistDesignElement( ( DesignElement ) entity );
            // cascade from array design, do nothing
        } else if ( entity instanceof Reporter ) {
            // if ( firstReporter )
            // log.warn( "*** Attempt to directly persist a reporter "
            // + "*** Reporters are only persisted by association to other objects." );
            // firstReporter = false;
            // return null;
            // // cascade from array design, do nothing
            return persistDesignElement( ( DesignElement ) entity );
        } else if ( entity instanceof Hardware ) {
            return null;
        } else if ( entity instanceof QuantitationType ) {
            return persistQuantitationType( ( QuantitationType ) entity );
        } else if ( entity instanceof BioMaterial ) {
            return persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof ExternalDatabase ) {
            return persistExternalDatabase( ( ExternalDatabase ) entity );
        } else if ( entity instanceof LocalFile ) {
            return persistLocalFile( ( LocalFile ) entity );
        } else if ( entity instanceof BioAssay ) {
            return persistBioAssay( ( BioAssay ) entity );
        } else if ( entity instanceof OntologyEntry ) {
            return persistOntologyEntry( ( OntologyEntry ) entity );
        } else if ( entity instanceof Software ) {
            return persistSoftware( ( Software ) entity );
        } else if ( entity instanceof Gene ) {
            return persistGene( ( Gene ) entity );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else if ( entity instanceof DesignElementDataVector ) {
            return persistDesignElementDataVector( ( DesignElementDataVector ) entity );
        } else if ( entity instanceof Taxon ) { // AS
            return persistTaxon( ( Taxon ) entity );
        } else if ( entity.getClass() == ( new HashMap() ).values().getClass() ) {
            // This is a kludge because Java thinks that HashMap() ).values() and Collections are not the same thing.
            // -PP
            return persist( ( Collection<Object> ) entity );
        } else if ( entity instanceof Collection ) {
            return persist( ( Collection<Object> ) entity );
        } else {
            throw new IllegalArgumentException( "Don't know how to persist a " + entity.getClass().getName() );
        }

    }

    /**
     * @param software
     * @return
     */
    @SuppressWarnings("unchecked")
    private Software persistSoftware( Software software ) {

        if ( !isTransient( software ) ) return software;

        Collection<Software> components = software.getSoftwareComponents();

        if ( components != null && components.size() > 0 ) {
            for ( Software component : components ) {
                component.setId( persistSoftware( component ).getId() );
            }
        }

        if ( software.getSoftwareManufacturers() != null && software.getSoftwareManufacturers().size() > 0 ) {
            for ( Contact manufacturer : ( Collection<Contact> ) software.getSoftwareManufacturers() ) {
                manufacturer.setId( persistContact( manufacturer ).getId() );
            }
        }

        software.setHardware( persistHardware( software.getHardware() ) );

        return softwareService.findOrCreate( software );

    }

    /**
     * @param designElement
     * @return
     */
    private DesignElement persistDesignElement( DesignElement designElement ) {
        if ( designElement == null ) return null;
        designElement.setArrayDesign( persistArrayDesign( designElement.getArrayDesign() ) );
        if ( !isTransient( designElement ) ) return designElement;
        if ( designElement instanceof CompositeSequence ) {
            return compositeSequenceService.findOrCreate( ( CompositeSequence ) designElement );
        } else if ( designElement instanceof Reporter ) {
            return reporterService.findOrCreate( ( Reporter ) designElement );
        } else {
            throw new IllegalArgumentException( "Unknown subclass of DesignElement" );
        }

    }

    /**
     * @param vector
     * @return
     */
    private DesignElementDataVector persistDesignElementDataVector( DesignElementDataVector vector ) {
        if ( vector == null ) return null;
        DesignElement designElement = vector.getDesignElement();

        if ( designElement instanceof CompositeSequence ) {
            CompositeSequence cs = compositeSequenceService.find( ( ( CompositeSequence ) designElement ) );
            if ( cs == null )
                throw new IllegalStateException(
                        "Cannot persist DesignElementDataVector until DesignElements are stored" );
            vector.setDesignElement( cs );
        } else if ( designElement instanceof Reporter ) {
            Reporter reporter = reporterService.find( ( Reporter ) designElement );
            if ( reporter == null )
                throw new IllegalStateException(
                        "Cannot persist DesignElementDataVector until DesignElements are stored" );
            vector.setDesignElement( reporter );
        }

        vector.setQuantitationType( persistQuantitationType( vector.getQuantitationType() ) );
        return designElementDataVectorService.findOrCreate( vector );
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * Fill in the categoryTerm and valueTerm associations of a
     * 
     * @param Characteristics Collection of Characteristics
     */
    private void fillInOntologyEntries( Collection<Characteristic> Characteristics ) {
        for ( Characteristic Characteristic : Characteristics ) {
            persistOntologyEntry( Characteristic.getCategoryTerm() );
            persistOntologyEntry( Characteristic.getValueTerm() );
        }
    }

    /**
     * @param databaseEntry
     */
    private DatabaseEntry fillInPersistentExternalDatabase( DatabaseEntry databaseEntry ) {
        assert externalDatabaseService != null;
        ExternalDatabase externalDatabase = databaseEntry.getExternalDatabase();
        if ( externalDatabase == null ) {
            log.debug( "No externalDatabase" );
            return null;
        }
        databaseEntry.setExternalDatabase( externalDatabaseService.findOrCreate( externalDatabase ) );
        return databaseEntry;
    }

    /**
     * @param ontologyEntry
     */
    @SuppressWarnings("unchecked")
    private void fillInPersistentExternalDatabase( OntologyEntry ontologyEntry ) {
        this.fillInPersistentExternalDatabase( ( DatabaseEntry ) ontologyEntry );
        for ( OntologyEntry associatedOntologyEntry : ( Collection<OntologyEntry> ) ontologyEntry.getAssociations() ) {
            fillInPersistentExternalDatabase( associatedOntologyEntry );
        }
    }

    /**
     * @param protocol
     */
    @SuppressWarnings("unchecked")
    private void fillInProtocol( Protocol protocol ) {
        if ( protocol == null ) {
            log.warn( "Null protocol" );
            return;
        }
        OntologyEntry type = protocol.getType();
        persistOntologyEntry( type );
        protocol.setType( type );

        for ( Software software : ( Collection<Software> ) protocol.getSoftwareUsed() ) {
            software.setId( persistSoftware( software ).getId() );
        }

        for ( Hardware hardware : ( Collection<Hardware> ) protocol.getHardwares() ) {
            hardware.setId( persistHardware( hardware ).getId() );
        }
    }

    /**
     * @param hardware
     * @return
     */
    @SuppressWarnings("unchecked")
    private Hardware persistHardware( Hardware hardware ) {

        if ( hardware == null ) return null;
        if ( !isTransient( hardware ) ) return hardware;

        if ( hardware.getSoftwares() != null && hardware.getSoftwares().size() > 0 ) {
            for ( Software software : ( Collection<Software> ) hardware.getSoftwares() ) {
                software.setId( persistSoftware( software ).getId() );
            }
        }

        hardware.setType( persistOntologyEntry( hardware.getType() ) );

        if ( hardware.getHardwareManufacturers() != null && hardware.getHardwareManufacturers().size() > 0 ) {
            for ( Contact manufacturer : ( Collection<Contact> ) hardware.getHardwareManufacturers() ) {
                manufacturer.setId( persistContact( manufacturer ).getId() );
            }
        }

        return hardwareService.findOrCreate( hardware );
    }

    /**
     * @param protocolApplication
     */
    @SuppressWarnings("unchecked")
    private void fillInProtocolApplication( ProtocolApplication protocolApplication ) {
        if ( protocolApplication == null ) return;

        log.debug( "Filling in protocolApplication" );

        Protocol protocol = protocolApplication.getProtocol();
        if ( protocol == null )
            throw new IllegalStateException( "Must have protocol associated with ProtocolApplication" );

        if ( protocol.getName() == null ) throw new IllegalStateException( "Protocol must have a name" );

        fillInProtocol( protocol );
        protocolApplication.setProtocol( protocolService.findOrCreate( protocol ) );

        for ( Person performer : ( Collection<Person> ) protocolApplication.getPerformers() ) {
            log.debug( "Filling in performer" );
            performer.setId( personService.findOrCreate( performer ).getId() );
        }

        for ( SoftwareApplication softwareApplication : ( Collection<SoftwareApplication> ) protocolApplication
                .getSoftwareApplications() ) {
            Software software = softwareApplication.getSoftware();
            if ( software == null )
                throw new IllegalStateException( "Must have software associated with SoftwareApplication" );

            OntologyEntry type = software.getType();
            persistOntologyEntry( type );
            software.setType( type );

            softwareApplication.setSoftware( softwareService.findOrCreate( software ) );

        }

        for ( HardwareApplication HardwareApplication : ( Collection<HardwareApplication> ) protocolApplication
                .getHardwareApplications() ) {
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
    private void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personService.findByFullName( "nobody", "nobody", "nobody" );

        assert matchingPersons.size() == 1 : "Found " + matchingPersons.size() + " contacts matching 'nobody'";

        defaultOwner = matchingPersons.iterator().next();

        if ( defaultOwner == null ) throw new NullPointerException( "Default Person 'nobody' not found in database." );
    }

    /**
     * Determine if a entity transient (not persistent).
     * 
     * @param ontologyEntry
     * @return
     */
    private boolean isTransient( Object entity ) {
        try {
            return BeanUtils.getSimpleProperty( entity, "id" ) == null;
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Persist an array design. If possible we avoid re-checking all the design elements. This is done by comparing the
     * number of design elements that already exist for the array design. If it is the same, no additional action is
     * going to be taken. In this case the array design will not be updated at all.
     * <p>
     * Therefore, if an array design needs to be updated (e.g., manufacturer or description) but the design elements
     * have already been entered, a different mechanism must be used.
     * 
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign persistArrayDesign( ArrayDesign arrayDesign ) {

        if ( !isTransient( arrayDesign ) ) return arrayDesign;

        arrayDesign.setDesignProvider( persistContact( arrayDesign.getDesignProvider() ) );
        ArrayDesign existing = arrayDesignService.find( arrayDesign );

        if ( existing != null ) {
            assert existing.getId() != null;
            log.info( "Array design " + existing.getName() + " already exists." );
            Collection<DesignElement> existingDesignElements = arrayDesign.getDesignElements();
            if ( existingDesignElements.size() == arrayDesign.getDesignElements().size() ) {
                log.warn( "Number of design elements in existing version of " + arrayDesign + " is the same ("
                        + existingDesignElements.size() + "). No further processing will be done." );
                return existing;
            } else if ( arrayDesign.getDesignElements().size() == 0 ) {
                log.warn( arrayDesign + ": No design elements in newly supplied version of " + arrayDesign
                        + ", no further processing of design elements will be done." );
                return existing;
            } else {
                log.info( "Array Design " + arrayDesign + " exists but design elements are to be updated." );
                arrayDesign = existing;
            }
        } else {
            log.info( "Array Design " + arrayDesign + " is new, processing..." );
        }

        int i = 0;
        log.info( "Filling in or updating sequences in design elements for " + arrayDesign );
        for ( DesignElement designElement : ( Collection<DesignElement> ) arrayDesign.getDesignElements() ) {
            designElement.setArrayDesign( arrayDesign );
            if ( designElement instanceof CompositeSequence ) {
                CompositeSequence cs = ( CompositeSequence ) designElement;
                cs.setBiologicalCharacteristic( persistBioSequence( cs.getBiologicalCharacteristic() ) );
            } else if ( designElement instanceof Reporter ) {
                Reporter reporter = ( Reporter ) designElement;
                reporter.setImmobilizedCharacteristic( persistBioSequence( reporter.getImmobilizedCharacteristic() ) );
            }
            i++;
            if ( i % 100 == 0 ) {
                try {
                    Thread.sleep( 10 );
                } catch ( InterruptedException e ) {
                    ;
                }
            }
            if ( i % 1000 == 0 ) {
                log.info( i + " design element sequences examined for " + arrayDesign );
            }
        }

        return arrayDesignService.findOrCreate( arrayDesign );
    }

    /**
     * @param assay
     */
    @SuppressWarnings("unchecked")
    private BioAssay persistBioAssay( BioAssay assay ) {

        if ( assay == null ) return null;

        if ( !isTransient( assay ) ) return assay;

        for ( FactorValue factorValue : ( Collection<FactorValue> ) assay.getFactorValues() ) {
            // factors are not compositioned in any more, but by assciation with the ExperimentalFactor.
            factorValue.setId( persistFactorValue( factorValue ).getId() );
        }

        assay.setAccession( persistDatabaseEntry( assay.getAccession() ) );

        for ( Iterator iter = assay.getArrayDesignsUsed().iterator(); iter.hasNext(); ) {
            ArrayDesign arrayDesign = ( ArrayDesign ) iter.next();
            arrayDesign.setId( persistArrayDesign( arrayDesign ).getId() );
        }

        for ( LocalFile file : ( Collection<LocalFile> ) assay.getDerivedDataFiles() ) {
            file.setId( persistLocalFile( file ).getId() );
        }

        for ( BioMaterial bioMaterial : ( Collection<BioMaterial> ) assay.getSamplesUsed() ) {
            bioMaterial.setId( persistBioMaterial( bioMaterial ).getId() );
        }

        LocalFile f = assay.getRawDataFile();
        if ( f != null ) {
            LocalFile persistentLocalFile = persistLocalFile( f );
            if ( persistentLocalFile != null ) {
                f.setId( persistentLocalFile.getId() );
            } else {
                log.error( "Null local file for " + f.getLocalURI() );
                throw new RuntimeException( "Null local file for" + f.getLocalURI() );
            }
        }

        return bioAssayService.findOrCreate( assay );
    }

    /**
     * @param entity
     */
    @SuppressWarnings("unchecked")
    private BioMaterial persistBioMaterial( BioMaterial entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setExternalAccession( persistDatabaseEntry( entity.getExternalAccession() ) );

        OntologyEntry materialType = entity.getMaterialType();
        if ( materialType != null ) {
            entity.setMaterialType( ontologyEntryService.findOrCreate( materialType ) );
        }

        for ( Treatment treatment : ( Collection<Treatment> ) entity.getTreatments() ) {
            OntologyEntry action = treatment.getAction();
            action.setId( persistOntologyEntry( action ).getId() );

            for ( ProtocolApplication protocolApplication : ( Collection<ProtocolApplication> ) treatment
                    .getProtocolApplications() ) {
                fillInProtocolApplication( protocolApplication );
            }
        }

        fillInOntologyEntries( entity.getCharacteristics() );

        return bioMaterialService.findOrCreate( entity );
    }

    /**
     * @param bioSequence
     */
    private BioSequence persistBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;
        fillInBioSequenceTaxon( bioSequence );
        if ( isTransient( bioSequence ) ) return bioSequenceService.findOrCreate( bioSequence );
        return bioSequence;
    }

    /**
     * @param bioSequence
     */
    private void fillInBioSequenceTaxon( BioSequence bioSequence ) {
        Taxon t = bioSequence.getTaxon();
        if ( t == null ) throw new IllegalArgumentException( "BioSequence Taxon cannot be null" );

        // Avoid trips to the database to get the taxon.
        String scientificName = t.getScientificName();
        String commonName = t.getCommonName();
        Integer ncbiId = t.getNcbiId();
        if ( scientificName != null && seenTaxa.get( scientificName ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( scientificName ) );
        } else if ( commonName != null && seenTaxa.get( commonName ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( commonName ) );
        } else if ( ncbiId != null && seenTaxa.get( ncbiId ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( ncbiId ) );
        } else if ( isTransient( t ) ) {
            Taxon foundOrCreatedTaxon = taxonService.findOrCreate( t );
            bioSequence.setTaxon( foundOrCreatedTaxon );
            if ( foundOrCreatedTaxon.getScientificName() != null )
                seenTaxa.put( foundOrCreatedTaxon.getScientificName(), bioSequence.getTaxon() );
            if ( foundOrCreatedTaxon.getCommonName() != null )
                seenTaxa.put( foundOrCreatedTaxon.getCommonName(), bioSequence.getTaxon() );
            if ( foundOrCreatedTaxon.getNcbiId() != null )
                seenTaxa.put( foundOrCreatedTaxon.getNcbiId(), bioSequence.getTaxon() );
        }
    }

    /**
     * @param compound
     * @return
     */
    private Compound persistCompound( Compound compound ) {
        if ( compound == null ) return null;
        persistOntologyEntry( compound.getCompoundIndices() );
        if ( compound.getIsSolvent() == null )
            throw new IllegalArgumentException( "Compound must have 'isSolvent' value set." );
        return compoundService.findOrCreate( compound );
    }

    /**
     * @param designProvider
     */
    private Contact persistContact( Contact designProvider ) {
        if ( designProvider == null ) return null;
        return this.contactService.findOrCreate( designProvider );
    }

    /**
     * @param databaseEntry
     * @return
     */
    private DatabaseEntry persistDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) return null;
        databaseEntry.setExternalDatabase( persistExternalDatabase( databaseEntry.getExternalDatabase() ) );
        return databaseEntryService.findOrCreate( databaseEntry );
    }

    /**
     * @param entity
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment persistExpressionExperiment( ExpressionExperiment entity ) {

        if ( entity == null ) return null;

        if ( entity.getOwner() == null ) {
            entity.setOwner( defaultOwner );
        }

        if ( entity.getAccession() != null && entity.getAccession().getExternalDatabase() != null ) {
            entity.setAccession( persistDatabaseEntry( entity.getAccession() ) );
        } else {
            log.warn( "Null accession for expressionExperiment" );
        }

        for ( ExperimentalDesign experimentalDesign : ( Collection<ExperimentalDesign> ) entity
                .getExperimentalDesigns() ) {

            // type
            for ( OntologyEntry type : ( Collection<OntologyEntry> ) experimentalDesign.getTypes() ) {
                type.setId( persistOntologyEntry( type ).getId() );
            }

            for ( ExperimentalFactor experimentalFactor : ( Collection<ExperimentalFactor> ) experimentalDesign
                    .getExperimentalFactors() ) {
                for ( OntologyEntry annotation : ( Collection<OntologyEntry> ) experimentalFactor.getAnnotations() ) {
                    annotation.setId( persistOntologyEntry( annotation ).getId() );
                }

                OntologyEntry category = experimentalFactor.getCategory();
                if ( category == null ) {
                    log.debug( "No 'category' for ExperimentalDesign" );
                } else {
                    persistOntologyEntry( category );
                    log.debug( "ExperimentalDesign.category=" + category.getId() );
                }

                for ( FactorValue factorValue : ( Collection<FactorValue> ) experimentalFactor.getFactorValues() ) {
                    factorValue.setId( persistFactorValue( factorValue ).getId() );
                }
            }
        }

        for ( BioAssay bA : ( Collection<BioAssay> ) entity.getBioAssays() ) {
            bA.setId( persistBioAssay( bA ).getId() );
        }

        for ( ExpressionExperimentSubSet subset : ( Collection<ExpressionExperimentSubSet> ) entity.getSubsets() ) {
            for ( BioAssay bA : ( Collection<BioAssay> ) subset.getBioAssays() ) {
                bA.setId( persistBioAssay( bA ).getId() );
            }
        }

        for ( DesignElementDataVector vect : ( Collection<DesignElementDataVector> ) entity
                .getDesignElementDataVectors() ) {

            DesignElement persistentDesignElement = null;
            DesignElement maybeExistingDesignElement = vect.getDesignElement();
            if ( maybeExistingDesignElement instanceof CompositeSequence ) {
                persistentDesignElement = compositeSequenceService
                        .find( ( CompositeSequence ) maybeExistingDesignElement );
            } else if ( maybeExistingDesignElement instanceof Reporter ) {
                persistentDesignElement = reporterService.find( ( Reporter ) maybeExistingDesignElement );
            }

            if ( persistentDesignElement == null ) {
                throw new IllegalStateException( maybeExistingDesignElement + " does not have a persistent version" );
            }

            ArrayDesign ad = persistentDesignElement.getArrayDesign();
            ad.setId( this.persistArrayDesign( ad ).getId() );

            vect.setBioAssayDimension( persistBioAssayDimension( vect.getBioAssayDimension() ) );

            vect.setDesignElement( persistentDesignElement );

            assert vect.getQuantitationType() != null;
            vect.getQuantitationType().setId( persistQuantitationType( vect.getQuantitationType() ).getId() );
        }

        return expressionExperimentService.findOrCreate( entity );
    }

    /**
     * @param bioAssayDimension
     * @return
     */
    @SuppressWarnings("unchecked")
    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) return null;
        if ( !isTransient( bioAssayDimension ) ) return bioAssayDimension;

        for ( BioAssay bioAssay : ( Collection<BioAssay> ) bioAssayDimension.getDimensionBioAssays() ) {
            bioAssay.setId( persistBioAssay( bioAssay ).getId() );
        }

        return bioAssayDimensionService.findOrCreate( bioAssayDimension );
    }

    /**
     * @param database
     */
    private ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {
        if ( database == null ) return null;
        log.debug( "Persisting " + database );
        return externalDatabaseService.findOrCreate( database );
    }

    /**
     * @param factorValue
     * @return
     */
    private FactorValue persistFactorValue( FactorValue factorValue ) {

        if ( factorValue.getOntologyEntry() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
            OntologyEntry ontologyEntry = factorValue.getOntologyEntry();
            ontologyEntry.setId( persistOntologyEntry( ontologyEntry ).getId() );
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getOntologyEntry() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        } else {
            // no need to do anything, the measurement will be cascaded in.
        }

        if ( isTransient( factorValue ) ) {
            return factorValueService.findOrCreate( factorValue );
        }
        return factorValue;
    }

    /**
     * @param gene
     */
    private Object persistGene( Gene gene ) {
        return geneService.findOrCreate( gene );
    }

    // AS
    /**
     * @param taxon
     */
    private Object persistTaxon( Taxon taxon ) {
        return taxonService.findOrCreate( taxon );
    }

    // end AS

    /**
     * @param file
     */
    private LocalFile persistLocalFile( LocalFile file ) {
        return localFileService.findOrCreate( file );
    }

    /**
     * Ontology entr
     * 
     * @param ontologyEntry
     */
    @SuppressWarnings("unchecked")
    private OntologyEntry persistOntologyEntry( OntologyEntry ontologyEntry ) {
        if ( ontologyEntry == null ) return null;
        fillInPersistentExternalDatabase( ontologyEntry );
        if ( isTransient( ontologyEntry ) )
            ontologyEntry.setId( ontologyEntryService.findOrCreate( ontologyEntry ).getId() );
        for ( OntologyEntry associatedOntologyEntry : ( Collection<OntologyEntry> ) ontologyEntry.getAssociations() ) {
            persistOntologyEntry( associatedOntologyEntry );
        }
        return ontologyEntry;
    }

    /**
     * @param entity
     */
    private QuantitationType persistQuantitationType( QuantitationType entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        return quantitationTypeService.findOrCreate( entity );
    }

    /**
     * @param entity
     */
    public void remove( Object entity ) {
        if ( entity instanceof Collection ) {
            this.remove( ( Collection ) entity );
        }
        throw new UnsupportedOperationException( "remove not supported" );
        // String entityName = ReflectionUtil.getBaseForImpl( entity ).getSimpleName();
        // String daoName = StringUtil.lowerCaseFirstLetter( entityName ) + "Dao";
        // FIXME, make this work.
    }

    /**
     * @param entity
     */
    public void remove( Collection entities ) {
        if ( entities == null ) return;
        for ( Object object : entities ) {
            remove( object );
        }
    }

    /**
     * @param bioAssayService The bioAssayService to set.
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param bioMaterialService The bioMaterialService to set.
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param bioSequenceService The bioSequenceService to set.
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param compositeSequenceService The compositeSequenceService to set.
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param compoundService The compoundService to set.
     */
    public void setCompoundService( CompoundService compoundService ) {
        this.compoundService = compoundService;
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
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param externalDatabaseService The externalDatabaseService to set.
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param factorValueService The factorValueService to set.
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

    /**
     * @param firstBioSequence The firstBioSequence to set.
     */
    public void setFirstBioSequence( boolean firstBioSequence ) {
        this.firstBioSequence = firstBioSequence;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
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
     * @param reporterService The reporterService to set.
     */
    public void setReporterService( ReporterService reporterService ) {
        this.reporterService = reporterService;
    }

    /**
     * @param seenTaxa The seenTaxa to set.
     */
    public void setSeenTaxa( Map<Object, Taxon> seenTaxa ) {
        this.seenTaxa = seenTaxa;
    }

    /**
     * @param softwareService The softwareService to set.
     */
    public void setSoftwareService( SoftwareService softwareService ) {
        this.softwareService = softwareService;
    }

    /**
     * @param taxonService The taxonService to set.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param bioAssayDimensionService The bioAssayDimensionService to set.
     */
    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * @param quantitationTypeDimensionService The quantitationTypeDimensionService to set.
     */
    public void setQuantitationTypeDimensionService( QuantitationTypeDimensionService quantitationTypeDimensionService ) {
        this.quantitationTypeDimensionService = quantitationTypeDimensionService;
    }
}
