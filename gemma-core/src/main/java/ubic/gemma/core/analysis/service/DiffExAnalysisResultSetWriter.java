package ubic.gemma.core.analysis.service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.datastructure.matrix.io.TsvUtils;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.DifferentialExpressionAnalysisResultComparator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.format;
import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.formatComment;

/**
 * Writer for {@link DifferentialExpressionAnalysis}.
 * @author paul
 * @author poirigui
 */
@CommonsLog
public class DiffExAnalysisResultSetWriter {

    /**
     * Write a {@link DifferentialExpressionAnalysis} as a ZIP archive containing entries for the analysis and each
     * result set.
     * @param analysis                    the analysis to write
     * @param geneAnnotations             gene annotations include in each row
     * @param config                      configuration used for the DE analysis
     * @param hasSignificantBatchConfound whether a significant batch confound was detected, a warning will be included
     *                                    in the file if it is the case
     * @param stream                      a stream where to write the resulting ZIP archive
     */
    public void write( DifferentialExpressionAnalysis analysis, Map<CompositeSequence, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config, boolean hasSignificantBatchConfound, OutputStream stream ) throws IOException {
        Date timestamp = new Date( System.currentTimeMillis() );
        try ( ZipOutputStream zipOut = new ZipOutputStream( stream ) ) {

            // top-level analysis results - ANOVA-style
            zipOut.putNextEntry( new ZipEntry( "analysis.results.txt" ) );
            writeDiffExpressionAnalysisData( analysis, geneAnnotations, config, timestamp, new OutputStreamWriter( zipOut, StandardCharsets.UTF_8 ) );
            zipOut.closeEntry();

            // Add a file for each result set with contrasts information.
            int i = 0;
            for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                if ( resultSet.getExperimentalFactors().size() > 1 ) {
                    // Skip interactions.
                    log.info( "Result file for interaction is omitted" ); // Why?
                    continue;
                }

                if ( resultSet.getId() == null ) { // -nodb option on analysis
                    zipOut.putNextEntry( new ZipEntry( "resultset_" + ++i + "of" + analysis.getResultSets().size()
                            + ".data.txt" ) ); // to make it clearer this is not an ID
                } else {
                    zipOut.putNextEntry( new ZipEntry( "resultset_ID" + resultSet.getId() + ".data.txt" ) );
                }
                writeDiffExpressionResultSetData( resultSet, geneAnnotations, config, hasSignificantBatchConfound, timestamp, new OutputStreamWriter( zipOut, StandardCharsets.UTF_8 ) );
                zipOut.closeEntry();
            }
        }
    }

    /**
     * Given diff exp analysis and gene annotation generate header and tab delimited data. The output is qValue....
     *
     * @param analysis (might not be persistent)
     */
    private void writeDiffExpressionAnalysisData( DifferentialExpressionAnalysis analysis, Map<CompositeSequence, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config, Date timestamp, Writer writer ) throws IOException {
        Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();
        if ( results == null || results.isEmpty() ) {
            log.warn( "No differential expression results found for " + analysis );
            return;
        }
        makeDiffExpressionFileHeader( analysis, analysis.getResultSets(), geneAnnotations, config, timestamp, writer );
        writeAnalysisResultSets( results, geneAnnotations, writer );
    }

    private void makeDiffExpressionFileHeader( DifferentialExpressionAnalysis analysis, Collection<ExpressionAnalysisResultSet> resultSets, Map<CompositeSequence, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config, Date timestamp, Writer buf ) throws IOException {
        BioAssaySet bas = analysis.getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        buf
                .append( "# Differential expression analysis for:  " ).append( format( ee.getShortName() ) ).append( " : " ).append( format( ee.getName() ) ).append( " (ID=" ).append( format( ee.getId() ) ).append( ")\n" )
                .append( "# This file contains summary statistics for the factors included in the analysis (e.g. ANOVA effects); details of contrasts are in separate files.\n" );

        // It might not be a persistent analysis.
        if ( analysis.getId() != null ) {
            buf.append( "# Analysis ID = " ).append( format( analysis.getId() ) ).append( "\n" );
        } else {
            buf.append( "# Analysis was not persisted to the database\n" );
        }

        if ( config != null ) {
            buf.append( formatComment( config.toString() ) );
        } else if ( analysis.getProtocol() != null && StringUtils.isNotBlank( analysis.getProtocol().getDescription() ) ) {
            buf.append( formatComment( analysis.getProtocol().getDescription() ) );
        } else {
            // This can happen if we are re-writing files for a stored analysis that didn't get proper protocol information saved.
            // Basically this is here for backwards compatibility.
            log.warn( "No configuration or protocol available, adding available analysis information to header" );
            buf.append( "# Configuration information was not fully available\n" );
            buf.append( "# Factors:\n" );

            if ( analysis.getSubsetFactorValue() != null ) {
                buf.append( "# Subset ID=" ).append( format( bas.getId() ) ).append( "\n" );
                buf.append( "# Subset factor " ).append( format( analysis.getSubsetFactorValue().getExperimentalFactor() ) ).append( "\n" );
                buf.append( "# Subset is of samples with " ).append( format( analysis.getSubsetFactorValue() ) ).append( "\n" );
            }

            for ( ExpressionAnalysisResultSet rs : resultSets ) {
                buf.append( "# " ).append( format( StringUtils.join( rs.getExperimentalFactors(), ":" ) ) ).append( "\n" );
            }
        }

        buf.append( "# Generated by Gemma " ).append( format( timestamp ) ).append( "\n" );

        buf.append( Arrays.stream( TsvUtils.GEMMA_CITATION_NOTICE ).map( line -> "# " + line + "\n" ).collect( Collectors.joining() ) );

        // Different Headers if Gene Annotations missing.
        if ( geneAnnotations.isEmpty() ) {
            //   log.info( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf
                    .append( "#\n" )
                    .append( "# The gene annotations were not available\n" );
            // but leave the blank columns there to make parsing easier.
        }
        buf.append( "Element_Name\tGene_Symbol\tGene_Name\tNCBI_ID" );// column information

        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.
    }

    private void writeAnalysisResultSets( Collection<ExpressionAnalysisResultSet> results, Map<CompositeSequence, String[]> geneAnnotations, Writer buf ) throws IOException {
        Map<CompositeSequence, StringBuilder> probe2String = new HashMap<>();
        List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults = null;

        for ( ExpressionAnalysisResultSet ears : results ) {
            sortedFirstColumnOfResults = this
                    .writeAnalysisResultSet( ears, geneAnnotations, probe2String, sortedFirstColumnOfResults, buf );

        } // ears loop

        buf.append( "\n" );

        if ( sortedFirstColumnOfResults == null ) {
            throw new IllegalStateException( "No results for " );
        }

        // Dump the probe data in the sorted order of the 1st column that we originally sorted
        for ( DifferentialExpressionAnalysisResult sortedResult : sortedFirstColumnOfResults ) {

            CompositeSequence cs = sortedResult.getProbe();
            StringBuilder sb = probe2String.get( cs );
            if ( sb == null ) {
                log.warn( "Unable to find element " + cs + " in map" );
                break;
            }
            buf.append( sb );
            buf.append( "\n" );

        }
    }

    private List<DifferentialExpressionAnalysisResult> writeAnalysisResultSet( ExpressionAnalysisResultSet ears, Map<CompositeSequence, String[]> geneAnnotations,
            Map<CompositeSequence, StringBuilder> probe2String, @Nullable List<DifferentialExpressionAnalysisResult> sortedFirstColumnOfResults, Writer buf ) throws IOException {

        if ( sortedFirstColumnOfResults == null ) { // Sort P values in ears (because 1st column)
            sortedFirstColumnOfResults = new ArrayList<>( ears.getResults() );
            sortedFirstColumnOfResults.sort( DifferentialExpressionAnalysisResultComparator.Factory.newInstance() );
        }

        // Generate a description of the factors involved "factor1_factor2", trying to be R-friendly
        StringBuilder factorColumnNameBuilder = new StringBuilder();
        for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
            factorColumnNameBuilder.append( ef.getName().replaceAll( "\\s+", "_" ) ).append( "_" );
        }
        String factorColumnName = StringUtil.makeValidForR( StringUtils.removeEnd( factorColumnNameBuilder.toString(), "_" ) );

        // Generate headers
        buf.append( "\tQValue_" ).append( factorColumnName );
        buf.append( "\tPValue_" ).append( factorColumnName );

        // Generate probe details
        for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {
            StringBuilder probeBuffer = new StringBuilder();

            CompositeSequence cs = dear.getProbe();

            // Make a hashMap so we can organize the data by probe with factors as columns
            // Need to cache the information until we have it organized in the correct format to write
            if ( probe2String.containsKey( cs ) ) {
                probeBuffer = probe2String.get( cs );
            } else {// no entry for probe yet
                probeBuffer.append( format( cs.getName() ) );
                if ( geneAnnotations.containsKey( cs ) ) {
                    String[] annotationStrings = geneAnnotations.get( cs );
                    /*
                     * Fields:
                     *
                     * 1: gene symbols
                     * 2: gene name
                     * 4: ncbi ID
                     */
                    probeBuffer
                            .append( "\t" ).append( format( annotationStrings[1] ) )
                            .append( "\t" ).append( format( annotationStrings[2] ) )
                            .append( "\t" ).append( format( annotationStrings[4] ) );
                } else {
                    probeBuffer.append( "\t\t\t" );
                }

                probe2String.put( cs, probeBuffer );
            }

            Double correctedPvalue = dear.getCorrectedPvalue();
            Double pvalue = dear.getPvalue();

            probeBuffer
                    .append( "\t" ).append( format( correctedPvalue ) )
                    .append( "\t" ).append( format( pvalue ) );
        }

        return sortedFirstColumnOfResults;
    }

    /**
     * Given result set and gene annotation generate header and tab delimited data. The output is foldChange and pValue
     * associated with each contrast.
     * eneAnnotations
     */
    private void writeDiffExpressionResultSetData( ExpressionAnalysisResultSet resultSet, Map<CompositeSequence, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config, boolean hasSignificantBatchConfound, Date timestamp, Writer writer ) throws IOException {
        this.writeDiffExpressionResultSetFileHeader( resultSet, geneAnnotations, config, hasSignificantBatchConfound, timestamp, writer );
        this.writeAnalysisResultSetWithContrasts( resultSet, geneAnnotations, writer );
    }

    private void writeDiffExpressionResultSetFileHeader( ExpressionAnalysisResultSet resultSet, Map<CompositeSequence, String[]> geneAnnotations,
            @Nullable DifferentialExpressionAnalysisConfig config, boolean hasSignificantBatchConfound, Date timestamp, Writer buf ) throws IOException {
        BioAssaySet bas = resultSet.getAnalysis().getExperimentAnalyzed();

        ExpressionExperiment ee = experimentForBioAssaySet( bas );

        buf.append( "# Differential expression result set for:  " ).append( format( ee.getShortName() ) ).append( " : " )
                .append( format( ee.getName() ) ).append( " (ID=" ).append( format( ee.getId() ) ).append( ")\n" );
        buf.append( "# This file contains contrasts for:" ).append( format( StringUtils.join( resultSet.getExperimentalFactors(), " x " ) ) ).append( "\n" );

        if ( resultSet.getAnalysis().getId() == null ) {
            buf.append( "# Analysis is not stored in the database\n" );
        } else {
            buf.append( "# Analysis ID = " ).append( format( resultSet.getAnalysis().getId() ) ).append( "\n" );
        }

        if ( resultSet.getId() != null ) {
            buf.append( "# ResultSet ID = " ).append( format( resultSet.getId() ) ).append( "\n" );
        }

        /*
         * Use the config if available; otherwise the protocol description
         * (which currently is same as config.toString() anyway; fall back on "by-hand", which we can probably get rid
         * of
         * later and always use the config (for new analyses) or stored protocol (for stored analyses)
         */
        buf.append( "# Analysis configuration:\n" );
        if ( config != null ) {
            buf.append( formatComment( config.toString() ) );
        } else if ( resultSet.getAnalysis().getProtocol() != null && StringUtils
                .isNotBlank( resultSet.getAnalysis().getProtocol().getDescription() ) ) {
            buf.append( formatComment( resultSet.getAnalysis().getProtocol().getDescription() ) );
        } else {
            log.warn( "Full configuration not available, adding available analysis information to header" );
            if ( resultSet.getAnalysis().getSubsetFactorValue() != null ) {
                buf.append( "# This analysis is for subset ID=" ).append( format( bas.getId() ) ).append( "\n" );
                buf.append( "# The subsetting factor was " )
                        .append( format( resultSet.getAnalysis().getSubsetFactorValue().getExperimentalFactor() ) )
                        .append( "\n" );
                buf.append( "# This subset is of samples with " )
                        .append( format( resultSet.getAnalysis().getSubsetFactorValue() ) ).append( "\n" );
            }
        }

        if ( hasSignificantBatchConfound ) {
            buf.append( "# !!! Warning, this dataset has a batch confound with the factors analysed\n" );
        }

        buf
                .append( "#\n" )
                .append( "# Generated by Gemma " ).append( format( timestamp ) ).append( " \n" );
        buf.append( Arrays.stream( TsvUtils.GEMMA_CITATION_NOTICE ).map( line -> "# " + line + "\n" ).collect( Collectors.joining() ) ).append( "#\n" );

        if ( geneAnnotations.isEmpty() ) {
            // log.debug( "Annotation file is missing for this experiment, unable to include gene annotation information" );
            buf
                    .append( "# The platform annotation file is missing for this Experiment, gene annotation information is omitted\n" )
                    .append( "#\n" );
            // but leave the blank columns there to make parsing easier.
        }
        buf.append( "Element_Name\tGene_Symbol\tGene_Name\tNCBI_ID" );// column information
        // Note we don't put a newline here, because the rest of the headers have to be added for the pvalue columns.
    }

    private void writeAnalysisResultSetWithContrasts( ExpressionAnalysisResultSet resultSet,
            Map<CompositeSequence, String[]> geneAnnotations, Writer buf ) throws IOException {
        Assert.isTrue( resultSet.getExperimentalFactors().size() == 1,
                resultSet + " should have exactly one experimental factor." );
        ExperimentalFactor ef = resultSet.getExperimentalFactors().iterator().next();

        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

            buf
                    .append( "\tCoefficient_" ).append( StringUtil.makeValidForR( ef.getName() ) )
                    .append( "\tPValue_" ).append( StringUtil.makeValidForR( ef.getName() ) )
                    .append( "\n" );

            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
//                if ( geneAnnotations.isEmpty() ) {
//                    rowBuffer.append( dear.getProbe().getName() );
//                } else {
                this.writeGeneAnnotations( dear, geneAnnotations, buf );
                // }

                /*
                If there are no results for the DEAR then we wouldn't expect contrasts, so we just leave a blank.
                 */
                if ( dear.getPvalue() == null ) {
                    buf.append( "\t\t\n" );
                    continue;
                }

                if ( dear.getContrasts().size() != 1 ) {
                    //
                    throw new IllegalStateException( "Expected exactly one contrast for continuous factor" );
                }

                ContrastResult contrast = dear.getContrasts().iterator().next();
                Double coefficient = contrast.getCoefficient();
                Double pValue = contrast.getPvalue();
                buf.append( "\t" ).append( format( coefficient ) ).append( "\t" ).append( format( pValue ) ).append( "\n" );
            }

        } else {
            FactorValue baseline = resultSet.getBaselineGroup();
            List<FactorValue> factorValueOrder = new ArrayList<>();

            /*
             * First find out what factor values are relevant in case this is a subsetted analysis. With this we
             * probably not worry about the baselineId since it won't be here.
             */
            Collection<FactorValue> usedFactorValues = new HashSet<>();
            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                for ( ContrastResult contrast : dear.getContrasts() ) {
                    if ( contrast.getFactorValue() != null ) {
                        usedFactorValues.add( contrast.getFactorValue() );
                    }
                }
                break; // only have to look at one.
            }

            for ( FactorValue factorValue : ef.getFactorValues() ) {

                /*
                 * deal correctly with subset situations - only use factor values relevant to the subset
                 */
                if ( Objects.equals( factorValue, baseline ) || !usedFactorValues.contains( factorValue ) ) {
                    continue;
                }
                factorValueOrder.add( factorValue );
                // Generate column headers, try to be R-friendly
                String colSuffix = this.getFactorValueColumnNameSuffix( factorValue );
                buf.append( "\tFoldChange_" ).append( colSuffix );
                buf.append( "\tTstat_" ).append( colSuffix );
                buf.append( "\tPValue_" ).append( colSuffix );
            }

            buf.append( '\n' );

            // Generate element details
            for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
                this.writeGeneAnnotations( dear, geneAnnotations, buf );

                Map<FactorValue, String> factorValueToData = new HashMap<>();
                // I don't think we can expect them in the same order.
                for ( ContrastResult contrast : dear.getContrasts() ) {
                    Double foldChange = contrast.getLogFoldChange();
                    Double pValue = contrast.getPvalue();
                    Double tStat = contrast.getTstat();
                    String contrastData = "\t" + format( foldChange ) + "\t" + format( tStat ) + "\t" + format( pValue );
                    assert contrast.getFactorValue() != null;
                    factorValueToData.put( contrast.getFactorValue(), contrastData );
                }

                // Get them in the right order.
                for ( FactorValue factorValue : factorValueOrder ) {
                    buf.append( factorValueToData.getOrDefault( factorValue, "\t\t\t" ) );
                }

                buf.append( '\n' );

            } // resultSet.getResults() loop
        }
    }

    private void writeGeneAnnotations( DifferentialExpressionAnalysisResult dear, Map<CompositeSequence, String[]> geneAnnotations, Writer writer ) throws IOException {
        CompositeSequence cs = dear.getProbe();
        writer.append( format( cs.getName() ) );
        String[] annotationStrings = geneAnnotations.get( cs );
        if ( annotationStrings != null ) {
            writer
                    .append( "\t" ).append( format( annotationStrings[1] ) )
                    .append( "\t" ).append( format( annotationStrings[2] ) )
                    // leaving out Gemma ID, which is annotationStrings[3]
                    // ncbi id, if we have it.
                    .append( "\t" ).append( annotationStrings.length > 4 ? format( annotationStrings[4] ) : "" );
        } else {
            writer.append( "\t\t\t" );
        }
    }

    @SuppressWarnings("deprecation")
    private String getFactorValueColumnNameSuffix( FactorValue fv ) {
        String result;
        if ( fv.getCharacteristics() != null && !fv.getCharacteristics().isEmpty() ) {
            result = fv.getCharacteristics().stream()
                    .map( Statement::getSubject )
                    .collect( Collectors.joining( "_" ) );
        } else if ( fv.getMeasurement() != null && StringUtils.isNotBlank( fv.getMeasurement().getValue() ) ) {
            result = fv.getMeasurement().getValue();
        } else if ( StringUtils.isNotBlank( fv.getValue() ) ) {
            result = fv.getValue();
        } else {
            log.warn( "No suitable column suffix found for " + fv + ", will use its ID in the file." );
            result = format( fv.getId() );
        }
        // R-friendly, but no need to add "X" to the beginning since this is a suffix.
        return result.replaceAll( "\\W+", "." );
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
}
