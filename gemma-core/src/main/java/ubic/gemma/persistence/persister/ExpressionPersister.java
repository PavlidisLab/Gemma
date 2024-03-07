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

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.ExpressionExperimentPrePersistService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.service.expression.biomaterial.CompoundDao;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author pavlidis
 */
public abstract class ExpressionPersister extends ArrayDesignPersister implements PersisterHelper {

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

    @Override
    @Transactional
    public ExpressionExperiment persist( ExpressionExperiment ee, @Nullable ArrayDesignsForExperimentCache cachedArrays ) {
        try {
            getSessionFactory().getCurrentSession().setFlushMode( FlushMode.MANUAL );
            ExpressionExperiment persistedEntity = persistExpressionExperiment( ee, Caches.empty( cachedArrays ) );
            getSessionFactory().getCurrentSession().flush();
            return persistedEntity;
        } finally {
            getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
        }
    }

    protected ExpressionExperiment persistExpressionExperiment( ExpressionExperiment ee, Caches caches ) {
        ExpressionExperiment existingEE = expressionExperimentDao.findByShortName( ee.getShortName() );
        if ( existingEE != null ) {
            AbstractPersister.log.warn( "Expression experiment with same short name exists (" + existingEE
                    + "), returning it (this method does not handle updates)" );
            return existingEE;
        }

        AbstractPersister.log.debug( ">>>>>>>>>> Persisting " + ee );

        if ( ee.getPrimaryPublication() != null ) {
            ee.setPrimaryPublication( ( BibliographicReference ) this.doPersist( ee.getPrimaryPublication(), caches ) );
        }
        if ( ee.getOwner() != null ) {
            ee.setOwner( ( Contact ) this.doPersist( ee.getOwner(), caches ) );
        }
        ee.setTaxon( this.persistTaxon( ee.getTaxon(), caches ) );

        this.doPersist( ee.getQuantitationTypes(), caches );
        this.doPersist( ee.getOtherRelevantPublications(), caches );

        if ( ee.getAccession() != null ) {
            this.fillInDatabaseEntry( ee.getAccession(), caches );
        }

        // This has to come first and be persisted, so our FactorValues get persisted before we process the
        // BioAssays.
        if ( ee.getExperimentalDesign() != null ) {
            ExperimentalDesign experimentalDesign = ee.getExperimentalDesign();
            this.processExperimentalDesign( experimentalDesign, caches );
            assert experimentalDesign.getId() != null;
            ee.setExperimentalDesign( experimentalDesign );
        }

        this.checkExperimentalDesign( ee );

        // This does most of the preparatory work.
        this.processBioAssays( ee, caches );

        ee = expressionExperimentDao.create( ee );

        AbstractPersister.log.debug( "<<<<<< FINISHED Persisting " + ee );
        return ee;
    }

    @Secured("GROUP_USER")
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return expressionExperimentPrePersistService.prepare( ee );
    }

    @Override
    protected Object doPersist( Object entity, Caches caches ) {
        if ( entity instanceof ExpressionExperiment ) {
            if ( caches.getArrayDesignCache() == null ) {
                AbstractPersister.log.warn( "Consider doing the 'prepare' step in a separate transaction." );
                caches = caches.withArrayDesignCache( this.prepare( ( ExpressionExperiment ) entity ) );
            }
            return this.persistExpressionExperiment( ( ExpressionExperiment ) entity, caches );
        } else if ( entity instanceof BioAssayDimension ) {
            return this.persistBioAssayDimension( ( BioAssayDimension ) entity, caches );
        } else if ( entity instanceof BioMaterial ) {
            return this.persistBioMaterial( ( BioMaterial ) entity, caches );
        } else if ( entity instanceof BioAssay ) {
            return this.persistBioAssay( ( BioAssay ) entity, caches );
        } else if ( entity instanceof Compound ) {
            return this.persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return this.persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        } else {
            return super.doPersist( entity, caches );
        }
    }

    /**
     * If there are factorValues, check if they are setup right and if they are used by biomaterials.
     */
    private void checkExperimentalDesign( ExpressionExperiment expExp ) {
        if ( expExp.getExperimentalDesign() == null ) {
            AbstractPersister.log.warn( "No experimental design!" );
            return;
        }

        Collection<ExperimentalFactor> efs = expExp.getExperimentalDesign().getExperimentalFactors();

        if ( efs.size() == 0 )
            return;

        AbstractPersister.log.debug( "Checking experimental design for valid setup" );

        Collection<BioAssay> bioAssays = expExp.getBioAssays();

        /*
         * note this is very inefficient but it doesn't matter.
         */
        for ( ExperimentalFactor ef : efs ) {
            AbstractPersister.log
                    .info( "Checking: " + ef + ", " + ef.getFactorValues().size() + " factor values to check..." );

            for ( FactorValue fv : ef.getFactorValues() ) {

                if ( fv.getExperimentalFactor() == null || !fv.getExperimentalFactor().equals( ef ) ) {
                    throw new IllegalStateException(
                            "Factor value " + fv + " should have had experimental factor " + ef + ", it had " + fv
                                    .getExperimentalFactor() );
                }

                boolean found = false;
                // Make sure there is at least one bioassay using it.
                for ( BioAssay ba : bioAssays ) {
                    BioMaterial bm = ba.getSampleUsed();
                    for ( FactorValue fvb : bm.getFactorValues() ) {

                        // They should be persistent already at this point.
                        if ( ( fvb.getId() != null || fv.getId() != null ) && fvb.equals( fv ) && fvb == fv ) {
                            // Note we use == because they should be the same objects.
                            found = true;
                            break;
                        }
                    }
                }

                if ( !found ) {
                    /*
                     * Basically this means there is factor value but no biomaterial is associated with it. This can
                     * happen...especially with test objects, so we just warn.
                     */
                    // FIXME: throw new IllegalStateException( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                    AbstractPersister.log.warn( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                }
            }

        }
    }

    private void fillInBioAssayAssociations( BioAssay bioAssay, Caches caches ) {

        ArrayDesignsForExperimentCache c = caches.getArrayDesignCache();
        ArrayDesign arrayDesign = bioAssay.getArrayDesignUsed();
        ArrayDesign arrayDesignUsed;
        if ( arrayDesign.getId() != null ) {
            arrayDesignUsed = arrayDesign;
        } else if ( c == null || !c.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            throw new UnsupportedOperationException( "You must provide the persistent platforms in a cache object" );
        } else {
            arrayDesignUsed = c.getArrayDesignCache().get( arrayDesign.getShortName() );

            if ( arrayDesignUsed == null || arrayDesignUsed.getId() == null ) {
                throw new IllegalStateException( "You must provide the platform in the cache object" );
            }

            arrayDesignUsed = ( ArrayDesign ) this.getSessionFactory().getCurrentSession()
                    .load( ArrayDesign.class, arrayDesignUsed.getId() );

            if ( arrayDesignUsed == null ) {
                throw new IllegalStateException( "No platform matching " + arrayDesign.getShortName() );
            }

            AbstractPersister.log.debug( "Setting platform used for bioassay to " + arrayDesignUsed.getId() );
        }

        bioAssay.setArrayDesignUsed( arrayDesignUsed );

        BioMaterial material = bioAssay.getSampleUsed();
        Set<FactorValue> savedFactorValues = new HashSet<>();
        for ( FactorValue factorValue : material.getFactorValues() ) {
            // Factors are not compositioned in any more, but by association with the ExperimentalFactor.
            this.fillInFactorValueAssociations( factorValue, caches );
            savedFactorValues.add( this.persistFactorValue( factorValue, caches ) );
        }
        material.setFactorValues( savedFactorValues );

        if ( !savedFactorValues.isEmpty() )
            AbstractPersister.log.debug( "factor values done" );

        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( bioAssay.getAccession() != null ) {
            bioAssay.getAccession().setExternalDatabase(
                    this.persistExternalDatabase( bioAssay.getAccession().getExternalDatabase(), caches ) );
            AbstractPersister.log.debug( "external database done" );
        }

        // BioMaterials
        bioAssay.setSampleUsed( ( BioMaterial ) this.doPersist( bioAssay.getSampleUsed(), caches ) );

        AbstractPersister.log.debug( "Done with " + bioAssay );

    }

    private BioAssayDimension fillInDesignElementDataVectorAssociations( DesignElementDataVector dataVector, Caches caches ) {
        // we should have done this already.
        assert dataVector.getDesignElement() != null;

        BioAssayDimension bioAssayDimension = this.getBioAssayDimensionFromCacheOrCreate( dataVector, caches );

        dataVector.setBioAssayDimension( bioAssayDimension );

        assert dataVector.getQuantitationType() != null;
        QuantitationType qt = this.persistQuantitationType( dataVector.getQuantitationType(), caches );
        qt = ( QuantitationType ) this.getSessionFactory().getCurrentSession().merge( qt );
        dataVector.setQuantitationType( qt );

        return bioAssayDimension;
    }

    private void fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor, Caches caches ) {
        this.doPersist( experimentalFactor.getAnnotations(), caches );
    }

    private Set<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment ee, Caches caches ) {
        AbstractPersister.log.debug( "Filling in DesignElementDataVectors..." );

        Set<BioAssay> bioAssays = new HashSet<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int count = 0;
        for ( RawExpressionDataVector dataVector : ee.getRawExpressionDataVectors() ) {
            BioAssayDimension bioAssayDimension = this.fillInDesignElementDataVectorAssociations( dataVector, caches );

            if ( timer.getTime() > 5000 ) {
                if ( count == 0 ) {
                    AbstractPersister.log.debug( "Setup: " + timer.getTime() );
                } else {
                    AbstractPersister.log
                            .info( "Filled in " + ( count ) + " DesignElementDataVectors (" + timer.getTime()
                                    + "ms since last check)" );
                }
                timer.reset();
                timer.start();
            }

            bioAssays.addAll( bioAssayDimension.getBioAssays() );

            ++count;
        }

        AbstractPersister.log.debug( "Filled in total of " + count + " DesignElementDataVectors, " + bioAssays.size()
                + " bioassays" );
        return bioAssays;
    }

    private void fillInFactorValueAssociations( FactorValue factorValue, Caches caches ) {

        this.fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor(), caches );

        factorValue.setExperimentalFactor( this.persistExperimentalFactor( factorValue.getExperimentalFactor(), caches ) );

        // validate categorical v.s. continuous factor values
        FactorType factorType = factorValue.getExperimentalFactor().getType();
        if ( factorType.equals( FactorType.CONTINUOUS ) && factorValue.getMeasurement() == null ) {
            throw new IllegalStateException( "Continuous factor value must have a measurement." );
        } else if ( factorType.equals( FactorType.CATEGORICAL ) && factorValue.getCharacteristics().isEmpty() ) {
            throw new IllegalStateException( "Categorical factor value must have at least one characteristic." );
        }

        // sanity check
        if ( factorValue.getCharacteristics().size() > 0 && factorValue.getMeasurement() != null ) {
            throw new IllegalStateException( "FactorValue can only have one of ontology entry or measurement." );
        }

        // measurement will cascade, but not unit.
        if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
            factorValue.getMeasurement().setUnit( this.persistUnit( factorValue.getMeasurement().getUnit() ) );
        }
    }

    private BioAssayDimension getBioAssayDimensionFromCacheOrCreate( DesignElementDataVector vector, Caches caches ) {
        Map<String, BioAssayDimension> bioAssayDimensionCache = caches.getBioAssayDimensionCache();

        String dimensionName = vector.getBioAssayDimension().getName();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vector.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            BioAssayDimension bAd = this.persistBioAssayDimension( vector.getBioAssayDimension(), caches );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vector.setBioAssayDimension( bAd );
        }

        return bioAssayDimensionCache.get( dimensionName );
    }

    private BioAssay persistBioAssay( BioAssay assay, Caches caches ) {
        AbstractPersister.log.debug( "Persisting " + assay );
        this.fillInBioAssayAssociations( assay, caches );
        return bioAssayDao.create( assay );
    }

    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension, Caches caches ) {
        AbstractPersister.log.debug( "Persisting bioAssayDimension" );
        List<BioAssay> persistedBioAssays = new ArrayList<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            assert bioAssay != null;
            // bioAssay.setId( null ); // in case of retry.
            persistedBioAssays.add( this.persistBioAssay( bioAssay, caches ) );
            if ( persistedBioAssays.size() % 10 == 0 ) {
                AbstractPersister.log.debug( "Persisted: " + persistedBioAssays.size() + " bioassays" );
            }
        }
        AbstractPersister.log.debug( "Done persisting " + persistedBioAssays.size() + " bioassays" );
        assert persistedBioAssays.size() > 0;
        bioAssayDimension.setBioAssays( persistedBioAssays );
        // bioAssayDimension.setId( null ); // in case of retry.
        return bioAssayDimensionDao.findOrCreate( bioAssayDimension );
    }

    private BioMaterial persistBioMaterial( BioMaterial entity, Caches caches ) {
        AbstractPersister.log.debug( "Persisting " + entity );
        assert entity.getSourceTaxon() != null;

        AbstractPersister.log.debug( "Persisting " + entity );
        if ( entity.getExternalAccession() != null ) {
            this.fillInDatabaseEntry( entity.getExternalAccession(), caches );
        }

        AbstractPersister.log.debug( "db entry done" );
        entity.setSourceTaxon( this.persistTaxon( entity.getSourceTaxon(), caches ) );

        AbstractPersister.log.debug( "taxon done" );

        AbstractPersister.log.debug( "start save" );
        BioMaterial bm = bioMaterialDao.findOrCreate( entity );
        AbstractPersister.log.debug( "save biomaterial done" );

        return bm;
    }

    private Compound persistCompound( Compound compound ) {
        return compoundDao.findOrCreate( compound );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     */
    private ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor, Caches caches ) {
        assert experimentalFactor.getType() != null;
        this.fillInExperimentalFactorAssociations( experimentalFactor, caches );
        return experimentalFactorDao.create( experimentalFactor );
    }

    private ExpressionExperimentSubSet persistExpressionExperimentSubSet( ExpressionExperimentSubSet entity ) {
        if ( entity.getBioAssays().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot make a subset with no bioassays" );
        } else if ( entity.getSourceExperiment().getId() == null ) {
            throw new IllegalArgumentException(
                    "Subsets are only supported for expression experiments that are already persistent" );
        } else {
            return expressionExperimentSubSetDao.findOrCreate( entity );
        }
    }

    /**
     * If we get here first (e.g., via bioAssay->bioMaterial) we have to override the cascade.
     */
    private FactorValue persistFactorValue( FactorValue factorValue, Caches caches ) {
        if ( factorValue.getExperimentalFactor().getId() == null ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        this.fillInFactorValueAssociations( factorValue, caches );
        return factorValueDao.findOrCreate( factorValue );
    }

    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment, Caches caches ) {
        if ( expressionExperiment.getRawExpressionDataVectors().isEmpty() ) {
            AbstractPersister.log.debug( "Filling in bioassays" );
            for ( BioAssay bioAssay : expressionExperiment.getBioAssays() ) {
                this.fillInBioAssayAssociations( bioAssay, caches );
            }
        } else {
            AbstractPersister.log.debug( "Filling in bioassays via data vectors" ); // usual case.
            Set<BioAssay> alreadyFilled;
            alreadyFilled = this.fillInExpressionExperimentDataVectorAssociations( expressionExperiment, caches );
            expressionExperiment.setBioAssays( alreadyFilled );
            expressionExperiment.setNumberOfSamples( alreadyFilled.size() );
        }
    }

    private void processExperimentalDesign( ExperimentalDesign experimentalDesign, Caches caches ) {

        this.doPersist( experimentalDesign.getTypes(), caches );

        // Withhold to avoid premature cascade.
        Set<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors();
        if ( factors == null ) {
            factors = new HashSet<>();
        }
        experimentalDesign.setExperimentalFactors( null );

        // Note we use create because this is specific to the instance. (we're overriding a cascade)
        experimentalDesign = experimentalDesignDao.create( experimentalDesign );

        // Put back.
        experimentalDesign.setExperimentalFactors( factors );

        // assert !this.isTransient( experimentalDesign );
        assert experimentalDesign.getExperimentalFactors() != null;

        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

            // experimentalFactor.setId( null ); // in case of retry.
            experimentalFactor.setExperimentalDesign( experimentalDesign );

            // Override cascade like above.
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            experimentalFactor.setFactorValues( null );
            experimentalFactor = this.persistExperimentalFactor( experimentalFactor, caches );

            if ( factorValues == null ) {
                AbstractPersister.log.warn( "Factor values collection was null for " + experimentalFactor );
                continue;
            }

            Set<FactorValue> createdFactorValues = new HashSet<>( factorValues.size() );
            for ( FactorValue factorValue : factorValues ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                this.fillInFactorValueAssociations( factorValue, caches );

                // this cascades from updates to the factor, but because auto-flush is off, we have to do this here to
                // get ACLs populated.
                createdFactorValues.add( factorValueDao.create( factorValue ) );
            }

            experimentalFactor.setFactorValues( createdFactorValues );

            experimentalFactorDao.update( experimentalFactor );

        }
    }

}