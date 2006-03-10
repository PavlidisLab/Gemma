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
package ubic.gemma.model.genome.biosequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public class BioSequenceDaoImpl extends ubic.gemma.model.genome.biosequence.BioSequenceDaoBase {

    private static Log log = LogFactory.getLog( BioSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public BioSequence find( BioSequence bioSequence ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( BioSequence.class );
            queryObject.add( Restrictions.eq( "name", bioSequence.getName() ) );

            if ( bioSequence.getSequenceDatabaseEntry() != null )
                queryObject.add( Restrictions.eq( "sequenceDatabaseEntry", bioSequence.getSequenceDatabaseEntry() ) );

            if ( bioSequence.getSequence() != null )
                queryObject.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioSequence.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( BioSequence ) results.iterator().next();
                }
            }
            return ( BioSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public BioSequence findOrCreate( BioSequence bioSequence ) {
        if ( bioSequence.getName() == null ) {
            log.debug( "BioSequence must have a name to use as comparison key" );
            return null;
        }
        BioSequence newBioSequence = this.find( bioSequence );
        if ( newBioSequence != null ) {
            BeanPropertyCompleter.complete( newBioSequence, bioSequence );
            return newBioSequence;
        }
        log.debug( "Creating new bioSequence: " + bioSequence.getName() );
        return ( BioSequence ) create( bioSequence );
    }
}