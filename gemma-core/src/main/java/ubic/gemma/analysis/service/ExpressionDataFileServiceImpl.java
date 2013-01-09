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
package ubic.gemma.analysis.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.ProbeLink;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.DifferentialExpressionAnalysisResultComparator;
import ubic.gemma.util.EntityUtils;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 * <p>
 * FIXME there is a possibility of having stale data, if the data have been updated since the last file was generated.
 * This can be tested for. Also the gene annotations are (generally) read in from a file, which can also be stale.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class ExpressionDataFileServiceImpl implements ExpressionDataFileService {

    private static final String DECIMAL_FORMAT = "%.4g";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );

    /**
     * FIXME this is a common chunk of code... should refactor.
     * 
     * @param bas
     * @return
     */
    private static ExpressionExperiment experimentForBioAssaySet( BioAssaySet bas ) {
        ExpressionExperiment ee = null;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }
        return ee;
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#analysisResultSetsToString(java.util.Collection,
     * java.util.Map, java.lang.StringBuilder)
     */
    @Override
    public void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf ) {
        Map<Long, StringBuilder> probe2String = new HashMap<Long, StringBuilder>();

        List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults = null;

        /*
         * See bug 2085
         */

        for ( ExpressionAnalysisResultSet ears : results ) {
            ears = differentialExpressionResultService.thaw( ears );
            sortedFirstColumnOfResults = analysisResultSetToString( ears, geneAnnotations, buf, probe2String,
                    sortedFirstColumnOfResults );

        } // ears loop

        buf.append( "\n" );

        if ( sortedFirstColumnOfResults == null ) {
            throw new IllegalStateException( "No results for " );
        }

        // Dump the probe data in the sorted order of the 1st column that we orginally sorted
        for ( DifferentialExpressionAnalysisResult sortedResult : sortedFirstColumnOfResults ) {

            CompositeSequence cs = sortedResult.getProbe();
            StringBuilder sb = probe2String.get( cs.getId() );
            if ( sb == null ) {
                log.warn( "Unable to find probe " + cs.getId() + " in map" );
                break;
            }
            buf.append( sb );
            buf.append( "\n" );

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#analysisResultSetToString(ubic.gemma.model.analysis.expression
     * .diff.ExpressionAnalysisResultSet, java.util.Map, java.lang.StringBuilder, java.util.Map, java.util.List)
     */
    @Override
    public List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults ) {

        if ( sortedFirstColumnOfResults == null ) { // Sort P values in ears (because 1st column)
            sortedFirstColumnOfResults = new ArrayList<DifferentialExpressionAnalysisResult>( ears.getResults() );
            Collections.sort( sortedFirstColumnOfResults,
                    DifferentialExpressionAnalysisResultComparator.Factory.newInstance() );
        }

        // Generate a description of the factors involved "(factor1:factor2: .... :factorN)"
        String factorColumnName = "(";
        for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
            factorColumnName += ef.getName() + ":";
        }
        factorColumnName = StringUtils.chomp( factorColumnName, ":" ) + ")";

        // Generate headers
        buf.append( "\tQValue" + factorColumnName );
        buf.append( "\tPValue" + factorColumnName );

        // Generate probe details
        for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {
            StringBuilder probeBuffer = new StringBuilder();

            CompositeSequence cs = dear.getProbe();

            // Make a hashmap so we can organize the data by probe with factors as colums
            // Need to cache the information untill we have it organized in the correct format to write
            Long csid = cs.getId();
            if ( probe2String.containsKey( csid ) ) {
                probeBuffer = probe2String.get( csid );
            } else {// no entry for probe yet
                probeBuffer.append( cs.getName() );
                if ( geneAnnotations.containsKey( csid ) ) {
                    String[] annotationStrings = geneAnnotations.get( csid );
                    probeBuffer.append( "\t" + annotationStrings[1] + "\t" + annotationStrings[2] );

                    // leaving out Gemma ID, which is annotationStrings[3]
                    if ( annotationStrings.length > 4 ) {
                        // ncbi id.
                        probeBuffer.append( "\t" + annotationStrings[4] );
                    }
                }

                probe2String.put( csid, probeBuffer );
            }

            Double correctedPvalue = dear.getCorrectedPvalue();
            Double pvalue = dear.getPvalue();

            String formattedCP = correctedPvalue == null ? "" : String.format( DECIMAL_FORMAT, correctedPvalue );
            String formattedP = pvalue == null ? "" : String.format( DECIMAL_FORMAT, pvalue );
            probeBuffer.append( "\t" + formattedCP + "\t" + formattedP );

        } // ears.getResults loop
        return sortedFirstColumnOfResults;
    }

    /**
     * @param resultSet
     * @param geneAnnotations
     * @return
     */
    public String analysisResultSetWithContrastsToString( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations ) {
        Map<Long, StringBuilder> probe2String = new HashMap<Long, StringBuilder>();
        StringBuilder buf = new StringBuilder();

        ExperimentalFactor ef = resultSet.getExperimentalFactors().iterator().next();

        Long baselineId = resultSet.getBaselineGroup().getId();
        List<Long> factorValueIdOrder = new ArrayList<Long>();
        for ( FactorValue factorValue : ef.getFactorValues() ) {
            if ( factorValue.getId() == baselineId ) continue;
            factorValueIdOrder.add( factorValue.getId() );

            // Generate column headers.
            buf.append( "\tFoldChage (" + getFactorValueString( factorValue ) + ")" );
            buf.append( "\tPValue (" + getFactorValueString( factorValue ) + ")" );
        }

        buf.append( '\n' );

        differentialExpressionResultService.thaw( resultSet.getResults() );

        // Generate probe details
        for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
            StringBuilder rowBuffer = new StringBuilder();

            CompositeSequence cs = dear.getProbe();

            // Make a hashmap so we can organize the data by probe with factors as columns
            // Need to cache the information until we have it organized in the correct format to write
            Long csid = cs.getId();
            if ( probe2String.containsKey( csid ) ) {
                rowBuffer = probe2String.get( csid );
            } else {// no entry for probe yet
                rowBuffer.append( cs.getName() );
                if ( geneAnnotations.containsKey( csid ) ) {
                    String[] annotationStrings = geneAnnotations.get( csid );
                    rowBuffer.append( "\t" + annotationStrings[1] + "\t" + annotationStrings[2] );

                    // leaving out Gemma ID, which is annotationStrings[3]
                    if ( annotationStrings.length > 4 ) {
                        // ncbi id.
                        rowBuffer.append( "\t" + annotationStrings[4] );
                    }
                }

                probe2String.put( csid, rowBuffer );
            }

            Map<Long, String> factorValueIdToData = new HashMap<Long, String>();
            // I don't think we can expect them in the same order.
            for ( ContrastResult contrast : dear.getContrasts() ) {
                Double foldChange = contrast.getLogFoldChange();
                Double pValue = contrast.getPvalue();
                String formattedPvalue = pValue == null ? "" : String.format( DECIMAL_FORMAT, pValue );
                String formattedFoldChange = foldChange == null ? "" : String.format( DECIMAL_FORMAT, foldChange );
                String contrastData = "\t" + formattedFoldChange + "\t" + formattedPvalue;
                factorValueIdToData.put( contrast.getFactorValue().getId(), contrastData );
            }

            // Get them in the right order.
            for ( Long factorValueId : factorValueIdOrder ) {
                String s = factorValueIdToData.get( factorValueId );
                if ( s == null ) s = "";
                rowBuffer.append( s );
            }

            buf.append( rowBuffer.toString() + '\n' );

        } // ears.getResults loop
        return buf.toString();
    }

    @Override
    public void deleteAllFiles( ExpressionExperiment ee ) throws IOException {
        ee = this.expressionExperimentService.thawLite( ee );

        // data files.
        deleteAndLog( getOutputFile( ee, true ) );
        deleteAndLog( getOutputFile( ee, false ) );

        // diff ex files
        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            deleteDiffExArchiveFile( analysis );
        }

        // coexpression file
        deleteAndLog( getOutputFile( getCoexpressionDataFilename( ee ) ) );

        // design file
        deleteAndLog( getOutputFile( getDesignFileName( ee ) ) );
    }

    /**
     * @param analysis
     */
    @Override
    public void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) throws IOException {
        String filename = getDiffExArchiveFileName( analysis );
        deleteAndLog( getOutputFile( filename ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateDiffExpressionDataFile(ubic.gemma.model.analysis
     * .expression.diff.DifferentialExpressionAnalysis, boolean)
     */
    @Override
    public File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) {
        DifferentialExpressionAnalysis analysis = this.differentialExpressionAnalysisService.load( analysisId );
        this.differentialExpressionAnalysisService.thaw( analysis );

        Collection<ArrayDesign> arrayDesigns = this.expressionExperimentService.getArrayDesignsUsed( analysis
                .getExperimentAnalyzed() );
        Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );

        String filename = getDiffExArchiveFileName( analysis );
        File f = getOutputFile( filename );

        if ( !forceCreate && f.canRead() ) {
            log.info( f + " exists, not regenerating" );
            return f;
        }

        try {
            // We create .zip file with analysis results.
            log.info( "Creating new Differential Expression data archive file: " + f.getName() );
            ZipOutputStream zipOut = new ZipOutputStream( new FileOutputStream( f ) );

            // Add gene x factor analysis results file.
            zipOut.putNextEntry( new ZipEntry( "analysis.data.txt" ) );
            String analysisData = convertDiffExpressionAnalysisData( analysis, geneAnnotations );
            zipOut.write( analysisData.getBytes() );
            zipOut.closeEntry();

            // Add a file for each result set with contrasts information.
            for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                if ( resultSet.getExperimentalFactors().size() > 1 ) {
                    continue; // Skip interactions.
                }
                String resultSetData = convertDiffExpressionResultSetData( resultSet, geneAnnotations );
                zipOut.putNextEntry( new ZipEntry( "resultset_" + resultSet.getId() + ".data.txt" ) );
                zipOut.write( resultSetData.getBytes() );
                zipOut.closeEntry();
            }

            zipOut.close();
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return f;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#getOutputFile(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    @Override
    public File getOutputFile( ExpressionExperiment ee, boolean filtered ) {
        String filteredAdd = "";
        if ( !filtered ) {
            filteredAdd = ".unfilt";
        }
        String filename = getDataFileName( ee, filteredAdd );
        return getOutputFile( filename );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#getOutputFile(java.lang.String)
     */
    @Override
    public File getOutputFile( String filename ) {
        String fullFilePath = DATA_DIR + filename;
        File f = new File( fullFilePath );

        if ( f.exists() ) {
            return f;
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        return f;
    }

    @Override
    public File writeDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException {
        File f = new File( fileName );
        return writeDataFile( ee, filtered, f, compress );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateCoexpressionDataFile(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, boolean)
     */
    @Override
    public File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        String getCoexpressionDataFilename = getCoexpressionDataFilename( ee );
        String filename = getCoexpressionDataFilename;
        try {
            File f = getOutputFile( filename );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new Co-Expression data file: " + f.getName() );
            writeCoexpressionData( f, ee );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateDataFile(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, boolean, boolean)
     */
    @Override
    public File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) {

        try {
            File f = getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }
            return writeDataFile( ee, filtered, f, true );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateDataFile(ubic.gemma.model.common.quantitationtype
     * .QuantitationType, boolean)
     */
    @Override
    public File writeOrLocateDataFile( QuantitationType type, boolean forceWrite ) {

        try {
            File f = getOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new quantitation type expression data file: " + f.getName() );

            Collection<? extends DesignElementDataVector> vectors = designElementDataVectorService.find( type );
            Collection<ArrayDesign> arrayDesigns = getArrayDesigns( vectors );
            Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );

            if ( vectors.size() == 0 ) {
                log.warn( "No vectors for " + type );
                return null;
            }

            writeVectors( f, type.getRepresentation(), vectors, geneAnnotations );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateDesignFile(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, boolean)
     */
    @Override
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        String filename = getDesignFileName( ee );
        try {
            File f = getOutputFile( filename );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new experimental design file: " + f.getName() );
            writeDesignMatrix( f, ee, true );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateDiffExpressionDataFiles(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, boolean)
     */
    @Override
    public Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) {

        ee = this.expressionExperimentService.thawLite( ee );

        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );

        Collection<File> result = new HashSet<File>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            result.add( this.getDiffExpressionAnalysisArchiveFile( analysis.getId(), forceWrite ) );
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateJSONDataFile(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, boolean, boolean)
     */
    @Override
    public File writeOrLocateJSONDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) {

        try {
            File f = getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new JSON expression data file: " + f.getName() );
            ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

            Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
            Map<Long, Collection<Gene>> geneAnnotations = this.getGeneAnnotations( arrayDesigns );

            writeJson( f, geneAnnotations, matrix );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ExpressionDataFileSerivce#writeOrLocateJSONDataFile(ubic.gemma.model.common.
     * quantitationtype.QuantitationType, boolean)
     */
    @Override
    public File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite ) {

        try {
            File f = getJSONOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new quantitation type  JSON data file: " + f.getName() );

            Collection<? extends DesignElementDataVector> vectors = designElementDataVectorService.find( type );

            if ( vectors.size() == 0 ) {
                log.warn( "No vectors for " + type );
                return null;
            }

            writeJson( f, type.getRepresentation(), vectors );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Given diff exp analysis and gene annotation generate header and tab delimited data. The output is qValue....
     * 
     * @param analysis
     * @param geneAnnotations
     * @return
     */
    private String convertDiffExpressionAnalysisData( DifferentialExpressionAnalysis analysis,
            Map<Long, String[]> geneAnnotations ) {
        Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();
        if ( results == null || results.isEmpty() ) {
            log.warn( "No differential expression results found for " + analysis );
            return "";
        }

        StringBuilder buf = new StringBuilder();

        buf.append( makeDiffExpressionFileHeader( analysis, geneAnnotations ) );
        analysisResultSetsToString( results, geneAnnotations, buf );

        return buf.toString();
    }

    /**
     * Given result set and gene annotation generate header and tab delimited data. The output is foldChange and pValue
     * associated with each contrast.
     * 
     * @param resultSet
     * @param geneAnnotations
     * @return
     */
    private String convertDiffExpressionResultSetData( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();
        // Write header.
        buf.append( makeDiffExpressionResultSetFileHeader( resultSet, geneAnnotations ) );
        // Write contrasts data.
        buf.append( analysisResultSetWithContrastsToString( resultSet, geneAnnotations ) );

        return buf.toString();
    }

    /**
     * @param f1
     */
    private void deleteAndLog( File f1 ) {
        if ( f1.canWrite() && f1.delete() ) {
            log.info( "Deleted: " + f1 );
        }
    }

    /**
     * @param vectors
     * @return
     */
    private Collection<ArrayDesign> getArrayDesigns( Collection<? extends DesignElementDataVector> vectors ) {
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( DesignElementDataVector v : vectors ) {
            ads.add( v.getDesignElement().getArrayDesign() );
        }
        return ads;
    }

    /**
     * @param ee
     * @return
     */
    private String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp" + DATA_FILE_SUFFIX;
    }

    /**
     * @param ee
     * @param filteredAdd
     * @return Name, without full path.
     */
    private String getDataFileName( ExpressionExperiment ee, String filteredAdd ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + filteredAdd
                + DATA_FILE_SUFFIX;
    }

    /**
     * @param ee
     * @param filtered
     * @param f
     * @return
     */
    private ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered, File f ) {

        FilterConfig filterConfig = new FilterConfig();
        filterConfig.setIgnoreMinimumSampleThreshold( true );
        filterConfig.setIgnoreMinimumRowsThreshold( true );
        ee = expressionExperimentService.thawLite( ee );
        ExpressionDataDoubleMatrix matrix;
        if ( filtered ) {
            matrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig );
        } else {
            matrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        }
        return matrix;
    }

    /**
     * @param ee
     * @return
     */
    private String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign" + DATA_FILE_SUFFIX;
    }

    /**
     * @param diff
     * @return
     */
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

        return experimentAnalyzed.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_diffExpAnalysis_"
                + diff.getId() + DATA_ARCHIVE_FILE_SUFFIX;
    }

    /**
     * @param fv
     * @return
     */
    private String getFactorValueString( FactorValue fv ) {
        if ( fv == null ) return "null";

        if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
            String fvString = "";
            for ( Characteristic c : fv.getCharacteristics() ) {
                fvString += c.getValue() + " ";
            }
            return fvString;
        } else if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
            return fv.getValue();
        } else
            return "absent ";
    }

    /**
     * @param ads
     * @return
     */
    private Map<Long, Collection<Gene>> getGeneAnnotations( Collection<ArrayDesign> ads ) {
        Map<Long, Collection<Gene>> annots = new HashMap<Long, Collection<Gene>>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesign = arrayDesignService.thaw( arrayDesign );
            annots.putAll( ArrayDesignAnnotationServiceImpl.readAnnotationFile( arrayDesign ) );
        }
        return annots;
    }

    /**
     * @param ads
     * @return Map of composite sequence ids to an array of strings: [probe name, genes symbol(s), gene Name(s), gemma
     *         id(s), ncbi id(s)].
     */
    private Map<Long, String[]> getGeneAnnotationsAsStrings( Collection<ArrayDesign> ads ) {
        Map<Long, String[]> annots = new HashMap<Long, String[]>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesign = arrayDesignService.thaw( arrayDesign );
            annots.putAll( ArrayDesignAnnotationServiceImpl.readAnnotationFileAsString( arrayDesign ) );
        }
        return annots;
    }

    /**
     * @param ads
     * @return
     */
    private Map<CompositeSequence, String[]> getGeneAnnotationsAsStringsByProbe( Collection<ArrayDesign> ads ) {
        Map<CompositeSequence, String[]> annots = new HashMap<CompositeSequence, String[]>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesign = arrayDesignService.thaw( arrayDesign );

            Map<Long, CompositeSequence> csidmap = EntityUtils.getIdMap( arrayDesign.getCompositeSequences() );

            Map<Long, String[]> geneAnnots = ArrayDesignAnnotationServiceImpl.readAnnotationFileAsString( arrayDesign );

            for ( Entry<Long, String[]> e : geneAnnots.entrySet() ) {

                if ( !csidmap.containsKey( e.getKey() ) ) {
                    continue;
                }

                annots.put( csidmap.get( e.getKey() ), e.getValue() );

            }

        }
        return annots;
    }

    /**
     * @param type
     * @return
     * @throws IOException
     */
    private File getJSONOutputFile( QuantitationType type ) throws IOException {
        String filename = getJSONOutputFilename( type );
        String fullFilePath = DATA_DIR + filename;

        File f = new File( fullFilePath );

        if ( f.exists() ) {
            log.warn( "Will overwrite existing file " + f );
            f.delete();
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        f.createNewFile();
        return f;
    }

    /**
     * @param type
     * @return Name, without full path.
     */
    private String getJSONOutputFilename( QuantitationType type ) {
        return FileTools.cleanForFileName( type.getName() ) + JSON_FILE_SUFFIX;
    }

    private File getOutputFile( QuantitationType type ) {
        String filename = getOutputFilename( type );
        return getOutputFile( filename );
    }

    /**
     * @param type
     * @return Name, without full path.
     */
    private String getOutputFilename( QuantitationType type ) {
        return type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + DATA_FILE_SUFFIX;
    }

    /**
     * @param analysis
     * @param geneAnnotations
     * @param buf
     * @return header string
     */
    private String makeDiffExpressionFileHeader( DifferentialExpressionAnalysis analysis,
            Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();

        BioAssaySet bas = analysis.getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        Date timestamp = new Date( System.currentTimeMillis() );
        buf.append( "# Differential Expression Data for:  " + ee.getShortName() + " : " + ee.getName() + " (ID="
                + ee.getId() + ")\n" );
        buf.append( "# Analysis ID = " + analysis.getId() + "\n" );
        if ( analysis.getSubsetFactorValue() != null ) {
            buf.append( "# This analysis is for subset ID=" + bas.getId() + "\n" );
            buf.append( "# The subsetting factor was " + analysis.getSubsetFactorValue().getExperimentalFactor() + "\n" );
            buf.append( "# This subset is of samples with " + analysis.getSubsetFactorValue() + "\n" );
        }

        buf.append( "# The following factors were used\n" );
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            String f = StringUtils.join( rs.getExperimentalFactors(), ":" );
            buf.append( "# " + f + "\n" );
        }

        buf.append( "# Generated by Gemma " + timestamp + " \n" );

        buf.append( DISCLAIMER );

        // Different Headers if Gene Annotations missing.
        if ( geneAnnotations.isEmpty() ) {
            log.info( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append( "# The annotation file is missing for this Experiment, gene annotation information is omitted\n" );
            buf.append( "Probe_Name" );
        } else {
            buf.append( "Probe_Name\tGene_Symbol\tGene_Name" );// column information

            if ( geneAnnotations.values().iterator().next().length > 4 ) {
                buf.append( "\tNCBI_ID" ); // leaving out the Gemma ID.
            }
        }

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.S

        return buf.toString();
    }

    private String makeDiffExpressionResultSetFileHeader( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();

        BioAssaySet bas = resultSet.getAnalysis().getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        Date timestamp = new Date( System.currentTimeMillis() );
        buf.append( "# Differential Expression Data for:  " + ee.getShortName() + " : " + ee.getName() + " (ID="
                + ee.getId() + ")\n" );
        buf.append( "# Analysis ID = " + resultSet.getAnalysis().getId() + "\n" );
        if ( resultSet.getAnalysis().getSubsetFactorValue() != null ) {
            buf.append( "# This analysis is for subset ID=" + bas.getId() + "\n" );
            buf.append( "# The subsetting factor was "
                    + resultSet.getAnalysis().getSubsetFactorValue().getExperimentalFactor() + "\n" );
            buf.append( "# This subset is of samples with " + resultSet.getAnalysis().getSubsetFactorValue() + "\n" );
        }
        buf.append( "# ResultSet ID = " + resultSet.getId() + "\n" );

        buf.append( "# File contains contrasts for the following factor \n" );
        String f = StringUtils.join( resultSet.getExperimentalFactors(), " x " );
        buf.append( "# " + f + "\n" );

        buf.append( "# Generated by Gemma " + timestamp + " \n" );

        buf.append( DISCLAIMER );

        // Different Headers if Gene Annotations missing.
        if ( geneAnnotations.isEmpty() ) {
            log.info( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append( "# The annotation file is missing for this Experiment, gene annotation information is omitted\n" );
            buf.append( "Probe_Name" );
        } else {
            buf.append( "Probe_Name\tGene_Symbol\tGene_Name" );// column information

            if ( geneAnnotations.values().iterator().next().length > 4 ) {
                buf.append( "\tNCBI_ID" ); // leaving out the Gemma ID.
            }
        }

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.S
        return buf.toString();
    }

    /**
     * Loads the probe to probe coexpression link information for a given expression experiment and writes it to disk.
     * 
     * @param file
     * @param ee
     * @throws IOException
     */
    private void writeCoexpressionData( File file, ExpressionExperiment ee ) throws IOException {

        Taxon tax = expressionExperimentService.getTaxon( ee );
        assert tax != null;
        Collection<ProbeLink> probeLinks = probe2ProbeCoexpressionService
                .getProbeCoExpression( ee, tax.getCommonName() );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );

        Date timestamp = new Date( System.currentTimeMillis() );
        StringBuffer buf = new StringBuffer();

        // Write header information
        buf.append( "# Coexpression Data for:  " + ee.getShortName() + " : " + ee.getName() + " \n" );
        buf.append( "# Generated On: " + timestamp + " \n" );
        buf.append( DISCLAIMER );
        if ( geneAnnotations.isEmpty() ) {
            log.info( "Platform anotation File Missing for Experiment, unable to include annotation information" );
            buf.append( "# The platform annotation file is missing for this Experiment, unable to include gene annotation information \n" );
            buf.append( "probeId_1 \t probeId_2 \t score \n" );
        } else
            buf.append( "probe_1 \t gene_symbol_1 \t gene_name_1 \t probe_2 \t gene_symbol_2 \t gene_name_2 \t score \n" );

        // Data
        for ( ProbeLink link : probeLinks ) {

            if ( geneAnnotations.isEmpty() ) {
                buf.append( link.getFirstDesignElementId() + "\t" + link.getSecondDesignElementId() + "\t" );
            } else {
                String[] firstAnnotation = geneAnnotations.get( link.getFirstDesignElementId() );
                String[] secondAnnotation = geneAnnotations.get( link.getSecondDesignElementId() );

                buf.append( firstAnnotation[0] + "\t" + firstAnnotation[1] + "\t" + firstAnnotation[2] + "\t" );
                buf.append( secondAnnotation[0] + "\t" + secondAnnotation[1] + "\t" + secondAnnotation[2] + "\t" );
            }

            buf.append( StringUtils.substring( link.getScore().toString(), 0, 5 ) + "\n" );
        }

        // Write coexpression data to file (zipped of course)
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        writer.write( buf.toString() );
        writer.flush();
        writer.close();

    }

    /**
     * @param ee
     * @param filtered
     * @param f
     * @param compress if true, file will be output in GZIP format.
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private File writeDataFile( ExpressionExperiment ee, boolean filtered, File f, boolean compress )
            throws IOException, FileNotFoundException {
        log.info( "Creating new expression data file: " + f.getName() );
        ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );
        writeMatrix( f, geneAnnotations, matrix );
        return f;
    }

    /**
     * Writes out the experimental design for the given experiment. The bioassays (col 0) matches match the header row
     * of the data matrix printed out by the {@link MatrixWriter}.
     * 
     * @param file
     * @param expressionExperiment
     * @param orderByesign
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void writeDesignMatrix( File file, ExpressionExperiment expressionExperiment, boolean orderByDesign )
            throws IOException, FileNotFoundException {
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
        edWriter.write( writer, expressionExperiment, true, orderByDesign );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param geneAnnotations
     * @param expressionDataMatrix
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void writeJson( File file, Map<Long, Collection<Gene>> geneAnnotations,
            ExpressionDataMatrix<?> expressionDataMatrix ) throws IOException, FileNotFoundException {
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeJSON( writer, expressionDataMatrix, true );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param representation
     * @param vectors
     * @throws IOException
     */
    private void writeJson( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors ) throws IOException {
        this.designElementDataVectorService.thaw( vectors );
        ExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeJSON( writer, expressionDataMatrix, true );
    }

    /**
     * @param file
     * @param geneAnnotations
     * @param expressionDataMatrix
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void writeMatrix( File file, Map<CompositeSequence, String[]> geneAnnotations,
            ExpressionDataMatrix<?> expressionDataMatrix ) throws IOException, FileNotFoundException {

        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations, true );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param representation
     * @param vectors
     * @param geneAnnotations
     * @throws IOException
     */
    private void writeVectors( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors, Map<CompositeSequence, String[]> geneAnnotations )
            throws IOException {
        this.designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );

        writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

}
