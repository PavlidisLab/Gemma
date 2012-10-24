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
package ubic.gemma.model.genome;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;
import ubic.gemma.util.SequenceBinUtils;
import ubic.gemma.util.TaxonUtility;
import cern.colt.list.DoubleArrayList;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.Gene
 */
@Repository
public class GeneDaoImpl extends ubic.gemma.model.genome.GeneDaoBase {

    private static final int BATCH_SIZE = 100;
    private static Log log = LogFactory.getLog( GeneDaoImpl.class.getName() );
    private static final int MAX_RESULTS = 100;

    private static final int MAX_WINDOW = 1000000;

    private static final int WINDOW_INCREMENT = 500;

    @Autowired
    private CacheManager cacheManager;

    private String G2CS_CACHE_NAME = "Gene2CsCache";
    private Cache gene2CsCache;

    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#find(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene find( Gene gene ) {

        Criteria queryObject = super.getSession().createCriteria( Gene.class );

        BusinessKey.checkKey( gene );

        BusinessKey.createQueryObject( queryObject, gene );

        java.util.List<Gene> results = queryObject.list();
        Object result = null;

        if ( results.isEmpty() ) {

            // Gene nearMatch = findByOfficialSymbol( gene.getOfficialSymbol(), gene.getTaxon() );
            // if ( nearMatch != null ) {
            // log.warn( "Strict find did not locate the gene, but match by symbol:" + gene + " matches " + nearMatch );
            // }

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
                if ( StringUtils.isBlank( foundGene.getPreviousNcbiId() ) ) continue;
                // Note hack we used to allow multiple previous ids.
                for ( String previousId : StringUtils.split( foundGene.getPreviousNcbiId(), "," ) ) {
                    try {
                        if ( gene.getNcbiGeneId().equals( Integer.parseInt( previousId ) ) ) {
                            toDelete.add( foundGene );
                            continue;
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
                if ( foundGene.getNcbiGeneId() != null && gene.getNcbiGeneId() != null
                        && foundGene.getNcbiGeneId().equals( gene.getNcbiGeneId() ) ) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#find(ubic.gemma.model.genome.PhysicalLocation )
     */
    @Override
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return findByPosition( physicalLocation.getChromosome(), physicalLocation.getNucleotide(),
                physicalLocation.getNucleotide() + physicalLocation.getNucleotideLength(), physicalLocation.getStrand() );
    }

    @Override
    public Collection<? extends Gene> findByEnsemblId( String id ) {
        final String query = "from GeneImpl g where g.ensemblId = :id";
        return this.getHibernateTemplate().findByNamedParam( query, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialNameInexact(java.lang.String )
     */
    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        final String query = "from GeneImpl g where g.officialName like :officialName order by g.officialName";
        org.hibernate.Query queryObject = this.getSession().createQuery( query );
        queryObject.setParameter( "officialName", officialName );
        queryObject.setMaxResults( MAX_RESULTS );
        return queryObject.list();
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        final String query = "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol";
        org.hibernate.Query queryObject = this.getSession().createQuery( query );
        queryObject.setParameter( "officialSymbol", officialSymbol );
        queryObject.setMaxResults( MAX_RESULTS );
        return queryObject.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#findNearest(ubic.gemma.model.genome.PhysicalLocation)
     */
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

            if ( targetStart < 0 ) windowStart = 0L;

            windowEnd = windowEnd + i * WINDOW_INCREMENT;

            if ( log.isDebugEnabled() )
                log.debug( "Search: " + physicalLocation + " length=" + ( windowEnd - windowStart ) + " strand="
                        + physicalLocation.getStrand() );

            // note that here we ignore the strand.
            Collection<Gene> candidates = findByPosition( chrom, windowStart, windowEnd, useStrand ? strand : null );
            if ( !candidates.isEmpty() ) {
                if ( log.isDebugEnabled() )
                    log.debug( physicalLocation + ": " + candidates.size() + " nearby genes at window size " + i
                            * WINDOW_INCREMENT );

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
                            // g -------oooooo
                            // t ooooo
                            range = geneStart - targetEnd;
                            if ( log.isDebugEnabled() )
                                log.debug( gene + " is " + range + " from the right end of " + physicalLocation );
                        } else if ( geneStart <= targetStart ) {
                            if ( geneEnd >= targetEnd ) {
                                // g oooooooooo
                                // t --ooooo---
                                candidate.setContainedWithinGene( true );
                                candidate.setOverlapsGene( true );
                                range = 0;
                                if ( log.isDebugEnabled() ) log.debug( gene + " contains target " + physicalLocation );

                            } else if ( geneEnd > targetStart ) {
                                // g ooooooooo
                                // t ----ooooooooo
                                range = 0;
                                candidate.setOverlapsGene( true );
                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " overlaps left end of " + physicalLocation );

                            } else {
                                assert geneEnd < targetStart;
                                // g ooooooo
                                // t ---------ooooooo
                                log.debug( gene + " is " + range + " from the left end of " + physicalLocation );
                                range = targetStart - geneEnd;
                            }
                        } else {
                            if ( geneEnd > targetEnd ) {
                                // g ---oooooooo
                                // t ooooooo
                                if ( log.isDebugEnabled() )
                                    log.debug( gene + " overlaps right end of " + physicalLocation );
                                range = 0;
                                candidate.setOverlapsGene( true );
                            } else {
                                assert geneEnd <= targetEnd;
                                // g ----oooo----
                                // t oooooooooooo
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
            // log.debug( "Widening search..." );
            i++;
        }

        // nuthin'
        log.debug( "Nothing found" );
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#getGeneCoexpressionNodeDegree(java.util.Collection)
     */
    @Override
    public Map<Gene, GeneCoexpressionNodeDegree> getGeneCoexpressionNodeDegree( Collection<Gene> genes ) {

        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "from GeneCoexpressionNodeDegreeImpl n where n.gene in (:g)", "g", genes );

        Map<Gene, GeneCoexpressionNodeDegree> result = new HashMap<Gene, GeneCoexpressionNodeDegree>();
        for ( Object o : r ) {
            GeneCoexpressionNodeDegree n = ( GeneCoexpressionNodeDegree ) o;
            result.put( n.getGene(), n );
        }

        return result;
    }
    
    @Override
    public Map<Long, GeneCoexpressionNodeDegree> getGeneIdCoexpressionNodeDegree( Collection<Long> geneIds ) {

        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "from GeneCoexpressionNodeDegreeImpl n where n.gene.id in (:g)", "g", geneIds );

        Map<Long, GeneCoexpressionNodeDegree> result = new HashMap<Long, GeneCoexpressionNodeDegree>();
        for ( Object o : r ) {
            GeneCoexpressionNodeDegree n = ( GeneCoexpressionNodeDegree ) o;
            result.put( n.getGene().getId(), n );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#getGeneCoexpressionNodeDegree(java.util .Collection, java.util.Collection)
     */
    @SuppressWarnings("deprecation")
    @Override
    public Map<Gene, Double> getGeneCoexpressionNodeDegree( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees ) {
        org.springframework.util.StopWatch watch = new org.springframework.util.StopWatch( "getCoexpressionNodeDegree" );

        Map<Long, Gene> idMap = EntityUtils.getIdMap( genes );
        Map<Long, Collection<Long>> cs2GeneMap = CommonQueries.getCs2GeneIdMap( idMap.keySet(), this.getSession() );

        /*
         * When we aggregate, it's only over data sets that had the gene tested (inner join)
         */
        watch.start( "DB query: " + ees.size() + " experiments, " + cs2GeneMap.keySet().size() + " probes." );
        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "select p.probe, p.nodeDegreeRank from ProbeCoexpressionAnalysisImpl pca "
                        + "join pca.probesUsed p where pca.experimentAnalyzed in (:ees) and p.probe.id in (:ps)",
                new String[] { "ps", "ees" }, new Object[] { cs2GeneMap.keySet(), ees } );
        watch.stop();
        watch.start( "Post processs " + r.size() + " results" );

        Map<Long, DoubleArrayList> interm = new HashMap<Long, DoubleArrayList>();
        for ( Gene g : genes ) {
            interm.put( g.getId(), new DoubleArrayList() );
        }

        for ( Object o : r ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence cs = ( CompositeSequence ) oa[0];
            Double nodeDegreeRank = ( Double ) oa[1];

            Collection<Long> gs = cs2GeneMap.get( cs.getId() );

            // if ( gs.size() > 1 ) continue; // nonspecific - perhaps control
            // this.
            interm.get( gs.iterator().next() ).add( nodeDegreeRank );
        }

        // aggregate.
        Map<Gene, Double> result = new HashMap<Gene, Double>();
        for ( Long g : interm.keySet() ) {
            DoubleArrayList vals = interm.get( g );
            /*
             * Note: under the null, each node degree is drawn from a uniform(0,1); sampling properties for the mean of
             * this are the same as for pvalues, so we treat them thusly. (My first pass implementation just used the
             * mean). Note we don't do 1 - fp here -- high node degrees are still represented as values near 1 after
             * this transformation. See bug 2379
             */
            Gene gene = idMap.get( g );
            if ( vals.size() == 0 ) {
                result.put( gene, null );
            } else {
                result.put( gene, MetaAnalysis.fisherCombinePvalues( vals ) );
            }

        }
        watch.stop();
        log.info( watch.prettyPrint() );
        return result;
    }

    @Override
    public GeneCoexpressionNodeDegree getGeneCoexpressionNodeDegree( Gene gene ) {

        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "from GeneCoexpressionNodeDegreeImpl n where n.gene = :g", "g", gene );

        if ( r.isEmpty() ) return null;
        if ( r.size() > 1 ) log.warn( "More than one node degree record found for " + gene );
        return ( GeneCoexpressionNodeDegree ) r.get( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#getGeneCoexpressionNodeDegree(ubic.gemma.model.genome.Gene,
     * java.util.Collection)
     */
    @Override
    public Map<BioAssaySet, Double> getGeneCoexpressionNodeDegree( Gene gene, Collection<? extends BioAssaySet> ees ) {
        StopWatch timer = new StopWatch();
        timer.start();
        /*
         * Typically this is being run for all data sets for a taxon, so this unconstrained query is okay.
         */
        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession() );

        if ( ees.isEmpty() ) throw new IllegalArgumentException( "You must provide at least one experiment" );

        if ( probes.isEmpty() ) {
            return null;
        }

        Map<Long, BioAssaySet> eeidmap = EntityUtils.getIdMap( ees );
        Map<Long, CompositeSequence> pidmap = EntityUtils.getIdMap( probes );

        String queryString = "select pca.experimentAnalyzed.id, avg(p.nodeDegreeRank) from ProbeCoexpressionAnalysisImpl pca "
                + "join pca.probesUsed p where pca.experimentAnalyzed.id in (:ees) and p.probe.id in (:ps) "
                + "group by pca.experimentAnalyzed.id";

        if ( log.isDebugEnabled() ) log.debug( NativeQueryUtils.toSql( getHibernateTemplate(), queryString ) );

        List<?> r = this.getHibernateTemplate().findByNamedParam(
        /* Note the avg() ... group by operation here on the rank */
        queryString, new String[] { "ps", "ees" }, new Object[] { pidmap.keySet(), eeidmap.keySet() } );

        Map<BioAssaySet, Double> result = new HashMap<BioAssaySet, Double>();
        for ( Object o : r ) {
            Object[] oa = ( Object[] ) o;
            Long ee = ( Long ) oa[0];

            // See query above: this is the mean for the probes for the gene.
            Double nodeDegreeRank = ( Double ) oa[1];
            if ( nodeDegreeRank == null ) continue; // should not happen!

            result.put( eeidmap.get( ee ), nodeDegreeRank );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "getGeneCoexpressionNodeDegree " + gene + ": " + timer.getTime() + "ms; query was: "
                    + NativeQueryUtils.toSql( getHibernateTemplate(), queryString ) );
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#loadThawed(java.util.Collection)
     */
    @Override
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<Gene>();

        if ( ids.isEmpty() ) return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
            result.addAll( doLoadThawedLite( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Load+thaw " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }
    
    @Override
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<Gene>();

        if ( ids.isEmpty() ) return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
            result.addAll( doLoadThawedLiter( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Load+thaw " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'entities' can not be null" );
        }
        // remove associations
        List<?> assocs = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select ba from BioSequence2GeneProductImpl ba join ba.geneProduct gp join gp.gene g where g.id in (:g)",
                        "g", EntityUtils.getIds( entities ) );
        if ( !assocs.isEmpty() ) this.getHibernateTemplate().deleteAll( assocs );

        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#remove(ubic.gemma.model.genome.Gene)
     */
    @Override
    public void remove( ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'gene' can not be null" );
        }
        // remove associations
        List<?> assocs = this.getHibernateTemplate().findByNamedParam(
                "select ba from BioSequence2GeneProductImpl ba join ba.geneProduct gp join gp.gene g where g=:g ", "g",
                gene );
        if ( !assocs.isEmpty() ) this.getHibernateTemplate().deleteAll( assocs );

        this.getHibernateTemplate().delete( gene );
    }

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    @Override
    public Gene thawAliases( final Gene gene ) {
        if ( gene.getId() == null ) return gene;

        List<?> res = this.getHibernateTemplate().findByNamedParam(
                "select distinct g from GeneImpl g "
                        + "left join fetch g.aliases left join fetch g.accessions acc where g.id=:gid", "gid",
                gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#thawLite(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene thawLite( final Gene gene ) {
        return this.thaw( gene );
    }
    
    @Override
    public Gene thawLiter( final Gene gene ) {
    	if ( gene.getId() == null ) return gene;

        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct g from GeneImpl g "                                
                                + " left join fetch g.taxon" + " where g.id=:gid",
                        "gid", gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleCountAll()
     */
    @Override
    protected Integer handleCountAll() {
        final String query = "select count(*) from GeneImpl";
        List<?> r = getHibernateTemplate().find( query );
        return ( Integer ) r.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleFindByAccession(java.lang.String,
     * ubic.gemma.model.common.description.ExternalDatabase)
     */
    @SuppressWarnings({ "cast" })
    @Override
    protected Gene handleFindByAccession( String accession, ExternalDatabase source ) {
        Collection<Gene> genes = new HashSet<Gene>();
        final String accessionQuery = "select g from GeneImpl g inner join g.accessions a where a.accession = :accession";
        final String externalDbquery = accessionQuery + " and a.externalDatabase = :source";

        if ( source == null ) {
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
                genes = this.getHibernateTemplate().findByNamedParam( externalDbquery,
                        new String[] { "accession", "source" }, new Object[] { accession, source } );
            }
        }
        if ( genes.size() > 0 ) {
            return ( Gene ) genes.iterator().next();
        }
        return null;

    }

    /**
     * Gets all the genes referred to by the alias defined by the search string.
     * 
     * @param search
     * @return Collection
     */
    @Override
    protected Collection<Gene> handleFindByAlias( String search ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.aliases als where als.alias = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "symbol", "taxon" },
                new Object[] { symbol, taxon } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + symbol + " in " + taxon + ", return first hit" );
        }
        return ( Gene ) results.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCoexpressedGenes(java.util.Collection, java.util.Collection,
     * java.lang.Integer, boolean)
     */
    @Override
    protected Map<Gene, QueryGeneCoexpression> handleGetCoexpressedGenes( final Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean interGeneOnly ) {

        if ( genes.size() == 0 || ees.size() == 0 ) {
            throw new IllegalArgumentException( "nothing to search" );
        }

        final Map<Gene, QueryGeneCoexpression> coexpressions = new HashMap<Gene, QueryGeneCoexpression>();

        if ( genes.size() == 1 ) {
            Gene soleQueryGene = genes.iterator().next();
            QueryGeneCoexpression coexpressedGenes = this.getCoexpressedGenes( soleQueryGene, ees, stringency );
            coexpressions.put( soleQueryGene, coexpressedGenes );
            return coexpressions;
        }

        Map<Long, Gene> queryGenes = new HashMap<Long, Gene>();
        for ( Gene g : genes ) {
            queryGenes.put( g.getId(), g );
            coexpressions.put( g, new QueryGeneCoexpression( g.getId(), stringency ) );
        }

        /*
         * NOTE: assuming all genes are from the same taxon!
         */
        Gene givenG = genes.iterator().next();
        log.debug( "Gene: " + givenG.getName() );

        final String p2pClassName = getP2PClassName( givenG );
        final Collection<Long> eeIds = EntityUtils.getIds( ees );

        String queryString = getNativeBatchQueryString( p2pClassName, "firstVector", "secondVector", eeIds,
                interGeneOnly );

        Session session = this.getSession();
        org.hibernate.Query queryObject = setCoexpQueryParameters( session, genes, queryString );

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        // This is the actual business of querying the database.
        processCoexpQuery( queryGenes, queryObject, coexpressions );

        overallWatch.stop();
        if ( overallWatch.getTime() > 3000 ) {
            log.info( "Raw query for " + genes.size() + " genes in batch: " + overallWatch.getTime()
                    + "ms;\n Query was: " + queryObject.getQueryString() );
        }

        for ( QueryGeneCoexpression coexp : coexpressions.values() ) {
            postProcessSpecificity( coexp );
        }

        return coexpressions;

    }

    /**
     * Gets all the genes that are coexpressed with another gene based on stored coexpression 'links', essentially as
     * described in Lee et al. (2004) Genome Research.
     * 
     * @param gene to use as the query
     * @param ees Data sets to restrict the search to.
     * @param stringency minimum number of data sets the coexpression has to occur in before it 'counts'.
     * @return Collection of QueryGeneCoexpression. This needs to be 'postprocessed' before it has all the data needed
     *         for web display.
     */
    @Override
    protected QueryGeneCoexpression handleGetCoexpressedGenes( final Gene gene, Collection<? extends BioAssaySet> ees,
            Integer stringency ) {

        log.debug( "Gene: " + gene.getName() );

        final String p2pClassName = getP2PClassName( gene );

        final QueryGeneCoexpression coexpressions = new QueryGeneCoexpression( gene.getId(), stringency );

        if ( ees.size() == 0 ) {
            log.debug( "No experiments selected" );
            coexpressions.setErrorState( "No experiments were selected" );
            return coexpressions;
        }

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        /*
         * Check cache first, if we have already queried experiment X for the query, then we don't need to query
         * experiment X at all.
         */
        Collection<BioAssaySet> eesToSearch = new HashSet<BioAssaySet>();
        Map<Long, Collection<CoexpressionCacheValueObject>> cachedResults = new HashMap<Long, Collection<CoexpressionCacheValueObject>>();

        if ( this.getProbe2ProbeCoexpressionCache().isEnabled() ) {
            for ( BioAssaySet ee : ees ) {
                Collection<CoexpressionCacheValueObject> eeResults = this.getProbe2ProbeCoexpressionCache().get( ee,
                        gene );

                if ( eeResults != null ) {
                    cachedResults.put( ee.getId(), eeResults );
                    if ( log.isDebugEnabled() ) log.debug( "Cache hit! for ee=" + ee.getId() );
                } else {
                    eesToSearch.add( ee );
                }
            }
            overallWatch.stop();
            if ( overallWatch.getTime() > 100 ) {
                if ( log.isInfoEnabled() )
                    log.info( "Probe2probe cache check: " + overallWatch.getTime() + "ms for " + ees.size()
                            + " EEs, found " + cachedResults.size() + " results in cache; must still search "
                            + eesToSearch.size() );
            }
            overallWatch.reset();
            overallWatch.start();
        } else {
            eesToSearch.addAll( ees );
        }

        if ( eesToSearch.size() > 0 ) {

            final Collection<Long> eeIds = EntityUtils.getIds( eesToSearch );

            String queryString = getNativeQueryString( p2pClassName, "firstVector", "secondVector", eeIds );

            Session session = this.getSession( false );
            Query queryObject = setCoexpQueryParameters( session, gene, queryString );

            // This is the actual business of querying the database.
            processCoexpQuery( gene, queryObject, coexpressions );
        }

        overallWatch.stop();
        if ( overallWatch.getTime() > 2000 ) {
            log.info( "Raw query: " + overallWatch.getTime() + "ms" );
        }

        if ( cachedResults.size() > 0 ) {
            overallWatch.reset();
            overallWatch.start();
            mergeCachedCoexpressionResults( coexpressions, cachedResults );
            overallWatch.stop();
            if ( overallWatch.getTime() > 100 ) {
                log.info( "Merge " + cachedResults.size() + " cached results in: " + overallWatch.getTime() + "ms" );
            }
        }

        if ( coexpressions.getQueryGeneProbes().size() == 0 ) {
            if ( log.isDebugEnabled() ) log.debug( "Coexpression query gene " + gene + " has no probes" );
            coexpressions.setErrorState( "Query gene " + gene + " has no probes" );
            return coexpressions;
        }

        postProcessSpecificity( coexpressions );

        return coexpressions;
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     * 
     * @param id
     * @return Collection
     */
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) {
        final String queryString = "select count(distinct cs) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List<?> r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( Long ) r.iterator().next();
    }

    /*
     * Gets all the CompositeSequences related to the gene identified by the given gene and arrayDesign. (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCompositeSequences(ubic. gemma.model.genome.Gene,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compSeq = null;
        final String queryString = "select distinct cs from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence "
                + " and gene = :gene and cs.arrayDesign = :arrayDesign ";

        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "arrayDesign", arrayDesign );
            queryObject.setParameter( "gene", gene );
            compSeq = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compSeq;
    }

    /**
     * Gets all the CompositeSequences related to the gene identified by the given id.
     * 
     * @param id
     * @return Collection
     */
    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequencesById( long id ) {
        final String queryString = "select distinct cs from GeneImpl as gene  inner join gene.products as gp, BioSequence2GeneProductImpl "
                + " as bs2gp , CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetGenesByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<Gene> handleGetGenesByTaxon( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon ";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadKnownGenes(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<Gene> handleLoadKnownGenes( Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene fetch all properties where gene.taxon = :taxon";

        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleLoadMultiple( Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return new HashSet<Gene>();
        }
        int batchSize = 2000;
        if ( ids.size() > batchSize ) {
            log.info( "Loading " + ids.size() + " genes ..." );
        }

        final String queryString = "select gene from GeneImpl gene where gene.id in (:ids)";
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();

        for ( Long gene : ids ) {
            batch.add( gene );
            if ( batch.size() == batchSize ) {
                genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
        }

        if ( ids.size() > batchSize ) {
            log.info( "... done" );
        }

        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleThaw(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Gene handleThaw( final Gene gene ) {
        if ( gene.getId() == null ) return gene;

        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct g from GeneImpl g "
                                + "left join fetch g.aliases left join fetch g.accessions acc"
                                + " left join fetch acc.externalDatabase left join fetch g.products gp "
                                + " left join fetch g.auditTrail at left join fetch at.events "
                                + "left join fetch gp.accessions gpacc left join fetch gpacc.externalDatabase left join"
                                + " fetch gp.physicalLocation gppl left join fetch gppl.chromosome chr left join fetch chr.taxon "
                                + " left join fetch g.taxon t left join fetch t.externalDatabase" + " where g.id=:gid",
                        "gid", gene.getId() );

        return ( Gene ) res.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleThawLite(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleThawLite( final Collection<Gene> genes ) {
        if ( genes.isEmpty() ) return new HashSet<Gene>();

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
    protected void initDao() throws Exception {
        /*
         * Initialize the cache; if it already exists it will not be recreated.
         */
        boolean terracottaEnabled = ConfigUtils.getBoolean( "gemma.cache.clustered", false );
        boolean diskPersistent = false;
        int maxElements = 500000;
        boolean eternal = false;
        boolean overFlowToDisk = false;
        int diskExpiryThreadIntervalSeconds = 600;
        int maxElementsOnDisk = 0;
        boolean terracottaCoherentReads = false;
        boolean clearOnFlush = false;

        if ( terracottaEnabled ) {
            CacheConfiguration config = new CacheConfiguration( G2CS_CACHE_NAME, maxElements );
            config.setStatistics( false );
            config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
            config.setOverflowToDisk( false );
            config.setEternal( eternal );
            config.setTimeToIdleSeconds( 0 );
            config.setMaxElementsOnDisk( maxElementsOnDisk );
            config.addTerracotta( new TerracottaConfiguration() );
            config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
            config.clearOnFlush( clearOnFlush );
            config.setTimeToLiveSeconds( 0 );
            config.getTerracottaConfiguration().setClustered( true );
            config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
            NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
            TimeoutBehaviorConfiguration tobc = new TimeoutBehaviorConfiguration();
            tobc.setType( TimeoutBehaviorType.NOOP.getTypeName() );
            nonstopConfiguration.addTimeoutBehavior( tobc );
            config.getTerracottaConfiguration().addNonstop( nonstopConfiguration );
            this.gene2CsCache = new Cache( config );
        } else {
            this.gene2CsCache = new Cache( G2CS_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk,
                    null, eternal, 0, 0, diskPersistent, diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( gene2CsCache );
        this.gene2CsCache = cacheManager.getCache( G2CS_CACHE_NAME );

    }

    /**
     * @param results
     */
    private void debug( List<Gene> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Gene g : results ) {
            buf.append( g + "\n" );
        }
        log.error( buf );

    }

    /**
     * @param ids
     * @return
     */
    private Collection<Gene> doLoadThawedLite( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select g from GeneImpl g left join fetch g.aliases left join fetch g.accessions acc "
                        + "join fetch g.taxon t left join fetch g.products gp left join fetch g.multifunctionality "
                        + "where g.id in (:gids)", "gids", ids );
    }
    
    private Collection<Gene> doLoadThawedLiter( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select g from GeneImpl g left "
                        + "join fetch g.taxon t "
                        + "where g.id in (:gids)", "gids", ids );
    }

    /**
     * Returns genes in the region.
     * 
     * @param chrom
     * @param targetStart
     * @param targetEnd
     * @param strand
     * @return
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
        return getHibernateTemplate().findByNamedParam( query, params, vals );
    }

    /**
     * @param css
     * @return map
     */
    private Map<Long, Collection<Long>> getCS2GeneMap( Collection<Long> css ) {
        Map<Long, Collection<Long>> csId2geneIds = new HashMap<Long, Collection<Long>>();
        if ( css == null || css.size() == 0 ) {
            return csId2geneIds;
        }

        int CHUNK_SIZE = 1000;
        Session session = this.getSession();

        for ( Collection<Long> chunk : new BatchIterator<Long>( css, CHUNK_SIZE ) ) {
            csId2geneIds.putAll( processCS2GeneChunk( chunk, session ) );
        }

        return csId2geneIds;

    }

    /**
     * For queries involving multiple genes as inputs.
     * 
     * @param p2pClassName
     * @param in
     * @param out
     * @param eeIds this is required.
     * @param interGeneOnly true to restrict to links among the query genes only (this will not work correctly if you
     *        only put in one gene!)
     * @return
     */
    private String getNativeBatchQueryString( String p2pClassName, String in, String out, Collection<Long> eeIds,
            boolean interGeneOnly ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";

        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds, "," );
            eeClause += ") AND ";
        }

        String interGeneOnlyClause = "";

        if ( interGeneOnly ) {
            interGeneOnlyClause = " AND gcOut.GENE in (:ids) ";
        }

        String p2pClass = getP2PTableNameForClassName( p2pClassName );

        /**
         * Fields:
         * 
         * <pre>
         * 0 Geneid
         * 1 exper 
         * 2 score
         * 3 csin
         * 4 csout
         * 5 queryGene id
         * </pre>
         */
        String query = "SELECT gcOut.GENE as id, coexp.EXPRESSION_EXPERIMENT_FK as exper, coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut, gcIn.GENE as queryGeneId FROM GENE2CS gcIn STRAIGHT_JOIN "
                + p2pClass + " coexp ON gcIn.CS=coexp." + inKey + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp."
                + outKey + " WHERE " + eeClause + " gcIn.GENE in (:ids) " + interGeneOnlyClause;

        if ( log.isDebugEnabled() ) log.debug( query );
        return query;
    }

    /**
     * @param p2pClassName
     * @param in
     * @param out
     * @param eeIds this is required.
     * @return
     */
    private String getNativeQueryString( String p2pClassName, String in, String out, Collection<Long> eeIds ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";

        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds, "," );
            eeClause += ") AND ";
        }

        String p2pClass = getP2PTableNameForClassName( p2pClassName );

        /**
         * Fields:
         * 
         * <pre>
         * 0 Geneid
         * 1 exper 
         * 2 score
         * 3 csin
         * 4 csout
         * </pre>
         */
        String query = "SELECT gcOut.GENE as id, coexp.EXPRESSION_EXPERIMENT_FK as exper,  coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut FROM GENE2CS gcIn STRAIGHT_JOIN " + p2pClass
                + " coexp ON gcIn.CS=coexp." + inKey + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp." + outKey
                + " WHERE " + eeClause + " gcIn.GENE=:id ";

        // AND gcOut.GENE <> :id // Omit , see below!

        /*
         * Important Implementation Note: The clause to exclude self-hits actually causes problems. When a self-match
         * happens, it means that the probe in question hybridizes to the query gene. When the probe in question also
         * hybridizes to other genes, we need to know that that 'link' is potentially due to a self-match. Such probes
         * are excluded from later analysis. If we throw those matches out at the SQL query stage, it is hard for us to
         * detect such crosshybridization problems later.
         */

        if ( log.isDebugEnabled() ) log.debug( query );
        return query;
    }

    /**
     * @param givenG
     * @return
     */
    private String getP2PClassName( Gene givenG ) {
        if ( false ) // TODO - provide means of determining if we should use the 'UserProbeCoExpressionImpl'
            return "UserProbeCoExpressionImpl";
        else if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
            return "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( givenG.getTaxon() ) )
            return "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( givenG.getTaxon() ) )
            return "RatProbeCoExpressionImpl";
        else
            return "OtherProbeCoExpressionImpl";
    }

    /**
     * @param className
     * @return
     */
    private String getP2PTableNameForClassName( String className ) {
        if ( className.equals( "UserProbeCoExpressionImpl" ) ) {
            return "USER_PROBE_CO_EXPRESSION";
        } else if ( className.equals( "HumanProbeCoExpressionImpl" ) )
            return "HUMAN_PROBE_CO_EXPRESSION";
        else if ( className.equals( "MouseProbeCoExpressionImpl" ) )
            return "MOUSE_PROBE_CO_EXPRESSION";
        else if ( className.equals( "RatProbeCoExpressionImpl" ) )
            return "RAT_PROBE_CO_EXPRESSION";
        else
            return "OTHER_PROBE_CO_EXPRESSION";
    }

    /**
     * Merge in the cached results. The CVOs that are cached only contain results for a single expression experiment.
     * 
     * @param coexpressions
     * @param cachedResults
     */
    private void mergeCachedCoexpressionResults( QueryGeneCoexpression coexpressions,
            Map<Long, Collection<CoexpressionCacheValueObject>> cachedResults ) {
        for ( Long eeid : cachedResults.keySet() ) {
            Collection<CoexpressionCacheValueObject> cache = cachedResults.get( eeid );

            for ( CoexpressionCacheValueObject cachedCVO : cache ) {
                assert cachedCVO.getQueryProbe() != null;
                assert cachedCVO.getCoexpressedProbe() != null;
                if ( cachedCVO.getQueryGene().equals( cachedCVO.getCoexpressedGene() ) ) {
                    // defensive check against self-links being in the cache
                    // (shouldn't happen)
                    continue;
                }

                coexpressions.addLink( eeid, cachedCVO.getScore(), cachedCVO.getQueryProbe(),
                        cachedCVO.getCoexpressedGene(), cachedCVO.getCoexpressedProbe() );

                assert coexpressions.contains( cachedCVO.getCoexpressedGene() );
            }
        }
    }

    /**
     * Fill in specificity information.
     * <p>
     * Performance notes: This seems to rarely take more than 1 sec. But this is also the site of a major memory leak.
     * 
     * @param coexpressions
     */
    private void postProcessSpecificity( final QueryGeneCoexpression coexpressions ) {
        // fill in information about the query gene
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Long> queryGeneProbeIds = coexpressions.getQueryGeneProbes();
        Collection<Long> targetGeneProbeIds = coexpressions.getTargetGeneProbes();

        Map<Long, Collection<Long>> querySpecificity = getCS2GeneMap( queryGeneProbeIds );
        Map<Long, Collection<Long>> targetSpecificity = getCS2GeneMap( targetGeneProbeIds );

        if ( timer.getTime() > 1000 ) {
            log.info( "Specificity postprocess CS2GeneMap: " + timer.getTime() + "ms" );
        }

        coexpressions.postProcess( querySpecificity, targetSpecificity );
    }

    /**
     * Perform and process the coexpression query.
     * 
     * @param queryGene
     * @param geneMap
     * @param queryObject
     */
    private void processCoexpQuery( Gene queryGene, Query queryObject, QueryGeneCoexpression coexpressions ) {
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            processCoexpQueryResult( queryGene, scroll, coexpressions );
        }

    }

    /**
     * @param queryGenes
     * @param queryObject
     * @param coexpressions
     */
    private void processCoexpQuery( Map<Long, Gene> queryGenes, Query queryObject,
            Map<Gene, QueryGeneCoexpression> coexpressions ) {
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            processCoexpQueryResult( queryGenes, scroll, coexpressions );
        }

    }

    /**
     * Process a single query result from the coexpression search, converting it into a
     * CoexpressionCollectionValueObject
     * 
     * @param queryGene
     * @param geneMap
     * @param resultSet
     * @see getFastNativeQueryString for the parameterization of the query output.
     */
    private void processCoexpQueryResult( Gene queryGene, ScrollableResults resultSet,
            QueryGeneCoexpression coexpressions ) {

        Long coexpressedGene = resultSet.getLong( 0 );

        if ( coexpressedGene.equals( queryGene.getId() ) ) {
            return;
        }

        Long eeID = resultSet.getLong( 1 );
        Double score = resultSet.getDouble( 2 );
        Long queryProbe = resultSet.getLong( 3 );
        Long coexpressedProbe = resultSet.getLong( 4 );
        coexpressions.addLink( eeID, score, queryProbe, coexpressedGene, coexpressedProbe );

        /*
         * Cache the result. Note that this will be disabled during 'large' analyses.
         */
        if ( this.getProbe2ProbeCoexpressionCache().isEnabled() ) {
            CoexpressionCacheValueObject coExVOForCache = new CoexpressionCacheValueObject();
            coExVOForCache.setQueryGene( queryGene.getId() );
            coExVOForCache.setCoexpressedGene( coexpressedGene );
            coExVOForCache.setExpressionExperiment( eeID );
            coExVOForCache.setScore( score );
            coExVOForCache.setQueryProbe( queryProbe );
            coExVOForCache.setCoexpressedProbe( coexpressedProbe );
            if ( log.isDebugEnabled() ) log.debug( "Caching: " + coExVOForCache );

            this.getProbe2ProbeCoexpressionCache().addToCache( coExVOForCache );
        }

    }

    /**
     * @param queryGenes
     * @param resultSet
     * @param coexpressions
     */
    private void processCoexpQueryResult( Map<Long, Gene> queryGenes, ScrollableResults resultSet,
            Map<Gene, QueryGeneCoexpression> coexpressions ) {

        Long coexpressedGene = resultSet.getLong( 0 );
        Long eeID = resultSet.getLong( 1 );
        Double score = resultSet.getDouble( 2 );
        Long queryProbe = resultSet.getLong( 3 );
        Long coexpressedProbe = resultSet.getLong( 4 );
        Long queryGeneId = resultSet.getLong( 5 );

        if ( queryGeneId.equals( coexpressedGene ) ) {
            return;
        }

        Gene queryGene = queryGenes.get( queryGeneId );
        assert queryGene != null : queryGeneId + " did not match given queries";
        QueryGeneCoexpression ccvo = coexpressions.get( queryGene );
        assert ccvo != null;
        ccvo.addLink( eeID, score, queryProbe, coexpressedGene, coexpressedProbe );

        /*
         * Cache the result.
         */
        if ( this.getProbe2ProbeCoexpressionCache().isEnabled() ) {
            CoexpressionCacheValueObject coExVOForCache = new CoexpressionCacheValueObject();
            coExVOForCache.setQueryGene( queryGene.getId() );
            coExVOForCache.setCoexpressedGene( coexpressedGene );
            coExVOForCache.setExpressionExperiment( eeID );
            coExVOForCache.setScore( score );
            coExVOForCache.setQueryProbe( queryProbe );
            coExVOForCache.setCoexpressedProbe( coexpressedProbe );
            if ( log.isDebugEnabled() ) log.debug( "Caching: " + coExVOForCache );
            this.getProbe2ProbeCoexpressionCache().addToCache( coExVOForCache );
        }
    }

    /**
     * @param csId2geneIds
     * @param csIdChunk
     * @param session
     */
    private Map<Long, Collection<Long>> processCS2GeneChunk( Collection<Long> csIdChunk, Session session ) {
        assert csIdChunk.size() > 0;
        Map<Long, Collection<Long>> csId2geneIds = new HashMap<Long, Collection<Long>>();

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * Check the cache first.
         */
        Collection<Long> neededCs = new HashSet<Long>();
        for ( Long csid : csIdChunk ) {
            Element element = gene2CsCache.get( csid );
            if ( element != null ) {
                csId2geneIds.put( csid, ( Collection<Long> ) element.getValue() );
            } else {
                neededCs.add( csid );
            }
        }

        if ( neededCs.size() == 0 ) {
            return csId2geneIds;
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Gene2Cs Cache check: " + timer.getTime() + "ms" );
        }
        timer.reset();
        timer.start();

        String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS WHERE CS in ("
                + StringUtils.join( neededCs, "," ) + ")";

        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.addScalar( "id", new LongType() );
        queryObject.addScalar( "geneId", new LongType() );

        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        Map<Long, Collection<Long>> newitems = new HashMap<Long, Collection<Long>>();

        while ( scroll.next() ) {
            Long csid = scroll.getLong( 0 );
            Long geneId = scroll.getLong( 1 );
            if ( !csId2geneIds.containsKey( csid ) ) {
                csId2geneIds.put( csid, new HashSet<Long>() );

            }
            if ( !newitems.containsKey( csid ) ) {
                newitems.put( csid, new HashSet<Long>() );
            }

            csId2geneIds.get( csid ).add( geneId );
            newitems.get( csid ).add( geneId );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Gene2Cs batch fetch " + newitems.size() + " elements: " + timer.getTime() + "ms" );
        }
        timer.reset();
        timer.start();

        for ( Long csid : newitems.keySet() ) {
            gene2CsCache.put( new Element( csid, newitems.get( csid ) ) );
        }

        if ( timer.getTime() > 200 ) {
            log.info( "Gene2Cs cache fill " + newitems.size() + " elements: " + timer.getTime() + "ms" );
            try {
                log.info( gene2CsCache.getMemoryStoreSize() + " items in cache memory; " );
            } catch ( Exception e ) {
                // no big deal.
                log.info( "Was unable to get Gene2CS cache stats:  " + e.getMessage() );
            }
        }
        return csId2geneIds;
    }

    /**
     * For batch queries
     * 
     * @param genes
     * @param ees
     * @param eeIds
     * @param queryString
     * @return
     */
    private org.hibernate.Query setCoexpQueryParameters( Session session, Collection<Gene> genes, String queryString ) {
        SQLQuery queryObject;
        queryObject = session.createSQLQuery( queryString ); // for native query.
        queryObject.addScalar( "id", new LongType() ); // gene out.
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.addScalar( "queryGeneId", new LongType() );
        queryObject.setParameterList( "ids", EntityUtils.getIds( genes ) );

        return queryObject;
    }

    /**
     * @param gene
     * @param ees
     * @param eeIds
     * @param queryString
     * @return
     */
    private org.hibernate.Query setCoexpQueryParameters( Session session, Gene gene, String queryString ) {
        org.hibernate.SQLQuery queryObject;
        queryObject = session.createSQLQuery( queryString ); // for native query.

        queryObject.addScalar( "id", new LongType() ); // gene out.
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.setLong( "id", gene.getId() );

        return queryObject;
    }

}