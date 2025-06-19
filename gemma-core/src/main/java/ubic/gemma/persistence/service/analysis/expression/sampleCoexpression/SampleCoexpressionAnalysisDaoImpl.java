/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author paul
 */
@Repository
class SampleCoexpressionAnalysisDaoImpl extends AbstractDao<SampleCoexpressionAnalysis>
        implements SampleCoexpressionAnalysisDao, ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDao<SampleCoexpressionAnalysis> {

    @Autowired
    public SampleCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( SampleCoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public SampleCoexpressionAnalysis load( ExpressionExperiment ee ) {

        Collection<SampleCoexpressionAnalysis> r = this.findByExperiment( ee, true );

        if ( r.isEmpty() )
            return null;

        if ( r.size() > 1 ) {
            log.warn( "More than one analysis found! Run analysis recomputation to attempt automatic cleanup." );
        }

        return r.iterator().next();

    }

    @Override
    public boolean existsByExperiment( BioAssaySet ee, boolean includeSubSets ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createCriteria( SampleCoexpressionAnalysis.class )
                .setProjection( Projections.rowCount() )
                .add( Restrictions.eq( "experimentAnalyzed", ee ) )
                .uniqueResult() > 0L;
    }

    @Override
    public Collection<SampleCoexpressionAnalysis> findByExperiment( BioAssaySet experiment, boolean includeSubSets ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( SampleCoexpressionAnalysis.class )
                .add( Restrictions.eq( "experimentAnalyzed", experiment ) )
                .list();
    }

    @Override
    public Map<BioAssaySet, Collection<SampleCoexpressionAnalysis>> findByExperiments( Collection<? extends BioAssaySet> experiments, boolean includeSubSets ) {
        //noinspection unchecked
        List<SampleCoexpressionAnalysis> results = ( List<SampleCoexpressionAnalysis> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( SampleCoexpressionAnalysis.class )
                .add( Restrictions.in( "experimentAnalyzed", experiments ) )
                .list();
        return results.stream().collect( Collectors.groupingBy( SingleExperimentAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
    }
}
