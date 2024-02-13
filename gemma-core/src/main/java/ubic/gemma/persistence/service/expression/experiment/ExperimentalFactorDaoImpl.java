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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Objects;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
@Repository
public class ExperimentalFactorDaoImpl extends AbstractVoEnabledDao<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorDao {

    @Autowired
    public ExperimentalFactorDaoImpl( SessionFactory sessionFactory ) {
        super( ExperimentalFactor.class, sessionFactory );
    }

    @Override
    public ExperimentalFactor load( Long id ) {
        return ( ExperimentalFactor ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select ef from ExperimentalFactor ef left join fetch ef.factorValues fv left join fetch fv.characteristics c where ef.id=:id" )
                .setParameter( "id", id ).uniqueResult();
    }

    @Override
    public ExperimentalFactor find( ExperimentalFactor experimentalFactor ) {
        BusinessKey.checkValidKey( experimentalFactor );
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( ExperimentalFactor.class );
        BusinessKey.addRestrictions( queryObject, experimentalFactor );
        return ( ExperimentalFactor ) queryObject.uniqueResult();
    }

    @Override
    protected ExperimentalFactorValueObject doLoadValueObject( ExperimentalFactor e ) {
        return new ExperimentalFactorValueObject( e );
    }

    @Override
    public ExperimentalFactor thaw( ExperimentalFactor ef ) {
        ef = Objects.requireNonNull( this.load( ef.getId() ),
                String.format( "No ExperimentalFactory with ID %d.", ef.getId() ) );
        Hibernate.initialize( ef );
        Hibernate.initialize( ef.getExperimentalDesign() );
        return ef;
    }
}