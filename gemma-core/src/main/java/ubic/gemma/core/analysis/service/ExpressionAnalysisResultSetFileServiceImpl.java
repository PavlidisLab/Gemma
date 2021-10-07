package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpressionAnalysisResultSetFileServiceImpl extends AbstractTsvFileService<ExpressionAnalysisResultSet> implements ExpressionAnalysisResultSetFileService {

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Map<DifferentialExpressionAnalysisResult, List<Gene>> result2Genes, Appendable appendable ) throws IOException {
        Collection<ExperimentalFactor> experimentalFactors = analysisResultSet.getExperimentalFactors();

        // TODO: have a structured format for header metadata
        String experimentalFactorsMetadata = analysisResultSet.getExperimentalFactors().stream()
                .map( this::formatExperimentalFactor )
                .collect( Collectors.joining( ", " ) );

        List<String> header = new ArrayList<>();

        // add the basic columns
        header.addAll( Arrays.asList( "id", "probe_id", "probe_name", "gene_id", "gene_name", "gene_ncbi_id", "gene_official_symbol", "pvalue", "corrected_pvalue", "rank" ) );

        // add all the contrasts present among the results
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            for ( FactorValue fv : experimentalFactor.getFactorValues() ) {
                header.addAll( Arrays.asList(
                        "contrast_" + experimentalFactor.getName() + "_" + fv.getValue() + "_log2fc",
                        "contrast_" + experimentalFactor.getName() + "_" + fv.getValue() + "_tstat",
                        "contrast_" + experimentalFactor.getName() + "_" + fv.getValue() + "_pvalue" ) );
            }
        }

        CSVPrinter printer = getTsvFormatBuilder( "Experimental factors: " + experimentalFactorsMetadata )
                .setHeader( header.toArray( new String[header.size()] ) )
                .build()
                .print( appendable );
        for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
            final List<Object> record = new ArrayList<>();
            final List<Gene> genes = result2Genes.getOrDefault( analysisResult, Collections.emptyList() );
            final Map<FactorValue, ContrastResult> factorValue2Contrast = analysisResult.getContrasts()
                    .stream()
                    .collect( Collectors.toMap( ContrastResult::getFactorValue, Function.identity() ) );
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
            // add contrasts columns
            for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
                for ( FactorValue fv : experimentalFactor.getFactorValues() ) {
                    if ( factorValue2Contrast.containsKey( fv ) ) {
                        ContrastResult contrastResult = factorValue2Contrast.get( fv );
                        record.addAll( Arrays.asList( format( contrastResult.getLogFoldChange() ), format( contrastResult.getTstat() ), format( contrastResult.getPvalue() ) ) );
                    } else {
                        // the contrast might not be present if configured to
                        record.addAll( Arrays.asList( null, null, null ) );
                    }
                }
            }
            printer.printRecord( record );
        }

    }

    private String formatExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        return experimentalFactor.getName() + " (" +
                experimentalFactor.getFactorValues()
                        .stream()
                        .map( this::formatFactorValue )
                        .collect( Collectors.joining( ", " ) ) + ")";
    }

    private String formatFactorValue( FactorValue factorValue ) {
        return factorValue.getValue() + ( factorValue.getIsBaseline() != null && factorValue.getIsBaseline() ? "*" : "" );
    }

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet entity, Appendable appendable ) throws IOException {
        writeTsvToAppendable( entity, Collections.emptyMap(), appendable );
    }
}
