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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDaoBase;

import java.util.Collection;

/**
 * @author paul
 */
@Repository
class SampleCoexpressionAnalysisDaoImpl extends SingleExperimentAnalysisDaoBase<SampleCoexpressionAnalysis>
        implements SampleCoexpressionAnalysisDao {

    @Autowired
    public SampleCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( SampleCoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public SampleCoexpressionAnalysis load( ExpressionExperiment ee ) {

        Collection<SampleCoexpressionAnalysis> r = this.findByExperiment( ee );

        if ( r.isEmpty() )
            return null;

        if ( r.size() > 1 ) {
            AbstractDao.log.warn( "More than one analysis found! Run analysis recomputation to attempt automatic cleanup." );
        }

        return r.iterator().next();

    }
}
