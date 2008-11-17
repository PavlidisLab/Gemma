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
package ubic.gemma.model.expression.bioAssay;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDaoImpl extends ubic.gemma.model.expression.bioAssay.BioAssayDaoBase {

    private static Log log = LogFactory.getLog( BioAssayDaoImpl.class.getName() );

    @SuppressWarnings("unchecked")
    @Override
    public BioAssay find( BioAssay bioAssay ) {
        try {
            Criteria queryObject = BusinessKey.createQueryObject( super.getSession( false ), bioAssay );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioAssay.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BioAssay ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public BioAssay findOrCreate( BioAssay bioAssay ) {
        if ( bioAssay == null || bioAssay.getName() == null ) {
            throw new IllegalArgumentException( "BioAssay was null or had no name : " + bioAssay );
        }
        BioAssay newBioAssay = find( bioAssay );
        if ( newBioAssay != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing bioAssay: " + newBioAssay );
            return newBioAssay;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new bioAssay: " + bioAssay );
        return create( bioAssay );
    }

    @Override
    public void handleThaw( final BioAssay bioAssay ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( bioAssay, LockMode.NONE );
                Hibernate.initialize( bioAssay.getArrayDesignUsed() );
                Hibernate.initialize( bioAssay.getDerivedDataFiles() );
                for ( BioMaterial bm : bioAssay.getSamplesUsed() ) {
                    Hibernate.initialize( bm );
                    Hibernate.initialize( bm.getBioAssaysUsedIn() );
                    Hibernate.initialize( bm.getFactorValues() );
                }
                session.evict( bioAssay );
                return null;
            }
        } );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioAssayImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( "countsCache" );
            return ( ( Long ) queryObject.iterate().next() ).intValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}