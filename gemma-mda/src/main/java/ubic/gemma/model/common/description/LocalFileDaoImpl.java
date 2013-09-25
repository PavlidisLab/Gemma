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

import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.persistence.AbstractDao;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class LocalFileDaoImpl extends AbstractDao<LocalFile> implements LocalFileDao {

    private static Log log = LogFactory.getLog( LocalFileDaoImpl.class.getName() );

    @Autowired
    public LocalFileDaoImpl( SessionFactory sessionFactory ) {
        super( LocalFileImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /**
     * 
     */
    @Override
    public LocalFile find( LocalFile localFile ) {

        BusinessKey.checkValidKey( localFile );

        HibernateTemplate t = new HibernateTemplate( this.getSessionFactory() );
        t.setFlushMode( HibernateAccessor.FLUSH_COMMIT );
        List<?> results;
        if ( localFile.getRemoteURL() == null ) {
            results = t.findByNamedParam( "from LocalFileImpl where localURL=:u and remoteURL is null ", "u",
                    localFile.getLocalURL() );
        } else if ( localFile.getLocalURL() == null ) {
            results = t.findByNamedParam( "from LocalFileImpl where localURL is null and remoteURL=:r", "r",
                    localFile.getRemoteURL() );
        } else {
            results = t.findByNamedParam( "from LocalFileImpl where localURL=:u and remoteURL=:r", new String[] { "u",
                    "r" }, new Object[] { localFile.getLocalURL(), localFile.getRemoteURL() } );
        }

        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + LocalFile.class.getName() + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.get( 0 );
            }
        }
        return ( LocalFile ) result;

    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(int, java.lang.String, URL, java.lang.Long)
     */
    public LocalFile findByLocalURL( final String queryString, final URL url, final java.lang.Long size ) {

        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "url", "size" },
                new Object[] { url, size } );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.get( 0 );
        }
        return ( LocalFile ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(int, URL, java.lang.Long)
     */
    @Override
    public LocalFile findByLocalURL( final URL url, final java.lang.Long size ) {
        return this.findByLocalURL(
                "from LocalFileImpl as localFile where localFile.url = :url and localFile.size = :size", url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(int, java.lang.String, URL, java.lang.Long)
     */

    public LocalFile findByRemoteURL( final java.lang.String queryString, final URL url, final java.lang.Long size ) {

        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "url", "size" },
                new Object[] { url, size } );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.get( 0 );
        }
        return ( LocalFile ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(int, URL, java.lang.Long)
     */
    @Override
    public LocalFile findByRemoteURL( final URL url, final java.lang.Long size ) {
        return this.findByRemoteURL( "from LocalFileImpl as localFile "
                + "where localFile.url = :url and localFile.size = :size", url, size );
    }

    /**
     * 
     */
    @Override
    public LocalFile findOrCreate( ubic.gemma.model.common.description.LocalFile localFile ) {
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