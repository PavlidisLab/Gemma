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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.BusinessKey;

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
            Criteria queryObject = super.getSessionFactory().getCurrentSession()
                    .createCriteria( ExpressionExperimentSubSet.class );

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ExpressionExperimentSubSetDao#getFactorValuesUsed(ubic.gemma.model.expression
     * .experiment.ExpressionExperimentSubSet, ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        return this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select distinct fv from ExpressionExperimentSubSetImpl es join es.bioAssays ba join ba.sampleUsed bm "
                                + "join bm.factorValues fv where es=:es and fv.experimentalFactor = :ef " )
                .setParameter( "es", entity ).setParameter( "ef", factor ).list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ExpressionExperimentSubSetDao#getFactorValuesUsed(java.lang.Long,
     * java.lang.Long)
     */
    @Override
    public Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        List<FactorValue> list = this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select distinct fv from ExpressionExperimentSubSetImpl es join es.bioAssays ba join ba.sampleUsed bm "
                                + "join bm.factorValues fv where es.id=:es and fv.experimentalFactor.id = :ef " )
                .setParameter( "es", subSetId ).setParameter( "ef", experimentalFactor ).list();
        Collection<FactorValueValueObject> result = new HashSet<>();
        for ( FactorValue fv : list ) {
            result.add( new FactorValueValueObject( fv ) );
        }
        return result;
    }

    @Override
    public Collection<? extends ExpressionExperimentSubSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSubSetImpl where id in (:ids)",
                "ids", ids );
    }
}