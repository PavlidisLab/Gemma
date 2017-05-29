/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.genome;

import net.sf.ehcache.CacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.util.*;

import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class GeneDaoImpl extends GeneDaoBase {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RESULTS = 100;
    private static final int MAX_WINDOW = 1000000;
    private static final int WINDOW_INCREMENT = 500;
    private static final String G2CS_CACHE_NAME = "Gene2CsCache";

    private final CacheManager cacheManager;

    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory, CacheManager cacheManager ) {
        super( sessionFactory );
        this.cacheManager = cacheManager;
    }

    @Override
    public Gene find( Gene gene ) {

        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Gene.class );

        BusinessKey.checkKey( gene );

        BusinessKey.createQueryObject( queryObject, gene );

        //noinspection unchecked,unchecked
        List<Gene> results = queryObject.list();
        Object result;

        if ( results.isEmpty() ) {

            return null;

        } else if ( results.size() > 1 ) {

            /*
             * As a side-effect, we remove relics. This is a bit ugly, but takes care of the problem! It was put in
             * place to help in the cleanup of duplicated genes. But this can happen fairly routinely when NCBI
             * information changes in messy ways.
             * 
             * FIXME this can fail because 'find' methods are read-only; it will be okay if it is a nested call from a
             * read-write method.
             */
            Collection<Gene> toDelete = new HashSet<>();
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
        final String query = "from GeneImpl g where g.ensemblId = :id";
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam( query, "id", id );
    }

    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        final String query = "from GeneImpl g where g.officialName like :officialName order by g.officialName";
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( query );
        queryObject.setParameter( "officialName", officialName );
        queryObject.setMaxResults( MAX_RESULTS );
        //noinspection unchecked
        return queryObject.list();
    }

    @Override
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        final String query = "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol";
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( query );
        queryObject.setParameter( "officialSymbol", officialSymbol );
        queryObject.setMaxResults( MAX_RESULTS );
        //noinspection unchecked
        return queryObject.list();
    }

    @Override
    public Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, Gene> result = new HashMap<>();
        final String queryString = "select g from GeneImpl as g join fetch g.taxon t where g.officialSymbol in (:symbols) and t.id = :taxonId";

        for ( Collection<String> batch : new BatchIterator<>( query, BATCH_SIZE ) ) {
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
        Map<Integer, Gene> result = new HashMap<>();
        final String queryString = "from GeneImpl g where g.ncbiGeneId in (:ncbi)";

        for ( Collection<Integer> batch : new BatchIterator<>( ncbiIds, BATCH_SIZE ) ) {
            //noinspection unchecked
            List<Gene> results = getHibernateTemplate().findByNamedParam( queryString, "ncbi", batch );
            for ( Gene g : results ) {
                result.put( g.getNcbiGeneId(), g );
            }
        }
        return result;
    }

    @Override
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {

        // FIXME Should return a collection of relativeLocationData in the case
        // of ties

        if ( physicalLocation.getNucleotide() == null ) {
            throw new IllegalArgumentException( "Locations must have a nucleotide position" );
        }

        /*
         * Strategy: start with a small window, enlarge it until we decide enough is enough.
         */
        Chromosome chrom = physicalLocation.getChromosome();
        final Long targetStart = physicalLocation.getNucleotide();
        Integer nucleotideLength = physicalLocation.getNucleotideLength();
        final Long targetEnd = targetStart + ( nucleotideLength == null ? 0 : nucleotideLength );
        final String strand = physicalLocation.getStrand();
        if ( log.isDebugEnabled() )
            log.debug( "Start Search: " + physicalLocation + " length=" + ( targetEnd - targetStart ) );

        /*
         * Starting with exact location, look for genes, enlarging the region as needed -- ignoring strand.. Finds the
         * nearest hit, but tracks if the strand is the same.
         */
        int i = 0;
        long windowStart = targetStart;
        long windowEnd = targetEnd;
        while ( windowStart >= 0 && windowEnd - windowStart < MAX_WINDOW ) {
            windowStart = windowStart - i * WINDOW_INCREMENT;

            if ( targetStart < 0 )
                windowStart = 0L;

            windowEnd = windowEnd + i * WINDOW_INCREMENT;

            if ( log.isDebugEnabled() )
                log.debug( "Search: " + physicalLocation + " length=" + ( windowEnd - windowStart ) + " strand="
                        + physicalLocation.getStrand() );

            // note that here we ignore the strand.
            Collection<Gene> candidates = findByPosition( chrom, windowStart, windowEnd, useStrand ? strand : null );
            if ( !candidates.isEmpty() ) {
                if ( log.isDebugEnabled() )
                    log.debug( physicalLocation + ": " + candidates.size() + " nearby genes at window size "
                            + i * WINDOW_INCREMENT );

                long closestRange = ( long ) 1e10;
                RelativeLocationData result = null;
                for ( Gene gene : candidates ) {
                    this.thaw( gene );
                    for ( GeneProduct gp : gene.getProducts() ) {
                        PhysicalLocation genelocation = gp.getPhysicalLocation();

                        boolean onSameStrand = genelocation.getStrand().equals( strand );

                        assert genelocation.getChromosome().equals( physicalLocation.getChromosome() );
                        Long geneStart = genelocation.getNucleotide();
                        Long geneEnd = genelocation.getNucleotideLength() + geneStart;

                        RelativeLocationData candidate = new RelativeLocationData( physicalLocation, gene, gp,
                                genelocation );
                        candidate.setOnSameStrand( onSameStrand );

                        long range = 0;
                        // note we use the 'real' location of the par, not the
                        // window.

                        if ( geneStart > targetEnd ) {

                            range = geneStart - targetEnd;
                            if ( log.isDebugEnabled() )
                                log.debug( gene + " is " + range + " from the right end of " + physicalLocation );
                        } else if ( geneStart <= targetStart ) {
                            if ( geneEnd >= targetEnd ) {

                                candidate.setContainedWithinGene( true );
                                candidate.setOverlapsGene( true );
                                range = 0;
                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " contains target " + physicalLocation );

                            } else if ( geneEnd > targetStart ) {

                                range = 0;
                                candidate.setOverlapsGene( true );
                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " overlaps left end of " + physicalLocation );

                            } else {
                                assert geneEnd < targetStart;

                                log.debug( gene + " is " + range + " from the left end of " + physicalLocation );
                                range = targetStart - geneEnd;
                            }
                        } else {
                            if ( geneEnd > targetEnd ) {

                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " overlaps right end of " + physicalLocation );
                                range = 0;
                                candidate.setOverlapsGene( true );
                            } else {
                                assert geneEnd <= targetEnd;

                                range = 0;
                                candidate.setOverlapsGene( true );
                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " is contained within " + physicalLocation );
                            }
                        }

                        assert range >= 0;

                        if ( range < closestRange ) {
                            result = candidate;
                            result.setRange( range );
                            closestRange = range;
                        }

                    }

                }

                return result;
            }
            i++;
        }

        log.debug( "Nothing found" );
        return null;

    }

    @Override
    public int getPlatformCountById( Long id ) {
        final String queryString =
                "select count(distinct cs.arrayDesign) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List<?> r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( ( Long ) r.iterator().next() ).intValue();

    }

    @Override
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<>();

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<>( ids, BATCH_SIZE ) ) {
            result.addAll( doLoadThawedLite( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            log.debug( "Load+thaw " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<>();

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<>( ids, BATCH_SIZE ) ) {
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
        List<?> assocs = this.getHibernateTemplate().findByNamedParam(
                "select ba from BioSequence2GeneProduct ba join ba.geneProduct gp join gp.gene g where g=:g ", "g",
                gene );
        if ( !assocs.isEmpty() )
            this.getHibernateTemplate().deleteAll( assocs );

        this.getHibernateTemplate().delete( gene );
    }

    /**
     * Only thaw the Aliases, very light version
     */
    @Override
    public void thawAliases( final Gene gene ) {
        Hibernate.initialize( gene.getAliases() );
        Hibernate.initialize( gene.getAccessions() );
    }

    @Override
    public void thawLite( final Gene gene ) {
        this.thaw( gene );
    }

    @Override
    public void thawLiter( final Gene gene ) {
        Hibernate.initialize( gene.getTaxon() );
    }

    @Override
    protected Gene handleFindByAccession( String accession, ExternalDatabase source ) {
        Collection<Gene> genes = new HashSet<>();
        final String accessionQuery = "select g from GeneImpl g inner join g.accessions a where a.accession = :accession";
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
    protected Collection<Gene> handleFindByAlias( String search ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.aliases als where als.alias = :search";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        List<?> results = getHibernateTemplate()
                .findByNamedParam( queryString, new String[] { "symbol", "taxon" }, new Object[] { symbol, taxon } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + symbol + " in " + taxon + ", return first hit" );
        }
        return ( Gene ) results.iterator().next();
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     *
     * @return Collection
     */
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) {
        final String queryString =
                "select count(distinct cs) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List<?> r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( Long ) r.iterator().next();
    }

    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compSeq;
        final String queryString =
                "select distinct cs from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence "
                        + " and gene = :gene and cs.arrayDesign = :arrayDesign ";

        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
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
    protected Collection<CompositeSequence> handleGetCompositeSequencesById( long id ) {
        final String queryString =
                "select distinct cs from GeneImpl as gene  inner join gene.products as gp, BioSequence2GeneProduct "
                        + " as bs2gp , CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    @Override
    protected Collection<Gene> handleGetGenesByTaxon( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon ";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";
        //noinspection unchecked
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<Gene> handleLoadKnownGenes( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene fetch all properties where gene.taxon = :taxon";

        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<Gene> handleLoadMultiple( Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return new HashSet<>();
        }
        int batchSize = 2000;
        if ( ids.size() > batchSize ) {
            log.info( "Loading " + ids.size() + " genes ..." );
        }

        final String queryString = "from GeneImpl where id in (:ids)";
        Collection<Gene> genes = new HashSet<>();

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
    protected void handleThaw( final Gene gene ) {

        thawAliases( gene );
        for ( DatabaseEntry de : gene.getAccessions() ) {
            Hibernate.initialize( de.getExternalDatabase() );
        }
        Hibernate.initialize( gene.getProducts() );
        for ( GeneProduct gp : gene.getProducts() ) {
            Hibernate.initialize( gp.getAccessions() );
            for ( DatabaseEntry de : gp.getAccessions() ) {
                Hibernate.initialize( de.getExternalDatabase() );
            }
            Hibernate.initialize( gp.getPhysicalLocation() );
            Hibernate.initialize( gp.getPhysicalLocation().getChromosome() );
            Hibernate.initialize( gp.getPhysicalLocation().getChromosome().getTaxon() );
        }
        Hibernate.initialize( gene.getTaxon() );
        Hibernate.initialize( gene.getTaxon().getExternalDatabase() );
        Hibernate.initialize( gene.getMultifunctionality() );
        Hibernate.initialize( gene.getPhenotypeAssociations() );

    }

    @Override
    protected void handleThawLite( final Collection<Gene> genes ) {
        Collection<Gene> batch = new HashSet<>();

        for ( Gene g : genes ) {
            batch.add( g );
            if ( batch.size() == BATCH_SIZE ) {
                thaw( batch );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            thaw( batch );
        }
    }

    @Override
    protected void initDao() throws Exception {
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );
        CacheUtils.createOrLoadCache( cacheManager, G2CS_CACHE_NAME, terracottaEnabled, 500000, false, false, 0, 0,
                false );
    }

    private void debug( List<Gene> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Gene g : results ) {
            buf.append( g ).append( "\n" );
        }
        log.error( buf );

    }

    private Collection<Gene> doLoadThawedLite( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam(
                "select g from GeneImpl g left join fetch g.aliases left join fetch g.accessions acc "
                        + "join fetch g.taxon t left join fetch g.products gp left join fetch g.multifunctionality "
                        + "where g.id in (:gids)", "gids", ids );
    }

    private Collection<Gene> doLoadThawedLiter( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getHibernateTemplate()
                .findByNamedParam( "select g from GeneImpl g join fetch g.taxon t " + "where g.id in (:gids)", "gids",
                        ids );
    }

    /**
     * Returns genes in the region.
     */
    private Collection<Gene> findByPosition( Chromosome chrom, final Long targetStart, final Long targetEnd,
            final String strand ) {

        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        String query = "select distinct g from GeneImpl as g "
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

    private void thaw( Collection<Gene> genes ) {
        for ( Gene g : genes ) {
            this.thaw( g );
        }
    }

    @Override
    public GeneValueObject loadValueObject( Gene entity ) {
        return new GeneValueObject( entity );
    }

    @Override
    public Collection<GeneValueObject> loadValueObjects( Collection<Gene> entities ) {
        Collection<GeneValueObject> vos = new LinkedHashSet<>();
        for ( Gene e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }
}