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
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.ExpressionExperimentPrePersistService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.service.expression.biomaterial.CompoundDao;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author pavlidis
 */
abstract public class ExpressionPersister extends ArrayDesignPersister {

    private final Map<String, BioAssayDimension> bioAssayDimensionCache = new ConcurrentHashMap<>();
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
    public ExpressionExperiment persist( ExpressionExperiment ee, ArrayDesignsForExperimentCache cachedArrays ) {

        if ( ee == null )
            return null;
        if ( !this.isTransient( ee ) )
            return ee;

        this.clearCache();

        ExpressionExperiment existingEE = expressionExperimentDao.findByShortName( ee.getShortName() );
        if ( existingEE != null ) {
            AbstractPersister.log.warn( "Expression experiment with same short name exists (" + existingEE
                    + "), returning it (this method does not handle updates)" );
            return existingEE;
        }

        try {

            AbstractPersister.log.info( ">>>>>>>>>> Persisting " + ee );

            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.COMMIT );

            ee.setPrimaryPublication( ( BibliographicReference ) this.persist( ee.getPrimaryPublication() ) );
            ee.setOwner( ( Contact ) this.persist( ee.getOwner() ) );

            this.persistCollectionElements( ee.getQuantitationTypes() );
            this.persistCollectionElements( ee.getOtherRelevantPublications() );
            this.persistCollectionElements( ee.getInvestigators() );

            if ( ee.getAccession() != null ) {
                this.fillInDatabaseEntry( ee.getAccession() );
            }

            // This has to come first and be persisted, so our FactorValues get persisted before we process the
            // BioAssays.
            if ( ee.getExperimentalDesign() != null ) {
                ExperimentalDesign experimentalDesign = ee.getExperimentalDesign();
                experimentalDesign.setId( null ); // in case of retry.
                this.processExperimentalDesign( experimentalDesign );
                assert experimentalDesign.getId() != null;
                ee.setExperimentalDesign( experimentalDesign );
            }

            this.checkExperimentalDesign( ee );

            // This does most of the preparatory work.
            this.processBioAssays( ee, cachedArrays );

            ee = expressionExperimentDao.create( ee );

        } finally {
            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
        }

        this.clearCache();
        AbstractPersister.log.info( "<<<<<< FINISHED Persisting " + ee );
        return ee;
    }

    @Override
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return expressionExperimentPrePersistService.prepare( ee );
    }

    @Override
    public Object persist( Object entity ) {
        if ( entity == null )
            return null;

        if ( entity instanceof ExpressionExperiment ) {
            AbstractPersister.log.warn( "Consider doing the 'setup' step in a separate transaction" );
            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
            ArrayDesignsForExperimentCache c = expressionExperimentPrePersistService
                    .prepare( ( ExpressionExperiment ) entity );
            return this.persist( ( ExpressionExperiment ) entity, c );
        } else if ( entity instanceof BioAssayDimension ) {
            return this.persistBioAssayDimension( ( BioAssayDimension ) entity, null );
        } else if ( entity instanceof BioMaterial ) {
            return this.persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof BioAssay ) {
            return this.persistBioAssay( ( BioAssay ) entity, null );
        } else if ( entity instanceof Compound ) {
            return this.persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return this.persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        }
        return super.persist( entity );
    }

    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null )
            return null;
        return super.persistOrUpdate( entity );
    }

    /**
     * If there are factorValues, check if they are setup right and if they are used by biomaterials.
     */
    private void checkExperimentalDesign( ExpressionExperiment expExp ) {

        if ( expExp == null ) {
            return;
        }

        if ( expExp.getExperimentalDesign() == null ) {
            AbstractPersister.log.warn( "No experimental design!" );
            return;
        }

        Collection<ExperimentalFactor> efs = expExp.getExperimentalDesign().getExperimentalFactors();

        if ( efs.size() == 0 )
            return;

        AbstractPersister.log.info( "Checking experimental design for valid setup" );

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
                    AbstractPersister.log.warn( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                }
            }

        }
    }

    private void clearCache() {
        bioAssayDimensionCache.clear();
        this.clearCommonCache();
    }

    private void fillInBioAssayAssociations( BioAssay bioAssay, ArrayDesignsForExperimentCache c ) {

        ArrayDesign arrayDesign = bioAssay.getArrayDesignUsed();
        ArrayDesign arrayDesignUsed;
        if ( !this.isTransient( arrayDesign ) ) {
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

        assert !this.isTransient( arrayDesignUsed );

        bioAssay.setArrayDesignUsed( arrayDesignUsed );

        boolean hadFactors = false;
        BioMaterial material = bioAssay.getSampleUsed();
        for ( FactorValue factorValue : material.getFactorValues() ) {
            // Factors are not compositioned in any more, but by association with the ExperimentalFactor.
            this.fillInFactorValueAssociations( factorValue );
            this.persistFactorValue( factorValue );
            hadFactors = true;
        }

        if ( hadFactors )
            AbstractPersister.log.debug( "factor values done" );

        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( bioAssay.getAccession() != null ) {
            bioAssay.getAccession().setExternalDatabase(
                    this.persistExternalDatabase( bioAssay.getAccession().getExternalDatabase() ) );
            bioAssay.getAccession().setId( null ); // IN CASE we are retrying.
            AbstractPersister.log.debug( "external database done" );
        }

        // BioMaterials
        bioAssay.setSampleUsed( ( BioMaterial ) this.persist( bioAssay.getSampleUsed() ) );

        AbstractPersister.log.debug( "biomaterials done" );

        LocalFile rawDataFile = bioAssay.getRawDataFile();
        if ( rawDataFile != null ) {
            if ( this.isTransient( rawDataFile ) ) {
                rawDataFile.setId( null ); // in case of retry.
                // raw file is unique for bioassay.
                bioAssay.setRawDataFile( this.persistLocalFile( rawDataFile, true ) );
            } else {
                // re-sync.
                this.localFileDao.update( rawDataFile );
            }
            AbstractPersister.log.debug( "raw data file done" );
        }

        for ( LocalFile file : bioAssay.getDerivedDataFiles() ) {
            if ( this.isTransient( file ) )
                file.setId( null ); // in case of retry
            this.persistLocalFile( file );
        }

        if ( this.isTransient( bioAssay.getAuditTrail() ) && bioAssay.getAuditTrail() != null )
            bioAssay.getAuditTrail().setId( null ); // in case of retry;

        AbstractPersister.log.debug( "Done with " + bioAssay );

    }

    private BioAssayDimension fillInDesignElementDataVectorAssociations( DesignElementDataVector dataVector,
            ArrayDesignsForExperimentCache c ) {
        // we should have done this already.
        assert dataVector.getDesignElement() != null && !this.isTransient( dataVector.getDesignElement() );

        BioAssayDimension bioAssayDimension = this.getBioAssayDimensionFromCacheOrCreate( dataVector, c );

        assert !this.isTransient( bioAssayDimension );
        dataVector.setBioAssayDimension( bioAssayDimension );

        assert dataVector.getQuantitationType() != null;
        QuantitationType qt = this.persistQuantitationType( dataVector.getQuantitationType() );
        qt = ( QuantitationType ) this.getSessionFactory().getCurrentSession().merge( qt );
        dataVector.setQuantitationType( qt );

        return bioAssayDimension;
    }

    private void fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null )
            return;
        if ( !this.isTransient( experimentalFactor ) )
            return;

        Collection<Characteristic> annotations = experimentalFactor.getAnnotations();
        for ( Characteristic c : annotations ) {
            // in case of retry.
            c.setId( null );
            if ( c.getAuditTrail() != null && this.isTransient( c.getAuditTrail() ) ) {
                c.getAuditTrail().setId( null );
            }
        }

        this.persistCollectionElements( annotations );
    }

    private Collection<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment ee,
            ArrayDesignsForExperimentCache c ) {
        AbstractPersister.log.info( "Filling in DesignElementDataVectors..." );

        Collection<BioAssay> bioAssays = new HashSet<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int count = 0;
        for ( RawExpressionDataVector dataVector : ee.getRawExpressionDataVectors() ) {
            BioAssayDimension bioAssayDimension = this.fillInDesignElementDataVectorAssociations( dataVector, c );

            if ( timer.getTime() > 5000 ) {
                if ( count == 0 ) {
                    AbstractPersister.log.info( "Setup: " + timer.getTime() );
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

            if ( Thread.interrupted() ) {
                AbstractPersister.log.info( "Cancelled" );
                return null;
            }
        }

        AbstractPersister.log.info( "Filled in total of " + count + " DesignElementDataVectors, " + bioAssays.size()
                + " bioassays" );
        return bioAssays;
    }

    private void fillInFactorValueAssociations( FactorValue factorValue ) {

        this.fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor() );

        factorValue.setExperimentalFactor( this.persistExperimentalFactor( factorValue.getExperimentalFactor() ) );

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
            factorValue.getMeasurement().setUnit( this.persistUnit( factorValue.getMeasurement().getUnit() ) );
        }

    }

    private BioAssayDimension getBioAssayDimensionFromCacheOrCreate( DesignElementDataVector vector,
            ArrayDesignsForExperimentCache c ) {
        if ( !this.isTransient( vector.getBioAssayDimension() ) )
            return vector.getBioAssayDimension();

        String dimensionName = vector.getBioAssayDimension().getName();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vector.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            vector.getBioAssayDimension().setId( null );
            BioAssayDimension bAd = this.persistBioAssayDimension( vector.getBioAssayDimension(), c );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vector.setBioAssayDimension( bAd );
        }
        BioAssayDimension bioAssayDimension = bioAssayDimensionCache.get( dimensionName );
        assert !this.isTransient( bioAssayDimension );

        return bioAssayDimension;
    }

    private BioAssay persistBioAssay( BioAssay assay, ArrayDesignsForExperimentCache c ) {

        if ( assay == null )
            return null;
        if ( !this.isTransient( assay ) ) {
            return assay;
        }
        AbstractPersister.log.debug( "Persisting " + assay );
        this.fillInBioAssayAssociations( assay, c );

        return bioAssayDao.create( assay );
    }

    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension,
            ArrayDesignsForExperimentCache c ) {
        if ( bioAssayDimension == null )
            return null;
        if ( !this.isTransient( bioAssayDimension ) )
            return bioAssayDimension;
        AbstractPersister.log.debug( "Persisting bioAssayDimension" );
        List<BioAssay> persistedBioAssays = new ArrayList<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            bioAssay.setId( null ); // in case of retry.
            persistedBioAssays.add( this.persistBioAssay( bioAssay, c ) );
            if ( persistedBioAssays.size() % 10 == 0 ) {
                AbstractPersister.log.info( "Persisted: " + persistedBioAssays.size() + " bioassays" );
            }
        }
        AbstractPersister.log.debug( "Done persisting " + persistedBioAssays.size() + " bioassays" );
        assert persistedBioAssays.size() > 0;
        bioAssayDimension.setBioAssays( persistedBioAssays );
        bioAssayDimension.setId( null ); // in case of retry.
        return bioAssayDimensionDao.findOrCreate( bioAssayDimension );
    }

    private BioMaterial persistBioMaterial( BioMaterial entity ) {
        if ( entity == null )
            return null;
        AbstractPersister.log.debug( "Persisting " + entity );
        if ( !this.isTransient( entity ) )
            return entity;

        assert entity.getSourceTaxon() != null;

        AbstractPersister.log.debug( "Persisting " + entity );
        this.fillInDatabaseEntry( entity.getExternalAccession() );

        AbstractPersister.log.debug( "db entry done" );
        entity.setSourceTaxon( this.persistTaxon( entity.getSourceTaxon() ) );

        AbstractPersister.log.debug( "taxon done" );

        for ( Treatment treatment : entity.getTreatments() ) {

            Characteristic action = treatment.getAction();
            AbstractPersister.log.debug( treatment + " action: " + action );
            AbstractPersister.log.debug( "treatment done" );
        }
        AbstractPersister.log.debug( "start save" );
        BioMaterial bm = bioMaterialDao.findOrCreate( entity );
        AbstractPersister.log.debug( "save biomaterial done" );

        return bm;
    }

    private Compound persistCompound( Compound compound ) {
        if ( compound == null )
            return null;
        if ( compound.getIsSolvent() == null )
            throw new IllegalArgumentException( "Compound must have 'isSolvent' value set." );
        return compoundDao.findOrCreate( compound );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     */
    private ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        if ( !this.isTransient( experimentalFactor ) || experimentalFactor == null )
            return experimentalFactor;
        assert experimentalFactor.getType() != null;
        this.fillInExperimentalFactorAssociations( experimentalFactor );

        // in case of retry
        Characteristic category = experimentalFactor.getCategory();
        if ( this.isTransient( category ) ) {
            category.setId( null );
            if ( category.getAuditTrail() != null && this.isTransient( category.getAuditTrail() ) ) {
                category.getAuditTrail().setId( null );
            }
        }

        assert ( !this.isTransient( experimentalFactor.getExperimentalDesign() ) );
        return experimentalFactorDao.create( experimentalFactor );
    }

    private ExpressionExperimentSubSet persistExpressionExperimentSubSet( ExpressionExperimentSubSet entity ) {
        if ( !this.isTransient( entity ) )
            return entity;

        if ( entity.getBioAssays().size() == 0 ) {
            throw new IllegalArgumentException( "Cannot make a subset with no bioassays" );
        } else if ( this.isTransient( entity.getSourceExperiment() ) ) {
            throw new IllegalArgumentException(
                    "Subsets are only supported for expression experiments that are already persistent" );
        }

        return expressionExperimentSubSetDao.findOrCreate( entity );
    }

    /**
     * If we get here first (e.g., via bioAssay->bioMaterial) we have to override the cascade.
     */
    private void persistFactorValue( FactorValue factorValue ) {
        if ( factorValue == null )
            return;
        if ( !this.isTransient( factorValue ) )
            return;
        if ( this.isTransient( factorValue.getExperimentalFactor() ) ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        this.fillInFactorValueAssociations( factorValue );
        factorValueDao.findOrCreate( factorValue );
    }

    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment, ArrayDesignsForExperimentCache c ) {

        Collection<BioAssay> alreadyFilled = new HashSet<>();

        if ( expressionExperiment.getRawExpressionDataVectors().isEmpty() ) {
            AbstractPersister.log.info( "Filling in bioassays" );
            for ( BioAssay bioAssay : expressionExperiment.getBioAssays() ) {
                this.fillInBioAssayAssociations( bioAssay, c );
                alreadyFilled.add( bioAssay );
            }
        } else {
            AbstractPersister.log.info( "Filling in bioassays via data vectors" ); // usual case.
            alreadyFilled = this.fillInExpressionExperimentDataVectorAssociations( expressionExperiment, c );
            expressionExperiment.setBioAssays( alreadyFilled );
        }
    }

    private void processExperimentalDesign( ExperimentalDesign experimentalDesign ) {

        this.persistCollectionElements( experimentalDesign.getTypes() );

        // Withhold to avoid premature cascade.
        Collection<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors();
        if ( factors == null ) {
            factors = new HashSet<>();
        }
        experimentalDesign.setExperimentalFactors( null );

        if ( experimentalDesign.getAuditTrail() != null ) {
            experimentalDesign.getAuditTrail().setId( null ); // in case of retry
        }
        // prob not necessary?
        experimentalDesign.setAuditTrail( this.persistAuditTrail( experimentalDesign.getAuditTrail() ) );

        // Note we use create because this is specific to the instance. (we're overriding a cascade)
        experimentalDesign = experimentalDesignDao.create( experimentalDesign );

        // Put back.
        experimentalDesign.setExperimentalFactors( factors );

        assert !this.isTransient( experimentalDesign );
        assert experimentalDesign.getExperimentalFactors() != null;

        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

            experimentalFactor.setId( null ); // in case of retry.
            experimentalFactor.setExperimentalDesign( experimentalDesign );

            // Override cascade like above.
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            experimentalFactor.setFactorValues( null );
            experimentalFactor = this.persistExperimentalFactor( experimentalFactor );

            if ( factorValues == null ) {
                AbstractPersister.log.warn( "Factor values collection was null for " + experimentalFactor );
                continue;
            }

            for ( FactorValue factorValue : factorValues ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                this.fillInFactorValueAssociations( factorValue );

                // this cascades from updates to the factor, but because auto-flush is off, we have to do this here to
                // get ACLs populated.
                factorValueDao.create( factorValue );
            }

            experimentalFactor.setFactorValues( factorValues );

            experimentalFactorDao.update( experimentalFactor );

        }
    }

}