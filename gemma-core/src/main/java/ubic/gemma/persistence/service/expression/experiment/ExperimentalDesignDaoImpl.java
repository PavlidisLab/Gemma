/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.List;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesign
 */
@Repository
public class ExperimentalDesignDaoImpl extends AbstractDao<ExperimentalDesign> implements ExperimentalDesignDao {

    @Autowired
    public ExperimentalDesignDaoImpl( SessionFactory sessionFactory ) {
        super( ExperimentalDesign.class, sessionFactory );
    }

    @Override
    public ExperimentalDesign find( ExperimentalDesign experimentalDesign ) {

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( ExperimentalDesign.class );

        queryObject.add( Restrictions.eq( "name", experimentalDesign.getName() ) );

        List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + ExperimentalDesign.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( ExperimentalDesign ) result;

    }

    /**
     * @see ExperimentalDesignDao#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ExpressionExperiment getExpressionExperiment( final ExperimentalDesign experimentalDesign ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee FROM ExpressionExperiment as ee where ee.experimentalDesign = :ed " )
                .setParameter( "ed", experimentalDesign ).uniqueResult();
    }
}