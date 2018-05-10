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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

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
    public void remove( ExperimentalFactor experimentalFactor ) {
        Long experimentalDesignId = experimentalFactor.getExperimentalDesign().getId();
        ExperimentalDesign ed = ( ExperimentalDesign ) this.getSessionFactory().getCurrentSession()
                .load( ExperimentalDesign.class, experimentalDesignId );

        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee where ee.experimentalDesign = :ed";
        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString, "ed", ed );

        if ( results.size() == 0 ) {
            throw new IllegalArgumentException( "No expression experiment for experimental design " + ed );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) results.iterator().next();
        Session session = this.getSessionFactory().getCurrentSession();

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();

            Collection<FactorValue> factorValuesToRemoveFromBioMaterial = new HashSet<>();
            for ( FactorValue factorValue : bm.getFactorValues() ) {
                if ( experimentalFactor.equals( factorValue.getExperimentalFactor() ) ) {
                    factorValuesToRemoveFromBioMaterial.add( factorValue );
                    this.getSessionFactory().getCurrentSession().evict( factorValue.getExperimentalFactor() );
                }
            }

            // if there are factor values to remove
            if ( factorValuesToRemoveFromBioMaterial.size() > 0 ) {
                bm.getFactorValues().removeAll( factorValuesToRemoveFromBioMaterial );
                session.update( bm );
            }
        }

        // remove the experimental factor this cascades to values.
        session.delete( experimentalFactor );
    }

    @Override
    public ExperimentalFactor find( ExperimentalFactor experimentalFactor ) {

        BusinessKey.checkValidKey( experimentalFactor );
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( ExperimentalFactor.class );
        BusinessKey.addRestrictions( queryObject, experimentalFactor );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {

                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        results.size() + " " + ExperimentalFactor.class.getName()
                                + "s were found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( ExperimentalFactor ) result;

    }

    @Override
    public ExperimentalFactor findOrCreate( ExperimentalFactor experimentalFactor ) {
        ExperimentalFactor existing = this.find( experimentalFactor );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        AbstractDao.log.debug( "Creating new arrayDesign: " + experimentalFactor.getName() );
        return this.create( experimentalFactor );
    }

    @Override
    public ExperimentalFactorValueObject loadValueObject( ExperimentalFactor e ) {
        return new ExperimentalFactorValueObject( e );
    }

    @Override
    public Collection<ExperimentalFactorValueObject> loadValueObjects( Collection<ExperimentalFactor> entities ) {
        Collection<ExperimentalFactorValueObject> vos = new LinkedHashSet<>();
        for ( ExperimentalFactor fv : entities ) {
            vos.add( new ExperimentalFactorValueObject( fv ) );
        }
        return vos;
    }

}