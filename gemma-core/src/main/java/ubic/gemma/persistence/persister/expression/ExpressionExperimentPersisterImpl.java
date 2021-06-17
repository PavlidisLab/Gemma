package ubic.gemma.persistence.persister.expression;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.*;
import ubic.gemma.persistence.service.ExpressionExperimentPrePersistService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignDao;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExpressionExperimentPersisterImpl extends AuditablePersister<ExpressionExperiment> implements ExpressionExperimentPersister {

    private final Map<String, BioAssayDimension> bioAssayDimensionCache = new ConcurrentHashMap<>();

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExperimentalDesignDao experimentalDesignDao;

    @Autowired
    private FactorValueDao factorValueDao;

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private ExpressionExperimentPrePersistService expressionExperimentPrePersistService;

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    private FactorValuePersister factorValuePersister;

    @Autowired
    private ExperimentalFactorPersister experimentalFactorPersister;

    @Autowired
    private Persister<BibliographicReference> bibliographicReferencePersister;

    @Autowired
    private Persister<Contact> contactPersister;

    @Autowired
    private Persister<Taxon> taxonPersister;

    @Autowired
    private BioAssayDimensionPersister bioAssayDimensionPersister;

    @Autowired
    private Persister<ExperimentalDesign> experimentalDesignPersister;

    @Autowired
    private Persister<CompositeSequence> compositeSequencePersister;

    @Autowired
    private CachingPersister<QuantitationType> quantitationTypePersister;

    @Autowired
    private Persister<Characteristic> characteristicPersister;

    @Autowired
    private BioAssayPersister bioAssayPersister;

    @Autowired
    public ExpressionExperimentPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public ExpressionExperiment persistAuditable( ExpressionExperiment entity ) {
        AbstractPersister.log.warn( "Consider doing the 'setup' step in a separate transaction" );
        this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
        ArrayDesignsForExperimentCache c = expressionExperimentPrePersistService
                .prepare( entity );
        return this.persist( entity, c );
    }

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

            ee.setPrimaryPublication( this.bibliographicReferencePersister.persist( ee.getPrimaryPublication() ) );
            ee.setOwner( this.contactPersister.persist( ee.getOwner() ) );
            ee.setTaxon( this.taxonPersister.persist( ee.getTaxon() ) );

            this.quantitationTypePersister.persistCollectionElements( ee.getQuantitationTypes() );
            this.bibliographicReferencePersister.persistCollectionElements( ee.getOtherRelevantPublications() );

            if ( ee.getAccession() != null ) {
                this.databaseEntryPersister.persist( ee.getAccession() );
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
    public void clearCache() {
        bioAssayDimensionCache.clear();
        quantitationTypePersister.clearCache();
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


    private BioAssayDimension fillInDesignElementDataVectorAssociations( DesignElementDataVector dataVector,
            ArrayDesignsForExperimentCache c ) {
        // we should have done this already.
        assert dataVector.getDesignElement() != null && !this.compositeSequencePersister.isTransient( dataVector.getDesignElement() );

        BioAssayDimension bioAssayDimension = this.getBioAssayDimensionFromCacheOrCreate( dataVector, c );

        assert !this.bioAssayDimensionPersister.isTransient( bioAssayDimension );
        dataVector.setBioAssayDimension( bioAssayDimension );

        assert dataVector.getQuantitationType() != null;
        QuantitationType qt = this.quantitationTypePersister.persist( dataVector.getQuantitationType() );
        qt = ( QuantitationType ) this.getSessionFactory().getCurrentSession().merge( qt );
        dataVector.setQuantitationType( qt );

        return bioAssayDimension;
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


    private BioAssayDimension getBioAssayDimensionFromCacheOrCreate( DesignElementDataVector vector,
            ArrayDesignsForExperimentCache c ) {
        if ( !this.bioAssayDimensionPersister.isTransient( vector.getBioAssayDimension() ) )
            return vector.getBioAssayDimension();

        String dimensionName = vector.getBioAssayDimension().getName();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vector.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            vector.getBioAssayDimension().setId( null );
            BioAssayDimension bAd = this.bioAssayDimensionPersister.persistBioAssayDimension( vector.getBioAssayDimension(), c );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vector.setBioAssayDimension( bAd );
        }
        BioAssayDimension bioAssayDimension = bioAssayDimensionCache.get( dimensionName );
        assert !this.bioAssayDimensionPersister.isTransient( bioAssayDimension );

        return bioAssayDimension;
    }


    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment, ArrayDesignsForExperimentCache c ) {

        Collection<BioAssay> alreadyFilled = new HashSet<>();

        if ( expressionExperiment.getRawExpressionDataVectors().isEmpty() ) {
            AbstractPersister.log.info( "Filling in bioassays" );
            for ( BioAssay bioAssay : expressionExperiment.getBioAssays() ) {
                this.bioAssayPersister.fillInBioAssayAssociations( bioAssay, c );
                alreadyFilled.add( bioAssay );
            }
        } else {
            AbstractPersister.log.info( "Filling in bioassays via data vectors" ); // usual case.
            alreadyFilled = this.fillInExpressionExperimentDataVectorAssociations( expressionExperiment, c );
            expressionExperiment.setBioAssays( alreadyFilled );
        }
    }

    private void processExperimentalDesign( ExperimentalDesign experimentalDesign ) {

        this.characteristicPersister.persistCollectionElements( experimentalDesign.getTypes() );

        // Withhold to avoid premature cascade.
        Collection<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors();
        if ( factors == null ) {
            factors = new HashSet<>();
        }
        experimentalDesign.setExperimentalFactors( null );

        // Note we use create because this is specific to the instance. (we're overriding a cascade)
        experimentalDesign = experimentalDesignDao.create( experimentalDesign );

        // Put back.
        experimentalDesign.setExperimentalFactors( factors );

        assert !this.experimentalDesignPersister.isTransient( experimentalDesign );
        assert experimentalDesign.getExperimentalFactors() != null;

        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

            experimentalFactor.setId( null ); // in case of retry.
            experimentalFactor.setExperimentalDesign( experimentalDesign );

            // Override cascade like above.
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            experimentalFactor.setFactorValues( null );
            experimentalFactor = this.experimentalFactorPersister.persist( experimentalFactor );

            if ( factorValues == null ) {
                AbstractPersister.log.warn( "Factor values collection was null for " + experimentalFactor );
                continue;
            }

            for ( FactorValue factorValue : factorValues ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                this.factorValuePersister.fillInFactorValueAssociations( factorValue );

                // this cascades from updates to the factor, but because auto-flush is off, we have to do this here to
                // get ACLs populated.
                factorValueDao.create( factorValue );
            }

            experimentalFactor.setFactorValues( factorValues );

            experimentalFactorDao.update( experimentalFactor );

        }
    }
}
