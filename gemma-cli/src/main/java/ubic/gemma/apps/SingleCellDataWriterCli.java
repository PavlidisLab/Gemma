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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.cli.util.OptionsUtils.*;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getDataOutputFilename;
import static ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils.createAggregator;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

@SuppressWarnings("unused")
public class SingleCellDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    public SingleCellDataWriterCli() {
        super( SingleCellExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
    }

    enum MatrixFormat {
        TABULAR,
        CELL_BROWSER,
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
    private boolean excludeSampleIdentifiers;
    private boolean useBioAssayIds;
    private boolean useRawColumnNames;
    private boolean useEnsemblIds;
    private boolean useStreaming;
    private int fetchSize;
    private boolean useCursorFetchIfSupported;
    private boolean autoFlush;
    private ExpressionDataFileResult result;

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
    private boolean aggregateUnknownCharacteristics;

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
        addEnumOption( options, "scaleType", "scale-type", "Scale type to use when generating data to disk. This is incompatible with -standardLocation/--standard-location.", ScaleType.class );
        options.addOption( "useBioAssayIds", "use-bioassay-ids", false, "Use BioAssay IDs instead of their names (only for CELL_BROWSER and TABULAR outputs)." );
        options.addOption( "useRawColumnNames", "use-raw-column-names", false, "Use raw column names instead of R-friendly ones (only for CELL_BROWSER and TABULAR outputs)." );
        options.addOption( "useEnsemblIds", "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output). This is incompatible with -standardLocation/--standard-location." );
        options.addOption( "noStreaming", "no-streaming", false, "Use in-memory storage instead of streaming for retrieving and writing vectors." );
        options.addOption( Option.builder( "fetchSize" ).longOpt( "fetch-size" ).hasArg( true ).type( Integer.class ).desc( "Fetch size to use when retrieving vectors, incompatible with -noStreaming/--no-streaming." ).get() );
        options.addOption( "noCursorFetch", "no-cursor-fetch", false, "Disable cursor fetching on the database server and produce results immediately. This is incompatible with -noStreaming." );
        options.addOption( "noAutoFlush", "no-auto-flush", false, "Do not flush the output stream after writing each vector." );

        addExpressionDataFileOptions( options, "single-cell expression data", true );

        // slicing individual samples
        addSingleExperimentOption( options, Option.builder( "samples" )
                .longOpt( "samples" ).hasArg().valueSeparator( ',' )
                .desc( "List of sample identifiers to slice. This is incompatible with -standardLocation/--standard-location." )
                .get() );

        // aggregation
        options.addOption( "aggregateByAssay", "aggregate-by-assay", false, "Aggregate by assay. This is incompatible with -format, -useEnsemblIds and -standardLocation." );
        String incompatibleWithFormat = "Requires -aggregateByAssay to be set. This is incompatible with -format, -useEnsemblIds and -standardLocation.";
        options.addOption( "aggregateByPreferredCta", "aggregate-by-preferred-cell-type-assignment", false, "Aggregate by the preferred cell type assignment." );
        addSingleExperimentOption( options, "aggregateByCta", "aggregate-by-cell-type-assignment", true, "Cell type assignment to aggregate by. " + incompatibleWithFormat );
        addSingleExperimentOption( options, "aggregateByClc", "aggregate-by-cell-level-characteristics", true, "Cell-level characteristics to aggregate by. " + incompatibleWithFormat );
        addEnumOption( options, "aggregateMethod", "aggregate-method", "Method to use to aggregate single-cell data. " + incompatibleWithFormat, SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.class );
        options.addOption( "aggregateUnknownCharacteristics", "aggregate-unknown-characteristics", false, "Aggregate unknown cell types or characteristics. " + incompatibleWithFormat );

        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "noStreaming" ) && commandLine.hasOption( "fetchSize" ) ) {
            throw new ParseException( "Cannot use -noStreaming/--no-streaming and -fetchSize/--fetch-size at the same time." );
        }
        if ( commandLine.hasOption( "noStreaming" ) && commandLine.hasOption( "noCursorFetch" ) ) {
            throw new ParseException( "Cannot use -noStreaming/--no-streaming and -noCursorFetch/--no-cursor-fetch at the same time." );
        }
        this.useStreaming = !commandLine.hasOption( "noStreaming" );
        this.fetchSize = commandLine.getParsedOptionValue( "fetchSize", 30 );
        this.useCursorFetchIfSupported = !commandLine.hasOption( "noCursorFetch" );
        this.autoFlush = !commandLine.hasOption( "noAutoFlush" );
        if ( commandLine.hasOption( "format" ) ) {
            this.format = getEnumOptionValue( commandLine, "format" );
        } else {
            this.format = MatrixFormat.TABULAR;
        }
        if ( commandLine.hasOption( "useEnsemblIds" ) ) {
            if ( this.format != MatrixFormat.MEX ) {
                throw new ParseException( "Cannot use -useEnsemblIds with other formats than MEX." );
            }
            this.useEnsemblIds = true;
        }
        if ( commandLine.hasOption( "useBioAssayIds" ) ) {
            if ( this.format != MatrixFormat.CELL_BROWSER && this.format != MatrixFormat.TABULAR ) {
                throw new ParseException( "Cannot use -useBioAssayIds with other formats than CELL_BROWSER or TABULAR." );
            }
            this.useBioAssayIds = commandLine.hasOption( "useBioAssayIds" );
        }
        if ( commandLine.hasOption( "useRawColumnNames" ) ) {
            if ( this.format != MatrixFormat.CELL_BROWSER && this.format != MatrixFormat.TABULAR ) {
                throw new ParseException( "Cannot use -useRawColumnNames with other formats than CELL_BROWSER or TABULAR." );
            }
            this.useRawColumnNames = commandLine.hasOption( "useRawColumnNames" );
        }
        if ( commandLine.hasOption( "scaleType" ) ) {
            this.scaleType = getEnumOptionValue( commandLine, "scaleType" );
        }
        this.result = getExpressionDataFileResult( commandLine, true );
        if ( this.result.isStandardLocation() && scaleType != null ) {
            throw new ParseException( "Cannot use -standardLocation/--standard-location and -scaleType/--scale-type at the same time." );
        }
        if ( this.result.isStandardLocation() && useEnsemblIds ) {
            throw new ParseException( "Data cannot be written to the standard location using Ensembl IDs." );
        }

        samples = commandLine.getOptionValues( "samples" );
        if ( result.isStandardLocation() && samples != null ) {
            throw new ParseException( "Cannot use -samples/--samples with -standardLocation/--standard-location." );
        }

        aggregateByAssay = hasOption( commandLine, "aggregateByAssay",
                requires( allOf( toBeUnset( "format" ), toBeUnset( "useEnsemblIds" ) ) ) );
        if ( result.isStandardLocation() && aggregateByAssay ) {
            throw new ParseException( "Cannot use -aggregateByAssay with -standardLocation/--standard-location." );
        }
        Predicate<CommandLine> aggregateRequirements = allOf(
                toBeSet( "aggregateByAssay" ),
                toBeUnset( "format" ),
                toBeUnset( "useEnsemblIds" ) );
        aggregateByPreferredCellTypeAssignment = hasOption( commandLine, "aggregateByPreferredCta", requires( aggregateRequirements ) );
        aggregateByCellTypeAssignment = getOptionValue( commandLine, "aggregateByCta", requires( aggregateRequirements ) );
        aggregateByCellLevelCharacteristics = getOptionValue( commandLine, "aggregateByClc", requires( aggregateRequirements ) );
        aggregationMethod = getEnumOptionValue( commandLine, "aggregateMethod", requires( aggregateRequirements ) );
        aggregateUnknownCharacteristics = hasOption( commandLine, "aggregateUnknownCharacteristics", requires( aggregateRequirements ) );
    }

    @Nullable
    private Path fileName;

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
            addSuccessObject( ee, qt, String.format( "%s%s single-cell vectors%s for %s%s.", did,
                    written > 0 ? " " + written : "",
                    fileName != null ? " to " + fileName : "",
                    useEnsemblIds ? " using Ensembl IDs" : "",
                    samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
            ) );
        } catch ( IOException e ) {
            addErrorObject( ee, qt, e );
        }
    }

    private int aggregate( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        List<BioAssay> assays;
        if ( samples != null ) {
            assays = Arrays.stream( samples )
                    .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                    .collect( Collectors.toList() );
        } else {
            assays = null;
        }
        if ( result.isStandardLocation() ) {
            throw new UnsupportedOperationException( "Writing aggregated data to the standard location is not supported." );
        } else if ( result.isStandardOutput() ) {
            fileName = null;
            try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                return aggregate( ee, qt, assays, writer, null );
            }
        } else {
            if ( samples != null ) {
                assays = Arrays.stream( samples )
                        .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                        .collect( Collectors.toList() );
                fileName = result.getOutputFile( getDataOutputFilename( ee, assays, qt, ".aggregated.tsv.gz" ) );
            } else {
                fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ".aggregated.tsv.gz" ) );
            }
            try ( Writer writer = new OutputStreamWriter( openOutputFile( fileName ), StandardCharsets.UTF_8 ) ) {
                return aggregate( ee, qt, assays, writer, getCliContext().getConsole() );
            }
        }
    }

    private int aggregate( ExpressionExperiment ee, QuantitationType qt, @Nullable List<BioAssay> assays, Writer writer, @Nullable Console console ) throws IOException {
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
        Collection<RawExpressionDataVector> vecs;
        SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig config = SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig.builder()
                .includeBiologicalCharacteristics( true )
                .includeCellIds( false )
                // COUNT_FAST does not even need the data!
                .includeData( aggregationMethod != SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT_FAST )
                .includeDataIndices( true )
                .build();
        if ( useStreaming ) {
            log.info( "Single-cell data will be streamed by batch of " + fetchSize + " vectors." );
            long numberOfVectors = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
            try ( Stream<SingleCellExpressionDataVector> scVecs = assays != null ?
                    singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, assays, qt, fetchSize, useCursorFetchIfSupported, true, config ) :
                    singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize, useCursorFetchIfSupported, true, config ) ) {
                vecs = scVecs
                        .peek( createStreamMonitor( ee, qt, getClass().getName(), 100, numberOfVectors, console ) )
                        .map( createAggregator( aggregationMethod, cellLevelCharacteristics, aggregateUnknownCharacteristics ) )
                        .collect( Collectors.toList() );
            }
        } else {
            log.info( "Single-cell data will be loaded into memory. This process can use a lot of memory, press Ctrl-C at any time to interrupt." );
            Collection<SingleCellExpressionDataVector> scVecs;
            if ( assays != null ) {
                scVecs = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, assays, qt, config );
            } else {
                scVecs = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt, config );
            }
            vecs = SingleCellDataVectorAggregatorUtils.aggregate( scVecs, aggregationMethod, cellLevelCharacteristics, aggregateUnknownCharacteristics );
        }
        BulkExpressionDataMatrix<?> matrix;
        if ( aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT || aggregationMethod == SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.COUNT_FAST ) {
            matrix = new ExpressionDataIntegerMatrix( vecs );
        } else {
            matrix = new ExpressionDataDoubleMatrix( vecs );
        }
        MatrixWriter matrixWriter = new MatrixWriter( entityUrlBuilder, buildInfo );
        matrixWriter.setExcludeSampleIdentifiers( excludeSampleIdentifiers );
        matrixWriter.setUseBioAssayIds( useBioAssayIds );
        matrixWriter.setUseRawColumnNames( useRawColumnNames );
        matrixWriter.setAutoFlush( autoFlush );
        matrixWriter.setScaleType( scaleType );
        return matrixWriter.write( matrix, RawExpressionDataVector.class, writer );
    }

    private int slice( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        Assert.notNull( samples );
        List<BioAssay> assays = Arrays.stream( samples )
                .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                .collect( Collectors.toList() );
        int written;
        switch ( format ) {
            case TABULAR:
                if ( result.isStandardLocation() ) {
                    throw new UnsupportedOperationException( "Writing sliced data to the standard location is not supported." );
                } else if ( result.isStandardOutput() ) {
                    fileName = null;
                    try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeTabularSingleCellExpressionData( ee, assays, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, null );
                    }
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, assays, qt, ExpressionDataFileUtils.TABULAR_SC_DATA_SUFFIX ) );
                    try ( Writer writer = Files.newBufferedWriter( fileName ) ) {
                        return expressionDataFileService.writeTabularSingleCellExpressionData( ee, assays, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, getCliContext().getConsole() );
                    }
                }
            case MEX:
                if ( result.isStandardLocation() ) {
                    throw new UnsupportedOperationException( "Writing sliced data to the standard location is not supported." );
                } else if ( result.isStandardOutput() ) {
                    log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                    fileName = null;
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, assays, qt, scaleType, useEnsemblIds, getCliContext().getOutputStream() );
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, assays, qt, ExpressionDataFileUtils.MEX_SC_DATA_SUFFIX ) );
                    assert fileName != null;
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, assays, qt, scaleType, useEnsemblIds, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, isForce(), fileName, autoFlush, getCliContext().getConsole() );
                }
            default:
                throw new IllegalArgumentException( "Unsupported format: " + format );
        }
    }

    private int raw( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        switch ( format ) {
            case TABULAR:
                if ( result.isStandardLocation() ) {
                    try ( LockedPath path = expressionDataFileService.writeOrLocateTabularSingleCellExpressionData( ee, qt, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, isForce(), getCliContext().getConsole() ) ) {
                        fileName = path.getPath();
                        return 0;
                    }
                } else if ( result.isStandardOutput() ) {
                    fileName = null;
                    try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, null );
                    }
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ExpressionDataFileUtils.TABULAR_SC_DATA_SUFFIX ) );
                    try ( Writer writer = new OutputStreamWriter( openOutputFile( fileName ), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, getCliContext().getConsole() );
                    }
                }
            case CELL_BROWSER:
                if ( result.isStandardLocation() ) {
                    throw new UnsupportedOperationException( "Writing Cell Browser-compatible data to the standard location is not supported." );
                } else if ( result.isStandardOutput() ) {
                    fileName = null;
                    try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeCellBrowserSingleCellExpressionData( ee, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, null );
                    }
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ExpressionDataFileUtils.CELL_BROWSER_SC_DATA_SUFFIX ) );
                    try ( Writer writer = new OutputStreamWriter( openOutputFile( fileName ), StandardCharsets.UTF_8 ) ) {
                        return expressionDataFileService.writeCellBrowserSingleCellExpressionData( ee, qt, scaleType, useBioAssayIds, useRawColumnNames, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, writer, autoFlush, getCliContext().getConsole() );
                    }
                }
            case MEX:
                if ( result.isStandardLocation() ) {
                    try ( LockedPath path = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, isForce(), getCliContext().getConsole() ) ) {
                        fileName = path.getPath();
                        return 0;
                    }
                } else if ( result.isStandardOutput() ) {
                    log.warn( "Writing MEX to a stream requires a lot of memory and cannot be streamed, you can cancel this any anytime with Ctrl-C." );
                    fileName = null;
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, getCliContext().getOutputStream() );
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ExpressionDataFileUtils.MEX_SC_DATA_SUFFIX ) );
                    assert fileName != null;
                    return expressionDataFileService.writeMexSingleCellExpressionData( ee, qt, scaleType, useEnsemblIds, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, isForce(), fileName, autoFlush, getCliContext().getConsole() );
                }
            case CELL_IDS:
                if ( result.isStandardLocation() ) {
                    throw new UnsupportedOperationException( "Writing cell IDs to the standard location is not supported." );
                } else if ( result.isStandardOutput() ) {
                    fileName = null;
                    try ( Stream<String> stream = singleCellExpressionExperimentService.streamCellIds( ee, qt, true ) ) {
                        if ( stream != null ) {
                            stream.forEach( getCliContext().getOutputStream()::println );
                            return 0;
                        } else {
                            throw new RuntimeException( "Could not find cell IDs for " + qt + "." );
                        }
                    }
                } else {
                    fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ".cellIds.txt.gz" ) );
                    try ( PrintStream printStream = new PrintStream( openOutputFile( fileName ), autoFlush, StandardCharsets.UTF_8.name() );
                            Stream<String> stream = singleCellExpressionExperimentService.streamCellIds( ee, qt, true ) ) {
                        if ( stream != null ) {
                            stream.forEach( printStream::println );
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

    private OutputStream openOutputFile( Path fileName ) throws IOException {
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new GZIPOutputStream( Files.newOutputStream( fileName ) );
        } else {
            return Files.newOutputStream( fileName );
        }
    }
}
