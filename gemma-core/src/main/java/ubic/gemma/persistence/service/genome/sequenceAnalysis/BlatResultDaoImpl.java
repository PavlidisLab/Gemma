/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.List;

import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.sequenceAnalysis.BlatResult</code>.
 * </p>
 *
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
@Repository
public class BlatResultDaoImpl extends AbstractVoEnabledDao<BlatResult, BlatResultValueObject>
        implements BlatResultDao {

    @Autowired
    public BlatResultDaoImpl( SessionFactory sessionFactory ) {
        super( BlatResult.class, sessionFactory );
    }

    @Override
    public BlatResult thaw( BlatResult blatResult ) {
        if ( blatResult.getId() == null )
            return blatResult;
        return ( BlatResult ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BlatResult b left join fetch b.querySequence qs left join fetch b.targetSequence ts  "
                                + " left join fetch b.searchedDatabase left join fetch b.targetChromosome tc left join fetch tc.taxon left join fetch tc.sequence"
                                + " left join fetch qs.taxon t "
                                + " left join fetch t.externalDatabase left join fetch qs.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase" + " where b.id = :id" )
                .setParameter( "id", blatResult.getId() )
                .uniqueResult();
    }

    @Override
    public Collection<BlatResult> thaw( Collection<BlatResult> blatResults ) {
        if ( blatResults.isEmpty() )
            return blatResults;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct b from BlatResult b left join fetch b.querySequence qs left join fetch b.targetSequence ts  "
                                + " left join fetch b.searchedDatabase left join fetch b.targetChromosome tc left join tc.taxon left join fetch tc.sequence"
                                + " left join fetch qs.taxon t "
                                + " left join fetch t.externalDatabase left join fetch qs.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase"
                                + " where b in :blatResults" )
                .setParameterList( "blatResults", optimizeIdentifiableParameterList( blatResults ) )
                .list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( BlatResult.class );

        BusinessKey.attachCriteria( queryObject, bioSequence, "querySequence" );

        List<?> results = queryObject.list();

        for ( Object object : results ) {
            BlatResult br = ( BlatResult ) object;
            if ( br.getTargetChromosome() != null ) {
                Hibernate.initialize( br.getTargetChromosome() );
            }
            Hibernate.initialize( br.getQuerySequence() );
        }

        return ( Collection<BlatResult> ) results;
    }

    @Override
    protected BlatResultValueObject doLoadValueObject( BlatResult entity ) {
        return new BlatResultValueObject( entity );
    }

}