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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
@Repository
public class BlatResultDaoImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase {

    @Autowired
    public BlatResultDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = super.getSession().createCriteria( BlatResult.class );

        BusinessKey.attachCriteria( queryObject, bioSequence, "querySequence" );

        List results = queryObject.list();

        if ( results != null ) {
            for ( Object object : results ) {
                BlatResult br = ( BlatResult ) object;
                if ( br.getTargetChromosome() != null ) {
                    br.getTargetChromosome().getName(); // to initialize the proxies.
                }
                br.getQuerySequence();
            }
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#findOrCreate(ubic.gemma.model.genome.sequenceAnalysis
     * .BlatResult)
     */
    @Override
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        if ( blatResult.getQuerySequence() == null )
            throw new IllegalArgumentException( "BlatResult must have a querySequence associated with it." );

        BlatResult result = this.find( blatResult );
        if ( result != null ) return result;

        logger.debug( "Creating new BlatResult: " + blatResult.toString() );
        result = create( blatResult );
        return result;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        final String queryString = "select distinct blatResult from BlatResultImpl blatResult where blatResult.id in (:ids)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
    }

}