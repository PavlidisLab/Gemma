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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.*;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.*;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

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
    @Autowired
    private EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private BuildInfo buildInfo;
    @Autowired
    @Qualifier("expressionDataFileTaskExecutor")
    private AsyncTaskExecutor expressionDataFileTaskExecutor;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    @Override
    public int deleteAllFiles( ExpressionExperiment ee ) {
        int deleted = 0;

        // NOTE: Never ever delete the metadata directory, it may contain precious information that was not generated by
        //       Gemma

        // design file
        if ( deleteDesignFile( ee ) ) {
            deleted++;
        }

        // per-QT output files (raw and single-cell data)
        for ( QuantitationType qt : expressionExperimentService.getQuantitationTypes( ee ) ) {
            deleted += deleteAllDataFiles( ee, qt );
        }

        // processed data files
        deleted += deleteAllProcessedDataFiles( ee );

        // analysis files which are generally derived from the processed data
        deleted += deleteAllAnalysisFiles( ee );

        return deleted;
    }

    @Override
    public boolean deleteDesignFile( ExpressionExperiment ee ) {
        return this.deleteAndLog( dataDir.resolve( getDesignFileName( ee ) ) );
    }

    @Override
    public int deleteAllAnnotatedFiles( ExpressionExperiment ee ) {
        int deleted = 0;

        // per-QT output files (raw and single-cell data)
        for ( QuantitationType qt : expressionExperimentService.getQuantitationTypes( ee ) ) {
            deleted += deleteAllDataFiles( ee, qt );
        }

        // processed data files
        deleted += deleteAllProcessedDataFiles( ee );

        // diff. ex. files
        Collection<DifferentialExpressionAnalysis> analyses = helperService.getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            if ( deleteDiffExArchiveFile( analysis ) ) {
                deleted++;
            }
        }

        // we're not deleting coexpression files here, they are not annotated

        return deleted;
    }

    @Override
    public int deleteAllDataFiles( ExpressionExperiment ee, QuantitationType qt ) {
        int deleted = 0;
        for ( ExpressionExperimentDataFileType type : ExpressionExperimentDataFileType.values() ) {
            try {
                if ( deleteDataFile( ee, qt, type ) ) {
                    deleted++;
                }
            } catch ( IllegalArgumentException e ) {
                // ignore, this is just an illegal combination of QT and file type
            } catch ( IOException e ) {
                log.error( "Failed to delete data file for " + qt + ".", e );
            }
        }
        return deleted;
    }

    @Override
    public int deleteAllProcessedDataFiles( ExpressionExperiment ee ) {
        int deleted = 0;
        for ( ExpressionExperimentDataFileType type : ExpressionExperimentDataFileType.values() ) {
            try {
                deleteDataFile( ee, true, type );
            } catch ( IllegalArgumentException e ) {
                // ignore, this is just an illegal combination of QT and file type
            } catch ( IOException e ) {
                log.error( "Failed to delete: " + getDataFileInternal( ee, true, type ), e );
            }
            try {
                deleteDataFile( ee, false, type );
            } catch ( IllegalArgumentException e ) {
                // ignore, this is just an illegal combination of QT and file type
            } catch ( IOException e ) {
                log.error( "Failed to delete: " + getDataFileInternal( ee, false, type ), e );
            }
        }
        return deleted;
    }

    @Override
    public int deleteAllAnalysisFiles( ExpressionExperiment ee ) {
        int deleted = 0;

        // diff ex files
        Collection<DifferentialExpressionAnalysis> analyses = helperService.getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            if ( deleteDiffExArchiveFile( analysis ) ) {
                deleted++;
            }
        }

        // coexpression file
        if ( deleteCoexpressionDataFile( ee ) ) {
            deleted++;
        }

        return deleted;
    }

    @Override
    public boolean deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) {
        return deleteAndLog( dataDir.resolve( getDiffExArchiveFileName( analysis ) ) );
    }

    @Override
    public boolean deleteCoexpressionDataFile( ExpressionExperiment ee ) {
        return deleteAndLog( dataDir.resolve( getCoexpressionDataFilename( ee ) ) );
    }

    private boolean deleteAndLog( Path path ) {
        try ( LockedPath lockedPath = acquirePathLock( path, true ) ) {
            if ( Files.deleteIfExists( lockedPath.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( "Deleted: " + lockedPath.getPath() );
                return true;
            }
        } catch ( IOException e ) {
            ExpressionDataFileServiceImpl.log.error( "Failed to delete: " + path, e );
        }
        return false;
    }

    @Override
    public Optional<LockedPath> getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type, boolean exclusive ) throws IOException {
        try ( LockedPathImpl lock = acquirePathLock( metadataDir.resolve( getEEFolderName( ee ) ).resolve( type.getFileName( ee ) ), exclusive ) ) {
            if ( type.isDirectory() ) {
                if ( !Files.exists( lock.getPath() ) ) {
                    return Optional.empty();
                }
                // If this is a directory, check if we can read the most recent file.
                try ( Stream<Path> files = Files.list( lock.getPath() ) ) {
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
                            } ) )
                            .map( lock::stealWithPath );
                }
            } else {
                // lock will be managed by the LockedFile
                return Optional.of( lock.steal() );
            }
        }
    }

    @Override
    public Path copyMetadataFile( ExpressionExperiment ee, Path existingFile, ExpressionExperimentMetaFileType type, boolean forceWrite ) throws IOException {
        Assert.isTrue( !type.isDirectory(), "Copy metadata file to a directory is not supported." );
        Assert.isTrue( Files.isReadable( existingFile ), existingFile + " must be readable." );
        Path destinationFile = metadataDir.resolve( getEEFolderName( ee ) ).resolve( type.getFileName( ee ) );
        return copyMetadataFileInternal( existingFile, destinationFile, forceWrite );
    }

    @Override
    public Path copyMetadataFile( ExpressionExperiment ee, Path existingFile, String filename, boolean forceWrite ) throws IOException {
        Assert.isTrue( Files.isReadable( existingFile ), existingFile + " must be readable." );
        Set<String> reservedMetadataFilenames = new HashSet<>();
        reservedMetadataFilenames.add( "CHANGELOG.md" );
        for ( ExpressionExperimentMetaFileType type : ExpressionExperimentMetaFileType.values() ) {
            reservedMetadataFilenames.add( type.getFileName( ee ) );
        }
        Assert.isTrue( !reservedMetadataFilenames.contains( filename ), filename + " is reserved for metadata files, use a different filename." );
        Path destinationFile = metadataDir.resolve( getEEFolderName( ee ) ).resolve( filename );
        return copyMetadataFileInternal( existingFile, destinationFile, forceWrite );
    }

    private Path copyMetadataFileInternal( Path existingFile, Path destinationFile, boolean forceWrite ) throws IOException {
        if ( !forceWrite && Files.exists( destinationFile ) ) {
            throw new RuntimeException( "Metadata file already exists, use forceWrite is not override." );
        }
        try ( LockedPath ignored = acquirePathLock( destinationFile, true ) ) {
            PathUtils.createParentDirectories( destinationFile );
            log.info( "Copying metadata file: " + existingFile + " to " + destinationFile + "." );
            Files.copy( existingFile, destinationFile, StandardCopyOption.REPLACE_EXISTING );
            return destinationFile;
        } catch ( Exception e ) {
            Files.deleteIfExists( destinationFile );
            throw e;
        }
    }

    @Override
    public boolean deleteMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type ) throws IOException {
        Path destinationFile = metadataDir.resolve( getEEFolderName( ee ) ).resolve( type.getFileName( ee ) );
        try ( LockedPath ignored = acquirePathLock( destinationFile, true ) ) {
            if ( Files.exists( destinationFile ) ) {
                log.info( "Deleting metadata file: " + destinationFile + "." );
                PathUtils.delete( destinationFile );
                return true;
            }
            return false;
        }
    }

    @Override
    public LockedPath getDataFile( String filename ) {
        return getOutputFile( filename );
    }

    @Override
    public LockedPath getDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type, long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException {
        return getOutputFile( getDataOutputFilename( ee, qt, requireNonNull( getDataSuffix( qt, type ), "Couldn't determine filename suffix for " + qt + " and " + type + "." ) ), timeout, timeUnit );
    }

    @Nullable
    private String getDataSuffix( QuantitationType qt, ExpressionExperimentDataFileType type ) {
        Class<? extends DataVector> dataType = quantitationTypeService.getDataVectorType( qt );
        if ( dataType == null ) {
            return null; // happens if the QT was deleted
        }
        switch ( type ) {
            case TABULAR:
                if ( BulkExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    return TABULAR_BULK_DATA_FILE_SUFFIX;
                } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    return TABULAR_SC_DATA_SUFFIX;
                } else {
                    throw new IllegalArgumentException( "Unknown vector type: " + dataType );
                }
            case JSON:
                Assert.isAssignable( BulkExpressionDataVector.class, dataType );
                return JSON_BULK_DATA_FILE_SUFFIX;
            case MEX:
                Assert.isAssignable( SingleCellExpressionDataVector.class, dataType );
                return MEX_SC_DATA_SUFFIX;
            default:
                throw new IllegalArgumentException( "Unsupported data file type: " + type );
        }
    }

    private Path getDataFileInternal( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type ) {
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
        return dataDir.resolve( getDataOutputFilename( ee, filtered, suffix ) );
    }

    @Override
    public boolean deleteDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type ) throws IOException {
        String suffix = getDataSuffix( qt, type );
        if ( suffix != null ) {
            return deleteDataFileInternal( ee, qt, suffix );
        } else {
            // QT was likely deleted, try all possible suffixes
            log.debug( "Could not determine filename suffix for " + qt + " and " + type + ", trying all possible suffixes..." );
            String[] suffixes;
            switch ( type ) {
                case TABULAR:
                    suffixes = new String[] { TABULAR_BULK_DATA_FILE_SUFFIX, TABULAR_SC_DATA_SUFFIX };
                    break;
                case MEX:
                    suffixes = new String[] { MEX_SC_DATA_SUFFIX };
                    break;
                case JSON:
                    suffixes = new String[] { JSON_BULK_DATA_FILE_SUFFIX };
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported data file type: " + type );
            }
            boolean deleted = false;
            for ( String suffix2 : suffixes ) {
                deleted |= deleteDataFileInternal( ee, qt, suffix2 );
            }
            return deleted;
        }
    }

    private boolean deleteDataFileInternal( ExpressionExperiment ee, QuantitationType qt, String suffix ) throws IOException {
        try ( LockedPath ignored = acquirePathLock( dataDir.resolve( getDataOutputFilename( ee, qt, suffix ) ), true ) ) {
            if ( Files.exists( ignored.getPath() ) ) {
                PathUtils.delete( ignored.getPath() );
                log.info( "Deleted raw data file: " + ignored.getPath() );
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean deleteDataFile( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type ) throws IOException {
        // we still want to delete the file even if there are no processed vectors
        try ( LockedPath ignored = acquirePathLock( getDataFileInternal( ee, filtered, type ), true ) ) {
            if ( Files.exists( ignored.getPath() ) ) {
                PathUtils.delete( ignored.getPath() );
                log.info( "Deleted processed data file: " + ignored.getPath() + "." );
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public int writeTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useStreaming, int fetchSize, Writer writer ) throws IOException {
        log.info( "Will write tabular data for " + qt + " to a stream." );
        return writeTabularSingleCellExpressionDataInternal( ee, qt, scaleType, useStreaming, fetchSize, writer );
    }

    @Override
    public LockedPath writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        try ( LockedPath dest = getOutputFile( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ) ) ) {
            if ( !forceWrite && Files.exists( dest.getPath() ) ) {
                return dest.steal();
            }
            try ( LockedPath lockedPath = dest.toExclusive(); Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                log.info( "Will write tabular data for " + qt + " to " + lockedPath.getPath() + "." );
                int written = writeTabularSingleCellExpressionDataInternal( ee, qt, null, useStreaming, fetchSize, writer );
                log.info( "Wrote " + written + " vectors to " + lockedPath.getPath() + "." );
                return lockedPath.toShared();
            } catch ( Exception e ) {
                Files.deleteIfExists( dest.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Future<Path> writeOrLocateTabularSingleCellExpressionDataAsync( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) {
        return expressionDataFileTaskExecutor.submit( () -> {
            try ( LockedPath lockedPath = writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming, fetchSize, forceWrite ) ) {
                return lockedPath.getPath();
            }
        } );
    }

    private int writeTabularSingleCellExpressionDataInternal( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useStreaming, int fetchSize, Writer writer ) throws IOException {
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        TabularMatrixWriter matrixWriter = new TabularMatrixWriter( entityUrlBuilder, buildInfo );
        matrixWriter.setScaleType( scaleType );
        if ( scaleType != null && qt.getScale() != scaleType ) {
            log.info( "Data will be converted from " + qt.getScale() + " to " + scaleType + "." );
        }
        if ( useStreaming ) {
            AtomicLong numVecs = new AtomicLong();
            try ( Stream<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, fetchSize ) ) {
                return matrixWriter.write( vectors.peek( createStreamMonitor( ExpressionDataFileServiceImpl.class.getName(), numVecs.get() ) ), cs2gene, writer );
            }
        } else {
            Collection<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene );
            return matrixWriter.write( vectors.stream().peek( createStreamMonitor( ExpressionDataFileServiceImpl.class.getName(), vectors.size() ) ), cs2gene, writer );
        }
    }

    @Override
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useEnsemblIds, OutputStream stream ) throws IOException {
        Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
        Collection<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene );
        log.info( "Will write MEX data for " + qt + " to a stream " + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
        if ( scaleType != null && qt.getScale() != scaleType ) {
            log.info( "Data will be converted from " + qt.getScale() + " to " + scaleType + "." );
        }
        DoubleSingleCellExpressionDataMatrix matrix = new DoubleSingleCellExpressionDataMatrix( vectors );
        MexMatrixWriter writer = new MexMatrixWriter();
        writer.setScaleType( scaleType );
        writer.setUseEnsemblIds( useEnsemblIds );
        return writer.write( matrix, cs2gene, stream );
    }

    @Override
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useEnsemblIds, boolean useStreaming, int fetchSize, boolean forceWrite, Path destDir ) throws IOException {
        if ( !forceWrite && Files.exists( destDir ) ) {
            throw new IllegalArgumentException( "Output directory " + destDir + " already exists." );
        }
        try ( LockedPath ignored = acquirePathLock( destDir, true ) ) {
            Map<CompositeSequence, Set<Gene>> cs2gene = new HashMap<>();
            MexMatrixWriter writer = new MexMatrixWriter();
            writer.setScaleType( scaleType );
            writer.setUseEnsemblIds( useEnsemblIds );
            if ( useStreaming ) {
                Map<BioAssay, Long> nnzBySample = new HashMap<>();
                AtomicLong numVecs = new AtomicLong();
                try ( Stream<SingleCellExpressionDataVector> vectors = helperService.getSingleCellVectors( ee, qt, cs2gene, numVecs, nnzBySample, fetchSize ) ) {
                    if ( Files.exists( destDir ) ) {
                        log.info( destDir + " already exists, removing..." );
                        PathUtils.deleteDirectory( destDir );
                    }
                    log.info( "Will write MEX data for " + qt + " to " + destDir + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
                    if ( scaleType != null && qt.getScale() != scaleType ) {
                        log.info( "Data will be converted from " + qt.getScale() + " to " + scaleType + "." );
                    }
                    return writer.write( vectors.peek( createStreamMonitor( ExpressionDataFileServiceImpl.class.getName(), numVecs.get() ) ), ( int ) numVecs.get(), nnzBySample, cs2gene, destDir );
                }
            } else {
                SingleCellExpressionDataMatrix<Double> matrix = helperService.getSingleCellMatrix( ee, qt, cs2gene );
                log.info( "Will write MEX data for " + qt + " to " + destDir + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
                if ( scaleType != null && qt.getScale() != scaleType ) {
                    log.info( "Data will be converted from " + qt.getScale() + " to " + scaleType + "." );
                }
                return writer.write( matrix, cs2gene, destDir );
            }
        } catch ( Exception e ) {
            PathUtils.deleteDirectory( destDir );
            throw e;
        }
    }

    @Override
    public LockedPath writeOrLocateMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        try ( LockedPath dest = getOutputFile( getDataOutputFilename( ee, qt, MEX_SC_DATA_SUFFIX ) ) ) {
            if ( !forceWrite && Files.exists( dest.getPath() ) ) {
                return dest.steal();
            }
            try ( LockedPath lockedPath = dest.toExclusive() ) {
                int written = writeMexSingleCellExpressionData( ee, qt, null, false, useStreaming, fetchSize, forceWrite, lockedPath.getPath() );
                log.info( "Wrote " + written + " vectors for " + qt + " to " + lockedPath.getPath() + "." );
                return lockedPath.toShared();
            }
        }
    }

    @Override
    public Future<Path> writeOrLocateMexSingleCellExpressionDataAsync( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) {
        return expressionDataFileTaskExecutor.submit( () -> {
            try ( LockedPath lockedPath = writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming, fetchSize, forceWrite ) ) {
                return lockedPath.getPath();
            }
        } );
    }

    @Override
    public int writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix( ee, qt, geneAnnotations );
        return new MatrixWriter( entityUrlBuilder, buildInfo ).writeWithStringifiedGeneAnnotations( writer, matrix, geneAnnotations );
    }

    @Override
    public void writeDesignMatrix( ExpressionExperiment ee, Writer writer ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }
        new ExperimentalDesignWriter( entityUrlBuilder, buildInfo ).write( writer, ee, true );
    }

    @Override
    public int writeProcessedExpressionData( ExpressionExperiment ee, boolean filtered, Writer writer ) throws FilteringException, IOException {
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix( ee, filtered, geneAnnotations );
        return new MatrixWriter( entityUrlBuilder, buildInfo ).writeWithStringifiedGeneAnnotations( writer, matrix, geneAnnotations );
    }

    @Override
    public LockedPath writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        try ( LockedPath f = this.getOutputFile( getCoexpressionDataFilename( ee ) ) ) {
            if ( !forceWrite && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f.steal();
            }

            // Write coexpression data to file (zipped of course)
            try ( LockedPath lockedPath = f.toExclusive(); Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                Collection<CoexpressionValueObject> geneLinks = helperService.getGeneLinks( ee );
                ExpressionDataFileServiceImpl.log.info( "Creating new coexpression data file: " + lockedPath.getPath() );
                new CoexpressionWriter( buildInfo ).write( ee, geneLinks, writer );
                return lockedPath.toShared();
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Optional<LockedPath> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean filtered, boolean forceWrite ) throws IOException, FilteringException {
        // randomize file name if temporary in case of access by more than one user at once
        String result;
        if ( filtered ) {
            result = getDataOutputFilename( ee, true, TABULAR_BULK_DATA_FILE_SUFFIX );
        } else {
            result = getDataOutputFilename( ee, false, TABULAR_BULK_DATA_FILE_SUFFIX );
        }
        try ( LockedPath f = this.getOutputFile( result ) ) {
            Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

            if ( this.checkFileOkToReturn( forceWrite, f.getPath(), check ) ) {
                return Optional.of( f.steal() );
            }

            if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
                return Optional.empty();
            }

            try ( LockedPath lockedPath = f.toExclusive(); Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + f );
                int written = writeProcessedExpressionData( ee, filtered, writer );
                log.info( "Wrote " + written + " vectors to " + lockedPath.getPath() + "." );
                return Optional.of( lockedPath.toShared() );
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Optional<LockedPath> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean filtered, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException, FilteringException {
        // randomize file name if temporary in case of access by more than one user at once
        String result;
        if ( filtered ) {
            result = getDataOutputFilename( ee, true, TABULAR_BULK_DATA_FILE_SUFFIX );
        } else {
            result = getDataOutputFilename( ee, false, TABULAR_BULK_DATA_FILE_SUFFIX );
        }
        try ( LockedPath f = this.getOutputFile( result, timeout, timeUnit ) ) {
            Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

            if ( this.checkFileOkToReturn( forceWrite, f.getPath(), check ) ) {
                return Optional.of( f.steal() );
            }

            if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
                return Optional.empty();
            }

            try ( LockedPath ignored = f.toExclusive(); Writer writer = openCompressedFile( ignored.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( "Creating new tabular expression data file: " + f );
                int written = writeProcessedExpressionData( ee, filtered, writer );
                log.info( "Wrote " + written + " vectors to " + f + "." );
                return Optional.of( ignored.toShared() );
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public LockedPath writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        try ( LockedPath f = this.getOutputFile( getDataOutputFilename( ee, type, TABULAR_BULK_DATA_FILE_SUFFIX ) ) ) {
            if ( !forceWrite && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f.steal();
            }
            try ( LockedPath lockedPath = f.toExclusive(); Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + lockedPath.getPath() );
                int written = writeRawExpressionData( ee, type, writer );
                log.info( "Wrote " + written + " vectors for " + type + "." );
                return lockedPath.toShared();
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public LockedPath writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException {
        try ( LockedPath f = this.getOutputFile( getDataOutputFilename( ee, type, TABULAR_BULK_DATA_FILE_SUFFIX ), timeout, timeUnit ) ) {
            if ( !forceWrite && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f.steal();
            }
            // FIXME: subtract elapsed time
            try ( LockedPath lockedPath = f.toExclusive( timeout, timeUnit );
                    Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + lockedPath.getPath() );
                int written = writeRawExpressionData( ee, type, writer );
                log.info( "Wrote " + written + " vectors for " + type + "." );
                return lockedPath.toShared();
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Optional<LockedPath> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            return Optional.empty();
        }
        try ( LockedPath f = this.getOutputFile( getDesignFileName( ee ) ) ) {
            Date check = ee.getCurationDetails().getLastUpdated();
            if ( check != null && this.checkFileOkToReturn( forceWrite, f.getPath(), check ) ) {
                return Optional.of( f.steal() );
            }
            try ( LockedPath lockedPath = f.toExclusive();
                    Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                writeDesignMatrix( ee, writer );
                return Optional.of( lockedPath.toShared() );
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Optional<LockedPath> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            return Optional.empty();
        }
        try ( LockedPath f = this.getOutputFile( getDesignFileName( ee ), timeout, timeUnit ) ) {
            Date check = ee.getCurationDetails().getLastUpdated();
            if ( check != null && this.checkFileOkToReturn( forceWrite, f.getPath(), check ) ) {
                return Optional.of( f.steal() );
            }
            try ( LockedPath lockedPath = f.toExclusive( timeout, timeUnit );
                    Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                writeDesignMatrix( ee, writer );
                return Optional.of( lockedPath.toShared() );
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public Collection<LockedPath> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        Collection<DifferentialExpressionAnalysis> analyses = helperService.getAnalyses( ee );
        Collection<LockedPath> result = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            result.add( this.writeOrLocateDiffExAnalysisArchiveFile( analysis, forceWrite ) );
        }
        return result;
    }

    @Override
    public LockedPath writeOrLocateDiffExArchiveFile( Long analysisId, boolean forceCreate ) throws IOException {
        DifferentialExpressionAnalysis analysis = helperService.getAnalysisById( analysisId );
        return writeOrLocateDiffExAnalysisArchiveFile( analysis, forceCreate );
    }

    private LockedPath writeOrLocateDiffExAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, boolean forceCreate ) throws IOException {
        String filename = getDiffExArchiveFileName( analysis );
        try ( LockedPath f = this.getOutputFile( filename ) ) {
            // Force create if file is older than one year
            if ( !forceCreate && Files.exists( f.getPath() ) ) {
                Date d = new Date( f.getPath().toFile().lastModified() );
                Calendar calendar = Calendar.getInstance();
                calendar.add( Calendar.YEAR, -1 );
                forceCreate = d.before( new Date( calendar.getTimeInMillis() ) );
            }

            // If not force create and the file exists (can be read from), return the existing file.
            if ( !forceCreate && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f.steal();
            }

            // (Re-)create the file
            try ( LockedPath lockedPath = f.toExclusive();
                    OutputStream stream = Files.newOutputStream( lockedPath.getPath() ) ) {
                log.info( "Creating differential expression analysis archive file: " + lockedPath.getPath() );
                writeDiffExArchive( analysis, stream );
                return lockedPath.toShared();
            }
        }
    }

    @Override
    public LockedPath writeDiffExAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException {
        try ( LockedPath lockedPath = acquirePathLock( dataDir.resolve( getDiffExArchiveFileName( analysis ) ), true );
                OutputStream stream = Files.newOutputStream( lockedPath.getPath() ) ) {
            log.info( "Creating differential expression analysis archive file: " + lockedPath.getPath() );
            writeDiffExArchive( analysis, stream );
            return lockedPath.toShared();
        }
    }

    private void writeDiffExArchive( DifferentialExpressionAnalysis analysis, OutputStream stream ) throws IOException {
        BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();
        Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
        AtomicBoolean hasSignificantBatchConfound = new AtomicBoolean();
        analysis = helperService.getAnalysis( experimentAnalyzed, analysis, geneAnnotations, hasSignificantBatchConfound );
        new DiffExAnalysisResultSetWriter( buildInfo ).write( analysis, geneAnnotations, null, hasSignificantBatchConfound.get(), stream );
    }

    @Override
    public Optional<LockedPath> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException {
        // randomize file name if temporary in case of access by more than one user at once
        try ( LockedPath f = this.getOutputFile( getDataOutputFilename( ee, filtered, JSON_BULK_DATA_FILE_SUFFIX ) ) ) {
            if ( !forceWrite && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return Optional.of( f.steal() );
            }
            if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
                return Optional.empty();
            }
            try ( LockedPath ignored = f.toExclusive();
                    Writer writer = openCompressedFile( ignored.getPath() ) ) {
                ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix( ee, filtered );
                ExpressionDataFileServiceImpl.log.info( "Creating new JSON expression data file: " + ignored.getPath() );
                int written = new MatrixWriter( entityUrlBuilder, buildInfo ).writeJSON( writer, matrix );
                log.info( "Wrote " + written + " vectors to " + ignored.getPath() + "." );
                return Optional.of( f.toShared() );
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
    }

    @Override
    public LockedPath writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        try ( LockedPath f = getOutputFile( getDataOutputFilename( ee, type, JSON_BULK_DATA_FILE_SUFFIX ) ) ) {
            if ( !forceWrite && Files.exists( f.getPath() ) ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f.steal();
            }
            try ( LockedPath lockedPath = f.toExclusive(); Writer writer = openCompressedFile( lockedPath.getPath() ) ) {
                Collection<BulkExpressionDataVector> vectors = helperService.getVectors( ee, type );
                BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
                ExpressionDataFileServiceImpl.log.info( "Creating new JSON expression data file: " + lockedPath.getPath() );
                int written = new MatrixWriter( entityUrlBuilder, buildInfo ).writeJSON( writer, expressionDataMatrix );
                log.info( "Wrote " + written + " vectors for " + type + " to " + lockedPath.getPath() );
                return lockedPath.toShared();
            } catch ( Exception e ) {
                Files.deleteIfExists( f.getPath() );
                throw e;
            }
        }
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

    /**
     * Resolve a filename in the {@link #dataDir} directory.
     * <p>
     * If the file is locked for writing, this method will wait until the lock is released.
     * @param filename without the path - that is, just the name of the file
     * @return File, with location in the appropriate target directory.
     */
    private LockedPath getOutputFile( String filename ) {
        return acquirePathLock( dataDir.resolve( filename ), false );
    }

    private LockedPath getOutputFile( String filename, long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException {
        return tryAcquirePathLock( dataDir.resolve( filename ), false, timeout, timeUnit );
    }

    /**
     * Open a given path for writing, ensuring that all necessary parent directories are created.
     */
    private Writer openCompressedFile( Path file ) throws IOException {
        return new OutputStreamWriter( new GZIPOutputStream( openFile( file ) ), StandardCharsets.UTF_8 );
    }

    /**
     * Open a given path for writing, ensuring that all necessary parent directories are created.
     */
    private OutputStream openFile( Path file ) throws IOException {
        PathUtils.createParentDirectories( file );
        return Files.newOutputStream( file );
    }

    private final Map<Path, ReadWriteLock> fileLocks = new WeakHashMap<>();

    /**
     * Lock a given path.
     * TODO: implement file-level locking with {@link java.nio.channels.FileChannel} and {@link java.nio.channels.FileLock}
     *       to prevent other processes from writing to the same file.
     * @param exclusive make the lock exclusive for the purpose of creating of modifying the path
     */
    private synchronized LockedPathImpl acquirePathLock( Path path, boolean exclusive ) {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, k -> new ReentrantReadWriteLock() );
        if ( exclusive ) {
            rwLock.writeLock().lock();
            return new LockedPathImpl( path, rwLock.writeLock(), false );
        } else {
            rwLock.readLock().lock();
            return new LockedPathImpl( path, rwLock.readLock(), true );
        }
    }

    /**
     * Attempt to lock a path.
     */
    private synchronized LockedPathImpl tryAcquirePathLock( Path path, boolean exclusive, long timeout, TimeUnit timeUnit ) throws TimeoutException, InterruptedException {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, k -> new ReentrantReadWriteLock() );
        if ( exclusive ) {
            if ( rwLock.writeLock().tryLock( timeout, timeUnit ) ) {
                return new LockedPathImpl( path, rwLock.writeLock(), false );
            } else {
                throw new TimeoutException();
            }
        } else {
            if ( rwLock.readLock().tryLock( timeout, timeUnit ) ) {
                return new LockedPathImpl( path, rwLock.readLock(), true );
            } else {
                throw new TimeoutException();
            }
        }
    }

    private class LockedPathImpl implements LockedPath {

        private final Path path;
        private final Lock lock;
        private final boolean shared;

        /**
         * Indicate if this lock was stolen and thus not to be closed.
         */
        private boolean stolen = false;

        /**
         * Indicate if this lock was converted (ether shared -> exclusive or exclusive -> shared) and thus already
         * closed.
         */
        private boolean converted = false;

        public LockedPathImpl( Path path, Lock lock, boolean shared ) {
            this.path = path;
            this.lock = lock;
            this.shared = shared;
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public void close() {
            if ( !stolen && !converted ) {
                lock.unlock();
            }
        }

        @Override
        public Path closeAndGetPath() {
            close();
            return path;
        }

        @Override
        public LockedPath toExclusive() {
            Assert.state( shared, "This lock is already exclusive." );
            Assert.state( !converted, "This lock was already converted to an exclusive lock." );
            Assert.state( !stolen, "This lock was stolen." );
            try {
                return acquirePathLock( closeAndGetPath(), true );
            } finally {
                converted = true;
            }
        }

        @Override
        public LockedPath toExclusive( long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException {
            Assert.state( shared, "This lock is already exclusive." );
            Assert.state( !converted, "This lock was already converted to an exclusive lock." );
            Assert.state( !stolen, "This lock was stolen." );
            try {
                return tryAcquirePathLock( closeAndGetPath(), true, timeout, timeUnit );
            } finally {
                converted = true;
            }
        }

        @Override
        public LockedPath toShared() {
            Assert.state( !shared, "This lock is already shared." );
            Assert.state( !converted, "This lock was already converted to a shared lock." );
            Assert.state( !stolen, "This lock was stolen." );
            try {
                return acquirePathLock( closeAndGetPath(), false );
            } finally {
                converted = true;
            }
        }

        @Override
        public LockedPath steal() {
            Assert.state( !stolen, "This lock was already stolen." );
            try {
                return new LockedPathImpl( path, lock, shared );
            } finally {
                stolen = true;
            }
        }

        /**
         * Steal this lock with a different path.
         */
        public LockedPathImpl stealWithPath( Path file ) {
            Assert.state( !stolen, "This lock was already stolen." );
            try {
                return new LockedPathImpl( file, lock, shared );
            } finally {
                stolen = true;
            }
        }

        @Override
        public String toString() {
            return path + " " + ( shared ? "[shared]" : "[exclusive]" );
        }
    }
}
