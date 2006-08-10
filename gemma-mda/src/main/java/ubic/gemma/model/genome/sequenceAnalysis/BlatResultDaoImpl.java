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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
public class BlatResultDaoImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> find( BioSequence bioSequence ) {
        if ( bioSequence == null
                || ( StringUtils.isBlank( bioSequence.getName() ) && StringUtils.isBlank( bioSequence.getSequence() ) && bioSequence
                        .getSequenceDatabaseEntry() == null ) ) {
            throw new IllegalArgumentException(
                    "BioSequence must have a name, sequence, and/or accession to use as comparison key" );
        }

        Criteria queryObject = super.getSession( false ).createCriteria( BlatResult.class );
        Criteria innerQuery = queryObject.createCriteria( "bioSequence" );

        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            innerQuery.add( Restrictions.eq( "name", bioSequence.getName() ) );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null ) {
            innerQuery.createCriteria( "sequenceDatabaseEntry" ).add(
                    Restrictions.eq( "accession", bioSequence.getSequenceDatabaseEntry().getAccession() ) );
        }

        if ( StringUtils.isNotBlank( bioSequence.getSequence() ) ) {
            innerQuery.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );
        }

        return queryObject.list();
    }
}