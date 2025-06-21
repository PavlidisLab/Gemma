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
package ubic.gemma.persistence.service.analysis.expression.coexpression;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.listByBatch;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis</code>.
 * </p>
 *
 * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis
 */
@Repository
public class CoexpressionAnalysisDaoImpl extends ubic.gemma.persistence.service.AbstractDao<CoexpressionAnalysis>
        implements CoexpressionAnalysisDao, ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDao<CoexpressionAnalysis> {

    @Autowired
    public CoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( CoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public Collection<CoexpressionAnalysis> findByExperiment( final BioAssaySet experiment, boolean includeSubSets ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .add( Restrictions.eq( "experimentAnalyzed", experiment ) )
                .list();
    }

    @Override
    public Map<BioAssaySet, Collection<CoexpressionAnalysis>> findByExperiments( final Collection<? extends BioAssaySet> experiments, boolean includeSubSets ) {
        //noinspection unchecked
        List<CoexpressionAnalysis> results = ( List<CoexpressionAnalysis> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .add( Restrictions.in( "experimentAnalyzed", experiments ) )
                .list();
        return results.stream().collect( Collectors.groupingBy( SingleExperimentAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
    }

    @Override
    public Collection<CoexpressionAnalysis> findByTaxon( final Taxon taxon ) {
        //noinspection unchecked
        return ( List<CoexpressionAnalysis> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .createAlias( "experimentAnalyzed", "ee" )
                .createAlias( "ee.bioAssays", "ba" )
                .createAlias( "ba.sampleUsed", "sample" )
                .add( Restrictions.eq( "sample.sourceTaxon", taxon ) )
                .list();
    }

    @Override
    public boolean existsByExperiment( BioAssaySet ee, boolean includeSubSets ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .setProjection( Projections.rowCount() )
                .add( Restrictions.eq( "experimentAnalyzed", ee ) )
                .uniqueResult() > 0L;
    }

    @Override
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment ) {
        return ( CoexpCorrelationDistribution ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ccd from CoexpressionAnalysis pca "
                        + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee" )
                .setParameter( "ee", expressionExperiment )
                .uniqueResult();

    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return listByBatch( this.getSessionFactory().getCurrentSession()
                        .createQuery( "select experimentAnalyzed.id from CoexpressionAnalysis where experimentAnalyzed.id in (:ids)" ),
                "ids", idsToFilter, 2048 );
    }

    @Override
    public boolean hasCoexpCorrelationDistribution( ExpressionExperiment ee ) {
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ccd from CoexpressionAnalysis pca "
                        + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult() != null;
    }
}