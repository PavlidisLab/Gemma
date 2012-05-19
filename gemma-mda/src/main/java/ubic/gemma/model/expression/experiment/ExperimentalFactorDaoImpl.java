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
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
@Repository
public class ExperimentalFactorDaoImpl extends ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase {

    @Override
    public void remove( ExperimentalFactor experimentalFactor ) {
        Long experimentalDesignId = experimentalFactor.getExperimentalDesign().getId();
        ExperimentalDesign ed = ( ExperimentalDesign ) this.getSession().load( ExperimentalDesignImpl.class,
                experimentalDesignId );

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee where ee.experimentalDesign = :ed";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "ed", ed );

        if ( results.size() == 0 ) {
            throw new IllegalArgumentException( "No expression experiment for experimental design " + ed );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) results.iterator().next();

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {

                Collection<FactorValue> factorValuesToRemoveFromBioMaterial = new HashSet<FactorValue>();
                for ( FactorValue factorValue : bm.getFactorValues() ) {
                    if ( experimentalFactor.equals( factorValue.getExperimentalFactor() ) ) {
                        factorValuesToRemoveFromBioMaterial.add( factorValue );
                        this.getSession().evict( factorValue.getExperimentalFactor() );
                    }
                }

                // if there are factorvalues to remove
                if ( factorValuesToRemoveFromBioMaterial.size() > 0 ) {
                    bm.getFactorValues().removeAll( factorValuesToRemoveFromBioMaterial );
                    // getBioMaterialDao().update( bm ); // needed?
                }
            }
        }

        // ed.getExperimentalFactors().remove( experimentalFactor );
        // delete the experimental factor this cascades to values.

        // this.getExperimentalDesignDao().update( ed );
        this.getHibernateTemplate().delete( experimentalFactor );
    }

    private static Log log = LogFactory.getLog( ExperimentalFactorDaoImpl.class.getName() );

    @Autowired
    public ExperimentalFactorDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
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
        Criteria queryObject = super.getSession().createCriteria( ExperimentalFactor.class );
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
}