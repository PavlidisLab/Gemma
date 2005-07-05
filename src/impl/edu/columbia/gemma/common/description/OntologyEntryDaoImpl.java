/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.common.description;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @see edu.columbia.gemma.common.description.OntologyEntry
 */
public class OntologyEntryDaoImpl extends edu.columbia.gemma.common.description.OntologyEntryDaoBase {

    @Override
    public OntologyEntry find( OntologyEntry ontologyEntry ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( OntologyEntry.class );

            /* go by accession/database if present, otherwise by category/value */
            if ( ontologyEntry.getAccession() != null && ontologyEntry.getExternalDatabase() != null ) {
                queryObject.add( Restrictions.eq( "accession", ontologyEntry.getAccession() ) ).add(
                        Restrictions.eq( "externalDatabase", ontologyEntry.getExternalDatabase() ) );
            } else {
                queryObject.add( Restrictions.eq( "category", ontologyEntry.getCategory() ) ).add(
                        Restrictions.eq( "value", ontologyEntry.getValue() ) );
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
                    result = ( edu.columbia.gemma.common.description.OntologyEntry ) results.iterator().next();
                }
            }
            return ( OntologyEntry ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    public OntologyEntry findOrCreate( OntologyEntry ontologyEntry ) {
        OntologyEntry newOntologyEntry = find( ontologyEntry );
        if ( newOntologyEntry != null ) return newOntologyEntry;
        return ( OntologyEntry ) create( ontologyEntry );
    }
}