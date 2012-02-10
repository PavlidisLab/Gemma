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
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.ProbeLink;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.DifferentialExpressionAnalysisResultComparator;

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
public class ExpressionDataFileService {

    private static final String DECIMAL_FORMAT = "%.4g";

    public static final String DATA_FILE_SUFFIX = ".data.txt.gz";

    public static final String JSON_FILE_SUFFIX = ".data.json.gz";

    public static final String DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    public static final String DISCLAIMER = "# If you use this file for your research, please cite the Gemma web site\n";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService = null;

    /**
     * @param ee
     * @param filtered if the data matrix is filtered
     * @return
     */
    public File getOutputFile( ExpressionExperiment ee, boolean filtered ) {
        String filteredAdd = "";
        if ( !filtered ) {
            filteredAdd = ".unfilt";
        }
        String filename = ee.getId() + "_" + ee.getShortName().replaceAll( "\\s+", "_" ) + "_expmat" + filteredAdd
                + DATA_FILE_SUFFIX;
        return getOutputFile( filename );
    }

    /**
     * @param type
     * @return
     */
    public File getOutputFile( QuantitationType type ) {
        String filename = type.getId() + "_" + type.getName().replaceAll( "\\s+", "_" ) + DATA_FILE_SUFFIX;
        return getOutputFile( filename );
    }

    /**
     * @param filename
     * @return
     */
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

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param differentialExpressionResultService the differentialExpressionResultService to set
     */
    public void setDifferentialExpressionResultService(
            DifferentialExpressionResultService differentialExpressionResultService ) {
        this.differentialExpressionResultService = differentialExpressionResultService;
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) {

        try {
            File f = getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }
            log.info( "Creating new expression data file: " + f.getName() );
            ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

            Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
            Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );
            writeMatrix( f, geneAnnotations, matrix );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     * 
     * @param type
     * @param forceWrite To not return the existing file, but create it anew.
     * @return location of the resulting file.
     */
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
            Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );

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

    /**
     * Locate or create an experimental design file for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        String filename = ee.getId() + "_" + ee.getShortName().replaceAll( "\\s+", "_" ) + "_expdesign"
                + DATA_FILE_SUFFIX;
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

    /**
     * @param ee
     * @param forceWrite
     * @param filtered if the data should be filtered.
     * @see ExpressionDataMatrixService.getFilteredMatrix
     * @return
     */
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

    /**
     * @param type
     * @param forceWrite
     */
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
     * Locate or create the differential expression data file for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDiffExpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        String filename = getDiffExFileName( ee );
        try {
            File f = getOutputFile( filename );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new Differential Expression data file: " + f.getName() );
            writeDiffExpressionData( f, ee );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param analysis
     * @param forceRewrite
     * @return
     */
    public File writeOrLocateDiffExpressionDataFile( DifferentialExpressionAnalysis analysis, boolean forceRewrite ) {
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    /**
     * @param ee
     * @return
     */
    private String getDiffExFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + ee.getShortName().replaceAll( "[\\s\\/]+", "_" ) + "_diffExp" + DATA_FILE_SUFFIX;
    }

    /**
     * Delete the differential expression file for the given experiment
     * 
     * @param ee
     */
    public void deleteDiffExFile( ExpressionExperiment ee ) {
        File f = getOutputFile( getDiffExFileName( ee ) );
        if ( f.exists() ) {
            if ( f.delete() ) {
                log.info( "Deleted: " + f );
            } else {
                log.info( "Failed to delete: " + f );
            }
        }
    }

    /**
     * Write or located the coexpression data file for a given experiment
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateCoexpressionDataFile( ExpressionExperiment ee, boolean forceWrite ) {

        ee = expressionExperimentService.thawLite( ee );

        String filename = ee.getId() + "_" + ee.getShortName().replaceAll( "[\\s\\/]+", "_" ) + "_coExp"
                + DATA_FILE_SUFFIX;
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

    /**
     * Loads the probe to probe coexpression link information for a given expression experiment and writes it to disk.
     * 
     * @param file
     * @param ee
     * @throws IOException
     */
    private void writeCoexpressionData( File file, ExpressionExperiment ee ) throws IOException {

        Taxon tax = expressionExperimentService.getTaxon( ee.getId() );
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
            log.info( "Micro Array Annotation File Missing for Experiment, unable to include annotation information" );
            buf.append( "# The micro array annotation file is missing for this Experiment, unable to include gene annotation information \n" );
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
     * Loads the differential expression Data for the experiment from the DB and writes it to disk.
     * 
     * @param file
     * @param ee
     * @throws IOException
     */
    private void writeDiffExpressionData( File file, ExpressionExperiment ee ) throws IOException {

        Collection<ExpressionAnalysisResultSet> results = differentialExpressionAnalysisService.getResultSets( ee );

        if ( results == null || results.isEmpty() ) {
            log.warn( "No differential expression results found for " + ee );
            return;
        }

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<Long, String[]> geneAnnotations = this.getGeneAnnotationsAsStrings( arrayDesigns );

        // Write header
        StringBuilder buf = new StringBuilder();
        buf.append( makeDiffExpressionFileHeader( ee, geneAnnotations ) );

        analysisResultSetsToString( results, geneAnnotations, buf );

        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        writer.write( buf.toString() );
        writer.flush();
        writer.close();
    }

    /**
     * @param results
     * @param geneAnnotations
     * @param buf
     */
    public void analysisResultSetsToString( Collection<ExpressionAnalysisResultSet> results,
            Map<Long, String[]> geneAnnotations, StringBuilder buf ) {
        Map<Long, StringBuilder> probe2String = new HashMap<Long, StringBuilder>();

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

            if ( sortedResult instanceof ProbeAnalysisResult ) {
                CompositeSequence cs = ( ( ProbeAnalysisResult ) sortedResult ).getProbe();
                StringBuilder sb = probe2String.get( cs.getId() );
                if ( sb == null ) {
                    log.warn( "Unable to find probe " + cs.getId() + " in map" );
                    break;
                }
                buf.append( sb );
                buf.append( "\n" );
            }
        }
    }

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
            List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults ) {
        differentialExpressionResultService.thaw( ears );

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

            if ( dear instanceof ProbeAnalysisResult ) {
                CompositeSequence cs = ( ( ProbeAnalysisResult ) dear ).getProbe();

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
                String formattedP = correctedPvalue == null ? "" : String.format( DECIMAL_FORMAT, pvalue );
                probeBuffer.append( "\t" + formattedCP + "\t" + formattedP );
            } else {
                log.warn( "probe details missing.  Unable to retrieve probe level information. Skipping  "
                        + dear.getClass() + " with id: " + dear.getId() );
            }

        } // ears.getResults loop
        return sortedFirstColumnOfResults;
    }

    /**
     * @param probeAnalysisResult
     * @return
     */
    private String formatDiffExResult( ExpressionExperiment ee, ProbeAnalysisResult probeAnalysisResult,
            String factorName, String factorURI, String baselineDescription ) {

        CompositeSequence cs = probeAnalysisResult.getProbe();

        Collection<ContrastResult> contrasts = probeAnalysisResult.getContrasts();

        StringBuilder buf = new StringBuilder();
        for ( ContrastResult cr : contrasts ) {
            FactorValue factorValue = cr.getFactorValue();

            String direction = cr.getLogFoldChange() < 0 ? "-" : "+";

            String factorValueDescription = ExperimentalDesignUtils.prettyString( factorValue );

            // FIXME
        }

        return buf.toString();
    }

    /**
     * @param ee
     * @param geneAnnotations
     * @param buf
     * @return header string
     */
    private String makeDiffExpressionFileHeader( ExpressionExperiment ee, Map<Long, String[]> geneAnnotations ) {
        StringBuilder buf = new StringBuilder();
        Date timestamp = new Date( System.currentTimeMillis() );
        buf.append( "# Differential Expression Data for:  " + ee.getShortName() + " : " + ee.getName() + " \n" );
        buf.append( "# " + timestamp + " \n" );
        buf.append( DISCLAIMER );

        // Different Headers if Gene Annotations missing.
        if ( geneAnnotations.isEmpty() ) {
            log.info( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf.append( "# The annotation file is missing for this Experiment, unable to include gene annotation information \n" );
            buf.append( "Probe_Name" );
        } else {
            buf.append( "Probe_Name\tGene_Symbol\tGene_Name" );// column information

            if ( geneAnnotations.values().iterator().next().length > 4 ) {
                buf.append( "\tNCBI_ID" ); // leaving out the Gemma ID.
            }
            // buf.append("\n"); // FIXME this wasn't here before, do we need it?
        }

        return buf.toString();
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
     * @param ads
     * @return
     */
    private Map<Long, Collection<Gene>> getGeneAnnotations( Collection<ArrayDesign> ads ) {
        Map<Long, Collection<Gene>> annots = new HashMap<Long, Collection<Gene>>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesign = arrayDesignService.thaw( arrayDesign );
            annots.putAll( ArrayDesignAnnotationService.readAnnotationFile( arrayDesign ) );
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
            annots.putAll( ArrayDesignAnnotationService.readAnnotationFileAsString( arrayDesign ) );
        }
        return annots;
    }

    /**
     * @param type
     * @return
     * @throws IOException
     */
    private File getJSONOutputFile( QuantitationType type ) throws IOException {
        String filename = type.getName().replaceAll( "\\s+", "_" ) + JSON_FILE_SUFFIX;
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
    @SuppressWarnings("unchecked")
    private void writeJson( File file, Map<Long, Collection<Gene>> geneAnnotations,
            ExpressionDataMatrix expressionDataMatrix ) throws IOException, FileNotFoundException {
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
    @SuppressWarnings("unchecked")
    private void writeJson( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors ) throws IOException {
        designElementDataVectorService.thaw( vectors );
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
    @SuppressWarnings("unchecked")
    private void writeMatrix( File file, Map<Long, String[]> geneAnnotations, ExpressionDataMatrix expressionDataMatrix )
            throws IOException, FileNotFoundException {

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
            Collection<? extends DesignElementDataVector> vectors, Map<Long, String[]> geneAnnotations )
            throws IOException {
        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrix<?> expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );

        writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

}
