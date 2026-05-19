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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ExpressionExperimentSubSetReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link ExpressionExperimentService} interface — this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentSubSetReadService")
public class ExpressionExperimentSubSetReadServiceImpl implements ExpressionExperimentSubSetReadService {

    private final ExpressionExperimentDao expressionExperimentDao;
    private final ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    public ExpressionExperimentSubSetReadServiceImpl( ExpressionExperimentDao expressionExperimentDao,
            ExpressionExperimentSubSetDao expressionExperimentSubSetDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
        this.expressionExperimentSubSetDao = expressionExperimentSubSetDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> getSubSetsWithBioAssays( Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSubSetsByExpressionExperiments( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithCharacteristics( ExpressionExperiment ee ) {
        Collection<ExpressionExperimentSubSet> result = this.expressionExperimentDao.getSubSets( ee );
        for ( ExpressionExperimentSubSet subSet : result ) {
            Hibernate.initialize( subSet.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimensionWithBioAssays( ExpressionExperiment expressionExperiment ) {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> result = expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
        for ( Set<ExpressionExperimentSubSet> subSets : result.values() ) {
            for ( ExpressionExperimentSubSet s : subSets ) {
                for ( BioAssay ba : s.getBioAssays() ) {
                    Hibernate.initialize( ba.getSampleUsed() );
                    Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return expressionExperimentDao.getSubSets( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentDao.getSubSets( expressionExperiment, dimension );
        for ( ExpressionExperimentSubSet s : subSets ) {
            for ( BioAssay ba : s.getSourceExperiment().getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
            for ( BioAssay ba : s.getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
        }
        return subSets;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        // TODO: could this be made more efficient for a single factor?
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) )
                .get( experimentalFactor );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValueWithCharacteristicsAndBioAssays( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        Map<FactorValue, ExpressionExperimentSubSet> result;
        result = getSubSetsByFactorValue( expressionExperiment, experimentalFactor, dimension );
        if ( result != null ) {
            for ( ExpressionExperimentSubSet subSet : result.values() ) {
                Hibernate.initialize( subSet.getCharacteristics() );
                for ( BioAssay ba : subSet.getBioAssays() ) {
                    Thaws.thawBioAssay( ba );
                }
            }
        }
        return result;
    }

    private Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValueInternal( Collection<ExpressionExperimentSubSet> subSets ) {
        Map<ExperimentalFactor, Map<FactorValue, Set<ExpressionExperimentSubSet>>> result = new HashMap<>();
        for ( ExpressionExperimentSubSet subSet : subSets ) {
            for ( BioAssay ba : subSet.getBioAssays() ) {
                for ( FactorValue fv : ba.getSampleUsed().getAllFactorValues() ) {
                    result.computeIfAbsent( fv.getExperimentalFactor(), k -> new HashMap<>() )
                            .computeIfAbsent( fv, k -> new HashSet<>() )
                            .add( subSet );
                }
            }
        }
        return result.entrySet().stream()
                // only retain FVs that fully separates subsets
                // if there are as many FVs than subsets, we know there is exactly one subset per FV
                .filter( e -> e.getValue().size() == subSets.size() )
                .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, e2 -> e2.getValue().iterator().next() ) ) ) );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristics( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            Hibernate.initialize( result.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristicsAndBioAssays( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            result.getSourceExperiment().getBioAssays().forEach( Thaws::thawBioAssay );
            result.getBioAssays().forEach( Thaws::thawBioAssay );
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Migrated from ExpressionExperimentSubSetServiceImpl
    // -----------------------------------------------------------------------

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet loadSubSet( Long id ) {
        return expressionExperimentSubSetDao.load( id );
    }

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet loadSubSetWithBioAssays( Long id ) {
        return expressionExperimentSubSetDao.loadWithBioAssays( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays ) {
        return expressionExperimentSubSetDao.findByBioAssayIn( bioAssays );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        return expressionExperimentSubSetDao.getFactorValuesUsed( entity, factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValueValueObject> getFactorValuesUsedAsVO( Long subSetId, Long experimentalFactor ) {
        Collection<FactorValue> list = expressionExperimentSubSetDao.getFactorValuesUsed( subSetId, experimentalFactor );
        Collection<FactorValueValueObject> result = new HashSet<>();
        for ( FactorValue fv : list ) {
            result.add( new FactorValueValueObject( fv ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperimentSubSet subset ) {
        return expressionExperimentSubSetDao.getArrayDesignsUsed( subset );
    }
}
