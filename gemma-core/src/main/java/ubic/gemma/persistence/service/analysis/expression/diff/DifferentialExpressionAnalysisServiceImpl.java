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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTask;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author paul
 * @author keshav
 * @see DifferentialExpressionAnalysisService
 */
@Service
public class DifferentialExpressionAnalysisServiceImpl extends AbstractService<DifferentialExpressionAnalysis> implements DifferentialExpressionAnalysisService {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisTask.class.getName() );

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
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.differentialExpressionAnalysisDao.countDownregulated( par, threshold );
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {
        return this.differentialExpressionAnalysisDao.countProbesMeetingThreshold( ears, threshold );

    }

    @Override
    @Transactional(readOnly = true)
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.differentialExpressionAnalysisDao.countUpregulated( par, threshold );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        return this.differentialExpressionAnalysisDao.find( gene, resultSet, threshold );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.differentialExpressionAnalysisDao.findByFactor( ef );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentIds(
            Collection<Long> experimentIds ) {
        return this.differentialExpressionAnalysisDao.findByExperimentIds( experimentIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.differentialExpressionAnalysisDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.differentialExpressionAnalysisDao.findExperimentsWithAnalyses( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments ) {
        return this.differentialExpressionAnalysisDao.getAnalyses( expressionExperiments );
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
    public List<DifferentialExpressionAnalysisValueObject> getAnalysesByExperiment( BioAssaySet experiment, boolean includeAnalysesOfSubsets ) {
        return differentialExpressionAnalysisDao
                .getAnalysesByExperimentIds( Collections.singleton( experiment.getId() ), includeAnalysesOfSubsets )
                .values()
                .stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiments(
            Collection<? extends BioAssaySet> experiments, boolean includeAnalysesOfSubsets ) {
        return getAnalysesByExperimentIds( EntityUtils.getIds( experiments ), includeAnalysesOfSubsets );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds( Collection<Long> experimentIds, boolean includeAnalysesOfSubsets ) {
        Map<Long, List<DifferentialExpressionAnalysisValueObject>> analysesByExperimentIds = this.differentialExpressionAnalysisDao
                .getAnalysesByExperimentIds( experimentIds, includeAnalysesOfSubsets );

        Map<Long, ExpressionExperimentDetailsValueObject> idMap = EntityUtils.getIdMap( expressionExperimentDao
                .loadDetailsValueObjectsByIds( analysesByExperimentIds.keySet() ) );

        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> result = new HashMap<>();

        for ( Long id : analysesByExperimentIds.keySet() ) {
            if ( !idMap.containsKey( id ) )
                continue; // defensive....
            result.put( idMap.get( id ), analysesByExperimentIds.get( id ) );
        }
        return result;
    }

    @Override
    @Transactional
    public void remove( DifferentialExpressionAnalysis toDelete ) {
        // Remove meta analyses that use the analyzed experiment
        log.info( "Removing meta analyses with this experiment..." );
        Collection<GeneDifferentialExpressionMetaAnalysis> metas = this.geneDiffExMetaAnalysisDao
                .findByExperiment( toDelete.getExperimentAnalyzed() );
        geneDiffExMetaAnalysisDao.remove( metas );

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
    public void removeAllInBatch() {
        throw new UnsupportedOperationException( "Removing all analyses in batch is not supported." );
    }

    @Override
    @Transactional
    public void removeForExperiment( BioAssaySet ee ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByExperiment( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            this.remove( de );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByExperiment( BioAssaySet experiment ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( experiment );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> findByExperiments(
            Collection<BioAssaySet> experiments ) {
        return this.differentialExpressionAnalysisDao
                .findByExperiments( experiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.differentialExpressionAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( idsToFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( taxon );
    }
}