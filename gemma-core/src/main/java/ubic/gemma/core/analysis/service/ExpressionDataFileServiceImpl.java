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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.DifferentialExpressionAnalysisResultComparator;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 *
 * @author paul
 */
@Component
public class ExpressionDataFileServiceImpl implements ExpressionDataFileService {

    private static final String DECIMAL_FORMAT = "%.4g";

    private static final Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private CoexpressionService gene2geneCoexpressionService = null;
    @Autowired
    private DifferentialExpressionAnalyzerService analyzerService;

    /**
     * FIXME this is a common chunk of code... should refactor.
     */
    private static ExpressionExperiment experimentForBioAssaySet( BioAssaySet bas ) {
        ExpressionExperiment ee;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }
        return ee;
    }

    @Override
    public void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf ) {
        Map<Long, StringBuilder> probe2String = new HashMap<>();

        List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults = null;

        /*
         * See bug 2085
         */

        for ( ExpressionAnalysisResultSet ears : results ) {
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
                log.warn( "Unable to find element " + cs.getId() + " in map" );
                break;
            }
            buf.append( sb );
            buf.append( "\n" );

        }
    }

    @Override
    public List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults ) {

        if ( sortedFirstColumnOfResults == null ) { // Sort P values in ears (because 1st column)
            sortedFirstColumnOfResults = new ArrayList<>( ears.getResults() );
            Collections.sort( sortedFirstColumnOfResults,
                    DifferentialExpressionAnalysisResultComparator.Factory.newInstance() );
        }

        // Generate a description of the factors involved "factor1_factor2", trying to be R-friendly
        StringBuilder factorColumnName = new StringBuilder();
        for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
            factorColumnName.append( ef.getName().replaceAll( "\\s+", "_" ) ).append( "_" );
        }
        factorColumnName = new StringBuilder(
                StringUtil.makeValidForR( StringUtils.removeEnd( factorColumnName.toString(), "_" ) ) );

        // Generate headers
        buf.append( "\tQValue_" ).append( factorColumnName );
        buf.append( "\tPValue_" ).append( factorColumnName );

        // Generate probe details
        for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {
            StringBuilder probeBuffer = new StringBuilder();

            CompositeSequence cs = dear.getProbe();

            // Make a hashMap so we can organize the data by probe with factors as columns
            // Need to cache the information until we have it organized in the correct format to write
            Long csid = cs.getId();
            if ( probe2String.containsKey( csid ) ) {
                probeBuffer = probe2String.get( csid );
            } else {// no entry for probe yet
                probeBuffer.append( cs.getName() );
                if ( geneAnnotations.containsKey( csid ) ) {
                    String[] annotationStrings = geneAnnotations.get( csid );
                    /*
                     * Fields:
                     * 
                     * 1: gene symbols
                     * 2: gene name
                     * 4: ncbi ID
                     */
                    probeBuffer.append( "\t" ).append( annotationStrings[1] ).append( "\t" )
                            .append( annotationStrings[2] ).append( "\t" ).append( annotationStrings[4] );
                } else {
                    probeBuffer.append( "\t\t\t" );
                }

                probe2String.put( csid, probeBuffer );
            }

            Double correctedPvalue = dear.getCorrectedPvalue();
            Double pvalue = dear.getPvalue();

            String formattedCP = correctedPvalue == null ? "" : String.format( DECIMAL_FORMAT, correctedPvalue );
            String formattedP = pvalue == null ? "" : String.format( DECIMAL_FORMAT, pvalue );
            probeBuffer.append( "\t" ).append( formattedCP ).append( "\t" ).append( formattedP );

        }
        return sortedFirstColumnOfResults;

    }

    public String analysisResultSetWithContrastsToString( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();

        ExperimentalFactor ef = resultSet.getExperimentalFactors().iterator().next();

        // This is a bit risky, we're only looking at the first one. But this is how we do it for the header.
        boolean hasNCBIIDs = !geneAnnotations.isEmpty() && geneAnnotations.values().iterator().next().length > 4;

        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

            buf.append( "\tCoefficient_" ).append( StringUtil.makeValidForR( ef.getName() ) ).append( "\tPValue_" )
                    .append( StringUtil.makeValidForR( ef.getName() ) ).append( "\n" );

            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                StringBuilder rowBuffer = new StringBuilder();

                if ( geneAnnotations.isEmpty() ) {
                    rowBuffer.append( dear.getProbe().getName() );
                } else {
                    addGeneAnnotationsToLine( rowBuffer, dear, hasNCBIIDs, geneAnnotations );
                }

                assert dear.getContrasts().size() == 1;

                ContrastResult contrast = dear.getContrasts().iterator().next();

                Double coef = contrast.getCoefficient();
                Double pValue = contrast.getPvalue();
                String formattedPvalue = pValue == null ? "" : String.format( DECIMAL_FORMAT, pValue );
                String formattedcoef = coef == null ? "" : String.format( DECIMAL_FORMAT, coef );
                String contrastData = "\t" + formattedcoef + "\t" + formattedPvalue;

                rowBuffer.append( contrastData );

                buf.append( rowBuffer.toString() ).append( '\n' );
            }

        } else {

            Long baselineId = resultSet.getBaselineGroup().getId();
            List<Long> factorValueIdOrder = new ArrayList<>();
            for ( FactorValue factorValue : ef.getFactorValues() ) {
                if ( Objects.equals( factorValue.getId(), baselineId ) ) {
                    continue;
                }
                factorValueIdOrder.add( factorValue.getId() );
                // Generate column headers, try to be R-friendly
                buf.append( "\tFoldChange_" ).append( getFactorValueString( factorValue ) );
                buf.append( "\tTstat_" ).append( getFactorValueString( factorValue ) );
                buf.append( "\tPValue_" ).append( getFactorValueString( factorValue ) );
            }

            buf.append( '\n' );

            // Generate element details
            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                StringBuilder rowBuffer = new StringBuilder();

                addGeneAnnotationsToLine( rowBuffer, dear, hasNCBIIDs, geneAnnotations );

                Map<Long, String> factorValueIdToData = new HashMap<>();
                // I don't think we can expect them in the same order.
                for ( ContrastResult contrast : dear.getContrasts() ) {
                    Double foldChange = contrast.getLogFoldChange();
                    Double pValue = contrast.getPvalue();
                    Double tStat = contrast.getTstat();
                    String formattedPvalue = pValue == null ? "" : String.format( DECIMAL_FORMAT, pValue );
                    String formattedFoldChange = foldChange == null ? "" : String.format( DECIMAL_FORMAT, foldChange );
                    String formattedTState = tStat == null ? "" : String.format( DECIMAL_FORMAT, tStat );
                    String contrastData = "\t" + formattedFoldChange + "\t" + formattedTState + "\t" + formattedPvalue;
                    assert contrast.getFactorValue() != null;

                    factorValueIdToData.put( contrast.getFactorValue().getId(), contrastData );
                }

                // Get them in the right order.
                for ( Long factorValueId : factorValueIdOrder ) {
                    String s = factorValueIdToData.get( factorValueId );
                    if ( s == null )
                        s = "";
                    rowBuffer.append( s );
                }

                buf.append( rowBuffer.toString() ).append( '\n' );

            } // resultSet.getResults() loop
        }
        return buf.toString();
    }

    @Override
    public void deleteAllFiles( ExpressionExperiment ee ) throws IOException {
        this.expressionExperimentService.thawLite( ee );

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
        deleteAndLog( getOutputFile( getDesignFileName( ee, DATA_FILE_SUFFIX_COMPRESSED ) ) );
    }

    @Override
    public void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) throws IOException {
        String filename = getDiffExArchiveFileName( analysis );
        deleteAndLog( getOutputFile( filename ) );
    }

    @Override
    public File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) {
        DifferentialExpressionAnalysis analysis = this.differentialExpressionAnalysisService.load( analysisId );

        String filename = getDiffExArchiveFileName( analysis );
        File f = getOutputFile( filename );

        if ( !forceCreate && f.canRead() ) {
            log.info( f + " exists, not regenerating" );
            return f;
        }

        this.differentialExpressionAnalysisService.thawFully( analysis );
        BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();

        /*
         * Decide if we need to extend the analysis first.
         */
        boolean extend = false;
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            if ( rs.getQvalueThresholdForStorage() != null && rs.getQvalueThresholdForStorage() < 1.0 ) {
                extend = true;
                break;
            }
        }

        if ( extend ) {
            log.info( "Extending an existing analysis to incldue all results" );

            ExpressionExperiment ee = experimentForBioAssaySet( experimentAnalyzed );

            Collection<ExpressionAnalysisResultSet> updatedResultSets = analyzerService.extendAnalysis( ee, analysis );

            boolean added = analysis.getResultSets().addAll( updatedResultSets );
            assert !added : "Should have simply replaced";
        }

        try {
            this.differentialExpressionAnalysisService.thawFully( analysis );
            writeDiffExArchiveFile( experimentAnalyzed, analysis, null );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return f;
    }

    @Override
    public File getOutputFile( ExpressionExperiment ee, boolean filtered ) {
        return getOutputFile( ee, filtered, true, false );
    }

    @Override
    public File getOutputFile( ExpressionExperiment ee, boolean filtered, boolean compressed, boolean temporary ) {
        String filteredAdd = "";
        if ( !filtered ) {
            filteredAdd = ".unfilt";
        }
        String suffix;

        if ( compressed ) {
            suffix = DATA_FILE_SUFFIX_COMPRESSED;
        } else {
            suffix = DATA_FILE_SUFFIX;
        }

        String filename = getDataFileName( ee, filteredAdd, suffix );

        // randomize file name if temporary in case of access by more than one user at once
        if ( temporary ) {

            filename = RandomStringUtils.randomAlphabetic( 6 ) + filename;

        }

        return getOutputFile( filename, temporary );
    }

    @Override
    public File getOutputFile( String filename ) {
        return getOutputFile( filename, false );

    }

    @Override
    public File getOutputFile( String filename, boolean temporary ) {
        String fullFilePath;
        if ( temporary ) {
            fullFilePath = TMP_DATA_DIR + filename;
        } else {
            fullFilePath = DATA_DIR + filename;
        }
        File f = new File( fullFilePath );

        if ( f.exists() ) {
            return f;
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() )
            parentDir.mkdirs();
        return f;
    }

    @Override
    public File writeDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException {
        File f = new File( fileName );
        return writeDataFile( ee, filtered, f, compress );
    }

    @Override
    public void writeDiffExArchiveFile( BioAssaySet experimentAnalyzed, DifferentialExpressionAnalysis analysis,
            DifferentialExpressionAnalysisConfig config ) throws IOException {
        Collection<ArrayDesign> arrayDesigns = this.expressionExperimentService
                .getArrayDesignsUsed( experimentAnalyzed );
        Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );
        String filename = getDiffExArchiveFileName( analysis );
        File f = getOutputFile( filename );

        log.info( "Creating differential expression analysis archive file: " + f.getName() );
        try (ZipOutputStream zipOut = new ZipOutputStream( new FileOutputStream( f ) )) {

            // top-level analysis results - ANOVA-style
            zipOut.putNextEntry( new ZipEntry( "analysis.results.txt" ) );
            String analysisData = convertDiffExpressionAnalysisData( analysis, geneAnnotations, config );
            zipOut.write( analysisData.getBytes() );
            zipOut.closeEntry();

            // Add a file for each result set with contrasts information.
            int i = 0;
            for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                if ( resultSet.getExperimentalFactors().size() > 1 ) {
                    // Skip interactions.
                    log.info( "Result file for interaction is omitted" ); // Why?
                    continue;
                }

                assert resultSet.getQvalueThresholdForStorage() == null
                        || resultSet.getQvalueThresholdForStorage() == 1.0;

                String resultSetData = convertDiffExpressionResultSetData( resultSet, geneAnnotations, config );

                if ( resultSet.getId() == null ) { // -nodb option on analysis
                    zipOut.putNextEntry( new ZipEntry( "resultset_" + ++i + "of" + analysis.getResultSets().size()
                            + ".data.txt" ) ); // to make it clearer this is not an ID
                } else {
                    zipOut.putNextEntry( new ZipEntry( "resultset_ID" + resultSet.getId() + ".data.txt" ) );
                }

                zipOut.write( resultSetData.getBytes() );
                zipOut.closeEntry();
            }
        }
    }

    @Override
    public File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) {

        expressionExperimentService.thawLite( ee );

        try {
            File f = getOutputFile( getCoexpressionDataFilename( ee ) );
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

    @Override
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) {

        expressionExperimentService.thawLite( ee );

        String filename = getDesignFileName( ee, DATA_FILE_SUFFIX_COMPRESSED );
        try {
            File f = getOutputFile( filename );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new experimental design file: " + f.getName() );
            return writeDesignMatrix( f, ee, true );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @Override
    public Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) {

        this.expressionExperimentService.thawLite( ee );

        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );

        Collection<File> result = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            assert analysis.getId() != null;
            result.add( this.getDiffExpressionAnalysisArchiveFile( analysis.getId(), forceWrite ) );
        }

        return result;

    }

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

    @Override
    public File writeTemporaryDataFile( ExpressionExperiment ee, boolean filtered ) {

        try {

            File f = getOutputFile( ee, filtered, false, true );

            return writeDataFile( ee, filtered, f, false );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public File writeTemporaryDesignFile( ExpressionExperiment ee ) {

        expressionExperimentService.thawLite( ee );

        // not compressed
        String filename = getDesignFileName( ee, DATA_FILE_SUFFIX );

        filename = RandomStringUtils.randomAlphabetic( 6 ) + filename;
        try {
            File f = getOutputFile( filename, true );

            log.info( "Creating new experimental design file: " + f.getName() );
            return writeDesignMatrix( f, ee, true, false );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param hasNCBIIDs Whether the annotations have the NCBI gene ids. This is based on just peeking at one, so it
     *                   might be wrong! But the format will be okay.
     */
    private void addGeneAnnotationsToLine( StringBuilder rowBuffer, DifferentialExpressionAnalysisResult dear,
            boolean hasNCBIIDs, Map<Long, String[]> geneAnnotations ) {
        CompositeSequence cs = dear.getProbe();
        Long csid = cs.getId();
        rowBuffer.append( cs.getName() );
        if ( geneAnnotations.containsKey( csid ) ) {
            String[] annotationStrings = geneAnnotations.get( csid );
            rowBuffer.append( "\t" ).append( annotationStrings[1] ).append( "\t" ).append( annotationStrings[2] );

            // leaving out Gemma ID, which is annotationStrings[3]
            if ( hasNCBIIDs ) {
                // ncbi id.
                rowBuffer.append( "\t" ).append( annotationStrings[4] );
            }
        } else {
            rowBuffer.append( "\t\t" );
            if ( hasNCBIIDs ) {
                rowBuffer.append( "\t" );
            }
        }
    }

    /**
     * Given diff exp analysis and gene annotation generate header and tab delimited data. The output is qValue....
     */
    private String convertDiffExpressionAnalysisData( DifferentialExpressionAnalysis analysis,
            Map<Long, String[]> geneAnnotations, DifferentialExpressionAnalysisConfig config ) {
        differentialExpressionAnalysisService.thawFully( analysis );
        Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();
        if ( results == null || results.isEmpty() ) {
            log.warn( "No differential expression results found for " + analysis );
            return "";
        }

        StringBuilder buf = new StringBuilder();

        buf.append( makeDiffExpressionFileHeader( analysis, analysis.getResultSets(), geneAnnotations, config ) );
        analysisResultSetsToString( results, geneAnnotations, buf );

        return buf.toString();
    }

    /**
     * Given result set and gene annotation generate header and tab delimited data. The output is foldChange and pValue
     * associated with each contrast.
     * eneAnnotations
     */
    private String convertDiffExpressionResultSetData( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations, DifferentialExpressionAnalysisConfig config ) {
        // Write header.
        // Write contrasts data.
        return makeDiffExpressionResultSetFileHeader( resultSet, geneAnnotations, config )
                + analysisResultSetWithContrastsToString( resultSet, geneAnnotations );
    }

    private void deleteAndLog( File f1 ) {
        if ( f1.canWrite() && f1.delete() ) {
            log.info( "Deleted: " + f1 );
        }
    }

    private Collection<ArrayDesign> getArrayDesigns( Collection<? extends DesignElementDataVector> vectors ) {
        Collection<ArrayDesign> ads = new HashSet<>();
        for ( DesignElementDataVector v : vectors ) {
            ads.add( v.getDesignElement().getArrayDesign() );
        }
        return ads;
    }

    private String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp"
                + DATA_FILE_SUFFIX_COMPRESSED;
    }

    /**
     * @return Name, without full path.
     */
    private String getDataFileName( ExpressionExperiment ee, String filteredAdd, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + filteredAdd + suffix;
    }

    private ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered, File f ) {

        FilterConfig filterConfig = new FilterConfig();
        filterConfig.setIgnoreMinimumSampleThreshold( true );
        filterConfig.setIgnoreMinimumRowsThreshold( true );
        expressionExperimentService.thawLite( ee );
        ExpressionDataDoubleMatrix matrix;
        if ( filtered ) {
            matrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig );
        } else {
            matrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        }
        return matrix;
    }

    private String getDesignFileName( ExpressionExperiment ee, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign" + suffix;
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
                + ( diff.getId() != null ? "_" + diff.getId() : "" ) + DATA_ARCHIVE_FILE_SUFFIX;
    }

    private String getFactorValueString( FactorValue fv ) {
        String result;
        if ( fv == null )
            return "null";

        if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
            StringBuilder fvString = new StringBuilder();
            for ( Characteristic c : fv.getCharacteristics() ) {
                fvString.append( c.getValue() ).append( "_" );
            }
            result = StringUtils.removeEnd( fvString.toString(), "_" );
        } else if ( fv.getMeasurement() != null ) {
            result = fv.getMeasurement().getValue();
        } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
            result = fv.getValue();
        } else
            return "no_data";

        // R-friendly, but no need to add "X" to the beginning since this is a suffix.
        return result.replaceAll( "[\\W]+", "." );
    }

    private Map<Long, Collection<Gene>> getGeneAnnotations( Collection<ArrayDesign> ads ) {
        Map<Long, Collection<Gene>> annots = new HashMap<>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesignService.thaw( arrayDesign );
            annots.putAll( ArrayDesignAnnotationServiceImpl.readAnnotationFile( arrayDesign ) );
        }
        return annots;
    }

    /**
     * @return Map of composite sequence ids to an array of strings: [probe name, genes symbol(s), gene Name(s), gemma
     * id(s), ncbi id(s)].
     */
    private Map<Long, String[]> getGeneAnnotationsAsStrings( Collection<ArrayDesign> ads ) {
        Map<Long, String[]> annots = new HashMap<>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesignService.thaw( arrayDesign );
            annots.putAll( ArrayDesignAnnotationServiceImpl.readAnnotationFileAsString( arrayDesign ) );
        }
        return annots;
    }

    private Map<CompositeSequence, String[]> getGeneAnnotationsAsStringsByProbe( Collection<ArrayDesign> ads ) {
        Map<CompositeSequence, String[]> annots = new HashMap<>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesignService.thaw( arrayDesign );

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

    private File getJSONOutputFile( QuantitationType type ) throws IOException {
        String filename = getJSONOutputFilename( type );
        String fullFilePath = DATA_DIR + filename;

        File f = new File( fullFilePath );

        if ( f.exists() ) {
            log.warn( "Will overwrite existing file " + f );
            f.delete();
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() )
            parentDir.mkdirs();
        f.createNewFile();
        return f;
    }

    /**
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
     * @return Name, without full path.
     */
    private String getOutputFilename( QuantitationType type ) {
        return type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + DATA_FILE_SUFFIX_COMPRESSED;
    }

    private String makeDiffExpressionFileHeader( DifferentialExpressionAnalysis analysis,
            Collection<ExpressionAnalysisResultSet> resultSets, Map<Long, String[]> geneAnnotations,
            DifferentialExpressionAnalysisConfig config ) {

        if ( analysis.getId() != null ) // It might not be a persistent analysis: using -nodb
            differentialExpressionAnalysisService.thaw( analysis ); // bug 4023

        StringBuilder buf = new StringBuilder();

        BioAssaySet bas = analysis.getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        Date timestamp = new Date( System.currentTimeMillis() );
        buf.append( "# Differential expression analysis for:  " ).append( ee.getShortName() ).append( " : " )
                .append( ee.getName() ).append( " (ID=" ).append( ee.getId() ).append( ")\n" );

        buf.append(
                "# This file contains summary statistics for the factors included in the analysis (e.g. ANOVA effects); "
                        + "details of contrasts are in separate files.\n" );

        // It might not be a persistent analysis.
        if ( analysis.getId() != null ) {
            buf.append( "# Analysis ID = " ).append( analysis.getId() ).append( "\n" );
        } else {
            buf.append( "# Analysis was not persisted to the database\n" );
        }

        if ( config != null ) {
            buf.append( config.toString() );
        } else if ( analysis.getProtocol() != null && StringUtils
                .isNotBlank( analysis.getProtocol().getDescription() ) ) {
            buf.append( analysis.getProtocol().getDescription() );
        } else {
            // This can happen if we are re-writing files for a stored analysis that didn't get proper protocol information saved. 
            // Basically this is here for backwards compatibility. 
            log.warn( "No configuration or protocol available, adding available analysis information to header" );
            buf.append( "# Configuration information was not fully available" );
            buf.append( "# Factors:\n" );

            if ( analysis.getSubsetFactorValue() != null ) {
                buf.append( "# Subset ID=" ).append( bas.getId() ).append( "\n" );
                buf.append( "# Subset factor " ).append( analysis.getSubsetFactorValue().getExperimentalFactor() )
                        .append( "\n" );
                buf.append( "# Subset is of samples with " ).append( analysis.getSubsetFactorValue() ).append( "\n" );
            }

            for ( ExpressionAnalysisResultSet rs : resultSets ) {
                String f = StringUtils.join( rs.getExperimentalFactors(), ":" );
                buf.append( "# " ).append( f ).append( "\n" );
            }
        }

        buf.append( "# Generated by Gemma " ).append( timestamp ).append( " \n" );

        buf.append( DISCLAIMER );

        // Different Headers if Gene Annotations missing.
        if ( geneAnnotations.isEmpty() ) {
            //   log.info( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append( "#\n# The gene annotations were not available\n" );
            // but leave the blank columns there to make parsing easier.
        }
        buf.append( "Element_Name\tGene_Symbol\tGene_Name\tNCBI_ID" );// column information

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.

        return buf.toString();

    }

    private String makeDiffExpressionResultSetFileHeader( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations, DifferentialExpressionAnalysisConfig config ) {
        StringBuilder buf = new StringBuilder();

        System.out.println("accessing experiment analyzed for rs "+resultSet.getId());
        BioAssaySet bas = resultSet.getAnalysis().getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        Date timestamp = new Date( System.currentTimeMillis() );
        buf.append( "# Differential expression result set for:  " ).append( ee.getShortName() ).append( " : " )
                .append( ee.getName() ).append( " (ID=" ).append( ee.getId() ).append( ")\n" );
        buf.append( "# This file contains contrasts for:" );
        String f = StringUtils.join( resultSet.getExperimentalFactors(), " x " );
        buf.append( f ).append( "\n" );

        if ( resultSet.getAnalysis().getId() == null ) {
            buf.append( "# Analysis is not stored in the database\n" );
        } else {
            buf.append( "# Analysis ID = " ).append( resultSet.getAnalysis().getId() ).append( "\n" );
        }

        if ( resultSet.getId() != null ) {
            buf.append( "# ResultSet ID = " ).append( resultSet.getId() ).append( "\n" );
        }

        /*
         * Use the config if available; otherwise the protocol description
         * (which currently is same as config.toString() anyway; fall back on "by-hand", which we can probably get rid
         * of
         * later and always use the config (for new analyses) or stored protocol (for stored analyses)
         */
        buf.append( "# Analysis configuration:\n" );
        if ( config != null ) {
            buf.append( config );
        } else if ( resultSet.getAnalysis().getProtocol() != null && StringUtils
                .isNotBlank( resultSet.getAnalysis().getProtocol().getDescription() ) ) {
            buf.append( resultSet.getAnalysis().getProtocol().getDescription() );
        } else {
            log.warn( "Full configuration not available, adding available analysis information to header" );
            if ( resultSet.getAnalysis().getSubsetFactorValue() != null ) {
                buf.append( "# This analysis is for subset ID=" ).append( bas.getId() ).append( "\n" );
                buf.append( "# The subsetting factor was " )
                        .append( resultSet.getAnalysis().getSubsetFactorValue().getExperimentalFactor() )
                        .append( "\n" );
                buf.append( "# This subset is of samples with " )
                        .append( resultSet.getAnalysis().getSubsetFactorValue() ).append( "\n" );
            }
        }

        buf.append( "#\n# Generated by Gemma " ).append( timestamp ).append( " \n" );
        buf.append( DISCLAIMER + "#\n#\n" );

        if ( geneAnnotations.isEmpty() ) {
            // log.debug( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append(
                    "# The annotation file is missing for this Experiment, gene annotation information is omitted\n#\n" );
            // but leave the blank columns there to make parsing easier.
        }
        buf.append( "Element_Name\tGene_Symbol\tGene_Name\tNCBI_ID" );// column information

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.
        return buf.toString();
    }

    /**
     * Loads the probe to probe coexpression link information for a given expression experiment and writes it to disk.
     */
    private void writeCoexpressionData( File file, ExpressionExperiment ee ) throws IOException {

        Taxon tax = expressionExperimentService.getTaxon( ee );
        assert tax != null;

        // FIXME TESTME
        Collection<CoexpressionValueObject> geneLinks = gene2geneCoexpressionService.getCoexpression( ee, true );

        Date timestamp = new Date( System.currentTimeMillis() );
        StringBuilder buf = new StringBuilder();

        // Write header information
        buf.append( "# Coexpression data for:  " ).append( ee.getShortName() ).append( " : " ).append( ee.getName() )
                .append( " \n" );
        buf.append( "# Generated On: " ).append( timestamp ).append( " \n" );
        buf.append(
                "# Links are listed in an arbitrary order with an indication of positive or negative correlation\n" );
        buf.append( DISCLAIMER );
        buf.append( "GeneSymbol1\tGeneSymbol2\tDirection\tSupport\n" );

        // Data
        for ( CoexpressionValueObject link : geneLinks ) {

            buf.append( link.getQueryGeneSymbol() ).append( "\t" ).append( link.getCoexGeneSymbol() ).append( "\t" );

            buf.append( link.isPositiveCorrelation() ? "+" : "-" + "\n" );
        }

        // Write coexpression data to file (zipped of course)
        try (Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) )) {
            writer.write( buf.toString() );
        }

    }

    /**
     * @param compress if true, file will be output in GZIP format.
     */
    private File writeDataFile( ExpressionExperiment ee, boolean filtered, File f, boolean compress )
            throws IOException {
        log.info( "Creating new expression data file: " + f.getName() );
        ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );
        writeMatrix( f, geneAnnotations, matrix, compress );
        return f;
    }

    /**
     * Writes out the experimental design for the given experiment. The bioassays (col 0) matches match the header row
     * of the data matrix printed out by the {@link MatrixWriter}.
     *
     * @return file that was written
     */
    private File writeDesignMatrix( File file, ExpressionExperiment expressionExperiment, boolean orderByDesign )
            throws IOException {
        return writeDesignMatrix( file, expressionExperiment, orderByDesign, true );
    }

    /**
     * @return file that was written
     */
    private File writeDesignMatrix( File file, ExpressionExperiment expressionExperiment, boolean orderByDesign,
            boolean compress ) throws IOException {

        OutputStream ostream;

        if ( compress ) {
            ostream = new GZIPOutputStream( new FileOutputStream( file ) );
        } else {
            // TODO note that the file name will still have a .gz extension even though it is uncompressed, change later
            // if necessary
            file = new File( file.getAbsolutePath().replace( DATA_FILE_SUFFIX_COMPRESSED, DATA_FILE_SUFFIX ) );
            ostream = new FileOutputStream( file );
        }

        try (Writer writer = new OutputStreamWriter( ostream )) {
            ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
            edWriter.write( writer, expressionExperiment, true, orderByDesign );
        }
        return file;
    }

    private void writeJson( File file, Map<Long, Collection<Gene>> geneAnnotations,
            ExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {
        try (Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) )) {
            MatrixWriter matrixWriter = new MatrixWriter();
            matrixWriter.writeJSON( writer, expressionDataMatrix, true );
        }
    }

    private void writeJson( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors ) throws IOException {
        this.designElementDataVectorService.thaw( vectors );
        ExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );
        try (Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) )) {
            MatrixWriter matrixWriter = new MatrixWriter();
            matrixWriter.writeJSON( writer, expressionDataMatrix, true );
        }
    }

    private void writeMatrix( File file, Map<CompositeSequence, String[]> geneAnnotations,
            ExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {

        writeMatrix( file, geneAnnotations, expressionDataMatrix, true );

    }

    private void writeMatrix( File file, Map<CompositeSequence, String[]> geneAnnotations,
            ExpressionDataMatrix<?> expressionDataMatrix, boolean gzipped ) throws IOException {
        MatrixWriter matrixWriter = new MatrixWriter();

        if ( gzipped ) {
            try (Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) )) {
                matrixWriter.writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations, true );
            }
        } else {
            try (Writer writer = new OutputStreamWriter( new FileOutputStream( file ) )) {
                matrixWriter.writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations, true );
            }
        }

    }

    private void writeVectors( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors, Map<CompositeSequence, String[]> geneAnnotations )
            throws IOException {
        this.designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );

        writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

}
