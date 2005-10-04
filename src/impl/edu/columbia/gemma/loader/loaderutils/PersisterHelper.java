/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.Characteristic;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseEntryDao;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.common.protocol.Hardware;
import edu.columbia.gemma.common.protocol.HardwareApplication;
import edu.columbia.gemma.common.protocol.HardwareDao;
import edu.columbia.gemma.common.protocol.Protocol;
import edu.columbia.gemma.common.protocol.ProtocolApplication;
import edu.columbia.gemma.common.protocol.ProtocolDao;
import edu.columbia.gemma.common.protocol.Software;
import edu.columbia.gemma.common.protocol.SoftwareApplication;
import edu.columbia.gemma.common.protocol.SoftwareDao;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssay.BioAssayDao;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.Compound;
import edu.columbia.gemma.expression.biomaterial.CompoundDao;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentSubSet;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.expression.experiment.FactorValueDao;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.biosequence.BioSequenceDao;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="persisterHelper"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 * @spring.property name="personDao" ref="personDao"
 * @spring.property name="expressionExperimentDao" ref="expressionExperimentDao"
 * @spring.property name="bioMaterialDao" ref="bioMaterialDao"
 * @spring.property name="arrayDesignDao" ref="arrayDesignDao"
 * @spring.property name="designElementDao" ref="designElementDao"
 * @spring.property name="protocolDao" ref="protocolDao"
 * @spring.property name="softwareDao" ref="softwareDao"
 * @spring.property name="hardwareDao" ref="hardwareDao"
 * @spring.property name="geneDao" ref="geneDao"
 * @spring.property name="taxonDao" ref="taxonDao"
 * @spring.property name="localFileDao" ref="localFileDao"
 * @spring.property name="bioAssayDao" ref="bioAssayDao"
 * @spring.property name="externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name="quantitationTypeDao" ref="quantitationTypeDao"
 * @spring.property name="compoundDao" ref="compoundDao"
 * @spring.property name="databaseEntryDao" ref="databaseEntryDao"
 * @spring.property name="contactDao" ref="contactDao"
 * @spring.property name="bioSequenceDao" ref="bioSequenceDao"
 * @spring.property name="factorValueDao" ref="factorValueDao"
 */
public class PersisterHelper implements Persister {
    private static Log log = LogFactory.getLog( PersisterHelper.class.getName() );

    private ArrayDesignDao arrayDesignDao;

    private BioAssayDao bioAssayDao;

    private BioMaterialDao bioMaterialDao;

    private BioSequenceDao bioSequenceDao;

    private CompoundDao compoundDao;

    private ContactDao contactDao;

    private DatabaseEntryDao databaseEntryDao;

    private Person defaultOwner = null;

    private DesignElementDao designElementDao;

    private ExpressionExperimentDao expressionExperimentDao;

    private ExternalDatabaseDao externalDatabaseDao;

    private FactorValueDao factorValueDao;

    private GeneDao geneDao;

    private HardwareDao hardwareDao;

    private LocalFileDao localFileDao;

    private OntologyEntryDao ontologyEntryDao;

    private PersonDao personDao;

    private ProtocolDao protocolDao;

    private QuantitationTypeDao quantitationTypeDao;

    private SoftwareDao softwareDao;

    private TaxonDao taxonDao;

    private Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

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
    public Object persist( Object entity ) {
        if ( entity == null ) return null;
        log.debug( "Persisting " + entity.getClass().getName() + " " + entity );
        if ( entity instanceof ExpressionExperiment ) {
            return persistExpressionExperiment( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof ArrayDesign ) {
            return persistArrayDesign( ( ArrayDesign ) entity );
        } else if ( entity instanceof BioSequence ) {
            return null;
            // deal with in cascade from array design? Do nothing, probably.
        } else if ( entity instanceof Protocol ) {
            return null;
        } else if ( entity instanceof CompositeSequence ) {
            return null;
            // cascade from array design, do nothing
        } else if ( entity instanceof Reporter ) {
            return null;
            // cascade from array design, do nothing
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
        } else if ( entity instanceof Gene ) {
            return persistGene( ( Gene ) entity );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else {
            log.error( "Can't deal with " + entity.getClass().getName() );
            return null;
        }
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param bioAssayDao The bioAssayDao to set.
     */
    public void setBioAssayDao( BioAssayDao bioAssayDao ) {
        this.bioAssayDao = bioAssayDao;
    }

    /**
     * @param bioMaterialDao The bioMaterialDao to set.
     */
    public void setBioMaterialDao( BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    /**
     * @param bioSequenceDao The bioSequenceDao to set.
     */
    public void setBioSequenceDao( BioSequenceDao bioSequenceDao ) {
        this.bioSequenceDao = bioSequenceDao;
    }

    /**
     * @param compoundDao The compoundDao to set.
     */
    public void setCompoundDao( CompoundDao compoundDao ) {
        this.compoundDao = compoundDao;
    }

    /**
     * @param contactDao The contactDao to set.
     */
    public void setContactDao( ContactDao contactDao ) {
        this.contactDao = contactDao;
    }

    /**
     * @param databaseEntryDao The databaseEntryDao to set.
     */
    public void setDatabaseEntryDao( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /**
     * @param designElementDao The designElementDao to set.
     */
    public void setDesignElementDao( DesignElementDao designElementDao ) {
        this.designElementDao = designElementDao;
    }

    /**
     * @param expressionExperimentDao The expressionExperimentDao to set.
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    /**
     * @param externalDatabaseDao The externalDatabaseDao to set.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * @param factorValueDao The factorValueDao to set.
     */
    public void setFactorValueDao( FactorValueDao factorValueDao ) {
        this.factorValueDao = factorValueDao;
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @param hardwareDao The hardwareDao to set.
     */
    public void setHardwareDao( HardwareDao hardwareDao ) {
        this.hardwareDao = hardwareDao;
    }

    /**
     * @param localFileDao The localFileDao to set.
     */
    public void setLocalFileDao( LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

    /**
     * @param ontologyEntryDao
     */
    public void setOntologyEntryDao( OntologyEntryDao ontologyEntryDao ) {
        this.ontologyEntryDao = ontologyEntryDao;
    }

    /**
     * @param personDao
     */
    public void setPersonDao( PersonDao personDao ) {
        this.personDao = personDao;
    }

    /**
     * @param protocolDao The protocolDao to set
     */
    public void setProtocolDao( ProtocolDao protocolDao ) {
        this.protocolDao = protocolDao;
    }

    /**
     * @param quantitationTypeDao The quantitationTypeDao to set.
     */
    public void setQuantitationTypeDao( QuantitationTypeDao quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    /**
     * @param softwareDao The softwareDao to set.
     */
    public void setSoftwareDao( SoftwareDao softwareDao ) {
        this.softwareDao = softwareDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
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
        assert externalDatabaseDao != null;
        ExternalDatabase externalDatabase = databaseEntry.getExternalDatabase();
        if ( externalDatabase == null ) {
            log.debug( "No externalDatabase" );
            return null;
        }
        databaseEntry.setExternalDatabase( externalDatabaseDao.findOrCreate( externalDatabase ) );
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
            software = softwareDao.findOrCreate( software );
        }

        for ( Hardware hardware : ( Collection<Hardware> ) protocol.getHardwares() ) {
            hardware = hardwareDao.findOrCreate( hardware );
        }
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
        protocolApplication.setProtocol( protocolDao.findOrCreate( protocol ) );

        for ( Person performer : ( Collection<Person> ) protocolApplication.getPerformers() ) {
            log.debug( "Filling in performer" );
            performer.setId( personDao.findOrCreate( performer ).getId() );
        }

        for ( SoftwareApplication softwareApplication : ( Collection<SoftwareApplication> ) protocolApplication
                .getSoftwareApplications() ) {
            Software software = softwareApplication.getSoftware();
            if ( software == null )
                throw new IllegalStateException( "Must have software associated with SoftwareApplication" );

            OntologyEntry type = software.getType();
            persistOntologyEntry( type );
            software.setType( type );

            softwareApplication.setSoftware( softwareDao.findOrCreate( software ) );

        }

        for ( HardwareApplication HardwareApplication : ( Collection<HardwareApplication> ) protocolApplication
                .getHardwareApplications() ) {
            Hardware hardware = HardwareApplication.getHardware();
            if ( hardware == null )
                throw new IllegalStateException( "Must have hardware associated with HardwareApplication" );

            OntologyEntry type = hardware.getType();
            persistOntologyEntry( type );
            hardware.setType( type );

            HardwareApplication.setHardware( hardwareDao.findOrCreate( hardware ) );
        }
    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    @SuppressWarnings("unchecked")
    private void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personDao.findByFullName( "nobody", "nobody", "nobody" );

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
     * @param entity
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign persistArrayDesign( ArrayDesign entity ) {

        entity.setDesignProvider( persistContact( entity.getDesignProvider() ) );
        ArrayDesign existing = arrayDesignDao.find( entity );

        if ( existing != null ) {
            assert existing.getId() != null;
            log.info( "Array design " + existing.getName() + " already exists." );
            Collection<DesignElement> existingDesignElements = entity.getDesignElements();
            if ( existingDesignElements.size() == entity.getDesignElements().size() ) {
                log.warn( "Number of design elements in existing version " + "is the same ("
                        + existingDesignElements.size() + "). No further processing will be done." );
                return existing;
            } else if ( entity.getDesignElements().size() == 0 ) {
                log.warn( entity
                        + ": No design elements in newly supplied version, no further processing will be done." );
                return existing;
            } else {
                log.warn( "Design exists but design elements are to be updated." );
                entity = existing;
            }
        }

        int i = 0;
        log.debug( "Filling in design elements for " + entity );
        for ( DesignElement designElement : ( Collection<DesignElement> ) entity.getDesignElements() ) {
            designElement.setArrayDesign( entity );
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
                log.info( i + " design elements examined." );
            }
        }

        return arrayDesignDao.findOrCreate( entity );
    }

    /**
     * @param assay
     */
    @SuppressWarnings("unchecked")
    private BioAssay persistBioAssay( BioAssay assay ) {

        if ( assay == null ) return null;

        for ( FactorValue factorValue : ( Collection<FactorValue> ) assay.getFactorValues() ) {
            // factors are not compositioned in any more, but by assciation with the ExperimentalFactor.
            factorValue.setId( persistFactorValue( factorValue ).getId() );
        }

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

        return bioAssayDao.findOrCreate( assay );
    }

    /**
     * @param entity
     */
    @SuppressWarnings("unchecked")
    private BioMaterial persistBioMaterial( BioMaterial entity ) {

        entity.setExternalAccession( persistDatabaseEntry( entity.getExternalAccession() ) );

        OntologyEntry materialType = entity.getMaterialType();
        if ( materialType != null ) {
            entity.setMaterialType( ontologyEntryDao.findOrCreate( materialType ) );
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

        return bioMaterialDao.findOrCreate( entity );
    }

    /**
     * @param bioSequence
     */
    private BioSequence persistBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;
        fillInBioSequenceTaxon( bioSequence );
        if ( isTransient( bioSequence ) ) return bioSequenceDao.findOrCreate( bioSequence );
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
            Taxon foundOrCreatedTaxon = taxonDao.findOrCreate( t );
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
        return compoundDao.findOrCreate( compound );
    }

    /**
     * @param designProvider
     */
    private Contact persistContact( Contact designProvider ) {
        return this.contactDao.findOrCreate( designProvider );
    }

    /**
     * @param databaseEntry
     * @return
     */
    private DatabaseEntry persistDatabaseEntry( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) return null;
        databaseEntry.setExternalDatabase( persistExternalDatabase( databaseEntry.getExternalDatabase() ) );
        return databaseEntryDao.findOrCreate( databaseEntry );
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
            DesignElement persistentDesignElement = designElementDao.find( vect.getDesignElement() );
            if ( persistentDesignElement == null ) {
                log.error( vect.getDesignElement() + " does not have a persistent version" );
                continue;
            }

            ArrayDesign ad = persistentDesignElement.getArrayDesign();
            ad.setId( this.persistArrayDesign( ad ).getId() );

            vect.setDesignElement( persistentDesignElement );
        }

        // FIXME:java.lang.RuntimeException: org.springframework.orm.hibernate3.HibernateSystemException: a different
        // object with the same identifier value was already associated with the session:
        // [edu.columbia.gemma.expression.bioAssay.BioAssayImpl#26]; nested exception is
        // org.hibernate.NonUniqueObjectException: a different object with the same identifier value was already
        // associated with the session: [edu.columbia.gemma.expression.bioAssay.BioAssayImpl#26]
        // at edu.columbia.gemma.loader.loaderutils.PersisterHelper.persist(PersisterHelper.java:175)
        // at edu.columbia.gemma.loader.expression.geo.GeoDatasetService.fetchAndLoad(GeoDatasetService.java:63)
        // at
        // edu.columbia.gemma.loader.expression.geo.GeoDatasetServiceTest.testFetchAndLoadD(GeoDatasetServiceTest.java:108)
        // at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        // at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
        // at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
        // at java.lang.reflect.Method.invoke(Method.java:585)
        // at junit.framework.TestCase.runTest(TestCase.java:154)
        // at junit.framework.TestCase.runBare(TestCase.java:127)
        // at junit.framework.TestResult$1.protect(TestResult.java:106)
        // at junit.framework.TestResult.runProtected(TestResult.java:124)
        // at junit.framework.TestResult.run(TestResult.java:109)
        // at junit.framework.TestCase.run(TestCase.java:118)
        // at junit.framework.TestSuite.runTest(TestSuite.java:208)
        // at junit.framework.TestSuite.run(TestSuite.java:203)
        // at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:478)
        // at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:344)
        // at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:196)
        // Caused by: org.springframework.orm.hibernate3.HibernateSystemException: a different object with the same
        // identifier value was already associated with the session:
        // [edu.columbia.gemma.expression.bioAssay.BioAssayImpl#26]; nested exception is
        // org.hibernate.NonUniqueObjectException: a different object with the same identifier value was already
        // associated with the session: [edu.columbia.gemma.expression.bioAssay.BioAssayImpl#26]
        // at
        // org.springframework.orm.hibernate3.SessionFactoryUtils.convertHibernateAccessException(SessionFactoryUtils.java:656)
        // at
        // org.springframework.orm.hibernate3.HibernateAccessor.convertHibernateAccessException(HibernateAccessor.java:413)
        // at org.springframework.orm.hibernate3.HibernateTemplate.execute(HibernateTemplate.java:320)
        // at org.springframework.orm.hibernate3.HibernateTemplate.save(HibernateTemplate.java:552)
        // at
        // edu.columbia.gemma.expression.experiment.ExpressionExperimentDaoBase.create(ExpressionExperimentDaoBase.java:99)
        // at
        // edu.columbia.gemma.expression.experiment.ExpressionExperimentDaoBase.create(ExpressionExperimentDaoBase.java:86)
        // at
        // edu.columbia.gemma.expression.experiment.ExpressionExperimentDaoImpl.findOrCreate(ExpressionExperimentDaoImpl.java:89)
        // at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        // at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
        // at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
        // at java.lang.reflect.Method.invoke(Method.java:585)
        // at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:292)
        // at
        // org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:155)
        // at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:122)
        // at org.springframework.orm.hibernate3.HibernateInterceptor.invoke(HibernateInterceptor.java:97)
        // at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:144)
        // at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:174)
        // at $Proxy1.findOrCreate(Unknown Source)
        // at
        // edu.columbia.gemma.loader.loaderutils.PersisterHelper.persistExpressionExperiment(PersisterHelper.java:754)
        // at edu.columbia.gemma.loader.loaderutils.PersisterHelper.persist(PersisterHelper.java:189)
        // at edu.columbia.gemma.loader.loaderutils.PersisterHelper.persist(PersisterHelper.java:171)
        // ... 17 more
        // Caused by: org.hibernate.NonUniqueObjectException: a different object with the same identifier value was
        // already associated with the session: [edu.columbia.gemma.expression.bioAssay.BioAssayImpl#26]
        // at org.hibernate.engine.PersistenceContext.checkUniqueness(PersistenceContext.java:586)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.performUpdate(DefaultSaveOrUpdateEventListener.java:254)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.entityIsDetached(DefaultSaveOrUpdateEventListener.java:214)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.performSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:91)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.onSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:69)
        // at org.hibernate.impl.SessionImpl.saveOrUpdate(SessionImpl.java:468)
        // at org.hibernate.engine.Cascades$5.cascade(Cascades.java:154)
        // at org.hibernate.engine.Cascades.cascadeAssociation(Cascades.java:771)
        // at org.hibernate.engine.Cascades.cascade(Cascades.java:720)
        // at org.hibernate.engine.Cascades.cascadeCollection(Cascades.java:895)
        // at org.hibernate.engine.Cascades.cascadeAssociation(Cascades.java:792)
        // at org.hibernate.engine.Cascades.cascade(Cascades.java:720)
        // at org.hibernate.engine.Cascades.cascade(Cascades.java:847)
        // at org.hibernate.event.def.AbstractSaveEventListener.cascadeAfterSave(AbstractSaveEventListener.java:363)
        // at
        // org.hibernate.event.def.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:265)
        // at org.hibernate.event.def.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:160)
        // at org.hibernate.event.def.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:95)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.saveWithGeneratedOrRequestedId(DefaultSaveOrUpdateEventListener.java:184)
        // at
        // org.hibernate.event.def.DefaultSaveEventListener.saveWithGeneratedOrRequestedId(DefaultSaveEventListener.java:33)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.entityIsTransient(DefaultSaveOrUpdateEventListener.java:173)
        // at org.hibernate.event.def.DefaultSaveEventListener.performSaveOrUpdate(DefaultSaveEventListener.java:27)
        // at
        // org.hibernate.event.def.DefaultSaveOrUpdateEventListener.onSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:69)
        // at org.hibernate.impl.SessionImpl.save(SessionImpl.java:481)
        // at org.hibernate.impl.SessionImpl.save(SessionImpl.java:476)
        // at org.springframework.orm.hibernate3.HibernateTemplate$12.doInHibernate(HibernateTemplate.java:555)
        // at org.springframework.orm.hibernate3.HibernateTemplate.execute(HibernateTemplate.java:315)
        // ... 35 more

        return expressionExperimentDao.findOrCreate( entity );
    }

    /**
     * @param database
     */
    private ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {
        if ( database == null ) return null;
        return externalDatabaseDao.findOrCreate( database );
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
            return factorValueDao.create( factorValue );
        }
        return factorValue;
    }

    /**
     * @param gene
     */
    private Object persistGene( Gene gene ) {
        return geneDao.findOrCreate( gene );
    }

    /**
     * @param file
     */
    private LocalFile persistLocalFile( LocalFile file ) {
        return localFileDao.findOrCreate( file );
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
            ontologyEntry.setId( ontologyEntryDao.findOrCreate( ontologyEntry ).getId() );
        for ( OntologyEntry associatedOntologyEntry : ( Collection<OntologyEntry> ) ontologyEntry.getAssociations() ) {
            persistOntologyEntry( associatedOntologyEntry );
        }
        return ontologyEntry;
    }

    /**
     * @param entity
     */
    private QuantitationType persistQuantitationType( QuantitationType entity ) {
        return quantitationTypeDao.findOrCreate( entity );
    }

    /**
     * @param entity
     */
    public void remove( Object entity ) {
        if ( entity instanceof Collection ) {
            this.remove( ( Collection ) entity );
        }
        // String entityName = ReflectionUtil.getBaseForImpl( entity ).getSimpleName();
        // String daoName = StringUtil.lowerCaseFirstLetter( entityName ) + "Dao";
        // FIXME, make this work.
    }

    /**
     * @param entity
     */
    public void remove( Collection entities ) {
        for ( Object object : entities ) {
            remove( object );
        }
    }
}
