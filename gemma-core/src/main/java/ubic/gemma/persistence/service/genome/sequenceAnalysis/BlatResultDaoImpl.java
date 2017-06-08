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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
@Repository
public class BlatResultDaoImpl extends BlatResultDaoBase {

    @Autowired
    public BlatResultDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = this.getSession().createCriteria( BlatResult.class );

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

    @Override
    public void thaw( BlatResult blatResult ) {
        Hibernate.initialize( blatResult.getQuerySequence() );
        Hibernate.initialize( blatResult.getQuerySequence().getTaxon() );
        Hibernate.initialize( blatResult.getQuerySequence().getTaxon().getParentTaxon() );
        Hibernate.initialize( blatResult.getQuerySequence().getTaxon().getExternalDatabase() );
        Hibernate.initialize( blatResult.getQuerySequence().getSequenceDatabaseEntry() );
        Hibernate.initialize( blatResult.getQuerySequence().getSequenceDatabaseEntry().getExternalDatabase() );
        Hibernate.initialize( blatResult.getTargetSequence() );
        Hibernate.initialize( blatResult.getSearchedDatabase() );
        Hibernate.initialize( blatResult.getTargetChromosome() );
        Hibernate.initialize( blatResult.getTargetChromosome().getTaxon() );
        Hibernate.initialize( blatResult.getTargetChromosome().getSequence() );
    }

    @Override
    public BlatResult find( BlatResult entity ) {
        return load( entity.getId() );
    }

    @Override
    public void thaw( Collection<BlatResult> blatResults ) {
        for ( BlatResult br : blatResults ) {
            thaw( br );
        }
    }

    @Override
    public BlatResultValueObject loadValueObject( BlatResult entity ) {
        return new BlatResultValueObject( entity );
    }

    @Override
    public Collection<BlatResultValueObject> loadValueObjects( Collection<BlatResult> entities ) {
        Collection<BlatResultValueObject> vos = new LinkedHashSet<>();
        for ( BlatResult e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }
}