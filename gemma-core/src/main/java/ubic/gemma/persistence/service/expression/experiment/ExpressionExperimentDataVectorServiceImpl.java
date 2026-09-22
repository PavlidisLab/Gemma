/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ExpressionExperimentDataVectorService}.
 * <p>
 * Read methods are {@code @Transactional(readOnly = true)}; write methods are
 * {@code @Transactional}. ACL enforcement is the responsibility of the facade
 * {@link ExpressionExperimentService} interface — this class is unsecured at the AOP boundary
 * on purpose, so intra-{@code gemma-core} callers can bypass duplicate ACL checks once
 * authenticated.
 *
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentDataVectorService")
public class ExpressionExperimentDataVectorServiceImpl implements ExpressionExperimentDataVectorService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentDataVectorServiceImpl.class );

    private final ExpressionExperimentDao expressionExperimentDao;
    private final BioAssayDimensionService bioAssayDimensionService;
    private final QuantitationTypeService quantitationTypeService;

    @Autowired
    public ExpressionExperimentDataVectorServiceImpl( ExpressionExperimentDao expressionExperimentDao,
            BioAssayDimensionService bioAssayDimensionService,
            QuantitationTypeService quantitationTypeService ) {
        this.expressionExperimentDao = expressionExperimentDao;
        this.bioAssayDimensionService = bioAssayDimensionService;
        this.quantitationTypeService = quantitationTypeService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, samples, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getPreferredRawDataVectors( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getPreferredRawDataVectors( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Collection<RawExpressionDataVector>> getMissingValuesVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.getMissingValuesVectors( ee );
    }

    @Override
    @Transactional
    public int addRawDataVectors( ExpressionExperiment ee,
            QuantitationType quantitationType,
            Collection<RawExpressionDataVector> newVectors ) {
        createDimensionIfNecessary( newVectors );
        createQuantitationTypeIfNecessary( newVectors, RawExpressionDataVector.class );
        return expressionExperimentDao.addRawDataVectors( ee, quantitationType, newVectors );
    }

    @Override
    @Transactional
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        return expressionExperimentDao.replaceRawDataVectors( ee, qt, vectors );
    }

    @Override
    @Transactional
    public int replaceAllRawDataVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {
        if ( newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        // Queried rather than read off ee.getRawExpressionDataVectors(). The experiment arrives DETACHED from
        // every caller that computes before it writes — affyFromCel reads it, spends minutes in
        // apt-probeset-summarize with no session open, then lands here via
        // DataUpdaterImpl.reprocessAffyDataFromCel — so navigating that lazy collection threw
        // LazyInitializationException ("no Session") on every such run: the collection belongs to the session
        // that closed, and the one this method opens cannot initialize it. FRB reproduced it on GSE37623 and on
        // GSE14263, the second after 632 s of successful CEL processing (2026-09-22). Below this line the DAO
        // reattaches the experiment itself (ensureEeInSession). The query is also what keeps every raw vector's
        // data blob out of memory when all we want is the set of quantitation types.
        Set<QuantitationType> existingQts = new HashSet<>(
                quantitationTypeService.findByExpressionExperiment( ee, RawExpressionDataVector.class ) );

        Set<QuantitationType> newQts = newVectors.stream()
                .map( RawExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> preferredQts = newQts.stream()
                .filter( QuantitationType::getIsPreferred )
                .collect( Collectors.toSet() );
        if ( preferredQts.size() > 1 ) {
            throw new IllegalArgumentException( "There must be exactly one preferred quantitation type." );
        }

        // group the vectors up by QT
        Map<QuantitationType, Set<RawExpressionDataVector>> vectorsByQt = newVectors.stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getQuantitationType, Collectors.toSet() ) );

        int replaced = 0;
        for ( Map.Entry<QuantitationType, Set<RawExpressionDataVector>> e : vectorsByQt.entrySet() ) {
            if ( existingQts.contains( e.getKey() ) ) {
                replaced += replaceRawDataVectors( ee, e.getKey(), e.getValue() );
            } else {
                replaced += addRawDataVectors( ee, e.getKey(), e.getValue() );
            }
        }

        for ( QuantitationType qt : existingQts ) {
            if ( !newQts.contains( qt ) ) {
                removeRawDataVectors( ee, qt );
            }
        }

        // Issues #902 / #1129: when prior replace cycles failed to cascade their QT delete
        // (or when AffyFromCel re-runs over an EE that already had a stub QT pre-attached),
        // ee.quantitationTypes holds orphan rows that no vector references. The previous
        // implementation derived `existingQts` from vectors only and left those orphans behind,
        // producing "stray preferred" QTs and downstream FK violations on subsequent imports.
        expressionExperimentDao.removeOrphanQuantitationTypes( ee );

        return replaced;
    }

    @Override
    @Transactional
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeAllRawDataVectors( ee );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return removeRawDataVectors( ee, qt, false );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        return expressionExperimentDao.removeRawDataVectors( ee, qt, keepDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee, assays ) );
    }

    @Override
    @Transactional
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.createProcessedDataVectors( ee, vectors );
    }

    @Override
    @Transactional
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeProcessedDataVectors( ee );
    }

    @Override
    @Transactional
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        // unlike raw vectors, the "new" processed vectors might use a different QT
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.replaceProcessedDataVectors( ee, vectors );
    }

    private void createDimensionIfNecessary( Collection<? extends BulkExpressionDataVector> vectors ) {
        Collection<BioAssayDimension> dimension = vectors.stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
        if ( dimension.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassay dimension" );
        }
        BioAssayDimension bad = dimension.iterator().next();
        if ( bad.getId() == null ) {
            log.info( "Creating " + bad + "..." );
            bad = this.bioAssayDimensionService.findOrCreate( bad );
            for ( BulkExpressionDataVector vector : vectors ) {
                vector.setBioAssayDimension( bad );
            }
        }
    }

    private <T extends DataVector> void createQuantitationTypeIfNecessary( Collection<T> vectors, Class<? extends DataVector> vectorType ) {
        Set<QuantitationType> quantitationType = vectors.stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );
        if ( quantitationType.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common quantitation type." );
        }
        QuantitationType qt = quantitationType.iterator().next();
        if ( qt.getId() == null ) {
            log.info( "Creating " + qt + "..." );
            qt = quantitationTypeService.create( qt, vectorType );
            for ( DataVector vector : vectors ) {
                vector.setQuantitationType( qt );
            }
        }
    }
}
