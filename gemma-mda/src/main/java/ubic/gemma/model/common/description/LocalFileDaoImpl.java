/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
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