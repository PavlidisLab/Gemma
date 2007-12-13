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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.DetachedCriteria;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.DatabaseEntry
 */
public class DatabaseEntryDaoImpl extends ubic.gemma.model.common.description.DatabaseEntryDaoBase {

    private static Log log = LogFactory.getLog( DatabaseEntryDaoImpl.class.getName() );

    @Override
    public DatabaseEntry find( DatabaseEntry databaseEntry ) {

        DetachedCriteria queryObject = DetachedCriteria.forClass( DatabaseEntry.class );
        BusinessKey.checkKey( databaseEntry );

        BusinessKey.addRestrictions( queryObject, databaseEntry );

        List results = this.getHibernateTemplate().findByCriteria( queryObject );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                log.error( debug( results ) );

                result = cleanup( databaseEntry );
                if ( result == null ) { // means there were no composite sequences involved.
                    result = results.iterator().next();
                }

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( DatabaseEntry ) result;

    }

    /**
     * This is a hack to fix a problem that is still lingering in the persisting of some genbank identifiers.
     * 
     * @param databaseEntry
     * @return
     */
    private DatabaseEntry cleanup( DatabaseEntry databaseEntry ) {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join fetch cs.biologicalCharacteristic as bs "
                + "inner join fetch bs.sequenceDatabaseEntry de inner join de.externalDatabase ed "
                + "where ed = :expdb and de.accession = :accession";
        /*
         * select c.*,b.name,d.* from COMPOSITE_SEQUENCE as c INNER JOIN BIO_SEQUENCE as b ON
         * c.BIOLOGICAL_CHARACTERISTIC_FK=b.ID inner join DATABASE_ENTRY as d on b.SEQUENCE_DATABASE_ENTRY_FK=d.id where
         * d.ACCESSION="U46691";
         */
        org.hibernate.Query queryObject = getSession( false ).createQuery( queryString );
        queryObject.setParameter( "expdb", databaseEntry.getExternalDatabase() );
        queryObject.setParameter( "accession", databaseEntry.getAccession() );
        List compositeSequences = queryObject.list();

        if ( compositeSequences.size() == 0 ) {
            // we could delete either of them...
            log.warn( "No composite sequences are associated with " + databaseEntry );
            return null;
        } else if ( compositeSequences.size() > 1 ) {
            for ( Object object : compositeSequences ) {
                log.info( object );
            }
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException( compositeSequences.size()
                    + " composite sequences associated with multiple database entries for the same accession: "
                    + databaseEntry );
        } else {
            Object o = compositeSequences.iterator().next();
            assert o instanceof CompositeSequence : "Expected CompositeSequence, got a " + o.getClass().getName();
            DatabaseEntry sequenceDatabaseEntry = ( ( CompositeSequence ) o ).getBiologicalCharacteristic()
                    .getSequenceDatabaseEntry();
            // return the one that is being used
            log.warn( "Returning " + sequenceDatabaseEntry );
            return sequenceDatabaseEntry;
        }

    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from DatabaseEntryImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private String debug( List results ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Object object : results ) {
            DatabaseEntry de = ( DatabaseEntry ) object;
            buf.append( de + "\n" );
        }
        return buf.toString();
    }

    @Override
    public DatabaseEntry findOrCreate( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null
                || databaseEntry.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "No valid business key for " + databaseEntry );
        }
        DatabaseEntry newDatabaseEntry = find( databaseEntry );
        if ( newDatabaseEntry != null ) {
            return newDatabaseEntry;
        }
        return create( databaseEntry );
    }
}