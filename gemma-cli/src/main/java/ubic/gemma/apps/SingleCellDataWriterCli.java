package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataIntegerMatrix;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.cli.util.OptionsUtils.*;
import static ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils.createAggregator;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

@SuppressWarnings("unused")
public class SingleCellDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    public SingleCellDataWriterCli() {
        super( SingleCellExpressionDataVector.class );
        setUsePreferredQuantitationType();
    }

    enum MatrixFormat {
        TABULAR,
        MEX,
        CELL_IDS
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
    private boolean standardOutput;
    @Nullable
    private Path outputFile;

    @Nullable
    private String[] samples;

    @Nullable
    private SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod aggregationMethod;
    private boolean aggregateByAssay;
    private boolean aggregateByPreferredCellTypeAssignment;
    @Nullable
    private String aggregateByCellTypeAssignment;
    @Nullable
    private String aggregateByCellLevelCharacteristics;

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
        addEnumOption( options, "format", "format", "Format to write the matrix for (defaults to tabular)", MatrixFormat.class );
        addEnumOption( options, "scaleType", "scale-type", "Scale type to use when generating data to disk.", ScaleType.class );
        options.addOption( "useEnsemblIds", "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output)" );
        options.addOption( "noStreaming", "no-streaming", false, "Use in-memory storage instead streaming for retrieving and writing vectors (defaults to false)" );
        options.addOption( Option.builder( "fetchSize" ).longOpt( "fetch-size" ).hasArg( true ).type( Integer.class ).desc( "Fetch size to use when retrieving vectors, incompatible with -noStreaming/--no-streaming." ).build() );
        options.addOption( "standardLocation", "standard-location", false, "Write the file to the standard location under, this is incompatible with -scaleType/--scale-type, -useEnsemblIds/--use-ensembl-ids, -stdout/--stdout or -o/--output." );
        addSingleExperimentOption( options, "stdout", "stdout", false, "Write to the standard output." );
        addSingleExperimentOption( options, Option.builder( "o" ).longOpt( "output" ).hasArg( true ).type( Path.class ).desc( "Destination for the matrix file, or a directory if -format is set to MEX." ).build() );

        // slicing individual samples
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice. This is incompatible with -standardLocation." ).build() );

        // aggregation
        options.addOption( "aggregateByAssay", "aggregate-by-assay", false, "Aggregate by assay. This is incompatible with -format, -useEnsemblIds and -standardLocation." );
        options.addOption( "aggregateByPreferredCta", "aggregate-by-preferred-cell-type-assignment", false, "Aggregate by the preferred cell type assignment. Requires -aggregateByAssay to be set. This is incompatible with -format, -useEnsemblIds and -standardLocation." );
        addSingleExperimentOption( options, "aggregateByCta", "aggregate-by-cell-type-assignment", true, "Cell type assignment to aggregate by. Requires -aggregateByAssay to be set. This is incompatible with -format, -useEnsemblIds and -standardLocation." );
        addSingleExperimentOption( options, "aggregateByClc", "aggregate-by-cell-level-characteristics", true, "Cell-level characteristics to aggregate by. Requires -aggregateByAssay to be set. This is incompatible with -format, -useEnsemblIds and -standardLocation." );
        addEnumOption( options, "aggregateMethod", "aggregate-method", "Method to use to aggregate single-cell data. Require at least one -aggregateBy option to be set. This is incompatible with -format, -useEnsemblIds and -standardLocation.", SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.class );

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
            this.format = getEnumOptionValue( commandLine, "format" );
        } else {
            this.format = MatrixFormat.TABULAR;
        }
        if ( commandLine.hasOption( "scaleType" ) ) {
            this.scaleType = getEnumOptionValue( commandLine, "scaleType" );
        }
        this.standardLocation = commandLine.hasOption( "standardLocation" );
        this.standardOutput = commandLine.hasOption( "stdout" );
        this.outputFile = commandLine.getParsedOptionValue( "o" );
        if ( standardOutput && outputFile != null ) {
            throw new ParseException( "Cannot set both -stdout/--stdout and -o/--output" );
        }
        if ( standardLocation && scaleType != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -scaleType/--scale-type at the same time." );
        }
        if ( standardLocation && standardOutput ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -stdout/--stdout at the same time." );
        }
        if ( standardLocation && outputFile != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -o/--output at the same time." );
        }
        if ( standardLocation && useEnsemblIds ) {
            throw new ParseException( "Data cannot be written to the standard location using Ensembl IDs." );
        }

        samples = getOptionValues( commandLine, "samples", requires( allOf( toBeUnset( "standardLocation" ) ) ) );

        aggregateByAssay = hasOption( commandLine, "aggregateByAssay",
                requires( allOf( toBeUnset( "standardLocation" ), toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
        aggregateByPreferredCellTypeAssignment = hasOption( commandLine, "aggregateByPreferredCta",
                requires( allOf( toBeSet( "aggregateByAssay" ), toBeUnset( "standardLocation" ), toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
        aggregateByCellTypeAssignment = getOptionValue( commandLine, "aggregateByCta",
                requires( allOf( toBeSet( "aggregateByAssay" ), toBeUnset( "standardLocation" ), toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
        aggregateByCellLevelCharacteristics = getOptionValue( commandLine, "aggregateByClc",
                requires( allOf( toBeSet( "aggregateByAssay" ), toBeUnset( "standardLocation" ), toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
        aggregationMethod = getEnumOptionValue( commandLine, "aggregateMethod",
                requires( allOf( anyOf( toBeSet( "aggregateByAssay" ), toBeSet( "aggregateByPreferredCellTypeAssignment" ), toBeSet( "aggregateByCellTypeAssignment" ), toBeSet( "aggregateByCellLevelCharacteristics" ) ),
                        toBeUnset( "standardLocation" ), toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
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
            int written;
            String did;
            if ( aggregateByAssay || aggregateByPreferredCellTypeAssignment || aggregateByCellTypeAssignment != null || aggregateByCellLevelCharacteristics != null ) {
                did = "Aggregated";
                written = aggregate( ee, qt );
            } else if ( samples != null ) {
                did = "Sliced";
                written = slice( ee, qt );
            } else {
                did = "Wrote";
                written = raw( ee, qt );
            }
            addSuccessObject( ee, String.format( "%s%s single-cell vectors%s for %s%s%s.", did,
                    written > 0 ? " " + written : "",
                    fileName != null ? " to " + fileName : "",
                    qt,
                    useEnsemblIds ? " using Ensembl IDs" : "",
                    samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
            ) );
        } catch ( IOException e ) {
            addErrorObject( ee, e );
        }
    }

    private int aggregate( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod aggregationMethod = this.aggregationMethod != null
                ? this.aggregationMethod : SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.SUM;
        CellLevelCharacteristics cellLevelCharacteristics;
        if ( aggregateByPreferredCellTypeAssignment ) {
            cellLevelCharacteristics = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt )
                    .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred cell type assignment." ) );
        } else if ( aggregateByCellTypeAssignment != null ) {
            cellLevelCharacteristics = entityLocator.locateCellTypeAssignment( ee, qt, aggregateByCellTypeAssignment );
        } else if ( aggregateByCellLevelCharacteristics != null ) {
            cellLevelCharacteristics = entityLocator.locateCellLevelCharacteristics( ee, qt, aggregateByCellLevelCharacteristics );
        } else {
            cellLevelCharacteristics = null;
        }
        log.info( String.format( "Aggregating vectors for %s by assay%s using %s%s...", qt,
                cellLevelCharacteristics != null ? ( " and " + cellLevelCharacteristics ) : "",
                aggregationMethod,
                samples != null ? " for the following samples: " + Arrays.toString( samples ) : "" ) );
        List<BioAssay> assays = null;
        Collection<RawExpressionDataVector> vecs;
        SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig config = SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig.builder()
                .includeCellIds( false )
                // COUNT_FAST does not even need the data!
                .includeData( aggregationMethod != SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT_FAST )
                .includeDataIndices( true )
                .build();
        if ( useStreaming ) {
            long numberOfVectors = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
            Stream<SingleCellExpressionDataVector> scVecs;
            if ( samples != null ) {
                assays = Arrays.stream( samples )
                        .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                        .collect( Collectors.toList() );
                scVecs = singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, assays, qt, fetchSize, true, config );
            } else {
                scVecs = singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize, true, config );
            }
            vecs = scVecs
                    .peek( createStreamMonitor( getClass().getName(), 10, numberOfVectors ) )
                    .map( createAggregator( aggregationMethod, cellLevelCharacteristics ) )
                    .collect( Collectors.toList() );
        } else {
            Collection<SingleCellExpressionDataVector> scVecs;
            if ( samples != null ) {
                assays = Arrays.stream( samples )
                        .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                        .collect( Collectors.toList() );
                scVecs = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, assays, qt, config );
            } else {
                scVecs = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt, config );
            }
            vecs = SingleCellDataVectorAggregatorUtils.aggregate( scVecs,
                    aggregationMethod, cellLevelCharacteristics );
        }
        BulkExpressionDataMatrix<?> matrix;
        if ( aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT || aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT_FAST ) {
            matrix = new ExpressionDataIntegerMatrix( vecs );
        } else {
            matrix = new ExpressionDataDoubleMatrix( vecs );
        }
        try ( Writer writer = new OutputStreamWriter( openOutputFile( ee, assays, qt, ".aggregated.tsv.gz", isForce() ), StandardCharsets.UTF_8 ) ) {
            MatrixWriter matrixWriter = new MatrixWriter( entityUrlBuilder, buildInfo );
            matrixWriter.setAutoFlush( true );
            matrixWriter.setScaleType( scaleType );
            return matrixWriter.write( matrix, writer );
        }
    }

    private int slice( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        Assert.notNull( samples );
        List<BioAssay> assays = Arrays.stream( samples )
                .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                .collect( Collectors.toList() );
        int written;
        switch ( format ) {
            case TABULAR:
                try ( Writer writer = new OutputStreamWriter( openOutputFile( ee, assays, qt, ExpressionDataFileUtils.TABULAR_SC_DATA_SUFFIX, isForce() ), StandardCharsets.UTF_8 ) ) {
                    return expressionDataFileService.writeTabularSingleCellExpressionData( ee, assays, qt, scaleType, useStreaming ? fetchSize : -1, writer, true );
                }
            case MEX:
                if ( outputFile == null || outputFile.toString().endsWith( ".tar" ) || outputFile.toString().endsWith( ".tar.gz" ) ) {
                    log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                    try ( OutputStream stream = openOutputFile( ee, assays, qt, ExpressionDataFileUtils.MEX_SC_DATA_SUFFIX, isForce() ) ) {
                        return expressionDataFileService.writeMexSingleCellExpressionData( ee, assays, qt, scaleType, useEnsemblIds, stream );
                    }
                } else {
                    if ( !isForce() && Files.exists( outputFile ) ) {
                        throw new RuntimeException( outputFile + " already exists, use -force/--force to override." );
                    }
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, assays, qt, scaleType, useEnsemblIds, useStreaming ? fetchSize : -1, isForce(), outputFile );
                }
            default:
                throw new IllegalArgumentException( "Unsupported format: " + format );
        }
    }

    private int raw( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        switch ( format ) {
            case TABULAR:
                if ( standardLocation ) {
                    try ( LockedPath path = expressionDataFileService.writeOrLocateTabularSingleCellExpressionData( ee, qt, useStreaming ? fetchSize : -1, isForce() ) ) {
                        return 0;
                    }
                } else {
                    try ( Writer writer = new OutputStreamWriter( openOutputFile( ee, null, qt, ExpressionDataFileUtils.TABULAR_SC_DATA_SUFFIX, isForce() ), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, scaleType, useStreaming ? fetchSize : -1, writer, true );
                    }
                }
            case MEX:
                if ( standardLocation ) {
                    try ( LockedPath path = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming ? fetchSize : -1, isForce() ) ) {
                        return 0;
                    }
                } else if ( outputFile == null || outputFile.toString().endsWith( ".tar" ) || outputFile.toString().endsWith( ".tar.gz" ) ) {
                    log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                    try ( OutputStream stream = openOutputFile( ee, null, qt, ExpressionDataFileUtils.MEX_SC_DATA_SUFFIX, isForce() ) ) {
                        return expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, stream );
                    }
                } else {
                    if ( !isForce() && Files.exists( outputFile ) ) {
                        throw new RuntimeException( outputFile + " already exists, use -force/--force to override." );
                    }
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, useStreaming ? fetchSize : -1, isForce(), outputFile );
                }
            case CELL_IDS:
                try ( PrintStream printer = new PrintStream( openOutputFile( ee, null, qt, ".cellIds.txt.gz", isForce() ), true, StandardCharsets.UTF_8.name() ) ) {
                    try ( Stream<String> stream = singleCellExpressionExperimentService.streamCellIds( ee, qt, true ) ) {
                        if ( stream != null ) {
                            stream.forEach( printer::println );
                            return 0;
                        } else {
                            throw new RuntimeException( "Could not find cell IDs for " + qt + "." );
                        }
                    }
                }
            default:
                throw new IllegalArgumentException( "Unsupported format: " + format );
        }
    }

    @Nullable
    private Path fileName;

    private OutputStream openOutputFile( ExpressionExperiment ee, @Nullable List<BioAssay> assays, QuantitationType qt, String suffix, boolean overwriteExisting ) throws IOException {
        if ( standardOutput ) {
            fileName = null;
            return getCliContext().getOutputStream();
        }
        if ( this.outputFile != null ) {
            fileName = this.outputFile;
        } else {
            if ( assays != null ) {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, assays, qt, suffix ) );
            } else {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, qt, suffix ) );
            }
        }
        if ( !overwriteExisting && Files.exists( fileName ) ) {
            throw new RuntimeException( fileName + " already exists, use -force/--force to override." );
        }
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new GZIPOutputStream( Files.newOutputStream( fileName ) );
        } else {
            return Files.newOutputStream( fileName );
        }
    }
}
