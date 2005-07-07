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
package edu.columbia.gemma.loader.expression.mage;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.BioAssayData.BioAssayData;

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
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.biomaterial.TreatmentDao;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.loader.loaderutils.Loader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mageLoader"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 * @spring.property name="personDao" ref="personDao"
 * @spring.property name="expressionExperimentDao" ref="expressionExperimentDao"
 * @spring.property name="bioMaterialDao" ref="bioMaterialDao"
 * @spring.property name="arrayDesignDao" ref="arrayDesignDao"
 */
public class MageLoaderImpl implements Loader {
    private static Log log = LogFactory.getLog( MageLoaderImpl.class.getName() );

    private Person defaultOwner = null;

    private PersonDao personDao;

    private OntologyEntryDao ontologyEntryDao;

    private ExpressionExperimentDao expressionExperimentDao;

    private BioMaterialDao bioMaterialDao;

    private ArrayDesignDao arrayDesignDao;

    private ExternalDatabaseDao externalDatabaseDao;

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
    public MageLoaderImpl() {

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
                    // probably won't use this much.
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
        for ( OntologyEntry characteristic : ( Collection<OntologyEntry> ) entity.getCharacteristics() ) {
            fillInPersistentExternalDatabase( characteristic );
            characteristic.setId( ontologyEntryDao.findOrCreate( characteristic ).getId() );
        }

        OntologyEntry materialType = entity.getMaterialType();
        if ( materialType != null ) materialType.setId( ontologyEntryDao.findOrCreate( materialType ).getId() );

        for ( Treatment treatment : ( Collection<Treatment> ) entity.getTreatments() ) {
            OntologyEntry action = treatment.getAction();
            if ( action != null ) action.setId( ontologyEntryDao.findOrCreate( action ).getId() );
        }

        bioMaterialDao.create( entity );

    }

    /**
     * @param characteristic
     */
    private void fillInPersistentExternalDatabase( DatabaseEntry databaseEntry ) {
        ExternalDatabase externalDatabase = databaseEntry.getExternalDatabase();
        if ( externalDatabase == null ) return;

        databaseEntry.getExternalDatabase().setId( externalDatabaseDao.findOrCreate( externalDatabase ).getId() );

    }

    /**
     * @param entity
     */
    private void loadArrayDesign( ArrayDesign entity ) {
        throw new UnsupportedOperationException( "Can't deal with " + entity.getClass().getName() + " yet" );
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
                type.setId( ontologyEntryDao.findOrCreate( type ).getId() );
            }

            for ( ExperimentalFactor experimentalFactor : ( Collection<ExperimentalFactor> ) experimentalDesign
                    .getExperimentalFactors() ) {
                for ( OntologyEntry annotation : ( Collection<OntologyEntry> ) experimentalFactor.getAnnotations() ) {
                    annotation.setId( ontologyEntryDao.findOrCreate( annotation ).getId() );
                }

                OntologyEntry category = experimentalFactor.getCategory();
                if ( category == null ) {
                    log.debug( "No 'category' for ExperimentalDesign" );
                } else {
                    category.setId( ontologyEntryDao.findOrCreate( category ).getId() );
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
                        factorValue.setValue( ontologyEntryDao.findOrCreate( value ) );
                        log.debug( "factorValue.value=" + value.getId() );
                    }
                }
            }
        }

        // manually persist: experimentaldesign->bioassay->factorvalue->value and bioassay->arraydesign
        for ( BioAssay bA : ( Collection<BioAssay> ) ( ( ExpressionExperiment ) entity ).getBioAssays() ) {
            for ( FactorValue factorValue : ( Collection<FactorValue> ) bA.getBioAssayFactorValues() ) {
                for ( OntologyEntry value : ( Collection<OntologyEntry> ) factorValue.getValue() ) {
                    value.setId( ontologyEntryDao.findOrCreate( value ).getId() );
                }
            }

            for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) bA.getArrayDesignsUsed() ) {
                ArrayDesign persistentArrayDesign = arrayDesignDao.findOrCreate( arrayDesign );
                log.debug( "Arraydesign for bioassay " + bA.getName() + " is " + persistentArrayDesign.getName()
                        + " id=" + persistentArrayDesign.getId() );
                if ( persistentArrayDesign != null ) arrayDesign.setId( persistentArrayDesign.getId() );
            }
        }

        expressionExperimentDao.create( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(edu.columbia.gemma.genome.Gene)
     */
    public void create( Object Obj ) {

        // TODO Auto-generated method stub

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
