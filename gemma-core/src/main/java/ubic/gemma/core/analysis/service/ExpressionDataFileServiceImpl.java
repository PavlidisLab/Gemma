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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.util.FileTools;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.*;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.DifferentialExpressionAnalysisResultComparator;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.core.config.Settings;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
@ParametersAreNonnullByDefault
public class ExpressionDataFileServiceImpl extends AbstractFileService<ExpressionExperiment> implements ExpressionDataFileService {

    private static final Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );
    private static final String MSG_FILE_EXISTS = " File (%s) exists, not regenerating";
    private static final String MSG_FILE_FORCED = "Forcing file (%s) regeneration";
    private static final String MSG_FILE_NOT_EXISTS = "File (%s) does not exist or can not be accessed ";
    private static final String MSG_FILE_OUTDATED = "File (%s) outdated, regenerating";

    private static ExpressionExperiment experimentForBioAssaySet( BioAssaySet bas ) {
        ExpressionExperiment ee;
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
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private CoexpressionService gene2geneCoexpressionService = null;
    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;

    @Override
    public void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf ) {
        Map<Long, StringBuilder> probe2String = new HashMap<>();

        List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults = null;

        for ( ExpressionAnalysisResultSet ears : results ) {
            sortedFirstColumnOfResults = this
                    .analysisResultSetToString( ears, geneAnnotations, buf, probe2String, sortedFirstColumnOfResults );

        } // ears loop

        buf.append( "\n" );

        if ( sortedFirstColumnOfResults == null ) {
            throw new IllegalStateException( "No results for " );
        }

        // Dump the probe data in the sorted order of the 1st column that we originally sorted
        for ( DifferentialExpressionAnalysisResult sortedResult : sortedFirstColumnOfResults ) {

            CompositeSequence cs = sortedResult.getProbe();
            StringBuilder sb = probe2String.get( cs.getId() );
            if ( sb == null ) {
                ExpressionDataFileServiceImpl.log.warn( "Unable to find element " + cs.getId() + " in map" );
                break;
            }
            buf.append( sb );
            buf.append( "\n" );

        }
    }

    @Override
    public List<DifferentialExpressionAnalysisResult> analysisResultSetToString( ExpressionAnalysisResultSet ears,
            Map<Long, String[]> geneAnnotations, StringBuilder buf, Map<Long, StringBuilder> probe2String,
            @Nullable List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults ) {

        if ( sortedFirstColumnOfResults == null ) { // Sort P values in ears (because 1st column)
            sortedFirstColumnOfResults = new ArrayList<>( ears.getResults() );
            sortedFirstColumnOfResults.sort( DifferentialExpressionAnalysisResultComparator.Factory.newInstance() );
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

            probeBuffer.append( "\t" ).append( format( correctedPvalue ) ).append( "\t" ).append( format( pvalue ) );

        }
        return sortedFirstColumnOfResults;

    }


    @Override
    public void deleteAllFiles( ExpressionExperiment ee ) {
        ee = this.expressionExperimentService.thawLite( ee );

        // data files.
        this.deleteAndLog( this.getOutputFile( ee, true ) );
        this.deleteAndLog( this.getOutputFile( ee, false ) );

        // diff ex files
        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            this.deleteDiffExArchiveFile( analysis );
        }

        // coexpression file
        this.deleteAndLog( this.getOutputFile( this.getCoexpressionDataFilename( ee ) ) );

        // design file
        this.deleteAndLog( this.getOutputFile( this.getDesignFileName( ee ) ) );
    }

    @Override
    public void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) {
        String filename = this.getDiffExArchiveFileName( analysis );
        this.deleteAndLog( this.getOutputFile( filename ) );
    }

    @Override
    public File getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) {
        DifferentialExpressionAnalysis analysis = this.differentialExpressionAnalysisService.loadOrFail( analysisId );
        return getDiffExpressionAnalysisArchiveFile( analysis, forceCreate );
    }

    @Override
    public File getOutputFile( ExpressionExperiment ee, boolean filtered ) {
        return this.getOutputFile( ee, filtered, true, false );
    }

    @Override
    public File getOutputFile( ExpressionExperiment ee, boolean filtered, boolean compressed, boolean temporary ) {
        String filteredAdd = "";
        if ( !filtered ) {
            filteredAdd = ".unfilt";
        }
        String suffix;

        if ( compressed ) {
            suffix = ExpressionDataFileService.DATA_FILE_SUFFIX_COMPRESSED;
        } else {
            suffix = ExpressionDataFileService.DATA_FILE_SUFFIX;
        }

        String filename = this.getDataFileName( ee, filteredAdd, suffix );

        // randomize file name if temporary in case of access by more than one user at once
        if ( temporary ) {

            filename = RandomStringUtils.randomAlphabetic( 6 ) + filename;

        }

        return this.getOutputFile( filename, temporary );
    }

    @Override
    public File getOutputFile( String filename ) {
        return this.getOutputFile( filename, false );

    }

    @Override
    public File getOutputFile( String filename, boolean temporary ) {
        String fullFilePath;
        if ( temporary ) {
            fullFilePath = ExpressionDataFileService.TMP_DATA_DIR + filename;
        } else {
            fullFilePath = ExpressionDataFileService.DATA_DIR + filename;
        }
        File f = new File( fullFilePath );

        if ( f.exists() ) {
            return f;
        }

        // ensure the parent directory exists
        try {
            FileUtils.forceMkdirParent( f );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return f;
    }

    @Override
    public File getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type ) {
        File file = Paths.get( Settings.getString( "gemma.appdata.home" ), "metadata", this.getEEFolderName( ee ), type.getFileName( ee ) )
                .toFile();

        // If this is a directory, check if we can read the most recent file.
        if ( type.isDirectory() ) {
            File fNew = this.getNewestFile( file );
            if ( fNew != null ) {
                file = fNew;
            }
        }

        return file;
    }

    /**
     * Forms a folder name where the given experiments metadata will be located (within the {@link ExpressionDataFileService#METADATA_DIR} directory).
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

    /**
     * @param file a directory to scan
     * @return the file in the directory that was last modified, or null, if such file doesn't exist or is not readable.
     */
    private File getNewestFile( File file ) {
        File[] files = file.listFiles();
        if ( files != null && files.length > 0 ) {
            List<File> fList = Arrays.asList( files );

            // Sort by last modified, we only want the newest file
            fList.sort( Comparator.comparingLong( File::lastModified ) );

            if ( fList.get( 0 ).canRead() ) {
                return fList.get( 0 );
            }
        }
        return null;
    }

    @Override
    public Optional<File> writeProcessedExpressionDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException, FilteringException {
        File f = new File( fileName );
        return this.writeDataFile( ee, filtered, f, compress );
    }

    @Override
    public void writeDiffExArchiveFile( BioAssaySet experimentAnalyzed, DifferentialExpressionAnalysis analysis,
            @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException {
        Collection<ArrayDesign> arrayDesigns = this.expressionExperimentService
                .getArrayDesignsUsed( experimentAnalyzed );
        Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );
        if ( analysis.getExperimentAnalyzed().getId() == null ) {// this can happen when using -nodb
            analysis.getExperimentAnalyzed().setId( experimentAnalyzed.getId() );
        }
        String filename = this.getDiffExArchiveFileName( analysis );
        File f = this.getOutputFile( filename );

        ExpressionDataFileServiceImpl.log
                .info( "Creating differential expression analysis archive file: " + f.getName() );
        try ( ZipOutputStream zipOut = new ZipOutputStream( new FileOutputStream( f ) ) ) {

            // top-level analysis results - ANOVA-style
            zipOut.putNextEntry( new ZipEntry( "analysis.results.txt" ) );
            String analysisData = this.convertDiffExpressionAnalysisData( analysis, geneAnnotations, config );
            zipOut.write( analysisData.getBytes() );
            zipOut.closeEntry();

            if ( analysis.getId() != null ) // might be transient if using -nodb from CLI
                analysis = differentialExpressionAnalysisService.thawFully( analysis );

            // Add a file for each result set with contrasts information.
            int i = 0;
            for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                if ( resultSet.getExperimentalFactors().size() > 1 ) {
                    // Skip interactions.
                    ExpressionDataFileServiceImpl.log.info( "Result file for interaction is omitted" ); // Why?
                    continue;
                }

                String resultSetData = this.convertDiffExpressionResultSetData( resultSet, geneAnnotations, config );

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
    @Transactional(readOnly = true)
    public void writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        ee = expressionExperimentService.find( ee );
        if ( ee == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment has been removed." );
        }
        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService.getRawExpressionDataMatrix( ee, qt );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        new MatrixWriter().writeWithStringifiedGeneAnnotations( writer, matrix, getGeneAnnotationsAsStringsByProbe( ads ), true );
    }

    @Override
    @Transactional(readOnly = true)
    public void writeProcessedExpressionData( ExpressionExperiment ee, Writer writer ) throws IOException {
        ee = expressionExperimentService.find( ee );
        if ( ee == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment has been removed." );
        }
        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        if ( matrix == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment has no processed data vectors." );
        }
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        new MatrixWriter().writeWithStringifiedGeneAnnotations( writer, matrix, getGeneAnnotationsAsStringsByProbe( ads ), true );
    }

    @Override
    public Optional<File> writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        try {
            File f = this.getOutputFile( this.getCoexpressionDataFilename( ee ) );
            if ( !forceWrite && f.canRead() ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return Optional.of( f );
            }

            if ( this.writeCoexpressionData( f, ee ) ) {
                return Optional.of( f );
            } else {
                return Optional.empty();
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @Override
    public Optional<File> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException {
        try {
            File f = this.getOutputFile( ee, filtered );
            Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

            if ( this.checkFileOkToReturn( forceWrite, f, check ) ) {
                return Optional.of( f );
            }

            return this.writeDataFile( ee, filtered, f, true );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public File writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) {

        try {
            File f = this.getOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f;
            }

            ExpressionDataFileServiceImpl.log
                    .info( "Creating new quantitation type expression data file: " + f.getName() );

            Collection<BulkExpressionDataVector> vectors = rawAndProcessedExpressionDataVectorService.findAndThaw( type );
            Collection<ArrayDesign> arrayDesigns = this.getArrayDesigns( vectors );
            Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );

            if ( vectors.isEmpty() ) {
                ExpressionDataFileServiceImpl.log.warn( "No vectors for " + type );
                return null;
            }

            this.writeVectors( f, vectors, geneAnnotations );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) {
        ee = expressionExperimentService.thawLite( ee );
        try {
            File f = this.getOutputFile( this.getDesignFileName( ee ) );
            Date check = ee.getCurationDetails().getLastUpdated();

            if ( check != null && this.checkFileOkToReturn( forceWrite, f, check ) ) {
                return f;
            }

            return this.writeDesignMatrix( f, ee );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @Override
    public Collection<File> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) {

        ee = this.expressionExperimentService.thawLite( ee );

        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );

        Collection<File> result = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            result.add( this.getDiffExpressionAnalysisArchiveFile( analysis, forceWrite ) );
        }

        return result;

    }

    @Override
    public Optional<File> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException {

        try {
            File f = this.getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return Optional.of( f );
            }

            ExpressionDataFileServiceImpl.log.info( "Creating new JSON expression data file: " + f.getName() );
            ExpressionDataDoubleMatrix matrix = this.getDataMatrix( ee, filtered );
            if ( matrix == null ) {
                return Optional.empty();
            }
            this.writeJson( f, matrix );
            return Optional.of( f );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public File writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) {

        try {
            File f = this.getJSONOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
                return f;
            }

            ExpressionDataFileServiceImpl.log.info( "Creating new quantitation type  JSON data file: " + f.getName() );

            Collection<BulkExpressionDataVector> vectors = rawAndProcessedExpressionDataVectorService.findAndThaw( type );

            if ( vectors.isEmpty() ) {
                ExpressionDataFileServiceImpl.log.warn( "No vectors for " + type );
                return null;
            }

            this.writeJson( f, vectors );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void addGeneAnnotationsToLine( StringBuilder rowBuffer, DifferentialExpressionAnalysisResult dear,
            Map<Long, String[]> geneAnnotations ) {
        CompositeSequence cs = dear.getProbe();
        Long csid = cs.getId();
        rowBuffer.append( cs.getName() );
        if ( geneAnnotations.containsKey( csid ) ) {
            String[] annotationStrings = geneAnnotations.get( csid );
            rowBuffer.append( "\t" ).append( annotationStrings[1] ).append( "\t" ).append( annotationStrings[2] ).append( "\t" );

            // leaving out Gemma ID, which is annotationStrings[3]
            if ( annotationStrings.length > 4 ) {
                // ncbi id, if we have it.
                rowBuffer.append( annotationStrings[4] );
            }
        } else {
            rowBuffer.append( "\t\t\t" );
        }
    }

    private String analysisResultSetWithContrastsToString( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();

        ExperimentalFactor ef = resultSet.getExperimentalFactors().iterator().next();

        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

            buf.append( "\tCoefficient_" ).append( StringUtil.makeValidForR( ef.getName() ) ).append( "\tPValue_" )
                    .append( StringUtil.makeValidForR( ef.getName() ) ).append( "\n" );

            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                StringBuilder rowBuffer = new StringBuilder();

//                if ( geneAnnotations.isEmpty() ) {
//                    rowBuffer.append( dear.getProbe().getName() );
//                } else {
                this.addGeneAnnotationsToLine( rowBuffer, dear, geneAnnotations );
                // }

                /*
                If there are no results for the DEAR then we wouldn't expect contrasts, so we just leave a blank.
                 */
                if ( dear.getPvalue() == null ) {
                    String contrastData = "\t\t";
                    rowBuffer.append( contrastData );
                    buf.append( rowBuffer ).append( '\n' );
                    continue;
                }


                if ( dear.getContrasts().size() != 1 ) {
                    //
                    throw new IllegalStateException( "Expected exactly one contrast for continuous factor" );
                }

                ContrastResult contrast = dear.getContrasts().iterator().next();

                Double coefficient = contrast.getCoefficient();
                Double pValue = contrast.getPvalue();
                String contrastData = "\t" + format( coefficient ) + "\t" + format( pValue );

                rowBuffer.append( contrastData );

                buf.append( rowBuffer ).append( '\n' );
            }

        } else {

            Long baselineId = resultSet.getBaselineGroup().getId();
            List<Long> factorValueIdOrder = new ArrayList<>();

            /*
             * First find out what factor values are relevant in case this is a subsetted analysis. With this we
             * probably not worry about the baselineId since it won't be here.
             */
            Collection<Long> usedFactorValueIds = new HashSet<>();
            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                for ( ContrastResult contrast : dear.getContrasts() ) {
                    if ( contrast.getFactorValue() != null ) {
                        usedFactorValueIds.add( contrast.getFactorValue().getId() );
                    }
                }
                break; // only have to look at one.
            }

            for ( FactorValue factorValue : ef.getFactorValues() ) {

                /*
                 * deal correctly with subset situations - only use factor values relevant to the subset
                 */
                if ( Objects.equals( factorValue.getId(), baselineId ) || !usedFactorValueIds.contains( factorValue.getId() ) ) {
                    continue;
                }
                factorValueIdOrder.add( factorValue.getId() );
                // Generate column headers, try to be R-friendly
                buf.append( "\tFoldChange_" ).append( this.getFactorValueString( factorValue ) );
                buf.append( "\tTstat_" ).append( this.getFactorValueString( factorValue ) );
                buf.append( "\tPValue_" ).append( this.getFactorValueString( factorValue ) );
            }

            buf.append( '\n' );

            // Generate element details
            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                StringBuilder rowBuffer = new StringBuilder();

                this.addGeneAnnotationsToLine( rowBuffer, dear, geneAnnotations );

                Map<Long, String> factorValueIdToData = new HashMap<>();
                // I don't think we can expect them in the same order.
                for ( ContrastResult contrast : dear.getContrasts() ) {
                    Double foldChange = contrast.getLogFoldChange();
                    Double pValue = contrast.getPvalue();
                    Double tStat = contrast.getTstat();
                    String contrastData = "\t" + format( foldChange ) + "\t" + format( tStat ) + "\t" + format( pValue );
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

                buf.append( rowBuffer ).append( '\n' );

            } // resultSet.getResults() loop
        }
        return buf.toString();
    }

    /**
     * Checks whether the given file is ok to return, or it should be regenerated.
     *
     * @param forceWrite whether the file should be overridden even if found.
     * @param f          the file to check.
     * @param check      the file will be considered invalid after this date.
     * @return true, if the given file is ok to be returned, false if it should be regenerated.
     */
    private boolean checkFileOkToReturn( boolean forceWrite, File f, Date check ) {
        Date modified = new Date( f.lastModified() );
        if ( f.canRead() ) {
            if ( forceWrite ) {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_FORCED, f.getPath() ) );
            } else if ( modified.after( check ) ) {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_OUTDATED, f.getPath() ) );
            } else {
                ExpressionDataFileServiceImpl.log
                        .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_EXISTS, f.getPath() ) );
                return true;
            }
        } else if ( !f.canRead() ) {
            ExpressionDataFileServiceImpl.log
                    .info( String.format( ExpressionDataFileServiceImpl.MSG_FILE_NOT_EXISTS, f.getPath() ) );
        }

        return false;
    }

    /**
     * Given diff exp analysis and gene annotation generate header and tab delimited data. The output is qValue....
     *
     * @param analysis (might not be persistent)
     */
    private String convertDiffExpressionAnalysisData( DifferentialExpressionAnalysis analysis,
            Map<Long, String[]> geneAnnotations, @Nullable DifferentialExpressionAnalysisConfig config ) {
        if ( analysis.getId() != null )
            analysis = differentialExpressionAnalysisService.thawFully( analysis );
        Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();
        if ( results == null || results.isEmpty() ) {
            ExpressionDataFileServiceImpl.log.warn( "No differential expression results found for " + analysis );
            return "";
        }

        StringBuilder buf = new StringBuilder();

        buf.append( this.makeDiffExpressionFileHeader( analysis, analysis.getResultSets(), geneAnnotations, config ) );
        this.analysisResultSetsToString( results, geneAnnotations, buf );

        return buf.toString();
    }

    /**
     * Given result set and gene annotation generate header and tab delimited data. The output is foldChange and pValue
     * associated with each contrast.
     * eneAnnotations
     */
    private String convertDiffExpressionResultSetData( ExpressionAnalysisResultSet resultSet,
            Map<Long, String[]> geneAnnotations, @Nullable DifferentialExpressionAnalysisConfig config ) {
        // Write header.
        // Write contrasts data.
        return this.makeDiffExpressionResultSetFileHeader( resultSet, geneAnnotations, config ) + this
                .analysisResultSetWithContrastsToString( resultSet, geneAnnotations );
    }

    private void deleteAndLog( File f1 ) {
        if ( f1.canWrite() && f1.delete() ) {
            ExpressionDataFileServiceImpl.log.info( "Deleted: " + f1 );
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
                + ExpressionDataFileService.DATA_FILE_SUFFIX_COMPRESSED;
    }

    /**
     * @return Name, without full path.
     */
    private String getDataFileName( ExpressionExperiment ee, String filteredAdd, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + filteredAdd + suffix;
    }

    @Nullable
    private ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered ) throws FilteringException {
        ee = expressionExperimentService.thawLite( ee );
        ExpressionDataDoubleMatrix matrix;
        if ( filtered ) {
            FilterConfig filterConfig = new FilterConfig();
            filterConfig.setIgnoreMinimumSampleThreshold( true );
            filterConfig.setIgnoreMinimumRowsThreshold( true );
            matrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig );
        } else {
            matrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        }
        if ( matrix == null ) {
            log.warn( String.format( "%s has no processed expression vectors.", ee ) );
        }
        return matrix;
    }

    private String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign"
                + ExpressionDataFileService.DATA_FILE_SUFFIX_COMPRESSED;
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
                + ExpressionDataFileService.DATA_ARCHIVE_FILE_SUFFIX;
    }

    private File getDiffExpressionAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, boolean forceCreate ) {
        String filename = this.getDiffExArchiveFileName( analysis );
        File f = this.getOutputFile( filename );

        // Force create if file is older than one year
        if ( !forceCreate && f.canRead() ) {
            Date d = new Date( f.lastModified() );
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.YEAR, -1 );
            forceCreate = d.before( new Date( calendar.getTimeInMillis() ) );
        }

        // If not force create and the file exists (can be read from), return the existing file.
        if ( !forceCreate && f.canRead() ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        // (Re-)create the file
        analysis = this.differentialExpressionAnalysisService.thawFully( analysis );
        BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();

        try {
            this.writeDiffExArchiveFile( experimentAnalyzed, analysis, null );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return f;
    }

    private String getFactorValueString( @Nullable FactorValue fv ) {
        String result;
        if ( fv == null )
            return "null";

        if ( fv.getCharacteristics() != null && !fv.getCharacteristics().isEmpty() ) {
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
        return result.replaceAll( "\\W+", "." );
    }

    /**
     * @return Map of composite sequence ids to an array of strings: [probe name, genes symbol(s), gene Name(s), gemma
     * id(s), ncbi id(s)].
     */
    private Map<Long, String[]> getGeneAnnotationsAsStrings( Collection<ArrayDesign> ads ) {
        Map<Long, String[]> annotations = new HashMap<>();
        ads = arrayDesignService.thaw( ads );
        for ( ArrayDesign arrayDesign : ads ) {
            annotations.putAll( ArrayDesignAnnotationServiceImpl.readAnnotationFileAsString( arrayDesign ) );
        }
        return annotations;
    }

    private Map<CompositeSequence, String[]> getGeneAnnotationsAsStringsByProbe( Collection<ArrayDesign> ads ) {
        Map<CompositeSequence, String[]> annotations = new HashMap<>();
        ads = arrayDesignService.thaw( ads );
        for ( ArrayDesign arrayDesign : ads ) {
            Map<Long, CompositeSequence> csIdMap = EntityUtils.getIdMap( arrayDesign.getCompositeSequences() );

            Map<Long, String[]> geneAnnotations = ArrayDesignAnnotationServiceImpl
                    .readAnnotationFileAsString( arrayDesign );

            for ( Entry<Long, String[]> e : geneAnnotations.entrySet() ) {

                if ( !csIdMap.containsKey( e.getKey() ) ) {
                    continue;
                }

                annotations.put( csIdMap.get( e.getKey() ), e.getValue() );

            }

        }
        return annotations;
    }

    private File getJSONOutputFile( QuantitationType type ) throws IOException {
        String filename = this.getJSONOutputFilename( type );
        String fullFilePath = ExpressionDataFileService.DATA_DIR + filename;

        File f = new File( fullFilePath );

        if ( f.exists() ) {
            ExpressionDataFileServiceImpl.log.warn( "Will overwrite existing file " + f );
            EntityUtils.deleteFile( f );
        }

        FileUtils.forceMkdirParent( f );
        EntityUtils.createFile( f );
        return f;
    }

    /**
     * @return Name, without full path.
     */
    private String getJSONOutputFilename( QuantitationType type ) {
        return FileTools.cleanForFileName( type.getName() ) + ExpressionDataFileService.JSON_FILE_SUFFIX;
    }

    private File getOutputFile( QuantitationType type ) {
        String filename = this.getOutputFilename( type );
        return this.getOutputFile( filename );
    }

    /**
     * @return Name, without full path.
     */
    private String getOutputFilename( QuantitationType type ) {
        return type.getId() + "_" + FileTools.cleanForFileName( type.getName() )
                + ExpressionDataFileService.DATA_FILE_SUFFIX_COMPRESSED;
    }

    private String makeDiffExpressionFileHeader( DifferentialExpressionAnalysis analysis,
            Collection<ExpressionAnalysisResultSet> resultSets, Map<Long, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config ) {

        if ( analysis.getId() != null ) // It might not be a persistent analysis: using -nodb
            analysis = differentialExpressionAnalysisService.thaw( analysis );

        StringBuilder buf = new StringBuilder();

        BioAssaySet bas = analysis.getExperimentAnalyzed();

        ExpressionExperiment ee = ExpressionDataFileServiceImpl.experimentForBioAssaySet( bas );

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
            buf.append( config );
        } else if ( analysis.getProtocol() != null && StringUtils
                .isNotBlank( analysis.getProtocol().getDescription() ) ) {
            buf.append( analysis.getProtocol().getDescription() );
        } else {
            // This can happen if we are re-writing files for a stored analysis that didn't get proper protocol information saved. 
            // Basically this is here for backwards compatibility. 
            ExpressionDataFileServiceImpl.log
                    .warn( "No configuration or protocol available, adding available analysis information to header" );
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

        buf.append( ExpressionDataFileService.DISCLAIMER );

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
            Map<Long, String[]> geneAnnotations, @Nullable DifferentialExpressionAnalysisConfig config ) {
        StringBuilder buf = new StringBuilder();

        BioAssaySet bas = resultSet.getAnalysis().getExperimentAnalyzed();

        ExpressionExperiment ee = ExpressionDataFileServiceImpl.experimentForBioAssaySet( bas );

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
            ExpressionDataFileServiceImpl.log
                    .warn( "Full configuration not available, adding available analysis information to header" );
            if ( resultSet.getAnalysis().getSubsetFactorValue() != null ) {
                buf.append( "# This analysis is for subset ID=" ).append( bas.getId() ).append( "\n" );
                buf.append( "# The subsetting factor was " )
                        .append( resultSet.getAnalysis().getSubsetFactorValue().getExperimentalFactor() )
                        .append( "\n" );
                buf.append( "# This subset is of samples with " )
                        .append( resultSet.getAnalysis().getSubsetFactorValue() ).append( "\n" );
            }
        }

        String batchConf = expressionExperimentService.getBatchConfound( ee );

        if ( batchConf != null ) {
            buf.append( "# !!! Warning, this dataset has a batch confound with the factors analysed\n" );
        }

        buf.append( "#\n# Generated by Gemma " ).append( timestamp ).append( " \n" );
        buf.append( ExpressionDataFileService.DISCLAIMER + "#\n" );

        if ( geneAnnotations.isEmpty() ) {
            // log.debug( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append(
                    "# The platform annotation file is missing for this Experiment, gene annotation information is omitted\n#\n" );
            // but leave the blank columns there to make parsing easier.
        }
        buf.append( "Element_Name\tGene_Symbol\tGene_Name\tNCBI_ID" );// column information

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.
        return buf.toString();
    }

    /**
     * Loads the probe to probe coexpression link information for a given expression experiment and writes it to disk.
     */
    private boolean writeCoexpressionData( File file, ExpressionExperiment ee ) throws IOException {
        Taxon tax = expressionExperimentService.getTaxon( ee );
        assert tax != null;

        Collection<CoexpressionValueObject> geneLinks = gene2geneCoexpressionService.getCoexpression( ee, true );

        if ( geneLinks.isEmpty() ) {
            log.warn( "No coexpression links for this experiment, file will not be created: " + ee );
            return false;
        }

        ExpressionDataFileServiceImpl.log.info( "Creating new coexpression data file: " + file.getAbsolutePath() );

        Date timestamp = new Date( System.currentTimeMillis() );
        StringBuilder buf = new StringBuilder();

        // Write header information
        buf.append( "# Coexpression data for:  " ).append( ee.getShortName() ).append( " : " ).append( ee.getName() )
                .append( " \n" );
        buf.append( "# Generated On: " ).append( timestamp ).append( " \n" );
        buf.append(
                "# Links are listed in an arbitrary order with an indication of positive or negative correlation\n" );
        buf.append( ExpressionDataFileService.DISCLAIMER );
        buf.append( "GeneSymbol1\tGeneSymbol2\tDirection\tSupport\n" );

        // Data
        for ( CoexpressionValueObject link : geneLinks ) {

            buf.append( link.getQueryGeneSymbol() ).append( "\t" ).append( link.getCoexGeneSymbol() ).append( "\t" );

            buf.append( link.isPositiveCorrelation() ? "+" : "-" + "\n" );
        }

        // Write coexpression data to file (zipped of course)
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) ) ) {
            writer.write( buf.toString() );
        }

        return true;
    }

    /**
     * @param compress if true, file will be output in GZIP format.
     */
    private Optional<File> writeDataFile( ExpressionExperiment ee, boolean filtered, File f, boolean compress )
            throws IOException, FilteringException {
        ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + f.getName() );
        ExpressionDataDoubleMatrix matrix = this.getDataMatrix( ee, filtered );
        if ( matrix == null ) {
            return Optional.empty();
        }
        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );
        this.writeMatrix( f, geneAnnotations, matrix, compress );
        return Optional.of( f );
    }

    /**
     * Writes out the experimental design for the given experiment. The bioassays (col 0) matches match the header row
     * of the data matrix printed out by the {@link MatrixWriter}.
     *
     * @return file that was written
     */
    private File writeDesignMatrix( File file, ExpressionExperiment expressionExperiment ) throws IOException {

        OutputStream oStream;
        oStream = new GZIPOutputStream( new FileOutputStream( file ) );

        try ( Writer writer = new OutputStreamWriter( oStream ) ) {
            ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
            edWriter.write( writer, expressionExperiment, true );
        }
        return file;
    }

    private void writeJson( File file, Collection<BulkExpressionDataVector> vectors ) throws IOException {
        BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) ) ) {
            MatrixWriter matrixWriter = new MatrixWriter();
            matrixWriter.writeJSON( writer, expressionDataMatrix );
        }
    }

    private void writeJson( File file, BulkExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) ) ) {
            MatrixWriter matrixWriter = new MatrixWriter();
            matrixWriter.writeJSON( writer, expressionDataMatrix );
        }
    }

    private void writeMatrix( File file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {

        this.writeMatrix( file, geneAnnotations, expressionDataMatrix, true );

    }

    private void writeMatrix( File file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix, boolean gzipped ) throws IOException {
        MatrixWriter matrixWriter = new MatrixWriter();

        if ( gzipped ) {
            try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) ) ) {
                matrixWriter.writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations, true );
            }
        } else {
            try ( Writer writer = new OutputStreamWriter( new FileOutputStream( file ) ) ) {
                matrixWriter.writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations, true );
            }
        }

    }

    private void writeVectors( File file, Collection<BulkExpressionDataVector> vectors,
            Map<CompositeSequence, String[]> geneAnnotations ) throws IOException {
        BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        this.writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

    @Override
    @Transactional(readOnly = true)
    public void writeTsv( ExpressionExperiment entity, Writer writer ) throws IOException {
        writeProcessedExpressionData( entity, writer );
    }
}
