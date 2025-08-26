package ubic.gemma.model.analysis.expression.diff;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Utilities for generating random {@link DifferentialExpressionAnalysis} for testing purposes.
 * @author poirigui
 */
public class RandomDifferentialExpressionAnalysisUtils {

    private static final ThreadLocal<NormalDistribution> normalDistribution = ThreadLocal.withInitial( NormalDistribution::new );

    private static Random random() {
        return ThreadLocalRandom.current();
    }

    private static NormalDistribution normalDistribution() {
        return normalDistribution.get();
    }

    public static void setSeed( long seed ) {
        random().setSeed( seed );
        normalDistribution.get().reseedRandomGenerator( seed );
    }

    public static DifferentialExpressionAnalysis randomAnalysis( BioAssaySet experimentAnalyzed, ExperimentalDesign design, ArrayDesign platform ) {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        analysis.setExperimentAnalyzed( experimentAnalyzed );
        // do all possible factors
        for ( ExperimentalFactor factor : design.getExperimentalFactors() ) {
            analysis.getResultSets().add( randomResultSet( analysis, Collections.singleton( factor ), platform ) );
        }
        // do all possible interactions
        for ( ExperimentalFactor factor : design.getExperimentalFactors() ) {
            for ( ExperimentalFactor factor2 : design.getExperimentalFactors() ) {
                if ( factor != factor2 && factor.getType() == FactorType.CATEGORICAL && factor2.getType() == FactorType.CATEGORICAL ) {
                    analysis.getResultSets().add( randomResultSet( analysis, new HashSet<>( Arrays.asList( factor, factor2 ) ), platform ) );
                }
            }
        }
        return analysis;
    }

    private static ExpressionAnalysisResultSet randomResultSet( DifferentialExpressionAnalysis analysis, Set<ExperimentalFactor> factors, ArrayDesign platform ) {
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( analysis );
        resultSet.setExperimentalFactors( factors );
        resultSet.setPvalueDistribution( randomPvalueDistribution() );
        for ( CompositeSequence probe : platform.getCompositeSequences() ) {
            resultSet.getResults().add( randomResult( resultSet, probe ) );
        }
        return resultSet;
    }

    private static PvalueDistribution randomPvalueDistribution() {
        return PvalueDistribution.Factory.newInstance( new double[100] );
    }

    private static DifferentialExpressionAnalysisResult randomResult( ExpressionAnalysisResultSet resultSet, CompositeSequence probe ) {
        DifferentialExpressionAnalysisResult result = new DifferentialExpressionAnalysisResult();
        result.setResultSet( resultSet );
        result.setProbe( probe );
        double pvalue = random().nextDouble();
        result.setPvalue( pvalue );
        result.setCorrectedPvalue( pvalue );
        for ( int i = 0; i < 10; i++ ) {
            List<ExperimentalFactor> factors = resultSet.getExperimentalFactors().stream().sorted( ExperimentalFactor.COMPARATOR ).collect( Collectors.toList() );
            if ( factors.size() == 1 ) {
                ExperimentalFactor factor = factors.iterator().next();
                if ( factor.getType() == FactorType.CATEGORICAL ) {
                    for ( FactorValue fv : factor.getFactorValues() ) {
                        if ( !fv.equals( resultSet.getBaselineGroup() ) ) {
                            result.getContrasts().add( randomContrastResult( fv ) );
                        }
                    }
                } else if ( factor.getType() == FactorType.CONTINUOUS ) {
                    result.getContrasts().add( randomContrastResult( factor ) );
                } else {
                    throw new IllegalArgumentException( "Unknown factor type: " + factor.getType() );
                }
            } else if ( resultSet.getExperimentalFactors().size() == 2 ) {
                for ( FactorValue fv : factors.get( 0 ).getFactorValues() ) {
                    for ( FactorValue fv2 : factors.get( 1 ).getFactorValues() ) {
                        result.getContrasts().add( randomContrastResult( fv, fv2 ) );
                    }
                }
            } else {
                throw new IllegalArgumentException( "Unsupported number of factors (" + factors.size() + ") in " + resultSet + "." );
            }
        }
        return result;
    }

    private static ContrastResult randomContrastResult( FactorValue fv1 ) {
        Assert.isTrue( fv1.getExperimentalFactor().getType() == FactorType.CATEGORICAL );
        ContrastResult cr = new ContrastResult();
        cr.setFactorValue( fv1 );
        cr.setLogFoldChange( normalDistribution().sample() );
        cr.setPvalue( random().nextDouble() );
        return cr;
    }

    private static ContrastResult randomContrastResult( FactorValue fv1, FactorValue fv2 ) {
        Assert.isTrue( fv1.getExperimentalFactor().getType() == FactorType.CATEGORICAL );
        Assert.isTrue( fv2.getExperimentalFactor().getType() == FactorType.CATEGORICAL );
        ContrastResult cr = new ContrastResult();
        cr.setFactorValue( fv1 );
        cr.setSecondFactorValue( fv2 );
        cr.setLogFoldChange( normalDistribution().sample() );
        cr.setPvalue( random().nextDouble() );
        return cr;
    }

    private static ContrastResult randomContrastResult( ExperimentalFactor factor ) {
        Assert.isTrue( factor.getType() == FactorType.CONTINUOUS );
        ContrastResult cr = new ContrastResult();
        cr.setCoefficient( normalDistribution().sample() );
        cr.setTstat( normalDistribution().sample() );
        cr.setPvalue( random().nextDouble() );
        return cr;
    }

}
