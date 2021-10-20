package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpressionAnalysisResultSetFileServiceImpl extends AbstractTsvFileService<ExpressionAnalysisResultSet> implements ExpressionAnalysisResultSetFileService {

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Map<DifferentialExpressionAnalysisResult, List<Gene>> result2Genes, Appendable appendable ) throws IOException {
        String experimentalFactorsMetadata = "[" + analysisResultSet.getExperimentalFactors().stream()
                .map( this::formatExperimentalFactor )
                .collect( Collectors.joining( ", " ) ) + "]";

        List<String> header = new ArrayList<>();

        // add the basic columns
        header.addAll( Arrays.asList( "id", "probe_id", "probe_name", "gene_id", "gene_name", "gene_ncbi_id", "gene_official_symbol", "pvalue", "corrected_pvalue", "rank" ) );

        // we need to peek in the contrast result to understand factor value interactions
        // i.e. interaction between genotype and timepoint might result in a contrast_male_3h column, although we would
        // use factor value IDs in the actual column name which might result in something like contrast_1292_2938
        final Collection<ContrastResult> firstContrastResults = analysisResultSet.getResults().stream()
                .findFirst()
                .map( DifferentialExpressionAnalysisResult::getContrasts )
                .orElse( Collections.emptyList() );

        // this is the order the factor values are displayed
        Comparator<ContrastResult> contrastResultComparator = Comparator
                .comparing( ContrastResult::getFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) )
                .thenComparing( ContrastResult::getSecondFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) );

        firstContrastResults.stream().sorted( contrastResultComparator ).forEachOrdered( contrastResult -> {
            String contrastResultPrefix = "contrast"
                    + ( contrastResult.getFactorValue() != null ? "_" + contrastResult.getFactorValue().getId() : "" )
                    + ( contrastResult.getSecondFactorValue() != null ? "_" + contrastResult.getSecondFactorValue().getId() : "" );
            header.addAll( Arrays.asList(
                    contrastResultPrefix + "_log2fc",
                    contrastResultPrefix + "_tstat",
                    contrastResultPrefix + "_pvalue" ) );
        } );

        CSVPrinter printer = getTsvFormatBuilder( "Experimental factors: " + experimentalFactorsMetadata )
                .setHeader( header.toArray( new String[header.size()] ) )
                .build()
                .print( appendable );
        for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
            final List<Object> record = new ArrayList<>();
            final List<Gene> genes = result2Genes.getOrDefault( analysisResult, Collections.emptyList() );
            record.addAll( Arrays.asList( analysisResult.getId(),
                    analysisResult.getProbe().getId(),
                    analysisResult.getProbe().getName(),
                    genes.stream().map( Gene::getId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getName ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getNcbiGeneId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getOfficialSymbol ).collect( Collectors.joining( getSubDelimiter() ) ),
                    format( analysisResult.getPvalue() ),
                    format( analysisResult.getCorrectedPvalue() ),
                    format( analysisResult.getRank() ) ) );
            analysisResult.getContrasts().stream().sorted( contrastResultComparator ).forEachOrdered( contrastResult -> {
                record.addAll( Arrays.asList( format( contrastResult.getLogFoldChange() ), format( contrastResult.getTstat() ), format( contrastResult.getPvalue() ) ) );
            } );
            printer.printRecord( record );
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
        return measurement.getValue() + measurement.getUnit().getUnitNameCV();
    }

    private String formatCharacteristics( Collection<Characteristic> characteristics ) {
        return characteristics.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) );
    }

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet entity, Appendable appendable ) throws IOException {
        writeTsvToAppendable( entity, Collections.emptyMap(), appendable );
    }
}
