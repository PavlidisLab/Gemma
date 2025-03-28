package ubic.gemma.core.analysis.service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static ubic.gemma.core.util.TsvUtils.*;

@Service
@CommonsLog
public class ExpressionAnalysisResultSetFileServiceImpl implements ExpressionAnalysisResultSetFileService {

    @Autowired
    private BuildInfo buildInfo;

    @Override
    public void writeTsv( ExpressionAnalysisResultSet analysisResultSet, @Nullable Baseline baseline, @Nullable Map<Long, Set<Gene>> resultId2Genes, Writer writer ) throws IOException {
        String what = "Differential expression analysis result set with ID " + analysisResultSet.getId();
        List<String> extraHeaderComments = new ArrayList<>();

        String experimentalFactorsMetadata = "[" + analysisResultSet.getExperimentalFactors().stream()
                .map( this::formatExperimentalFactor )
                .collect( Collectors.joining( ", " ) ) + "]";
        extraHeaderComments.add( "Experimental factors: " + experimentalFactorsMetadata );

        if ( baseline != null ) {
            extraHeaderComments.add( "Baseline: " + formatFactorValue( baseline.getFactorValue() ) );
            if ( baseline.getSecondFactorValue() != null ) {
                extraHeaderComments.add( "Second baseline: " + formatFactorValue( baseline.getSecondFactorValue() ) );
            }
        }

        // add the basic columns
        List<String> header = new ArrayList<>( Arrays.asList( "id", "probe_id", "probe_name" ) );

        if ( resultId2Genes != null ) {
            header.addAll( Arrays.asList( "gene_id", "gene_name", "gene_ncbi_id", "gene_ensembl_id", "gene_official_symbol", "gene_official_name" ) );
        }

        header.addAll( Arrays.asList( "pvalue", "corrected_pvalue", "rank" ) );

        // for continuous
        Set<ExperimentalFactor> factorsIfContinuous = analysisResultSet.getExperimentalFactors().stream()
                .filter( ef -> ef.getType().equals( FactorType.CONTINUOUS ) )
                .collect( Collectors.toSet() );
        ExperimentalFactor factorIfContinuous;
        if ( factorsIfContinuous.isEmpty() ) {
            factorIfContinuous = null;
        } else if ( factorsIfContinuous.size() == 1 ) {
            factorIfContinuous = factorsIfContinuous.iterator().next();
        } else {
            throw new UnsupportedOperationException( "Result sets with more than one continuous factor are not supported." );
        }

        // this is the order the factor values are displayed
        // this is only relevant for interactions
        Comparator<Contrast> contrastResultComparator = Comparator
                .comparing( Contrast::getFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) )
                .thenComparing( Contrast::getSecondFactorValue, Comparator.nullsLast( Comparator.comparing( FactorValue::getId ) ) );

        // we need to peek in the contrast result to understand factor value interactions
        // i.e. interaction between genotype and time point might result in a contrast_male_3h column, although we would
        // use factor value IDs in the actual column name which might result in something like contrast_1292_2938
        LinkedHashSet<Contrast> allContrasts = analysisResultSet.getResults().stream()
                .flatMap( r -> r.getContrasts().stream() )
                .map( c -> contrastFromResult( c, factorIfContinuous ) )
                .sorted( contrastResultComparator )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );

        for ( Contrast contrast : allContrasts ) {
            StringBuilder contrastResultPrefix = new StringBuilder( "contrast_" );
            // this could be empty for a continuous factor, in which case it will be serialized as contrast_log2fc,
            // contrast_tstat, etc...
            for ( FactorValue fv : contrast.getFactorValues() ) {
                contrastResultPrefix.append( fv.getId() ).append( "_" );
            }
            header.addAll( Arrays.asList(
                    contrastResultPrefix + "coefficient",
                    contrastResultPrefix + "log2fc",
                    contrastResultPrefix + "tstat",
                    contrastResultPrefix + "pvalue" ) );
        }

        try ( CSVPrinter printer = getTsvFormatBuilder( what, buildInfo, extraHeaderComments.toArray( new String[1] ) )
                .setHeader( header.toArray( new String[0] ) )
                .build()
                .print( writer ) ) {
            for ( DifferentialExpressionAnalysisResult analysisResult : analysisResultSet.getResults() ) {
                final List<Object> record = new ArrayList<>( Arrays.asList( analysisResult.getId(),
                        analysisResult.getProbe().getId(),
                        analysisResult.getProbe().getName() ) );
                if ( resultId2Genes != null ) {
                    List<Gene> genes = resultId2Genes.getOrDefault( analysisResult.getId(), Collections.emptySet() )
                            .stream().sorted( Comparator.comparing( Gene::getOfficialSymbol ) )
                            .collect( Collectors.toList() );
                    record.addAll( Arrays.asList(
                            formatGenesLongAttribute( genes, Gene::getId ),
                            formatGenesAttribute( genes, Gene::getName ),
                            formatGenesIntAttribute( genes, Gene::getNcbiGeneId ),
                            formatGenesAttribute( genes, Gene::getEnsemblId ),
                            formatGenesAttribute( genes, Gene::getOfficialSymbol ),
                            formatGenesAttribute( genes, Gene::getOfficialName ) ) );
                }
                record.addAll( Arrays.asList(
                        format( analysisResult.getPvalue() ),
                        format( analysisResult.getCorrectedPvalue() ),
                        format( analysisResult.getRank() ) ) );
                Map<Contrast, ContrastResult> contrastResultMap = analysisResult.getContrasts().stream()
                        .collect( Collectors.toMap( cr -> contrastFromResult( cr, factorIfContinuous ), identity() ) );
                // render contrast results in the same order than the first row and handle possibly missing columns
                for ( Contrast contrast : allContrasts ) {
                    ContrastResult cr = contrastResultMap.get( contrast );
                    if ( cr != null ) {
                        record.add( format( cr.getCoefficient() ) );
                        record.add( format( cr.getLogFoldChange() ) );
                        record.add( format( cr.getTstat() ) );
                        record.add( format( cr.getPvalue() ) );
                    } else {
                        record.add( format( Double.NaN ) );
                        record.add( format( Double.NaN ) );
                        record.add( format( Double.NaN ) );
                        record.add( format( Double.NaN ) );
                    }
                }
                printer.printRecord( record );
            }
        }
    }

    private String formatGenesLongAttribute( List<Gene> genes, Function<Gene, Long> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private String formatGenesIntAttribute( List<Gene> genes, Function<Gene, Integer> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private String formatGenesAttribute( List<Gene> genes, Function<Gene, String> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    /**
     * Create a contrast from a {@link ContrastResult}.
     * @param factorIfContinuous a factor to use if the contrast is continuous
     */
    private Contrast contrastFromResult( ContrastResult cr, @Nullable ExperimentalFactor factorIfContinuous ) {
        if ( cr.getSecondFactorValue() != null ) {
            Assert.notNull( cr.getFactorValue(), "There must be a first factor value if a second factor value is present." );
            return Contrast.interaction( cr.getFactorValue(), cr.getSecondFactorValue() );
        } else if ( cr.getFactorValue() != null ) {
            return Contrast.categorical( cr.getFactorValue() );
        } else if ( factorIfContinuous != null ) {
            return Contrast.continuous( factorIfContinuous );
        } else {
            throw new IllegalArgumentException( "A factor must be provided for a continuous contrast." );
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
        if ( factorValue.getMeasurement() != null ) {
            return formatMeasurement( factorValue.getMeasurement() );
        }
        return "id: " + factorValue.getId()
                + ( factorValue.getIsBaseline() != null && factorValue.getIsBaseline() ? "*" : "" )
                + ", characteristics: [" + formatCharacteristics( factorValue.getCharacteristics() ) + "]";
    }

    private String formatMeasurement( Measurement measurement ) {
        return measurement.getValue()
                + ( measurement.getUnit() != null ? measurement.getUnit().getUnitNameCV() : "" );
    }

    private String formatCharacteristics( Collection<? extends Characteristic> characteristics ) {
        return characteristics.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) );
    }
}
