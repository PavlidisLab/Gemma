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
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

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
public class CoexpressionAnalysisDaoImpl extends AbstractDao<CoexpressionAnalysis> implements CoexpressionAnalysisDao {

    @Autowired
    public CoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( CoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public Collection<CoexpressionAnalysis> findByExperimentAnalyzed( final ExpressionExperiment experimentAnalyzed ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .add( Restrictions.eq( "experimentAnalyzed", experimentAnalyzed ) )
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Collection<CoexpressionAnalysis>> findByExperimentsAnalyzed( final Collection<ExpressionExperiment> experimentsAnalyzed ) {
        //noinspection unchecked
        List<CoexpressionAnalysis> results = ( List<CoexpressionAnalysis> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( CoexpressionAnalysis.class )
                .add( Restrictions.in( "experimentAnalyzed", experimentsAnalyzed ) )
                .list();
        return results.stream().collect( Collectors.groupingBy( CoexpressionAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
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
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment experimentAnalyzed ) {
        return ( CoexpCorrelationDistribution ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ccd from CoexpressionAnalysis pca "
                        + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee" )
                .setParameter( "ee", experimentAnalyzed )
                .uniqueResult();

    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> eeIds ) {
        return listByBatch( this.getSessionFactory().getCurrentSession()
                        .createQuery( "select experimentAnalyzed.id from CoexpressionAnalysis where experimentAnalyzed.id in (:ids)" ),
                "ids", eeIds, 2048 );
    }

}