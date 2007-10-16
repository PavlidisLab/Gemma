package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.ProbeLink;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ProbeAlignedRegionAnalysisCLI extends AbstractSpringAwareCLI {

    private Taxon taxon;
    private double threshold;
    private String outFileName;

    private ExpressionExperimentService eeService;
    private GeneService geneService;

    private Probe2ProbeCoexpressionService p2pService;

    private ArrayDesignService adService;

    public static final double DEFAULT_THRESHOLD = 0.5;

    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "taxon" ).withDescription(
                "the taxon of the genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );
        Option thresholdOption = OptionBuilder.hasArg().withArgName( "threshold" ).withDescription(
                "the rank threshold" ).withLongOpt( "threshold" ).create( 'h' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFileName" ).withDescription(
                "File name for saving the correlation data" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
        Option inputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "inFileName" ).withDescription(
                "Target gene list file name" ).withLongOpt( "inFile" ).create( 'i' );
        addOption( inputFileOption );
    }

    protected void processOptions() {
        super.processOptions();
        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        if ( hasOption( 'o' ) ) {
            this.outFileName = getOptionValue( 'o' );
        }
        if ( hasOption( 'h' ) ) {
            this.threshold = Double.parseDouble( getOptionValue( 'h' ) );
        } else {
            this.threshold = DEFAULT_THRESHOLD;
        }
        initBeans();
    }

    private void initBeans() {
        eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        geneService = ( GeneService ) getBean( "geneService" );
        p2pService = ( Probe2ProbeCoexpressionService ) getBean( "probe2ProbeCoexpressionService" );
    }

    private Map<Long, List<Double>> getParId2eeRankMap( Collection<Long> parIds, Collection<ExpressionExperiment> EEs ) {
        Map<Long, List<Double>> parId2eeRankMap = new HashMap<Long, List<Double>>();
        for ( ExpressionExperiment EE : EEs ) {
            // get quantitation type
            Collection<QuantitationType> qts = ( Collection<QuantitationType> ) eeService
                    .getPreferredQuantitationType( EE );
            if ( qts.size() < 1 ) {
                return null;
            }
            QuantitationType qt = qts.iterator().next();

            // get cs2gene map
            Collection<ArrayDesign> ADs = eeService.getArrayDesignsUsed( EE );
            Collection<Long> csIds = new HashSet<Long>();
            for ( ArrayDesign AD : ADs ) {
                Collection<CompositeSequence> CSs = adService.loadCompositeSequences( AD );
                for ( CompositeSequence CS : CSs ) {
                    csIds.add( CS.getId() );
                }
            }
            Map<Long, Collection<Long>> cs2geneMap = geneService.getCS2GeneMap( csIds );

            Map<DesignElementDataVector, Collection<Long>> dedv2geneMap = eeService.getDesignElementDataVectors(
                    cs2geneMap, qt );
            // invert dedv2geneMap to gene2dedvMap
            Map<Long, Collection<DesignElementDataVector>> gene2dedvMap = new HashMap<Long, Collection<DesignElementDataVector>>();
            for ( DesignElementDataVector dedv : dedv2geneMap.keySet() ) {
                Collection<Long> geneIds = dedv2geneMap.get( dedv );
                for ( Long geneId : geneIds ) {
                    Collection<DesignElementDataVector> dedvs = gene2dedvMap.get( dedv );
                    if ( dedvs == null ) {
                        dedvs = new HashSet<DesignElementDataVector>();
                        gene2dedvMap.put( geneId, dedvs );
                    }
                    dedvs.add( dedv );
                }
            }
            log.info( "Loaded design element data vectors" );

            for ( Long parId : parIds ) {
                List<Double> parRanks = parId2eeRankMap.get( parId );
                if ( parRanks == null ) {
                    parRanks = new ArrayList<Double>( EEs.size() );
                    parId2eeRankMap.put( parId, parRanks );
                }
                parRanks.add( getGeneRank( gene2dedvMap, parId ) );
            }
        }
        return parId2eeRankMap;
    }

    private Double getGeneRank( Map<Long, Collection<DesignElementDataVector>> gene2dedvMap, Long parId ) {
        Collection<DesignElementDataVector> dedvs = gene2dedvMap.get( parId );
        ArrayList<Double> ranks = new ArrayList<Double>();
        for ( DesignElementDataVector dedv : dedvs ) {
            ranks.add( dedv.getRank() );
        }
        Collections.sort( ranks );
        Double rank = ranks.get( ranks.size() / 2 );
        return rank;
    }

    @Override
    protected Exception doWork( String[] args ) {
        Collection<Long> parIds = new HashSet<Long>();
        Collection<Long> knownGeneIds = new HashSet<Long>();
        for ( Gene gene : ( Collection<Gene> ) geneService.getGenesByTaxon( taxon ) ) {
            if ( gene instanceof ProbeAlignedRegion )
                parIds.add( gene.getId() );
            else if ( !( gene instanceof PredictedGene ) ) knownGeneIds.add( gene.getId() );
        }

        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );
        Map<Long, Integer> eeId2IndexMap = getEeId2IndexMap( EEs );

        Map<Long, List<Double>> parId2eeRankMap = getParId2eeRankMap( parIds, EEs );
        removeAboveThreshold( parId2eeRankMap );
        try {
            saveRanksToFile( parId2eeRankMap );
        } catch ( IOException e ) {
            return e;
        }
        parIds = parId2eeRankMap.keySet();

        CompressedNamedBitMatrix linkMatrix = getLinkCountMatrix( EEs, parIds, knownGeneIds );
        CompressedNamedBitMatrix negativeLinkMatrix = getLinkCountMatrix( EEs, parIds, knownGeneIds );

        for ( ExpressionExperiment EE : EEs ) {
            int eeIndex = eeId2IndexMap.get( EE.getId() );
            Collection<ProbeLink> links = p2pService.getProbeCoExpression( EE, taxon.getCommonName(), false );
            if ( links == null || links.size() == 0 ) continue;
            Collection<Long> csIds = new HashSet<Long>();
            for ( ProbeLink link : links ) {
                csIds.add( link.getFirstDesignElementId() );
                csIds.add( link.getSecondDesignElementId() );
            }
            Map<Long, Collection<Long>> cs2geneMap = geneService.getCS2GeneMap( csIds );
            for ( ProbeLink link : links ) {
                Collection<Long> firstGeneIds = cs2geneMap.get( link.getFirstDesignElementId() );
                Collection<Long> secondGeneIds = cs2geneMap.get( link.getSecondDesignElementId() );
                for ( Long firstGeneId : firstGeneIds ) {
                    for ( Long secondGeneId : secondGeneIds ) {
                        int rowIndex = linkMatrix.getRowIndexByName( firstGeneId );
                        int colIndex = linkMatrix.getColIndexByName( secondGeneId );
                        if ( link.getScore() > 0 ) {
                            linkMatrix.set( rowIndex, colIndex, eeIndex );
                        } else {
                            negativeLinkMatrix.set( rowIndex, colIndex, eeIndex );
                        }
                    }
                }
            }
        }
        
        Map<Long, Integer> geneId2LinkCountMap = new HashMap<Long, Integer>();
        for (Long parId : parIds) {
            int i = linkMatrix.getRowIndexByName( parId );
            int[] positiveBitCount = linkMatrix.getRowBitCount( i );
            int[] negativeBitCount = negativeLinkMatrix.getRowBitCount( i );
            for (Long geneId : knownGeneIds) {
                int j = linkMatrix.getColIndexByName( geneId );
                
            }
        }
        
        return null;
    }

    private Map<Integer, Long> getInvertedMap(Map<Long, Integer> map) {
        Map<Integer, Long> invertedMap = new HashMap<Integer, Long>();
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            invertedMap.put( entry.getValue(), entry.getKey() );
        }
        return invertedMap;
    }

    private Map<Long, Integer> getEeId2IndexMap( Collection<ExpressionExperiment> EEs ) {
        Map<Long, Integer> eeId2IndexMap = new HashMap<Long, Integer>();
        int index = 0;
        for ( ExpressionExperiment EE : EEs ) {
            eeId2IndexMap.put( EE.getId(), new Integer( index++ ) );
        }
        return eeId2IndexMap;
    }

    /**
     * Build a bit matrix for storing the link counts
     * 
     * @param EEs expression experiments
     * @param parIds probe aligned region IDs
     * @param knownGeneIds known gene IDs
     * @return
     */
    private CompressedNamedBitMatrix getLinkCountMatrix( Collection<ExpressionExperiment> EEs, Collection<Long> parIds,
            Collection<Long> knownGeneIds ) {
        int n = parIds.size() + knownGeneIds.size();
        CompressedNamedBitMatrix linkCountMatrix = new CompressedNamedBitMatrix( n, n, EEs.size() );
        for ( Long l : parIds ) {
            linkCountMatrix.addColumnName( l );
            linkCountMatrix.addRowName( l );
        }
        for ( Long l : knownGeneIds ) {
            linkCountMatrix.addColumnName( l );
            linkCountMatrix.addRowName( l );
        }
        return linkCountMatrix;
    }

    private void removeAboveThreshold( Map<Long, List<Double>> parId2eeRankMap ) {
        for ( Iterator<List<Double>> it = parId2eeRankMap.values().iterator(); it.hasNext(); ) {
            List<Double> eeRanks = it.next();
            Collections.sort( eeRanks );
            Double rank = eeRanks.get( eeRanks.size() / 4 );
            if ( rank == null || rank < threshold ) it.remove();
        }
    }

    private void saveRanksToFile( Map<Long, List<Double>> parId2eeRankMap ) throws IOException {
        PrintWriter out = new PrintWriter( new FileWriter( outFileName ) );

        for ( Map.Entry<Long, List<Double>> entry : parId2eeRankMap.entrySet() ) {
            Long parId = entry.getKey();
            List<Double> eeRanks = entry.getValue();
            String line = parId.toString();
            for ( Double rank : eeRanks ) {
                line += "\t";
                if ( rank != null ) line += rank;
            }
            out.println( line );
        }
    }

    public static void main( String[] args ) {
        ProbeAlignedRegionAnalysisCLI analysis = new ProbeAlignedRegionAnalysisCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        Exception e = analysis.doWork( args );
        if ( e != null ) log.error( e.getMessage() );
        watch.stop();
        log.info( "Probe aligned region analysis completed in " + watch.getTime() / 1000 + " seconds" );
    }
}
