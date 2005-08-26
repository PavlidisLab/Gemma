/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LocalFileDaoImpl extends edu.columbia.gemma.common.description.LocalFileDaoBase {

    private static Log log = LogFactory.getLog( LocalFileDaoImpl.class.getName() );

    /**
     * @see edu.columbia.gemma.common.description.LocalFile#findOrCreate(int, java.lang.String,
     *      edu.columbia.gemma.common.description.LocalFile)
     */
    @SuppressWarnings("boxing")
    @Override
    public LocalFile find( edu.columbia.gemma.common.description.LocalFile localFile ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( LocalFile.class );

            queryObject.add( Restrictions.eq( "localURI", localFile.getLocalURI() ) );

            if ( localFile.getSize() != 0 ) queryObject.add( Restrictions.eq( "size", localFile.getSize() ) );

            if ( localFile.getRemoteURI() != null )
                queryObject.add( Restrictions.eq( "remoteURI", localFile.getRemoteURI() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + LocalFile.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.description.LocalFile ) results.iterator().next();
                }
            }
            return ( LocalFile ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @see edu.columbia.gemma.common.description.LocalFile#find(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    public edu.columbia.gemma.common.description.LocalFile findOrCreate(
            edu.columbia.gemma.common.description.LocalFile localFile ) {
        if ( localFile == null || localFile.getLocalURI() == null
                || ( localFile.getRemoteURI() == null && localFile.getSize() == 0 ) ) {
            log.error( "localFile was null or had no valid business keys : " + localFile.getLocalURI() );
            return null;
        }
        LocalFile newlocalFile = find( localFile );
        if ( newlocalFile != null ) {
            log.debug( "Found existing localFile: " + localFile.getLocalURI() );
            return newlocalFile;
        }
        log.debug( "Creating new localFile: " + localFile.getLocalURI() );
        return create( localFile );
    }

}