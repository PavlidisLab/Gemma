package ubic.gemma.core.analysis.service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service
@CommonsLog
public class ExpressionAnalysisResultSetFileServiceImpl extends AbstractFileService<ExpressionAnalysisResultSet> implements ExpressionAnalysisResultSetFileService {

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Map<Long, List<Gene>> resultId2Genes, Writer appendable ) throws IOException {
        String experimentalFactorsMetadata = "[" + analysisResultSet.getExperimentalFactors().stream()
                .map( this::formatExperimentalFactor )
                .collect( Collectors.joining( ", " ) ) + "]";

        // add the basic columns
        List<String> header = new ArrayList<>( Arrays.asList( "id", "probe_id", "probe_name", "gene_id", "gene_name", "gene_ncbi_id", "gene_official_symbol", "gene_official_name", "pvalue", "corrected_pvalue", "rank" ) );

        // this is the order the factor values are displayed
        Comparator<ContrastResult> contrastResultComparator = Comparator
                .comparing( ContrastResult::getFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) )
                .thenComparing( ContrastResult::getSecondFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) );

        // we need to peek in the contrast result to understand factor value interactions
        // i.e. interaction between genotype and time point might result in a contrast_male_3h column, although we would
        // use factor value IDs in the actual column name which might result in something like contrast_1292_2938
        final List<ContrastResult> firstContrastResults = analysisResultSet.getResults().stream()
                .findFirst()
                .map( DifferentialExpressionAnalysisResult::getContrasts )
                .orElse( Collections.emptySet() )
                .stream().sorted( contrastResultComparator )
                .collect( Collectors.toList() );

        for ( ContrastResult contrastResult : firstContrastResults ) {
            String contrastResultPrefix = "contrast"
                    + ( contrastResult.getFactorValue() != null ? "_" + contrastResult.getFactorValue().getId() : "" )
                    + ( contrastResult.getSecondFactorValue() != null ? "_" + contrastResult.getSecondFactorValue().getId() : "" );
            header.addAll( Arrays.asList(
                    contrastResultPrefix + "_log2fc",
                    contrastResultPrefix + "_tstat",
                    contrastResultPrefix + "_pvalue" ) );
        }

        try ( CSVPrinter printer = getTsvFormatBuilder( "Experimental factors: " + experimentalFactorsMetadata )
                .setHeader( header.toArray( new String[0] ) )
                .build()
                .print( appendable ) ) {
            for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
                final List<Gene> genes = resultId2Genes.getOrDefault( analysisResult.getId(), Collections.emptyList() );
                final List<Object> record = new ArrayList<>( Arrays.asList( analysisResult.getId(),
                        analysisResult.getProbe().getId(),
                        analysisResult.getProbe().getName(),
                        genes.stream().map( Gene::getId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                        genes.stream().map( Gene::getName ).collect( Collectors.joining( getSubDelimiter() ) ),
                        genes.stream().map( Gene::getNcbiGeneId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                        genes.stream().map( Gene::getOfficialSymbol ).collect( Collectors.joining( getSubDelimiter() ) ),
                        genes.stream().map( Gene::getOfficialName ).collect( Collectors.joining( getSubDelimiter() ) ),
                        format( analysisResult.getPvalue() ),
                        format( analysisResult.getCorrectedPvalue() ),
                        format( analysisResult.getRank() ) ) );
                Map<Pair<FactorValue, FactorValue>, ContrastResult> contrastsByFirstAndSecondFactorValue = analysisResult.getContrasts().stream()
                        .collect( Collectors.toMap( fv -> Pair.of( fv.getFactorValue(), fv.getSecondFactorValue() ), identity() ) );
                // render contrast results in the same order than the first row and handle possibly missing columns
                for ( ContrastResult contrastResult : firstContrastResults ) {
                    ContrastResult cr = contrastsByFirstAndSecondFactorValue.get( Pair.of( contrastResult.getFactorValue(), contrastResult.getSecondFactorValue() ) );
                    if ( cr != null ) {
                        record.add( format( cr.getLogFoldChange() ) );
                        record.add( format( cr.getTstat() ) );
                        record.add( format( cr.getPvalue() ) );
                    } else {
                        log.warn( String.format( "%s is missing contrast result for [%s, %s]. The corresponding column in the TSV will be treated as NaN.",
                                analysisResult, contrastResult.getFactorValue(), contrastResult.getSecondFactorValue() ) );
                        record.add( "" );
                        record.add( "" );
                        record.add( "" );
                    }
                }
                printer.printRecord( record );
            }
        }
    }

    private String formatExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        return "name: " + experimentalFactor.getName() + ", values: [" +
                experimentalFactor.getFactorValues()
                        .stream()
                        .map( this::formatFactorValue )
                        .collect( Collectors.joining( ", " ) ) + "]";
    }


    private String formatFactorValue( FactorValue factorValue ) {
        return "id: " + factorValue.getId()
                + ( factorValue.getIsBaseline() != null && factorValue.getIsBaseline() ? "*" : "" )
                + ( factorValue.getMeasurement() != null ? ", measurement: " + formatMeasurement( factorValue.getMeasurement() ) : "" )
                + ", characteristics: [" + formatCharacteristics( factorValue.getCharacteristics() ) + "]";
    }

    private String formatMeasurement( Measurement measurement ) {
        return measurement.getValue()
                + ( measurement.getUnit() != null ? measurement.getUnit().getUnitNameCV() : "" );
    }

    private String formatCharacteristics( Collection<? extends Characteristic> characteristics ) {
        return characteristics.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) );
    }

    @Override
    public void writeTsv( ExpressionAnalysisResultSet entity, Writer writer ) throws IOException {
        writeTsvToAppendable( entity, Collections.emptyMap(), writer );
    }
}
