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
package ubic.gemma.persistence.service.genome.biosequence;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.batchIdentifiableParameterList;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.genome.biosequence.BioSequence
 */
@Repository
public class BioSequenceDaoImpl extends AbstractVoEnabledDao<BioSequence, BioSequenceValueObject>
        implements BioSequenceDao {

    @Autowired
    public BioSequenceDaoImpl( SessionFactory sessionFactory ) {
        super( BioSequence.class, sessionFactory );
    }

    @Override
    public BioSequence findByAccession( DatabaseEntry databaseEntry ) {
        BusinessKey.checkValidKey( databaseEntry );

        String queryString;
        List<BioSequence> results;
        if ( databaseEntry.getId() != null ) {
            queryString = "select b from BioSequence b inner join fetch b.sequenceDatabaseEntry d inner join fetch d.externalDatabase e  where d=:dbe";
            //noinspection unchecked
            results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameter( "dbe", databaseEntry ).list();
        } else {
            queryString = "select b from BioSequence b inner join fetch b.sequenceDatabaseEntry d "
                    + "inner join fetch d.externalDatabase e where d.accession = :acc and e.name = :dbName";
            //noinspection unchecked
            results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameter( "acc", databaseEntry.getAccession() )
                    .setParameter( "dbName", databaseEntry.getExternalDatabase().getName() ).list();
        }

        if ( results.size() > 1 ) {
            this.debug( null, results );
            log.warn( "More than one instance of '" + BioSequence.class.getName()
                    + "' was found when executing query for accession=" + databaseEntry.getAccession() );

            // favor the one with name matching the accession.
            for ( BioSequence object : results ) {
                BioSequence bs = object;
                if ( bs.getName().equals( databaseEntry.getAccession() ) ) {
                    return bs;
                }
            }

            log.error( "No biosequence really matches " + databaseEntry.getAccession() );
            return null;

        } else if ( results.size() == 1 ) {
            return results.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes ) {
        if ( genes == null || genes.isEmpty() )
            return new HashMap<>();
        Map<Gene, Collection<BioSequence>> results = new HashMap<>();
        for ( Collection<Gene> batch : batchIdentifiableParameterList( genes, 500 ) ) {
            //noinspection unchecked
            List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                            "select distinct gene, bs from Gene gene "
                                    + "join fetch gene.products ggp, BioSequence bs "
                                    + "join bs.bioSequence2GeneProduct bs2gp join bs2gp.geneProduct bsgp "
                                    + "where ggp = bsgp and gene in (:genes)" )
                    .setParameterList( "genes", batch ).list();
            for ( Object[] row : qr ) {
                results.computeIfAbsent( ( Gene ) row[0], k -> new HashSet<>() ).add( ( BioSequence ) row[1] );
            }
        }
        return results;
    }

    @Override
    public Collection<BioSequence> findByName( String name ) {
        return this.findByProperty( "name", name );
    }

    @Override
    public Collection<Gene> getGenesByAccession( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gene from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct as bs2gp"
                                + " inner join bs2gp.bioSequence bs "
                                + "inner join bs.sequenceDatabaseEntry de where gp=bs2gp.geneProduct "
                                + " and de.accession = :search " )
                .setParameter( "search", search ).list();
    }

    @Override
    public Collection<Gene> getGenesByName( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gene from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct as bs2gp where gp=bs2gp.geneProduct "
                                + " and bs2gp.bioSequence.name like :search " )
                .setString( "search", search ).list();
    }

    @Override
    public Collection<BioSequence> thaw( final Collection<BioSequence> bioSequences ) {
        if ( bioSequences.isEmpty() )
            return new HashSet<>();

        Collection<BioSequence> result = new HashSet<>();
        for ( Collection<BioSequence> batch : batchIdentifiableParameterList( bioSequences, 100 ) ) {
            //noinspection unchecked
            result.addAll( this.getSessionFactory().getCurrentSession().createQuery( "select b from BioSequence b "
                            + "left join fetch b.taxon tax left join fetch tax.externalDatabase left join fetch b.sequenceDatabaseEntry s "
                            + "left join fetch s.externalDatabase" + " left join fetch b.bioSequence2GeneProduct bs2gp "
                            + "left join fetch bs2gp.geneProduct gp left join fetch gp.gene g "
                            + "left join fetch g.aliases left join fetch g.accessions  where b in (:bs)" )
                    .setParameterList( "bs", batch )
                    .list() );
        }

        return result;
    }

    @Override
    public BioSequence thaw( final BioSequence bioSequence ) {
        if ( bioSequence == null )
            return null;
        if ( bioSequence.getId() == null )
            return bioSequence;

        return ( BioSequence ) getSessionFactory().getCurrentSession().createQuery( "select b from BioSequence b "
                        + " left join fetch b.taxon tax left join fetch tax.externalDatabase "
                        + " left join fetch b.sequenceDatabaseEntry s left join fetch s.externalDatabase"
                        + " left join fetch b.bioSequence2GeneProduct bs2gp "
                        + " left join fetch bs2gp.geneProduct gp left join fetch gp.gene g"
                        + " left join fetch g.aliases left join fetch g.accessions  where b.id=:bid" )
                .setParameter( "bid", bioSequence.getId() )
                .uniqueResult();
    }

    @Override
    public BioSequence findByCompositeSequence( CompositeSequence compositeSequence ) {
        return ( BioSequence ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select cs.biologicalCharacteristic from CompositeSequence as cs where cs = :cs" )
                .setParameter( "cs", compositeSequence ).uniqueResult();
    }

    @Override
    protected BioSequenceValueObject doLoadValueObject( BioSequence entity ) {
        return BioSequenceValueObject.fromEntity( entity );
    }

    @SuppressWarnings("unchecked")
    @Override
    public BioSequence find( BioSequence bioSequence ) {

        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = BusinessKey
                .createQueryObject( this.getSessionFactory().getCurrentSession(), bioSequence );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        /*
         * this initially matches on name and taxon only.
         */
        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                this.debug( bioSequence, results );

                // Try to find the best match. See BusinessKey for more
                // explanation of why this is needed.
                BioSequence match = null;
                for ( BioSequence res : ( Collection<BioSequence> ) results ) {
                    if ( res.equals( bioSequence ) ) {
                        if ( match != null ) {
                            log.warn( "More than one sequence in the database matches " + bioSequence
                                    + ", returning arbitrary match: " + match );
                            break;
                        }
                        match = res;
                    }
                }

                return match;

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( BioSequence ) result;
    }

    private void debug( @Nullable BioSequence query, List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nMultiple BioSequences found matching query:\n" );

        if ( query != null ) {
            sb.append( "\tQuery: ID=" ).append( query.getId() ).append( " Name=" ).append( query.getName() );
            if ( StringUtils.isNotBlank( query.getSequence() ) )
                sb.append( " Sequence=" ).append( StringUtils.abbreviate( query.getSequence(), 10 ) );
            if ( query.getSequenceDatabaseEntry() != null )
                sb.append( " acc=" ).append( query.getSequenceDatabaseEntry().getAccession() );
            sb.append( "\n" );
        }

        for ( Object object : results ) {
            BioSequence entity = ( BioSequence ) object;
            sb.append( "\tMatch: ID=" ).append( entity.getId() ).append( " Name=" ).append( entity.getName() );
            if ( StringUtils.isNotBlank( entity.getSequence() ) )
                sb.append( " Sequence=" ).append( StringUtils.abbreviate( entity.getSequence(), 10 ) );
            if ( entity.getSequenceDatabaseEntry() != null )
                sb.append( " acc=" ).append( entity.getSequenceDatabaseEntry().getAccession() );
            sb.append( "\n" );
        }
        if ( log.isDebugEnabled() )
            log.debug( sb.toString() );
    }
}
