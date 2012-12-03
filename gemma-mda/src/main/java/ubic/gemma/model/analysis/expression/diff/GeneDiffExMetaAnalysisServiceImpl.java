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

package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.BaseValueObject;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.genome.Taxon;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Service
public class GeneDiffExMetaAnalysisServiceImpl implements GeneDiffExMetaAnalysisService {

    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    @Override
    public GeneDifferentialExpressionMetaAnalysis create( GeneDifferentialExpressionMetaAnalysis analysis ) {
        return geneDiffExMetaAnalysisDao.create( analysis );
    }

    @Override
    public void delete( GeneDifferentialExpressionMetaAnalysis toDelete ) {
        geneDiffExMetaAnalysisDao.remove( toDelete );

    }

    @Override
    public BaseValueObject delete( Long id ) {
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = load( id );

        BaseValueObject baseValueObject = new BaseValueObject();

        if ( metaAnalysis == null ) {
            baseValueObject.setErrorFound( true );
            baseValueObject.setObjectAlreadyRemoved( true );
        } else {
            delete( metaAnalysis );
        }

        return baseValueObject;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation ) {
        return geneDiffExMetaAnalysisDao.findByInvestigation( investigation );
    }

    @Override
    public Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return geneDiffExMetaAnalysisDao.findByInvestigations( investigations );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return geneDiffExMetaAnalysisDao.findByName( name );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByParentTaxon( Taxon taxon ) {
        return geneDiffExMetaAnalysisDao.findByParentTaxon( taxon );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        return geneDiffExMetaAnalysisDao.findByTaxon( taxon );
    }

    @Override
    public GeneDifferentialExpressionMetaAnalysis findByUniqueInvestigations(
            Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<GeneDifferentialExpressionMetaAnalysis> found = this.findByInvestigation( investigations.iterator()
                .next() );
        if ( found.isEmpty() ) return null;
        return found.iterator().next();
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> findIncludedResultSetsInfoById(
            long analysisId ) {
        return this.geneDiffExMetaAnalysisDao.findIncludedResultSetsInfoById( analysisId );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds ) {
        return this.geneDiffExMetaAnalysisDao.findMetaAnalyses( metaAnalysisIds );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId ) {
        return this.geneDiffExMetaAnalysisDao.findResultsById( analysisId );
    }

    @Override
    public GeneDifferentialExpressionMetaAnalysis load( Long id ) {
        return geneDiffExMetaAnalysisDao.load( id );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> loadAll() {
        return ( Collection<GeneDifferentialExpressionMetaAnalysis> ) geneDiffExMetaAnalysisDao.loadAll();
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> loadMyAnalyses() {
        return this.loadAll();
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> loadMySharedAnalyses() {
        return this.loadAll();
    }

    @Override
    public GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult ) {
        return this.geneDiffExMetaAnalysisDao.loadWithResultId( idResult );
    }

    @Override
    public GeneDifferentialExpressionMetaAnalysisResult loadResult( Long idResult ) {
        return this.geneDiffExMetaAnalysisDao.loadResult( idResult );
    }

    @Override
    public void update( GeneDifferentialExpressionMetaAnalysis analysis ) {
        geneDiffExMetaAnalysisDao.update( analysis );

    }
}
