package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpressionAnalysisResultSetFileServiceImpl extends AbstractTsvFileService<ExpressionAnalysisResultSet> implements ExpressionAnalysisResultSetFileService {

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Map<DifferentialExpressionAnalysisResult, List<Gene>> result2Genes, Appendable appendable ) throws IOException {
        String experimentalFactorNames = analysisResultSet.getExperimentalFactors().stream()
                .map( ExperimentalFactor::getName )
                .collect( Collectors.joining( ", " ) );
        CSVPrinter printer = getTsvFormatBuilder( "Experimental factors: " + experimentalFactorNames )
                .setHeader( "id", "probe_id", "probe_name", "gene_id", "gene_name", "gene_ncbi_id", "gene_official_symbol", "pvalue", "corrected_pvalue", "rank" )
                .build()
                .print( appendable );
        for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
            final List<Gene> genes = result2Genes.getOrDefault( analysisResult, Collections.emptyList() );
            printer.printRecord(
                    analysisResult.getId(),
                    analysisResult.getProbe().getId(),
                    analysisResult.getProbe().getName(),
                    genes.stream().map( Gene::getId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getName ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getNcbiGeneId ).map( String::valueOf ).collect( Collectors.joining( getSubDelimiter() ) ),
                    genes.stream().map( Gene::getOfficialSymbol ).collect( Collectors.joining( getSubDelimiter() ) ),
                    format( analysisResult.getPvalue() ),
                    format( analysisResult.getCorrectedPvalue() ),
                    format( analysisResult.getRank() ) );
        }
    }

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet entity, Appendable appendable ) throws IOException {
        writeTsvToAppendable( entity, Collections.emptyMap(), appendable );
    }
}
