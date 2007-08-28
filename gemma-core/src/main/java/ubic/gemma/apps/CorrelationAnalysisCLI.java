package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService;
import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService.CoexpressionMatrices;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

public class CorrelationAnalysisCLI extends AbstractGeneCoexpressionManipulatingCLI {
    private String outFilePrefix;

    private CoexpressionAnalysisService coexpressionAnalysisService;
    
    private FilterConfig filterConfig;

    public CorrelationAnalysisCLI() {
        super();
        filterConfig = new FilterConfig();  
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
        coexpressionAnalysisService = ( CoexpressionAnalysisService ) this.getBean( "effectSizeService" );
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
        CoexpressionMatrices matrices = coexpressionAnalysisService.calculateCoexpressionMatrices( ees, queryGenes, targetGenes, filterConfig );
        DenseDoubleMatrix3DNamed correlationMatrix = matrices.getCorrelationMatrix();
        
        DenseDoubleMatrix2DNamed maxCorrelationMatrix = coexpressionAnalysisService
                .getMaxCorrelationMatrix( correlationMatrix, 0 );
        DenseDoubleMatrix2DNamed correlationMatrix2D = coexpressionAnalysisService.foldCoexpressionMatrix( correlationMatrix );
        // create row/col name maps
        Map<String, String> geneIdPair2NameMap = getGeneIdPair2NameMap( queryGenes, targetGenes );
        Map<Long, String> qGeneId2NameMap = new HashMap<Long, String>();
        for (Gene gene : queryGenes)
        	qGeneId2NameMap.put(gene.getId(), gene.getOfficialSymbol());
        Map<Long, String> tGeneId2NameMap = new HashMap<Long, String>();
        for (Gene gene : targetGenes)
        	tGeneId2NameMap.put(gene.getId(), gene.getOfficialSymbol());
        Map<Long, String> eeId2NameMap = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : ees )
            eeId2NameMap.put( ee.getId(), ee.getShortName() );
        
        String topLeft = "GenePair";
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
        formatter.applyPattern("0.0000");
        Map<String, String> valMap = new HashMap<String, String>();
        valMap.put(formatter.format(Double.NaN), "");
        try {
            MatrixWriter out = new MatrixWriter( outFilePrefix + ".corr.txt", formatter, geneIdPair2NameMap, eeId2NameMap, valMap);
            out.writeMatrix( correlationMatrix2D, topLeft );
            out.close();
            
            out = new MatrixWriter( outFilePrefix + ".max_corr.txt", formatter ,  qGeneId2NameMap, tGeneId2NameMap, valMap);
            out.writeMatrix( maxCorrelationMatrix );
            out.close();
            
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    public static void main( String[] args ) {
        CorrelationAnalysisCLI analysis = new CorrelationAnalysisCLI();
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
