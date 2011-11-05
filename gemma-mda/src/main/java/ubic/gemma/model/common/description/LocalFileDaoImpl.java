/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class LocalFileDaoImpl extends ubic.gemma.model.common.description.LocalFileDaoBase {

    private static Log log = LogFactory.getLog( LocalFileDaoImpl.class.getName() );

    @Autowired
    public LocalFileDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /**
     * 
     */
    @Override
    public LocalFile find( ubic.gemma.model.common.description.LocalFile localFile ) {
        try {

            Criteria queryObject = super.getSession().createCriteria( LocalFile.class );

            BusinessKey.checkValidKey( localFile );

            if ( localFile.getLocalURL() != null ) {
                queryObject.add( Restrictions.eq( "localURL", localFile.getLocalURL() ) );
            }

            if ( localFile.getRemoteURL() != null ) {
                queryObject.add( Restrictions.eq( "remoteURL", localFile.getRemoteURL() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + LocalFile.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( LocalFile ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * 
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findOrCreate(
            ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null ) throw new IllegalArgumentException();
        LocalFile existingLocalFile = find( localFile );
        if ( existingLocalFile != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing localFile: " + existingLocalFile.getLocalURL() );
            return existingLocalFile;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new localFile: " + localFile.getLocalURL() );
        return create( localFile );
    }

}