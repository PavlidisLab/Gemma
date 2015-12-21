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
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.AbstractDao;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
@Repository
public class ExperimentalFactorDaoImpl extends AbstractDao<ExperimentalFactor> implements ExperimentalFactorDao {

    private static Log log = LogFactory.getLog( ExperimentalFactorDaoImpl.class.getName() );

    @Autowired
    public ExperimentalFactorDaoImpl( SessionFactory sessionFactory ) {
        super( ExperimentalFactorImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public ExperimentalFactor load( Long id ) {
        return ( ExperimentalFactor ) this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select ef from ExperimentalFactorImpl ef left join fetch ef.factorValues fv left join fetch fv.characteristics c where ef.id=:id" )
                .setParameter( "id", id ).uniqueResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor find( ExperimentalFactor experimentalFactor ) {

        BusinessKey.checkValidKey( experimentalFactor );
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( ExperimentalFactor.class );
        BusinessKey.addRestrictions( queryObject, experimentalFactor );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {

                throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                        + ExperimentalFactor.class.getName() + "s were found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( ExperimentalFactor ) result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase#findOrCreate(ubic.gemma.model.expression.experiment
     * .ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor findOrCreate( ExperimentalFactor experimentalFactor ) {
        ExperimentalFactor existing = this.find( experimentalFactor );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        log.debug( "Creating new arrayDesign: " + experimentalFactor.getName() );
        return create( experimentalFactor );
    }

    @Override
    public void remove( ExperimentalFactor experimentalFactor ) {
        Long experimentalDesignId = experimentalFactor.getExperimentalDesign().getId();
        ExperimentalDesign ed = ( ExperimentalDesign ) this.getSessionFactory().getCurrentSession()
                .load( ExperimentalDesignImpl.class, experimentalDesignId );

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee where ee.experimentalDesign = :ed";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "ed", ed );

        if ( results.size() == 0 ) {
            throw new IllegalArgumentException( "No expression experiment for experimental design " + ed );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) results.iterator().next();

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();

            Collection<FactorValue> factorValuesToRemoveFromBioMaterial = new HashSet<>();
            for ( FactorValue factorValue : bm.getFactorValues() ) {
                if ( experimentalFactor.equals( factorValue.getExperimentalFactor() ) ) {
                    factorValuesToRemoveFromBioMaterial.add( factorValue );
                    this.getSessionFactory().getCurrentSession().evict( factorValue.getExperimentalFactor() );
                }
            }

            // if there are factorvalues to remove
            if ( factorValuesToRemoveFromBioMaterial.size() > 0 ) {
                bm.getFactorValues().removeAll( factorValuesToRemoveFromBioMaterial );
                // this.getSessionFactory().getCurrentSession().update( bm ); // needed? see bug 4341
            }
        }

        // ed.getExperimentalFactors().remove( experimentalFactor );
        // delete the experimental factor this cascades to values.

        // this.getExperimentalDesignDao().update( ed );
        this.getHibernateTemplate().delete( experimentalFactor );
    }
}