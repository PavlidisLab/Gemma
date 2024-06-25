package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
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

@Service
public class DifferentialExpressionAnalysisResultListFileServiceImpl extends AbstractFileService<List<DifferentialExpressionAnalysisResult>> implements DifferentialExpressionAnalysisResultListFileService {

    @Override
    public void writeTsv( List<DifferentialExpressionAnalysisResult> entity, Writer writer ) throws IOException {
        writeTsvInternal( entity, null, null, null, writer );
    }

    @Override
    public void writeTsv( List<DifferentialExpressionAnalysisResult> entity, Gene gene, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Writer writer ) throws IOException {
        writeTsvInternal( entity, gene, sourceExperimentIdMap, experimentAnalyzedIdMap, writer );
    }

    private void writeTsvInternal( List<DifferentialExpressionAnalysisResult> payload, @Nullable Gene gene, @Nullable Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, @Nullable Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Writer writer ) throws IOException {
        List<String> extraHeaderComments = new ArrayList<>();
        if ( gene != null ) {
            extraHeaderComments.add( "Results for " + gene );
        }
        extraHeaderComments.add( String.format( "The 'contrasts' column contains contrasts delimited by '%s'. Each contrast is structured as space-delimited key=value pairs. Factors are encoded by their factor value ID for the 'factor' key. Interaction of factors are encoded as 'id1:id2'. Continuous contrasts will use the '[continuous]' indicator.", getSubDelimiter() ) );
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
        try ( CSVPrinter printer = getTsvFormatBuilder( extraHeaderComments.toArray( new String[0] ) )
                .setHeader( header.toArray( new String[0] ) ).build().print( writer ) ) {
            for ( DifferentialExpressionAnalysisResult result : payload ) {
                List<Object> record = new ArrayList<>( header.size() );
                record.add( result.getId() );
                if ( sourceExperimentIdMap != null ) {
                    record.add( sourceExperimentIdMap.get( result ) );
                }
                if ( experimentAnalyzedIdMap != null ) {
                    record.add( experimentAnalyzedIdMap.get( result ) );
                }
                record.add( result.getResultSet().getId() );
                record.add( result.getProbe().getId() );
                record.add( result.getProbe().getName() );
                record.add( format( result.getPvalue() ) );
                record.add( format( result.getCorrectedPvalue() ) );
                record.add( format( result.getRank() ) );
                record.add( formatContrasts( result.getContrasts() ) );
                printer.printRecord( record );
            }
        }
    }

    private String formatContrasts( Set<ContrastResult> contrasts ) {
        return contrasts.stream().map( this::formatContrast ).collect( Collectors.joining( getSubDelimiter() ) );
    }

    private String formatContrast( ContrastResult contrast ) {
        String d;
        if ( contrast.getFactorValue() != null ) {
            d = String.valueOf( contrast.getFactorValue().getId() );
            if ( contrast.getSecondFactorValue() != null ) {
                d += ":" + contrast.getSecondFactorValue().getId();
            }
        } else {
            d = "[continuous]";
        }
        return String.format( "factor=%s coefficient=%s log2fc=%s tstat=%s pvalue=%s",
                d,
                format( contrast.getCoefficient() ),
                format( contrast.getLogFoldChange() ),
                format( contrast.getTstat() ),
                format( contrast.getPvalue() ) );
    }
}
