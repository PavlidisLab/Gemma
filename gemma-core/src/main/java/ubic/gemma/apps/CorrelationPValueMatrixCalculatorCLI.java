package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;

/**
 * CLI for reading in a max correlation matrix to calculate p values from correlation histograms
 * 
 * @author raymond
 */
public class CorrelationPValueMatrixCalculatorCLI extends ExpressionExperimentManipulatingCLI {

    private String inFile;
    private String outFile;

    private CoexpressionAnalysisService coexpService;

    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option inFile = OptionBuilder.create( 'i' );
        addOption( inFile );
        Option outFile = OptionBuilder.create( 'o' );
        addOption( outFile );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        inFile = getOptionValue( 'i' );
        outFile = getOptionValue( 'o' );

        coexpService = ( CoexpressionAnalysisService ) getBean( "coexpressionAnalysisService" );
    }

    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( "DoubleMatrixReader", args );

        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        String taxonName = "human";
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        taxon = taxonService.find( taxon );

        DecimalFormat formatter = ( DecimalFormat ) NumberFormat.getNumberInstance( Locale.US );
        // formatter.applyPattern("0.0000");

        DoubleMatrixReader in = new DoubleMatrixReader();
        try {
            DoubleMatrixNamed matrix = ( DoubleMatrixNamed ) in.read( inFile );
            DoubleMatrixNamed pMatrix = coexpService.calculateMaxCorrelationPValueMatrix( matrix, 0,
                    expressionExperiments );
            MatrixWriter out = new MatrixWriter( outFile, formatter );
            out.writeMatrix( pMatrix, true );
            out.close();
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    public static void main( String[] args ) {
        CorrelationPValueMatrixCalculatorCLI cli = new CorrelationPValueMatrixCalculatorCLI();
        Exception exc = cli.doWork( args );
        if ( exc != null ) log.error( exc.getMessage() );

    }

}
