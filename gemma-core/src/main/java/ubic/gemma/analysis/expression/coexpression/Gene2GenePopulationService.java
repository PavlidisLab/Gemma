/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression;

import hep.aida.bin.QuantileBin1D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cern.colt.list.DoubleArrayList;
import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;
import cern.jet.stat.Descriptive;

import ubic.basecode.dataStructure.BitUtil;
import ubic.basecode.dataStructure.matrix.MatrixUtil;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisImpl;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeService;
import ubic.gemma.model.association.coexpression.HumanGeneCoExpression;
import ubic.gemma.model.association.coexpression.MouseGeneCoExpression;
import ubic.gemma.model.association.coexpression.OtherGeneCoExpression;
import ubic.gemma.model.association.coexpression.RatGeneCoExpression;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;

/**
 * Used to analyze already-persisted probe-level 'links' and turn them into gene-level coexpression information
 * (including node degree). The results are tied to a specific Analysis that can be referred to by clients. In practice
 * only one 'gene2gene' analysis is maintained for each taxon, in which all the datasets for the taxon are used. Results
 * are then filtered to include only the datasets that the user included. The gene2gene analysis for each taxon must be
 * updated periodically to include new datasets.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class Gene2GenePopulationService {
    private static final int BATCH_SIZE = 500;

    private static boolean SINGLE_QUERY_FOR_LINKS = ConfigUtils.getBoolean( "store.gene.coexpression.bothways", true );

    private static Log log = LogFactory.getLog( Gene2GenePopulationService.class.getName() );

    /**
     * @param experimentsAnalyzed
     * @return Map of location in the vector to EE ID.
     */
    public static Map<Integer, Long> getPositionToIdMap( Collection<Long> experimentIdsAnalyzed ) {
        List<Long> eeIds = new ArrayList<Long>( experimentIdsAnalyzed );
        Collections.sort( eeIds );
        Map<Integer, Long> eeOrderId = new HashMap<Integer, Long>();
        int location = 0;
        for ( Long id : eeIds ) {
            eeOrderId.put( location, id );
            location++;
        }
        return eeOrderId;
    }

    /**
     * @param ggc
     * @param eePositionToIdMap
     * @return
     */
    public static Collection<Long> getSpecificExperimentIds( Gene2GeneCoexpression ggc,
            Map<Integer, Long> eePositionToIdMap ) {
        if ( ggc.getSpecificityVector() == null ) {
            return new HashSet<Long>();
        }
        // log.info( BitUtil.prettyPrint( ggc.getSpecificityVector() ) );
        return convertBitVector( eePositionToIdMap, ggc.getSpecificityVector() );
    }

    /**
     * @param ggc
     * @param eePositionToIdMap
     * @return
     */
    public static Collection<Long> getSupportingExperimentIds( Gene2GeneCoexpression ggc,
            Map<Integer, Long> eePositionToIdMap ) {
        return convertBitVector( eePositionToIdMap, ggc.getDatasetsSupportingVector() );
    }

    /**
     * @param ggc
     * @param eePositionToIdMap
     * @return
     */
    public static Collection<Long> getTestedExperimentIds( Gene2GeneCoexpression ggc,
            Map<Integer, Long> eePositionToIdMap ) {
        return convertBitVector( eePositionToIdMap, ggc.getDatasetsTestedVector() );
    }

    /**
     * @param eePositionToIdMap
     * @param bitvector
     * @return
     */
    private static Collection<Long> convertBitVector( Map<Integer, Long> eePositionToIdMap, byte[] bitvector ) {
        List<Long> ids = new ArrayList<Long>();
        for ( int i = 0; i < eePositionToIdMap.size(); i++ ) {
            if ( BitUtil.get( bitvector, i ) ) {
                Long supportingEE = eePositionToIdMap.get( i );
                ids.add( supportingEE );
            }
        }
        return ids;
    }

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private GeneCoexpressionNodeDegreeService geneCoexpressionNodeDegreeService;

    // Caution stateful
    private List<GeneCoexpressionNodeDegree> allGeneNodeDegrees = new ArrayList<GeneCoexpressionNodeDegree>();

    /**
     * Perform a Gene-to-gene analysis for the given experiments and genes, subject to the other parameters. An
     * expressionExperimentSet will be generated.
     * 
     * @param expressionExperiments The experiments to limit the analyiss to.
     * @param toUseGenes The genes to limit the analysis to, as query genes.
     * @param stringency The minimum support before a gene2gene link will be stored
     * @param analysisName Name of the analysis as it will appear in the system
     * @param useDB If false, the results will not be saved to the database but written to stdout.
     */
    public void analyze( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes, int stringency,
            String analysisName, boolean useDB ) {

        log.info( "Initializing gene link analysis ... " );

        Taxon taxon = null;
        Map<Long, Gene> genesToAnalyzeMap = new HashMap<Long, Gene>();
        for ( Gene g : toUseGenes ) {
            if ( taxon == null ) {
                taxon = g.getTaxon();
            } else if ( !taxon.equals( g.getTaxon() ) ) {
                // sanity check.
                throw new IllegalArgumentException( "Cannot analyze genes from multiple taxa" );
            }
            genesToAnalyzeMap.put( g.getId(), g );
        }

        GeneCoexpressionAnalysis analysis = null;
        Collection<GeneCoexpressionAnalysis> oldAnalyses = findExistingAnalysis( taxon );
        if ( useDB ) {

            analysis = intializeNewAnalysis( expressionExperiments, taxon, toUseGenes, analysisName, stringency );
            assert analysis != null;
        }

        doAnalysis( expressionExperiments, toUseGenes, stringency, analysisName, genesToAnalyzeMap, analysis );

        if ( useDB ) {
            // FIXME Small risk here: there may be two enabled analyses until the next call is completed. If it fails we
            // definitely have a problem. Ideally there would be a transaction here...
            disableOldAnalyses( oldAnalyses );
        }
        /*
         * Note that we don't delete the old analysis. That has to be done manually for now -- just in case we need it
         * for something.
         */

    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setProbeLinkCoexpressionAnalyzer( ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer ) {
        this.probeLinkCoexpressionAnalyzer = probeLinkCoexpressionAnalyzer;
    }

    public void setProtocolService( ProtocolService protocolService ) {
        this.protocolService = protocolService;
    }

    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    private static RandomEngine randomNumberGenerator = new DRand( new Date() );

    /**
     * Finalize the node degree computation and persist it.
     * 
     * @param useDB if false, instead of saving to the database it will be printed to stdout.
     */
    private void completeNodeDegreeComputations( boolean useDB ) {

        DoubleArrayList l = new DoubleArrayList();

        for ( GeneCoexpressionNodeDegree n : this.allGeneNodeDegrees ) {
            // l.add( n.getMedian() );
            /*
             * The final ranks are based on the pvalues, not the medians.
             */
            l.add( n.getPvalue() );
        }

        DoubleArrayList ranks = Rank.rankTransform( l );

        QuantileBin1D f = new QuantileBin1D( true, l.size(), 0.0, 0.0, l.size(), randomNumberGenerator );
        f.addAllOf( l );
        log.info( "Finalizing node degree computation" );

        for ( int i = 0; i < this.allGeneNodeDegrees.size(); i++ ) {

            GeneCoexpressionNodeDegree n = this.allGeneNodeDegrees.get( i );
            double rank = ranks.get( i );

            n.setRank( rank / ranks.size() );

            if ( useDB ) {
                geneCoexpressionNodeDegreeService.deleteFor( n.getGene() );
                geneCoexpressionNodeDegreeService.create( n );
            } else {
                System.out.print( String.format( "%d\t%s\t%d\t%.2f\t%.2f\t%.2f\t%d\t%.2f\t%s\n", n.getGene().getId(), n
                        .getGene().getOfficialSymbol(), n.getGene().getTaxon().getId(), n.getMedian(), n
                        .getMedianDeviation(), n.getPvalue(), n.getNumTests(), n.getRank(), n.getDistribution() ) );
            }
        }

    }

    /**
     * @param queryGene
     * @param expressionExperiments
     */
    private void computeNodeDegree( Gene queryGene, Collection<BioAssaySet> expressionExperiments ) {

        Map<BioAssaySet, Double> nodeDegrees = geneService.getGeneCoexpressionNodeDegree( queryGene,
                expressionExperiments );

        if ( nodeDegrees == null || nodeDegrees.isEmpty() ) return;

        /*
         * The values we get are the Fisher's p-values for the node degrees of the individual probe-level data. High
         * values mean high node degree.
         */

        GeneCoexpressionNodeDegree n = GeneCoexpressionNodeDegree.Factory.newInstance();
        n.setGene( queryGene );
        n.setNumTests( nodeDegrees.size() );

        double[] array = ArrayUtils.toPrimitive( nodeDegrees.values().toArray( new Double[] {} ) );
        DoubleArrayList vals = new DoubleArrayList( array );

        /*
         * Because everything is rank transformed, we use the median and mad. Later we re-rank everything anyway.
         */
        double pvalue = MetaAnalysis.fisherCombinePvalues( vals );

        n.setPvalue( pvalue );
        n.setMedian( Descriptive.median( vals ) );
        n.setMedianDeviation( DescriptiveWithMissing.mad( vals ) );

        String distribution = computeDistribution( vals );
        n.setDistribution( distribution );

        // so we can compute summary stats at the end.
        this.allGeneNodeDegrees.add( n );

    }

    /**
     * Compute a string to represent the distribution of node degree values.
     * 
     * @param vals
     * @return
     */
    private String computeDistribution( DoubleArrayList vals ) {
        /*
         * Set up the distribution
         */
        int numBins = 10;
        Histogram h = new Histogram( "", numBins, 0.0, 1.0 );
        h.fill( MatrixUtil.fromList( vals ) );
        int tallestBar = h.getBiggestBinSize();
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < numBins; i++ ) {
            int f = ( int ) Math.floor( 9.0 * h.getArray()[i] / tallestBar );
            assert f < 10; // one digit
            buf.append( f );
        }
        String distribution = buf.toString();
        return distribution;
    }

    /**
     * Create a vector representing the datasets which had specific probes for the query and target genes. A '1' means
     * it did, '0' means it did not.
     * 
     * @param nonspecificEE
     * @param eeIdOrder
     * @return
     */
    private byte[] computeSpecificityVector( Collection<Long> nonspecificEE, Map<Long, Integer> eeIdOrder ) {

        assert nonspecificEE.size() <= eeIdOrder.size();

        byte[] result = new byte[( int ) Math.ceil( eeIdOrder.size() / ( double ) Byte.SIZE )];
        /*
         * Start initialized with 0's (might not be necessary...)
         */
        for ( int i = 0, j = result.length; i < j; i++ ) {
            result[i] = 0x0;
        }

        /*
         * Set the bits we're using to 1.
         */
        for ( int i = 0; i < eeIdOrder.size(); i++ ) {
            BitUtil.set( result, i );
        }

        /*
         * Set it so 1=specific 0=nonspecific
         */
        for ( Long id : nonspecificEE ) {
            BitUtil.clear( result, eeIdOrder.get( id ) );
        }

        assert BitUtil.count( result ) == eeIdOrder.size() - nonspecificEE.size() : "Got " + BitUtil.count( result )
                + " ones, expected " + ( eeIdOrder.size() - nonspecificEE.size() );
        return result;
    }

    /**
     * Algorithm:
     * <ol>
     * <li>Initialize byte array large enough to hold all the EE information (ceil(numeeids /Byte.SIZE))
     * <li>Flip the bit at the right location.
     * </ol>
     * 
     * @param idsToFlip
     * @param eeIdOrder
     * @return
     */
    private byte[] computeSupportingDatasetVector( Collection<Long> idsToFlip, Map<Long, Integer> eeIdOrder ) {
        byte[] supportVector = new byte[( int ) Math.ceil( eeIdOrder.size() / ( double ) Byte.SIZE )];
        for ( int i = 0, j = supportVector.length; i < j; i++ ) {
            supportVector[i] = 0x0;
        }

        for ( Long id : idsToFlip ) {
            BitUtil.set( supportVector, eeIdOrder.get( id ) );
        }
        assert BitUtil.count( supportVector ) == idsToFlip.size();
        return supportVector;
    }

    /**
     * @param datasetsTestedIn
     * @param eeIdOrder
     * @return
     */
    private byte[] computeTestedDatasetVector( Collection<Long> datasetsTestedIn, Map<Long, Integer> eeIdOrder ) {
        // initialize.
        byte[] result = new byte[( int ) Math.ceil( eeIdOrder.size() / ( double ) Byte.SIZE )];
        for ( int i = 0, j = result.length; i < j; i++ ) {
            result[i] = 0x0;
        }

        for ( Long id : datasetsTestedIn ) {
            BitUtil.set( result, eeIdOrder.get( id ) );
        }
        assert BitUtil.count( result ) == datasetsTestedIn.size();
        return result;
    }

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @return
     */
    private Protocol createProtocol( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes ) {
        log.info( "Creating protocol object ... " );
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Stored Gene2GeneCoexpressions" );
        protocol.setDescription( "Using: " + expressionExperiments.size() + " Expression Experiments,  "
                + toUseGenes.size() + " Genes" );
        protocol = protocolService.findOrCreate( protocol );
        return protocol;
    }

    /**
     * @param oldAnalyses
     */
    private void disableOldAnalyses( Collection<GeneCoexpressionAnalysis> oldAnalyses ) {
        if ( !oldAnalyses.isEmpty() ) {
            for ( GeneCoexpressionAnalysis oldAnalysis : oldAnalyses ) {
                log.info( "Disabling old analysis: " + oldAnalysis );
                oldAnalysis.setEnabled( false );
                geneCoexpressionAnalysisService.update( oldAnalysis );
            }

        }
    }

    /**
     * Populate the node degree table, without analyzing coexpression. This method is public simply to allow
     * initialization of the table
     * 
     * @param expressionExperiments
     * @param toUseGenes
     */
    public void nodeDegreeAnalysis( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes,
            boolean useDB ) {
        allGeneNodeDegrees.clear();
        int count = 0;
        for ( Gene queryGene : toUseGenes ) {
            computeNodeDegree( queryGene, expressionExperiments );
            if ( count++ % 200 == 0 ) {
                log.info( count + "/" + toUseGenes.size() + " genes analyzed for node degree" );
            }
        }
        log.info( "Finalizing node degree computation" );
        completeNodeDegreeComputations( useDB );
    }

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @param stringency
     * @param analysisName
     * @param genesToAnalyzeMap
     * @param analysis if null, no results will be saved to the database, and will be printed to stdout instead.
     * @return genes that were processed.
     */
    private Collection<Gene> doAnalysis( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes,
            int stringency, String analysisName, Map<Long, Gene> genesToAnalyzeMap, GeneCoexpressionAnalysis analysis ) {
        int totalLinks = 0;
        Collection<Gene> processedGenes = new HashSet<Gene>();
        Map<Long, Integer> eeIdOrder = ProbeLinkCoexpressionAnalyzer.getOrderingMap( expressionExperiments );

        log.info( "Starting gene link analysis '" + analysisName + " on " + toUseGenes.size() + " genes in "
                + expressionExperiments.size() + " experiments with a stringency of " + stringency );

        try {
            for ( Gene queryGene : toUseGenes ) {

                // Do it
                CoexpressionCollectionValueObject coexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis(
                        queryGene, expressionExperiments, stringency, 0 );

                // persist it or write out

                StopWatch timer = new StopWatch();
                if ( analysis != null ) {
                    timer.start();
                    Collection<Gene2GeneCoexpression> created = persistCoexpressions( eeIdOrder, queryGene,
                            coexpressions, analysis, genesToAnalyzeMap, processedGenes, stringency );
                    totalLinks += created.size();
                    timer.stop();
                    if ( timer.getTime() > 1000 ) {
                        log.info( "Persist links: " + timer.getTime() + "ms" );
                    }
                } else {
                    List<CoexpressionValueObject> coexps = coexpressions.getAllGeneCoexpressionData( stringency );
                    int usedLinks = 0;
                    for ( CoexpressionValueObject co : coexps ) {
                        if ( !genesToAnalyzeMap.containsKey( co.getGeneId() ) ) {
                            continue;
                        }
                        Gene secondGene = genesToAnalyzeMap.get( co.getGeneId() );
                        if ( processedGenes.contains( secondGene ) ) {
                            continue;
                        }
                        usedLinks++;
                        totalLinks++;
                        System.out.println( format( co ) );
                    }
                    if ( usedLinks > 0 ) log.info( usedLinks + " links printed for " + queryGene );
                }

                computeNodeDegree( queryGene, expressionExperiments );

                processedGenes.add( queryGene );
                if ( processedGenes.size() % 100 == 0 ) {
                    log.info( "Processed " + processedGenes.size() + " genes..." );
                }
            } // end loop over genes

            if ( analysis != null ) {
                // All done...
                analysis.setDescription( analysis.getDescription() + "; " + totalLinks + " gene pairs stored." );
                analysis.setEnabled( true );
                geneCoexpressionAnalysisService.update( analysis );

                securityService.makePublic( analysis );
            }

            completeNodeDegreeComputations( analysis != null );

            log.info( totalLinks + " gene pairs processed." );

        } catch ( Exception e ) {
            log.error( "There was an error during analysis!" );

            if ( analysis != null ) {
                log.error( "Cleaning up if possible ..." );
                geneCoexpressionAnalysisService.delete( analysis );
            }
            throw new RuntimeException( e );
        }
        return processedGenes;
    }

    private Collection<GeneCoexpressionAnalysis> findExistingAnalysis( Taxon taxon ) {
        /*
         * Find the old analysis so we can disable it afterwards
         */

        Collection<? extends Analysis> analysis = null;
        if ( taxon.getIsSpecies() ) {
            analysis = geneCoexpressionAnalysisService.findByTaxon( taxon );
        } else {
            analysis = geneCoexpressionAnalysisService.findByParentTaxon( taxon );
        }

        Collection<GeneCoexpressionAnalysis> oldAnalyses = new HashSet<GeneCoexpressionAnalysis>();

        for ( Analysis a : ( Collection<? extends Analysis> ) analysis ) {
            assert a instanceof GeneCoexpressionAnalysis;
            oldAnalyses.add( ( GeneCoexpressionAnalysis ) a );

        }

        log.info( "Found " + oldAnalyses.size() + " analyses that will be disabled when this one is finished" );
        return oldAnalyses;

    }

    /**
     * Check for an old expression experiment set that has the same name as the one we want to use. If it exists, check
     * if it contains exactly the experiments we want to use. If it does, return it. Otherwise rename the old one and
     * return null.
     * 
     * @param analysisName
     * @param expressionExperiments
     * @return expressionExperimentSet if a usable old one exists.
     */
    private ExpressionExperimentSet findOrFlagOldEESets( String analysisName,
            Collection<BioAssaySet> expressionExperiments ) {
        Collection<ExpressionExperimentSet> oldEESets = expressionExperimentSetService.findByName( analysisName );

        if ( oldEESets.size() > 0 ) {
            if ( oldEESets.size() > 1 ) {
                // uh-oh.
            } else {
                ExpressionExperimentSet oldSet = oldEESets.iterator().next();

                if ( oldSet.getExperiments().containsAll( expressionExperiments )
                        && oldSet.getExperiments().size() == expressionExperiments.size() ) {
                    log.info( "Reusing an old EE set" );
                    return oldSet;
                }

                log.info( "Flagging old EEset '" + oldSet.getName() + "'as 'old'" );
                oldSet.setName( oldSet.getName() + " (old)" );
                expressionExperimentSetService.update( oldSet );
                return null;

            }
        }
        log.info( "New EESet will be created" );
        return null;
    }

    private String format( CoexpressionValueObject co ) {
        StringBuilder buf = new StringBuilder();

        buf.append( co.getQueryGene().getId() + "\t" );
        buf.append( co.getQueryGene().getOfficialSymbol() + "\t" );
        buf.append( co.getGeneId() + "\t" );
        buf.append( co.getGeneName() + "\t" ); // this is not populated.
        buf.append( co.getNumDatasetsTestedIn() + "\t" );
        buf.append( co.getPositiveLinkSupport() + "\t" );
        buf.append( co.getNegativeLinkSupport() + "\t" );
        buf.append( BitUtil.prettyPrint( co.getDatasetsTestedInBytes() ) );

        return buf.toString();
    }

    /**
     * @param taxon
     * @return
     */
    private Gene2GeneCoexpression getNewGGCOInstance( Taxon taxon ) {
        Gene2GeneCoexpression g2gCoexpression;
        if ( taxon.getCommonName().equalsIgnoreCase( "mouse" ) )
            g2gCoexpression = MouseGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "rat" ) )
            g2gCoexpression = RatGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "human" ) )
            g2gCoexpression = HumanGeneCoExpression.Factory.newInstance();
        else
            g2gCoexpression = OtherGeneCoExpression.Factory.newInstance();
        return g2gCoexpression;
    }

    /**
     * @param expressionExperiments
     * @param taxon
     * @param toUseGenes
     * @param analysisName
     * @param stringency
     * @return
     */
    private GeneCoexpressionAnalysis intializeNewAnalysis( Collection<BioAssaySet> expressionExperiments, Taxon taxon,
            Collection<Gene> toUseGenes, String analysisName, int stringency ) {
        GeneCoexpressionAnalysisImpl analysis = ( GeneCoexpressionAnalysisImpl ) GeneCoexpressionAnalysis.Factory
                .newInstance();

        analysis.setDescription( "Coexpression analysis for " + taxon.getCommonName() + " using "
                + expressionExperiments.size() + " expression experiments; stringency=" + stringency );

        Protocol protocol = createProtocol( expressionExperiments, toUseGenes );

        analysis.setTaxon( taxon );
        analysis.setStringency( stringency );
        analysis.setName( analysisName );
        analysis.setDescription( "Coexpression analysis of " + expressionExperiments.size() + " EEs from "
                + taxon.getCommonName() );
        analysis.setProtocol( protocol );
        analysis.setEnabled( false );

        ExpressionExperimentSet eeSet = findOrFlagOldEESets( analysisName, expressionExperiments );

        /*
         * If a set matches exactly, we just reuse it. Otherwise we create a new one.
         */
        if ( eeSet == null ) {
            eeSet = ExpressionExperimentSet.Factory.newInstance();
            eeSet.setTaxon( taxon );
            eeSet.setName( analysisName );
            eeSet.setDescription( "Automatically generated for " + expressionExperiments.size() + " EEs" );
            eeSet.setExperiments( expressionExperiments );
        }

        analysis.setExpressionExperimentSetAnalyzed( eeSet );

        analysis = ( GeneCoexpressionAnalysisImpl ) persisterHelper.persist( analysis );

        securityService.makePrivate( analysis );
        log.info( "Done" );
        return analysis;
    }

    /**
     * @param eeIdOrder map for creating bit vector data.
     * @param firstGene the query gene
     * @param toPersist results to persist
     * @param analysis Analysis object to associate with links
     * @param genesToAnalyze list of genes to limit analysis for (links to other genes are ignored)
     * @param alreadyPersisted to track genes we've already done, so we don't persist links in both directions
     * @param stringency minimum support to store a link.
     * @return
     */
    private Collection<Gene2GeneCoexpression> persistCoexpressions( Map<Long, Integer> eeIdOrder, Gene firstGene,
            CoexpressionCollectionValueObject toPersist, GeneCoexpressionAnalysis analysis,
            final Map<Long, Gene> genesToAnalyze, final Collection<Gene> alreadyPersisted, int stringency ) {

        assert analysis != null;

        Taxon taxon = firstGene.getTaxon();

        Collection<Gene2GeneCoexpression> all = new ArrayList<Gene2GeneCoexpression>();
        Collection<Gene2GeneCoexpression> batch = new ArrayList<Gene2GeneCoexpression>();

        for ( CoexpressionValueObject co : toPersist.getAllGeneCoexpressionData( stringency ) ) {

            if ( !genesToAnalyze.containsKey( co.getGeneId() ) ) {
                if ( log.isDebugEnabled() )
                    log.debug( "coexpressed Gene " + co.getGeneId() + " " + co.getGeneName()
                            + " is not among the genes selected for analysis, so it will be skipped (while analyzing "
                            + firstGene.getOfficialSymbol() + ")" );
                continue;
            }

            Gene secondGene = genesToAnalyze.get( co.getGeneId() );

            // note we just check the id to avoid any problems with thaw etc.
            if ( secondGene.getId().equals( firstGene.getId() ) ) {
                /*
                 * This check is 'just in case'; they should have been removed earlier. These can leak in because we 1)
                 * there can be two probes for the same genes that are coexpressed and 2) we allow them in initial query
                 * results
                 */
                continue;
            }

            /*
             * If we store the links in both directions, only one query is needed for retrieval.
             */
            if ( !SINGLE_QUERY_FOR_LINKS && alreadyPersisted.contains( secondGene ) ) continue;

            if ( log.isDebugEnabled() )
                log.debug( firstGene.getName() + " link to " + secondGene.getName() + " tested in "
                        + co.getDatasetsTestedIn().size() + " datasets" );

            byte[] testedInVector = co.getDatasetsTestedInBytes();
            if ( testedInVector == null ) {
                testedInVector = computeTestedDatasetVector( co.getDatasetsTestedIn(), eeIdOrder );
            }

            byte[] specificityVector = computeSpecificityVector( co.getNonspecificEE(), eeIdOrder );

            /*
             * Note that we are storing the 'raw' link support, which includes 'non-specific' probes. These can be
             * filtered later.
             */
            if ( co.getNegativeLinkSupport() >= stringency ) {
                Gene2GeneCoexpression g2gCoexpression = getNewGGCOInstance( taxon );
                g2gCoexpression.setDatasetsTestedVector( testedInVector );
                g2gCoexpression.setSpecificityVector( specificityVector );
                g2gCoexpression.setSourceAnalysis( analysis );
                g2gCoexpression.setFirstGene( firstGene );
                g2gCoexpression.setSecondGene( secondGene );
                g2gCoexpression.setPvalue( co.getNegPValue() );

                Collection<Long> contributing2NegativeLinks = co.getEEContributing2NegativeLinks();
                assert contributing2NegativeLinks.size() == co.getNegativeLinkSupport();
                byte[] supportVector = computeSupportingDatasetVector( contributing2NegativeLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getNegativeLinkSupport() );
                g2gCoexpression.setEffect( co.getNegativeScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );

                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
                    batch.clear();
                }
            }

            if ( co.getPositiveLinkSupport() >= stringency ) {
                Gene2GeneCoexpression g2gCoexpression = getNewGGCOInstance( taxon );
                g2gCoexpression.setDatasetsTestedVector( testedInVector );
                g2gCoexpression.setSpecificityVector( specificityVector );
                g2gCoexpression.setSourceAnalysis( analysis );
                g2gCoexpression.setFirstGene( firstGene );
                g2gCoexpression.setSecondGene( secondGene );
                g2gCoexpression.setPvalue( co.getPosPValue() );

                Collection<Long> contributing2PositiveLinks = co.getEEContributing2PositiveLinks();
                assert contributing2PositiveLinks.size() == co.getPositiveLinkSupport();
                byte[] supportVector = computeSupportingDatasetVector( contributing2PositiveLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getPositiveLinkSupport() );
                g2gCoexpression.setEffect( co.getPositiveScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );
                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
                    batch.clear();
                }
            }

            if ( log.isDebugEnabled() )
                log.debug( "Persisted: " + firstGene.getOfficialSymbol() + " --> " + secondGene.getOfficialSymbol()
                        + " ( " + co.getNegativeScore() + " , +" + co.getPositiveScore() + " )" );
        }

        if ( batch.size() > 0 ) {
            all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
            batch.clear();
        }
        if ( all.size() > 0 ) {
            log.info( "Persisted " + all.size() + " gene2geneCoexpressions for " + firstGene.getName()
                    + " in analysis: " + analysis.getName() );
        }
        return all;

    }
}
