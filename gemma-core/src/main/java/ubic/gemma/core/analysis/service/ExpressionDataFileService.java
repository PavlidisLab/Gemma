/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.service;

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author paul
 */
public interface ExpressionDataFileService {

    /**
     * A locked path.
     */
    interface LockedPath extends AutoCloseable {

        /**
         * Retrieve the path being locked.
         */
        Path getPath();

        /**
         * Indicate if the lock is shared.
         */
        boolean isShared();

        /**
         * Release the lock.
         */
        @Override
        void close();

        /**
         * Release the lock and obtain the underlying {@link Path} object.
         */
        @WillClose
        Path closeAndGetPath();

        /**
         * Convert this lock to an exclusive lock.
         * <p>
         * This lock will be closed as a result.
         * @throws IllegalStateException if this lock is already exclusive
         */
        @WillClose
        LockedPath toExclusive();

        /**
         * Try to convert this lock to an exclusive lock.
         * <p>
         * This lock will be closed as a result.
         * @throws IllegalStateException if this lock is already exclusive
         */
        @WillClose
        LockedPath toExclusive( long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException;

        /**
         * Convert this lock to a shared lock.
         * <p>
         * This lock will be closed as a result.
         * @throws IllegalStateException if this lock is already shared
         */
        @WillClose
        LockedPath toShared();

        /**
         * Steal this lock.
         * <p>
         * Once stolen, this lock will no-longer be released when closed.
         * @throws IllegalStateException if this lock is already stolen
         */
        @WillClose
        LockedPath steal();
    }

    /**
     * Delete any existing design, coexpression, data, or differential expression data files.
     * <p>
     * Experiment metadata are not deleted, use {@link #deleteMetadataFile(ExpressionExperiment, ExpressionExperimentMetaFileType)}
     * for that purpose.
     * @see #deleteDesignFile(ExpressionExperiment)
     * @see #deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     * @see #deleteAllProcessedDataFiles(ExpressionExperiment)
     * @see #deleteAllAnnotatedFiles(ExpressionExperiment)
     */
    int deleteAllFiles( ExpressionExperiment ee );

    /**
     * Delete the experimental design file for a given experiment.
     */
    boolean deleteDesignFile( ExpressionExperiment ee );

    /**
     * Delete all files that contain platform annotations for a given experiment.
     * <p>
     * This includes all the data and analysis files.
     * @see #deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     * @see #deleteAllProcessedDataFiles(ExpressionExperiment)
     * @see #deleteDiffExArchiveFile(DifferentialExpressionAnalysis)
     */
    int deleteAllAnnotatedFiles( ExpressionExperiment ee );

    /**
     * Delete all data files for a given QT.
     * <p>
     * This includes all the possible file types enumerated in {@link ExpressionExperimentDataFileType}.
     */
    int deleteAllDataFiles( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Delete all the processed files for a given experiment.
     * <p>
     * This includes all the possible file types enumerated in {@link ExpressionExperimentDataFileType}.
     */
    int deleteAllProcessedDataFiles( ExpressionExperiment ee );

    /**
     * Delete all analyses files for a given experiment.
     * @see #deleteDiffExArchiveFile(DifferentialExpressionAnalysis)
     * @see #deleteCoexpressionDataFile(ExpressionExperiment)
     */
    int deleteAllAnalysisFiles( ExpressionExperiment ee );

    /**
     * Delete a diff. ex. analysis archive file for a given analysis.
     * @return true if any file was deleted
     */
    boolean deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis );

    /**
     * Delete a coexpression data file if it exists.
     */
    boolean deleteCoexpressionDataFile( ExpressionExperiment ee );

    /**
     * Locate a metadata file.
     * If the metadata file is represented by a directory (i.e. {@link ExpressionExperimentMetaFileType#isDirectory()}
     * is true), then the latest file in the directory is returned. If no such file exists. {@link Optional#empty()} is
     * returned.
     * <p>
     * If the data file is being written (i.e. with {@link #copyMetadataFile(ExpressionExperiment, Path, ExpressionExperimentMetaFileType, boolean)},
     * this method will block until it is completed.
     * @return the metadata file or empty if none is present in the case of a directory-structured metadata
     */
    Optional<LockedPath> getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type, boolean exclusive ) throws IOException;

    Optional<LockedPath> getMetadataFile(ExpressionExperiment ee, String filename, boolean exclusive) throws IOException;

    /**
     * Copy a metadata file to the location of a given metadata type.
     * <p>
     * Writing to a directory (i.e. {@link ExpressionExperimentMetaFileType#isDirectory()} is true) is not supported.
     * <p>
     * If the metadata file is in use, this method will block until it is released.
     * @param existingFile file to copy, must exist
     * @param forceWrite   override any existing metadata file
     * @return the resulting metadata file, which can also be retrieved with {@link #getMetadataFile(ExpressionExperiment, ExpressionExperimentMetaFileType, boolean)}
     */
    Path copyMetadataFile( ExpressionExperiment ee, Path existingFile, ExpressionExperimentMetaFileType type, boolean forceWrite ) throws IOException;

    /**
     * Copy a generic metadata file.
     * <p>
     * This can only be used for metadata files that are not listed in {@link ExpressionExperimentMetaFileType}; using
     * any of these reserved filenames will result in an exception. In addition, {@code CHANGELOG.md} is also reserved,
     * use {@link ExpressionMetadataChangelogFileService} to manipulate it.
     */
    Path copyMetadataFile( ExpressionExperiment ee, Path existingFile, String filename, boolean forceWrite ) throws IOException;

    /**
     * Delete a metadata file.
     * <p>
     * If the metadata file is organized as a directory, it is deleted recursively.
     * <p>
     * If the metadata file is in use, this method will block until it is released.
     * @param type the type of metadata file to delete
     * @return true if a metadata file was deleted
     *
     */
    boolean deleteMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type ) throws IOException;

    /**
     * Locate any data file in the data directory.
     * <p>
     * A shared lock is acquired on the path and must be released when reading is done.
     */
    LockedPath getDataFile( String filename );

    LockedPath getDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type, long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException;

    /**
     * Delete a raw or single-cell data file if it exists.
     * <p>
     * If the data file is in use, this method will block until it is released.
     * @return true if the file was deleted, false if it did not exist
     */
    boolean deleteDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type ) throws IOException;

    /**
     * Delete a processed data file if it exists.
     * <p>
     * If the data file is in use, this method will block until it is released.
     * @return true if the file was deleted, false if it did not exist
     */
    boolean deleteDataFile( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type ) throws IOException;

    /**
     * Write single-cell expression data to a given writer for a given quantitation type in tabular format.
     *
     * @param ee           the experiment to use
     * @param qt           the quantitation type to retrieve
     * @param scaleType    a scale type to use or null to leave the data untransformed
     * @param useStreaming retrieve data in a streaming fashion
     * @see ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter
     */
    int writeTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useStreaming, int fetchSize, Writer writer ) throws IOException;

    /**
     * Write single-cell expression data to a standard location for a given quantitation type in tabular format.
     * @return a path where the vectors were written
     * @see #writeTabularSingleCellExpressionData(ExpressionExperiment, QuantitationType, ScaleType, boolean, int, Writer)
     * @see ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter
     */
    LockedPath writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException;

    /**
     * @see #writeOrLocateTabularSingleCellExpressionData(ExpressionExperiment, QuantitationType, boolean, int, boolean)
     * @throws RejectedExecutionException if the queue for creating data files is full
     */
    Future<Path> writeOrLocateTabularSingleCellExpressionDataAsync( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws RejectedExecutionException;

    /**
     * Write single-cell expression data to a given output stream for a given quantitation type.
     * <p>
     * Note: this method is memory intensive because the whole matrix needs to be in-memory to write each individual
     * sample matrices. Thus, no streaming is possible.
     *
     * @param ee            the experiment to use
     * @param qt            the quantitation type to retrieve
     * @param scaleType     a scale type to use or null to leave the data untransformed
     * @param useEnsemblIds use Ensembl IDs instead of official gene symbols
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useEnsemblIds, OutputStream stream ) throws IOException;

    /**
     * Write single-cell expression data to a given output stream for a given quantitation type.
     *
     * @param ee            the experiment to use
     * @param qt            the quantitation type to retrieve
     * @param scaleType     a scale type to use or null to leave the data untransformed
     * @param useEnsemblIds use Ensembl IDs instead of official gene symbols
     * @param useStreaming  retrieve data in a streaming fashion
     * @param fetchSize     fetch size to use for streaming
     * @param forceWrite    whether to force write and ignore any pre-existing directory
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, @Nullable ScaleType scaleType, boolean useEnsemblIds, boolean useStreaming, int fetchSize, boolean forceWrite, Path destDir ) throws IOException;

    /**
     * Write single-cell expression data to a standard location for a given quantitation type.
     * @return a path where the vectors were written
     * @see #writeMexSingleCellExpressionData(ExpressionExperiment, QuantitationType, ScaleType, boolean, boolean, int, boolean, Path)
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    LockedPath writeOrLocateMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException;

    /**
     * @see #writeOrLocateMexSingleCellExpressionData(ExpressionExperiment, QuantitationType, boolean, int, boolean)
     * @throws RejectedExecutionException if the queue for creating data files is full
     */
    Future<Path> writeOrLocateMexSingleCellExpressionDataAsync( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws RejectedExecutionException;

    /**
     * Write raw expression data to a given writer for a given quantitation type.
     * <p>
     * Note: For compression, wrap a {@link java.util.zip.GZIPOutputStream} with a {@link java.io.OutputStreamWriter}.
     * To write to a string, consider using {@link java.io.StringWriter}.
     *
     * @param ee     the expression experiment
     * @param qt     a quantitation type to use
     * @param writer the destination for the raw expression data
     * @throws IOException if operations with the writer fails
     */
    int writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException;

    /**
     * Write processed expression data to a given writer for a given quantitation type.
     * <p>
     * Note: For compression, wrap a {@link java.util.zip.GZIPOutputStream} with a {@link java.io.OutputStreamWriter}.
     * To write to a string, consider using {@link java.io.StringWriter}.
     *
     * @param ee     the expression experiment
     * @param writer the destination for the raw expression data
     * @throws IOException if operations with the writer fails
     */
    int writeProcessedExpressionData( ExpressionExperiment ee, boolean filtered, Writer writer ) throws FilteringException, IOException;

    /**
     * Writes out the experimental design for the given experiment.
     * <p>
     * The bioassays (col 0) matches the header row of the data matrix printed out by the {@link MatrixWriter}.
     * @see ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter
     */
    void writeDesignMatrix( ExpressionExperiment ee, Writer writer ) throws IOException;

    /**
     * Write or located the coexpression data file for a given experiment
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return file
     */
    LockedPath writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only). It will be gzip-compressed.
     * The file will be regenerated even if one already exists if the forceWrite parameter is true, or if there was
     * a recent change (more recent than the last modified date of the existing file) to any of the experiments platforms.
     *
     * @param ee         the experiment
     * @param filtered   filtered
     * @param forceWrite force re-write even if file already exists and is up to date.
     * @return file, or empty if the experiment has no processed vectors
     */
    Optional<LockedPath> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean filtered, boolean forceWrite ) throws FilteringException, IOException;

    Optional<LockedPath> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean filtered, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException, FilteringException;

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     *
     * @param forceWrite To not return the existing file, but create it anew.
     * @param type       the quantitation type
     * @return file
     */
    LockedPath writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException;

    LockedPath writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType qt, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException;

    /**
     * Locate or create an experimental design file for a given experiment.
     * The file will be regenerated even if one already exists if the forceWrite parameter is true, or if there was
     * a recent change (more recent than the last modified date of the existing file) to any of the experiments platforms.
     *
     * @param ee         the experiment
     * @param forceWrite force re-write even if file already exists and is up to date
     * @return a file or empty if the experiment does not have a design
     * @see #writeDesignMatrix(ExpressionExperiment, Writer)
     */
    Optional<LockedPath> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    Optional<LockedPath> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite, long timeout, TimeUnit timeUnit ) throws TimeoutException, IOException, InterruptedException;

    /**
     * @see #writeOrLocateProcessedDataFile(ExpressionExperiment, boolean, boolean)
     */
    Optional<LockedPath> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException;

    /**
     * @see #writeOrLocateRawExpressionDataFile(ExpressionExperiment, QuantitationType, boolean)
     */
    LockedPath writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException;

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return collection of files, one per analysis.
     */
    Collection<LockedPath> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    /**
     * Locate or create the differential expression data file(s) for a given experiment. We generate an archive that
     * contains following files: - differential expression analysis file (q-values per factor) - file for each result
     * set with contrasts info (such as fold change for each factor value)
     *
     * @param analysisId  analysis ID
     * @param forceCreate whether to force creation
     * @return file
     */
    LockedPath writeOrLocateDiffExArchiveFile( Long analysisId, boolean forceCreate ) throws IOException;

    LockedPath writeDiffExAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException;
}