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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.BioMaterialDimension;
import ubic.gemma.model.expression.bioAssayData.BioMaterialDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.biomaterial.CompoundService;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.util.BeanPropertyCompleter;

/**
 * @spring.property name="factorValueService" ref="factorValueService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @spring.property name="bioMaterialDimensionService" ref="bioMaterialDimensionService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioMaterialService" ref="bioMaterialService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 * @spring.property name="compoundService" ref="compoundService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class ExpressionPersister extends ArrayDesignPersister {

    private DesignElementDataVectorService designElementDataVectorService;

    private ExpressionExperimentService expressionExperimentService;

    private BioAssayDimensionService bioAssayDimensionService;

    private BioAssayService bioAssayService;

    private BioMaterialDimensionService bioMaterialDimensionService;

    private BioMaterialService bioMaterialService;

    private FactorValueService factorValueService;

    private CompoundService compoundService;

    Map<String, BioAssayDimension> bioAssayDimensionCache = new HashMap<String, BioAssayDimension>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        if ( log.isDebugEnabled() ) {
            log.debug( "Persisting: " + entity.getClass().getSimpleName() + " (" + entity + ")" );
        }

        if ( entity instanceof ExpressionExperiment ) {
            return persistExpressionExperiment( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof BioAssayDimension ) {
            return null; // done via expression experiments.
        } else if ( DesignElement.class.isAssignableFrom( entity.getClass() ) ) {
            return persistDesignElement( ( DesignElement ) entity );
        } else if ( entity instanceof BioMaterial ) {
            return persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof BioAssay ) {
            return persistBioAssay( ( BioAssay ) entity );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else if ( entity instanceof DesignElementDataVector ) {
            return persistDesignElementDataVector( ( DesignElementDataVector ) entity );
        }
        return super.persist( entity );

    }

    /**
     * @param vect
     */
    private void fillInDesignElementDataVectorAssociations( DesignElementDataVector vect ) {
        DesignElement persistentDesignElement = null;
        DesignElement maybeExistingDesignElement = vect.getDesignElement();

        assert maybeExistingDesignElement != null;
        ArrayDesign ad = maybeExistingDesignElement.getArrayDesign();

        cacheArrayDesign( ad );

        String key = maybeExistingDesignElement.getName() + " " + ad.getName();

        if ( designElementCache.containsKey( key ) ) {
            persistentDesignElement = designElementCache.get( key );
        } else {
            persistentDesignElement = getPersistentDesignElement( persistentDesignElement, maybeExistingDesignElement,
                    key );
        }
        assert persistentDesignElement != null;
        assert persistentDesignElement.getId() != null;
        vect.setDesignElement( persistentDesignElement );

        checkBioAssayDimensionCache( vect );

        assert vect.getQuantitationType() != null;
        vect.setQuantitationType( persistQuantitationType( vect.getQuantitationType() ) );
    }

    /**
     * @param entity
     */
    private void fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment entity ) {

        log.info( "Filling in DesignElementDataVectors" );

        int count = 0;
        for ( DesignElementDataVector vect : entity.getDesignElementDataVectors() ) {

            fillInDesignElementDataVectorAssociations( vect );

            if ( ++count % 2000 == 0 ) {
                log.info( "Filled in " + count + " DesignElementDataVectors" );
            }
        }
        log.info( "Done, filled in " + count + " DesignElementDataVectors" );
    }

    /**
     * @param assay
     */

    private BioAssay persistBioAssay( BioAssay assay ) {

        if ( assay == null ) return null;

        if ( !isTransient( assay ) ) return assay;

        log.info( "Persisting " + assay );

        fillInBioAssayAssociations( assay );

        return bioAssayService.findOrCreate( assay );
    }

    /**
     * @param assay
     */
    private void fillInBioAssayAssociations( BioAssay assay ) {
        for ( ArrayDesign arrayDesign : assay.getArrayDesignsUsed() ) {
            arrayDesign.setId( persistArrayDesign( arrayDesign ).getId() );
        }

        for ( FactorValue factorValue : assay.getFactorValues() ) {
            // factors are not compositioned in any more, but by association with the ExperimentalFactor.
            factorValue.setId( persistFactorValue( factorValue ).getId() );
        }

        // assay.setAccession( persistDatabaseEntry( assay.getAccession() ) );
        
        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
//        assay.getAccession()
//                .setExternalDatabase( persistExternalDatabase( assay.getAccession().getExternalDatabase() ) );

        for ( BioMaterial bioMaterial : assay.getSamplesUsed() ) {
            bioMaterial.setId( persistBioMaterial( bioMaterial ).getId() );
        }

        assay.setRawDataFile( persistLocalFile( assay.getRawDataFile() ) );

        for ( LocalFile file : assay.getDerivedDataFiles() ) {
            file.setId( persistLocalFile( file ).getId() );
        }
    }

    /**
     * @param bioAssayDimension
     * @return
     */
    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) return null;
        if ( !isTransient( bioAssayDimension ) ) return bioAssayDimension;

        List<BioAssay> persistedBioAssays = new ArrayList<BioAssay>();
        for ( BioAssay bioAssay : bioAssayDimension.getDimensionBioAssays() ) {
            persistedBioAssays.add( persistBioAssay( bioAssay ) );
        }
        bioAssayDimension.setDimensionBioAssays( persistedBioAssays );

        Collection<BioMaterialDimension> persistedBioMaterialDimensions = new HashSet<BioMaterialDimension>();
        for ( BioMaterialDimension bad : bioAssayDimension.getBioMaterialDimensions() ) {
            persistedBioMaterialDimensions.add( persistBioMaterialDimension( bad ) );
        }
        bioAssayDimension.setBioMaterialDimensions( persistedBioMaterialDimensions );

        return bioAssayDimensionService.findOrCreate( bioAssayDimension );
    }

    /**
     * @param entity
     */

    private BioMaterial persistBioMaterial( BioMaterial entity ) {
        if ( entity == null ) return null;
        log.debug( "Persisting " + entity );
        if ( !isTransient( entity ) ) return entity;

        entity.setExternalAccession( persistDatabaseEntry( entity.getExternalAccession() ) );
        entity.setMaterialType( persistOntologyEntry( entity.getMaterialType() ) );

        for ( Treatment treatment : entity.getTreatments() ) {

            OntologyEntry action = treatment.getAction();
            treatment.setAction( persistOntologyEntry( action ) );
            log.debug( treatment + " action: " + action );

            for ( ProtocolApplication protocolApplication : treatment.getProtocolApplications() ) {
                fillInProtocolApplication( protocolApplication );
            }
        }

        fillInOntologyEntries( entity.getCharacteristics() );

        return bioMaterialService.findOrCreate( entity );
    }

    /**
     * @param bioMaterialDimension
     * @return
     */
    private BioMaterialDimension persistBioMaterialDimension( BioMaterialDimension bioMaterialDimension ) {
        assert bioMaterialDimensionService != null;
        List<BioMaterial> persistentBioMaterials = new ArrayList<BioMaterial>();
        for ( BioMaterial bioMaterial : bioMaterialDimension.getBioMaterials() ) {
            persistentBioMaterials.add( persistBioMaterial( bioMaterial ) );
        }
        bioMaterialDimension.setBioMaterials( persistentBioMaterials );
        return bioMaterialDimensionService.findOrCreate( bioMaterialDimension );
    }

    /**
     * @param entity
     * @return
     */
    private ExpressionExperiment persistExpressionExperiment( ExpressionExperiment entity ) {

        if ( entity == null ) return null;

        if ( !isTransient( entity ) ) {
            return entity;
        }

        if ( entity.getOwner() == null ) {
            entity.setOwner( defaultOwner );
        }

        if ( entity.getAccession() != null && entity.getAccession().getExternalDatabase() != null ) {
            entity.setAccession( persistDatabaseEntry( entity.getAccession() ) );
        } else {
            log.warn( "Null accession for expressionExperiment" );
        }

        for ( ExperimentalDesign experimentalDesign : entity.getExperimentalDesigns() ) {

            for ( OntologyEntry type : experimentalDesign.getTypes() ) {
                type = persistOntologyEntry( type );
            }

            for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

                for ( OntologyEntry annotation : experimentalFactor.getAnnotations() ) {
                    annotation = persistOntologyEntry( annotation );
                }

                OntologyEntry category = experimentalFactor.getCategory();
                if ( category == null ) {
                    log.debug( "No 'category' for ExperimentalDesign" );
                } else {
                    persistOntologyEntry( category );
                    log.debug( "ExperimentalDesign.category=" + category.getId() );
                }

                for ( FactorValue factorValue : experimentalFactor.getFactorValues() ) {
                    factorValue = persistFactorValue( factorValue );
                }
            }
        }

        Collection<BioAssay> alreadyFilled = new HashSet<BioAssay>();
        for ( ExpressionExperimentSubSet subset : entity.getSubsets() ) {
            for ( BioAssay bA : subset.getBioAssays() ) {
                fillInBioAssayAssociations( bA );
                alreadyFilled.add( bA );
            }
        }

        for ( BioAssay bA : entity.getBioAssays() ) {
            if ( alreadyFilled.contains( bA ) ) continue;
            fillInBioAssayAssociations( bA );
        }

        fillInExpressionExperimentDataVectorAssociations( entity );

        log.info( "Filled in references, persisting ExpressionExperiment " + entity );

        ExpressionExperiment ee = expressionExperimentService.find( entity );

        // FIXME make sure this works...this would allow update.
        if ( ee != null ) {
            BeanPropertyCompleter.complete( ee, entity );
            expressionExperimentService.update( ee );
        }

        return expressionExperimentService.findOrCreate( entity );
    }

    /**
     * @param factorValue
     * @return
     */
    private FactorValue persistFactorValue( FactorValue factorValue ) {
        if ( factorValue == null ) return null;
        if ( !isTransient( factorValue ) ) return factorValue;

        if ( factorValue.getOntologyEntry() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
            OntologyEntry ontologyEntry = factorValue.getOntologyEntry();
            ontologyEntry = persistOntologyEntry( ontologyEntry );
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getOntologyEntry() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        } else {
            Measurement measurement = factorValue.getMeasurement();
            measurement = persistMeasurement( measurement );
        }

        return factorValueService.findOrCreate( factorValue );
    }

    /**
     * This is used when creating vectors "one by one" rather than by composition with an ExpressionExperiment.
     * 
     * @param vector
     * @return FIXME we may not want to use this, and always do it with an update of the ExpressionExperiment instead.
     */
    private DesignElementDataVector persistDesignElementDataVector( DesignElementDataVector vector ) {
        if ( vector == null ) return null;
        this.fillInDesignElementDataVectorAssociations( vector );
        vector.setExpressionExperiment( persistExpressionExperiment( vector.getExpressionExperiment() ) );
        return designElementDataVectorService.findOrCreate( vector );
    }

    /**
     * @param bioAssayDimensionCache
     * @param vect
     */
    private void checkBioAssayDimensionCache( DesignElementDataVector vect ) {
        if ( !isTransient( vect.getBioAssayDimension() ) ) return;
        if ( bioAssayDimensionCache.containsKey( vect.getBioAssayDimension().getName() ) ) {
            vect.setBioAssayDimension( bioAssayDimensionCache.get( vect.getBioAssayDimension().getName() ) );
        } else {
            BioAssayDimension bAd = persistBioAssayDimension( vect.getBioAssayDimension() );
            bioAssayDimensionCache.put( vect.getBioAssayDimension().getName(), bAd );
            vect.setBioAssayDimension( bAd );
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
     * @param bioAssayDimensionService The bioAssayDimensionService to set.
     */
    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * @param bioAssayService The bioAssayService to set.
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param bioMaterialDimensionService The bioMaterialDimensionService to set.
     */
    public void setBioMaterialDimensionService( BioMaterialDimensionService bioMaterialDimensionService ) {
        this.bioMaterialDimensionService = bioMaterialDimensionService;
    }

    /**
     * @param bioMaterialService The bioMaterialService to set.
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param compoundService The compoundService to set.
     */
    public void setCompoundService( CompoundService compoundService ) {
        this.compoundService = compoundService;
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
     * @param factorValueService The factorValueService to set.
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

}
