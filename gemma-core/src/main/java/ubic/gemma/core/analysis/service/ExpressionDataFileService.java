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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
@ParametersAreNonnullByDefault
public interface ExpressionDataFileService extends TsvFileService<ExpressionExperiment> {

    String DISCLAIMER = "# If you use this file for your research, please cite: \n"
            + "# Lim et al. (2021) Curation of over 10 000 transcriptomic studies to enable data reuse. \n"
            + "# Database, baab006 (doi:10.1093/database/baab006). \n";

    void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf );

    List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            @Nullable List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults );

    /**
     * Delete any existing coexpression, data, or differential expression data files.
     *
     * @param ee the experiment
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
    File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate );

    /**
     * Locate a metadata file.
     */
    File getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type );

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
    Optional<File> writeProcessedExpressionDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException, FilteringException;

    /**
     * Write raw expression data to a given writer for a given quantitation type.
     * <p>
     * Note: For compression, wrap a {@link java.util.zip.GZIPOutputStream} with a {@link java.io.OutputStreamWriter}.
     * To write to a string, consider using {@link java.io.StringWriter}.
     *
     * @param ee the expression experiment
     * @param qt a quantitation type to use
     * @param writer the destination for the raw expression data
     * @throws IOException if operations with the writer fails
     */
    void writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException;

    /**
     * Write processed expression data to a given writer for a given quantitation type.
     * <p>
     * Note: For compression, wrap a {@link java.util.zip.GZIPOutputStream} with a {@link java.io.OutputStreamWriter}.
     * To write to a string, consider using {@link java.io.StringWriter}.
     *
     * @param ee the expression experiment
     * @param writer the destination for the raw expression data
     * @throws IOException if operations with the writer fails
     */
    void writeProcessedExpressionData( ExpressionExperiment ee, Writer writer ) throws IOException;

    /**
     * Write or located the coexpression data file for a given experiment
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return file
     */
    Optional<File> writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite );

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
    Optional<File> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException;

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     *
     * @param forceWrite To not return the existing file, but create it anew.
     * @param type       the quantitation type
     * @return file
     */
    File writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite );

    /**
     * Locate or create an experimental design file for a given experiment.
     * The file will be regenerated even if one already exists if the forceWrite parameter is true, or if there was
     * a recent change (more recent than the last modified date of the existing file) to any of the experiments platforms.
     *
     * @param ee         the experiment
     * @param forceWrite force re-write even if file already exists and is up to date
     * @return file
     */
    File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     *
     * @param ee         the experiment
     * @param forceWrite whether to force write
     * @return collection of files, one per analysis.
     */
    Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @see #writeOrLocateProcessedDataFile(ExpressionExperiment, boolean, boolean)
     */
    Optional<File> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException;

    /**
     * @see #writeOrLocateRawExpressionDataFile(ExpressionExperiment, QuantitationType, boolean)
     */
    File writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite );

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