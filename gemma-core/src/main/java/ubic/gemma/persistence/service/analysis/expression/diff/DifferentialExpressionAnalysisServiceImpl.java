/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.analysis.expression.diff;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Thaws;

import java.util.*;

import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * @author paul
 * @author keshav
 * @see DifferentialExpressionAnalysisService
 */
@Service
@CommonsLog
public class DifferentialExpressionAnalysisServiceImpl extends AbstractService<DifferentialExpressionAnalysis> implements DifferentialExpressionAnalysisService {

    private final DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;
    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;
    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    @Autowired
    public DifferentialExpressionAnalysisServiceImpl( DifferentialExpressionAnalysisDao mainDao ) {
        super( mainDao );
        this.differentialExpressionAnalysisDao = mainDao;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis loadWithExperimentAnalyzed( Long id ) {
        DifferentialExpressionAnalysis analysis = load( id );
        if ( analysis != null ) {
            Hibernate.initialize( analysis.getExperimentAnalyzed() );
        }
        return analysis;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.differentialExpressionAnalysisDao.findByFactor( ef );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.differentialExpressionAnalysisDao.findExperimentsWithAnalyses( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment expressionExperiment, boolean includeSubSets, Long analysisId ) {
        return differentialExpressionAnalysisDao.findByExperimentAndAnalysisId( expressionExperiment, includeSubSets, analysisId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        HashSet<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            results.add( this.thaw( ea ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ensureInSession( differentialExpressionAnalysis );

        Hibernate.initialize( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() );
        for ( BioAssay bm : differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() ) {
            visitBioMaterials( bm.getSampleUsed(), b -> {
                for ( FactorValue fv : b.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() );
                }
            } );
        }

        Hibernate.initialize( differentialExpressionAnalysis.getProtocol() );

        if ( differentialExpressionAnalysis.getSubsetFactorValue() != null ) {
            Hibernate.initialize( differentialExpressionAnalysis.getSubsetFactorValue() );
        }

        Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
        Hibernate.initialize( ears );
        for ( ExpressionAnalysisResultSet ear : ears ) {
            Hibernate.initialize( ear );
            Hibernate.initialize( ear.getExperimentalFactors() );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw: " + timer.getTime() + "ms" );
        }

        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        differentialExpressionAnalysis = thaw( differentialExpressionAnalysis );
        // just loading the entities in the session is sufficient for thawing them
        for ( ExpressionAnalysisResultSet dears : differentialExpressionAnalysis.getResultSets() ) {
            expressionAnalysisResultSetDao.loadWithResultsAndContrasts( dears.getId() );
        }
        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.expressionAnalysisResultSetDao.canDelete( differentialExpressionAnalysis );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets, boolean includeAssays ) {
        Map<Long, Collection<Long>> arrayDesignsUsed = new HashMap<>();
        Map<Long, Collection<FactorValue>> experimentAnalyzed2FactorValuesUsed = new HashMap<>();
        Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> hits = this.differentialExpressionAnalysisDao
                .findByExperimentIds( experimentIds, includeSubSets, arrayDesignsUsed, experimentAnalyzed2FactorValuesUsed );

        if ( hits.isEmpty() ) {
            return Collections.emptyMap();
        }

        // initialize result sets and hit list sizes
        // this is necessary because the DEA VO constructor will ignore uninitialized associations
        for ( Collection<DifferentialExpressionAnalysis> deas : hits.values() ) {
            for ( DifferentialExpressionAnalysis dea : deas ) {
                Hibernate.initialize( dea.getResultSets() );
                for ( ExpressionAnalysisResultSet rs : dea.getResultSets() ) {
                    Hibernate.initialize( rs.getHitListSizes() );
                }
                if ( includeAssays ) {
                    dea.getExperimentAnalyzed().getBioAssays().forEach( Thaws::thawBioAssay );
                }
            }
        }

        Map<Long, ExpressionExperimentDetailsValueObject> idMap = IdentifiableUtils.getIdMap( expressionExperimentDao
                .loadDetailsValueObjectsByIds( IdentifiableUtils.getIds( hits.keySet() ) ) );

        Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> result = new HashMap<>();

        for ( Map.Entry<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> e : hits.entrySet() ) {
            ExpressionExperiment sourceExperiment = e.getKey();
            ExpressionExperimentDetailsValueObject eeVo = idMap.get( sourceExperiment.getId() );

            if ( eeVo == null ) {
                log.warn( "Could not find details VO for experiment with ID " + e.getKey() + ", ignoring." );
                continue;
            }

            Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<>();
            for ( DifferentialExpressionAnalysis analysis : e.getValue() ) {
                Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

                DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

                BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();

                avo.setExperimentAnalyzedId( experimentAnalyzed.getId() ); // might be a subset.

                if ( analysis.getSubsetFactorValue() != null ) {
                    // subset analysis
                    assert experimentAnalyzed instanceof ExpressionExperimentSubSet;
                    avo.setSourceExperimentId( ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().getId() );
                    avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );
                    avo.setSubsetFactor(
                            new ExperimentalFactorValueObject( analysis.getSubsetFactorValue().getExperimentalFactor() ) );
                }

                if ( arrayDesignsUsed.containsKey( experimentAnalyzed.getId() ) ) {
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( experimentAnalyzed.getId() ) );
                } else {
                    log.warn( "No array designs found for experiment analyzed with ID " + experimentAnalyzed.getId() + ", ignoring." );
                }

                if ( experimentAnalyzed2FactorValuesUsed.containsKey( experimentAnalyzed.getId() ) ) {
                    Collection<FactorValue> fvs = experimentAnalyzed2FactorValuesUsed.get( experimentAnalyzed.getId() );
                    ExperimentalFactorValueObject subsetFactor = avo.getSubsetFactor();
                    for ( FactorValue fv : fvs ) {
                        Long experimentalFactorId = fv.getExperimentalFactor().getId();
                        if ( subsetFactor != null && experimentalFactorId.equals( subsetFactor.getId() ) ) {
                            continue;
                        }
                        avo.getFactorValuesUsedByExperimentalFactorId()
                                .computeIfAbsent( experimentalFactorId, k -> new HashSet<>() )
                                .add( new FactorValueValueObject( fv ) );
                    }
                } else {
                    log.warn( "No factor values found for experiment analyzed with ID " + experimentAnalyzed.getId() + ", ignoring." );
                }

                for ( ExpressionAnalysisResultSet resultSet : results ) {
                    DiffExResultSetSummaryValueObject desvo = new DiffExResultSetSummaryValueObject( resultSet );
                    desvo.setArrayDesignsUsed( avo.getArrayDesignsUsed() );
                    desvo.setBioAssaySetAnalyzedId( experimentAnalyzed.getId() ); // might be a subset.
                    desvo.setAnalysisId( analysis.getId() );
                    avo.getResultSets().add( desvo );
                }

                summaries.add( avo );
            }
            result.put( eeVo, summaries );
        }
        return result;
    }

    @Override
    @Transactional
    public void remove( DifferentialExpressionAnalysis toDelete ) {
        toDelete = ensureInSession( toDelete );

        log.info( "Removing " + toDelete + "..." );

        // Remove meta analyses that use the analyzed experiment
        Collection<GeneDifferentialExpressionMetaAnalysis> metas = this.geneDiffExMetaAnalysisDao
                .findByExperiment( toDelete.getExperimentAnalyzed() );
        if ( !metas.isEmpty() ) {
            log.info( "Removing " + metas.size() + " meta analyses with this experiment..." );
            geneDiffExMetaAnalysisDao.remove( metas );
        }

        // Remove the DEA
        super.remove( toDelete );
    }

    @Override
    public void remove( Collection<DifferentialExpressionAnalysis> entities ) {
        entities.forEach( this::remove );
    }

    @Override
    public void remove( Long id ) {
        throw new UnsupportedOperationException( "Removing an analysis by ID is not supported, use remove() with an entity instead." );
    }

    @Override
    @Transactional
    public void removeForExperiment( ExpressionExperiment ee, boolean includeSubSets ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByExperiment( ee, includeSubSets );
        this.remove( diffAnalyses );
    }

    @Override
    @Transactional
    public void removeForExperimentAnalyzed( BioAssaySet experimentAnalyzed ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByExperimentAnalyzed( experimentAnalyzed );
        this.remove( diffAnalyses );
    }

    @Override
    @Transactional
    public int removeForExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        Collection<DifferentialExpressionAnalysis> found = differentialExpressionAnalysisDao.findByFactor( experimentalFactor );
        this.remove( found );
        return found.size();
    }

    @Override
    @Transactional
    public int removeForExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        Collection<DifferentialExpressionAnalysis> found = differentialExpressionAnalysisDao.findByFactors( experimentalFactors );
        this.remove( found );
        return found.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experiment, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( experiment, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments(
            Collection<ExpressionExperiment> experiments, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao
                .findByExperiments( experiments, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.differentialExpressionAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentIds, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( experimentIds, includeSubSets );
    }
}