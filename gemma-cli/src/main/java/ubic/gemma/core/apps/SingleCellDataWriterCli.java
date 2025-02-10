package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataIntegerMatrix;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils.aggregate;
import static ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils.createAggregator;
import static ubic.gemma.core.util.OptionsUtils.*;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

@SuppressWarnings("unused")
public class SingleCellDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    public SingleCellDataWriterCli() {
        super( SingleCellExpressionDataVector.class );
        setUsePreferredQuantitationType();
    }

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

    @Autowired
    private BuildInfo buildInfo;

    private MatrixFormat format;
    @Nullable
    private ScaleType scaleType;
    private boolean useEnsemblIds;
    private boolean useStreaming;
    private int fetchSize;
    private boolean standardLocation;
    @Nullable
    private Path outputFile;
    @Nullable
    private SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod aggregationMethod;

    @Nullable
    @Override
    public String getCommandName() {
        return "getSingleCellDataMatrix";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Write single-cell data matrix to a file; gene information is included if available.";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( "format", "format", true, "Format to write the matrix for (possible values: tabular, MEX, defaults to tabular)" );
        options.addOption( "scaleType", "scale-type", true, "Scale type to use when generating data to disk (possible values: " + Arrays.stream( ScaleType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + ")." );
        options.addOption( "useEnsemblIds", "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output)" );
        options.addOption( "noStreaming", "no-streaming", false, "Use in-memory storage instead streaming for retrieving and writing vectors (defaults to false)" );
        options.addOption( Option.builder( "fetchSize" ).longOpt( "fetch-size" ).hasArg( true ).type( Integer.class ).desc( "Fetch size to use when retrieving vectors, incompatible with -noStreaming/--no-streaming." ).build() );
        options.addOption( "standardLocation", "standard-location", false, "Write the file to the standard location under, this is incompatible with -scaleType/--scale-type, -useEnsemblIds/--use-ensembl-ids and -o/--output." );
        options.addOption( Option.builder( "o" ).longOpt( "output" ).hasArg( true ).type( Path.class ).desc( "Destination for the matrix file, or a directory if -format is set to MEX." ).build() );
        addEnumOption( options, "aggregate", "aggregate", "Aggregate the single-cell data by sample. This is incompatible with -format, -useEnsemblIds and -standardLocation.", SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
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
        if ( commandLine.hasOption( "scaleType" ) ) {
            this.scaleType = ScaleType.valueOf( commandLine.getOptionValue( "scaleType" ).toUpperCase() );
        }
        this.standardLocation = commandLine.hasOption( "standardLocation" );
        this.outputFile = commandLine.getParsedOptionValue( "o" );
        if ( standardLocation && scaleType != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -scaleType/--scale-type at the same time." );
        }
        if ( standardLocation && outputFile != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -o/--output at the same time." );
        }
        if ( standardLocation && useEnsemblIds ) {
            throw new ParseException( "Data cannot be written to the standard location using Ensembl IDs." );
        }
        aggregationMethod = getEnumOptionValue( commandLine, "aggregate",
                SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.class,
                requires( noneOf( toBeSet( "standardLocation" ), toBeSet( "format" ), toBeSet( "useEnsemblIds" ) ) ) );
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( !standardLocation ) {
            throw new IllegalStateException( "Can only process multiple experiments with -standardLocation/--standard-location option." );
        }
        super.processBioAssaySets( expressionExperiments );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        try {
            if ( aggregationMethod != null ) {
                log.info( "Aggregating vectors for " + qt + " using " + aggregationMethod + "..." );
                Collection<RawExpressionDataVector> vecs;
                if ( useStreaming ) {
                    vecs = singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30 )
                            .peek( createStreamMonitor( getClass().getName(), 10000 ) )
                            .map( createAggregator( aggregationMethod ) )
                            .limit( 50 )
                            .collect( Collectors.toList() );
                } else {
                    vecs = aggregate( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ), aggregationMethod );
                }
                BulkExpressionDataMatrix<?> matrix;
                if ( aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT || aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT_FAST ) {
                    matrix = new ExpressionDataIntegerMatrix( vecs );
                } else {
                    matrix = new ExpressionDataDoubleMatrix( vecs );
                }
                try ( Writer writer = new OutputStreamWriter( openOutputFile( isForce() ), StandardCharsets.UTF_8 ) ) {
                    MatrixWriter matrixWriter = new MatrixWriter( entityUrlBuilder, buildInfo );
                    matrixWriter.setScaleType( scaleType );
                    matrixWriter.write( matrix, writer );
                }
                return;
            }
            switch ( format ) {
                case TABULAR:
                    if ( standardLocation ) {
                        try ( ExpressionDataFileService.LockedPath path = expressionDataFileService.writeOrLocateTabularSingleCellExpressionData( ee, qt, useStreaming, fetchSize, isForce() ) ) {
                            addSuccessObject( ee, "Written vectors for " + qt + " to " + path.getPath() + "." );
                        }
                    } else {
                        try ( Writer writer = new OutputStreamWriter( openOutputFile( isForce() ), StandardCharsets.UTF_8 ) ) {
                            int written = expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, scaleType, useStreaming, fetchSize, writer );
                            addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + "." );
                        }
                    }
                    break;
                case MEX:
                    if ( standardLocation ) {
                        try ( ExpressionDataFileService.LockedPath path = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming, fetchSize, isForce() ) ) {
                            addSuccessObject( ee, "Successfully written vectors for " + qt + " to " + path.getPath() + "." );
                        }
                    } else if ( outputFile == null || outputFile.toString().endsWith( ".tar" ) || outputFile.toString().endsWith( ".tar.gz" ) ) {
                        log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                        try ( OutputStream stream = openOutputFile( isForce() ) ) {
                            int written = expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, stream );
                            addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + ( useEnsemblIds ? " using Ensembl IDs " : "" ) + "." );
                        }
                    } else {
                        if ( !isForce() && Files.exists( outputFile ) ) {
                            throw new RuntimeException( outputFile + " already exists, use -force/--force to override." );
                        }
                        int written = expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, useStreaming, fetchSize, isForce(), outputFile );
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
            if ( !overwriteExisting && Files.exists( outputFile ) ) {
                throw new RuntimeException( outputFile + " already exists, use -force/--force to override." );
            }
            if ( outputFile.toString().endsWith( ".gz" ) ) {
                return new GZIPOutputStream( Files.newOutputStream( outputFile ) );
            } else {
                return Files.newOutputStream( outputFile );
            }
        } else {
            return System.out;
        }
    }
}
