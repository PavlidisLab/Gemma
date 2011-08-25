/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.genome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.SequenceBinUtils;
import ubic.gemma.util.TaxonUtility;

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

    /*
     * FIXME use a regular ehcache so we can use timeouts.
     */
    private HashMap<Long, Collection<Long>> gene2CsCache = new HashMap<Long, Collection<Long>>();

    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#find(ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Gene find( Gene gene ) {

        try {
            Criteria queryObject = super.getSession().createCriteria( Gene.class );

            BusinessKey.checkKey( gene );

            BusinessKey.createQueryObject( queryObject, gene );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    /*
                     * this can happen in semi-rare cases in queries by symbol, where the gene symbol is not unique for
                     * the taxon and the query did not have the gene name to further restrict the query.
                     */
                    log.error( "Multiple genes found for " + gene + ":" );
                    debug( results );

                    Collections.sort( results, new Comparator<Gene>() {
                        public int compare( Gene arg0, Gene arg1 ) {
                            return arg0.getId().compareTo( arg1.getId() );
                        }
                    } );
                    result = results.iterator().next();
                    log.error( "Returning arbitrary gene: " + result );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Gene ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#find(ubic.gemma.model.genome.PhysicalLocation)
     */
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return findByPosition( physicalLocation.getChromosome(), physicalLocation.getNucleotide(), physicalLocation
                .getNucleotide()
                + physicalLocation.getNucleotideLength(), physicalLocation.getStrand() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialNameInexact(java.lang.String)
     */
    @SuppressWarnings( { "unchecked" })
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        final String query = "from GeneImpl g where g.officialName like :officialName order by g.officialName";
        org.hibernate.Query queryObject = this.getSession( false ).createQuery( query );
        queryObject.setParameter( "officialName", officialName );
        queryObject.setMaxResults( MAX_RESULTS );
        return queryObject.list();
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    @SuppressWarnings( { "unchecked" })
    @Override
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        final String query = "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol";
        org.hibernate.Query queryObject = this.getSession( false ).createQuery( query );
        queryObject.setParameter( "officialSymbol", officialSymbol );
        queryObject.setMaxResults( MAX_RESULTS );
        return queryObject.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#findNearest(ubic.gemma.model.genome.PhysicalLocation)
     */
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {

        // FIXME Should return a collection of relativeLocationData in the case of ties

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
                        // note we use the 'real' location of the par, not the window.

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
     * @see ubic.gemma.model.genome.GeneDaoBase#findOrCreate(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene findOrCreate( Gene gene ) {
        Gene existingGene = this.find( gene );
        if ( existingGene != null ) {
            return existingGene;
        }
        // We consider this abnormal because we expect most genes to have been loaded into the system already.
        log.warn( "*** Creating new gene: " + gene + " ***" );
        return create( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntity(ubic.gemma.model.genome.gene.GeneValueObject)
     */
    public Gene geneValueObjectToEntity( GeneValueObject geneValueObject ) {
        return this.load( geneValueObject.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#loadThawed(java.util.Collection)
     */
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<Gene>();

        if ( ids.isEmpty() ) return result;

        Collection<Long> batch = new HashSet<Long>();

        StopWatch timer = new StopWatch();
        timer.start();
        for ( Long g : ids ) {
            batch.add( g );
            if ( batch.size() == BATCH_SIZE ) {
                result.addAll( doLoadThawedLite( batch ) );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            result.addAll( doLoadThawedLite( batch ) );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Load+thaw " + ids.size() + " genes: " + timer.getTime() + "ms" );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#thawLite(ubic.gemma.model.genome.Gene)
     */
    public Gene thawLite( final Gene gene ) {
        return this.thaw( gene );
    }

    /**
     * @param coexpressions
     * @param eeID
     * @param geneType
     * @return
     */
    private ExpressionExperimentValueObject addExpressionExperimentToCoexpressions(
            CoexpressionCollectionValueObject coexpressions, Long eeID, String geneType ) {
        ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( geneType, eeID );
        if ( eeVo == null ) {
            eeVo = new ExpressionExperimentValueObject();
            eeVo.setId( eeID );
            coexpressions.addExpressionExperiment( geneType, eeVo ); // unorganized.
        }
        return eeVo;
    }

    /**
     * Generically add a coexpression record to the results set.
     * 
     * @param coexpressions
     * @param eeID
     * @param queryGene
     * @param queryProbe
     * @param pvalue
     * @param score
     * @param coexpressedGene
     * @param geneType
     * @param coexpressedProbe
     */
    private void addResult( CoexpressionCollectionValueObject coexpressions, Long eeID, Gene queryGene,
            Long queryProbe, Double pvalue, Double score, Long coexpressedGene, String geneType, Long coexpressedProbe ) {
        CoexpressionValueObject coExVO;

        // add the gene (if not already seen)
        if ( coexpressions.contains( coexpressedGene ) ) {
            coExVO = coexpressions.get( coexpressedGene );
        } else {
            coExVO = new CoexpressionValueObject();
            coExVO.setQueryGene( queryGene );
            coExVO.setGeneId( coexpressedGene );
            coExVO.setGeneType( geneType );
            coexpressions.add( coExVO );
        }

        // log.info( "EE=" + eeID + " in Probe=" + queryProbe + " out Probe=" + coexpressedProbe + " gene="
        // + coexpressedGene + " score=" + String.format( "%.3f", score ) );
        // add the expression experiment.
        ExpressionExperimentValueObject eeVo = addExpressionExperimentToCoexpressions( coexpressions, eeID, geneType );

        // add the ee here so we know it is associated with this specific gene.
        coExVO.addSupportingExperiment( eeVo );

        coExVO.addScore( eeID, score, pvalue, queryProbe, coexpressedProbe );

        coexpressions.initializeSpecificityDataStructure( eeID, queryProbe );

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

    //
    // /**
    // * @param ids
    // * @return
    // */
    // @SuppressWarnings("unchecked")
    // private Collection<Gene> doLoadThawed( Collection<Long> ids ) {
    // return this
    // .getHibernateTemplate()
    // .findByNamedParam(
    // "select distinct g from GeneImpl g left join fetch g.aliases left join fetch g.accessions acc "
    // + "join fetch g.taxon t left join fetch t.externalDatabase"
    // + " left join fetch acc.externalDatabase left join fetch g.products gp "
    // + " left join fetch g.auditTrail at left join fetch at.events "
    // + "left join fetch gp.accessions gpacc left join fetch gpacc.externalDatabase left join"
    // + " fetch gp.physicalLocation gppl left join fetch gppl.chromosome chr left join fetch chr.taxon "
    // + " where g.id in (:gids)", "gids", ids );
    // }

    /**
     * @param ids
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<Gene> doLoadThawedLite( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select distinct g from GeneImpl g left join fetch g.aliases left join fetch g.accessions acc "
                        + "join fetch g.taxon t left join fetch g.products gp where g.id in (:gids)", "gids", ids );
    }

    /**
     * Returns KNOWN genes in the region.
     * 
     * @param chrom
     * @param targetStart
     * @param targetEnd
     * @param strand
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<Gene> findByPosition( Chromosome chrom, final Long targetStart, final Long targetEnd,
            final String strand ) {

        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        String query = "select distinct g from GeneImpl as g "
                + "inner join fetch g.products prod  inner join fetch prod.physicalLocation pl inner join fetch pl.chromosome "
                + "where ((pl.nucleotide >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end) "
                + "OR (pl.nucleotide <= :start AND (pl.nucleotide + pl.nucleotideLength) >= :end) OR "
                + "(pl.nucleotide >= :start AND pl.nucleotide <= :end) "
                + "OR  ((pl.nucleotide + pl.nucleotideLength) >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end )) "
                + "and pl.chromosome = :chromosome and g.class='GeneImpl' ";

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
     * @return
     * @throws Exception
     */
    private Map<Long, Collection<Long>> getCS2GeneMap( Collection<Long> css ) throws Exception {
        Map<Long, Collection<Long>> csId2geneIds = new HashMap<Long, Collection<Long>>();
        if ( css == null || css.size() == 0 ) {
            return csId2geneIds;
        }

        int CHUNK_SIZE = 1000;
        Session session = this.getSession();

        if ( css.size() <= CHUNK_SIZE ) {
            processCS2GeneChunk( csId2geneIds, css, session );
            return csId2geneIds;
        }

        Collection<Long> batch = new HashSet<Long>();

        for ( Long csId : css ) {
            batch.add( csId );
            if ( batch.size() == CHUNK_SIZE ) {
                processCS2GeneChunk( csId2geneIds, batch, session );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            processCS2GeneChunk( csId2geneIds, batch, session );
        }

        return csId2geneIds;

    }

    /**
     * @param ees
     * @return
     */
    private Collection<Long> getEEIds( Collection<? extends BioAssaySet> ees ) {
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( BioAssaySet e : ees ) {
            eeIds.add( e.getId() );
        }
        return eeIds;
    }

    /**
     * For queries involving multiple genes as inputs. Query outputs:
     * <ol >
     * <li>output gene id</li>
     * <li>output gene name</li>
     * <li>output gene official name</li>
     * <li>expression experiment
     * <li>pvalue</li>
     * <li>score</li>
     * <li>query gene probe id</li>
     * <li>output gene probe id</li>
     * <li>output gene type (predicted etc)</li>
     * <li>expression experiment name</li>
     * </ol>
     * 
     * @param p2pClassName
     * @param in
     * @param out
     * @param eeIds this is required.
     * @param knownGenesOnly
     * @param interGeneOnly true to restrict to links among the query genes only (this will not work correctly if you
     *        only put in one gene!)
     * @return
     */
    private String getNativeBatchQueryString( String p2pClassName, String in, String out, Collection<Long> eeIds,
            boolean knownGenesOnly, boolean interGeneOnly ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";

        // note that with current index scheme, you have to have EE ids specified.
        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds.iterator(), "," );
            eeClause += ") AND ";
        } else {
            log.warn( "This query may run very slowly without EE restriction" );
        }

        String knownGeneClause = "";
        if ( knownGenesOnly ) {
            knownGeneClause = " gcOut.GTYPE = 'GeneImpl' AND ";
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
         * 2 pvalue
         * 3 score
         * 4 csin
         * 5 csout
         * 6 genetype
         * 7 queryGene id
         * </pre>
         */
        String query = "SELECT gcOut.GENE as id, coexp.EXPRESSION_EXPERIMENT_FK as exper, coexp.PVALUE as pvalue, coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut, gcOut.GTYPE as geneType, gcIn.GENE as queryGeneId FROM GENE2CS gcIn INNER JOIN "
                + p2pClass
                + " coexp FORCE INDEX (EEKey) ON gcIn.CS=coexp."
                + inKey
                + " "
                + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp."
                + outKey
                + " INNER JOIN INVESTIGATION ee ON ee.ID=coexp.EXPRESSION_EXPERIMENT_FK "
                + " WHERE "
                + eeClause
                + knownGeneClause + " gcIn.GENE in (:ids) " + interGeneOnlyClause;

        return query;
    }

    /**
     * Query outputs:
     * <ol >
     * <li>output gene id</li>
     * <li>output gene name</li>
     * <li>output gene official name</li>
     * <li>expression experiment
     * <li>pvalue</li>
     * <li>score</li>
     * <li>query gene probe id</li>
     * <li>output gene probe id</li>
     * <li>output gene type (predicted etc)</li>
     * <li>expression experiment name</li>
     * </ol>
     * 
     * @param p2pClassName
     * @param in
     * @param out
     * @param eeIds this is required.
     * @param knownGenesOnly
     * @return
     */
    private String getNativeQueryString( String p2pClassName, String in, String out, Collection<Long> eeIds,
            boolean knownGenesOnly ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";

        // note that with current index scheme, you have to have EE ids specified.
        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds.iterator(), "," );
            eeClause += ") AND ";
        } else {
            log.warn( "This query may run very slowly without EE restriction" );
        }
        // eeClause = " coexp.EXPRESSION_EXPERIMENT_FK = " + eeIds.iterator().next() + " AND ";

        String knownGeneClause = "";
        if ( knownGenesOnly ) {
            knownGeneClause = " gcOut.GTYPE = 'GeneImpl' AND ";
        }

        String p2pClass = getP2PTableNameForClassName( p2pClassName );

        /**
         * Fields:
         * 
         * <pre>
         * 0 Geneid
         * 1 exper
         * 2 pvalue
         * 3 score
         * 4 csin
         * 5 csout
         * 6 genetype
         * </pre>
         */
        String query = "SELECT gcOut.GENE as id, coexp.EXPRESSION_EXPERIMENT_FK as exper, coexp.PVALUE as pvalue, coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut, gcOut.GTYPE as geneType FROM GENE2CS gcIn INNER JOIN "
                + p2pClass
                + " coexp FORCE INDEX (EEKey) ON gcIn.CS=coexp."
                + inKey
                + " "
                + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp."
                + outKey
                + " INNER JOIN INVESTIGATION ee ON ee.ID=coexp.EXPRESSION_EXPERIMENT_FK "
                + " WHERE "
                + eeClause
                + knownGeneClause + " gcIn.GENE=:id ";

        // AND gcOut.GENE <> :id // Omit , see below!

        /*
         * Important Implementation Note: The clause to exclude self-hits actually causes problems. When a self-match
         * happens, it means that the probe in question hybridizes to the query gene. When the probe in question also
         * hybridizes to other genes, we need to know that that 'link' is potentially due to a self-match. Such probes
         * are excluded from later analysis. If we throw those matches out at the SQL query stage, it is hard for us to
         * detect such crosshybridization problems later.
         */

        // log.info( query );
        return query;
    }

    /**
     * @param givenG
     * @return
     */
    private String getP2PClassName( Gene givenG ) {
        if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
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
        if ( className.equals( "HumanProbeCoExpressionImpl" ) )
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
     */
    private void mergeCachedCoexpressionResults( CoexpressionCollectionValueObject coexpressions,
            Map<Long, Collection<CoexpressionCacheValueObject>> cachedResults ) {
        for ( Long eeid : cachedResults.keySet() ) {
            Collection<CoexpressionCacheValueObject> cache = cachedResults.get( eeid );

            for ( CoexpressionCacheValueObject cachedCVO : cache ) {
                assert cachedCVO.getQueryProbe() != null;
                assert cachedCVO.getCoexpressedProbe() != null;
                if ( cachedCVO.getQueryGene().getId().equals( cachedCVO.getCoexpressedGene() ) ) {
                    // defensive check against self-links being in the cache (shouldn't happen)
                    continue;
                }
                addResult( coexpressions, eeid, cachedCVO.getQueryGene(), cachedCVO.getQueryProbe(), cachedCVO
                        .getPvalue(), cachedCVO.getScore(), cachedCVO.getCoexpressedGene(), cachedCVO.getGeneType(),
                        cachedCVO.getCoexpressedProbe() );

                assert coexpressions.contains( cachedCVO.getCoexpressedGene() );
            }
        }
    }

    /**
     * @param coexpressions
     * @param knownGenesOnly this probably doesn't matter much, as results are already determined, but defensive
     *        programming.
     *        <p>
     *        Performance notes: Empirically this rarely takes very long, definitely less than 1s in vast majority of
     *        cases. I changed the logging to trigger at 250ms get a bit better resolution - PP feb 2010
     */
    private void postProcess( CoexpressionCollectionValueObject coexpressions, boolean knownGenesOnly ) {

        StopWatch watch = new StopWatch();
        watch.start();

        postProcessKnownGenes( coexpressions );

        if ( !knownGenesOnly ) {
            postProcessProbeAlignedRegions( coexpressions );
            postProcessPredictedGenes( coexpressions );
        }

        watch.stop();
        Long elapsed = watch.getTime();
        coexpressions.setPostProcessTime( elapsed );

        if ( elapsed > 250 ) log.info( "Specificity check: " + elapsed + "ms." );
    }

    /**
     * @param coexpressions
     */
    private void postProcessKnownGenes( CoexpressionCollectionValueObject coexpressions ) {
        if ( coexpressions.getNumKnownGenes() == 0 ) return;
        CoexpressedGenesDetails knownGeneCoexpression = coexpressions.getKnownGeneCoexpression();
        knownGeneCoexpression.postProcess();
    }

    /**
     * @param coexpressions
     */
    private void postProcessPredictedGenes( CoexpressionCollectionValueObject coexpressions ) {
        if ( coexpressions.getNumPredictedGenes() == 0 ) return;
        CoexpressedGenesDetails predictedCoexpressionType = coexpressions.getPredictedGeneCoexpression();
        predictedCoexpressionType.postProcess();
    }

    /**
     * @param coexpressions
     */
    private void postProcessProbeAlignedRegions( CoexpressionCollectionValueObject coexpressions ) {
        if ( coexpressions.getNumProbeAlignedRegions() == 0 ) return;
        CoexpressedGenesDetails probeAlignedCoexpressionType = coexpressions.getProbeAlignedRegionCoexpression();
        probeAlignedCoexpressionType.postProcess();
    }

    /**
     * Fill in specificity information.
     * 
     * @param knownGenesOnly
     * @param coexpressions
     * @throws Exception <p>
     *         Performance notes: This often takes 3-5 seconds. PP Feb 2010
     */
    private void postProcessSpecificity( boolean knownGenesOnly, final CoexpressionCollectionValueObject coexpressions )
            throws Exception {
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
        timer.stop();
        timer.reset();

        coexpressions.setQueryGeneSpecifityInfo( querySpecificity );
        coexpressions.setTargetGeneSpecificityInfo( targetSpecificity );

        postProcess( coexpressions, knownGenesOnly );
    }

    /**
     * Perform and process the coexpression query.
     * 
     * @param queryGene
     * @param geneMap
     * @param queryObject
     */
    private void processCoexpQuery( Gene queryGene, Query queryObject, CoexpressionCollectionValueObject coexpressions ) {
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
            Map<Gene, CoexpressionCollectionValueObject> coexpressions ) {
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
            CoexpressionCollectionValueObject coexpressions ) {

        Long coexpressedGene = resultSet.getLong( 0 );

        if ( coexpressedGene.equals( queryGene.getId() ) ) {
            return;
        }

        Long eeID = resultSet.getLong( 1 );
        Double pvalue = resultSet.getDouble( 2 );
        Double score = resultSet.getDouble( 3 );
        Long queryProbe = resultSet.getLong( 4 );
        Long coexpressedProbe = resultSet.getLong( 5 );
        String geneType = resultSet.getString( 6 );

        addResult( coexpressions, eeID, queryGene, queryProbe, pvalue, score, coexpressedGene, geneType,
                coexpressedProbe );

        /*
         * Cache the result.
         */
        if ( this.getProbe2ProbeCoexpressionCache().isEnabled() ) {
            CoexpressionCacheValueObject coExVOForCache = new CoexpressionCacheValueObject();
            coExVOForCache.setQueryGene( queryGene );
            coExVOForCache.setCoexpressedGene( coexpressedGene );
            coExVOForCache.setGeneType( geneType );
            coExVOForCache.setExpressionExperiment( eeID );
            coExVOForCache.setScore( score );
            coExVOForCache.setPvalue( pvalue );
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
            Map<Gene, CoexpressionCollectionValueObject> coexpressions ) {

        Long coexpressedGene = resultSet.getLong( 0 );
        Long eeID = resultSet.getLong( 1 );
        Double pvalue = resultSet.getDouble( 2 );
        Double score = resultSet.getDouble( 3 );
        Long queryProbe = resultSet.getLong( 4 );
        Long coexpressedProbe = resultSet.getLong( 5 );
        String geneType = resultSet.getString( 6 );
        Long queryGeneId = resultSet.getLong( 7 );

        if ( queryGeneId.equals( coexpressedGene ) ) {
            return;
        }

        Gene queryGene = queryGenes.get( queryGeneId );
        assert queryGene != null : queryGeneId + " did not match given queries";
        CoexpressionCollectionValueObject ccvo = coexpressions.get( queryGene );
        assert ccvo != null;
        addResult( ccvo, eeID, queryGene, queryProbe, pvalue, score, coexpressedGene, geneType, coexpressedProbe );

        /*
         * Cache the result.
         */
        if ( this.getProbe2ProbeCoexpressionCache().isEnabled() ) {
            CoexpressionCacheValueObject coExVOForCache = new CoexpressionCacheValueObject();
            coExVOForCache.setQueryGene( queryGene );
            coExVOForCache.setCoexpressedGene( coexpressedGene );
            coExVOForCache.setGeneType( geneType );
            coExVOForCache.setExpressionExperiment( eeID );
            coExVOForCache.setScore( score );
            coExVOForCache.setPvalue( pvalue );
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
    private void processCS2GeneChunk( Map<Long, Collection<Long>> csId2geneIds, Collection<Long> csIdChunk,
            Session session ) {
        assert csIdChunk.size() > 0;

        /*
         * Check the cache first.
         */
        Collection<Long> neededCs = new HashSet<Long>();
        for ( Long csid : csIdChunk ) {
            if ( gene2CsCache.containsKey( csid ) && gene2CsCache.get( csid ) != null ) {
                csId2geneIds.put( csid, gene2CsCache.get( csid ) );
            } else {
                neededCs.add( csid );
            }
        }

        if ( neededCs.size() == 0 ) {
            return;
        }

        String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS WHERE CS in ("
                + StringUtils.join( neededCs, "," ) + ")";

        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.addScalar( "id", new LongType() );
        queryObject.addScalar( "geneId", new LongType() );

        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( scroll.next() ) {
            Long csid = scroll.getLong( 0 );
            Long geneId = scroll.getLong( 1 );
            if ( !csId2geneIds.containsKey( csid ) ) {
                csId2geneIds.put( csid, new HashSet<Long>() );
            }
            csId2geneIds.get( csid ).add( geneId );
        }

        /*
         * FIXME this could be repetitive.
         */
        gene2CsCache.putAll( csId2geneIds );
    }

    /**
     * For batch queries
     * 
     * @param genes
     * @param ees
     * @param id
     * @param eeIds
     * @param queryString
     * @return
     */
    private org.hibernate.Query setCoexpQueryParameters( Session session, Collection<Gene> genes, long id,
            String queryString ) {
        org.hibernate.SQLQuery queryObject;
        queryObject = session.createSQLQuery( queryString ); // for native query.

        queryObject.addScalar( "id", new LongType() ); // gene out.
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "pvalue", new DoubleType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.addScalar( "geneType", new StringType() );
        queryObject.addScalar( "queryGeneId", new LongType() );

        Collection<Long> ids = new HashSet<Long>();
        for ( Gene gene : genes ) {
            ids.add( gene.getId() );
        }

        queryObject.setParameterList( "ids", ids );

        return queryObject;
    }

    /**
     * @param gene
     * @param ees
     * @param id
     * @param eeIds
     * @param queryString
     * @return
     */
    private org.hibernate.Query setCoexpQueryParameters( Session session, Gene gene, long id, String queryString ) {
        org.hibernate.SQLQuery queryObject;
        queryObject = session.createSQLQuery( queryString ); // for native query.

        queryObject.addScalar( "id", new LongType() ); // gene out.
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "pvalue", new DoubleType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.addScalar( "geneType", new StringType() );
        queryObject.setLong( "id", id );

        return queryObject;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from GeneImpl";
        List r = getHibernateTemplate().find( query );
        return ( Integer ) r.iterator().next();
    }

    @SuppressWarnings( { "unchecked", "cast" })
    @Override
    protected Gene handleFindByAccession( String accession, ExternalDatabase source ) throws Exception {
        Collection<Gene> genes;
        final String accessionQuery = "select g from GeneImpl g inner join g.accessions a where a.accession = :accession";
        final String externalDbquery = accessionQuery + " and a.externalDatabase = :source";

        if ( source == null ) {
            genes = this.getHibernateTemplate().findByNamedParam( accessionQuery, "accession", "accession" );
            if ( genes.size() == 0 ) {
                genes = this.findByNcbiId( accession );
            }
        } else {
            if ( source.getName().equalsIgnoreCase( "NCBI" ) ) {
                genes = this.findByNcbiId( accession );
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByAlias( String search ) throws Exception {
        final String queryString = "select distinct g from GeneImpl as g inner join g.aliases als where als.alias = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        List results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "symbol", "taxon" },
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
    protected Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( final Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly, boolean interGeneOnly )
            throws Exception {

        if ( genes.size() == 0 || ees.size() == 0 ) {
            throw new IllegalArgumentException( "nothing to search" );
        }

        final Map<Gene, CoexpressionCollectionValueObject> coexpressions = new HashMap<Gene, CoexpressionCollectionValueObject>();
        Map<Long, Gene> queryGenes = new HashMap<Long, Gene>();
        for ( Gene g : genes ) {
            queryGenes.put( g.getId(), g );
            coexpressions.put( g, new CoexpressionCollectionValueObject( g, stringency ) );
        }

        if ( genes.size() == 1 ) {
            Gene soleQueryGene = genes.iterator().next();
            coexpressions
                    .put( soleQueryGene, this.getCoexpressedGenes( soleQueryGene, ees, stringency, knownGenesOnly ) );
            return coexpressions;
        }

        /*
         * FIXME: check the cache. This is kind of a pain, because each query gene might have different experiments to
         * consider. So we don't really remove datasets from consideration very readily. An exception might be where
         * interGeneOnly is true.
         */

        /*
         * NOTE: assuming all genes are from the same taxon!
         */
        Gene givenG = genes.iterator().next();
        final long id = givenG.getId();
        log.debug( "Gene: " + givenG.getName() );

        final String p2pClassName = getP2PClassName( givenG );

        final Collection<Long> eeIds = getEEIds( ees );

        String queryString = getNativeBatchQueryString( p2pClassName, "firstVector", "secondVector", eeIds,
                knownGenesOnly, interGeneOnly );

        Session session = this.getSession( false );
        org.hibernate.Query queryObject = setCoexpQueryParameters( session, genes, id, queryString );

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        // This is the actual business of querying the database.
        processCoexpQuery( queryGenes, queryObject, coexpressions );

        overallWatch.stop();
        if ( overallWatch.getTime() > 1000 ) {
            log.info( "Raw query for " + genes.size() + " genes in batch: " + overallWatch.getTime() + "ms" );
        }

        for ( CoexpressionCollectionValueObject coexp : coexpressions.values() ) {
            postProcessSpecificity( knownGenesOnly, coexp );
        }

        return coexpressions;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#getGeneCoexpressionNodeDegree(java.util.Collection, java.util.Collection)
     */
    public Map<Gene, Double> getGeneCoexpressionNodeDegree( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees ) {

        Map<CompositeSequence, Collection<Gene>> cs2GeneMap = CommonQueries.getCs2GeneMap( genes, this.getSession() );

        /*
         * When we aggregate, it's only over data sets that had the gene tested (inner join)
         */
        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "select p.probe, p.nodeDegreeRank from ProbeCoexpressionAnalysisImpl pca "
                        + "join pca.probesUsed p where pca.experimentAnalyzed in (:ees) and p.probe in (:ps)",
                new String[] { "ps", "ees" }, new Object[] { cs2GeneMap.keySet(), ees } );

        Map<Gene, DoubleArrayList> interm = new HashMap<Gene, DoubleArrayList>();
        for ( Gene g : genes ) {
            interm.put( g, new DoubleArrayList() );
        }

        for ( Object o : r ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence cs = ( CompositeSequence ) oa[0];
            Double nodeDegreeRank = ( Double ) oa[1];

            Collection<Gene> gs = cs2GeneMap.get( cs );

            // if ( gs.size() > 1 ) continue; // nonspecific - perhaps control this.
            interm.get( gs.iterator().next() ).add( nodeDegreeRank );
        }

        // aggregate.
        Map<Gene, Double> result = new HashMap<Gene, Double>();
        for ( Gene g : interm.keySet() ) {
            DoubleArrayList vals = interm.get( g );
            result.put( g, Descriptive.mean( vals ) );
        }

        return result;
    }

    @Override
    public Map<BioAssaySet, Double> getGeneCoexpressionNodeDegree( Gene gene, Collection<? extends BioAssaySet> ees ) {
        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession() );

        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "select  pca.experimentAnalyzed, p.nodeDegreeRank from ProbeCoexpressionAnalysisImpl pca "
                        + "join pca.probesUsed p where pca.experimentAnalyzed ee in (:ees) and p.probe in (:ps)",
                new String[] { "ps", "ees" }, new Object[] { probes, ees } );

        Map<BioAssaySet, Double> result = new HashMap<BioAssaySet, Double>();
        for ( Object o : r ) {
            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[1];
            Double nodeDegreeRank = ( Double ) oa[2];
            result.put( ee, nodeDegreeRank );
        }

        return result;

    }

    /**
     * Gets all the genes that are coexpressed with another gene based on stored coexpression 'links', essentially as
     * described in Lee et al. (2004) Genome Research.
     * 
     * @param gene to use as the query
     * @param ees Data sets to restrict the search to.
     * @param stringency minimum number of data sets the coexpression has to occur in before it 'counts'.
     * @param knownGenesOnly
     * @return Collection of CoexpressionCollectionValueObjects. This needs to be 'postprocessed' before it has all the
     *         data needed for web display.
     */
    @Override
    protected CoexpressionCollectionValueObject handleGetCoexpressedGenes( final Gene gene,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly ) throws Exception {

        Gene givenG = gene;
        final long id = givenG.getId();
        log.debug( "Gene: " + gene.getName() );

        final String p2pClassName = getP2PClassName( givenG );

        final CoexpressionCollectionValueObject coexpressions = new CoexpressionCollectionValueObject( gene, stringency );

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
        for ( BioAssaySet ee : ees ) {
            Collection<CoexpressionCacheValueObject> eeResults = this.getProbe2ProbeCoexpressionCache().get( ee, gene );

            if ( eeResults != null ) {
                cachedResults.put( ee.getId(), eeResults );
                if ( log.isDebugEnabled() ) log.debug( "Cache hit! for ee=" + ee.getId() );
            } else {
                eesToSearch.add( ee );
            }
        }
        overallWatch.stop();
        if ( overallWatch.getTime() > 100 ) {
            if ( log.isInfoEnabled() ) log.info( "Probe2probe cache check: " + overallWatch.getTime() + "ms" );
        }
        overallWatch.reset();
        overallWatch.start();

        if ( eesToSearch.size() > 0 ) {

            final Collection<Long> eeIds = getEEIds( eesToSearch );

            String queryString = getNativeQueryString( p2pClassName, "firstVector", "secondVector", eeIds,
                    knownGenesOnly );

            Session session = this.getSession( false );
            org.hibernate.Query queryObject = setCoexpQueryParameters( session, gene, id, queryString );

            // This is the actual business of querying the database.
            processCoexpQuery( gene, queryObject, coexpressions );
        }

        overallWatch.stop();
        if ( overallWatch.getTime() > 1000 ) {
            log.info( "Raw query: " + overallWatch.getTime() + "ms" );
        }

        coexpressions.setDbQuerySeconds( overallWatch.getTime() );
        overallWatch.reset();
        overallWatch.start();
        if ( cachedResults.size() > 0 ) {
            mergeCachedCoexpressionResults( coexpressions, cachedResults );
            overallWatch.stop();
            if ( overallWatch.getTime() > 100 ) {
                log.info( "Merge cached: " + overallWatch.getTime() + "ms" );
            }
            overallWatch.reset();
            overallWatch.start();
        }

        if ( coexpressions.getQueryGeneProbes().size() == 0 ) {
            if ( log.isDebugEnabled() ) log.debug( "Coexpression query gene " + gene + " has no probes" );
            coexpressions.setErrorState( "Query gene " + gene + " has no probes" );
            return coexpressions;
        }

        postProcessSpecificity( knownGenesOnly, coexpressions );

        return coexpressions;
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) throws Exception {
        final String queryString = "select count(distinct cs) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( Long ) r.iterator().next();
    }

    /*
     * Gets all the CompositeSequences related to the gene identified by the given gene and arrayDesign. (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCompositeSequences(ubic.gemma.model.genome.Gene,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) throws Exception {
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequencesById( long id ) throws Exception {
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByTaxon( Taxon taxon ) throws Exception {

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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {

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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleLoadKnownGenes( Taxon taxon ) throws Exception {

        if ( taxon == null ) {
            throw new IllegalArgumentException( "Must provide taxon" );
        }

        final String queryString = "select gene from GeneImpl as gene fetch all properties where gene.taxon = :taxon"
                + " and gene.class = " + CoexpressionCollectionValueObject.GENE_IMPL;

        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        if ( ids.size() == 0 ) {
            return new HashSet();
        }
        int BATCH_SIZE = 2000;
        if ( ids.size() > BATCH_SIZE ) {
            log.info( "Loading " + ids.size() + " genes ..." );
        }

        final String queryString = "select gene from GeneImpl gene where gene.id in (:ids)";
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();

        for ( Long gene : ( Collection<Long> ) ids ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
        }

        if ( ids.size() > BATCH_SIZE ) {
            log.info( "... done" );
        }

        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadPredictedGenes(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<PredictedGene> handleLoadPredictedGenes( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene fetch all properties where gene.taxon = :taxon"
                + " and gene.class = " + CoexpressionCollectionValueObject.PREDICTED_GENE_IMPL;

        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadProbeAlignedRegions(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ProbeAlignedRegion> handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene fetch all properties where gene.taxon = :taxon"
                + " and gene.class = " + CoexpressionCollectionValueObject.PROBE_ALIGNED_REGION_IMPL;

        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleThaw(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Gene handleThaw( final Gene gene ) throws Exception {
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
    protected Collection<Gene> handleThawLite( final Collection<Gene> genes ) throws Exception {
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

}