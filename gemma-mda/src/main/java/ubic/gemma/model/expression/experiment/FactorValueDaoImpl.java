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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class FactorValueDaoImpl extends ubic.gemma.model.expression.experiment.FactorValueDaoBase {

    private Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    public FactorValueDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.FactorValueDaoBase#find(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public FactorValue find( FactorValue factorValue ) {
        try {
            Criteria queryObject = super.getSession().createCriteria( FactorValue.class );

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
     * ubic.gemma.model.expression.experiment.FactorValueDaoBase#findOrCreate(ubic.gemma.model.expression.experiment
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
     * ubic.gemma.model.expression.experiment.FactorValueDaoBase#remove(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public void remove( final FactorValue factorValue ) {
        if ( factorValue == null ) return;

        Collection<BioMaterial> bms = this.getHibernateTemplate().findByNamedParam(
                "select distinct bm from BioMaterialImpl as bm inner join bm.factorValues fv where fv = :fv", "fv",
                factorValue );

        for ( BioMaterial bioMaterial : bms ) {
            bioMaterial.getFactorValues().remove( factorValue );
            this.getHibernateTemplate().update( bioMaterial );
        }

        List<?> efs = this.getHibernateTemplate().findByNamedParam(
                "select ef from ExperimentalFactorImpl ef inner join ef.factorValues fv where fv = :fv", "fv",
                factorValue );

        ExperimentalFactor ef = ( ExperimentalFactor ) efs.iterator().next();
        ef.getFactorValues().remove( factorValue );
        this.getHibernateTemplate().update( ef );

        // we have to do this to avoid the 'already in session' error. Annoying.
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        // finally delete it.
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

    @Override
    public void remove( Long id ) {
        this.remove( this.load( id ) );
    }

    @Override
    public Collection<? extends FactorValue> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from FactorValueImpl f where f.id in (:ids)", "ids", ids );
    }

}