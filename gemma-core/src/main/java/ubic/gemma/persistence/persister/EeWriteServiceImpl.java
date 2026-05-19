/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.service.expression.biomaterial.CompoundDao;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignDao;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetDao;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strangler-fig replacement for the EE-graph write path historically owned by
 * {@code ExpressionPersister}. Lives in the {@code persister} package (rather than
 * {@code service.expression.experiment}) so it can share the package-protected
 * {@link AbstractPersister.Caches} type and the protected helper methods on the
 * persister chain ({@code persistTaxon}, {@code persistExternalDatabase},
 * {@code fillInDatabaseEntry}). The per-call ExternalDatabase map is plumbed as
 * an explicit {@code Map<String, ExternalDatabase>} parameter alongside
 * {@code Caches} (Phase 3 lift; formerly carried as the {@code externalDatabaseCache}
 * field of the POJO).
 * <p>
 * Until the persister chain is fully retired (E5), this class collaborates with
 * {@link ExpressionPersister} via the injected {@link PersisterHelper}: the helper
 * provides {@code doPersist}-routed access to those inherited helpers through the
 * dispatch table. ExpressionPersister's body methods delegate here; this class
 * holds the canonical implementations.
 *
 * @see EeWriteService
 * @author pavlidis
 */
@Component
public class EeWriteServiceImpl implements EeWriteService {

    private static final Log log = LogFactory.getLog( EeWriteServiceImpl.class );

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;
    @Autowired
    private BioAssayDao bioAssayDao;
    @Autowired
    private BioMaterialDao bioMaterialDao;
    @Autowired
    private CompoundDao compoundDao;
    @Autowired
    private ContactDao contactDao;
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
    private UnitDao unitDao;
    @Autowired
    private QuantitationTypeDao quantitationTypeDao;
    /**
     * Used to call back into the persister chain for the inherited helpers
     * ({@code persistTaxon}, {@code persistExternalDatabase},
     * {@code fillInDatabaseEntry}) and for {@code doPersist} dispatch on
     * non-EE associations (Publications, Persons, ...).
     * When the persister chain is deleted in E5, these calls become direct
     * collaborator-DAO calls.
     * <p>
     * Field type is the {@link PersisterHelper} interface (not the impl) so
     * Spring can inject the {@code @Transactional} JDK proxy without a
     * {@code BeanNotOfRequiredTypeException}. The protected helpers used here
     * ({@code doPersist}, {@code persistTaxon}, {@code getSessionFactory},
     * {@code fillInDatabaseEntry}, {@code persistExternalDatabase}) live on
     * {@link AbstractPersister} and are not part of the public interface;
     * access them via {@link #persister()} which unwraps the proxy to the
     * underlying {@link PersisterHelperImpl}.
     */
    @Autowired
    private PersisterHelper persisterHelper;

    /**
     * Returns the underlying {@link PersisterHelperImpl}, unwrapping the
     * Spring AOP proxy if necessary. Needed to reach the protected helpers
     * inherited from {@link AbstractPersister} that are not on the
     * {@link PersisterHelper} interface. Goes away with the persister chain
     * in E5.
     */
    private PersisterHelperImpl persister() {
        Object target = AopProxyUtils.getSingletonTarget( persisterHelper );
        return ( PersisterHelperImpl ) ( target != null ? target : persisterHelper );
    }

    @Override
    @Transactional
    public ExpressionExperiment create( ExpressionExperiment ee, @Nullable ArrayDesignsForExperimentCache cache ) {
        try {
            persister().getSessionFactory().getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            ExpressionExperiment persistedEntity = persistExpressionExperiment( ee, AbstractPersister.Caches.empty(), new HashMap<>(), cache, new HashMap<>() );
            persister().getSessionFactory().getCurrentSession().flush();
            return persistedEntity;
        } finally {
            persister().getSessionFactory().getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    /**
     * Persist the EE graph. Called both from {@link #create} and (during the
     * strangler-fig window) from {@link ExpressionPersister#doPersist} when an
     * EE is reached via the polymorphic dispatch.
     */
    ExpressionExperiment persistExpressionExperiment( ExpressionExperiment ee, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Object, Taxon> taxonCache ) {
        ExpressionExperiment existingEE = expressionExperimentDao.findByShortName( ee.getShortName() );
        if ( existingEE != null ) {
            log.warn( "Expression experiment with same short name exists (" + existingEE
                    + "), returning it (this method does not handle updates)" );
            return existingEE;
        }

        log.debug( ">>>>>>>>>> Persisting " + ee );

        if ( ee.getPrimaryPublication() != null ) {
            // Phase 3 lift: was doPersist (instanceof BibliographicReference arm); now a
            // direct call to the per-call-Map persistBibliographicReference helper.
            ee.setPrimaryPublication( persister().persistBibliographicReference( ee.getPrimaryPublication(), xdbCache ) );
        }
        if ( ee.getOwner() != null ) {
            // BK lookup via ContactDao.find (which delegates to BusinessKey.find(Session, Contact));
            // covers Person too since Person extends Contact.
            Contact owner = ee.getOwner();
            Contact existingOwner = contactDao.find( owner );
            ee.setOwner( existingOwner != null ? existingOwner : contactDao.create( owner ) );
        }
        if ( ee.getTaxon() != null ) {
            ee.setTaxon( persister().persistTaxon( ee.getTaxon(), taxonCache ) );
        }

        // Phase 3 lift: was doPersist (instanceof QuantitationType arm in CommonPersister);
        // now a direct call to the per-call-Map findOrCreateQuantitationType helper. The
        // map is shared with fillInDesignElementDataVectorAssociations below so QTs
        // referenced both at the EE level and per-vector dedupe to the same row.
        Map<Integer, QuantitationType> qtCache = new HashMap<>();
        Set<QuantitationType> persistedQts = new HashSet<>();
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            persistedQts.add( findOrCreateQuantitationType( qt, qtCache ) );
        }
        ee.setQuantitationTypes( persistedQts );
        if ( ee.getOtherRelevantPublications() != null ) {
            // Phase 3 lift: was doPersist (instanceof BibliographicReference arm); now a
            // direct call to the per-call-Map persistBibliographicReference helper.
            Set<BibliographicReference> persistedOther = new HashSet<>();
            for ( BibliographicReference pub : ee.getOtherRelevantPublications() ) {
                persistedOther.add( persister().persistBibliographicReference( pub, xdbCache ) );
            }
            ee.setOtherRelevantPublications( persistedOther );
        }

        if ( ee.getAccession() != null ) {
            // Phase 3 lift: per-call Map; see fillInBioAssayAssociations note.
            persister().fillInDatabaseEntry( ee.getAccession(), xdbCache );
        }

        // This has to come first and be persisted, so our FactorValues get persisted before we process the
        // BioAssays.
        if ( ee.getExperimentalDesign() != null ) {
            ExperimentalDesign experimentalDesign = ee.getExperimentalDesign();
            processExperimentalDesign( experimentalDesign, caches, xdbCache );
            assert experimentalDesign.getId() != null;
            ee.setExperimentalDesign( experimentalDesign );
        }

        checkExperimentalDesign( ee );

        // This does most of the preparatory work.
        processBioAssays( ee, caches, xdbCache, adCache, qtCache, taxonCache );

        ee = expressionExperimentDao.create( ee );

        log.debug( "<<<<<< FINISHED Persisting " + ee );
        return ee;
    }

    /**
     * If there are factorValues, check if they are setup right and if they are used by biomaterials.
     */
    void checkExperimentalDesign( ExpressionExperiment expExp ) {
        if ( expExp.getExperimentalDesign() == null ) {
            log.warn( "No experimental design!" );
            return;
        }

        Collection<ExperimentalFactor> efs = expExp.getExperimentalDesign().getExperimentalFactors();

        if ( efs.isEmpty() )
            return;

        log.debug( "Checking experimental design for valid setup" );

        Collection<BioAssay> bioAssays = expExp.getBioAssays();

        /*
         * note this is very inefficient but it doesn't matter.
         */
        for ( ExperimentalFactor ef : efs ) {
            log.info( "Checking: " + ef + ", " + ef.getFactorValues().size() + " factor values to check..." );

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
                    log.warn( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                }
            }

        }
    }

    void fillInBioAssayAssociations( BioAssay bioAssay, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Object, Taxon> taxonCache ) {

        ArrayDesign arrayDesign = bioAssay.getArrayDesignUsed();
        ArrayDesign arrayDesignUsed;
        if ( arrayDesign.getId() != null ) {
            arrayDesignUsed = arrayDesign;
        } else if ( adCache == null || !adCache.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            throw new UnsupportedOperationException( "You must provide the persistent platforms in a cache object" );
        } else {
            arrayDesignUsed = adCache.getArrayDesignCache().get( arrayDesign.getShortName() );

            if ( arrayDesignUsed == null || arrayDesignUsed.getId() == null ) {
                throw new IllegalStateException( "You must provide the platform in the cache object" );
            }

            arrayDesignUsed = ( ArrayDesign ) persister().getSessionFactory().getCurrentSession()
                    .load( ArrayDesign.class, arrayDesignUsed.getId() );

            if ( arrayDesignUsed == null ) {
                throw new IllegalStateException( "No platform matching " + arrayDesign.getShortName() );
            }

            log.debug( "Setting platform used for bioassay to " + arrayDesignUsed.getId() );
        }

        bioAssay.setArrayDesignUsed( arrayDesignUsed );

        BioMaterial material = bioAssay.getSampleUsed();
        Set<FactorValue> savedFactorValues = new HashSet<>();
        for ( FactorValue factorValue : material.getFactorValues() ) {
            // Factors are not compositioned in any more, but by association with the ExperimentalFactor.
            fillInFactorValueAssociations( factorValue, caches, xdbCache );
            savedFactorValues.add( persistFactorValue( factorValue, caches, xdbCache ) );
        }
        material.setFactorValues( savedFactorValues );

        if ( !savedFactorValues.isEmpty() )
            log.debug( "factor values done" );

        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( bioAssay.getAccession() != null ) {
            // Phase 3 lift: helper takes the per-call Map<String, ExternalDatabase>
            // threaded through this persist (formerly carried on Caches).
            bioAssay.getAccession().setExternalDatabase(
                    persister().persistExternalDatabase( bioAssay.getAccession().getExternalDatabase(), xdbCache ) );
            log.debug( "external database done" );
        }

        // BioMaterials
        // Phase 3 lift: was persister().doPersist (instanceof BioMaterial arm in
        // ExpressionPersister); now a direct call to persistBioMaterial so the threaded
        // taxonCache stays alive across all BioAssays in this EE graph.
        bioAssay.setSampleUsed( persistBioMaterial( bioAssay.getSampleUsed(), caches, xdbCache, taxonCache ) );

        log.debug( "Done with " + bioAssay );

    }

    BioAssay persistBioAssay( BioAssay assay, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Object, Taxon> taxonCache ) {
        log.debug( "Persisting " + assay );
        fillInBioAssayAssociations( assay, caches, xdbCache, adCache, taxonCache );
        return bioAssayDao.create( assay );
    }

    BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Object, Taxon> taxonCache ) {
        log.debug( "Persisting bioAssayDimension" );
        List<BioAssay> persistedBioAssays = new ArrayList<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            assert bioAssay != null;
            // bioAssay.setId( null ); // in case of retry.
            persistedBioAssays.add( persistBioAssay( bioAssay, caches, xdbCache, adCache, taxonCache ) );
            if ( persistedBioAssays.size() % 10 == 0 ) {
                log.debug( "Persisted: " + persistedBioAssays.size() + " bioassays" );
            }
        }
        log.debug( "Done persisting " + persistedBioAssays.size() + " bioassays" );
        assert !persistedBioAssays.isEmpty();
        bioAssayDimension.setBioAssays( persistedBioAssays );
        // bioAssayDimension.setId( null ); // in case of retry.
        return bioAssayDimensionDao.findOrCreate( bioAssayDimension );
    }

    BioAssayDimension fillInDesignElementDataVectorAssociations( BulkExpressionDataVector dataVector, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Integer, QuantitationType> qtCache, Map<Object, Taxon> taxonCache ) {
        // we should have done this already.
        assert dataVector.getDesignElement() != null;

        BioAssayDimension bioAssayDimension = getBioAssayDimensionFromCacheOrCreate( dataVector, caches, xdbCache, adCache, taxonCache );

        dataVector.setBioAssayDimension( bioAssayDimension );

        assert dataVector.getQuantitationType() != null;
        QuantitationType qt = findOrCreateQuantitationType( dataVector.getQuantitationType(), qtCache );
        qt = ( QuantitationType ) persister().getSessionFactory().getCurrentSession().merge( qt );
        dataVector.setQuantitationType( qt );

        return bioAssayDimension;
    }

    Set<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment ee, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Integer, QuantitationType> qtCache, Map<Object, Taxon> taxonCache ) {
        log.debug( "Filling in DesignElementDataVectors..." );

        Set<BioAssay> bioAssays = new HashSet<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int count = 0;
        for ( RawExpressionDataVector dataVector : ee.getRawExpressionDataVectors() ) {
            BioAssayDimension bioAssayDimension = fillInDesignElementDataVectorAssociations( dataVector, caches, xdbCache, adCache, qtCache, taxonCache );

            if ( timer.getTime() > 5000 ) {
                if ( count == 0 ) {
                    log.debug( "Setup: " + timer.getTime() );
                } else {
                    log.info( "Filled in " + ( count ) + " DesignElementDataVectors (" + timer.getTime()
                            + "ms since last check)" );
                }
                timer.reset();
                timer.start();
            }

            bioAssays.addAll( bioAssayDimension.getBioAssays() );

            ++count;
        }

        log.debug( "Filled in total of " + count + " DesignElementDataVectors, " + bioAssays.size()
                + " bioassays" );
        return bioAssays;
    }

    private BioAssayDimension getBioAssayDimensionFromCacheOrCreate( BulkExpressionDataVector vector, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Object, Taxon> taxonCache ) {
        Map<Integer, BioAssayDimension> bioAssayDimensionCache = caches.getBioAssayDimensionCache();

        Integer dimensionName = vector.getBioAssayDimension().hashCode();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vector.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            BioAssayDimension bAd = persistBioAssayDimension( vector.getBioAssayDimension(), caches, xdbCache, adCache, taxonCache );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vector.setBioAssayDimension( bAd );
        }

        return bioAssayDimensionCache.get( dimensionName );
    }

    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     */
    void processBioAssays( ExpressionExperiment expressionExperiment, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, @Nullable ArrayDesignsForExperimentCache adCache, Map<Integer, QuantitationType> qtCache, Map<Object, Taxon> taxonCache ) {
        if ( expressionExperiment.getRawExpressionDataVectors().isEmpty() ) {
            log.debug( "Filling in bioassays" );
            for ( BioAssay bioAssay : expressionExperiment.getBioAssays() ) {
                fillInBioAssayAssociations( bioAssay, caches, xdbCache, adCache, taxonCache );
            }
        } else {
            log.debug( "Filling in bioassays via data vectors" ); // usual case.
            Set<BioAssay> alreadyFilled;
            alreadyFilled = fillInExpressionExperimentDataVectorAssociations( expressionExperiment, caches, xdbCache, adCache, qtCache, taxonCache );
            expressionExperiment.setBioAssays( alreadyFilled );
            expressionExperiment.setNumberOfSamples( alreadyFilled.size() );
        }
    }

    /**
     * Persist the ExperimentalDesign and its EF/FV graph, then return it persistent.
     * <p>
     * Historical note (pre-Phase-3): this method used to explicitly withhold each
     * collection and call {@code experimentalFactorDao.create} / {@code factorValueDao.create}
     * so the {@code @AfterReturning} ACL advice would fire on each DAO call.
     * Post-{@code 21e4fc41} the {@code EntityInsert} listener attaches ACLs from
     * Hibernate insert events directly, so cascade inserts produce identical ACL
     * results. The explicit per-FV create loop and the cascade-override withhold-
     * and-put-back gymnastics are gone; we let the {@code ExperimentalDesign}
     * cascade (ED -&gt; EF -&gt; FV) do the work and the listener attach the ACLs.
     */
    void processExperimentalDesign( ExperimentalDesign experimentalDesign, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache ) {

        persister().doPersist( experimentalDesign.getTypes(), caches, xdbCache );

        if ( experimentalDesign.getExperimentalFactors() == null ) {
            experimentalDesign.setExperimentalFactors( new HashSet<>() );
        }

        // Fill in associations that the cascade doesn't reach (EF annotations,
        // FV measurement units, back-references). Cascade handles ED->EF->FV
        // inserts and the EntityInsert listener attaches their ACLs.
        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {
            experimentalFactor.setExperimentalDesign( experimentalDesign );
            fillInExperimentalFactorAssociations( experimentalFactor, caches, xdbCache );

            if ( experimentalFactor.getFactorValues() == null ) {
                log.warn( "Factor values collection was null for " + experimentalFactor );
                continue;
            }

            for ( FactorValue factorValue : experimentalFactor.getFactorValues() ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                // measurement will cascade, but not unit.
                if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
                    factorValue.getMeasurement().setUnit( findOrCreateUnit( factorValue.getMeasurement().getUnit() ) );
                }
            }
        }

        // Cascade=all on ExperimentalDesign.experimentalFactors and ExperimentalFactor.factorValues
        // makes this one create() walk the whole subgraph; the EntityInsert listener attaches ACLs.
        experimentalDesignDao.create( experimentalDesign );
    }

    BioMaterial persistBioMaterial( BioMaterial entity, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache, Map<Object, Taxon> taxonCache ) {
        log.debug( "Persisting " + entity );
        assert entity.getSourceTaxon() != null;

        log.debug( "Persisting " + entity );
        if ( entity.getExternalAccession() != null ) {
            // Phase 3 lift: per-call Map; see fillInBioAssayAssociations note.
            persister().fillInDatabaseEntry( entity.getExternalAccession(), xdbCache );
        }

        log.debug( "db entry done" );
        entity.setSourceTaxon( persister().persistTaxon( entity.getSourceTaxon(), taxonCache ) );

        log.debug( "taxon done" );

        log.debug( "start save" );
        BioMaterial bm = bioMaterialDao.findOrCreate( entity );
        log.debug( "save biomaterial done" );

        return bm;
    }

    Compound persistCompound( Compound compound ) {
        return compoundDao.findOrCreate( compound );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     */
    ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        assert experimentalFactor.getType() != null;
        fillInExperimentalFactorAssociations( experimentalFactor, caches, xdbCache );
        return experimentalFactorDao.create( experimentalFactor );
    }

    ExpressionExperimentSubSet persistExpressionExperimentSubSet( ExpressionExperimentSubSet entity ) {
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
     * If we get here first (e.g., via bioAssay-&gt;bioMaterial) we have to override the cascade.
     */
    FactorValue persistFactorValue( FactorValue factorValue, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        if ( factorValue.getId() != null ) {
            // already persistent
            return factorValue;
        }
        if ( factorValue.getExperimentalFactor().getId() == null ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        fillInFactorValueAssociations( factorValue, caches, xdbCache );
        return factorValueDao.findOrCreate( factorValue );
    }

    void fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        persister().doPersist( experimentalFactor.getAnnotations(), caches, xdbCache );
    }

    void fillInFactorValueAssociations( FactorValue factorValue, AbstractPersister.Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor(), caches, xdbCache );
        if ( factorValue.getExperimentalFactor().getId() == null ) {
            factorValue.setExperimentalFactor( persistExperimentalFactor( factorValue.getExperimentalFactor(), caches, xdbCache ) );
        }
        // measurement will cascade, but not unit.
        if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
            factorValue.getMeasurement().setUnit( findOrCreateUnit( factorValue.getMeasurement().getUnit() ) );
        }
    }

    /**
     * Find-or-create for {@link Unit}. UnitDao.find delegates to
     * {@code BusinessKey.find(Session, Unit)} (name-only BK). Inlined from
     * the former {@code CommonPersister.persistUnit} during the persister sweep.
     */
    private Unit findOrCreateUnit( Unit unit ) {
        Unit existing = unitDao.find( unit );
        return existing != null ? existing : unitDao.create( unit );
    }

    /**
     * Per-EE-persist memoised create-or-reuse for {@link QuantitationType}.
     * <p>
     * QTs are deliberately per-experiment: a QT named "Signal" in EE A is a distinct
     * row from a QT named "Signal" in EE B. Within one EE-graph persist, however, many
     * data vectors typically share the same QT instance (the same {@code (name,
     * description)}); without dedup that would create N duplicate rows for N vectors,
     * because {@code AbstractPersister.persist} runs under {@link FlushMode#MANUAL} so
     * a DAO {@code find} cannot see an in-flight {@code create} until end-of-transaction.
     * <p>
     * The cache key matches {@code BusinessKey.matches(QT, QT)} semantics — the hash of
     * {@code (name, description)}. Callers must pass the same map for all QT references
     * within one EE persist; see {@link #persistExpressionExperiment}.
     * <p>
     * Inlined from the former {@code CommonPersister.persistQuantitationType} during
     * the persister sweep.
     */
    // package-private so EeWriteServiceImplQtDedupTest can exercise the dedup
    // semantic without standing up a full EE-graph fixture. Was the load-bearing
    // behaviour of the former CommonPersister.persistQuantitationType.
    QuantitationType findOrCreateQuantitationType( QuantitationType qType, Map<Integer, QuantitationType> qtCache ) {
        if ( qType.getName() == null )
            throw new IllegalArgumentException( "QuantitationType must have a name" );
        int key = qType.getName().hashCode();
        if ( qType.getDescription() != null )
            key += qType.getDescription().hashCode();
        if ( qtCache.containsKey( key ) ) {
            return qtCache.get( key );
        }
        QuantitationType qt = quantitationTypeDao.create( qType );
        qtCache.put( key, qt );
        return qt;
    }
}
