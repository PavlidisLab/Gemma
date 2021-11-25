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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTask;
import ubic.gemma.model.analysis.Investigation;
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
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author paul
 * @author keshav
 * @see DifferentialExpressionAnalysisService
 */
@Service
public class DifferentialExpressionAnalysisServiceImpl implements DifferentialExpressionAnalysisService {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisTask.class.getName() );

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;
    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;
    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;
    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

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
    @Transactional
    public DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis analysis ) {
        return this.differentialExpressionAnalysisDao.create( analysis );
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
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            Collection<Long> investigationIds ) {
        return this.differentialExpressionAnalysisDao.findByInvestigationIds( investigationIds );
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
        return this.differentialExpressionAnalysisDao.findByInvestigation( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments ) {
        return this.differentialExpressionAnalysisDao.getAnalyses( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.differentialExpressionAnalysisDao.thaw( expressionAnalyses );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.differentialExpressionAnalysisDao.thaw( differentialExpressionAnalysis );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.differentialExpressionAnalysisDao.thaw( differentialExpressionAnalysis );
        return this.expressionAnalysisResultSetDao.thawFully( differentialExpressionAnalysis );
    }

    @Override
    @Transactional
    public void update( DifferentialExpressionAnalysis o ) {
        this.differentialExpressionAnalysisDao.update( o );
    }

    @Override
    @Transactional
    public void update( ExpressionAnalysisResultSet a ) {
        this.expressionAnalysisResultSetDao.update( a );

    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.expressionAnalysisResultSetDao.canDelete( differentialExpressionAnalysis );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids ) {
        return this.getAnalysesByExperiment( ids, 0, -1 );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids, int offset, int limit ) {
        Map<Long, List<DifferentialExpressionAnalysisValueObject>> analysesByExperimentIds = this.differentialExpressionAnalysisDao
                .getAnalysesByExperimentIds( ids, offset, limit );

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
                .findByInvestigation( toDelete.getExperimentAnalyzed() );
        geneDiffExMetaAnalysisDao.remove( metas );

        // Remove result sets
        this.removeResultSets( this.load( toDelete.getId() ) );

        // Remove the DEA
        this.differentialExpressionAnalysisDao.remove( toDelete );
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByInvestigation( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            this.remove( de );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByInvestigation( Investigation investigation ) {
        return this.differentialExpressionAnalysisDao.findByInvestigation( investigation );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<Investigation, Collection<DifferentialExpressionAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.differentialExpressionAnalysisDao
                .findByInvestigations( ( Collection<Investigation> ) investigations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.differentialExpressionAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis findByUniqueInvestigations(
            Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<DifferentialExpressionAnalysis> found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() )
            return null;
        return found.iterator().next();
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

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis load( java.lang.Long id ) {
        return this.differentialExpressionAnalysisDao.load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> loadAll() {
        return this.differentialExpressionAnalysisDao.loadAll();
    }

    private void removeResultSets( DifferentialExpressionAnalysis toDelete ) {
        this.differentialExpressionAnalysisDao.thawResultSets( toDelete );
        Collection<ExpressionAnalysisResultSet> rss = toDelete.getResultSets();
        log.info( "Removing result sets..." );

        // Wipe references
        toDelete.setResultSets( new HashSet<ExpressionAnalysisResultSet>() );
        this.update( toDelete );

        StopWatch sw = new StopWatch();
        sw.start();
        int rsCnt = 0;

        // remove from database
        for ( ExpressionAnalysisResultSet rs : rss ) {
            this.expressionAnalysisResultSetDao.remove( rs );
            rsCnt++;
        }

        sw.stop();
        log.info( "Removed in " + rsCnt + " result sets." );
    }

}