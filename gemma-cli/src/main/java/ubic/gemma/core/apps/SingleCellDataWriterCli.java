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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("unused")
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
    private ExpressionDataFileService expressionDataFileService;

    private String quantitationTypeName;
    private MatrixFormat format;
    private boolean useEnsemblIds;
    private boolean useStreaming;
    private int fetchSize;
    private boolean standardLocation;
    @Nullable
    private Path outputFile;

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
        options.addOption( "qtName", "quantitation-type-name", true, "Name of the quantitation type to obtain (defaults to the preferred one)" );
        options.addOption( "format", "format", true, "Format to write the matrix for (possible values: tabular, MEX, defaults to tabular)" );
        options.addOption( "useEnsemblIds", "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output)" );
        options.addOption( "noStreaming", "no-streaming", false, "Use in-memory storage instead streaming for retrieving and writing vectors (defaults to false)" );
        options.addOption( Option.builder( "fetchSize" ).longOpt( "fetch-size" ).hasArg( true ).type( Integer.class ).desc( "Fetch size to use when retrieving vectors, incompatible with -noStreaming/--no-streaming." ).build() );
        options.addOption( "standardLocation", "standard-location", false, "Write the file to the standard location under, this is incompatible with -o/--output." );
        options.addOption( Option.builder( "o" ).longOpt( "output" ).hasArg( true ).type( Path.class ).desc( "Destination for the matrix file, or a directory if -format is set to MEX." ).build() );
        addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        this.quantitationTypeName = commandLine.getOptionValue( "qtName" );
        this.useEnsemblIds = commandLine.hasOption( "useEnsemblIds" );
        if ( commandLine.hasOption( "noStreaming" ) && commandLine.hasOption( "fetchSize" ) ) {
            throw new ParseException( "Cannot use -noStreaming/--no-streaming and -fetchSize/--fetch-size at the same time." );
        }
        this.useStreaming = !commandLine.hasOption( "noStreaming" );
        this.fetchSize = commandLine.getParsedOptionValue( "fetchSize", 30 );
        if ( commandLine.hasOption( "format" ) ) {
            this.format = MatrixFormat.valueOf( commandLine.getOptionValue( "format" ).toUpperCase() );
        } else {
            this.format = MatrixFormat.TABULAR;
        }
        this.standardLocation = commandLine.hasOption( "standardLocation" );
        this.outputFile = commandLine.getParsedOptionValue( "o" );
        if ( standardLocation && outputFile != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -o/--output at the same time." );
        }
        if ( standardLocation && useEnsemblIds ) {
            throw new ParseException( "Data cannot be written to the standard location using Ensembl IDs." );
        }
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( !standardLocation ) {
            throw new IllegalStateException( "Can only process multiple experiments with -standardLocation/--standard-location option." );
        }
        super.processBioAssaySets( expressionExperiments );
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
        try {
            switch ( format ) {
                case TABULAR:
                    if ( standardLocation ) {
                        Path path = expressionDataFileService.writeOrLocateTabularSingleCellExpressionData( ee, qt, useStreaming, fetchSize, isForce() );
                        addSuccessObject( ee, "Written vectors for " + qt + " to " + path + "." );
                    } else {
                        try ( Writer writer = new OutputStreamWriter( openOutputFile( isForce() ), StandardCharsets.UTF_8 ) ) {
                            int written = expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, useStreaming, fetchSize, writer );
                            addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + "." );
                        }
                    }
                    break;
                case MEX:
                    if ( standardLocation ) {
                        Path path = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming, fetchSize, isForce() );
                        addSuccessObject( ee, "Successfully written vectors for " + qt + " to " + path + "." );
                    } else if ( outputFile == null || outputFile.endsWith( ".tar" ) || outputFile.endsWith( ".tar.gz" ) ) {
                        log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                        try ( OutputStream stream = openOutputFile( isForce() ) ) {
                            int written = expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, useEnsemblIds, stream );
                            addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + ( useEnsemblIds ? " using Ensembl IDs " : "" ) + "." );
                        }
                    } else {
                        int written = expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, useEnsemblIds, useStreaming, fetchSize, isForce(), outputFile );
                        addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + ( useEnsemblIds ? " using Ensembl IDs " : "" ) + "." );
                    }
                    break;
            }
        } catch ( IOException e ) {
            addErrorObject( ee, e );
        }
    }

    private OutputStream openOutputFile( boolean overwriteExisting ) throws IOException {
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
