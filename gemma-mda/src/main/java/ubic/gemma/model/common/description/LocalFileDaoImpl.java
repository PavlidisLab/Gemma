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
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BeanPropertyCompleter;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
public class LocalFileDaoImpl extends ubic.gemma.model.common.description.LocalFileDaoBase {

    private static Log log = LogFactory.getLog( LocalFileDaoImpl.class.getName() );

    /**
     * 
     */
    @Override
    public LocalFile find( ubic.gemma.model.common.description.LocalFile localFile ) {
        try {

            BusinessKey.checkValidKey( localFile );

            Criteria queryObject = super.getSession( false ).createCriteria( LocalFile.class );

            queryObject.add( Restrictions.eq( "localURI", localFile.getLocalURI() ) );

            assert localFile != null;
            if ( localFile.getSize() != null && localFile.getSize() != 0 )
                queryObject.add( Restrictions.eq( "size", localFile.getSize() ) );

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

        LocalFile existingLocalFile = find( localFile );
        if ( existingLocalFile != null ) {
            log.debug( "Found existing localFile: " + existingLocalFile.getLocalURI() );
            BeanPropertyCompleter.complete( existingLocalFile, localFile );
            return existingLocalFile;
        }
        log.debug( "Creating new localFile: " + localFile.getLocalURI() );
        return create( localFile );
    }

}