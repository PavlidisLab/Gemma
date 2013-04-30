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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
@Repository
public class BioSequenceDaoImpl extends ubic.gemma.model.genome.biosequence.BioSequenceDaoBase {

    private static Log log = LogFactory.getLog( BioSequenceDaoImpl.class.getName() );

    @Autowired
    public BioSequenceDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#find(ubic.gemma
     * .model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public BioSequence find( BioSequence bioSequence ) {

        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = BusinessKey.createQueryObject( getSessionFactory().getCurrentSession(), bioSequence );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        /*
         * this initially matches on name and taxon only.
         */
        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                debug( bioSequence, results );

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

    /*
     * (non-Javadoc)
     * 
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceDaoBase#findByAccession (ubic.gemma.model.common.description.
     * DatabaseEntry)
     */
    @Override
    public BioSequence findByAccession( DatabaseEntry databaseEntry ) {
        BusinessKey.checkValidKey( databaseEntry );

        String queryString = "";
        List<BioSequence> results = null;
        if ( databaseEntry.getId() != null ) {
            queryString = "select b from BioSequenceImpl b inner join fetch b.sequenceDatabaseEntry d inner join fetch d.externalDatabase e  where d=:dbe";
            results = this.getHibernateTemplate().findByNamedParam( queryString, "dbe", databaseEntry );
        } else {
            queryString = "select b from BioSequenceImpl b inner join fetch b.sequenceDatabaseEntry d "
                    + "inner join fetch d.externalDatabase e where d.accession = :acc and e.name = :dbname";
            results = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "acc", "dbname" },
                    new Object[] { databaseEntry.getAccession(), databaseEntry.getExternalDatabase().getName() } );
        }

        if ( results.size() > 1 ) {
            debug( null, results );
            log.warn( "More than one instance of '" + BioSequence.class.getName()
                    + "' was found when executing query for accession=" + databaseEntry.getAccession() );

            // favor the one with name matching the accession.
            for ( Object object : results ) {
                BioSequence bs = ( BioSequence ) object;
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#findOrCreate(ubic
     * .gemma.model.genome.biosequence.BioSequence )
     */
    @Override
    public BioSequence findOrCreate( BioSequence bioSequence ) {
        BioSequence existingBioSequence = this.find( bioSequence );
        if ( existingBioSequence != null ) {
            return existingBioSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new: " + bioSequence );
        return create( bioSequence );
    }

    @Override
    protected Integer handleCountAll() {
        final String query = "select count(*) from BioSequenceImpl";
        return ( ( Long ) getHibernateTemplate().find( query ).iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleGetGenesByName (java.lang.String)
     */
    @Override
    protected Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes ) {
        if ( genes == null || genes.isEmpty() ) return new HashMap<Gene, Collection<BioSequence>>();

        Map<Gene, Collection<BioSequence>> results = new HashMap<Gene, Collection<BioSequence>>();

        int batchsize = 500;

        if ( genes.size() <= batchsize ) {
            findByGenesBatch( genes, results );
            return results;
        }

        Collection<Gene> batch = new HashSet<Gene>();

        for ( Gene gene : genes ) {
            batch.add( gene );
            if ( batch.size() == batchsize ) {
                findByGenesBatch( genes, results );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            findByGenesBatch( genes, results );
        }

        return results;
    }

    @Override
    protected Collection<BioSequence> handleFindByName( String name ) {
        if ( name == null ) return null;
        final String query = "from BioSequenceImpl b where b.name = :name";
        return getHibernateTemplate().findByNamedParam( query, "name", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceDaoBase# handleGetGenesByAccession(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByAccession( String search ) {
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl as bs2gp"
                + " inner join bs2gp.bioSequence bs "
                + "inner join bs.sequenceDatabaseEntry de where gp=bs2gp.geneProduct " + " and de.accession = :search ";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleGetGenesByName (java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl as bs2gp where gp=bs2gp.geneProduct "
                + " and bs2gp.bioSequence.name like :search ";
        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleLoad(java .util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleLoad( Collection<Long> ids ) {
        final String queryString = "select distinct bs from BioSequenceImpl bs where bs.id in (:ids)";
        return getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(ubic
     * .gemma.model.genome.biosequence.BioSequence )
     */
    @Override
    protected BioSequence handleThaw( final BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;
        if ( bioSequence.getId() == null ) return bioSequence;

        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select b from BioSequenceImpl b "
                                + " left join fetch b.taxon tax left join fetch tax.externalDatabase left join fetch tax.parentTaxon pt "
                                + " left join fetch pt.externalDatabase "
                                + " left join fetch b.sequenceDatabaseEntry s left join fetch s.externalDatabase"
                                + " left join fetch b.bioSequence2GeneProduct bs2gp "
                                + " left join fetch bs2gp.geneProduct gp left join fetch gp.gene g"
                                + " left join fetch g.aliases left join fetch g.accessions  where b.id=:bid", "bid",
                        bioSequence.getId() );

        BioSequence thawedBioSequence = ( BioSequence ) res.iterator().next();

        return thawedBioSequence;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(java .util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleThaw( final Collection<BioSequence> bioSequences ) {
        return doThaw( bioSequences );
    }

    /**
     * @param results
     */
    private void debug( BioSequence query, List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nMultiple BioSequences found matching query:\n" );

        if ( query != null ) {
            sb.append( "\tQuery: ID=" + query.getId() + " Name=" + query.getName() );
            if ( StringUtils.isNotBlank( query.getSequence() ) )
                sb.append( " Sequence=" + StringUtils.abbreviate( query.getSequence(), 10 ) );
            if ( query.getSequenceDatabaseEntry() != null )
                sb.append( " acc=" + query.getSequenceDatabaseEntry().getAccession() );
            sb.append( "\n" );
        }

        for ( Object object : results ) {
            BioSequence entity = ( BioSequence ) object;
            sb.append( "\tMatch: ID=" + entity.getId() + " Name=" + entity.getName() );
            if ( StringUtils.isNotBlank( entity.getSequence() ) )
                sb.append( " Sequence=" + StringUtils.abbreviate( entity.getSequence(), 10 ) );
            if ( entity.getSequenceDatabaseEntry() != null )
                sb.append( " acc=" + entity.getSequenceDatabaseEntry().getAccession() );
            sb.append( "\n" );
        }
        if ( log.isDebugEnabled() ) log.debug( sb.toString() );
    }

    /**
     * @param bioSequences
     * @param deep
     */
    private Collection<BioSequence> doThaw( final Collection<BioSequence> bioSequences ) {

        if ( bioSequences.isEmpty() ) return new HashSet<BioSequence>();

        Collection<BioSequence> result = new HashSet<BioSequence>();
        Collection<BioSequence> batch = new HashSet<BioSequence>();

        for ( BioSequence g : bioSequences ) {
            batch.add( g );
            if ( batch.size() == 100 ) {
                result.addAll( doThawBatch( batch ) );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            result.addAll( doThawBatch( batch ) );
        }

        return result;

    }

    /**
     * @param batch
     * @return
     */
    private Collection<? extends BioSequence> doThawBatch( Collection<BioSequence> batch ) {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select b from BioSequenceImpl b "
                                + " left join fetch b.taxon tax left join fetch tax.externalDatabase left join fetch tax.parentTaxon left join fetch b.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase"
                                + " left join fetch b.bioSequence2GeneProduct bs2gp "
                                + " left join fetch bs2gp.geneProduct gp left join fetch gp.gene g"
                                + " left join fetch g.aliases left join fetch g.accessions  where b.id in (:bids)",
                        "bids", EntityUtils.getIds( batch ) );
    }

    /**
     * @param genes
     * @param results
     */
    private void findByGenesBatch( Collection<Gene> genes, Map<Gene, Collection<BioSequence>> results ) {
        final String queryString = "select distinct gene,bs from GeneImpl gene inner join fetch gene.products ggp,"
                + " BioSequenceImpl bs inner join bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct bsgp"
                + " where ggp=bsgp and gene in (:genes)";

        List<Object[]> qr = getHibernateTemplate().findByNamedParam( queryString, "genes", genes );
        for ( Object[] oa : qr ) {
            Gene g = ( Gene ) oa[0];
            BioSequence b = ( BioSequence ) oa[1];
            if ( !results.containsKey( g ) ) {
                results.put( g, new HashSet<BioSequence>() );
            }
            results.get( g ).add( b );
        }
    }
}
