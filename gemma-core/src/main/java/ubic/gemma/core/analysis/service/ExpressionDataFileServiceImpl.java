/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.service;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.*;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 * <p>
 * Never use this service in a {@link Transactional} context, it uses locks for files and spends significant time
 * writing to disk.
 *
 * @author paul
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class ExpressionDataFileServiceImpl implements ExpressionDataFileService {

    private static final Log log = LogFactory.getLog( ExpressionDataFileServiceImpl.class.getName() );

    // for single-cell vectors
    private static final String SC_DATA_SUFFIX = ".scdata";
    private static final String MEX_SC_DATA_SUFFIX = SC_DATA_SUFFIX;
    private static final String TABULAR_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".tsv.gz";
    // for bulk (raw or processed vectors)
    private static final String BULK_DATA_SUFFIX = ".data";
    private static final String TABULAR_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".txt.gz";
    private static final String JSON_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".json.gz";

    private static final String MSG_FILE_EXISTS = " File (%s) exists, not regenerating";
    private static final String MSG_FILE_FORCED = "Forcing file (%s) regeneration";
    private static final String MSG_FILE_NOT_EXISTS = "File (%s) does not exist or can not be accessed ";
    private static final String MSG_FILE_OUTDATED = "File (%s) outdated, regenerating";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionDataFileHelperService helperService;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    @Value("${gemma.hosturl}")
    private String gemmaHostUrl;

    @Override
    public void deleteAllFiles( ExpressionExperiment ee ) {
        // NOTE: Never ever delete the metadata directory, it may contain precious information that was not generated by
        //       Gemma

        // data files.
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) ) );

        // per-QT output file
        for ( QuantitationType qt : expressionExperimentService.getQuantitationTypes( ee ) ) {
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, MEX_SC_DATA_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, JSON_BULK_DATA_FILE_SUFFIX ) ) );
        }

        // diff ex files
        Collection<DifferentialExpressionAnalysis> analyses = helperService.getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            this.deleteDiffExArchiveFile( analysis );
        }

        // coexpression file
        this.deleteAndLog( dataDir.resolve( this.getCoexpressionDataFilename( ee ) ) );

        // design file
        this.deleteAndLog( dataDir.resolve( this.getDesignFileName( ee ) ) );
    }

    private void deleteAndLog( Path f1 ) {
        Lock lock = acquirePathLock( f1, true );
        try {
            if ( Files.deleteIfExists( f1 ) ) {
                ExpressionDataFileServiceImpl.log.info( "Deleted: " + f1 );
            }
        } catch ( IOException e ) {
            ExpressionDataFileServiceImpl.log.error( "Failed to delete " + f1, e );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) {
        this.deleteAndLog( dataDir.resolve( this.getDiffExArchiveFileName( analysis ) ) );
    }

    @Override
    public Path getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) throws IOException {
        DifferentialExpressionAnalysis analysis = helperService.getAnalysisById( analysisId );
        return getDiffExpressionAnalysisArchiveFile( analysis, forceCreate );
    }

    @Override
    public Optional<Path> getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type ) throws IOException {
        // TODO: add support for locking, in this case it will be via FileChannel/FileLock because those files are
        //       generated externally
        Path file = metadataDir.resolve( this.getEEFolderName( ee ) ).resolve( type.getFileName( ee ) );
        Lock lock = acquirePathLock( file, false );
        try {
            if ( !Files.exists( file ) ) {
                return Optional.empty();
            }
            if ( type.isDirectory() ) {
                // If this is a directory, check if we can read the most recent file.
                try ( Stream<Path> files = Files.list( file ) ) {
                    return files
                            // ignore sub-directories
                            .filter( f -> !Files.isDirectory( f ) )
                            // Sort by last modified, we only want the newest file
                            .max( Comparator.comparing( path -> {
                                try {
                                    return Files.getLastModifiedTime( path );
                                } catch ( IOException e ) {
                                    return FileTime.fromMillis( 0 );
                                }
                            } ) );
                }
            } else {
                return Optional.of( file );
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Forms a folder name where the given experiments metadata will be located (within the {@link #metadataDir} directory).
     *
     * @param ee the experiment to get the folder name for.
     * @return folder name based on the given experiments properties. Usually this will be the experiments short name,
     * without any splitting suffixes (e.g. for GSE123.1 the folder name would be GSE123). If the short name is empty for
     * any reason, the experiments ID will be used.
     */
    private String getEEFolderName( ExpressionExperiment ee ) {
        String sName = ee.getShortName();
        if ( StringUtils.isBlank( sName ) ) {
            return ee.getId().toString();
        }
        return sName.replaceAll( "\\.\\d+$", "" );
    }

    @Override
    public Optional<Path> getDataFile( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type ) {
        if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
            return Optional.empty();
        }
        String suffix;
        switch ( type ) {
            case TABULAR:
                suffix = TABULAR_BULK_DATA_FILE_SUFFIX;
                break;
            case JSON:
                suffix = JSON_BULK_DATA_FILE_SUFFIX;
                break;
            default:
                throw new IllegalArgumentException( "Unsupported data file type: " + type );
        }
        return Optional.of( getOutputFile( getDataOutputFilename( ee, filtered, suffix ) ) );
    }

    @Override
    public Path getDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type ) {
        Class<? extends DataVector> dataType = quantitationTypeService.getDataVectorType( qt );
        String suffix;
        switch ( type ) {
            case TABULAR:
                if ( BulkExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    suffix = TABULAR_BULK_DATA_FILE_SUFFIX;
                } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    suffix = TABULAR_SC_DATA_SUFFIX;
                } else {
                    throw new IllegalArgumentException( "Unknown vector type: " + dataType );
                }
                break;
            case JSON:
                Assert.isAssignable( BulkExpressionDataVector.class, dataType );
                suffix = JSON_BULK_DATA_FILE_SUFFIX;
                break;
            case MEX:
                Assert.isAssignable( SingleCellExpressionDataVector.class, dataType );
                suffix = MEX_SC_DATA_SUFFIX;
                break;
            default:
                throw new IllegalArgumentException( "Unsupported data file type: " + type );
        }
        return getOutputFile( getDataOutputFilename( ee, qt, suffix ) );
    }

    @Override
    public boolean deleteDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type ) throws IOException {
        Path file = getDataFile( ee, qt, type );
        Lock lock = acquirePathLock( file, true );
        try {
            if ( Files.exists( file ) ) {
                log.info( "Deleting data file: " + file );
                PathUtils.delete( file );
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Path> getExperimentalDesignFile( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            return Optional.empty();
        }
        return Optional.of( getOutputFile( getDesignFileName( ee ) ) );
    }

    @Override
    public Optional<Path> writeProcessedExpressionDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException, FilteringException {
        return this.writeDataFile( ee, filtered, Paths.get( fileName ), compress );
    }

    @Override
    public int writeTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, Writer writer ) throws IOException {
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        Stream<SingleCellExpressionDataVector> vectors;
        AtomicLong numVecs = new AtomicLong();
        if ( useStreaming ) {
            vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, fetchSize );
        } else {
            Collection<SingleCellExpressionDataVector> col = helperService.getSingleCellVectors( ee, qt, cs2gene );
            numVecs.set( col.size() );
            vectors = col.stream();
        }
        log.info( "Will write tabular data for " + qt + " to a stream." );
        return new TabularMatrixWriter( gemmaHostUrl ).write( vectors.peek( createStreamMonitor( numVecs.get() ) ), cs2gene, writer );
    }

    @Override
    public Path writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        Path dest = getOutputFile( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ) );
        if ( !forceWrite && Files.exists( dest ) ) {
            return dest;
        }
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        Stream<SingleCellExpressionDataVector> vectors;
        AtomicLong numVecs = new AtomicLong();
        if ( useStreaming ) {
            vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, fetchSize );
        } else {
            Collection<SingleCellExpressionDataVector> col = helperService.getSingleCellVectors( ee, qt, cs2gene );
            numVecs.set( col.size() );
            vectors = col.stream();
        }
        Lock lock = acquirePathLock( dest, true );
        try ( Writer writer = new OutputStreamWriter( openFile( dest, true ), StandardCharsets.UTF_8 ) ) {
            log.info( "Will write tabular data for " + qt + " to " + dest + "." );
            int written = new TabularMatrixWriter( gemmaHostUrl ).write( vectors.peek( createStreamMonitor( numVecs.get() ) ), cs2gene, writer );
            log.info( "Wrote " + written + " vectors to " + dest + "." );
            return dest;
        } catch ( Exception e ) {
            Files.deleteIfExists( dest );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Path writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException, IOException {
        Path dest = getOutputFile( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ), timeout, timeUnit );
        if ( !forceWrite && Files.exists( dest ) ) {
            return dest;
        }
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        Stream<SingleCellExpressionDataVector> vectors;
        AtomicLong numVecs = new AtomicLong();
        if ( useStreaming ) {
            vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, fetchSize );
        } else {
            Collection<SingleCellExpressionDataVector> col = helperService.getSingleCellVectors( ee, qt, cs2gene );
            numVecs.set( col.size() );
            vectors = col.stream();
        }
        Lock lock = tryAcquirePathLock( dest, true, timeout, timeUnit );
        try ( Writer writer = new OutputStreamWriter( openFile( dest, true ), StandardCharsets.UTF_8 ) ) {
            log.info( "Will write tabular data for " + qt + " to " + dest + "." );
            int written = new TabularMatrixWriter( gemmaHostUrl ).write( vectors.peek( createStreamMonitor( numVecs.get() ) ), cs2gene, writer );
            log.info( "Wrote " + written + " vectors to " + dest + "." );
            return dest;
        } catch ( Exception e ) {
            Files.deleteIfExists( dest );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, OutputStream stream ) throws IOException {
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        Collection<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene );
        log.info( "Will write MEX data for " + qt + " to a stream " + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
        DoubleSingleCellExpressionDataMatrix matrix = new DoubleSingleCellExpressionDataMatrix( vectors );
        MexMatrixWriter writer = new MexMatrixWriter();
        writer.setUseEnsemblIds( useEnsemblIds );
        return writer.write( matrix, cs2gene, stream );
    }

    @Override
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, boolean useStreaming, int fetchSize, boolean forceWrite, Path destDir ) throws IOException {
        if ( !forceWrite && Files.exists( destDir ) ) {
            throw new IllegalArgumentException( "Output directory " + destDir + " already exists." );
        }
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        if ( useStreaming ) {
            Map<BioAssay, Long> nnzBySample = new HashMap<>();
            AtomicLong numVecs = new AtomicLong();
            Stream<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, nnzBySample, fetchSize );
            if ( Files.exists( destDir ) ) {
                log.info( destDir + " already exists, removing..." );
                PathUtils.deleteDirectory( destDir );
            }
            log.info( "Will write MEX data for " + qt + " to " + destDir + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
            Lock lock = acquirePathLock( destDir, true );
            try {
                MexMatrixWriter writer = new MexMatrixWriter();
                writer.setUseEnsemblIds( useEnsemblIds );
                return writer.write( vectors.peek( createStreamMonitor( numVecs.get() ) ), ( int ) numVecs.get(), nnzBySample, cs2gene, destDir );
            } catch ( Exception e ) {
                PathUtils.deleteDirectory( destDir );
                throw e;
            } finally {
                lock.unlock();
            }
        } else {
            SingleCellExpressionDataMatrix<Double> matrix = helperService.getSingleCellMatrix( ee, qt, cs2gene );
            log.info( "Will write MEX data for " + qt + " to " + destDir + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
            Lock lock = acquirePathLock( destDir, true );
            try {
                MexMatrixWriter writer = new MexMatrixWriter();
                writer.setUseEnsemblIds( useEnsemblIds );
                return writer.write( matrix, cs2gene, destDir );
            } catch ( Exception e ) {
                PathUtils.deleteDirectory( destDir );
                throw e;
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public Path writeOrLocateMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        Path dest = getOutputFile( getDataOutputFilename( ee, qt, MEX_SC_DATA_SUFFIX ) );
        if ( !forceWrite && Files.exists( dest ) ) {
            return dest;
        }
        int written = writeMexSingleCellExpressionData( ee, qt, false, useStreaming, fetchSize, forceWrite, dest );
        log.info( "Wrote " + written + " vectors for " + qt + " to " + dest + "." );
        return dest;
    }

    @Override
    public void writeDiffExArchiveFile( BioAssaySet experimentAnalyzed, DifferentialExpressionAnalysis analysis,
            @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException {
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        AtomicBoolean hasSignificantBatchConfound = new AtomicBoolean();
        analysis = helperService.getAnalysis( experimentAnalyzed, analysis, geneAnnotations, hasSignificantBatchConfound );
        Path f = this.getOutputFile( this.getDiffExArchiveFileName( analysis ) );
        log.info( "Creating differential expression analysis archive file: " + f );
        Lock lock = acquirePathLock( f, true );
        try ( OutputStream stream = openFile( f, false ) ) {
            new DiffExAnalysisResultSetWriter().write( analysis, geneAnnotations, config, hasSignificantBatchConfound.get(), stream );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Path copyMetadataFile( ExpressionExperiment ee, Path existingFile, ExpressionExperimentMetaFileType type, boolean forceWrite ) throws IOException {
        Assert.isTrue( !type.isDirectory(), "Copy metadata file to a directory is not supported." );
        Assert.isTrue( Files.isReadable( existingFile ), existingFile + " must be readable." );
        Path destinationFile = metadataDir.resolve( getEEFolderName( ee ) ).resolve( type.getFileName( ee ) );
        if ( !forceWrite && Files.exists( destinationFile ) ) {
            throw new RuntimeException( "Metadata file already exists, use forceWrite is not override." );
        }
        Lock lock = acquirePathLock( destinationFile, true );
        try {
            PathUtils.createParentDirectories( destinationFile );
            Files.copy( existingFile, destinationFile, StandardCopyOption.REPLACE_EXISTING );
            return destinationFile;
        } catch ( Exception e ) {
            Files.deleteIfExists( destinationFile );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix( ee, qt, geneAnnotations );
        return new MatrixWriter( gemmaHostUrl ).writeWithStringifiedGeneAnnotations( writer, matrix, geneAnnotations );
    }

    @Override
    public void writeDesignMatrix( ExpressionExperiment ee, Writer writer ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }
        new ExperimentalDesignWriter( gemmaHostUrl ).write( writer, ee, true );
    }

    @Override
    public int writeProcessedExpressionData( ExpressionExperiment ee, boolean filtered, Writer writer ) throws FilteringException, IOException {
        ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix( ee, filtered )
                .orElseThrow( () -> new IllegalArgumentException( ee + " has no processed data vectors." ) );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        return new MatrixWriter( gemmaHostUrl ).writeWithStringifiedGeneAnnotations( writer, matrix, helperService.getGeneAnnotationsAsStringsByProbe( ads ) );
    }

    @Override
    public Path writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        Path f = this.getOutputFile( this.getCoexpressionDataFilename( ee ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        Collection<CoexpressionValueObject> geneLinks = helperService.getGeneLinks( ee );

        // Write coexpression data to file (zipped of course)
        Lock lock = acquirePathLock( f, true );
        try ( Writer writer = new OutputStreamWriter( openFile( f, true ), StandardCharsets.UTF_8 ) ) {
            ExpressionDataFileServiceImpl.log.info( "Creating new coexpression data file: " + f );
            new CoexpressionWriter().write( ee, geneLinks, writer );
            return f;
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Path> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException {
        // randomize file name if temporary in case of access by more than one user at once
        String result;
        if ( filtered ) {
            result = getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        } else {
            result = getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        }
        Path f = this.getOutputFile( result );
        Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

        if ( this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }

        return this.writeDataFile( ee, filtered, f, true );
    }

    @Override
    public Optional<Path> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered, int timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException {
        // randomize file name if temporary in case of access by more than one user at once
        String result;
        if ( filtered ) {
            result = getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        } else {
            result = getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        }
        Path f = this.getOutputFile( result, timeout, timeUnit );
        Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

        if ( this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }

        return this.writeDataFile( ee, filtered, f, true );
    }

    @Override
    public Path writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        Path f = this.getOutputFile( this.getDataOutputFilename( ee, type, TABULAR_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        Collection<BulkExpressionDataVector> vectors = helperService.getVectors( ee, type, geneAnnotations );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors for " + type );
        }

        BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        int written = this.writeMatrix( f, geneAnnotations, expressionDataMatrix, true );
        log.info( "Wrote " + written + " vectors for " + type + "." );
        return f;
    }

    @Override
    public Path writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException {
        Path f = this.getOutputFile( this.getDataOutputFilename( ee, type, TABULAR_BULK_DATA_FILE_SUFFIX ), timeout, timeUnit );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        Collection<BulkExpressionDataVector> vectors = helperService.getVectors( ee, type, geneAnnotations );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors for " + type );
        }

        BulkExpressionDataMatrix<?> matrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        int written = this.writeMatrix( f, geneAnnotations, matrix, timeout, timeUnit );
        log.info( "Wrote " + written + " vectors for " + type + "." );
        return f;
    }

    @Override
    public Optional<Path> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            return Optional.empty();
        }
        Path f = this.getOutputFile( this.getDesignFileName( ee ) );
        Date check = ee.getCurationDetails().getLastUpdated();
        if ( check != null && this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }
        Lock lock = acquirePathLock( f, true );
        try ( Writer writer = new OutputStreamWriter( openFile( f, true ), StandardCharsets.UTF_8 ) ) {
            writeDesignMatrix( ee, writer );
            return Optional.of( f );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Path> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            return Optional.empty();
        }
        Path f = this.getOutputFile( this.getDesignFileName( ee ), timeout, timeUnit );
        Date check = ee.getCurationDetails().getLastUpdated();
        if ( check != null && this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }
        Lock lock = tryAcquirePathLock( f, true, timeout, timeUnit );
        try ( Writer writer = new OutputStreamWriter( openFile( f, true ), StandardCharsets.UTF_8 ) ) {
            writeDesignMatrix( ee, writer );
            return Optional.of( f );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<Path> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        Collection<DifferentialExpressionAnalysis> analyses = helperService.getAnalyses( ee );
        Collection<Path> result = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            result.add( this.getDiffExpressionAnalysisArchiveFile( analysis, forceWrite ) );
        }
        return result;
    }

    @Override
    public Optional<Path> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException {
        // randomize file name if temporary in case of access by more than one user at once
        Path f = this.getOutputFile( getDataOutputFilename( ee, filtered, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return Optional.of( f );
        }

        Optional<ExpressionDataDoubleMatrix> matrix = helperService.getDataMatrix( ee, filtered );
        if ( matrix.isPresent() ) {
            int written = this.writeJson( f, matrix.get() );
            log.info( "Wrote " + written + " vectors to " + f + "." );
            return Optional.of( f );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Path writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        Path f = getOutputFile( getDataOutputFilename( ee, type, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        Collection<BulkExpressionDataVector> vectors = helperService.getVectors( ee, type );

        int written = writeJson( f, ExpressionDataMatrixBuilder.getMatrix( vectors ) );
        log.info( "Wrote " + written + " vectors for " + type + " to " + f );
        return f;
    }

    /**
     * Checks whether the given file is ok to return, or it should be regenerated.
     *
     * @param forceWrite whether the file should be overridden even if found.
     * @param f          the file to check.
     * @param check      the file will be considered invalid after this date.
     * @return true, if the given file is ok to be returned, false if it should be regenerated.
     */
    private boolean checkFileOkToReturn( boolean forceWrite, Path f, Date check ) {
        Date modified = new Date( f.toFile().lastModified() );
        if ( Files.exists( f ) ) {
            if ( forceWrite ) {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_FORCED, f ) );
            } else if ( modified.after( check ) ) {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_OUTDATED, f ) );
            } else {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_EXISTS, f ) );
                return true;
            }
        } else if ( !Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log
                    .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_NOT_EXISTS, f ) );
        }

        return false;
    }

    private String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign"
                + ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    private String getDiffExArchiveFileName( DifferentialExpressionAnalysis diff ) {
        BioAssaySet experimentAnalyzed = diff.getExperimentAnalyzed();

        ExpressionExperiment ee;
        if ( experimentAnalyzed instanceof ExpressionExperiment ) {
            ee = ( ExpressionExperiment ) experimentAnalyzed;
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) experimentAnalyzed;
            ee = subset.getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Don't know about " + experimentAnalyzed.getClass().getName() );
        }

        return experimentAnalyzed.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_diffExpAnalysis"
                + ( diff.getId() != null ? "_" + diff.getId() : "" )
                + ".zip";
    }

    private Path getDiffExpressionAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, boolean forceCreate ) throws IOException {
        String filename = this.getDiffExArchiveFileName( analysis );
        Path f = this.getOutputFile( filename );

        // Force create if file is older than one year
        if ( !forceCreate && Files.exists( f ) ) {
            Date d = new Date( f.toFile().lastModified() );
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.YEAR, -1 );
            forceCreate = d.before( new Date( calendar.getTimeInMillis() ) );
        }

        // If not force create and the file exists (can be read from), return the existing file.
        if ( !forceCreate && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        // (Re-)create the file
        this.writeDiffExArchiveFile( analysis.getExperimentAnalyzed(), analysis, null );

        return f;
    }

    /**
     * Obtain a filename for writing the processed data.
     */
    private String getDataOutputFilename( ExpressionExperiment ee, boolean filtered, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    private String getDataOutputFilename( ExpressionExperiment ee, QuantitationType type, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + "_expmat.unfilt" + suffix;
    }

    /**
     * Obtain the filename for writing coexpression data.
     */
    private String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp"
                + ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    /**
     * Resolve a filename in the {@link #dataDir} directory.
     * <p>
     * If the file is locked for writing, this method will wait until the lock is released.
     * @param filename without the path - that is, just the name of the file
     * @return File, with location in the appropriate target directory.
     */
    private Path getOutputFile( String filename ) {
        Path fullFilePath = dataDir.resolve( filename );
        Lock lock = acquirePathLock( fullFilePath, false );
        try {
            if ( Files.exists( fullFilePath ) ) {
                return fullFilePath;
            }
            return fullFilePath;
        } finally {
            lock.unlock();
        }
    }

    private Path getOutputFile( String filename, long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException {
        Path fullFilePath = dataDir.resolve( filename );
        Lock lock = tryAcquirePathLock( fullFilePath, false, timeout, timeUnit );
        try {
            if ( Files.exists( fullFilePath ) ) {
                return fullFilePath;
            }
            return fullFilePath;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param compress if true, file will be output in GZIP format.
     */
    private Optional<Path> writeDataFile( ExpressionExperiment ee, boolean filtered, Path f, boolean compress )
            throws IOException, FilteringException {
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        Optional<ExpressionDataDoubleMatrix> matrix = helperService.getDataMatrix( ee, filtered, geneAnnotations );
        if ( matrix.isPresent() ) {
            ExpressionDataFileServiceImpl.log.info( "Creating new tabular expression data file: " + f );
            int written = this.writeMatrix( f, geneAnnotations, matrix.get(), compress );
            log.info( "Wrote " + written + " vectors to " + f + "." );
            return Optional.of( f );
        } else {
            return Optional.empty();
        }
    }

    private int writeJson( Path file, BulkExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {
        Lock lock = acquirePathLock( file, true );
        try ( Writer writer = new OutputStreamWriter( openFile( file, true ), StandardCharsets.UTF_8 ) ) {
            ExpressionDataFileServiceImpl.log.info( "Creating new JSON expression data file: " + file );
            return new MatrixWriter( gemmaHostUrl ).writeJSON( writer, expressionDataMatrix );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private int writeMatrix( Path file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix, boolean gzipped ) throws IOException {
        Lock lock = acquirePathLock( file, true );
        try ( Writer writer = new OutputStreamWriter( openFile( file, gzipped ), StandardCharsets.UTF_8 ) ) {
            ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + file );
            return new MatrixWriter( gemmaHostUrl ).writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private int writeMatrix( Path file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix, long timeout, TimeUnit timeUnit ) throws IOException, InterruptedException, TimeoutException {
        Lock lock = tryAcquirePathLock( file, true, timeout, timeUnit );
        try ( Writer writer = new OutputStreamWriter( openFile( file, true ), StandardCharsets.UTF_8 ) ) {
            ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + file );
            return new MatrixWriter( gemmaHostUrl ).writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Open a given path for writing, ensuring that all necessary parent directories are created.
     */
    private OutputStream openFile( Path file, boolean gzipped ) throws IOException {
        PathUtils.createParentDirectories( file );
        return gzipped ? new GZIPOutputStream( Files.newOutputStream( file ) ) : Files.newOutputStream( file );
    }

    private Consumer<SingleCellExpressionDataVector> createStreamMonitor( long numVecs ) {
        if ( !log.isDebugEnabled() ) {
            return v -> {
                // empty on purpose, debug logging is not enabled
            };
        }
        return new Consumer<SingleCellExpressionDataVector>() {
            final StopWatch timer = StopWatch.createStarted();
            final AtomicInteger i = new AtomicInteger();

            @Override
            public void accept( SingleCellExpressionDataVector x ) {
                int done = i.incrementAndGet();
                if ( done % 100 == 0 ) {
                    log.debug( String.format( "Processed %d/%d vectors (%f.2 vectors/sec)", done, numVecs, 1000.0 * done / timer.getTime() ) );
                }
            }
        };
    }

    private final Map<Path, ReadWriteLock> fileLocks = new WeakHashMap<>();

    /**
     * Lock a given path.
     * TODO: implement file-level locking with {@link java.nio.channels.FileChannel} and {@link java.nio.channels.FileLock}
     *       to prevent other processes from writing to the same file.
     * @param exclusive make the lock exclusive for the purpose of creating of modifying the path
     */
    private synchronized Lock acquirePathLock( Path path, boolean exclusive ) {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, k -> new ReentrantReadWriteLock() );
        if ( exclusive ) {
            rwLock.writeLock().lock();
            return rwLock.writeLock();
        } else {
            rwLock.readLock().lock();
            return rwLock.readLock();
        }
    }

    /**
     * Attempt to lock a path.
     */
    private synchronized Lock tryAcquirePathLock( Path path, boolean exclusive, long timeout, TimeUnit timeUnit ) throws TimeoutException, InterruptedException {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, k -> new ReentrantReadWriteLock() );
        if ( exclusive ) {
            if ( rwLock.writeLock().tryLock( timeout, timeUnit ) ) {
                return rwLock.writeLock();
            } else {
                throw new TimeoutException();
            }
        } else {
            if ( rwLock.readLock().tryLock( timeout, timeUnit ) ) {
                return rwLock.readLock();
            } else {
                throw new TimeoutException();
            }
        }
    }
}
