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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

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
    public Collection<IncludedResultSetInfoValueObject> findIncludedResultSetsInfoById( long analysisId ) {
        //language=HQL
        final String queryString =
                "select ra.experimentAnalyzed.id, ra.id, rs.id " + "from GeneDifferentialExpressionMetaAnalysis a "
                        + "join a.resultSetsIncluded rs " + "join rs.analysis ra " + "where a.id = :aId ";

        //noinspection unchecked
        List<Object[]> qResult = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "aId", analysisId ).list();

        Collection<IncludedResultSetInfoValueObject> allIncludedResultSetsInfo = new HashSet<>( qResult.size() );

        for ( Object[] object : qResult ) {
            int index = 0;

            IncludedResultSetInfoValueObject includedResultSetInfo = new IncludedResultSetInfoValueObject();

            includedResultSetInfo.setExperimentId( ( Long ) object[index++] );
            includedResultSetInfo.setAnalysisId( ( Long ) object[index++] );
            //noinspection UnusedAssignment // Better readability
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
            //language=HQL
            final String queryString = "select a.id, a.name, a.description, a.numGenesAnalyzed, "
                    + "count(distinct rs), count(distinct r) " + "from GeneDifferentialExpressionMetaAnalysis a "
                    + "left join a.resultSetsIncluded rs " + "left join a.results r " + "where a.id in (:aIds) "
                    + "group by a.id ";

            //noinspection unchecked
            List<Object[]> queryResults = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "aIds", optimizeParameterList( metaAnalysisIds ) ).list();

            for ( Object[] queryResult : queryResults ) {
                GeneDifferentialExpressionMetaAnalysisSummaryValueObject myMetaAnalysis = new GeneDifferentialExpressionMetaAnalysisSummaryValueObject();
                int index = 0;
                myMetaAnalysis.setId( ( Long ) queryResult[index++] );
                myMetaAnalysis.setName( ( String ) queryResult[index++] );
                myMetaAnalysis.setDescription( ( String ) queryResult[index++] );
                myMetaAnalysis.setNumGenesAnalyzed( ( Integer ) queryResult[index++] );
                myMetaAnalysis.setNumResultSetsIncluded( ( ( Long ) queryResult[index++] ).intValue() );
                //noinspection UnusedAssignment // Better readability
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
        List<Object[]> queryResults = this.getSessionFactory().getCurrentSession().createQuery( query )
                .setParameter( "aId", analysisId ).list();

        Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> metaAnalysisResults = new HashSet<>(
                queryResults.size() );

        for ( Object[] queryResult : queryResults ) {
            GeneDifferentialExpressionMetaAnalysisResultValueObject metaAnalysisResult = new GeneDifferentialExpressionMetaAnalysisResultValueObject();
            int index = 0;
            metaAnalysisResult.setGeneSymbol( ( String ) queryResult[index++] );
            metaAnalysisResult.setGeneName( ( String ) queryResult[index++] );
            metaAnalysisResult.setMetaPvalue( ( Double ) queryResult[index++] );
            metaAnalysisResult.setMetaQvalue( ( Double ) queryResult[index++] );
            //noinspection UnusedAssignment // Better readability
            metaAnalysisResult.setUpperTail( ( Boolean ) queryResult[index++] );
            metaAnalysisResults.add( metaAnalysisResult );
        }
        return metaAnalysisResults;
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from GeneDifferentialExpressionMetaAnalysis a "
                        + "join a.resultSetsIncluded rs "
                        + "join rs.analysis ra "
                        + "where ra.experimentAnalyzed.id in (:ids) "
                        + "group by a" )
                .setParameterList( "ids", optimizeParameterList( idsToFilter ) )
                .list();
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

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByExperiment( BioAssaySet experiment ) {
        Long id = experiment.getId();
        return this.findByExperimentId( id );
    }

    @Override
    public Map<BioAssaySet, Collection<GeneDifferentialExpressionMetaAnalysis>> findByExperiments(
            Collection<? extends BioAssaySet> experiments ) {
        Map<BioAssaySet, Collection<GeneDifferentialExpressionMetaAnalysis>> results = new HashMap<>();
        for ( BioAssaySet i : experiments ) {
            results.put( i, this.getAnalyses( i ) );
        }
        return results;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return this.findByProperty( "name", name );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from GeneDifferentialExpressionMetaAnalysis a "
                        + "join a.resultSetsIncluded rs "
                        + "join rs.analysis ra "
                        + "join ra.experimentAnalyzed ee "
                        + "join ee.bioAssays as ba "
                        + "join ba.sampleUsed as sample "
                        + "where sample.sourceTaxon = :taxon "
                        + "group by a" )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public void removeForExperiment( BioAssaySet ee ) {
        this.remove( this.findByProperty( "experimentAnalyzed", ee ) );
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> findByExperimentId( Long id ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from GeneDifferentialExpressionMetaAnalysis a "
                        + "join a.resultSetsIncluded rs "
                        + "join rs.analysis ra "
                        + "where ra.experimentAnalyzed.id = :eeId "
                        + "group by a" )
                .setParameter( "eeId", id )
                .list();
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalyses( Investigation investigation ) {
        return this.getAnalysesForExperiment( investigation.getId() );
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<GeneDifferentialExpressionMetaAnalysis> results = this.findByExperimentId( id );
        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                        "select a from ExpressionExperimentSubSet subset, GeneDifferentialExpressionMetaAnalysis a "
                                + "join subset.sourceExperiment see "
                                + "join a.resultSetsIncluded rs "
                                + "join rs.analysis ra "
                                + "join ra.experimentAnalyzed eeanalyzed "
                                + "where see.id=:ee and subset=eeanalyzed "
                                + "group by a" )
                .setParameter( "ee", id ).list() );

        return results;
    }

}
