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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FactorValueDaoImpl extends ubic.gemma.model.expression.experiment.FactorValueDaoBase {

    private Log log = LogFactory.getLog( getClass().getName() );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.FactorValueDaoBase#find(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public FactorValue find( FactorValue factorValue ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( FactorValue.class );

            BusinessKey.checkKey( factorValue );

            BusinessKey.createQueryObject( queryObject, factorValue );

            java.util.List results = queryObject.list();
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

    /*
     * (non-Javadoc)
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
     * @see
     * ubic.gemma.model.expression.experiment.FactorValueDaoBase#remove(ubic.gemma.model.expression.experiment.FactorValue
     * )
     */
    @Override
    public void remove( FactorValue factorValue ) {
        final FactorValue toDelete = factorValue;

        this.getHibernateTemplate().executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {

                log.info( "Loading data for deletion..." );
                session.update( toDelete );

                /*
                 * everything but the association to BioMaterials is taken care of by the cascade...
                 */
                final String queryString = "FROM BioMaterialImpl AS bm LEFT JOIN bm.factorValues AS fv "
                        + "WHERE fv = :fv";
                // List list = getHibernateTemplate().findByNamedParam( queryString, "fv", toDelete );
                Query query = session.createQuery( queryString );
                query.setEntity( "fv", toDelete );
                for ( Object[] row : ( List<Object[]> ) query.list() ) {
                    BioMaterial bm = ( BioMaterial ) row[0];
                    bm.getFactorValues().remove( toDelete );
                    session.update( bm );
                    // session.evict( bm ); // required?
                }

                session.delete( toDelete );
                session.flush();
                session.clear();

                log.info( "Deleted " + toDelete );
                return null;
            }
        } );
    }

    /**
     * @param results
     */
    private void debug( List results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object + "\n" );
        }
        log.error( sb.toString() );
    }
}