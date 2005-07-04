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

import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.Treatment;
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
 */
public class MageLoaderImpl implements Loader {
    private static Log log = LogFactory.getLog( MageLoaderImpl.class.getName() );

    private Person defaultOwner = null;

    private PersonDao personDao;

    private OntologyEntryDao ontologyEntryDao;

    private ExpressionExperimentDao expressionExperimentDao;

    private BioMaterialDao bioMaterialDao;

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
                log.debug( "Loading " + className );

                // check if className is on short list of classes to be persisted.
                // ArrayDesign (we won't usually use this - mage-ml of array designs is gigantic.)
                // ExpressionExperiment (most interested in this)
                // 
                if ( entity instanceof ExpressionExperiment ) {
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
                    loadBioMaterial( ( BioMaterial ) entity );
                } else if ( entity instanceof ExternalDatabase ) {
                    // probably won't use this much.
                    // loadExternalDatabase( ( ExternalDatabase ) entity );
                } else if ( entity instanceof LocalFile ) {
                    // loadLocalFile( ( LocalFile ) entity );
                } else {
                    throw new UnsupportedOperationException( "Sorry, can't deal with " + className );
                }
            }
        } catch ( Exception e ) {
            log.error( e, e );
        }
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
            characteristic = ( OntologyEntry ) ontologyEntryDao.findOrCreate( characteristic );
        }

        OntologyEntry materialType = entity.getMaterialType();
        materialType = ( OntologyEntry ) ontologyEntryDao.findOrCreate( materialType );

        for ( Treatment treatment : ( Collection<Treatment> ) entity.getTreatments() ) {

            // find or create the treatment object.
            OntologyEntry treatmentType = treatment.getTreatmentType();
            treatmentType = ( OntologyEntry ) ontologyEntryDao.findOrCreate( treatmentType );
        }

        bioMaterialDao.create( entity );

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
        for ( ExperimentalDesign experimentalDesign : ( Collection<ExperimentalDesign> ) entity
                .getExperimentalDesigns() ) {
            for ( ExperimentalFactor experimentalFactor : ( Collection<ExperimentalFactor> ) experimentalDesign
                    .getExperimentalFactors() ) {
                for ( OntologyEntry annotation : ( Collection<OntologyEntry> ) experimentalFactor.getAnnotations() ) {
                    annotation = ( OntologyEntry ) ontologyEntryDao.findOrCreate( annotation );
                }

                OntologyEntry category = experimentalFactor.getCategory();
                category = ( OntologyEntry ) ontologyEntryDao.findOrCreate( category );

                for ( FactorValue factorValue : ( Collection<FactorValue> ) experimentalFactor.getFactorValues() ) {
                    OntologyEntry value = factorValue.getValue();
                }
            }
        }

        // manually persist: experimentaldesign->bioassay->factorvalue->value
        for ( BioAssay bA : ( Collection<BioAssay> ) ( ( ExpressionExperiment ) entity ).getBioAssays() ) {
            for ( FactorValue factorValue : ( Collection<FactorValue> ) bA.getBioAssayFactorValues() ) {
                for ( OntologyEntry value : ( Collection<OntologyEntry> ) factorValue.getValue() ) {
                    value = ( OntologyEntry ) ontologyEntryDao.findOrCreate( value );
                }
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

}
