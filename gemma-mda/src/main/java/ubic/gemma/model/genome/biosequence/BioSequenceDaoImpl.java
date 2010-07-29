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
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.util.BusinessKey;

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

        try {

            Criteria queryObject = BusinessKey.createQueryObject( this.getSession( false ), bioSequence );

            /*
             * this initially matches on name and taxon only.
             */
            java.util.List results = queryObject.list();
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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceDaoBase#findByAccession (ubic.gemma.model.common.description.
     * DatabaseEntry)
     */
    @SuppressWarnings("unchecked")
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
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioSequenceImpl";
        return ( ( Long ) getHibernateTemplate().find( query ).iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleGetGenesByName (java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes ) throws Exception {
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

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BioSequence> handleFindByName( String name ) throws Exception {
        if ( name == null ) return null;
        final String query = "from BioSequenceImpl b where b.name = :name";
        return getHibernateTemplate().findByNamedParam( query, "name", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceDaoBase# handleGetGenesByAccession(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByAccession( String search ) throws Exception {
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl as bs2gp where gp=bs2gp.geneProduct "
                + " and bs2gp.bioSequence.name like :search ";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
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
    protected BioSequence handleThaw( final BioSequence bioSequence ) throws Exception {
        if ( bioSequence == null ) return null;
        if ( bioSequence.getId() == null ) return bioSequence;

        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select b from BioSequenceImpl b "
                                + " left join fetch b.taxon tax left join fetch tax.parentTaxon left join fetch b.sequenceDatabaseEntry s "
                                + " left join fetch s.externalDatabase"
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
    protected void handleThaw( final Collection<BioSequence> bioSequences ) throws Exception {
        doThaw( bioSequences, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(java .util.Collection)
     */
    @Override
    protected void handleThawLite( final Collection<BioSequence> bioSequences ) throws Exception {
        doThaw( bioSequences, false );
    }

    /**
     * @param results
     */
    private void debug( BioSequence query, List<BioSequence> results ) {
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
    private void doThaw( final Collection<BioSequence> bioSequences, final boolean deep ) {
        if ( bioSequences == null || bioSequences.size() == 0 ) return;
        
        HibernateTemplate template = this.getHibernateTemplate();
        Session session = template.getSessionFactory().openSession();
                        
        for ( BioSequence bioSequence : bioSequences ) {
        	session.lock(bioSequence, LockMode.NONE); // re-attach object to session
        	
            bioSequence.getType();
            ExternalDatabase extDB = bioSequence.getTaxon().getExternalDatabase();
            if (extDB != null) { 
            	extDB.getName();
            }
            bioSequence.getTaxon().getParentTaxon();
            
            DatabaseEntry dbEntry = bioSequence.getSequenceDatabaseEntry();
            if ( dbEntry != null ) {
              extDB = dbEntry.getExternalDatabase();
              if (extDB != null) extDB.getName();
            }
            
        }
        session.close();
        
    }                
//        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
//            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
//                FlushMode oldFlushMode = session.getFlushMode();
//                CacheMode oldCacheMode = session.getCacheMode();
//                session.setCacheMode( CacheMode.IGNORE ); // Don't hit the
//                // secondary
//                // cache
//                session.setFlushMode( FlushMode.MANUAL ); // We're
//                // READ-ONLY so
//                // this is okay.
//                int count = 0;
//                long lastTime = 0;
//                for ( BioSequence bioSequence : bioSequences ) {
//                    session.lock( bioSequence, LockMode.NONE );
//                    Hibernate.initialize( bioSequence );
//
//                    if ( deep ) {
//                        bioSequence.getTaxon();
//                        bioSequence.getTaxon().getExternalDatabase();
//                        Hibernate.initialize( bioSequence.getBioSequence2GeneProduct() );
//                        for ( BioSequence2GeneProduct bs2gp : bioSequence.getBioSequence2GeneProduct() ) {
//                            Hibernate.initialize( bs2gp.getGeneProduct() );
//                            if ( bs2gp instanceof BlatAssociation ) {
//                                Hibernate.initialize( ( ( BlatAssociation ) bs2gp ).getBlatResult() );
//                            }
//                        }
//                    }
//
//                    DatabaseEntry dbEntry = bioSequence.getSequenceDatabaseEntry();
//                    if ( dbEntry != null ) {
//                        session.lock( dbEntry, LockMode.NONE );
//                        Hibernate.initialize( dbEntry );
//                        session.lock( dbEntry.getExternalDatabase(), LockMode.NONE );
//                        Hibernate.initialize( dbEntry.getExternalDatabase() );
//                        session.evict( dbEntry );
//                        session.evict( dbEntry.getExternalDatabase() );
//                    }
//
//                    if ( ++count % 2000 == 0 ) {
//                        if ( timer.getTime() - lastTime > 10000 ) {
//                            log.info( "Thawed " + count + " sequences ..." );
//                            lastTime = timer.getTime();
//                        }
//                        session.clear();
//                    }
//                }
//
//                session.clear();
//                session.setFlushMode( oldFlushMode );
//                session.setCacheMode( oldCacheMode );
//
//                return null;
//            }
//        } );
    
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
