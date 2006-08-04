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

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.DatabaseEntry
 */
public class DatabaseEntryDaoImpl extends ubic.gemma.model.common.description.DatabaseEntryDaoBase {

    @Override
    public DatabaseEntry find( DatabaseEntry databaseEntry ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( DatabaseEntry.class );

            queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) ).createCriteria(
                    "externalDatabase" ).add( Restrictions.eq( "name", databaseEntry.getExternalDatabase().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.description.DatabaseEntry.class.getName()
                                    + "' was found when executing query for " + databaseEntry );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( DatabaseEntry ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public DatabaseEntry findOrCreate( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null
                || databaseEntry.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "No valid business key for " + databaseEntry );
        }
        DatabaseEntry newDatabaseEntry = find( databaseEntry );
        if ( newDatabaseEntry != null ) {
            BeanPropertyCompleter.complete( newDatabaseEntry, databaseEntry );
            return newDatabaseEntry;
        }
        return create( databaseEntry );
    }
}