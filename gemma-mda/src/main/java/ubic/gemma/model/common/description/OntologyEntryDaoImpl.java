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
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.OntologyEntry
 */
public class OntologyEntryDaoImpl extends ubic.gemma.model.common.description.OntologyEntryDaoBase {

    private static Log log = LogFactory.getLog( OntologyEntryDaoImpl.class.getName() );

    /**
     * @param results
     */
    private void debug( List results ) {
        log.info( "Multiple found:" );
        for ( Object object : results ) {
            log.info( object );
        }

    }

    @Override
    public OntologyEntry find( OntologyEntry ontologyEntry ) {

        BusinessKey.checkKey( ontologyEntry );

        try {
            Criteria queryObject = BusinessKey.createQueryObject( super.getSession( false ), ontologyEntry );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.description.OntologyEntry.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( OntologyEntry ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public OntologyEntry findOrCreate( OntologyEntry ontologyEntry ) {

        OntologyEntry existing = find( ontologyEntry );
        if ( existing != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing ontologyEntry: "
                        + existing
                        + " externalDatabase="
                        + existing.getExternalDatabase()
                        + ", Database Id="
                        + ( ontologyEntry.getExternalDatabase() == null ? "null" : ontologyEntry.getExternalDatabase()
                                .getId() ) );
            return existing;
        }
        if ( log.isDebugEnabled() )
            log.debug( "Creating new ontologyEntry: "
                    + ontologyEntry
                    + " externalDatabase="
                    + ontologyEntry.getExternalDatabase()
                    + ", Database Id="
                    + ( ontologyEntry.getExternalDatabase() == null ? "null" : ontologyEntry.getExternalDatabase()
                            .getId() ) );
        return ( OntologyEntry ) create( ontologyEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.OntologyEntry#getChildren()
     */
    @Override
    public java.util.Collection<OntologyEntry> handleGetChildren( final OntologyEntry ontologyEntry ) {
        final Collection<OntologyEntry> children = new HashSet<OntologyEntry>();
        if ( ontologyEntry.getId() == null ) {
            this.getChildren( ontologyEntry, children );
        } else {
            this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
                public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                    OntologyEntry innerOntologyEntry = ( OntologyEntry ) session.merge( ontologyEntry );
                    getChildren( innerOntologyEntry, children );
                    return null;
                }
            }, true );
        }
        return children;
    }

    /**
     * Used internally only.
     * 
     * @param start
     * @param addTo
     * @return
     */
    private void getChildren( OntologyEntry start, Collection<OntologyEntry> addTo ) {
        for ( OntologyEntry oe : start.getAssociations() ) {
            addTo.add( oe );
            if ( log.isDebugEnabled() ) log.debug( "Adding " + oe );
            getChildren( oe, addTo );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<OntologyEntry> handleGetParents( OntologyEntry ontologyEntry ) {
        if ( ontologyEntry.getId() == null ) {
            throw new IllegalArgumentException( "Cannot be run on a transient ontologyEntry" );
        }
        String queryString = "from OntologyEntryImpl parent where :oe in elements(parent.associations)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "oe", ontologyEntry );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }
}