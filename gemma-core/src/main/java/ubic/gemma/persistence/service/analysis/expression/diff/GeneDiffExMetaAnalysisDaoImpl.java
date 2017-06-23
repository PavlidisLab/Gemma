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

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;

/**
 * @author Paul
 */
@Repository
public class GeneDiffExMetaAnalysisDaoImpl extends AbstractDao<GeneDifferentialExpressionMetaAnalysis>
        implements GeneDiffExMetaAnalysisDao {

    @Autowired
    public GeneDiffExMetaAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( GeneDifferentialExpressionMetaAnalysis.class, sessionFactory );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation ) {
        Long id = investigation.getId();
        return findByInvestigationId( id );
    }

    @Override
    public Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> results = new HashMap<>();
        for ( Investigation i : investigations ) {
            results.put( i, this.getAnalyses( i ) );
        }
        return results;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return this.findByProperty( "name", name );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysis a"
                + "   inner join a.resultSetsIncluded rs inner join rs.analysis ra inner join ra.experimentAnalyzed"
                + " ee inner join ee.bioAssays as ba " + "inner join ba.sampleUsed as sample "
                + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon ).list();
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        final String queryString = "select goa from GeneDifferentialExpressionMetaAnalysis as goa where goa.taxon = :taxon ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon ).list();
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> findIncludedResultSetsInfoById(
            long analysisId ) {
        final String queryString =
                "select ra.experimentAnalyzed.id, ra.experimentAnalyzed.sourceExperiment.id, ra.id, rs.id "
                        + "from GeneDifferentialExpressionMetaAnalysis a " + "join a.resultSetsIncluded rs "
                        + "join rs.analysis ra " + "where a.id = :aId ";

        //noinspection unchecked
        List<Object[]> qResult = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "aId", analysisId ).list();

        Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> allIncludedResultSetsInfo = new HashSet<>(
                qResult.size() );

        for ( Object[] object : qResult ) {
            int index = 0;

            GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject includedResultSetInfo = new GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject();

            final Long experimentId = ( Long ) object[index++];
            final Long subsetExperimentId = ( Long ) object[index++];

            includedResultSetInfo.setExperimentId( subsetExperimentId == null ? experimentId : subsetExperimentId );
            includedResultSetInfo.setAnalysisId( ( Long ) object[index++] );
            includedResultSetInfo.setResultSetId( ( Long ) object[index++] );

            allIncludedResultSetsInfo.add( includedResultSetInfo );
        }

        return allIncludedResultSetsInfo;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds ) {
        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = new HashSet<>();

        if ( metaAnalysisIds.size() > 0 ) {
            final String queryString = "select a.id, a.name, a.description, a.numGenesAnalyzed, "
                    + "count(distinct rs), count(distinct r) " + "from GeneDifferentialExpressionMetaAnalysis a "
                    + "left join a.resultSetsIncluded rs " + "left join a.results r " + "where a.id in (:aIds) "
                    + "group by a.id ";

            //noinspection unchecked
            List<Object[]> queryResults = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "aIds", metaAnalysisIds ).list();

            for ( Object[] queryResult : queryResults ) {
                GeneDifferentialExpressionMetaAnalysisSummaryValueObject myMetaAnalysis = new GeneDifferentialExpressionMetaAnalysisSummaryValueObject();
                int index = 0;
                myMetaAnalysis.setId( ( Long ) queryResult[index++] );
                myMetaAnalysis.setName( ( String ) queryResult[index++] );
                myMetaAnalysis.setDescription( ( String ) queryResult[index++] );
                myMetaAnalysis.setNumGenesAnalyzed( ( Integer ) queryResult[index++] );
                myMetaAnalysis.setNumResultSetsIncluded( ( ( Long ) queryResult[index++] ).intValue() );
                myMetaAnalysis.setNumResults( ( ( Long ) queryResult[index++] ).intValue() );
                myMetaAnalyses.add( myMetaAnalysis );
            }
        }
        return myMetaAnalyses;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId ) {
        final String query =
                "select r.gene.officialSymbol, r.gene.officialName, " + "r.metaPvalue, r.metaQvalue, r.upperTail "
                        + "from GeneDifferentialExpressionMetaAnalysis a " + "left join a.results r "
                        + "where a.id = :aId " + "group by r ";

        //noinspection unchecked
        List<Object[]> queryResults = this.getSessionFactory().getCurrentSession().createQuery( query ).setParameter( "aId", analysisId ).list();

        Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> metaAnalysisResults = new HashSet<>(
                queryResults.size() );

        for ( Object[] queryResult : queryResults ) {
            GeneDifferentialExpressionMetaAnalysisResultValueObject metaAnalysisResult = new GeneDifferentialExpressionMetaAnalysisResultValueObject();
            int index = 0;
            metaAnalysisResult.setGeneSymbol( ( String ) queryResult[index++] );
            metaAnalysisResult.setGeneName( ( String ) queryResult[index++] );
            metaAnalysisResult.setMetaPvalue( ( Double ) queryResult[index++] );
            metaAnalysisResult.setMetaQvalue( ( Double ) queryResult[index++] );
            metaAnalysisResult.setUpperTail( ( Boolean ) queryResult[index++] );
            metaAnalysisResults.add( metaAnalysisResult );
        }
        return metaAnalysisResults;
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * loads a neDifferentialExpressionMetaAnalysisResult
     */
    @Override
    public GeneDifferentialExpressionMetaAnalysisResult loadResult( Long idResult ) {

        Criteria geneQueryMetaAnalysis = this.getSessionFactory().getCurrentSession()
                .createCriteria( GeneDifferentialExpressionMetaAnalysisResult.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
                .add( Restrictions.like( "id", idResult ) );

        return ( GeneDifferentialExpressionMetaAnalysisResult ) geneQueryMetaAnalysis.list().iterator().next();
    }

    /**
     * loads a DifferentialExpressionMetaAnalysis containing a specific result
     */
    @Override
    public GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult ) {

        Criteria geneQueryMetaAnalysis = this.getSessionFactory().getCurrentSession()
                .createCriteria( GeneDifferentialExpressionMetaAnalysis.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "results" )
                .add( Restrictions.like( "id", idResult ) );

        return ( GeneDifferentialExpressionMetaAnalysis ) geneQueryMetaAnalysis.list().iterator().next();
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigationId( Long id ) {
        final String queryString = "select distinct a from GeneDifferentialExpressionMetaAnalysis a"
                + "  inner join a.resultSetsIncluded rs inner join rs.analysis ra where ra.experimentAnalyzed.id = :eeId";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "eeId", id ).list();
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalyses( Investigation investigation ) {
        return getAnalysesForExperiment( investigation.getId() );
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<GeneDifferentialExpressionMetaAnalysis> results = findByInvestigationId( id );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct a from ExpressionExperimentSubSet subset, GeneDifferentialExpressionMetaAnalysis a"
                        + " join subset.sourceExperiment see "
                        + "   inner join a.resultSetsIncluded rs  join rs.analysis ra inner join ra.experimentAnalyzed eeanalyzed where see.id=:ee and subset=eeanalyzed" )
                .setParameter( "ee", id ).list() );

        return results;
    }

}
