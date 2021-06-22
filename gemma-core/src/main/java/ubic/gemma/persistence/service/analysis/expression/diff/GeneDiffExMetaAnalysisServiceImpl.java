/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.BaseValueObject;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Paul
 */
@Service
public class GeneDiffExMetaAnalysisServiceImpl implements GeneDiffExMetaAnalysisService {

    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    @Override
    @Transactional
    public GeneDifferentialExpressionMetaAnalysis create( GeneDifferentialExpressionMetaAnalysis analysis ) {
        return geneDiffExMetaAnalysisDao.create( analysis );
    }

    @Override
    @Transactional
    public BaseValueObject delete( Long id ) {
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.load( id );

        BaseValueObject baseValueObject = new BaseValueObject();

        if ( metaAnalysis == null ) {
            baseValueObject.setErrorFound( true );
            baseValueObject.setObjectAlreadyRemoved( true );
        } else {
            this.remove( metaAnalysis );
        }

        return baseValueObject;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        return geneDiffExMetaAnalysisDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<IncludedResultSetInfoValueObject> findIncludedResultSetsInfoById( long analysisId ) {
        return this.geneDiffExMetaAnalysisDao.findIncludedResultSetsInfoById( analysisId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds ) {
        return this.geneDiffExMetaAnalysisDao.findMetaAnalyses( metaAnalysisIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId ) {
        return this.geneDiffExMetaAnalysisDao.findResultsById( analysisId );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneDifferentialExpressionMetaAnalysisResult loadResult( Long idResult ) {
        return this.geneDiffExMetaAnalysisDao.loadResult( idResult );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult ) {
        return this.geneDiffExMetaAnalysisDao.loadWithResultId( idResult );
    }

    @Override
    @Transactional
    public void update( GeneDifferentialExpressionMetaAnalysis analysis ) {
        geneDiffExMetaAnalysisDao.update( analysis );
    }

    @Override
    @Transactional
    public void remove( GeneDifferentialExpressionMetaAnalysis toDelete ) {
        geneDiffExMetaAnalysisDao.remove( toDelete );
    }

    @Override
    @Transactional
    public void removeForExperiment( ExpressionExperiment ee ){
        this.geneDiffExMetaAnalysisDao.removeForExperiment(ee);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation ) {
        return geneDiffExMetaAnalysisDao.findByInvestigation( investigation );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return geneDiffExMetaAnalysisDao.findByInvestigations( investigations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return geneDiffExMetaAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneDifferentialExpressionMetaAnalysis findByUniqueInvestigations(
            Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<GeneDifferentialExpressionMetaAnalysis> found = this
                .findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() )
            return null;
        return found.iterator().next();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return geneDiffExMetaAnalysisDao.getExperimentsWithAnalysis( idsToFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        Collection<Long> haveAnalysis = new HashSet<>();
        Collection<GeneDifferentialExpressionMetaAnalysis> analyses = this.findByTaxon( taxon );
        for ( GeneDifferentialExpressionMetaAnalysis a : analyses ) {
            for ( ExpressionAnalysisResultSet r : a.getResultSetsIncluded() ) {
                haveAnalysis.add( r.getAnalysis().getExperimentAnalyzed().getId() );
            }
        }
        return haveAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneDifferentialExpressionMetaAnalysis load( Long id ) {
        return geneDiffExMetaAnalysisDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneDifferentialExpressionMetaAnalysis> loadAll() {
        return geneDiffExMetaAnalysisDao.loadAll();
    }

}
