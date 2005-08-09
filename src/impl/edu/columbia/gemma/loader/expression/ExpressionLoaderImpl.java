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
package edu.columbia.gemma.loader.expression;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.BioMaterialCharacteristic;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.loader.loaderutils.Loader;

/**
 * A generic class to persist Gemma-domain objects from the Expression package: arrayDesigns, expressionExperiments and
 * the like.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionDataLoader"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 * @spring.property name="personDao" ref="personDao"
 * @spring.property name="expressionExperimentDao" ref="expressionExperimentDao"
 * @spring.property name="bioMaterialDao" ref="bioMaterialDao"
 * @spring.property name="arrayDesignDao" ref="arrayDesignDao"
 * @spring.property name="designElementDao" ref="designElementDao"
 */
public class ExpressionLoaderImpl implements Loader {
    private static Log log = LogFactory.getLog( ExpressionLoaderImpl.class.getName() );

    private Person defaultOwner = null;

    private PersonDao personDao;

    private OntologyEntryDao ontologyEntryDao;

    private ExpressionExperimentDao expressionExperimentDao;

    private BioMaterialDao bioMaterialDao;

    private ArrayDesignDao arrayDesignDao;

    private ExternalDatabaseDao externalDatabaseDao;

    private DesignElementDao designElementDao;

    /**
     * @param designElementDao The designElementDao to set.
     */
    public void setDesignElementDao( DesignElementDao designElementDao ) {
        this.designElementDao = designElementDao;
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * 
     *
     */
    public ExpressionLoaderImpl() {

    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    private void initializeDefaultOwner() {
        Collection<Person> matchingPersons = personDao.findByFullName( "nobody", "nobody", "nobody" );

        assert matchingPersons.size() == 1;

        defaultOwner = matchingPersons.iterator().next();

        if ( defaultOwner == null ) throw new NullPointerException( "Default Person 'nobody' not found in database." );
    }

    /*
     * (non-Javadoc) TODO: finish implementing this.
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public void create( Collection col ) {
        log.debug( "Entering MageLoaderImpl.create()" );
        if ( defaultOwner == null ) initializeDefaultOwner();
        try {

            for ( Object entity : col ) {

                String className = entity.getClass().getName();

                // check if className is on short list of classes to be persisted.
                // ArrayDesign (we won't usually use this - mage-ml of array designs is gigantic.)
                // ExpressionExperiment (most interested in this)
                // 
                if ( entity instanceof ExpressionExperiment ) {
                    log.debug( "Loading " + className );
                    loadExpressionExperiment( ( ExpressionExperiment ) entity );
                } else if ( entity instanceof ArrayDesign ) {
                    // loadArrayDesign( ( ArrayDesign ) entity );
                } else if ( entity instanceof BioSequence ) {
                    // deal with in cascade from array design? Do nothing, probably.
                } else if ( entity instanceof CompositeSequence ) {
                    // cascade from array design, do nothing
                } else if ( entity instanceof Reporter ) {
                    // cascade from array design, do nothing
                } else if ( entity instanceof QuantitationType ) {
                    // loadQuantitationType( ( QuantitationType ) entity );
                } else if ( entity instanceof BioMaterial ) {
                    log.debug( "Loading " + className );
                    loadBioMaterial( ( BioMaterial ) entity );
                } else if ( entity instanceof ExternalDatabase ) {
                    // probably won't use this much; or get from associations via ontologyentry.
                    // loadExternalDatabase( ( ExternalDatabase ) entity );
                } else if ( entity instanceof LocalFile ) {
                    // loadLocalFile( ( LocalFile ) entity );
                } else if ( entity instanceof BioAssay ) {
                    // loadBioAssay( ( BioAssay ) entity );
                } else {
                    throw new UnsupportedOperationException( "Sorry, can't deal with " + className );
                }
            }
        } catch ( Exception e ) {
            log.error( e, e );
        }
    }

    // private void loadBioAssayData(BioAssayData data) {
    // throw new UnsupportedOperationException( "Can't deal with " + data.getClass().getName() + " yet" );
    // }

    /**
     * @param assay
     */
    private void loadBioAssay( BioAssay assay ) {
        throw new UnsupportedOperationException( "Can't deal with " + assay.getClass().getName() + " yet" );
    }

    /**
     * @param file
     */
    private void loadLocalFile( LocalFile file ) {
        throw new UnsupportedOperationException( "Can't deal with " + file.getClass().getName() + " yet" );
    }

    /**
     * @param database
     */
    private void loadExternalDatabase( ExternalDatabase database ) {
        throw new UnsupportedOperationException( "Can't deal with " + database.getClass().getName() + " yet" );
    }

    /**
     * @param entity
     */
    private void loadQuantitationType( QuantitationType entity ) {
        throw new UnsupportedOperationException( "Can't deal with " + entity.getClass().getName() + " yet" );
    }

    /**
     * @param entity
     */
    private void loadBioMaterial( BioMaterial entity ) {
        for ( BioMaterialCharacteristic characteristic : ( Collection<BioMaterialCharacteristic> ) entity
                .getBioMaterialCharacteristics() ) {
            persistBioMaterialCharacteristics( characteristic );
        }

        OntologyEntry materialType = entity.getMaterialType();
        if ( materialType != null ) {
            entity.setMaterialType( ontologyEntryDao.findOrCreate( materialType ) );
        }

        for ( Treatment treatment : ( Collection<Treatment> ) entity.getTreatments() ) {
            OntologyEntry action = treatment.getAction();
            persistOntologyEntry( action );
        }

        bioMaterialDao.create( entity );
    }

    /**
     * @param characteristic
     */
    private void persistBioMaterialCharacteristics( BioMaterialCharacteristic characteristic ) {
        // TODO Auto-generated method stub

    }

    /**
     * @param databaseEntry
     */
    private DatabaseEntry fillInPersistentExternalDatabase( DatabaseEntry databaseEntry ) {
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
    private void fillInPersistentExternalDatabase( OntologyEntry ontologyEntry ) {
        this.fillInPersistentExternalDatabase( ( DatabaseEntry ) ontologyEntry );
        for ( OntologyEntry associatedOntologyEntry : ( Collection<OntologyEntry> ) ontologyEntry.getAssociations() ) {
            fillInPersistentExternalDatabase( associatedOntologyEntry );
        }
    }

    /**
     * Ontology entr
     * 
     * @param ontologyEntry
     */
    private void persistOntologyEntry( OntologyEntry ontologyEntry ) {
        fillInPersistentExternalDatabase( ontologyEntry );
        ontologyEntry.setId( ontologyEntryDao.findOrCreate( ontologyEntry ).getId() );
        for ( OntologyEntry associatedOntologyEntry : ( Collection<OntologyEntry> ) ontologyEntry.getAssociations() ) {
            persistOntologyEntry( associatedOntologyEntry );
        }
    }

    /**
     * @param entity
     */
    private void loadArrayDesign( ArrayDesign entity ) {

        arrayDesignDao.create( entity );
    }

    /**
     * @param entity
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void loadExpressionExperiment( ExpressionExperiment entity ) {
        if ( entity.getOwner() == null ) {
            entity.setOwner( defaultOwner );
        }

        // this is very annoying code.
        // the following ontology entries must be persisted manually.
        // manually persist: experimentaldesign->experimentalFactor->annotation, category
        // manually persist: experimentaldesign->experimentalFactor->FactorValue->value
        // experimentaldesign->type
        for ( ExperimentalDesign experimentalDesign : ( Collection<ExperimentalDesign> ) entity
                .getExperimentalDesigns() ) {

            // type
            for ( OntologyEntry type : ( Collection<OntologyEntry> ) experimentalDesign.getTypes() ) {
                persistOntologyEntry( type );
            }

            for ( ExperimentalFactor experimentalFactor : ( Collection<ExperimentalFactor> ) experimentalDesign
                    .getExperimentalFactors() ) {
                for ( OntologyEntry annotation : ( Collection<OntologyEntry> ) experimentalFactor.getAnnotations() ) {
                    persistOntologyEntry( annotation );
                }

                OntologyEntry category = experimentalFactor.getCategory();
                if ( category == null ) {
                    log.debug( "No 'category' for ExperimentalDesign" );
                } else {
                    persistOntologyEntry( category );
                    log.debug( "ExperimentalDesign.category=" + category.getId() );
                }

                for ( FactorValue factorValue : ( Collection<FactorValue> ) experimentalFactor.getFactorValues() ) {

                    OntologyEntry value = factorValue.getValue();

                    if ( value == null ) {
                        log.debug( "No 'value' for FactorValue" ); // that's okay, it can be a measurement.
                        if ( factorValue.getMeasurement() == null ) {
                            throw new IllegalStateException( "FactorValue must have either a measurement or a value" );
                        }
                    } else {
                        if ( factorValue.getMeasurement() != null ) {
                            throw new IllegalStateException( "FactorValue cannot have both a measurement and a value" );
                        }
                        persistOntologyEntry( value );
                        // factorValue.setValue( value );
                        log.debug( "factorValue.value=" + value.getId() );
                    }
                }
            }
        }

        // manually persist: experimentaldesign->bioassay->factorvalue->value and bioassay->arraydesign
        for ( BioAssay bA : ( Collection<BioAssay> ) ( ( ExpressionExperiment ) entity ).getBioAssays() ) {
            for ( FactorValue factorValue : ( Collection<FactorValue> ) bA.getBioAssayFactorValues() ) {
                for ( OntologyEntry value : ( Collection<OntologyEntry> ) factorValue.getValue() ) {
                    persistOntologyEntry( value );
                }
            }

            for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) bA.getArrayDesignsUsed() ) {
                ArrayDesign persistentArrayDesign = arrayDesignDao.findOrCreate( arrayDesign );
                log.debug( "Arraydesign for bioassay " + bA.getName() + " is " + persistentArrayDesign.getName()
                        + " id=" + persistentArrayDesign.getId() );
                if ( persistentArrayDesign != null ) arrayDesign.setId( persistentArrayDesign.getId() );
            }
        }

        // manually persist expressionExperiment-->designElementDataVector-->DesignElement
        // FIXME - make sure this looks at the arraydesign.
        for ( DesignElementDataVector vect : ( Collection<DesignElementDataVector> ) ( ( ExpressionExperiment ) entity )
                .getDesignElementDataVectors() ) {
            DesignElement persistentDesignElement = designElementDao.find( vect.getDesignElement() );
            if ( persistentDesignElement == null ) {
                log.error( vect.getDesignElement() + " does not have a persistent version" );
                continue;
            }
            vect.setDesignElement( persistentDesignElement );
        }

        expressionExperimentDao.create( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(edu.columbia.gemma.genome.Gene)
     */
    public void create( Object obj ) {
        log.info( "Persisting " + obj.getClass().getName() + " " + obj );
        if ( obj instanceof ExpressionExperiment ) {
            this.loadExpressionExperiment( ( ExpressionExperiment ) obj );
        } else if ( obj instanceof BioMaterial ) {
            this.loadBioMaterial( ( BioMaterial ) obj );
        } else if ( obj instanceof BioAssay ) {
            this.loadBioAssay( ( BioAssay ) obj );
        } else if ( obj instanceof ArrayDesign ) {
            this.loadArrayDesign( ( ArrayDesign ) obj );
        } else {
            throw new UnsupportedOperationException( "Can't deal with " + obj.getClass().getName() );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#removeAll()
     */
    public void removeAll() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#removeAll(java.util.Collection)
     */
    public void removeAll( Collection collection ) {
        // TODO Auto-generated method stub

    }

    public void setOntologyEntryDao( OntologyEntryDao ontologyEntryDao ) {
        this.ontologyEntryDao = ontologyEntryDao;
    }

    public void setPersonDao( PersonDao personDao ) {
        this.personDao = personDao;
    }

    /**
     * @param bioMaterialDao The bioMaterialDao to set.
     */
    public void setBioMaterialDao( BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
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

}
