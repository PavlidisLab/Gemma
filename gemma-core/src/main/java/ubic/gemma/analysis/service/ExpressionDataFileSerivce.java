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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionDataFileSerivce {

    public static final String DATA_FILE_SUFFIX = ".data.txt.gz";
    public static final String JSON_FILE_SUFFIX = ".data.json.gz";
    public static final String DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;
    public static final String DISCLAIMER = "# If you use this file for your research, please cite the Gemma web site\n";

    /**
     * @param ee
     * @param filtered if the data matrix is filtered
     * @return
     */
    public abstract File getOutputFile( ExpressionExperiment ee, boolean filtered );

    /**
     * @param type
     * @return
     */
    public abstract File getOutputFile( QuantitationType type );

    /**
     * @param filename
     * @return
     */
    public abstract File getOutputFile( String filename );

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public abstract File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     * 
     * @param type
     * @param forceWrite To not return the existing file, but create it anew.
     * @return location of the resulting file.
     */
    public abstract File writeOrLocateDataFile( QuantitationType type, boolean forceWrite );

    /**
     * Locate or create an experimental design file for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public abstract File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @param ee
     * @param forceWrite
     * @param filtered if the data should be filtered.
     * @see ExpressionDataMatrixServiceImpl.getFilteredMatrix
     * @return
     */
    public abstract File writeOrLocateJSONDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered );

    /**
     * @param type
     * @param forceWrite
     */
    public abstract File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite );

    /**
     * Locate or create the differential expression data file(s) for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return collection of files, one per analysis.
     */
    public abstract Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @param analysis
     * @param forceRewrite
     * @return
     */
    public abstract File writeOrLocateDiffExpressionDataFile( DifferentialExpressionAnalysis analysis,
            boolean forceRewrite );

    /**
     * Delete the differential expression file for the given experiment
     * 
     * @param ee
     */
    public abstract void deleteDiffExFile( ExpressionExperiment ee );

    /**
     * Write or located the coexpression data file for a given experiment
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public abstract File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite );

    /**
     * @param results
     * @param geneAnnotations
     * @param buf
     */
    public abstract void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf );

    /**
     * @param ears
     * @param geneAnnotations
     * @param buf
     * @param probe2String
     * @param sortedFirstColumnOfResults
     * @return
     */
    public abstract List<DifferentialExpressionAnalysisResult> analysisResultSetToString(
            ExpressionAnalysisResultSet ears, Map<Long, String[]> geneAnnotations, StringBuilder buf,
            Map<Long, StringBuilder> probe2String, List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults );

}