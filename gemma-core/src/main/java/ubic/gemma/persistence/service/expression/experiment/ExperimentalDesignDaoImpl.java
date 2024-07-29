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

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import javax.annotation.Nullable;
import java.util.Random;

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
        return ( ExperimentalDesign ) this.getSessionFactory().getCurrentSession().createCriteria( ExperimentalDesign.class )
                .add( Restrictions.eq( "name", experimentalDesign.getName() ) )
                .uniqueResult();
    }

    @Override
    public ExperimentalDesign loadWithExperimentalFactors( Long id ) {
        return ( ExperimentalDesign ) getSessionFactory().getCurrentSession().createQuery( "select ed from ExperimentalDesign ed "
                        + "left join fetch ed.experimentalFactors "
                        + "where ed.id = :id" )
                .setParameter( "id", id )
                .uniqueResult();
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

    @Nullable
    @Override
    public ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludedDesign ) {
        Long numThatNeedsAttention = ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(distinct ed) from ExperimentalDesign ed join ed.experimentalFactors ef "
                        + "join ef.factorValues fv where ed.id != :edId and fv.needsAttention = true" )
                .setParameter( "edId", excludedDesign.getId() )
                .uniqueResult();
        return ( ExperimentalDesign ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ed from ExperimentalDesign ed join ed.experimentalFactors ef "
                        + "join ef.factorValues fv where ed.id != :edId and fv.needsAttention = true" )
                .setParameter( "edId", excludedDesign.getId() )
                .setFirstResult( RandomUtils.nextInt( numThatNeedsAttention.intValue() ) )
                .setMaxResults( 1 )
                .uniqueResult();
    }
}