package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class SingleCellDataWriterCli extends ExpressionExperimentManipulatingCLI {

    enum MatrixFormat {
        TABULAR,
        MEX
    }

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    private String quantitationTypeName;
    private MatrixFormat format;
    private boolean useEnsemblIds;
    @Nullable
    private Path outputFile;

    public SingleCellDataWriterCli() {
        setSingleExperimentMode();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "getSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Write single-cell data to disk with gene information if available.";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( "qtName", "quantitation-type-name", true, "Name of the quantitation type to obtain " );
        options.addOption( "format", "format", true, "Format to write the matrix for (possible values: tabular, MEX, defaults to tabular)" );
        options.addOption( "useEnsemblIds", "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output)" );
        options.addOption( Option.builder( "o" ).longOpt( "output" ).hasArg( true ).type( Path.class ).desc( "Destination for the matrix file, or a directory if -format is set to MEX." ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        this.quantitationTypeName = commandLine.getOptionValue( "qtName" );
        this.useEnsemblIds = commandLine.hasOption( "useEnsemblIds" );
        if ( commandLine.hasOption( "format" ) ) {
            format = MatrixFormat.valueOf( commandLine.getOptionValue( "format" ).toUpperCase() );
        } else {
            format = MatrixFormat.TABULAR;
        }
        this.outputFile = commandLine.getParsedOptionValue( "o" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        QuantitationType qt;
        if ( quantitationTypeName != null ) {
            try {
                qt = quantitationTypeService.findByNameAndVectorType( ee, quantitationTypeName, SingleCellExpressionDataVector.class );
                if ( qt == null ) {
                    String availableQts = singleCellExpressionExperimentService.getSingleCellQuantitationTypes( ee )
                            .stream().map( AbstractDescribable::getName )
                            .collect( Collectors.joining( "\t\n" ) );
                    throw new RuntimeException( ee + " does not have a quantitation type named " + quantitationTypeName + ", possible options are:\n" + availableQts + "." );
                }
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
        } else {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new RuntimeException( ee + " does not have preferred a single-cell quantitation type." ) );
        }
        log.info( "Will write data for " + qt + " to " + ( outputFile != null ? outputFile : " the standard output" ) + "." );
        try {
            switch ( format ) {
                case TABULAR:
                    try ( Writer writer = new OutputStreamWriter( openOutputFile(), StandardCharsets.UTF_8 ) ) {
                        expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, writer );
                    }
                    break;
                case MEX:
                    if ( outputFile == null || outputFile.endsWith( ".tar" ) || outputFile.endsWith( ".tar.gz" ) ) {
                        log.warn( "Writing MEX to a stream requires a lot of memory, you can cancel this any anytime with Ctrl-C." );
                        try ( OutputStream stream = openOutputFile() ) {
                            expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, useEnsemblIds, stream );
                        }
                    } else {
                        expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, useEnsemblIds, outputFile );
                    }
                    break;
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private OutputStream openOutputFile() throws IOException {
        if ( outputFile != null ) {
            if ( outputFile.endsWith( ".gz" ) ) {
                return new GZIPOutputStream( Files.newOutputStream( outputFile ) );
            } else {
                return Files.newOutputStream( outputFile );
            }
        } else {
            return System.out;
        }
    }
}
