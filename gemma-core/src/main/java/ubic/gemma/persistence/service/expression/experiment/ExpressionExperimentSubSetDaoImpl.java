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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
@Repository
public class ExpressionExperimentSubSetDaoImpl extends AbstractDao<ExpressionExperimentSubSet>
        implements ExpressionExperimentSubSetDao {

    @Autowired
    public ExpressionExperimentSubSetDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentSubSet.class, sessionFactory );
    }

    @Override
    protected ExpressionExperimentSubSet findByBusinessKey( ExpressionExperimentSubSet entity ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionExperimentSubSet.class );
        BusinessKey.checkKey( entity );
        BusinessKey.createQueryObject( queryObject, entity );
        return ( ExpressionExperimentSubSet ) queryObject.uniqueResult();
    }

    @Override
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct fv from ExpressionExperimentSubSet es join es.bioAssays ba join ba.sampleUsed bm "
                                + "join bm.factorValues fv where es=:es and fv.experimentalFactor = :ef " )
                .setParameter( "es", entity ).setParameter( "ef", factor ).list();
    }

    @Override
    public Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        //noinspection unchecked
        List<FactorValue> list = this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct fv from ExpressionExperimentSubSet es join es.bioAssays ba join ba.sampleUsed bm "
                                + "join bm.factorValues fv where es.id=:es and fv.experimentalFactor.id = :ef " )
                .setParameter( "es", subSetId ).setParameter( "ef", experimentalFactor ).list();
        Collection<FactorValueValueObject> result = new HashSet<>();
        for ( FactorValue fv : list ) {
            result.add( new FactorValueValueObject( fv ) );
        }
        return result;
    }
}