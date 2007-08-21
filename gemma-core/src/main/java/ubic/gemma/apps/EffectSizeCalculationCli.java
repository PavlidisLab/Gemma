package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService.CoexpressionMatrices;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;

/**
 * Calculate the effect size
 * 
 * @author xwan
 * @author raymond
 */
public class EffectSizeCalculationCli extends AbstractGeneCoexpressionManipulatingCLI {
    private String goTerm;

    private String outFilePrefix;

    private Taxon taxon;

    private EffectSizeService effectSizeService;

    private ExpressionExperimentService eeService;

    private GeneService geneService;
    private GeneOntologyService goService;

    private int stringency = 3;

    public static final int DEFAULT_STRINGENCY = 3;

    public EffectSizeCalculationCli() {
        super();
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option goOption = OptionBuilder.hasArg().withArgName( "GOTerm" ).withDescription( "Target GO term" )
                .withLongOpt( "GOTerm" ).create( 'g' );
        addOption( goOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" ).withDescription(
                "File prefix for saving the correlation data" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "Vote count stringency for link selection" ).withLongOpt( "stringency" ).create( 'r' );
        addOption( stringencyOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.goTerm = getOptionValue( 'g' );
        }
        if ( hasOption( 'g' ) ) {
            this.goTerm = getOptionValue( 'g' );
        }
        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        if ( hasOption( 'o' ) ) {
            this.outFilePrefix = getOptionValue( 'o' );
        }
        if ( hasOption( 'r' ) ) {
            this.stringency = Integer.parseInt( getOptionValue( 'r' ) );
        } else {
            this.stringency = DEFAULT_STRINGENCY;
        }

        initBeans();
    }

    protected void initBeans() {
        effectSizeService = ( EffectSizeService ) this.getBean( "effectSizeService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "EffectSizeCalculation ", args );
        if ( exc != null ) {
            return exc;
        }
        StopWatch watch = new StopWatch();
        watch.start();

        Collection<ExpressionExperiment> ees;
        Collection<Gene> queryGenes, targetGenes;
        try {
            ees = getExpressionExperiments( taxon );
            queryGenes = getQueryGenes();
            targetGenes = getTargetGenes();
        } catch ( IOException e ) {
            return e;
        }
        if ( goTerm != null ) {
            while ( !goService.isReady() ) {
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException e ) {
                }
            }
            targetGenes.addAll( goService.getGenes( goTerm, taxon ) );
        }

        if ( targetGenes.size() == 0 || queryGenes.size() == 0 ) {
            return new Exception( "No genes in query/target" );
        }

        CoexpressionMatrices matrices = effectSizeService.calculateCoexpressionMatrices( ees, queryGenes, targetGenes );
        DenseDoubleMatrix3DNamed correlationMatrix = matrices.getCorrelationMatrix();
        DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices.getSampleSizeMatrix();
        DenseDoubleMatrix2DNamed effectSizeMatrix = effectSizeService.calculateEffectSizeMatrix( correlationMatrix, sampleSizeMatrix );
        DenseDoubleMatrix2DNamed correlationMatrix2D = effectSizeService.foldCoexpressionMatrix( correlationMatrix );
        
        // create 2D correlation heat map
        ColorMatrix dataColorMatrix = new ColorMatrix( correlationMatrix2D );
        dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
        JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
        String figureFileName = outFilePrefix + ".corr.png";
        
        // create row/col name maps
        Map<String, String> geneIdPair2nameMap = getGeneIdPair2nameMap( queryGenes, targetGenes );
        Map<Long, String> eeId2nameMap = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : ees )
            eeId2nameMap.put( ee.getId(), ee.getShortName() );

        Format formatter = new DecimalFormat("0.0000");
        String topLeft = "GenePair";
        try {
            MatrixWriter out = new MatrixWriter( outFilePrefix + ".corr.txt", formatter, eeId2nameMap, geneIdPair2nameMap);
            out.writeMatrix( correlationMatrix2D, topLeft );
            out.close();
            
            out = new MatrixWriter( outFilePrefix + ".effect_size.txt", formatter, eeId2nameMap, geneIdPair2nameMap);
            out.writeMatrix( effectSizeMatrix, topLeft );
            out.close();
            
            dataMatrixDisplay.saveImage( figureFileName, true );
            log.info( "Saved correlation image to " + figureFileName );
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    public static void main( String[] args ) {
        EffectSizeCalculationCli analysis = new EffectSizeCalculationCli();
        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting Effect Size Analysis" );
        Exception exc = analysis.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
        log.info( "Finished analysis in " + watch.getTime() / 1000 + " seconds" );
    }
}