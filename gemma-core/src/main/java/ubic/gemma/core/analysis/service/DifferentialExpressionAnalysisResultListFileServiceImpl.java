package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.*;

@Service
public class DifferentialExpressionAnalysisResultListFileServiceImpl implements DifferentialExpressionAnalysisResultListFileService {

    @Override
    public void writeTsv( List<DifferentialExpressionAnalysisResult> entity, @Nullable Gene gene, @Nullable Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, @Nullable Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, @Nullable Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, Writer writer ) throws IOException {
        List<String> extraHeaderComments = new ArrayList<>();
        if ( gene != null ) {
            extraHeaderComments.add( "Results for " + gene );
        }
        extraHeaderComments.add( String.format( "The 'contrasts' column contains contrasts delimited by '%s'. Each contrast is structured as space-delimited key=value pairs. Factors are encoded by their factor value ID for the 'factor' key. Interaction of factors are encoded as 'id1:id2'. Continuous contrasts will use the '[continuous]' indicator.", SUB_DELIMITER ) );
        extraHeaderComments.add( "Baselines are encoded as a factor value ID. Baselines for interactions are encoded as 'id1:id2'." );
        List<String> header = new ArrayList<>();
        header.add( "id" );
        if ( sourceExperimentIdMap != null ) {
            header.add( "source_experiment_id" );
        }
        if ( sourceExperimentIdMap != null ) {
            header.add( "experiment_analyzed_id" );
        }
        header.add( "result_set_id" );
        header.add( "probe_id" );
        header.add( "probe_name" );
        header.add( "pvalue" );
        header.add( "corrected_pvalue" );
        header.add( "rank" );
        header.add( "contrasts" );
        if ( baselineMap != null ) {
            header.add( "baseline" );
        }
        try ( CSVPrinter printer = getTsvFormatBuilder( extraHeaderComments.toArray( new String[0] ) )
                .setHeader( header.toArray( new String[0] ) ).build().print( writer ) ) {
            for ( DifferentialExpressionAnalysisResult result : entity ) {
                List<Object> record = new ArrayList<>( header.size() );
                record.add( result.getId() );
                if ( sourceExperimentIdMap != null ) {
                    record.add( format( sourceExperimentIdMap.get( result ) ) );
                }
                if ( experimentAnalyzedIdMap != null ) {
                    record.add( format( experimentAnalyzedIdMap.get( result ) ) );
                }
                record.add( format( result.getResultSet().getId() ) );
                record.add( format( result.getProbe().getId() ) );
                record.add( format( result.getProbe().getName() ) );
                record.add( format( result.getPvalue() ) );
                record.add( format( result.getCorrectedPvalue() ) );
                record.add( format( result.getRank() ) );
                record.add( formatContrasts( result.getContrasts() ) );
                if ( baselineMap != null ) {
                    record.add( formatBaseline( baselineMap.get( result ) ) );
                }
                printer.printRecord( record );
            }
        }
    }

    private String formatContrasts( Set<ContrastResult> contrasts ) {
        return contrasts.stream().map( this::formatContrast ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private String formatContrast( ContrastResult contrast ) {
        String factor;
        if ( contrast.getFactorValue() != null ) {
            factor = String.valueOf( contrast.getFactorValue().getId() );
            if ( contrast.getSecondFactorValue() != null ) {
                factor += ":" + contrast.getSecondFactorValue().getId();
            }
        } else {
            factor = "[continuous]";
        }
        return String.format( "factor=%s coefficient=%s log2fc=%s tstat=%s pvalue=%s",
                format( factor ),
                format( contrast.getCoefficient() ),
                format( contrast.getLogFoldChange() ),
                format( contrast.getTstat() ),
                format( contrast.getPvalue() ) );
    }

    private String formatBaseline( @Nullable Baseline baseline ) {
        if ( baseline == null ) {
            return null;
        }
        return format( baseline.getFactorValue().getId() + ( baseline.getSecondFactorValue() != null ? ":" + baseline.getSecondFactorValue().getId() : "" ) );
    }
}
