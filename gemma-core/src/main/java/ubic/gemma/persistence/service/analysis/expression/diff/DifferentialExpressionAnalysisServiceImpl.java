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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;
import java.util.Map;

/**
 * @author paul
 * @author keshav
 * @see DifferentialExpressionAnalysisService
 */
@Service
@Slf4j
public class DifferentialExpressionAnalysisServiceImpl extends AbstractService<DifferentialExpressionAnalysis> implements DifferentialExpressionAnalysisService {

    private final DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    private final DifferentialExpressionAnalysisReadService differentialExpressionAnalysisReadService;

    @Autowired
    public DifferentialExpressionAnalysisServiceImpl( DifferentialExpressionAnalysisDao mainDao,
            DifferentialExpressionAnalysisReadService differentialExpressionAnalysisReadService ) {
        super( mainDao );
        this.differentialExpressionAnalysisDao = mainDao;
        this.differentialExpressionAnalysisReadService = differentialExpressionAnalysisReadService;
    }

    @Override
    public DifferentialExpressionAnalysis loadWithExperimentAnalyzed( Long id ) {
        return differentialExpressionAnalysisReadService.loadWithExperimentAnalyzed( id );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return differentialExpressionAnalysisReadService.findByFactor( ef );
    }

    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return differentialExpressionAnalysisReadService.findExperimentsWithAnalyses( gene );
    }

    @Override
    public DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment expressionExperiment, boolean includeSubSets, Long analysisId ) {
        return differentialExpressionAnalysisReadService.findByExperimentAndAnalysisId( expressionExperiment, includeSubSets, analysisId );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        return differentialExpressionAnalysisReadService.thaw( expressionAnalyses );
    }

    @Override
    public DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return differentialExpressionAnalysisReadService.thaw( differentialExpressionAnalysis );
    }

    @Override
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return differentialExpressionAnalysisReadService.thawFully( differentialExpressionAnalysis );
    }

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return differentialExpressionAnalysisReadService.canDelete( differentialExpressionAnalysis );
    }

    @Override
    public Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets, boolean includeAssays ) {
        return differentialExpressionAnalysisReadService.findByExperimentIds( experimentIds, includeSubSets, includeAssays );
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
    public Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experiment, boolean includeSubSets ) {
        return differentialExpressionAnalysisReadService.findByExperiment( experiment, includeSubSets );
    }

    @Override
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments(
            Collection<ExpressionExperiment> experiments, boolean includeSubSets ) {
        return differentialExpressionAnalysisReadService.findByExperiments( experiments, includeSubSets );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return differentialExpressionAnalysisReadService.findByName( name );
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentIds, boolean includeSubSets ) {
        return differentialExpressionAnalysisReadService.getExperimentsWithAnalysis( experimentIds, includeSubSets );
    }
}
