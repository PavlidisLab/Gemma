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
import ubic.gemma.core.datastructure.matrix.io.TsvUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author paul
 */
public interface ExpressionDataFileService {

    String DISCLAIMER = Arrays.stream( TsvUtils.GEMMA_CITATION_NOTICE ).map( line -> "# " + line + "\n" ).collect( Collectors.joining() );

    /**
     * Delete any existing coexpression, data, or differential expression data files.
     * <p>
     * Experiment metadata are not deleted.
     */
    void deleteAllFiles( ExpressionExperiment ee );

    /**
     * Locate or create the differential expression data file(s) for a given experiment. We generate an archive that
     * contains following files: - differential expression analysis file (q-values per factor) - file for each result
     * set with contrasts info (such as fold change for each factor value)
     *
     * @param analysisId  analysis ID
     * @param forceCreate whether to force creation
     * @return file
     */
    Path getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) throws IOException;

    /**
     * Locate a metadata file.
     */
    Path getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type );

    /**
     * Locate an data file.
     * @see #writeOrLocateRawExpressionDataFile(ExpressionExperiment, QuantitationType, boolean)
     */
    Optional<Path> getDataFile( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type );

    /**
     * Locate an data file.
     * @see #writeOrLocateRawExpressionDataFile(ExpressionExperiment, QuantitationType, boolean)
     */
    Path getDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type );

    /**
     * Locate an experimental design file.
     * @see #writeOrLocateDesignFile(ExpressionExperiment, boolean)
     */
    Optional<Path> getExperimentalDesignFile( ExpressionExperiment ee );

    /**
     * Create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     *
     * @param ee       the experiment
     * @param compress compress?
     * @param fileName file name
     * @param filtered fitlered?
     * @return file, or empty if the experiment has no processed expression data
     * @throws IOException when there are IO problems
     */
    Optional<Path> writeProcessedExpressionDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException, FilteringException;

    /**
     * Write single-cell expression data to a given writer for a given quantitation type in tabular format.
     *
     * @param ee           the experiment to use
     * @param qt           the quantitation type to retrieve
     * @param useStreaming retrieve data in a streaming fashion
     * @see ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter
     */
    int writeTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, Writer writer ) throws IOException;

    /**
     * Write single-cell expression data to a standard location for a given quantitation type in tabular format.
     * @return a path where the vectors were written
     * @see #writeTabularSingleCellExpressionData(ExpressionExperiment, QuantitationType, boolean, int, Writer)
     * @see ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter
     */
    Path writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException;

    /**
     * Write single-cell expression data to a given output stream for a given quantitation type.
     * <p>
     * Note: this method is memory intensive because the whole matrix needs to be in-memory to write each individual
     * sample matrices. Thus, no streaming is possible.
     *
     * @param ee            the experiment to use
     * @param qt            the quantitation type to retrieve
     * @param useEnsemblIds use Ensembl IDs instead of official gene symbols
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, OutputStream stream ) throws IOException;

    /**
     * Write single-cell expression data to a given output stream for a given quantitation type.
     *
     * @param ee            the experiment to use
     * @param qt            the quantitation type to retrieve
     * @param useEnsemblIds use Ensembl IDs instead of official gene symbols
     * @param useStreaming  retrieve data in a streaming fashion
     * @param fetchSize     fetch size to use for streaming
     * @param forceWrite    whether to force write and ignore any pre-existing directory
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, boolean useStreaming, int fetchSize, boolean forceWrite, Path destDir ) throws IOException;

    /**
     * Write single-cell expression data to a standard location for a given quantitation type.
     * @return a path where the vectors were written
     * @see #writeMexSingleCellExpressionData(ExpressionExperiment, QuantitationType, boolean, boolean, int, boolean, Path)
     * @see ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter
     */
    Path writeOrLocateMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException;

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
     * @see ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter
     */
    void writeDesignMatrix( ExpressionExperiment ee, Writer writer ) throws IOException;

    /**
     * Write or located the coexpression data file for a given experiment
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return file
     */
    Path writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only). It will be gzip-compressed.
     * The file will be regenerated even if one already exists if the forceWrite parameter is true, or if there was
     * a recent change (more recent than the last modified date of the existing file) to any of the experiments platforms.
     *
     * @param filtered   filtered
     * @param forceWrite force re-write even if file already exists and is up to date.
     * @param ee         the experiment
     * @return file, or empty if the experiment has no processed vectors
     */
    Optional<Path> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException;

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     *
     * @param forceWrite To not return the existing file, but create it anew.
     * @param type       the quantitation type
     * @return file
     */
    Path writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException;

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
    Optional<Path> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return collection of files, one per analysis.
     */
    Collection<Path> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) throws IOException;

    /**
     * @see #writeOrLocateProcessedDataFile(ExpressionExperiment, boolean, boolean)
     */
    Optional<Path> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException;

    /**
     * @see #writeOrLocateRawExpressionDataFile(ExpressionExperiment, QuantitationType, boolean)
     */
    Path writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException;

    void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis );

    /**
     * Writes to the configured gemma.appdata.home
     * The file created is a zip archive containing at least two files. The first is the summary model fit statistics,
     * ANOVA-style. The others are the contrast details for each factor.
     * They should be R-friendly (e.g., readable with
     * <code>read.delim("analysis.results.txt", header=T, comment.char="#", row.names=1)</code>
     *
     * @param ee       the experiment
     * @param analysis analysis
     * @param config   config
     * @throws IOException when there was a problem during write
     */
    void writeDiffExArchiveFile( BioAssaySet ee, DifferentialExpressionAnalysis analysis,
            @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException;
}