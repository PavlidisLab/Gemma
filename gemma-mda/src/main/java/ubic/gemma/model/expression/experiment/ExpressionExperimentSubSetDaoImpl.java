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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
@Repository
public class ExpressionExperimentSubSetDaoImpl extends ExpressionExperimentSubSetDaoBase {

    @Autowired
    public ExpressionExperimentSubSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        try {
            Criteria queryObject = super.getSession().createCriteria( ExpressionExperimentSubSet.class );

            BusinessKey.checkKey( entity );

            BusinessKey.createQueryObject( queryObject, entity );
            return ( ExpressionExperimentSubSet ) queryObject.uniqueResult();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet.getName() == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign must have name or external accession." );
        }
        ExpressionExperimentSubSet existing = this.find( expressionExperimentSubSet );
        if ( existing != null ) {
            return existing;
        }
        return create( expressionExperimentSubSet );
    }

    @Override
    public Collection<? extends ExpressionExperimentSubSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSubSetImpl where id in (:ids)",
                "ids", ids );
    }

}
