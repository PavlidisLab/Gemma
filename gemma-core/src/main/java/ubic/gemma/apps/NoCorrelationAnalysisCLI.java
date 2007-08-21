package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService.CoexpressionMatrices;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

public class NoCorrelationAnalysisCLI extends AbstractGeneCoexpressionManipulatingCLI {
    private String outFilePrefix;

    private EffectSizeService effectSizeService;

    public NoCorrelationAnalysisCLI() {
        super();
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" ).withDescription(
                "File prefix for saving the output" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'o' ) ) {
            this.outFilePrefix = getOptionValue( 'o' );
        }
        initBeans();
    }

    protected void initBeans() {
        effectSizeService = ( EffectSizeService ) this.getBean( "effectSizeService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "NoCorrelationAnalysis", args );
        if ( exc != null ) {
            return exc;
        }

        Collection<ExpressionExperiment> ees;
        Collection<Gene> queryGenes, targetGenes;
        try {
            ees = getExpressionExperiments(taxon);
            queryGenes = getQueryGenes();
            targetGenes = getTargetGenes();
        } catch ( IOException e ) {
            return e;
        }

        // calculate matrices
        CoexpressionMatrices matrices = effectSizeService.calculateCoexpressionMatrices( ees, queryGenes, targetGenes );
        DenseDoubleMatrix3DNamed correlationMatrix = matrices.getCorrelationMatrix();
        DenseDoubleMatrix3DNamed randCorrelationMatrix = effectSizeService
                .calculateRandomCorrelationMatrix( correlationMatrix );
        DenseDoubleMatrix2DNamed foldedCorrelationMatrix = effectSizeService.foldCoexpressionMatrix( correlationMatrix );
        DenseDoubleMatrix2DNamed foldedRandCorrelationMatrix = effectSizeService
                .foldCoexpressionMatrix( randCorrelationMatrix );
        DenseDoubleMatrix2DNamed maxCorrelationMatrix = effectSizeService
                .getMaxCorrelationMatrix( correlationMatrix, 0 );
        DenseDoubleMatrix2DNamed maxRandCorrelationMatrix = effectSizeService.getMaxCorrelationMatrix(
                randCorrelationMatrix, 0 );
        
        // create row/col name maps
        Map<String, String> geneIdPair2nameMap = getGeneIdPair2nameMap( queryGenes, targetGenes );
        Map<Long, String> eeId2nameMap = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : ees )
            eeId2nameMap.put( ee.getId(), ee.getShortName() );

        String topLeft = "GenePair";
        DecimalFormat formatter = new DecimalFormat( "0.0000" );
        try {
            MatrixWriter out = new MatrixWriter( outFilePrefix + ".corr.txt", formatter, eeId2nameMap, geneIdPair2nameMap);
            out.writeMatrix( foldedCorrelationMatrix, topLeft );
            out.close();
            
            out = new MatrixWriter( outFilePrefix + ".rand_corr.txt", formatter, eeId2nameMap, geneIdPair2nameMap );
            out.writeMatrix( foldedRandCorrelationMatrix, topLeft );
            out.close();

            out = new MatrixWriter( outFilePrefix + ".max_corr.txt", formatter , eeId2nameMap, geneIdPair2nameMap);
            out.writeMatrix( maxCorrelationMatrix );
            out.close();
            
            out = new MatrixWriter( outFilePrefix + ".max_rand_corr.txt", formatter , eeId2nameMap, geneIdPair2nameMap);
            out.writeMatrix( maxRandCorrelationMatrix );
            out.close();
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    public static void main( String[] args ) {
        NoCorrelationAnalysisCLI analysis = new NoCorrelationAnalysisCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting No Correlation Analysis" );
        Exception exc = analysis.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
        log.info( "Finished analysis in " + watch.getTime() / 1000 + " seconds" );
    }
}
