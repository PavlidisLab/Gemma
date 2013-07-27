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
package ubic.gemma.analysis.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.Settings;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionDataFileService {

    public static final String DATA_ARCHIVE_FILE_SUFFIX = ".archive.zip";
    public static final String DATA_DIR = Settings.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    public static final String TMP_DATA_DIR = Settings.getString( "gemma.tmpdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    public static final String DATA_FILE_SUFFIX_COMPRESSED = ".data.txt.gz";
    public static final String DATA_FILE_SUFFIX = ".data.txt";
    public static final String DISCLAIMER = "# If you use this file for your research, please cite: \n"
            + "# Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. "
            + "Bioinformatics, 2012. \n";
    public static final String JSON_FILE_SUFFIX = ".data.json.gz";

    /**
     * @param results
     * @param geneAnnotations
     * @param buf
     */
    public void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf );

    /**
     * @param ears
     * @param geneAnnotations
     * @param buf
     * @param probe2String
     * @param sortedFirstColumnOfResults
     * @return
     */
    public List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults );

    /**
     * Delete any existing coexpression, data, or differential expression data files.
     * 
     * @param ee
     * @throws IOException
     */
    public void deleteAllFiles( ExpressionExperiment ee ) throws IOException;

    /**
     * Locate or create the differential expression data file(s) for a given experiment. We generate an archive that
     * contains following files: - differential expression analysis file (q-values per factor) - file for each result
     * set with contrasts info (such as fold change for each factor value)
     * 
     * @param analysis
     * @param forceRewrite
     * @return
     */
    public File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate );

    /**
     * Intended for use when the analysis is not yet persisted fully, and before results below threshold are removed.
     * 
     * @param experimentAnalyzed
     * @param analysis
     * @param resultSets
     * @return
     */
    public File getDiffExpressionAnalysisArchiveFile( BioAssaySet experimentAnalyzed,
            DifferentialExpressionAnalysis analysis, Collection<ExpressionAnalysisResultSet> resultSets );

    /**
     * @param ee
     * @param filtered if the data matrix is filtered
     * @return
     */
    public File getOutputFile( ExpressionExperiment ee, boolean filtered );

    /**
     * @param ee
     * @param filtered if the data matrix is filtered
     * @param compressed if the filename should have a .gz extension
     * @param temporary if you want the file to be saved in the configuration file temporary location
     * @return
     */
    public File getOutputFile( ExpressionExperiment ee, boolean filtered, boolean compressed, boolean temporary );

    /**
     * @param filename without the path - that is, just the name of the file
     * @return File, with location in the appropriate target directory.
     */
    public File getOutputFile( String filename );

    /**
     * @param filename without the path - that is, just the name of the file
     * @param boolean temporary, if this is true then the file gets saved to the temporary location from the
     *        configuration file
     * @return File, with location in the appropriate target directory.
     */
    public File getOutputFile( String filename, boolean temporary );

    /**
     * Create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     * 
     * @param ee
     * @param filtered
     * @param fileName
     * @param compress
     * @return
     */
    public File writeDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException;

    /**
     * Write or located the coexpression data file for a given experiment
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only). It will be gzip-compressed.
     * 
     * @param ee
     * @param forceWrite
     * @param filtered
     * @return
     */
    public File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    /**
     * create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     * 
     * @param ee
     * @param filtered
     * @return
     */
    public File writeTemporaryDataFile( ExpressionExperiment ee, boolean filtered );

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     * 
     * @param type
     * @param forceWrite To not return the existing file, but create it anew.
     * @return location of the resulting file.
     */
    public File writeOrLocateDataFile( QuantitationType type, boolean forceWrite );

    /**
     * Locate or create an experimental design file for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite );

    public File writeTemporaryDesignFile( ExpressionExperiment ee );

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return collection of files, one per analysis.
     */
    public Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @param ee
     * @param forceWrite
     * @param filtered if the data should be filtered.
     * @see ExpressionDataMatrixServiceImpl.getFilteredMatrix
     * @return
     */
    public File writeOrLocateJSONDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    /**
     * @param type
     * @param forceWrite
     */
    public File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite );

    /**
     * @param analysis
     * @throws IOException
     */
    void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) throws IOException;

}