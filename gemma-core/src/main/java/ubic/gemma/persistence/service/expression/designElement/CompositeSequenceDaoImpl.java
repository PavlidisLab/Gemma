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

package ubic.gemma.persistence.service.expression.designElement;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilterQueryUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.text.MessageFormat;
import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class CompositeSequenceDaoImpl extends AbstractQueryFilteringVoEnabledDao<CompositeSequence, CompositeSequenceValueObject>
        implements CompositeSequenceDao {

    private static final int PROBE_TO_GENE_MAP_BATCH_SIZE = 2000;
    /**
     * Absolute maximum number of records to return when fetching raw summaries. This is necessary to avoid retrieving
     * millions of records (some sequences are repeats and can have >200,000 records.
     */
    private static final int MAX_CS_RECORDS = 10000;
    /**
     * Add your 'where' clause to this.
     */
    //language=MySQL
    private static final String nativeBaseSummaryQueryString = "SELECT cs.ID as deID, cs.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION as bsdbacc, ssr.ID as ssrid,"
            + "geneProductRNA.ID as gpId, geneProductRNA.NAME as gpName, geneProductRNA.NCBI_GI as gpNcbi, geneProductRNA.GENE_FK as geneid, "
            + "gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_GENE_ID as gNcbi, ad.SHORT_NAME as adShortName, ad.ID as adId, cs.DESCRIPTION as deDesc, "
            + " ssr.TARGET_CHROMOSOME_FK as chrom, ssr.TARGET_START as tgst, ssr.TARGET_END as tgend, ssr.TARGET_STARTS as tgstarts, ssr.QUERY_SEQUENCE_FK as bsId, ad.NAME as adName "
            + " from " + "COMPOSITE_SEQUENCE cs "
            + "left join BIO_SEQUENCE bs on cs.BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
            + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssr on ssr.QUERY_SEQUENCE_FK=cs.BIOLOGICAL_CHARACTERISTIC_FK "
            + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
            + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
            + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
            + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID)"
            + " left join ARRAY_DESIGN ad on (cs.ARRAY_DESIGN_FK=ad.ID) ";
    /**
     * Add your 'where' clause to this. returns much less stuff.
     */
    //language=MySQL
    private static final String nativeBaseSummaryShorterQueryString = "SELECT cs.ID AS deID, cs.NAME AS deName, bs.NAME AS bsName, bsDb.ACCESSION AS bsdbacc, ssr.ID AS ssrid,"
            + " gene.ID AS gId,gene.OFFICIAL_SYMBOL AS gSymbol FROM COMPOSITE_SEQUENCE cs "
            + "LEFT JOIN BIO_SEQUENCE bs ON BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
            + "LEFT JOIN SEQUENCE_SIMILARITY_SEARCH_RESULT ssr ON ssr.QUERY_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
            + "LEFT JOIN BIO_SEQUENCE2_GENE_PRODUCT bs2gp ON BIO_SEQUENCE_FK=bs.ID "
            + "LEFT JOIN DATABASE_ENTRY bsDb ON SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
            + "LEFT JOIN CHROMOSOME_FEATURE geneProductRNA ON (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
            + "LEFT JOIN CHROMOSOME_FEATURE gene ON (geneProductRNA.GENE_FK=gene.ID)"
            + " LEFT JOIN ARRAY_DESIGN ad ON (cs.ARRAY_DESIGN_FK=ad.ID) ";

    @Autowired
    public CompositeSequenceDaoImpl( SessionFactory sessionFactory ) {
        super( CompositeSequenceDao.OBJECT_ALIAS, CompositeSequence.class, sessionFactory );
    }

    @Override
    public CompositeSequenceValueObject loadValueObject( CompositeSequence entity ) {
        return new CompositeSequenceValueObject( entity );
    }

    @Override
    protected Query getLoadValueObjectsQuery( Filters filters, Sort sort, EnumSet<QueryHint> hints ) {
        //language=HQL
        String queryString = MessageFormat.format( "select {0} from CompositeSequence as {0} "
                        + "left join fetch {0}.arrayDesign as {1} "
                        + "where {0}.id is not null ", // needed to use formRestrictionCause()
                getObjectAlias(), ArrayDesignDao.OBJECT_ALIAS );

        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        if ( sort != null ) {
            queryString += ObjectFilterQueryUtils.formOrderByClause( sort );
        }

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected Query getCountValueObjectsQuery( Filters filters ) {
        //language=HQL
        String queryString = MessageFormat.format( "select count(distinct {0}) "
                        + "from CompositeSequence as {0} "
                        + "left join {0}.arrayDesign as " + ArrayDesignDao.OBJECT_ALIAS + " "
                        + "where {0}.id is not null ", // needed to use formRestrictionCause()
                getObjectAlias(), ArrayDesignDao.OBJECT_ALIAS );

        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    public Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence ) {
        //language=HQL
        final String queryString = "select distinct cs from CompositeSequence cs where cs.biologicalCharacteristic = :id";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "id", bioSequence )
                .list();
    }

    @Override
    public Collection<CompositeSequence> findByBioSequenceName( String name ) {
        //language=HQL
        final String queryString = "select distinct cs from CompositeSequence"
                + " cs inner join cs.biologicalCharacteristic b where b.name = :name";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "name", name )
                .list();
    }

    @Override
    public Collection<CompositeSequence> findByGene( Gene gene ) {
        //language=HQL
        final String queryString = "select distinct cs from CompositeSequence cs, BioSequence bs, BioSequence2GeneProduct ba, GeneProduct gp, Gene gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.geneProduct=gp and ba.bioSequence=bs and gene = :gene";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "gene", gene )
                .list();
    }

    @Override
    public Slice<CompositeSequence> findByGene( Gene gene, int start, int limit ) {
        //language=HQL
        final String queryString = "select distinct cs from CompositeSequence cs, BioSequence bs, BioSequence2GeneProduct ba, GeneProduct gp, Gene gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.geneProduct=gp  and ba.bioSequence=bs and gene = :gene";
        //noinspection unchecked
        List<CompositeSequence> list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setFirstResult( start )
                .setMaxResults( limit ).setParameter( "gene", gene ).list();
        return new Slice<>( list, null, start, limit, null );
    }

    @Override
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select distinct cs from CompositeSequence cs, BioSequence bs, BioSequence2GeneProduct ba, GeneProduct gp, Gene gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp  and gene = :gene and cs.arrayDesign=:arrayDesign ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "gene", gene )
                .setParameter( "arrayDesign", arrayDesign ).list();
    }

    @Override
    public Collection<CompositeSequence> findByName( final String name ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct cs from CompositeSequence" + " cs where cs.name = :name" )
                .setParameter( "name", name ).list();
    }

    @Override
    public CompositeSequence findByName( ArrayDesign arrayDesign, final String name ) {
        List<?> results = this.getSessionFactory().getCurrentSession().createQuery(
                        "from CompositeSequence as compositeSequence where compositeSequence.arrayDesign = :arrayDesign and compositeSequence.name = :name" )
                .setParameter( "arrayDesign", arrayDesign ).setParameter( "name", name ).list();

        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'CompositeSequence" + "' was found for name '" + name
                            + "' and array design '" + arrayDesign.getId() + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return ( CompositeSequence ) result;
    }

    @Override
    public Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> compositeSequences ) {
        Map<CompositeSequence, Collection<Gene>> returnVal = new HashMap<>();

        int BATCH_SIZE = 2000;

        if ( compositeSequences.size() == 0 )
            return returnVal;

        /*
         * Get the cs->gene mapping
         */
        final String nativeQuery = "SELECT CS, GENE FROM GENE2CS WHERE CS IN (:csids) ";

        for ( CompositeSequence cs : compositeSequences ) {
            returnVal.put( cs, new HashSet<Gene>() );
        }

        List<Object> csGene = new ArrayList<>();
        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( nativeQuery );
        queryObject.addScalar( "cs", new LongType() );
        queryObject.addScalar( "gene", new LongType() );

        Collection<Long> csIdBatch = new HashSet<>();
        for ( CompositeSequence cs : compositeSequences ) {
            csIdBatch.add( cs.getId() );

            if ( csIdBatch.size() == BATCH_SIZE ) {
                queryObject.setParameterList( "csids", csIdBatch );
                csGene.addAll( queryObject.list() );
                session.clear();
                csIdBatch.clear();
            }
        }

        if ( csIdBatch.size() > 0 ) {
            queryObject.setParameterList( "csids", csIdBatch );
            csGene.addAll( queryObject.list() );
            session.clear();
        }

        StopWatch watch = new StopWatch();
        watch.start();

        int count = 0;
        Collection<Long> genesToFetch = new HashSet<>();
        Map<Long, Collection<Long>> cs2geneIds = new HashMap<>();

        for ( Object object : csGene ) {
            Object[] ar = ( Object[] ) object;
            Long cs = ( Long ) ar[0];
            Long gene = ( Long ) ar[1];
            if ( !cs2geneIds.containsKey( cs ) ) {
                cs2geneIds.put( cs, new HashSet<Long>() );
            }
            cs2geneIds.get( cs ).add( gene );
            genesToFetch.add( gene );
        }

        // nothing found?
        if ( genesToFetch.size() == 0 ) {
            returnVal.clear();
            return returnVal;
        }

        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "Built cs -> gene map in " + watch.getTime() + " ms; fetching " + genesToFetch.size()
                    + " genes." );

        // fetch the genes
        Collection<Long> batch = new HashSet<>();
        Collection<Gene> genes = new HashSet<>();
        String geneQuery = "from Gene g where g.id in ( :gs )";

        org.hibernate.Query geneQueryObject = this.getSessionFactory().getCurrentSession().createQuery( geneQuery )
                .setFetchSize( 1000 );

        for ( Long gene : genesToFetch ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                AbstractDao.log.debug( "Processing batch ... " );
                geneQueryObject.setParameterList( "gs", batch );
                //noinspection unchecked
                genes.addAll( geneQueryObject.list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            geneQueryObject.setParameterList( "gs", batch );
            //noinspection unchecked
            genes.addAll( geneQueryObject.list() );
        }

        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "Got information on " + genes.size() + " genes in " + watch.getTime() + " ms" );

        Map<Long, Gene> geneIdMap = new HashMap<>();
        for ( Gene g : genes ) {
            Hibernate.initialize( g );
            Long id = g.getId();
            geneIdMap.put( id, g );
        }

        // fill in the return value.
        for ( CompositeSequence cs : compositeSequences ) {
            Long csId = cs.getId();
            assert csId != null;
            Collection<Long> genesToAttach = cs2geneIds.get( csId );
            if ( genesToAttach == null ) {
                // this means there was no gene for that cs; we should remove it from the result
                returnVal.remove( cs );
                continue;
            }
            for ( Long geneId : genesToAttach ) {
                returnVal.get( cs ).add( geneIdMap.get( geneId ) );
            }
            ++count;
        }

        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log
                    .debug( "Done, " + count + " result rows processed, " + returnVal.size() + "/" + compositeSequences
                            .size() + " probes are associated with genes" );
        return returnVal;
    }

    @Override
    public Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit ) {
        // gets all kinds of associations, not just blat.
        //language=HQL
        final String queryString = "select distinct gene from CompositeSequence cs, BioSequence bs, BioSequence2GeneProduct ba, "
                + "GeneProduct gp, Gene gene  " + "where gp.gene=gene and cs.biologicalCharacteristic=bs "
                + "and ba.bioSequence=bs and ba.geneProduct=gp and cs = :cs";
        //noinspection unchecked
        List<Gene> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "cs", compositeSequence ).setFirstResult( offset )
                .setMaxResults( limit > 0 ? limit : -1 ).list();
        return new Slice<>( list, null, offset, limit, null );

    }

    @Override
    public Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences ) {

        AbstractDao.log.info( "Getting cs -> alignment specificity map for " + compositeSequences.size()
                + " composite sequences" );
        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> results = new HashMap<>();

        BatchIterator<CompositeSequence> it = BatchIterator
                .batches( compositeSequences, CompositeSequenceDaoImpl.PROBE_TO_GENE_MAP_BATCH_SIZE );

        StopWatch timer = new StopWatch();
        timer.start();
        int total = 0;
        for ( ; it.hasNext(); ) {
            Collection<CompositeSequence> batch = it.next();
            this.batchGetGenesWithSpecificity( batch, results );
            total += batch.size();
        }

        timer.stop();
        if ( timer.getTime() > 10000 ) {
            AbstractDao.log.info( "Probe to gene map finished: " + total + " retrieved in " + timer.getTime() + "ms" );
        }
        return results;
    }

    @Override
    public Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences ) {
        if ( compositeSequences == null || compositeSequences.size() == 0 )
            return null;

        StringBuilder buf = new StringBuilder();

        for ( Iterator<CompositeSequence> it = compositeSequences.iterator(); it.hasNext(); ) {
            CompositeSequence compositeSequence = it.next();
            if ( compositeSequence == null || compositeSequence.getId() == null ) {
                throw new IllegalArgumentException();
            }
            long id = compositeSequence.getId();
            buf.append( id );
            if ( it.hasNext() )
                buf.append( "," );
        }

        // This uses the 'full' query, assuming that this list isn't too big.
        String nativeQueryString = CompositeSequenceDaoImpl.nativeBaseSummaryQueryString + " WHERE cs.ID IN (" + buf.toString() + ")";
        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( nativeQueryString );
        queryObject.addScalar( "deID" ).addScalar( "deName" ).addScalar( "bsName" ).addScalar( "bsdbacc" )
                .addScalar( "ssrid" ).addScalar( "gpId" ).addScalar( "gpName" ).addScalar( "gpNcbi" )
                .addScalar( "geneid" ).addScalar( "gId" ).addScalar( "gSymbol" )
                .addScalar( "gNcbi" ).addScalar( "adShortName" ).addScalar( "adId" );

        queryObject.addScalar( "chrom" ).addScalar( "tgst" ).addScalar( "tgend" ).addScalar( "tgstarts" )
                .addScalar( "bsId" );

        queryObject.addScalar( "deDesc", StandardBasicTypes.TEXT ); // must do this for CLOB or Hibernate is unhappy
        queryObject.addScalar( "adName" );
        queryObject.setMaxResults( CompositeSequenceDaoImpl.MAX_CS_RECORDS );
        //noinspection unchecked
        return queryObject.list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, Integer numResults ) {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }

        if ( numResults <= 0 ) {
            // get all probes. Uses a light-weight version of this query that omits as much as possible.
            final String queryString = CompositeSequenceDaoImpl.nativeBaseSummaryShorterQueryString + " where ad.id = " + arrayDesign
                    .getId();
            org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession()
                    .createSQLQuery( queryString );
            queryObject.addScalar( "deID" ).addScalar( "deName" ).addScalar( "bsName" ).addScalar( "bsdbacc" )
                    .addScalar( "ssrid" ).addScalar( "gId" ).addScalar( "gSymbol" );
            queryObject.setMaxResults( CompositeSequenceDaoImpl.MAX_CS_RECORDS );
            return queryObject.list();

        }
        // just a chunk but get the full set of results.
        //language=HQL
        final String queryString = "select cs from CompositeSequence as cs inner join cs.arrayDesign as ar where ar = :ar";
        List<CompositeSequence> cs = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ar", arrayDesign )
                .setMaxResults( numResults )
                .list();
        return this.getRawSummary( cs );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( final Collection<CompositeSequence> compositeSequences ) {
        Session session = getSessionFactory().getCurrentSession();
        int i = 0;
        int numToDo = compositeSequences.size();
        for ( CompositeSequence cs : compositeSequences ) {

            session.buildLockRequest( LockOptions.NONE ).lock( cs );
            Hibernate.initialize( cs.getArrayDesign() );
            session.buildLockRequest( LockOptions.NONE ).lock( cs.getArrayDesign() );
            Hibernate.initialize( cs.getArrayDesign().getPrimaryTaxon() );

            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                continue;
            }

            session.buildLockRequest( LockOptions.NONE ).lock( bs );
            Hibernate.initialize( bs );
            Hibernate.initialize( bs.getTaxon() );

            DatabaseEntry dbEntry = bs.getSequenceDatabaseEntry();
            if ( dbEntry != null ) {
                Hibernate.initialize( dbEntry );
                Hibernate.initialize( dbEntry.getExternalDatabase() );
                session.evict( dbEntry );
                session.evict( dbEntry.getExternalDatabase() );
            }

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
                AbstractDao.log.info( "Progress: " + i + "/" + numToDo + "..." );
                try {
                    Thread.sleep( 10 );
                } catch ( InterruptedException e ) {
                    //
                }
            }

            session.evict( bs );
        }
        session.clear();
    }

    @Override
    public CompositeSequence thaw( final CompositeSequence compositeSequence ) {
        this.thaw( Collections.singleton( compositeSequence ) );
        return compositeSequence;
    }

    //    @Override
    //    public CompositeSequence thaw( final CompositeSequence compositeSequence ) {
    //        if ( compositeSequence == null )
    //            return null;
    //        //noinspection unchecked
    //        HibernateTemplate templ = this.getHibernateTemplate();
    //        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
    //            @Override
    //            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
    //                Hibernate.initialize( compositeSequence );
    //                Hibernate.initialize( compositeSequence.getBiologicalCharacteristic() );
    //                if ( compositeSequence.getBiologicalCharacteristic() != null ) {
    //                    Hibernate.initialize( compositeSequence.getBiologicalCharacteristic().getTaxon() );
    //                    if ( compositeSequence.getBiologicalCharacteristic().getTaxon() != null ) {
    //                        Hibernate
    //                                .initialize( compositeSequence.getBiologicalCharacteristic().getTaxon().getExternalDatabase() );
    //                    }
    //                    Hibernate.initialize( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() );
    //                    if ( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null ) {
    //                        Hibernate.initialize( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
    //                                .getExternalDatabase() );
    //                    }
    //                    Hibernate.initialize( compositeSequence.getBiologicalCharacteristic().getBioSequence2GeneProduct() );
    //                    for ( BioSequence2GeneProduct bsgp : compositeSequence.getBiologicalCharacteristic()
    //                            .getBioSequence2GeneProduct() ) {
    //                        if ( bsgp != null ) {
    //                            Hibernate.initialize( bsgp );
    //                            if ( bsgp.getGeneProduct() != null ) {
    //                                Hibernate.initialize( bsgp.getGeneProduct() );
    //                                Hibernate.initialize( bsgp.getGeneProduct().getGene() );
    //                                if ( bsgp.getGeneProduct().getGene() != null ) {
    //                                    Hibernate.initialize( bsgp.getGeneProduct().getGene().getAliases() );
    //                                    Hibernate.initialize( bsgp.getGeneProduct().getGene().getAccessions() );
    //                                }
    //                            }
    //                        }
    //                    }
    //                }
    //                Hibernate.initialize( compositeSequence.getArrayDesign() );
    //                return compositeSequence;
    //            }
    //        } );
    //
    //    }

    @Override
    public Collection<CompositeSequence> load( Collection<Long> ids ) {

        if ( ids == null || ids.size() == 0 ) {
            return new HashSet<>();
        }

        //language=HQL
        final String queryString = "select cs from CompositeSequence cs where cs.id in (:ids)";
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        int batchSize = 2000;
        Collection<Long> batch = new HashSet<>();
        Collection<CompositeSequence> results = new HashSet<>();
        for ( Long id : ids ) {
            batch.add( id );

            if ( batch.size() == batchSize ) {
                queryObject.setParameterList( "ids", batch );
                //noinspection unchecked
                results.addAll( queryObject.list() );
                batch.clear();
            }
        }

        // tail end.
        if ( batch.size() > 0 ) {
            queryObject.setParameterList( "ids", batch );
            //noinspection unchecked
            results.addAll( queryObject.list() );
        }
        return results;
    }

    @Override
    public CompositeSequence find( CompositeSequence compositeSequence ) {

        if ( compositeSequence.getName() == null )
            return null;

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( CompositeSequence.class );

        queryObject.add( Restrictions.eq( "name", compositeSequence.getName() ) );
        queryObject.createCriteria( "arrayDesign" )
                .add( Restrictions.eq( "name", compositeSequence.getArrayDesign().getName() ) );

        java.util.List<?> results = queryObject.list();
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

    }

    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            throw new IllegalArgumentException( "compositeSequence must have name and arrayDesign." );
        }

        CompositeSequence existingCompositeSequence = this.find( compositeSequence );
        if ( existingCompositeSequence != null ) {
            if ( AbstractDao.log.isDebugEnabled() )
                AbstractDao.log.debug( "Found existing compositeSequence: " + existingCompositeSequence );
            return existingCompositeSequence;
        }
        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "Creating new compositeSequence: " + compositeSequence );
        return this.create( compositeSequence );
    }

    /**
     * @param batch   of composite sequences to process
     * @param results - adding to this
     */
    private void batchGetGenesWithSpecificity( Collection<CompositeSequence> batch,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> results ) {

        if ( batch.size() == 0 ) {
            return;
        }

        //language=HQL
        final String queryString = "select cs,bas from CompositeSequence cs, BioSequence2GeneProduct bas inner join cs.biologicalCharacteristic bs "
                + "inner join fetch bas.geneProduct gp inner join fetch gp.gene gene "
                + "where bas.bioSequence=bs and cs in (:cs)";
        List<?> qr = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "cs", batch )
                .list();

        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence csa = ( CompositeSequence ) oa[0];
            BioSequence2GeneProduct ba = ( BioSequence2GeneProduct ) oa[1];

            if ( ba instanceof BlatAssociation ) {
                BlatResult blatResult = ( ( BlatAssociation ) ba ).getBlatResult();
                PhysicalLocation pl = blatResult.getTargetAlignedRegion();

                /*
                 * We didn't always used to fill in the targetAlignedRegion ... this is just in case.
                 */
                if ( pl == null ) {
                    pl = PhysicalLocation.Factory.newInstance();
                    pl.setChromosome( blatResult.getTargetChromosome() );
                    pl.setNucleotide( blatResult.getTargetStart() );
                    pl.setNucleotideLength(
                            blatResult.getTargetEnd().intValue() - blatResult.getTargetStart().intValue() );
                    pl.setStrand( blatResult.getStrand() );
                    // Note: not bothering to fill in the bin.
                }

            }

            if ( !results.containsKey( csa ) ) {
                results.put( csa, new HashSet<BioSequence2GeneProduct>() );
            }

            results.get( csa ).add( ba );
        }

        /*
         * This is kind of important. We ensure we return an empty map for probes that do not have a mapping.
         */
        for ( CompositeSequence cs : batch ) {
            if ( !results.containsKey( cs ) ) {
                results.put( cs, new HashSet<BioSequence2GeneProduct>() );
            }

        }

    }
}