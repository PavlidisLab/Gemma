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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.AbstractDao;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Repository
public class GeneDiffExMetaAnalysisDaoImpl extends AbstractDao<GeneDifferentialExpressionMetaAnalysis> implements
        GeneDiffExMetaAnalysisDao {

    @Autowired
    public GeneDiffExMetaAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( GeneDifferentialExpressionMetaAnalysisImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation ) {
        Long id = investigation.getId();
        return findByInvestigationId( id );
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigationId( Long id ) {
        final String queryString = "select distinct e, a from GeneDifferentialExpressionMetaAnalysisImpl a"
                + "  inner join a.resultSetsIncluded rs inner join rs.analysis ra where ra.experimentAnalyzed.id = :eeId";

        List<GeneDifferentialExpressionMetaAnalysis> qresult = this.getHibernateTemplate().findByNamedParam(
                queryString, "eeId", id );
        return qresult;
    }

    @Override
    public Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> results = new HashMap<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>>();

        for ( Investigation i : investigations ) {
            results.put( i, this.getAnalyses( i ) );
        }

        return results;

    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return this.getHibernateTemplate()
                .find( "from GeneDifferentialExpressionMetaAnalysisImpl where name = ?", name );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
                + "   inner join a.resultSetsIncluded rs inner join rs.analysis ra inner join ra.experimentAnalyzed ee inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample "
                + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        final String queryString = "select goa from GeneDifferentialExpressionMetaAnalysisImpl as goa where goa.taxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /**
     * @param investigation
     * @return
     */
    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalyses( Investigation investigation ) {

        Long id = investigation.getId();

        return getAnalysesForExperiment( id );

    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<GeneDifferentialExpressionMetaAnalysis> results = findByInvestigationId( id );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        results.addAll( this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ExpressionExperimentSubSetImpl subset, GeneDifferentialExpressionMetaAnalysisImpl a"
                                + " join subset.sourceExperiment see "
                                + "   inner join a.resultSetsIncluded rs  join rs.analysis ra inner join ra.experimentAnalyzed eeanalyzed where see.id=:ee and subset=eeanalyzed",
                        "ee", id ) );

        return results;
    }

    /** loads a DifferentialExpressionMetaAnalysis containing a specifc result */
    @Override
    public GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult ) {

        Criteria geneQueryMetaAnalysis = super.getSession()
                .createCriteria( GeneDifferentialExpressionMetaAnalysis.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "results" )
                .add( Restrictions.like( "id", idResult ) );

        return ( GeneDifferentialExpressionMetaAnalysis ) geneQueryMetaAnalysis.list().iterator().next();
    }

	@Override
	public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
			Collection<Long> metaAnalysisIds) {
		Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = new HashSet<GeneDifferentialExpressionMetaAnalysisSummaryValueObject>();
		
		if (metaAnalysisIds.size() > 0) {
			final String queryString = "select a.id, a.name, a.description, a.numGenesAnalyzed, "
					+ "count(distinct rs), count(distinct r) "
					+ "from GeneDifferentialExpressionMetaAnalysisImpl a "
					+ "left join a.resultSetsIncluded rs "
					+ "left join a.results r "
					+ "where a.id in (:aIds) "
					+ "group by a.id ";

			Session s = this.getSession();
			Query q = s.createQuery( queryString );
			q.setParameterList( "aIds", metaAnalysisIds );
			    
			List<Object[]> queryResults = q.list();
	    
		    for ( Object[] queryResult : queryResults ) {
		    	GeneDifferentialExpressionMetaAnalysisSummaryValueObject myMetaAnalysis = new GeneDifferentialExpressionMetaAnalysisSummaryValueObject();
		    	int index = 0;
		    	myMetaAnalysis.setId((Long)queryResult[index++]);
		    	myMetaAnalysis.setName((String)queryResult[index++]);
		    	myMetaAnalysis.setDescription((String)queryResult[index++]);
		    	myMetaAnalysis.setNumGenesAnalyzed((Integer)queryResult[index++]);
		    	myMetaAnalysis.setNumResultSetsIncluded(((Long)queryResult[index++]).intValue());
		    	myMetaAnalysis.setNumResults(((Long)queryResult[index++]).intValue());
		    	myMetaAnalyses.add(myMetaAnalysis);
		    }
		}
	    return myMetaAnalyses;
	}
    
	@Override
	public Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> findIncludedResultSetsInfoById(long analysisId) {
	    final String queryString = 
	    		  "select ra.experimentAnalyzed.id, ra.experimentAnalyzed.sourceExperiment.id, ra.id, rs.id "
	    		+ "from GeneDifferentialExpressionMetaAnalysisImpl a "
	            + "join a.resultSetsIncluded rs "
	    		+ "join rs.analysis ra " 
	    		+ "where a.id = :aId ";            
	
	    List<Object[]> qresult = this.getHibernateTemplate().findByNamedParam(queryString, "aId", analysisId );
	    
	    Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> allIncludedResultSetsInfo = new HashSet<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject>(qresult.size());
	    
		for ( Object[] object : qresult ) {
			int index = 0;
			
			GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject includedResultSetInfo = new GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject();
			
			final Long experimentId = (Long)object[index++];
			final Long subsetExperimentId = (Long)object[index++];
			
			includedResultSetInfo.setExperimentId(subsetExperimentId == null ?
					experimentId :
					subsetExperimentId);
			includedResultSetInfo.setAnalysisId((Long)object[index++]);
			includedResultSetInfo.setResultSetId((Long)object[index++]);
			
			allIncludedResultSetsInfo.add(includedResultSetInfo);
		}
		
	    return allIncludedResultSetsInfo;
	}

	@Override
	public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById(long analysisId) {
	    final String query = 
	    		"select r.gene.officialSymbol, r.gene.officialName, "
	    			+ "r.metaPvalue, r.metaQvalue, r.upperTail "
	    		+ "from GeneDifferentialExpressionMetaAnalysisImpl a "
	    		+ "left join a.results r "
	    		+ "where a.id = :aId "
	    		+ "group by r ";
	
	    List<Object[]> queryResults = this.getHibernateTemplate().findByNamedParam(query, "aId", analysisId);
	    
		Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> metaAnalysisResults = new HashSet<GeneDifferentialExpressionMetaAnalysisResultValueObject>(queryResults.size());
	    
	    for ( Object[] queryResult : queryResults ) {
	    	GeneDifferentialExpressionMetaAnalysisResultValueObject metaAnalysisResult = new GeneDifferentialExpressionMetaAnalysisResultValueObject();
	    	int index = 0;
	    	metaAnalysisResult.setGeneSymbol((String)queryResult[index++]);
	    	metaAnalysisResult.setGeneName((String)queryResult[index++]);
	    	metaAnalysisResult.setMetaPvalue((Double)queryResult[index++]);
	    	metaAnalysisResult.setMetaQvalue((Double)queryResult[index++]);
	    	metaAnalysisResult.setUpperTail((Boolean)queryResult[index++]);
	    	metaAnalysisResults.add(metaAnalysisResult);
	    }
	    return metaAnalysisResults;
	}
}
