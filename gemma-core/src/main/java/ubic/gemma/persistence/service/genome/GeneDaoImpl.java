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
package ubic.gemma.persistence.service.genome;

import net.sf.ehcache.CacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.util.*;

import java.util.*;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Gene</code>.
 *
 * @see Gene
 */
@Repository
public class GeneDaoImpl extends VoEnabledDao<Gene, GeneValueObject> implements GeneDao {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RESULTS = 100;
    private static final int MAX_WINDOW = 1000000;
    private static final int WINDOW_INCREMENT = 500;
    private static final String G2CS_CACHE_NAME = "Gene2CsCache";
    private final CacheManager cacheManager;

    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory, CacheManager cacheManager ) {
        super( Gene.class, sessionFactory );
        this.cacheManager = cacheManager;
    }

    @Override
    public Gene findByNcbiId( Integer ncbiId ) {
        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.ncbiGeneId = :n" ).setParameter( "n", ncbiId ).uniqueResult();
    }

    @Override
    public Collection<Gene> findByOfficialSymbol( String officialSymbol ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialSymbol=:officialSymbol order by g.officialName" )
                .setParameter( "officialSymbol", officialSymbol ).list();
    }

    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.findByProperty( "officialName", officialName );
    }

    @Override
    public Collection<Gene> findByPhysicalLocation( final PhysicalLocation location ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene as gene where gene.physicalLocation = :location" )
                .setParameter( "location", location ).list();
    }

    @Override
    public Gene find( Gene gene ) {

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Gene.class );

        BusinessKey.checkKey( gene );

        BusinessKey.createQueryObject( queryObject, gene );

        //noinspection unchecked,unchecked
        List<Gene> results = queryObject.list();
        Object result;

        if ( results.isEmpty() ) {

            return null;

        } else if ( results.size() > 1 ) {

            /*
             * As a side-effect, we delete relics. This is a bit ugly, but takes care of the problem! It was put in
             * place to help in the cleanup of duplicated genes. But this can happen fairly routinely when NCBI
             * information changes in messy ways.
             *
             * FIXME this can fail because 'find' methods are read-only; it will be okay if it is a nested call from a
             * read-write method.
             */
            Collection<Gene> toDelete = new HashSet<Gene>();
            for ( Gene foundGene : results ) {
                if ( StringUtils.isBlank( foundGene.getPreviousNcbiId() ) )
                    continue;
                // Note hack we used to allow multiple previous ids.
                for ( String previousId : StringUtils.split( foundGene.getPreviousNcbiId(), "," ) ) {
                    try {
                        if ( gene.getNcbiGeneId().equals( Integer.parseInt( previousId ) ) ) {
                            toDelete.add( foundGene );
                        }
                    } catch ( NumberFormatException e ) {
                        // no action
                    }
                }
            }

            if ( !toDelete.isEmpty() ) {
                assert toDelete.size() < results.size(); // it shouldn't be everything!
                log.warn( "Deleting gene(s) that use a deprecated NCBI ID: " + StringUtils.join( toDelete, " | " ) );
                this.remove( toDelete ); // WARNING this might fail due to constraints.
            }
            results.removeAll( toDelete );

            for ( Gene foundGene : results ) {
                if ( foundGene.getNcbiGeneId() != null && gene.getNcbiGeneId() != null && foundGene.getNcbiGeneId()
                        .equals( gene.getNcbiGeneId() ) ) {
                    return foundGene;
                }
            }

            /*
             * This should be quite a rare situation if the database is kept tidy.
             */
            if ( results.size() > 1 ) {
                log.error( "Multiple genes found for " + gene + ":" );
                debug( results );

                Collections.sort( results, new Comparator<Gene>() {
                    @Override
                    public int compare( Gene arg0, Gene arg1 ) {
                        return arg0.getId().compareTo( arg1.getId() );
                    }
                } );
                result = results.iterator().next();
                log.error( "Returning arbitrary gene: " + result );
            } else {
                result = results.get( 0 );
            }

        } else {
            result = results.get( 0 );
        }

        return ( Gene ) result;

    }

    @Override
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return findByPosition( physicalLocation.getChromosome(), physicalLocation.getNucleotide(),
                physicalLocation.getNucleotide() + physicalLocation.getNucleotideLength(),
                physicalLocation.getStrand() );
    }

    @Override
    public Collection<? extends Gene> findByEnsemblId( String id ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from Gene g where g.ensemblId = :id" )
                .setParameter( "id", id ).list();
    }

    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialName like :officialName order by g.officialName" )
                .setParameter( "officialName", officialName ).setMaxResults( MAX_RESULTS ).list();
    }

    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialSymbol like :officialSymbol order by g.officialSymbol" )
                .setParameter( "officialSymbol", officialSymbol ).setMaxResults( MAX_RESULTS ).list();
    }

    @Override
    public Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, Gene> result = new HashMap<String, Gene>();
        final String queryString = "select g from Gene as g join fetch g.taxon t where g.officialSymbol in (:symbols) and t.id = :taxonId";

        for ( Collection<String> batch : new BatchIterator<String>( query, BATCH_SIZE ) ) {
            //noinspection unchecked
            List<Gene> results = getHibernateTemplate()
                    .findByNamedParam( queryString, new String[] { "symbols", "taxonId" },
                            new Object[] { batch, taxonId } );
            for ( Gene g : results ) {
                result.put( g.getOfficialSymbol().toLowerCase(), g );
            }
        }
        return result;
    }

    @Override
    public Map<Integer, Gene> findByNcbiIds( Collection<Integer> ncbiIds ) {
        Map<Integer, Gene> result = new HashMap<Integer, Gene>();
        final String queryString = "from Gene g where g.ncbiGeneId in (:ncbi)";

        for ( Collection<Integer> batch : new BatchIterator<Integer>( ncbiIds, BATCH_SIZE ) ) {
            //noinspection unchecked
            List<Gene> results = getHibernateTemplate().findByNamedParam( queryString, "ncbi", batch );
            for ( Gene g : results ) {
                result.put( g.getNcbiGeneId(), g );
            }
        }
        return result;
    }

    @Override
    public int getPlatformCountById( Long id ) {
        final String queryString = "select count(distinct cs.arrayDesign) from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List r = getSession().createQuery( queryString ).setParameter( "id", id ).list();
        return ( ( Long ) r.iterator().next() ).intValue();

    }

    @Override
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<Gene>();

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
            result.addAll( doLoadThawedLite( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            log.debug( "Load+thaw " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<Gene>();

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
            result.addAll( doLoadThawedLiter( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            log.debug( "Load+thaw " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public void remove( Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'gene' can not be null" );
        }
        // remove associations
        List associations = this.getHibernateTemplate().findByNamedParam(
                "select ba from BioSequence2GeneProduct ba join ba.geneProduct gp join gp.gene g where g=:g ", "g",
                gene );
        if ( !associations.isEmpty() )
            this.getHibernateTemplate().deleteAll( associations );

        this.getHibernateTemplate().delete( gene );
    }

    /**
     * Only thaw the Aliases, very light version
     */
    @Override
    public Gene thawAliases( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;

        List<?> res = this.getHibernateTemplate().findByNamedParam( "select distinct g from Gene g "
                + "left join fetch g.aliases left join fetch g.accessions acc where g.id=:gid", "gid", gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    @Override
    public Gene thawLite( final Gene gene ) {
        return this.thaw( gene );
    }

    @Override
    public Gene thawLiter( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;

        List<?> res = this.getHibernateTemplate().findByNamedParam(
                "select distinct g from Gene g " + " left join fetch g.taxon" + " where g.id=:gid", "gid",
                gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    @Override
    public Gene findByAccession( String accession, ExternalDatabase source ) {
        Collection<Gene> genes = new HashSet<Gene>();
        final String accessionQuery = "select g from Gene g inner join g.accessions a where a.accession = :accession";
        final String externalDbquery = accessionQuery + " and a.externalDatabase = :source";

        if ( source == null ) {
            //noinspection unchecked
            genes = this.getHibernateTemplate().findByNamedParam( accessionQuery, "accession", accession );
            if ( genes.size() == 0 ) {
                try {
                    return this.findByNcbiId( Integer.parseInt( accession ) );
                } catch ( NumberFormatException e ) {
                    // it's not an NCBIid
                }
            }
        } else {
            if ( source.getName().equalsIgnoreCase( "NCBI" ) ) {
                try {
                    return this.findByNcbiId( Integer.parseInt( accession ) );
                } catch ( NumberFormatException e ) {
                    // it's not an NCBIid
                }
            } else {
                //noinspection unchecked
                genes = this.getHibernateTemplate()
                        .findByNamedParam( externalDbquery, new String[] { "accession", "source" },
                                new Object[] { accession, source } );
            }
        }
        if ( genes.size() > 0 ) {
            return genes.iterator().next();
        }
        return null;

    }

    /**
     * Gets all the genes referred to by the alias defined by the search string.
     *
     * @return Collection
     */
    @Override
    public Collection<Gene> findByAlias( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct g from Gene as g inner join g.aliases als where als.alias = :search" )
                .setParameter( "search", search ).list();
    }

    @Override
    public Gene findByOfficialSymbol( String symbol, Taxon taxon ) {
        return ( Gene ) this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct g from Gene as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon" )
                .setParameter( "symbol", symbol ).setParameter( "taxon", taxon ).uniqueResult();
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     *
     * @return Collection
     */
    @Override
    public long getCompositeSequenceCountById( long id ) {
        final String queryString = "select count(distinct cs) from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List<?> r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( Long ) r.iterator().next();
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compSeq;
        final String queryString = "select distinct cs from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence "
                + " and gene = :gene and cs.arrayDesign = :arrayDesign ";

        try {
            org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
            queryObject.setParameter( "arrayDesign", arrayDesign );
            queryObject.setParameter( "gene", gene );
            //noinspection unchecked
            compSeq = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compSeq;
    }

    /**
     * Gets all the CompositeSequences related to the gene identified by the given id.
     *
     * @return Collection
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( long id ) {
        final String queryString = "select distinct cs from Gene as gene  inner join gene.products as gp, BioSequence2GeneProduct "
                + " as bs2gp , CompositeSequence as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    @Override
    public Collection<Gene> getGenesByTaxon( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from Gene as gene where gene.taxon = :taxon ";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection<Gene> getMicroRnaByTaxon( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from Gene as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection<Gene> loadKnownGenes( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from Gene as gene fetch all properties where gene.taxon = :taxon";

        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection<Gene> load( Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return new HashSet<Gene>();
        }
        int batchSize = 2000;
        if ( ids.size() > batchSize ) {
            log.info( "Loading " + ids.size() + " genes ..." );
        }

        final String queryString = "from Gene where id in (:ids)";
        Collection<Gene> genes = new HashSet<Gene>();

        BatchIterator<Long> it = BatchIterator.batches( ids, batchSize );
        for ( ; it.hasNext(); ) {
            //noinspection unchecked
            genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", it.next() ) );
        }

        if ( ids.size() > batchSize ) {
            log.info( "... done" );
        }

        return genes;
    }

    @Override
    public Gene thaw( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;

        List<?> res = this.getHibernateTemplate().findByNamedParam(
                "select distinct g from Gene g " + " left join fetch g.aliases left join fetch g.accessions acc"
                        + " left join fetch acc.externalDatabase left join fetch g.products gp "
                        + " left join fetch gp.accessions gpacc left join fetch gpacc.externalDatabase left join"
                        + " fetch gp.physicalLocation gppl left join fetch gppl.chromosome chr left join fetch chr.taxon "
                        + " left join fetch g.taxon t left join fetch t.externalDatabase "
                        + " left join fetch g.multifunctionality left join fetch g.phenotypeAssociations "
                        + " where g.id=:gid",
                "gid", gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        if ( genes.isEmpty() )
            return new HashSet<Gene>();

        Collection<Gene> result = new HashSet<Gene>();
        Collection<Gene> batch = new HashSet<Gene>();

        for ( Gene g : genes ) {
            batch.add( g );
            if ( batch.size() == BATCH_SIZE ) {
                result.addAll( loadThawed( EntityUtils.getIds( batch ) ) );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            result.addAll( loadThawed( EntityUtils.getIds( batch ) ) );
        }

        return result;
    }

    @Override
    public GeneValueObject loadValueObject( Gene entity ) {
        return new GeneValueObject( entity );
    }

    @Override
    public Collection<GeneValueObject> loadValueObjects( Collection<Gene> entities ) {
        Collection<GeneValueObject> vos = new LinkedHashSet<GeneValueObject>();
        for ( Gene e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }

    @Override
    protected void initDao() throws Exception {
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );
        CacheUtils.createOrLoadCache( cacheManager, G2CS_CACHE_NAME, terracottaEnabled, 500000, false, false, 0, 0,
                false );
    }

    private Collection<Gene> doLoadThawedLite( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam(
                "select g from Gene g left join fetch g.aliases left join fetch g.accessions acc "
                        + "join fetch g.taxon t left join fetch g.products gp left join fetch g.multifunctionality "
                        + "where g.id in (:gids)",
                "gids", ids );
    }

    private Collection<Gene> doLoadThawedLiter( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getHibernateTemplate()
                .findByNamedParam( "select g from Gene g join fetch g.taxon t " + "where g.id in (:gids)", "gids",
                        ids );
    }

    /**
     * Returns genes in the region.
     */
    private Collection<Gene> findByPosition( Chromosome chrom, final Long targetStart, final Long targetEnd,
            final String strand ) {

        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        String query = "select distinct g from Gene as g "
                + "inner join fetch g.products prod  inner join fetch prod.physicalLocation pl inner join fetch pl.chromosome "
                + "where ((pl.nucleotide >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end) "
                + "OR (pl.nucleotide <= :start AND (pl.nucleotide + pl.nucleotideLength) >= :end) OR "
                + "(pl.nucleotide >= :start AND pl.nucleotide <= :end) "
                + "OR  ((pl.nucleotide + pl.nucleotideLength) >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end )) "
                + "and pl.chromosome = :chromosome ";

        query = query + " and " + SequenceBinUtils.addBinToQuery( "pl", targetStart, targetEnd );

        String[] params;
        Object[] vals;
        if ( strand != null ) {
            query = query + " and pl.strand = :strand ";
            params = new String[] { "chromosome", "start", "end", "strand" };
            vals = new Object[] { chrom, targetStart, targetEnd, strand };
        } else {
            params = new String[] { "chromosome", "start", "end" };
            vals = new Object[] { chrom, targetStart, targetEnd };
        }
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( query, params, vals );
    }

    private void debug( List<Gene> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Gene g : results ) {
            buf.append( g ).append( "\n" );
        }
        log.error( buf );

    }

}