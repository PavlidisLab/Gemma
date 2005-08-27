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

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.description.DatabaseEntry
 */
public class DatabaseEntryDaoImpl extends edu.columbia.gemma.common.description.DatabaseEntryDaoBase {

    @Override
    public DatabaseEntry find( DatabaseEntry databaseEntry ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( DatabaseEntry.class );
            queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) ).add(
                    Restrictions.eq( "externalDatabase", databaseEntry.getExternalDatabase() ) );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + edu.columbia.gemma.common.description.DatabaseEntry.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.description.DatabaseEntry ) results.iterator().next();
                }
            }
            return ( DatabaseEntry ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public DatabaseEntry findOrCreate( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null ) return null;
        DatabaseEntry newDatabaseEntry = find( databaseEntry );
        if ( newDatabaseEntry != null ) {
            BeanPropertyCompleter.complete( newDatabaseEntry, databaseEntry );
            return newDatabaseEntry;
        }
        return create( databaseEntry );
    }
}