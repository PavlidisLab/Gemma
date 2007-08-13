package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

public class NoCorrelationAnalysisCLI extends AbstractGeneManipulatingCLI {
    private String queryGeneListFile;
    private String targetGeneListFile;

    private String outFilePrefix;

    private Taxon taxon;

    private EffectSizeService effectSizeService;

    private ExpressionExperimentService eeService;

    private GeneService geneService;

    public NoCorrelationAnalysisCLI() {
        super();
    }

    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "queryGeneFile" ).withDescription(
                "Query file containing list of gene offical symbols" ).withLongOpt( "queryGeneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option partnerFileOption = OptionBuilder.hasArg().isRequired().withArgName( "partnerGeneFile" )
                .withDescription( "File containing list of target gene offical symbols" ).withLongOpt(
                        "partnerGeneFile" ).create( 'a' );
        addOption( partnerFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" ).withDescription(
                "File prefix for saving the output" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.queryGeneListFile = getOptionValue( 'g' );
        }
        if ( hasOption( 'a' ) ) {
            this.targetGeneListFile = getOptionValue( 'a' );
        }
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            taxon = Taxon.Factory.newInstance();
            taxon.setCommonName( taxonName );
            TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
            taxon = taxonService.find( taxon );
            if ( taxon == null ) {
                log.info( "No Taxon found!" );
            }
        }
        if ( hasOption( 'o' ) ) {
            this.outFilePrefix = getOptionValue( 'o' );
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
        Exception exc = processCommandLine( "NoCorrelationAnalysis", args );
        if ( exc != null ) {
            return exc;
        }

        Collection<ExpressionExperiment> allEEs = eeService.findByTaxon( taxon );
        Collection<ExpressionExperiment> EEs = new ArrayList<ExpressionExperiment>();
        for ( ExpressionExperiment ee : allEEs ) {
            if ( ee.getShortName().equals( "GSE7529" ) ) {
                log.info( "Removing expression experiment GSE7529" );
            } else {
                EEs.add( ee );
            }
        }

        List<Long> queryGeneIds = new ArrayList<Long>();
        List<Long> targetGeneIds = new ArrayList<Long>();
        try {
            queryGeneIds.addAll( readGeneListFileToIds( queryGeneListFile, taxon ) );
            targetGeneIds.addAll( readGeneListFileToIds( targetGeneListFile, taxon ) );
        } catch ( IOException e ) {
            return e;
        }

        CoexpressionMatrices matrices = effectSizeService.calculateCoexpressionMatrices( EEs, queryGeneIds, targetGeneIds );
        DenseDoubleMatrix3DNamed correlationMatrix = matrices.getCorrelationMatrix();
        DenseDoubleMatrix3DNamed randCorrelationMatrix = effectSizeService.calculateRandomCorrelationMatrix( correlationMatrix);
        DenseDoubleMatrix2DNamed foldedCorrelationMatrix = effectSizeService.foldCoexpressionMatrix(correlationMatrix);
        DenseDoubleMatrix2DNamed foldedRandCorrelationMatrix = effectSizeService.foldCoexpressionMatrix(randCorrelationMatrix);
        DenseDoubleMatrix2DNamed maxCorrelationMatrix = effectSizeService.getMaxCorrelationMatrix(correlationMatrix, 0);
        DenseDoubleMatrix2DNamed maxRandCorrelationMatrix = effectSizeService.getMaxCorrelationMatrix(randCorrelationMatrix, 0);
        try {
        	String topLeft = "GenePair";
        	DecimalFormat formatter = new DecimalFormat("0.0000");
        	MatrixWriter out = new MatrixWriter(outFilePrefix + ".corr.txt", formatter);
        	out.writeMatrix(foldedCorrelationMatrix, topLeft);
        	out.close();
        	out = new MatrixWriter(outFilePrefix + ".rand_corr.txt", formatter);
        	out.writeMatrix(foldedRandCorrelationMatrix, topLeft);
        	out.close();
        	
        	out = new MatrixWriter(outFilePrefix + ".max_corr.txt", formatter);
        	out.writeMatrix(maxCorrelationMatrix);
        	out.close();
        	out = new MatrixWriter(outFilePrefix + ".max_rand_corr.txt", formatter);
        	out.writeMatrix(maxRandCorrelationMatrix);
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
