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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.*;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.MexMatrixWriter;
import ubic.gemma.core.datastructure.matrix.io.TabularMatrixWriter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 *
 * @author paul
 */
@Component
@ParametersAreNonnullByDefault
public class ExpressionDataFileServiceImpl implements ExpressionDataFileService {

    private static final Log log = LogFactory.getLog( ExpressionDataFileServiceImpl.class.getName() );

    // for single-cell vectors
    private static final String SC_DATA_SUFFIX = ".scdata";
    private static final String MEX_SC_DATA_SUFFIX = SC_DATA_SUFFIX;
    private static final String TABULAR_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".tsv.gz";
    // for bulk (raw or processed vectors)
    private static final String BULK_DATA_SUFFIX = ".data";
    private static final String TABULAR_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".txt.gz";
    private static final String JSON_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".json.gz";

    private static final String MSG_FILE_EXISTS = " File (%s) exists, not regenerating";
    private static final String MSG_FILE_FORCED = "Forcing file (%s) regeneration";
    private static final String MSG_FILE_NOT_EXISTS = "File (%s) does not exist or can not be accessed ";
    private static final String MSG_FILE_OUTDATED = "File (%s) outdated, regenerating";

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;
    @Autowired
    private CoexpressionService gene2geneCoexpressionService = null;
    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;


    @Override
    public void deleteAllFiles( ExpressionExperiment ee ) {
        // NOTE: Never ever delete the metadata directory, it may contain precious information that was not generated by
        //       Gemma

        // data files.
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) ) );
        this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) ) );

        // per-QT output file
        for ( QuantitationType qt : expressionExperimentService.getQuantitationTypes( ee ) ) {
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, MEX_SC_DATA_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX ) ) );
            this.deleteAndLog( dataDir.resolve( getDataOutputFilename( ee, qt, JSON_BULK_DATA_FILE_SUFFIX ) ) );
        }

        // diff ex files
        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            this.deleteDiffExArchiveFile( analysis );
        }

        // coexpression file
        this.deleteAndLog( dataDir.resolve( this.getCoexpressionDataFilename( ee ) ) );

        // design file
        this.deleteAndLog( dataDir.resolve( this.getDesignFileName( ee ) ) );
    }

    private void deleteAndLog( Path f1 ) {
        try {
            if ( Files.deleteIfExists( f1 ) ) {
                ExpressionDataFileServiceImpl.log.info( "Deleted: " + f1 );
            }
        } catch ( IOException e ) {
            ExpressionDataFileServiceImpl.log.error( "Failed to delete " + f1, e );
        }
    }

    @Override
    public void deleteDiffExArchiveFile( DifferentialExpressionAnalysis analysis ) {
        this.deleteAndLog( dataDir.resolve( this.getDiffExArchiveFileName( analysis ) ) );
    }

    @Override
    public Path getDiffExpressionAnalysisArchiveFile( Long analysisId, boolean forceCreate ) throws IOException {
        DifferentialExpressionAnalysis analysis = this.differentialExpressionAnalysisService.loadOrFail( analysisId );
        return getDiffExpressionAnalysisArchiveFile( analysis, forceCreate );
    }

    @Override
    public Path getMetadataFile( ExpressionExperiment ee, ExpressionExperimentMetaFileType type ) {
        Path file = metadataDir.resolve( this.getEEFolderName( ee ) ).resolve( type.getFileName( ee ) );
        // If this is a directory, check if we can read the most recent file.
        if ( type.isDirectory() ) {
            Path fNew = this.getNewestFile( file );
            if ( fNew != null ) {
                file = fNew;
            }
        }
        return file;
    }

    /**
     * Forms a folder name where the given experiments metadata will be located (within the {@link #metadataDir} directory).
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
    private Path getNewestFile( Path file ) {
        File[] files = file.toFile().listFiles();
        if ( files != null && files.length > 0 ) {
            List<File> fList = Arrays.asList( files );

            // Sort by last modified, we only want the newest file
            fList.sort( Comparator.comparingLong( File::lastModified ) );

            if ( fList.get( 0 ).canRead() ) {
                return fList.get( 0 ).toPath();
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Path> getDataFile( ExpressionExperiment ee, boolean filtered, ExpressionExperimentDataFileType type ) {
        if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
            return Optional.empty();
        }
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
        return Optional.of( dataDir.resolve( getDataOutputFilename( ee, filtered, suffix ) ) );
    }

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Override
    @Transactional(readOnly = true)
    public Path getDataFile( ExpressionExperiment ee, QuantitationType qt, ExpressionExperimentDataFileType type ) {
        Class<? extends DataVector> dataType = quantitationTypeService.getDataVectorType( qt );
        String suffix;
        switch ( type ) {
            case TABULAR:
                if ( BulkExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    suffix = TABULAR_BULK_DATA_FILE_SUFFIX;
                } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( dataType ) ) {
                    suffix = TABULAR_SC_DATA_SUFFIX;
                } else {
                    throw new IllegalArgumentException( "Unknown vector type: " + dataType );
                }
                break;
            case JSON:
                Assert.isAssignable( BulkExpressionDataVector.class, dataType );
                suffix = JSON_BULK_DATA_FILE_SUFFIX;
                break;
            case MEX:
                Assert.isAssignable( SingleCellExpressionDataVector.class, dataType );
                suffix = MEX_SC_DATA_SUFFIX;
                break;
            default:
                throw new IllegalArgumentException( "Unsupported data file type: " + type );
        }
        return dataDir.resolve( getDataOutputFilename( ee, qt, suffix ) );
    }

    @Override
    public Optional<Path> getExperimentalDesignFile( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            return Optional.empty();
        }
        return Optional.of( dataDir.resolve( getDesignFileName( ee ) ) );
    }

    @Override
    public Optional<Path> writeProcessedExpressionDataFile( ExpressionExperiment ee, boolean filtered, String fileName, boolean compress )
            throws IOException, FilteringException {
        return this.writeDataFile( ee, filtered, Paths.get( fileName ), compress );
    }

    @Override
    @Transactional(readOnly = true)
    public int writeTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, Writer writer ) throws IOException {
        long numVecs = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
        if ( numVecs == 0 ) {
            throw new IllegalStateException( "There are no vectors for " + qt + " in " + ee + "." );
        }
        log.info( "Will write tabular data for " + qt + " to  a stream." );
        return doWriteTabularSingleCellExpressionData( ee, qt, useStreaming, fetchSize, writer );
    }

    @Override
    @Transactional(readOnly = true)
    public Path writeOrLocateTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        Path dest = getOutputFile( getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX ) );
        if ( !forceWrite && Files.exists( dest ) ) {
            return dest;
        }
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( dest ) ), StandardCharsets.UTF_8 ) ) {
            log.info( "Will write tabular data for " + qt + " to " + dest + "." );
            doWriteTabularSingleCellExpressionData( ee, qt, useStreaming, fetchSize, writer );
            return dest;
        } catch ( Exception e ) {
            Files.deleteIfExists( dest );
            throw e;
        }
    }

    private int doWriteTabularSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, Writer writer ) throws IOException {
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee, qt, SingleCellExpressionDataVector.class );
        Map<CompositeSequence, Set<Gene>> cs2gene = arrayDesignService.getGenesByCompositeSequence( ads );
        Stream<SingleCellExpressionDataVector> vectors;
        if ( useStreaming ) {
            vectors = singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize );
        } else {
            vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ).stream();
        }
        return new TabularMatrixWriter().write( vectors, cs2gene, writer );
    }

    @Override
    @Transactional(readOnly = true)
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, OutputStream stream ) throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "There are no vectors associated to " + qt + " in " + ee + "." );
        }
        log.info( "Will write MEX data for " + qt + " to a stream " + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee, qt, SingleCellExpressionDataVector.class );
        Map<CompositeSequence, Set<Gene>> cs2gene = arrayDesignService.getGenesByCompositeSequence( ads );
        DoubleSingleCellExpressionDataMatrix matrix = new DoubleSingleCellExpressionDataMatrix( vectors );
        MexMatrixWriter writer = new MexMatrixWriter();
        writer.setUseEnsemblIds( useEnsemblIds );
        return writer.write( matrix, cs2gene, stream );
    }

    @Override
    @Transactional(readOnly = true)
    public int writeMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useEnsemblIds, boolean useStreaming, int fetchSize, boolean forceWrite, Path destDir ) throws IOException {
        if ( !forceWrite && Files.exists( destDir ) ) {
            throw new IllegalArgumentException( "Output directory " + destDir + " already exists." );
        }
        long numVecs = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
        if ( numVecs == 0 ) {
            throw new IllegalStateException( "There are no vectors for " + qt + " in " + ee + "." );
        }
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee, qt, SingleCellExpressionDataVector.class );
        Map<CompositeSequence, Set<Gene>> cs2gene = arrayDesignService.getGenesByCompositeSequence( ads );
        MexMatrixWriter writer = new MexMatrixWriter();
        writer.setUseEnsemblIds( useEnsemblIds );
        log.info( "Will write MEX data for " + qt + " to " + destDir + ( useEnsemblIds ? " using Ensembl IDs" : "" ) + "." );
        if ( useStreaming ) {
            Map<BioAssay, Long> nnzBySample = singleCellExpressionExperimentService.getNumberOfNonZeroesBySample( ee, qt, fetchSize );
            Stream<SingleCellExpressionDataVector> vectors = singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize );
            if ( Files.exists( destDir ) ) {
                log.info( destDir + " already exists, removing..." );
                PathUtils.deleteDirectory( destDir );
            }
            try {
                return writer.write( vectors.peek( createStreamMonitor( numVecs ) ), ( int ) numVecs, nnzBySample, cs2gene, destDir );
            } catch ( Exception e ) {
                PathUtils.deleteDirectory( destDir );
                throw e;
            }
        } else {
            SingleCellExpressionDataMatrix<Double> matrix = singleCellExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, qt );
            try {
                return writer.write( matrix, cs2gene, destDir );
            } catch ( Exception e ) {
                PathUtils.deleteDirectory( destDir );
                throw e;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Path writeOrLocateMexSingleCellExpressionData( ExpressionExperiment ee, QuantitationType qt, boolean useStreaming, int fetchSize, boolean forceWrite ) throws IOException {
        Path dest = getOutputFile( getDataOutputFilename( ee, qt, MEX_SC_DATA_SUFFIX ) );
        if ( !forceWrite && Files.exists( dest ) ) {
            return dest;
        }
        int written = writeMexSingleCellExpressionData( ee, qt, false, useStreaming, fetchSize, forceWrite, dest );
        log.info( "Wrote " + written + " vectors for " + qt + " to " + dest + "." );
        return dest;
    }

    @Override
    public void writeDiffExArchiveFile( BioAssaySet experimentAnalyzed, DifferentialExpressionAnalysis analysis,
            @Nullable DifferentialExpressionAnalysisConfig config ) throws IOException {
        ExpressionExperiment ee = experimentForBioAssaySet( experimentAnalyzed );
        boolean hasSignificantBatchConfound = expressionExperimentBatchInformationService.hasSignificantBatchConfound( ee );

        Collection<ArrayDesign> arrayDesigns = this.expressionExperimentService
                .getArrayDesignsUsed( experimentAnalyzed );
        Map<Long, String[]> geneAnnotations = getGeneAnnotationsAsStrings( arrayDesigns );
        if ( analysis.getExperimentAnalyzed().getId() == null ) {// this can happen when using -nodb
            analysis.getExperimentAnalyzed().setId( experimentAnalyzed.getId() );
        }

        // It might not be a persistent analysis: using -nodb
        if ( analysis.getId() != null ) {
            analysis = differentialExpressionAnalysisService.thawFully( analysis );
        }

        Path f = this.getOutputFile( this.getDiffExArchiveFileName( analysis ) );
        log.info( "Creating differential expression analysis archive file: " + f );
        try ( OutputStream stream = Files.newOutputStream( f ) ) {
            new DiffExAnalysisResultSetWriter().write( analysis, geneAnnotations, config, hasSignificantBatchConfound, stream );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        }
    }

    private ExpressionExperiment experimentForBioAssaySet( BioAssaySet bas ) {
        ExpressionExperiment ee;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }
        return ee;
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

    @Override
    @Transactional(readOnly = true)
    public int writeRawExpressionData( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        ee = expressionExperimentService.find( ee );
        if ( ee == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment has been removed." );
        }
        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService.getRawExpressionDataMatrix( ee, qt );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        return new MatrixWriter().writeWithStringifiedGeneAnnotations( writer, matrix, getGeneAnnotationsAsStringsByProbe( ads ) );
    }

    @Override
    public void writeDesignMatrix( ExpressionExperiment ee, Writer writer ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }
        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
        edWriter.write( writer, ee, true );
    }

    @Override
    @Transactional(readOnly = true)
    public int writeProcessedExpressionData( ExpressionExperiment ee, boolean filtered, Writer writer ) throws FilteringException, IOException {
        ee = expressionExperimentService.find( ee );
        if ( ee == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment has been removed." );
        }
        ExpressionExperiment finalEe = ee;
        ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered )
                .orElseThrow( () -> new IllegalArgumentException( finalEe + " has no processed data vectors." ) );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        return new MatrixWriter().writeWithStringifiedGeneAnnotations( writer, matrix, getGeneAnnotationsAsStringsByProbe( ads ) );
    }

    @Override
    public Path writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        Path f = this.getOutputFile( this.getCoexpressionDataFilename( ee ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }
        Taxon tax = expressionExperimentService.getTaxon( ee );
        assert tax != null;

        Collection<CoexpressionValueObject> geneLinks = gene2geneCoexpressionService.getCoexpression( ee, true );

        if ( geneLinks.isEmpty() ) {
            throw new IllegalStateException( "No coexpression links for this experiment, file will not be created: " + ee );
        }

        ExpressionDataFileServiceImpl.log.info( "Creating new coexpression data file: " + f.toAbsolutePath() );

        // Write coexpression data to file (zipped of course)
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( f ) ), StandardCharsets.UTF_8 ) ) {
            new CoexpressionWriter().write( ee, geneLinks, writer );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        }
        return f;
    }

    @Override
    public Optional<Path> writeOrLocateProcessedDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException {
        // randomize file name if temporary in case of access by more than one user at once
        String result;
        if ( filtered ) {
            result = getDataOutputFilename( ee, true, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        } else {
            result = getDataOutputFilename( ee, false, ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX );
        }
        Path f = this.getOutputFile( result );
        Date check = expressionExperimentService.getLastArrayDesignUpdate( ee );

        if ( this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }

        return this.writeDataFile( ee, filtered, f, true );
    }

    @Override
    public Path writeOrLocateRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        Path f = this.getOutputFile( this.getDataOutputFilename( ee, type, TABULAR_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        ExpressionDataFileServiceImpl.log
                .info( "Creating new quantitation type expression data file: " + f );

        Collection<BulkExpressionDataVector> vectors = rawAndProcessedExpressionDataVectorService.findAndThaw( type );
        Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( this.getArrayDesigns( vectors ) );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors for " + type );
        }

        int written = this.writeVectors( f, vectors, geneAnnotations );
        log.info( "Wrote " + written + " vectors for " + type + "." );
        return f;
    }

    @Override
    public Optional<Path> writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            return Optional.empty();
        }
        Path f = this.getOutputFile( this.getDesignFileName( ee ) );
        Date check = ee.getCurationDetails().getLastUpdated();
        if ( check != null && this.checkFileOkToReturn( forceWrite, f, check ) ) {
            return Optional.of( f );
        }
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( f ) ), StandardCharsets.UTF_8 ) ) {
            writeDesignMatrix( ee, writer );
        } catch ( Exception e ) {
            Files.deleteIfExists( f );
            throw e;
        }
        return Optional.of( f );
    }

    @Override
    public Collection<Path> writeOrLocateDiffExpressionDataFiles( ExpressionExperiment ee, boolean forceWrite ) throws IOException {

        ee = this.expressionExperimentService.thawLite( ee );

        Collection<DifferentialExpressionAnalysis> analyses = this.differentialExpressionAnalysisService
                .getAnalyses( ee );

        Collection<Path> result = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            result.add( this.getDiffExpressionAnalysisArchiveFile( analysis, forceWrite ) );
        }

        return result;

    }

    @Override
    public Optional<Path> writeOrLocateJSONProcessedExpressionDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) throws FilteringException, IOException {
        // randomize file name if temporary in case of access by more than one user at once
        Path f = this.getOutputFile( getDataOutputFilename( ee, filtered, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return Optional.of( f );
        }

        ExpressionDataFileServiceImpl.log.info( "Creating new JSON expression data file: " + f );
        Optional<ExpressionDataDoubleMatrix> matrix = this.getDataMatrix( ee, filtered );
        if ( matrix.isPresent() ) {
            int written = this.writeJson( f, matrix.get() );
            log.info( "Wrote " + written + " vectors to " + f + "." );
            return Optional.of( f );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Path writeOrLocateJSONRawExpressionDataFile( ExpressionExperiment ee, QuantitationType type, boolean forceWrite ) throws IOException {
        Path f = getOutputFile( getDataOutputFilename( ee, type, ExpressionDataFileServiceImpl.JSON_BULK_DATA_FILE_SUFFIX ) );
        if ( !forceWrite && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        ExpressionDataFileServiceImpl.log.info( "Creating new quantitation type  JSON data file: " + f );

        Collection<BulkExpressionDataVector> vectors = rawAndProcessedExpressionDataVectorService.findAndThaw( type );

        if ( vectors.isEmpty() ) {
            ExpressionDataFileServiceImpl.log.warn( "No vectors for " + type );
            return null;
        }

        int written = this.writeJson( f, vectors );
        log.info( "Wrote " + written + " vectors for " + type + " to " + f );
        return f;
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


    private Collection<ArrayDesign> getArrayDesigns( Collection<? extends DesignElementDataVector> vectors ) {
        Collection<ArrayDesign> ads = new HashSet<>();
        for ( DesignElementDataVector v : vectors ) {
            ads.add( v.getDesignElement().getArrayDesign() );
        }
        return ads;
    }

    private Optional<ExpressionDataDoubleMatrix> getDataMatrix( ExpressionExperiment ee, boolean filtered ) throws FilteringException {
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
        return Optional.ofNullable( matrix );
    }

    private String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign"
                + ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX;
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
                + ".zip";
    }

    private Path getDiffExpressionAnalysisArchiveFile( DifferentialExpressionAnalysis analysis, boolean forceCreate ) throws IOException {
        String filename = this.getDiffExArchiveFileName( analysis );
        Path f = this.getOutputFile( filename );

        // Force create if file is older than one year
        if ( !forceCreate && Files.exists( f ) ) {
            Date d = new Date( f.toFile().lastModified() );
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.YEAR, -1 );
            forceCreate = d.before( new Date( calendar.getTimeInMillis() ) );
        }

        // If not force create and the file exists (can be read from), return the existing file.
        if ( !forceCreate && Files.exists( f ) ) {
            ExpressionDataFileServiceImpl.log.info( f + " exists, not regenerating" );
            return f;
        }

        // (Re-)create the file
        analysis = this.differentialExpressionAnalysisService.thawFully( analysis );
        BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();

        this.writeDiffExArchiveFile( experimentAnalyzed, analysis, null );

        return f;
    }

    private Map<CompositeSequence, String[]> getGeneAnnotationsAsStringsByProbe( Collection<ArrayDesign> ads ) {
        Map<CompositeSequence, String[]> annotations = new HashMap<>();
        ads = arrayDesignService.thaw( ads );
        for ( ArrayDesign arrayDesign : ads ) {
            Map<Long, CompositeSequence> csIdMap = IdentifiableUtils.getIdMap( arrayDesign.getCompositeSequences() );

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

    /**
     * Obtain a filename for writing the processed data.
     */
    public String getDataOutputFilename( ExpressionExperiment ee, boolean filtered, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    private String getDataOutputFilename( ExpressionExperiment ee, QuantitationType type, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + "_expmat.unfilt" + suffix;
    }

    /**
     * Obtain the filename for writing coexpression data.
     */
    private String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp"
                + ExpressionDataFileServiceImpl.TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    /**
     * Resolve a filename in the {@link #dataDir} directory and create its parent directories.
     * @param filename without the path - that is, just the name of the file
     * @return File, with location in the appropriate target directory.
     */
    private Path getOutputFile( String filename ) throws IOException {
        Path fullFilePath = dataDir.resolve( filename );
        if ( Files.exists( fullFilePath ) ) {
            return fullFilePath;
        }
        // ensure the parent directory exists
        PathUtils.createParentDirectories( fullFilePath );
        return fullFilePath;
    }

    /**
     * @param compress if true, file will be output in GZIP format.
     */
    private Optional<Path> writeDataFile( ExpressionExperiment ee, boolean filtered, Path f, boolean compress )
            throws IOException, FilteringException {
        ExpressionDataFileServiceImpl.log.info( "Creating new expression data file: " + f );
        Optional<ExpressionDataDoubleMatrix> matrix = this.getDataMatrix( ee, filtered );
        if ( matrix.isPresent() ) {
            Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
            Map<CompositeSequence, String[]> geneAnnotations = this.getGeneAnnotationsAsStringsByProbe( arrayDesigns );
            int written = this.writeMatrix( f, geneAnnotations, matrix.get(), compress );
            log.info( "Wrote " + written + " vectors to " + f + "." );
            return Optional.of( f );
        } else {
            return Optional.empty();
        }
    }

    private int writeJson( Path file, Collection<BulkExpressionDataVector> vectors ) throws IOException {
        BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( file ) ), StandardCharsets.UTF_8 ) ) {
            MatrixWriter matrixWriter = new MatrixWriter();
            return matrixWriter.writeJSON( writer, expressionDataMatrix );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        }
    }

    private int writeJson( Path file, BulkExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( file ) ), StandardCharsets.UTF_8 ) ) {
            MatrixWriter matrixWriter = new MatrixWriter();
            return matrixWriter.writeJSON( writer, expressionDataMatrix );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        }
    }

    private int writeMatrix( Path file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix ) throws IOException {
        return this.writeMatrix( file, geneAnnotations, expressionDataMatrix, true );
    }

    private int writeMatrix( Path file, Map<CompositeSequence, String[]> geneAnnotations,
            BulkExpressionDataMatrix<?> expressionDataMatrix, boolean gzipped ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( gzipped ? new GZIPOutputStream( Files.newOutputStream( file ) ) : Files.newOutputStream( file ), StandardCharsets.UTF_8 ) ) {
            return new MatrixWriter().writeWithStringifiedGeneAnnotations( writer, expressionDataMatrix, geneAnnotations );
        } catch ( Exception e ) {
            Files.deleteIfExists( file );
            throw e;
        }
    }

    private int writeVectors( Path file, Collection<BulkExpressionDataVector> vectors,
            Map<CompositeSequence, String[]> geneAnnotations ) throws IOException {
        BulkExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( vectors );
        return this.writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

    private Consumer<SingleCellExpressionDataVector> createStreamMonitor( long numVecs ) {
        if ( !log.isDebugEnabled() ) {
            return v -> {
                // empty on purpose, debug logging is not enabled
            };
        }
        return new Consumer<SingleCellExpressionDataVector>() {
            final StopWatch timer = StopWatch.createStarted();
            final AtomicInteger i = new AtomicInteger();

            @Override
            public void accept( SingleCellExpressionDataVector x ) {
                int done = i.incrementAndGet();
                if ( done % 100 == 0 ) {
                    log.debug( String.format( "Processed %d/%d vectors (%f.2 vectors/sec)", done, numVecs, 1000.0 * done / timer.getTime() ) );
                }
            }
        };
    }
}
