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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

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
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( BlatResult.class );

        BusinessKey.attachCriteria( queryObject, bioSequence, "querySequence" );

        List<?> results = queryObject.list();

        if ( results != null ) {
            for ( Object object : results ) {
                BlatResult br = ( BlatResult ) object;
                if ( br.getTargetChromosome() != null ) {
                    Hibernate.initialize( br.getTargetChromosome() );
                }
                Hibernate.initialize( br.getQuerySequence() );
            }
        }

        return ( Collection<BlatResult> ) results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public BlatResult thaw( BlatResult blatResult ) {
        if ( blatResult.getId() == null ) return blatResult;
        return ( BlatResult ) this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select b from BlatResultImpl b left join fetch b.querySequence qs left join fetch b.targetSequence ts  "
                                + " left join fetch b.searchedDatabase left join fetch b.targetChromosome tc left join fetch tc.taxon left join fetch tc.sequence"
                                + " left join fetch qs.taxon t left join t.parentTaxon "
                                + " left join fetch t.externalDatabase left join fetch qs.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase" + " where b.id = :id", "id", blatResult.getId() )
                .iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao#thaw(java.util.Collection)
     */
    @Override
    public Collection<BlatResult> thaw( Collection<BlatResult> blatResults ) {
        if ( blatResults.isEmpty() ) return blatResults;
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct b from BlatResultImpl b left join fetch b.querySequence qs left join fetch b.targetSequence ts  "
                                + " left join fetch b.searchedDatabase left join fetch b.targetChromosome tc left join tc.taxon left join fetch tc.sequence"
                                + " left join fetch qs.taxon t "
                                + " left join fetch t.externalDatabase left join fetch qs.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase" + " where b.id in ( :ids)", "ids",
                        EntityUtils.getIds( blatResults ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection<BlatResult> handleLoad( Collection<Long> ids ) throws Exception {
        final String queryString = "select distinct blatResult from BlatResultImpl blatResult where blatResult.id in (:ids)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
    }

}