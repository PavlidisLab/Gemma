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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayDao;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.biomaterial.CompoundDao;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignDao;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueDao;

/**
 * @author pavlidis
 * @version $Id$
 */
abstract public class ExpressionPersister extends ArrayDesignPersister {

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    private BioAssayDao bioAssayDao;

    @Autowired
    private BioMaterialDao bioMaterialDao;

    @Autowired
    private CompoundDao compoundDao;

    @Autowired
    private ExperimentalDesignDao experimentalDesignDao;

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    private FactorValueDao factorValueDao;

    @Autowired
    private ExpressionExperimentPrePersistService expressionExperimentPrePersistService;

    private Map<String, BioAssayDimension> bioAssayDimensionCache = new HashMap<String, BioAssayDimension>();

    /**
     * @param ee
     * @return
     */
    public ExpressionExperiment persist( ExpressionExperiment ee, ArrayDesignsForExperimentCache c ) {

        if ( ee == null ) return null;
        if ( !isTransient( ee ) ) return ee;

        log.info( "Persisting " + ee );
        clearCache();

        ExpressionExperiment existingEE = expressionExperimentDao.findByShortName( ee.getShortName() );
        if ( existingEE != null ) {
            log.warn( "Expression experiment with same short name exists (" + existingEE
                    + "), returning it (this method does not handle updates)" );
            return existingEE;
        }

        ee.setPrimaryPublication( ( BibliographicReference ) persist( ee.getPrimaryPublication() ) );

        if ( ee.getOwner() == null ) {
            ee.setOwner( defaultOwner );
        }
        ee.setOwner( ( Contact ) persist( ee.getOwner() ) );

        persistCollectionElements( ee.getQuantitationTypes() );
        persistCollectionElements( ee.getOtherRelevantPublications() );
        persistCollectionElements( ee.getInvestigators() );

        if ( ee.getAccession() != null ) {
            fillInDatabaseEntry( ee.getAccession() );
        }

        // This has to come first and be persisted, so our FactorValues get persisted before we process the BioAssays.
        if ( ee.getExperimentalDesign() != null ) {
            ExperimentalDesign experimentalDesign = ee.getExperimentalDesign();
            processExperimentalDesign( experimentalDesign );
            assert experimentalDesign.getId() != null;
            ee.setExperimentalDesign( experimentalDesign );
        }

        checkExperimentalDesign( ee );

        // This does most of the preparatory work.
        processBioAssays( ee, c );

        ee = expressionExperimentDao.create( ee );

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled" );
            expressionExperimentDao.remove( ee );
            throw new java.util.concurrent.CancellationException( "Thread canceled during EE persisting. "
                    + this.getClass() );
        }
        clearCache();
        return ee;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.Persister#prepare(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return expressionExperimentPrePersistService.prepare( ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        if ( entity instanceof ExpressionExperiment ) {
            log.warn( "Consider doing the 'setup' step in a separate transaction" );
            ArrayDesignsForExperimentCache c = expressionExperimentPrePersistService
                    .prepare( ( ExpressionExperiment ) entity );
            return persist( ( ExpressionExperiment ) entity, c );
        } else if ( entity instanceof BioAssayDimension ) {
            return persistBioAssayDimension( ( BioAssayDimension ) entity, null );
        } else if ( entity instanceof BioMaterial ) {
            return persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof BioAssay ) {
            return persistBioAssay( ( BioAssay ) entity, null );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        }
        return super.persist( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        return super.persistOrUpdate( entity );
    }

    /**
     * If there are factorvalues, check if they are setup right and if they are used by biomaterials.
     * 
     * @param expExp
     */
    private void checkExperimentalDesign( ExpressionExperiment expExp ) {

        if ( expExp == null ) {
            return;
        }

        if ( expExp.getExperimentalDesign() == null ) {
            log.warn( "No experimental design!" );
            return;
        }

        Collection<ExperimentalFactor> efs = expExp.getExperimentalDesign().getExperimentalFactors();

        if ( efs.size() == 0 ) return;

        log.info( "Checking experimental design for valid setup" );

        Collection<BioAssay> bioAssays = expExp.getBioAssays();

        /*
         * note this is very inefficient but it doesn't matter.
         */
        for ( ExperimentalFactor ef : efs ) {
            log.info( "Checking: " + ef + ", " + ef.getFactorValues().size() + " factor values to check..." );

            for ( FactorValue fv : ef.getFactorValues() ) {

                if ( fv.getExperimentalFactor() == null || !fv.getExperimentalFactor().equals( ef ) ) {
                    throw new IllegalStateException( "Factor value " + fv + " should have had experimental factor "
                            + ef + ", it had " + fv.getExperimentalFactor() );
                }

                boolean found = false;
                // Make sure there is at least one bioassay using it.
                for ( BioAssay ba : bioAssays ) {
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        for ( FactorValue fvb : bm.getFactorValues() ) {

                            // They should be persistent already at this point.
                            if ( ( fvb.getId() != null || fv.getId() != null ) && fvb.equals( fv ) && fvb == fv ) {
                                // Note we use == because they should be the same objects.
                                found = true;
                            }
                        }
                    }
                }
                if ( !found ) {
                    /*
                     * Basically this means there is factorvalue but no biomaterial is associated with it. This can
                     * happen...especially with test objects, so we just warn.
                     */
                    // throw new IllegalStateException( "Unused factorValue: No bioassay..biomaterial association with "
                    // + fv );
                    log.warn( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                }
            }

        }
    }

    private void clearCache() {
        bioAssayDimensionCache.clear();
        clearCommonCache();
    }

    /**
     * @param bioAssay
     * @param c
     */
    private void fillInBioAssayAssociations( BioAssay bioAssay, ArrayDesignsForExperimentCache c ) {

        ArrayDesign arrayDesign = bioAssay.getArrayDesignUsed();

        if ( c != null && c.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            bioAssay.setArrayDesignUsed( c.getArrayDesignCache().get( arrayDesign.getShortName() ) );
        }

        if ( bioAssay.getArrayDesignUsed().getId() == null ) {
            throw new UnsupportedOperationException(
                    "Bioassay cannot be persisted this way, unless array design is already in the system." );
        }

        boolean hadFactors = false;
        for ( BioMaterial material : bioAssay.getSamplesUsed() ) {
            for ( FactorValue factorValue : material.getFactorValues() ) {
                // Factors are not compositioned in any more, but by association with the ExperimentalFactor.
                fillInFactorValueAssociations( factorValue );
                factorValue = persistFactorValue( factorValue );
                hadFactors = true;
            }
        }

        if ( hadFactors ) log.debug( "factor values done" );

        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( bioAssay.getAccession() != null ) {
            bioAssay.getAccession().setExternalDatabase(
                    persistExternalDatabase( bioAssay.getAccession().getExternalDatabase() ) );
            log.debug( "external database done" );
        }

        // BioMaterials
        persistCollectionElements( bioAssay.getSamplesUsed() );

        log.debug( "biomaterials done" );

        if ( bioAssay.getRawDataFile() != null ) {
            bioAssay.setRawDataFile( persistLocalFile( bioAssay.getRawDataFile() ) );
            log.debug( "raw data file done" );
        }

        for ( LocalFile file : bioAssay.getDerivedDataFiles() ) {
            file = persistLocalFile( file );
        }

        log.debug( "Done with " + bioAssay );

    }

    /**
     * @param dataVector
     */
    private BioAssayDimension fillInDesignElementDataVectorAssociations( DesignElementDataVector dataVector,
            ArrayDesignsForExperimentCache c ) {
        CompositeSequence probe = dataVector.getDesignElement();

        assert probe != null;

        ArrayDesign arrayDesign = probe.getArrayDesign();
        assert arrayDesign != null : probe + " does not have an array design";

        arrayDesign = c.getArrayDesignCache().get( arrayDesign.getShortName() );

        assert arrayDesign != null;

        String key = probe.getName() + ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR
                + arrayDesign.getName();

        if ( log.isDebugEnabled() ) log.debug( "Seeking design element matching key=" + key );
        if ( c.getDesignElementCache().containsKey( key ) ) {
            probe = c.getDesignElementCache().get( key );
            if ( log.isDebugEnabled() ) log.debug( "Found " + probe + " with key=" + key );
        } else {
            throw new IllegalStateException( "No platform for the EE has a probe matching: " + probe
                    + ": was it set up correctly?" );
        }

        assert probe != null && probe.getId() != null;
        dataVector.setDesignElement( probe ); // use the persistent one.

        BioAssayDimension bioAssayDimension = getBioAssayDimensionFromCacheOrCreate( dataVector, c );

        assert dataVector.getQuantitationType() != null;
        dataVector.setQuantitationType( persistQuantitationType( dataVector.getQuantitationType() ) );

        return bioAssayDimension;
    }

    /**
     * @param experimentalFactor
     * @return
     */
    private ExperimentalFactor fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null ) return null;
        if ( !isTransient( experimentalFactor ) ) return experimentalFactor;

        persistCollectionElements( experimentalFactor.getAnnotations() );

        return experimentalFactor;
    }

    /**
     * @param ee
     */
    private Collection<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment ee,
            ArrayDesignsForExperimentCache c ) {
        log.info( "Filling in DesignElementDataVectors..." );

        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();
        StopWatch timer = new StopWatch();
        timer.start();
        int count = 0;
        for ( DesignElementDataVector dataVector : ee.getRawExpressionDataVectors() ) {
            BioAssayDimension bioAssayDimension = fillInDesignElementDataVectorAssociations( dataVector, c );
            bioAssays.addAll( bioAssayDimension.getBioAssays() );

            if ( timer.getTime() > 5000 ) {
                log.info( "Filled in " + ( count ) + " DesignElementDataVectors (" + timer.getTime()
                        + "ms since last check)" );
                timer.reset();
                timer.start();
            }

            ++count;

            if ( Thread.interrupted() ) {
                log.info( "Cancelled" );
                return null;
            }
        }

        log.info( "Filled in total of " + count + " DesignElementDataVectors, " + bioAssays.size() + " bioassays" );
        return bioAssays;
    }

    /**
     * @param factorValue
     */
    private void fillInFactorValueAssociations( FactorValue factorValue ) {

        fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor() );

        factorValue.setExperimentalFactor( persistExperimentalFactor( factorValue.getExperimentalFactor() ) );

        if ( factorValue.getCharacteristics().size() > 0 ) {
            if ( factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getCharacteristics().size() > 0 ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        }

        // measurement will cascade, but not unit.
        if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
            factorValue.getMeasurement().setUnit( persistUnit( factorValue.getMeasurement().getUnit() ) );
        }

    }

    /**
     * @param bioAssayDimensionCache
     * @param vect
     */
    private BioAssayDimension getBioAssayDimensionFromCacheOrCreate( DesignElementDataVector vect,
            ArrayDesignsForExperimentCache c ) {
        if ( !isTransient( vect.getBioAssayDimension() ) ) return vect.getBioAssayDimension();
        assert bioAssayDimensionCache != null;
        String dimensionName = vect.getBioAssayDimension().getName();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vect.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            BioAssayDimension bAd = persistBioAssayDimension( vect.getBioAssayDimension(), c );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vect.setBioAssayDimension( bAd );
        }
        return bioAssayDimensionCache.get( dimensionName );
    }

    /**
     * @param assay
     */
    private BioAssay persistBioAssay( BioAssay assay, ArrayDesignsForExperimentCache c ) {

        if ( assay == null ) return null;
        if ( !isTransient( assay ) ) {
            return assay;
        }
        log.debug( "Persisting " + assay );
        fillInBioAssayAssociations( assay, c );

        /*
         * PP changed this to use 'create', as we don't want BioAssays associated with two ExpressionExperiments.
         * BioAssays don't exist on their own so this wouldn't get called in any conceivable situation where
         * findOrCreate would be appropriate (?)
         */
        return bioAssayDao.create( assay );
    }

    /**
     * @param bioAssayDimension
     * @return
     */
    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension,
            ArrayDesignsForExperimentCache c ) {
        if ( bioAssayDimension == null ) return null;
        if ( !isTransient( bioAssayDimension ) ) return bioAssayDimension;
        log.debug( "Persisting bioAssayDimension" );
        List<BioAssay> persistedBioAssays = new ArrayList<BioAssay>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            persistedBioAssays.add( persistBioAssay( bioAssay, c ) );
            if ( persistedBioAssays.size() % 10 == 0 ) {
                log.info( "Persisted: " + persistedBioAssays.size() + " bioassays" );
            }
        }
        log.debug( "Done persisting " + persistedBioAssays.size() + " bioassays" );
        assert persistedBioAssays.size() > 0;
        bioAssayDimension.setBioAssays( persistedBioAssays );
        return bioAssayDimensionDao.findOrCreate( bioAssayDimension );
    }

    /**
     * @param entity
     */
    private BioMaterial persistBioMaterial( BioMaterial entity ) {
        if ( entity == null ) return null;
        log.debug( "Persisting " + entity );
        if ( !isTransient( entity ) ) return entity;

        assert entity.getSourceTaxon() != null;

        log.debug( "Persisting " + entity );
        fillInDatabaseEntry( entity.getExternalAccession() );

        log.debug( "dbentry done" );
        entity.setSourceTaxon( persistTaxon( entity.getSourceTaxon() ) );

        log.debug( "taxon done" );

        for ( Treatment treatment : entity.getTreatments() ) {

            Characteristic action = treatment.getAction();
            log.debug( treatment + " action: " + action );

            for ( ProtocolApplication protocolApplication : treatment.getProtocolApplications() ) {
                fillInProtocolApplication( protocolApplication );
                log.debug( "protocol done" );
            }
            log.debug( "treatment done" );
        }
        log.debug( "start save" );
        BioMaterial bm = bioMaterialDao.findOrCreate( entity );
        log.debug( "save biomaterial done" );

        return bm;
    }

    /**
     * @param compound
     * @return
     */
    private Compound persistCompound( Compound compound ) {
        if ( compound == null ) return null;
        if ( compound.getIsSolvent() == null )
            throw new IllegalArgumentException( "Compound must have 'isSolvent' value set." );
        return compoundDao.findOrCreate( compound );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     * 
     * @param experimentalFactor
     * @return
     */
    private ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        if ( !isTransient( experimentalFactor ) || experimentalFactor == null ) return experimentalFactor;
        assert experimentalFactor.getType() != null;
        fillInExperimentalFactorAssociations( experimentalFactor );
        // assert ( !isTransient( experimentalFactor.getExperimentalDesign() ) );
        return experimentalFactorDao.create( experimentalFactor );
    }

    /**
     * @param entity
     * @return
     */
    private ExpressionExperimentSubSet persistExpressionExperimentSubSet( ExpressionExperimentSubSet entity ) {
        if ( !isTransient( entity ) ) return entity;

        if ( entity.getBioAssays().size() == 0 ) {
            throw new IllegalArgumentException( "Cannot make a subset with no bioassays" );
        } else if ( isTransient( entity.getSourceExperiment() ) ) {
            throw new IllegalArgumentException(
                    "Subsets are only supported for expressionexperiments that are already persistent" );
        }

        return expressionExperimentSubSetDao.findOrCreate( entity );
    }

    /**
     * If we get here first (e.g., via bioAssay->bioMaterial) we have to override the cascade.
     * 
     * @param factorValue
     * @return
     */
    private FactorValue persistFactorValue( FactorValue factorValue ) {
        if ( factorValue == null ) return null;
        if ( !isTransient( factorValue ) ) return factorValue;
        if ( isTransient( factorValue.getExperimentalFactor() ) ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        fillInFactorValueAssociations( factorValue );

        return factorValueDao.findOrCreate( factorValue );

    }

    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     * 
     * @param expressionExperiment
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment, ArrayDesignsForExperimentCache c ) {

        Collection<BioAssay> alreadyFilled = new HashSet<BioAssay>();

        if ( expressionExperiment.getRawExpressionDataVectors().isEmpty() ) {
            log.info( "Filling in bioassays" );
            for ( BioAssay bioAssay : expressionExperiment.getBioAssays() ) {
                fillInBioAssayAssociations( bioAssay, c );
                alreadyFilled.add( bioAssay );
            }
        } else {
            log.info( "Filling in bioassays via data vectors" ); // usual case.
            alreadyFilled = fillInExpressionExperimentDataVectorAssociations( expressionExperiment, c );
            expressionExperiment.setBioAssays( alreadyFilled );
        }
    }

    /**
     * @param experimentalDesign
     */
    private void processExperimentalDesign( ExperimentalDesign experimentalDesign ) {

        persistCollectionElements( experimentalDesign.getTypes() );

        // Withhold to avoid premature cascade.
        Collection<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors();
        if ( factors == null ) {
            factors = new HashSet<ExperimentalFactor>();
        }
        experimentalDesign.setExperimentalFactors( null );

        // Note we use create because this is specific to the instance. (we're overriding a cascade)
        experimentalDesign.setAuditTrail( persistAuditTrail( experimentalDesign.getAuditTrail() ) );
        experimentalDesign = experimentalDesignDao.create( experimentalDesign );

        // Put back.
        experimentalDesign.setExperimentalFactors( factors );

        assert experimentalDesign != null;
        assert !isTransient( experimentalDesign );
        assert experimentalDesign.getExperimentalFactors() != null;

        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

            experimentalFactor.setExperimentalDesign( experimentalDesign );

            // Override cascade like above.
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            experimentalFactor.setFactorValues( null );
            experimentalFactor = persistExperimentalFactor( experimentalFactor );

            for ( FactorValue factorValue : factorValues ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                fillInFactorValueAssociations( factorValue );
            }

            // FactorValue is cascaded.
            experimentalFactor.setFactorValues( factorValues );
            experimentalFactorDao.update( experimentalFactor );
        }
    }

}