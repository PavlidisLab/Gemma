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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class FactorValueDaoImpl extends FactorValueDaoBase {

    private Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    public FactorValueDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public FactorValue load( Long id ) {
        return ( FactorValue ) this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select fv from FactorValueImpl fv left join fetch fv.characteristics c left join fetch fv.measurement m left join fetch fv.experimentalFactor ef where fv.id=:id" )
                .setParameter( "id", id ).uniqueResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * FactorValueDaoBase#find(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public FactorValue find( FactorValue factorValue ) {
        try {
            Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( FactorValue.class );

            BusinessKey.checkKey( factorValue );

            BusinessKey.createQueryObject( queryObject, factorValue );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    this.debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size()
                            + " instances of '" + FactorValue.class.getName() + "' was found when executing query for "
                            + factorValue );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( FactorValue ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.getHibernateTemplate().find( "from FactorValueImpl where value like ?", valuePrefix + "%" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * FactorValueDaoBase#findOrCreate(ubic.gemma.model.expression.experiment
     * .FactorValue)
     */
    @Override
    public FactorValue findOrCreate( FactorValue factorValue ) {
        FactorValue existingFactorValue = this.find( factorValue );
        if ( existingFactorValue != null ) {
            return existingFactorValue;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new factorValue" );
        return create( factorValue );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * FactorValueDaoBase#remove(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public void remove( final FactorValue factorValue ) {
        if ( factorValue == null ) return;

        Collection<BioMaterial> bms = this.getHibernateTemplate().findByNamedParam(
                "select distinct bm from BioMaterialImpl as bm join bm.factorValues fv where fv = :fv", "fv",
                factorValue );

        log.info( "Disassociating " + factorValue + " from " + bms.size() + " biomaterials" );
        for ( BioMaterial bioMaterial : bms ) {
            log.info( "Processing " + bioMaterial ); // temporary, debugging.
            if ( bioMaterial.getFactorValues().remove( factorValue ) ) {
                this.getSessionFactory().getCurrentSession().update( bioMaterial );
            } else {
                log.warn( "Unexpectedly the factor value was not actually associated with " + bioMaterial );
            }
        }

        List<?> efs = this.getHibernateTemplate().findByNamedParam(
                "select ef from ExperimentalFactorImpl ef join ef.factorValues fv where fv = :fv", "fv", factorValue );

        ExperimentalFactor ef = ( ExperimentalFactor ) efs.iterator().next();
        ef.getFactorValues().remove( factorValue );
        this.getHibernateTemplate().update( ef );

        // will get the dreaded 'already in session' error if we don't do this.
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        this.getHibernateTemplate().delete( factorValue );

    }

    /**
     * @param results
     */
    private void debug( List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object + "\n" );
        }
        log.error( sb.toString() );
    }

    @Override
    public void remove( Collection<? extends FactorValue> entities ) {
        for ( FactorValue factorValue : entities ) {
            this.remove( factorValue );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.service.BaseDao#remove(java.lang.Long)
     */
    @Override
    public void remove( Long id ) {
        this.remove( this.load( id ) );
    }

    @Override
    public Collection<? extends FactorValue> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from FactorValueImpl f where f.id in (:ids)", "ids", ids );
    }

}