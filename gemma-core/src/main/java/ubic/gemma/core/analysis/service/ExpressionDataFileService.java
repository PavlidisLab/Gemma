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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author paul
 */
public interface ExpressionDataFileService {

    String DATA_ARCHIVE_FILE_SUFFIX = ".zip";
    String DATA_DIR =
            Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "dataFiles" + File.separatorChar;

    String DATA_FILE_SUFFIX = ".data.txt";

    String DATA_FILE_SUFFIX_COMPRESSED = ".data.txt.gz";
    String DISCLAIMER = "# If you use this file for your research, please cite: \n"
            + "# Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. "
            + "Bioinformatics, 2012. \n";
    String JSON_FILE_SUFFIX = ".data.json.gz";
    String TMP_DATA_DIR =
            Settings.getString( "gemma.tmpdata.home" ) + File.separatorChar + "dataFiles" + File.separatorChar;

    void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf );

    List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults );

    /**
     * Delete any existing coexpression, data, or differential expression data files.
     */
    void deleteAllFiles( ExpressionExperiment ee ) throws IOException;

    /**
     * Locate or create the differential expression data file(s) for a given experiment. We generate an archive that
     * contains following files: - differential expression analysis file (q-values per factor) - file for each result
     * set with contrasts info (such as fold change for each factor value)
     */
    File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate );

    /**
     * @param filtered if the data matrix is filtered
     */
    File getOutputFile( ExpressionExperiment ee, boolean filtered );

    /**
     * @param filtered   if the data matrix is filtered
     * @param compressed if the filename should have a .gz extension
     * @param temporary  if you want the file to be saved in the configuration file temporary location
     */
    File getOutputFile( ExpressionExperiment ee, boolean filtered, boolean compressed, boolean temporary );

    /**
     * @param filename without the path - that is, just the name of the file
     * @return File, with location in the appropriate target directory.
     */
    File getOutputFile( String filename );

    /**
     * @param filename   without the path - that is, just the name of the file
     * @param temporary, if this is true then the file gets saved to the temporary location from the
     *                   configuration file
     * @return File, with location in the appropriate target directory.
     */
    File getOutputFile( String filename, boolean temporary );

    /**
     * Create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     */
    File writeDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException;

    /**
     * Write or located the coexpression data file for a given experiment
     */
    File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only). It will be gzip-compressed.
     */
    File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     *
     * @param forceWrite To not return the existing file, but create it anew.
     * @return location of the resulting file.
     */
    File writeOrLocateDataFile( QuantitationType type, boolean forceWrite );

    /**
     * Locate or create an experimental design file for a given experiment.
     */
    File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     *
     * @return collection of files, one per analysis.
     */
    Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @param filtered if the data should be filtered.
     */
    File writeOrLocateJSONDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite );

    /**
     * create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     */
    File writeTemporaryDataFile( ExpressionExperiment ee, boolean filtered );

    File writeTemporaryDesignFile( ExpressionExperiment ee );

    void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) throws IOException;

    /**
     * Writes to the configured gemma.appdata.home
     * The file created is a zip archive containing at least two files. The first is the summary model fit statistics,
     * ANOVA-style. The others are the contrast details for each factor.
     * They should be R-friendly (e.g., readable with
     * <code>read.delim("analysis.results.txt", header=T, comment.char="#", row.names=1)</code>
     */
    void writeDiffExArchiveFile( BioAssaySet ee, DifferentialExpressionAnalysis analysis,
            DifferentialExpressionAnalysisConfig config ) throws IOException;

}