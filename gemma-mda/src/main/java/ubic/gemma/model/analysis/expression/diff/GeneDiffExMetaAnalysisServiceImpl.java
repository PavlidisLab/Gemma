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
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void update( GeneDifferentialExpressionMetaAnalysis analysis ) {
        geneDiffExMetaAnalysisDao.update( analysis );

    }
    
    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisValueObject> loadMyAnalysisVOs() {
	
    	Collection<GeneDifferentialExpressionMetaAnalysis> myAnalyses = loadMyAnalyses();
    	
    	Collection<GeneDifferentialExpressionMetaAnalysisValueObject> analysisVOs = new HashSet<GeneDifferentialExpressionMetaAnalysisValueObject>(myAnalyses.size());
    	
    	for (GeneDifferentialExpressionMetaAnalysis analysis: myAnalyses) {
    		analysisVOs.add(new GeneDifferentialExpressionMetaAnalysisValueObject(analysis));
    	}
    	
    	return analysisVOs;
	}

	@Override
	public void delete(Long id) {
		GeneDifferentialExpressionMetaAnalysis metaAnalysis = load( id );
		
		// TODO: Should throw exception if the meta-analysis cannot be deleted.
		if (metaAnalysis != null) {
			delete(metaAnalysis);
		}
	}
}


