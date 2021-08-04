package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpressionAnalysisResultSetFileServiceImpl extends AbstractTsvFileService<ExpressionAnalysisResultSet> implements ExpressionAnalysisResultSetFileService {

    @Override
    public void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Appendable appendable ) throws IOException {
        String experimentalFactorNames = analysisResultSet.getExperimentalFactors().stream()
                .map( ExperimentalFactor::getName )
                .collect( Collectors.joining( ", " ) );
        CSVPrinter printer = getCSVFormat()
                .withHeaderComments( " Experimental factors: " + experimentalFactorNames )
                .withHeader( "id", "probe_name", "probe_biological_characteristic_name", "probe_biological_characteristic_sequence_database_entry_accession", "pvalue", "corrected_pvalue", "rank" )
                .print( appendable );
        for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
            printer.printRecord(
                    analysisResult.getId(),
                    analysisResult.getProbe().getName(),
                    analysisResult.getProbe().getBiologicalCharacteristic().getName(),
                    Optional.ofNullable( analysisResult.getProbe().getBiologicalCharacteristic().getSequenceDatabaseEntry() ).map( DatabaseEntry::getAccession ).orElse( null ),
                    format( analysisResult.getPvalue() ),
                    format( analysisResult.getCorrectedPvalue() ),
                    format( analysisResult.getRank() ) );
        }
    }
}
