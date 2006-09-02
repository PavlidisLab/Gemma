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

    @Override
    public OntologyEntry find( OntologyEntry ontologyEntry ) {

        BusinessKey.checkKey( ontologyEntry );

        try {
            Criteria queryObject = BusinessKey.createQueryObject( super.getSession( false ), ontologyEntry );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
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

        OntologyEntry existingOntologyEntry = find( ontologyEntry );
        if ( existingOntologyEntry != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing ontologyEntry: "
                        + existingOntologyEntry
                        + " externalDatabase="
                        + existingOntologyEntry.getExternalDatabase()
                        + ", Database Id="
                        + ( ontologyEntry.getExternalDatabase() == null ? "null" : ontologyEntry.getExternalDatabase()
                                .getId() ) );
            return existingOntologyEntry;
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