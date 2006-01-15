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
package edu.columbia.gemma.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.description.OntologyEntry
 */
public class OntologyEntryDaoImpl extends edu.columbia.gemma.common.description.OntologyEntryDaoBase {

    private static Log log = LogFactory.getLog( OntologyEntryDaoImpl.class.getName() );

    @Override
    public OntologyEntry find( OntologyEntry ontologyEntry ) {

        try {
            Criteria queryObject = super.getSession( false ).createCriteria( OntologyEntry.class );

            if ( ontologyEntry.getAccession() != null && ontologyEntry.getExternalDatabase() != null ) {
                queryObject.add( Restrictions.eq( "accession", ontologyEntry.getAccession() ) ).add(
                        Restrictions.eq( "externalDatabase", ontologyEntry.getExternalDatabase() ) );
            } else {
                queryObject.add( Restrictions.ilike( "category", ontologyEntry.getCategory() ) ).add(
                        Restrictions.ilike( "value", ontologyEntry.getValue() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + edu.columbia.gemma.common.description.OntologyEntry.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( OntologyEntry ) results.iterator().next();
                }
            }
            return ( OntologyEntry ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public OntologyEntry findOrCreate( OntologyEntry ontologyEntry ) {

        if ( ( ontologyEntry.getAccession() == null || ontologyEntry.getExternalDatabase() == null )
                && ( ontologyEntry.getCategory() == null || ontologyEntry.getValue() == null ) ) {
            throw new IllegalArgumentException( "Either accession, or category+value must be filled in." );
        }
        OntologyEntry newOntologyEntry = find( ontologyEntry );
        if ( newOntologyEntry != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing ontologyEntry: "
                        + newOntologyEntry
                        + " externalDatabase="
                        + newOntologyEntry.getExternalDatabase()
                        + ", Database Id="
                        + ( ontologyEntry.getExternalDatabase() == null ? "null" : ontologyEntry.getExternalDatabase()
                                .getId() ) );
            BeanPropertyCompleter.complete( newOntologyEntry, ontologyEntry );
            return newOntologyEntry;
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
}