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

package ubic.gemma.model.expression.designElement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDaoImpl extends ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase {

    private static Log log = LogFactory.getLog( CompositeSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#find(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence find( CompositeSequence compositeSequence ) {

        if ( compositeSequence.getName() == null ) return null;

        try {

            Criteria queryObject = super.getSession( false ).createCriteria( CompositeSequence.class );

            queryObject.add( Restrictions.eq( "name", compositeSequence.getName() ) );

            // TODO make this use the full arraydesign
            // business key.
            queryObject.createCriteria( "arrayDesign" ).add(
                    Restrictions.eq( "name", compositeSequence.getArrayDesign().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + CompositeSequence.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( CompositeSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            throw new IllegalArgumentException( "compositeSequence must have name and arrayDesign." );
        }

        CompositeSequence existingCompositeSequence = this.find( compositeSequence );
        if ( existingCompositeSequence != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing compositeSequence: " + existingCompositeSequence );
            return existingCompositeSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new compositeSequence: " + compositeSequence );
        return ( CompositeSequence ) create( compositeSequence );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from CompositeSequenceImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct compositeSequence from CompositeSequenceImpl compositeSequence where compositeSequence.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    /**
     * FIXME duplicated code from ArrayDesignDao
     * 
     * @param id
     * @param queryString
     * @return
     */
    private Collection nativeQueryByIdReturnCollection( Long id, final String queryString ) {
        try {

            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private Collection nativeQuery( final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @Override
    protected Collection handleGetRawSummary( CompositeSequence compositeSequence, Integer numResults )
            throws Exception {
        if ( compositeSequence == null || compositeSequence.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = compositeSequence.getId();

        String nativeQueryString = nativeBaseSummaryQueryString + " WHERE cs.ID = :id";

        if ( numResults != null && numResults != 0 ) {
            nativeQueryString = nativeQueryString + " LIMIT " + numResults;
        }

        Collection retVal = nativeQueryByIdReturnCollection( id, nativeQueryString );
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(java.util.Collection)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetRawSummary( Collection compositeSequences, Integer numResults ) throws Exception {
        if ( compositeSequences == null || compositeSequences.size() == 0 ) return null;
        StringBuilder buf = new StringBuilder();

        for ( Iterator<CompositeSequence> it = compositeSequences.iterator(); it.hasNext(); ) {
            CompositeSequence compositeSequence = it.next();
            if ( compositeSequence == null || compositeSequence.getId() == null ) {
                throw new IllegalArgumentException();
            }
            long id = compositeSequence.getId();
            buf.append( id );
            if ( it.hasNext() ) buf.append( "," );
        }

        String nativeQueryString = nativeBaseSummaryQueryString + " WHERE cs.ID IN (" + buf.toString() + ")";

        if ( numResults != null && numResults != 0 ) {
            nativeQueryString = nativeQueryString + " LIMIT " + numResults;
        }

        Collection retVal = nativeQuery( nativeQueryString );
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleGetRawSummary( ArrayDesign arrayDesign, Integer numResults ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        String nativeQueryString = nativeBaseSummaryQueryString + " WHERE cs.ARRAY_DESIGN_FK = :id ";
        if ( numResults != null && numResults != 0 ) {
            nativeQueryString = nativeQueryString + " LIMIT " + numResults;
        }
        Collection retVal = nativeQueryByIdReturnCollection( id, nativeQueryString );
        return retVal;
    }

    /*
     * Add your 'where' clause to this.
     */
    private static final String nativeBaseSummaryQueryString = "SELECT de.ID as deID, de.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION, ssr.ID,"
            + "geneProductRNA.ID as gpId,geneProductRNA.NAME as gpName,geneProductRNA.NCBI_ID as gpNcbi, geneProductRNA.GENE_FK, "
            + "geneProductRNA.TYPE, gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_ID as gNcbi "
            + " from "
            + "COMPOSITE_SEQUENCE cs join DESIGN_ELEMENT de on cs.ID=de.ID "
            + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
            + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssr on ssr.QUERY_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
            + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
            + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
            + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
            + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID) ";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByBioSequence( BioSequence bioSequence ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs where cs.biologicalCharacteristic = :id";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", bioSequence );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection findByName( String name ) {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl" + " cs where cs.name = :id";
        try {
            log.info( "Query Name: " + name );
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );          
            queryObject.setString( "id", name );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene ) {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl   gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.geneProduct=gp  and ba.bioSequence=bs and gene = :gene";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl   gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp  and gene = :gene and cs.arrayDesign=:arrayDesign ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            queryObject.setParameter( "arrayDesign", arrayDesign );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleGetGenes( CompositeSequence compositeSequence ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp and cs = :cs";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "cs", compositeSequence );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<CompositeSequence, Collection<Gene>> handleGetGenes( Collection compositeSequences ) throws Exception {
        //
        // final String queryString = "select distinct cs, gene from CompositeSequenceImpl cs, BioSequenceImpl bs,
        // BlatAssociationImpl ba, GeneProductImpl gp, GeneImpl gene "
        // + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp and cs
        // in (:cs)";

        Map<CompositeSequence, Collection<Gene>> returnVal = new HashMap<CompositeSequence, Collection<Gene>>();
        for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
            returnVal.put( cs, new HashSet<Gene>() );
        }

        // build the query for fetching the cs -> gene relation
        final String nativeQuery = "select CS, GENE from GENE2CS WHERE CS IN ";

        StringBuilder buf = new StringBuilder();
        buf.append( nativeQuery );
        buf.append( "(" );
        for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
            buf.append( cs.getId() );
            buf.append( "," );
        }
        buf.setCharAt( buf.length() - 1, ')' );
        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( buf.toString() );
        queryObject.addScalar( "cs", new LongType() );
        queryObject.addScalar( "gene", new LongType() );

        StopWatch watch = new StopWatch();
        log.info( "Beginning query" );
        watch.start();

        List result = queryObject.list();

        log.info( "Done with initial query in " + watch.getTime() + " ms, got " + result.size()
                + " cs-to-gene mappings." );
        watch.reset();
        watch.start();

        int count = 0;
        Collection<Long> genesToFetch = new HashSet<Long>();
        Map<Long, Collection<Long>> cs2geneIds = new HashMap<Long, Collection<Long>>();

        for ( Object object : result ) {
            Object[] ar = ( Object[] ) object;
            Long cs = ( Long ) ar[0];
            Long gene = ( Long ) ar[1];
            if ( !cs2geneIds.containsKey( cs ) ) {
                cs2geneIds.put( cs, new HashSet<Long>() );
            }
            cs2geneIds.get( cs ).add( gene );
            genesToFetch.add( gene );
        }

        session.close();

        // nothing found?
        if ( genesToFetch.size() == 0 ) {
            returnVal.clear();
            return returnVal;
        }

        log.info( "Built cs -> gene map in " + watch.getTime() + " ms; fetching " + genesToFetch.size() + " genes." );
        watch.reset();
        watch.start();

        // fetch the genes
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();
        String geneQuery = "select g from GeneImpl g where g.id in ( :gs )";

        org.hibernate.Query geneQueryObject = super.getSession( false ).createQuery( geneQuery ).setFetchSize( 1000 );
        int BATCH_SIZE = 10000;
        for ( Long gene : genesToFetch ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                geneQueryObject.setParameterList( "gs", batch );
                genes.addAll( geneQueryObject.list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            geneQueryObject.setParameterList( "gs", batch );
            genes.addAll( geneQueryObject.list() );
        }

        log.info( "Got information on " + genes.size() + " genes in " + watch.getTime() + " ms" );

        Map<Long, Gene> geneIdMap = new HashMap<Long, Gene>();
        for ( Gene g : ( Collection<Gene> ) genes ) {
            Long id = g.getId();
            geneIdMap.put( id, g );
        }

        // fill in the return value.
        for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
            Long csId = cs.getId();
            assert csId != null;
            Collection<Long> genesToAttach = cs2geneIds.get( csId );
            if ( genesToAttach == null ) {
                // this means there was no gene for that cs; we should delete it from the result
                returnVal.remove( cs );
                continue;
            }
            for ( Long geneId : genesToAttach ) {
                returnVal.get( cs ).add( geneIdMap.get( geneId ) );
            }
            ++count;
        }

        // Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();

        // // Note that batching it doesn't help performance all that much, if at all. But at least the query parsing
        // stage
        // // is faster ;)
        // int BATCHSIZE = -1;
        // 
        // for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
        //
        // batch.add( cs );
        //
        // if ( BATCHSIZE > 0 && batch.size() == BATCHSIZE ) {
        // count = processCompositeSequenceBatch( queryString, returnVal, batch, count );
        // batch.clear();
        // }
        // }

        // if ( batch.size() > 0 ) {
        // count = processCompositeSequenceBatch( queryString, returnVal, compositeSequences, count );
        // }
        log.info( "Done, " + count + " result rows processed, " + returnVal.size() + "/" + compositeSequences.size()
                + " probes are associated with genes" );
        return returnVal;
    }

    // private int processCompositeSequenceBatch( final String queryString,
    // Map<CompositeSequence, Collection<Gene>> returnVal, Collection<CompositeSequence> batch, int count ) {
    // try {
    // org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
    // queryObject.setParameterList( "cs", batch );
    // List results = queryObject.list();
    //
    // for ( Object[] objects : ( List<Object[]> ) results ) {
    // CompositeSequence c = ( CompositeSequence ) objects[0];
    // Gene g = ( Gene ) objects[1];
    // if ( !returnVal.containsKey( c ) ) {
    // returnVal.put( c, new HashSet<Gene>() );
    // }
    // returnVal.get( c ).add( g );
    // if ( ++count % 2000 == 0 ) {
    // log.info( count + " result rows processed" );
    // }
    // }
    // } catch ( org.hibernate.HibernateException ex ) {
    // throw super.convertHibernateAccessException( ex );
    // }
    // return count;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequenceName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByBioSequenceName( String name ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs inner join cs.biologicalCharacteristic b where b.name = :name";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "name", name );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @Override
    protected void handleThaw( final Collection compositeSequences ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            @SuppressWarnings("unchecked")
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                int i = 0;
                /*
                 * Note this code is copied from ArrayDesignDaoImpl
                 */
                int numToDo = compositeSequences.size();
                for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
                    BioSequence bs = cs.getBiologicalCharacteristic();
                    if ( bs == null ) {
                        continue;
                    }

                    session.update( bs );

                    bs.getTaxon();

                    if ( bs.getBioSequence2GeneProduct() == null ) {
                        continue;
                    }

                    for ( BioSequence2GeneProduct bs2gp : bs.getBioSequence2GeneProduct() ) {
                        if ( bs2gp == null ) {
                            continue;
                        }
                        GeneProduct geneProduct = bs2gp.getGeneProduct();
                        if ( geneProduct != null && geneProduct.getGene() != null ) {
                            Gene g = geneProduct.getGene();
                            g.getAliases().size();
                            session.evict( g );
                            session.evict( geneProduct );
                        }

                    }

                    if ( ++i % 2000 == 0 ) {
                        log.info( "Progress: " + i + "/" + numToDo + "..." );
                        try {
                            Thread.sleep( 10 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }

                    session.update( cs.getArrayDesign() );
                    cs.getArrayDesign().getName();

                    if ( bs.getSequenceDatabaseEntry() != null ) session.evict( bs.getSequenceDatabaseEntry() );
                    session.evict( bs );
                }
                session.clear();
                return null;
            }
        }, true );

    }

}