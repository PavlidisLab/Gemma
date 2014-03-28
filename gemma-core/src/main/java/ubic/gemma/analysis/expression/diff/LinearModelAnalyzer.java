/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.expression.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.math.DesignMatrix;
import ubic.basecode.math.LeastSquaresFit;
import ubic.basecode.math.MathUtil;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.math.MeanVarianceEstimator;
import ubic.basecode.util.r.type.LinearModelSummary;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.Direction;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * Handles fitting linear models with continuous or fixed-level covariates. Data are always log-transformed.
 * <p>
 * Interactions can be included if a DifferentialExpressionAnalysisConfig is passed as an argument to 'run'. Currently
 * we only support interactions if there are two factors in the model (no more).
 * <p>
 * One factor can be constant (the same value for all samples); such a factor will be analyzed by looking at the
 * intercept in the fitted model. This is only appropriate for 'non-reference' designs on ratiometric arrays.
 * <p>
 * This also supports subsetting the data based on a factor. For example, a data set with "tissue" as a factor could be
 * analyzed per-tissue rather than with tissue as a covariate.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Scope(value = "prototype")
public class LinearModelAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private static Log log = LogFactory.getLog( LinearModelAnalyzer.class );

    /**
     * Preset levels for which we will store the HitListSizes.
     */
    private static final double[] qValueThresholdsForHitLists = new double[] { 0.001, 0.005, 0.01, 0.05, 0.1 };

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.DiffExAnalyzer#computeHitListSizes(java.util.List, java.util.Map)
     */
    @Override
    public Collection<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        Collection<HitListSize> hitListSizes = new HashSet<HitListSize>();
        StopWatch timer = new StopWatch();
        timer.start();
        double maxThreshold = MathUtil.max( qValueThresholdsForHitLists );

        assert probeToGeneMap != null;

        Collection<Gene> allGenes = new HashSet<Gene>();
        for ( Collection<Gene> genes : probeToGeneMap.values() ) {
            allGenes.addAll( genes );
        }

        // maps from Doubles are a bit dodgy...
        Map<Double, Integer> upCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> downCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCounts = new HashMap<Double, Integer>();

        Map<Double, Integer> upCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> downCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCountGenes = new HashMap<Double, Integer>();

        Collection<Gene> seenGenes = new HashSet<Gene>();
        for ( DifferentialExpressionAnalysisResult r : results ) {

            Double corrP = r.getCorrectedPvalue();
            if ( corrP == null || corrP > maxThreshold ) {
                continue;
            }

            CompositeSequence probe = r.getProbe();
            Collection<Gene> genesForProbe = probeToGeneMap.get( probe );
            int numGenes = 0;
            if ( genesForProbe != null ) {
                numGenes = countNumberOfGenesNotSeenAlready( genesForProbe, seenGenes );
            }

            if ( numGenes == 0 ) {
                // This is okay; it might mean we already counted it as differentially expressed.
            }

            Collection<ContrastResult> crs = r.getContrasts();
            boolean up = false;
            boolean down = false;

            /*
             * We set up and down to be true (either or both) if at least on contrast is shown.
             */
            for ( ContrastResult cr : crs ) {
                Double lf = cr.getLogFoldChange();
                if ( lf == null ) {
                    /*
                     * A contrast which is actually not valid, so it won't be counted in the hit list.
                     */
                    continue;
                } else if ( lf < 0 ) {
                    down = true;
                } else if ( lf > 0 ) {
                    up = true;
                }
            }

            for ( double thresh : qValueThresholdsForHitLists ) {

                if ( !upCounts.containsKey( thresh ) ) {
                    upCounts.put( thresh, 0 );
                    upCountGenes.put( thresh, 0 );
                }
                if ( !downCounts.containsKey( thresh ) ) {
                    downCounts.put( thresh, 0 );
                    downCountGenes.put( thresh, 0 );
                }
                if ( !eitherCounts.containsKey( thresh ) ) {
                    eitherCounts.put( thresh, 0 );
                    eitherCountGenes.put( thresh, 0 );
                }

                if ( corrP < thresh ) {
                    if ( up ) {
                        upCounts.put( thresh, upCounts.get( thresh ) + 1 );
                        upCountGenes.put( thresh, upCountGenes.get( thresh ) + numGenes );
                    }
                    if ( down ) {
                        downCounts.put( thresh, downCounts.get( thresh ) + 1 );
                        downCountGenes.put( thresh, downCountGenes.get( thresh ) + numGenes );
                    }

                    eitherCounts.put( thresh, eitherCounts.get( thresh ) + 1 );
                    eitherCountGenes.put( thresh, eitherCountGenes.get( thresh ) + numGenes );

                }
            }

        }

        for ( double thresh : qValueThresholdsForHitLists ) {
            // Ensure we don't set values to null.
            Integer up = upCounts.get( thresh ) == null ? 0 : upCounts.get( thresh );
            Integer down = downCounts.get( thresh ) == null ? 0 : downCounts.get( thresh );
            Integer either = eitherCounts.get( thresh ) == null ? 0 : eitherCounts.get( thresh );

            Integer upGenes = upCountGenes.get( thresh ) == null ? 0 : upCountGenes.get( thresh );
            Integer downGenes = downCountGenes.get( thresh ) == null ? 0 : downCountGenes.get( thresh );
            Integer eitherGenes = eitherCountGenes.get( thresh ) == null ? 0 : eitherCountGenes.get( thresh );

            assert !( allGenes.size() < upGenes || allGenes.size() < downGenes || allGenes.size() < eitherGenes ) : "There were more genes differentially expressed than exist in the experment";

            HitListSize upS = HitListSize.Factory.newInstance( thresh, up, Direction.UP, upGenes );
            HitListSize downS = HitListSize.Factory.newInstance( thresh, down, Direction.DOWN, downGenes );
            HitListSize eitherS = HitListSize.Factory.newInstance( thresh, either, Direction.EITHER, eitherGenes );

            hitListSizes.add( upS );
            hitListSizes.add( downS );
            hitListSizes.add( eitherS );

            assert upGenes <= eitherGenes : "More genes upregulated than upregulated+downregulated";
            assert downGenes <= eitherGenes : "More genes downregulated than upregulated+downregulated";

        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Hitlist computation: " + timer.getTime() + "ms" );
        }
        return hitListSizes;
    }

    /**
     * Determine if any factor should be treated as the intercept term.
     * 
     * @param factors
     * @param quantitationType
     * @return
     */
    @Override
    public ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType ) {
        ExperimentalFactor interceptFactor = null;
        for ( ExperimentalFactor experimentalFactor : factors ) {

            /*
             * Check if we need to treat the intercept as a factor.
             */
            boolean useI = checkIfNeedToTreatAsIntercept( experimentalFactor, quantitationType );

            if ( useI && interceptFactor != null ) {
                throw new IllegalStateException( "Can only deal with one constant factor (intercept)" );
            } else if ( useI ) {
                interceptFactor = experimentalFactor;
            }
        }
        return interceptFactor;
    }

    /**
     * @param resultList
     * @param probeToGeneMap
     * @return
     */
    @Override
    public int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {

        Collection<Gene> gs = new HashSet<Gene>();
        for ( DifferentialExpressionAnalysisResult d : resultList ) {
            CompositeSequence probe = d.getProbe();
            if ( probeToGeneMap.containsKey( probe ) ) {
                gs.addAll( probeToGeneMap.get( probe ) );
            }
        }
        return gs.size();
    }

    /**
     * @param matrix on which to perform regression.
     * @param config containing configuration of factors to include. Any interactions or subset configuration is
     *        ignored. Data are <em>NOT</em> log transformed unless they come in that way. (the qvaluethreshold will be
     *        ignored)
     * @param retainScale if true, the data retain the global mean (intercept)
     * @return residuals from the regression.
     */
    @Override
    public ExpressionDataDoubleMatrix regressionResiduals( ExpressionDataDoubleMatrix matrix,
            DifferentialExpressionAnalysisConfig config, boolean retainScale ) {

        if ( config.getFactorsToInclude().isEmpty() ) {
            log.warn( "No factors" );
            return matrix; // FIXME perhaps return a copy instead. OR throw an ex.
        }

        /*
         * Always retain all.
         */
        config.setQvalueThreshold( null );

        /*
         * Note that this method relies on similar code to doAnalysis, for the setup stages.
         */

        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( matrix, factors );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils.getBaselineConditions(
                samplesUsed, factors );

        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils.buildDesignMatrix( factors,
                samplesUsed, baselineConditions );

        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        ExpressionDataDoubleMatrix dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, matrix );
        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();

        DoubleMatrix<String, String> sNamedMatrix = makeDataMatrix( designMatrix, namedMatrix );

        // perform weighted least squares regression on COUNT data
        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        LeastSquaresFit fit = null;
        if ( quantitationType.getScale().equals( ScaleType.COUNT ) ) {
            log.info( "Calculating residuals of weighted least squares regression on COUNT data" );
            DoubleMatrix1D librarySize = MatrixStats.colSums( sNamedMatrix ); // note: data is not log transformed
            MeanVarianceEstimator mv = new MeanVarianceEstimator( properDesignMatrix, sNamedMatrix, librarySize );
            fit = new LeastSquaresFit( properDesignMatrix, sNamedMatrix, mv.getWeights() );
        } else {
            fit = new LeastSquaresFit( properDesignMatrix, sNamedMatrix );
        }

        DoubleMatrix2D residuals = fit.getResiduals();

        if ( retainScale ) {
            DoubleMatrix1D intercept = fit.getCoefficients().viewRow( 0 );
            for ( int i = 0; i < residuals.rows(); i++ ) {
                residuals.viewRow( i ).assign( Functions.plus( intercept.get( i ) ) );
            }
        }

        DoubleMatrix<CompositeSequence, BioMaterial> f = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                residuals.toArray() );
        f.setRowNames( dmatrix.getMatrix().getRowNames() );
        f.setColumnNames( dmatrix.getMatrix().getColNames() );
        return new ExpressionDataDoubleMatrix( dmatrix, f );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        return run( expressionExperiment, dmatrix, config );

    }

    /**
     * @param expressionExperiment
     * @param dmatrix
     * @param config
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {

        /*
         * I apologize for this being so complicated. Basically there are four phases:
         * 
         * 1. Get the data matrix and factors
         * 
         * 2. Determine baseline groups; build model and contrasts
         * 
         * 3. Run the analysis
         * 
         * 4. Postprocess the analysis
         * 
         * By far the most complex is #2 -- it depends on which factors and what kind they are.
         */

        /*
         * Initialize our matrix and factor lists...
         */
        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( dmatrix, factors );

        dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, dmatrix ); // enforce ordering

        /*
         * Do the analysis, by subsets if requested
         */
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

        ExperimentalFactor subsetFactor = config.getSubsetFactor();
        if ( subsetFactor != null ) {

            if ( factors.contains( subsetFactor ) ) {
                throw new IllegalStateException( "Subset factor cannot also be included in the analysis [ Factor was: "
                        + subsetFactor + "]" );
            }

            Map<FactorValue, ExpressionDataDoubleMatrix> subsets = makeSubSets( config, dmatrix, samplesUsed,
                    subsetFactor );

            /*
             * Now analyze each subset
             */
            for ( FactorValue subsetFactorValue : subsets.keySet() ) {

                log.info( "Analyzing subset: " + subsetFactorValue );

                List<BioMaterial> bioMaterials = ExperimentalDesignUtils.getOrderedSamples(
                        subsets.get( subsetFactorValue ), factors );

                /*
                 * make a EESubSet
                 */
                ExpressionExperimentSubSet eesubSet = ExpressionExperimentSubSet.Factory.newInstance();
                eesubSet.setSourceExperiment( expressionExperiment );
                eesubSet.setName( "Subset for " + subsetFactorValue );
                Collection<BioAssay> bioAssays = new HashSet<BioAssay>();
                for ( BioMaterial bm : bioMaterials ) {
                    bioAssays.addAll( bm.getBioAssaysUsedIn() );
                }
                eesubSet.getBioAssays().addAll( bioAssays );

                Collection<ExperimentalFactor> subsetFactors = fixFactorsForSubset( subsets.get( subsetFactorValue ),
                        eesubSet, factors );

                DifferentialExpressionAnalysisConfig subsetConfig = fixConfigForSubset( factors, config );

                if ( subsetFactors.isEmpty() ) {
                    log.warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
                    continue;
                }

                /*
                 * Run analysis on the subset.
                 */
                DifferentialExpressionAnalysis analysis = doAnalysis( eesubSet, subsetConfig,
                        subsets.get( subsetFactorValue ), bioMaterials, new ArrayList<ExperimentalFactor>(
                                subsetFactors ), subsetFactorValue );

                if ( analysis == null ) {
                    log.warn( "No analysis results were obtained for subset: " + subsetFactorValue );
                    continue;
                }

                results.add( analysis );

            }

        } else {

            /*
             * Analyze the whole thing as one
             */
            DifferentialExpressionAnalysis analysis = doAnalysis( expressionExperiment, config, dmatrix, samplesUsed,
                    factors, null );
            if ( analysis == null ) {
                log.warn( "No analysis results were obtained" );
            } else {
                results.add( analysis );
            }
        }
        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.DiffExAnalyzer#run(ubic.gemma.model.expression.experiment.
     * ExpressionExperimentSubSet, ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {

        /*
         * Start by setting it up like the full experiment.
         */
        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( subset
                .getSourceExperiment() );

        ExperimentalFactor ef = config.getSubsetFactor();
        Collection<BioMaterial> bmtmp = new HashSet<BioMaterial>();
        for ( BioAssay ba : subset.getBioAssays() ) {
            bmtmp.add( ba.getSampleUsed() );
        }

        List<BioMaterial> samplesInSubset = new ArrayList<BioMaterial>( bmtmp );

        FactorValue subsetFactorValue = null;
        for ( BioMaterial bm : samplesInSubset ) {
            Collection<FactorValue> fvs = bm.getFactorValues();
            for ( FactorValue fv : fvs ) {
                if ( fv.getExperimentalFactor().equals( ef ) ) {
                    if ( subsetFactorValue == null ) {
                        subsetFactorValue = fv;
                    } else if ( !subsetFactorValue.equals( fv ) ) {
                        throw new IllegalStateException(
                                "This subset has more than one factor value for the supposed subsetfactor: " + fv
                                        + " and " + subsetFactorValue );
                    }
                }
            }
        }

        samplesInSubset = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesInSubset,
                config.getFactorsToInclude() );

        // slice.
        ExpressionDataDoubleMatrix subsetMatrix = new ExpressionDataDoubleMatrix( samplesInSubset, dmatrix );

        Collection<ExperimentalFactor> subsetFactors = fixFactorsForSubset( dmatrix, subset,
                config.getFactorsToInclude() );

        if ( subsetFactors.isEmpty() ) {
            log.warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
            return null;
        }

        DifferentialExpressionAnalysisConfig subsetConfig = fixConfigForSubset( config.getFactorsToInclude(), config );

        DifferentialExpressionAnalysis analysis = doAnalysis( subset, subsetConfig, subsetMatrix, samplesInSubset,
                config.getFactorsToInclude(), subsetFactorValue );

        if ( analysis == null ) {
            throw new IllegalStateException( "Subset could not be analyzed with config: " + config );
        }
        return analysis;
    }

    /**
     * @param experimentalFactor
     * @param quantitationType
     * @return boolean true if we need the intercept.
     */
    protected boolean checkIfNeedToTreatAsIntercept( ExperimentalFactor experimentalFactor,
            QuantitationType quantitationType ) {
        if ( experimentalFactor.getFactorValues().size() == 1 ) {
            if ( quantitationType.getIsRatio() ) {
                return true;
            }
            throw new IllegalArgumentException(
                    "Cannot deal with constant factors unless the data are ratiometric non-reference design" );
        }
        return false;
    }

    /**
     * Create files that can be used in R to check the results.
     * 
     * @param dmatrix
     * @param designMatrix
     */
    protected void outputForDebugging( ExpressionDataDoubleMatrix dmatrix,
            ObjectMatrix<String, String, Object> designMatrix ) {
        MatrixWriter mw = new MatrixWriter();
        try (FileWriter writer = new FileWriter( File.createTempFile( "data.", ".txt" ) );
                FileWriter out = new FileWriter( File.createTempFile( "design.", ".txt" ) );) {

            mw.write( writer, dmatrix, null, true, false );

            ubic.basecode.io.writer.MatrixWriter<String, String> dem = new ubic.basecode.io.writer.MatrixWriter<String, String>(
                    out );
            dem.writeMatrix( designMatrix, true );

        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Assigns the design matrix columns as factors, defines contrasts. We use treatment contrasts, comparing to a
     * baseline condition.
     * 
     * @param designMatrix
     * @param baselineConditions
     */
    protected void setupFactors( ObjectMatrix<String, String, Object> designMatrix,
            Map<ExperimentalFactor, FactorValue> baselineConditions ) {

        for ( ExperimentalFactor factor : baselineConditions.keySet() ) {

            if ( factor.getFactorValues().size() < 2 ) {
                continue;
            }

            String factorName = ExperimentalDesignUtils.nameForR( factor );

            if ( !designMatrix.containsColumnName( factorName ) ) {
                /*
                 * This means the factor was dropped, so we don't need to figure out the baseline.
                 */
                continue;
            }

            Object[] column = designMatrix.getColumn( designMatrix.getColIndexByName( factorName ) );

            if ( ExperimentalDesignUtils.isContinuous( factor ) ) {
                double[] colD = new double[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colD[i] = ( Double ) column[i];
                }
            } else {
                String[] colS = new String[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colS[i] = ( String ) column[i];
                }

            }
        }
    }

    /**
     * Build the formula, omitting the factor taking the place of the intercept, if need be. (Mostly for R but with side
     * effect of populating the interactionFactorLists and label2Factors)
     * 
     * @param config
     * @param label2Factors
     * @param interceptFactor
     * @param interactionFactorLists gets populated
     * @return
     */
    private String buildModelFormula( final DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors, final ExperimentalFactor interceptFactor,
            final List<String[]> interactionFactorLists ) {
        String modelFormula;

        String factTerm = "";
        for ( String nameInR : label2Factors.keySet() ) {
            if ( interceptFactor != null && label2Factors.get( nameInR ).size() == 1
                    && label2Factors.get( nameInR ).iterator().next().equals( interceptFactor ) ) {
                continue;
            }
            factTerm = factTerm + " " + nameInR + " +";
        }
        factTerm = factTerm.replaceFirst( "\\+$", "" );

        /*
         * Add interaction terms
         */
        boolean hasInteractionTerms = config.getInteractionsToInclude() != null
                && !config.getInteractionsToInclude().isEmpty();
        if ( hasInteractionTerms ) {
            for ( Collection<ExperimentalFactor> interactionTerms : config.getInteractionsToInclude() ) {

                List<String> interactionFactorNames = new ArrayList<String>();
                for ( ExperimentalFactor factor : interactionTerms ) {
                    interactionFactorNames.add( ExperimentalDesignUtils.nameForR( factor ) );
                }

                factTerm = factTerm + " + " + StringUtils.join( interactionFactorNames, "*" ); // in the R
                // statement

                interactionFactorLists.add( interactionFactorNames.toArray( new String[] {} ) );

                // In the ANOVA table.
                String factTableLabel = StringUtils.join( interactionFactorNames, ":" );
                label2Factors.put( factTableLabel, new HashSet<ExperimentalFactor>() );
                label2Factors.get( factTableLabel ).addAll( interactionTerms );
            }
        }

        modelFormula = " ~ " + factTerm;
        return modelFormula;
    }

    /**
     * @param genesForProbe
     * @param seenGenes
     * @return
     */
    private int countNumberOfGenesNotSeenAlready( Collection<Gene> genesForProbe, Collection<Gene> seenGenes ) {
        int numGenes = 0;
        if ( genesForProbe != null ) {
            for ( Gene g : genesForProbe ) {
                if ( seenGenes.contains( g ) ) {
                    continue;
                }
                numGenes++;
            }
            seenGenes.addAll( genesForProbe );
        }

        return numGenes;
    }

    /**
     * @param bioAssaySet source data, could be a SubSet
     * @param config
     * @param dmatrix data
     * @param samplesUsed analyzed
     * @param factors included in the model
     * @param subsetFactorValue null unless analyzing a subset (only used for book-keeping)
     * @return analysis, or null if there was a problem.
     */
    private DifferentialExpressionAnalysis doAnalysis( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config, ExpressionDataDoubleMatrix dmatrix,
            List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, FactorValue subsetFactorValue ) {

        if ( factors.isEmpty() ) {
            log.error( "Must provide at least one factor" );
            return null;
        }

        if ( samplesUsed.size() <= factors.size() ) {
            log.error( "Must have more samples than factors" );
            return null;
        }

        final Map<String, Collection<ExperimentalFactor>> label2Factors = getRNames( factors );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils.getBaselineConditions(
                samplesUsed, factors );

        dropIncompleteFactors( samplesUsed, factors, baselineConditions );

        if ( factors.isEmpty() ) {
            log.error( "Must provide at least one factor; they were all removed due to incomplete values" );
            return null;
        }

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        ExperimentalFactor interceptFactor = determineInterceptFactor( factors, quantitationType );

        /*
         * Build our factor terms, with interactions handled specially
         */
        List<String[]> interactionFactorLists = new ArrayList<String[]>();
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils.buildDesignMatrix( factors,
                samplesUsed, baselineConditions );

        setupFactors( designMatrix, baselineConditions );

        String modelFormula = "";
        boolean oneSampleTtest = interceptFactor != null && factors.size() == 1;
        if ( oneSampleTtest ) {
            modelFormula = " ";
        } else {
            modelFormula = buildModelFormula( config, label2Factors, interceptFactor, interactionFactorLists );
        }

        // calculate library size before log transformation
        DoubleMatrix1D librarySize = MatrixStats.colSums( dmatrix.getMatrix() );

        dmatrix = ExpressionDataDoubleMatrixUtil.filterAndLogTransform( quantitationType, dmatrix );
        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();

        if ( log.isDebugEnabled() ) outputForDebugging( dmatrix, designMatrix );

        /*
         * PREPARATION FOR 'NATIVE' FITTING
         */
        DoubleMatrix<String, String> sNamedMatrix = makeDataMatrix( designMatrix, namedMatrix );
        DesignMatrix properDesignMatrix = makeDesignMatrix( designMatrix, interactionFactorLists, baselineConditions );

        /*
         * Run the analysis NOTE this can be simplified if we strip out R code.
         */
        final Map<String, LinearModelSummary> rawResults = runAnalysis( namedMatrix, sNamedMatrix, label2Factors,
                modelFormula, properDesignMatrix, interceptFactor, interactionFactorLists, baselineConditions,
                quantitationType, librarySize );

        if ( rawResults.size() == 0 ) {
            log.error( "Got no results from the analysis" );
            return null;
        }

        /*
         * Initialize data structures we need to hold results.
         */

        // this used to be a Set, but a List is much faster.
        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<>();
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<String, List<Double>>();

        for ( String factorName : label2Factors.keySet() ) {
            resultLists.put( factorName, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( factorName, new ArrayList<Double>() );
        }

        for ( String[] fs : interactionFactorLists ) {
            String intF = StringUtils.join( fs, ":" );
            resultLists.put( intF, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( intF, new ArrayList<Double>() );
        }

        if ( pvaluesForQvalue.isEmpty() ) {
            log.warn( "No results were obtained for the current stage of analysis." );
            return null;
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        boolean warned = false;
        int notUsable = 0;
        int processed = 0;
        for ( CompositeSequence el : namedMatrix.getRowNames() ) {

            if ( ++processed % 15000 == 0 ) {
                log.info( "Processed results for " + processed + " elements ..." );
            }

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            if ( log.isDebugEnabled() ) log.debug( el.getName() + "\n" + lm );

            if ( lm == null ) {
                if ( !warned ) {
                    log.warn( "No result for " + el + ", further warnings suppressed" );
                    warned = true;
                }
                notUsable++;
                continue;
            }

            for ( String factorName : label2Factors.keySet() ) {

                if ( !pvaluesForQvalue.containsKey( factorName ) ) {
                    // was dropped.
                    continue;
                }

                Double overallPValue = null;
                DifferentialExpressionAnalysisResult probeAnalysisResult = DifferentialExpressionAnalysisResult.Factory
                        .newInstance();
                probeAnalysisResult.setProbe( el );

                if ( lm.getCoefficients() == null ) {
                    // probeAnalysisResult.setPvalue( null );
                    // pvaluesForQvalue.get( factorName ).add( overallPValue );
                    // resultLists.get( factorName ).add( probeAnalysisResult );
                    notUsable++;
                    continue;
                }

                Collection<ExperimentalFactor> factorsForName = label2Factors.get( factorName );

                if ( factorsForName.size() > 1 ) {
                    /*
                     * Interactions
                     */
                    if ( factorsForName.size() > 2 ) {
                        log.error( "Handling more than two-way interactions is not implemented" );
                        return null;
                    }

                    assert factorName.contains( ":" );
                    String[] factorNames = StringUtils.split( factorName, ":" );
                    assert factorNames.length == factorsForName.size();
                    overallPValue = lm.getInteractionEffectP( factorNames );

                    if ( overallPValue != null && !Double.isNaN( overallPValue ) ) {

                        Map<String, Double> interactionContrastTStats = lm.getContrastTStats( factorName );
                        Map<String, Double> interactionContrastCoeffs = lm.getContrastCoefficients( factorName );
                        Map<String, Double> interactionContrastPValues = lm.getContrastPValues( factorName );

                        for ( String term : interactionContrastPValues.keySet() ) {
                            Double contrastPvalue = interactionContrastPValues.get( term );

                            makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                    interactionContrastTStats, interactionContrastCoeffs );

                        }
                    } else {
                        if ( !warned ) {
                            log.warn( "Interaction could not be computed for " + el + ", further warnings suppressed" );
                            warned = true;
                        }

                        if ( log.isDebugEnabled() )
                            log.debug( "Interaction could not be computed for " + el + ", further warnings suppressed" );

                        notUsable++; // will over count?
                        continue;
                    }

                } else {

                    /*
                     * Main effect
                     */

                    assert factorsForName.size() == 1;
                    ExperimentalFactor experimentalFactor = factorsForName.iterator().next();

                    if ( interceptFactor != null && factorsForName.size() == 1
                            && experimentalFactor.equals( interceptFactor ) ) {
                        overallPValue = lm.getInterceptP();
                    } else {
                        overallPValue = lm.getMainEffectP( factorName );
                    }

                    /*
                     * Add contrasts unless overallpvalue is NaN
                     */
                    if ( overallPValue != null && !Double.isNaN( overallPValue ) ) {

                        Map<String, Double> mainEffectContrastTStats = lm.getContrastTStats( factorName );
                        Map<String, Double> mainEffectContrastPvalues = lm.getContrastPValues( factorName );
                        Map<String, Double> mainEffectContrastCoeffs = lm.getContrastCoefficients( factorName );

                        for ( String term : mainEffectContrastPvalues.keySet() ) {

                            Double contrastPvalue = mainEffectContrastPvalues.get( term );

                            makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                    mainEffectContrastTStats, mainEffectContrastCoeffs );

                        }
                    } else {
                        if ( !warned ) {
                            log.warn( "ANOVA could not be done for " + experimentalFactor + " on " + el
                                    + ", further warnings suppressed" );
                            warned = true;
                        }

                        if ( log.isDebugEnabled() )
                            log.debug( "ANOVA could not be done for " + experimentalFactor + " on " + el );

                        notUsable++; // will over count?
                        continue;
                    }
                }

                assert !Double.isNaN( overallPValue ) : "We should not be keeping non-number pvalues (null or NaNs)";

                probeAnalysisResult.setPvalue( nan2Null( overallPValue ) );
                pvaluesForQvalue.get( factorName ).add( overallPValue );
                resultLists.get( factorName ).add( probeAnalysisResult );
            } // over terms

        } // over probes

        if ( notUsable > 0 ) {
            log.info( notUsable + " elements or results were not usable - model could not be fit, etc." );
        }

        getRanksAndQvalues( resultLists, pvaluesForQvalue );

        DifferentialExpressionAnalysis expressionAnalysis = makeAnalysisEntity( bioAssaySet, config, label2Factors,
                baselineConditions, interceptFactor, interactionFactorLists, oneSampleTtest, resultLists,
                subsetFactorValue );

        log.info( "Analysis processing phase done ..." );

        return expressionAnalysis;
    }

    /**
     * @param samplesUsed
     * @param factors
     * @param baselineConditions
     */
    private void dropIncompleteFactors( List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        Collection<ExperimentalFactor> toDrop = new HashSet<>();
        for ( ExperimentalFactor f : factors ) {
            if ( !ExperimentalDesignUtils.isComplete( f, samplesUsed, baselineConditions ) ) {
                toDrop.add( f );
                log.info( "Droppipng " + f + ", missing values" );
            }
        }
        factors.removeAll( toDrop );

    }

    /**
     * Remove all configurations that have to do with factors that aren't in the selected factors.
     * 
     * @param samplesInSubset
     * @param factors the factors that will be included
     * @param config
     * @return an updated config; the baselines are cleared; subset is cleared; interactions are only kept if they only
     *         involve the given factors.
     */
    private DifferentialExpressionAnalysisConfig fixConfigForSubset( List<ExperimentalFactor> factors,
            DifferentialExpressionAnalysisConfig config ) {

        DifferentialExpressionAnalysisConfig newConfig = new DifferentialExpressionAnalysisConfig();
        //
        // /*
        // * Drop factors that are constant in the subset.
        // */
        // Map<ExperimentalFactor, Collection<FactorValue>> ef2FvsUsedInSubset = new HashMap<ExperimentalFactor,
        // Collection<FactorValue>> ();
        // for ( BioMaterial bm : samplesInSubset ) {
        // for ( FactorValue fv : bm.getFactorValues() ) {
        // ExperimentalFactor ef = fv.getExperimentalFactor();
        // if ( !ef2FvsUsedInSubset.containsKey( ef ) ) {
        // ef2FvsUsedInSubset.put( ef, new HashSet<FactorValue>() );
        // }
        // ef2FvsUsedInSubset.get( ef ).add( fv );
        // }
        // }
        //
        // Collection<ExperimentalFactor> efsToUse = new HashSet<ExperimentalFactor>();
        // for ( ExperimentalFactor ef : factors ) {
        // Collection<FactorValue> fvsUsed = ef2FvsUsedInSubset.get( ef );
        // if ( fvsUsed.size() > 1 ) {
        // efsToUse.add( ef );
        // }
        // }

        newConfig.setBaseLineFactorValues( null );

        if ( !config.getInteractionsToInclude().isEmpty() ) {
            Collection<Collection<ExperimentalFactor>> newInteractionsToInclude = new HashSet<Collection<ExperimentalFactor>>();
            for ( Collection<ExperimentalFactor> interactors : config.getInteractionsToInclude() ) {
                if ( factors.containsAll( interactors ) ) {
                    newInteractionsToInclude.add( interactors );
                }
            }

            newConfig.setInteractionsToInclude( newInteractionsToInclude );
        }

        newConfig.setSubsetFactor( null );
        newConfig.setFactorsToInclude( factors );
        newConfig.setQvalueThreshold( config.getQvalueThreshold() );

        return newConfig;

    }

    /**
     * Remove factors which are no longer usable, based on the subset.
     * 
     * @param expressionDataDoubleMatrix
     * @param eesubSet
     * @param factors
     * @return
     */
    private Collection<ExperimentalFactor> fixFactorsForSubset( ExpressionDataDoubleMatrix dmatrix,
            ExpressionExperimentSubSet eesubSet, List<ExperimentalFactor> factors ) {

        List<ExperimentalFactor> result = new ArrayList<ExperimentalFactor>();

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        ExperimentalFactor interceptFactor = determineInterceptFactor( factors, quantitationType );

        /*
         * Remove any constant factors, unless they are that intercept.
         */
        for ( ExperimentalFactor f : factors ) {

            if ( f.getType().equals( FactorType.CONTINUOUS ) ) {
                result.add( f );
            } else if ( interceptFactor != null && interceptFactor.equals( f ) ) {
                result.add( f );
            } else {

                Collection<FactorValue> levels = new HashSet<FactorValue>();
                for ( BioAssay ba : eesubSet.getBioAssays() ) {
                    BioMaterial bm = ba.getSampleUsed();
                    for ( FactorValue fv : bm.getFactorValues() ) {
                        if ( fv.getExperimentalFactor().equals( f ) ) {
                            levels.add( fv );
                        }
                    }
                }

                if ( levels.size() > 1 ) {
                    result.add( f );
                } else {
                    log.info( "Dropping " + f + " from subset" );
                }

            }

        }

        return result;

    }

    /**
     * Needed to compute the number of genes tested/detected.
     * 
     * @param resultLists
     * @return
     */
    private Map<CompositeSequence, Collection<Gene>> getProbeToGeneMap(
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists ) {
        Map<CompositeSequence, Collection<Gene>> result = new HashMap<CompositeSequence, Collection<Gene>>();

        for ( Collection<DifferentialExpressionAnalysisResult> resultList : resultLists.values() ) {
            for ( DifferentialExpressionAnalysisResult d : resultList ) {
                CompositeSequence probe = d.getProbe();
                result.put( probe, new HashSet<Gene>() );
            }
        }

        // testing environment, etc.
        if ( result.isEmpty() ) {
            return new HashMap<CompositeSequence, Collection<Gene>>();
        }

        return compositeSequenceService.getGenes( result.keySet() );

    }

    /**
     * Fill in the ranks and qvalues in the results.
     * 
     * @param resultLists
     * @param pvaluesForQvalue Map of factorName to results.
     */
    private void getRanksAndQvalues(
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists,
            Map<String, List<Double>> pvaluesForQvalue ) {
        /*
         * qvalues and ranks, requires second pass over the result objects.
         */
        for ( String fName : pvaluesForQvalue.keySet() ) {
            Collection<Double> pvals = pvaluesForQvalue.get( fName );

            if ( pvals.isEmpty() ) {
                log.warn( "No pvalues for " + fName + ", ignoring." );
                continue;
            }
            log.info( pvals.size() + " pvalues for " + fName );

            Double[] pvalArray = pvals.toArray( new Double[] {} );
            for ( int i = 0; i < pvalArray.length; i++ ) {

                assert pvalArray[i] != null;
                assert !Double.isNaN( pvalArray[i] );

                // if ( Double.isNaN( pvalArray[i] ) ) {
                // log.warn( "Pvalue was NaN" );
                // }
                //
                // if ( pvalArray[i] == null ) {
                // log.warn( "Pvalue was null" );
                // pvalArray[i] = Double.NaN;
                // }

            }

            double[] ranks = super.computeRanks( ArrayUtils.toPrimitive( pvalArray ) );

            if ( ranks == null ) {
                log.error( "Ranks could not be computed " + fName );
                continue;
            }

            assert pvalArray.length == resultLists.get( fName ).size() : pvalArray.length + " != "
                    + resultLists.get( fName ).size();

            assert pvalArray.length == ranks.length;

            double[] qvalues = super.benjaminiHochberg( pvalArray );

            assert qvalues.length == resultLists.get( fName ).size();

            int i = 0;
            for ( DifferentialExpressionAnalysisResult pr : resultLists.get( fName ) ) {
                pr.setCorrectedPvalue( nan2Null( qvalues[i] ) );
                pr.setRank( nan2Null( ranks[i] ) );
                i++;
            }
        }
    }

    /**
     * @param factors
     * @return
     */
    private Map<String, Collection<ExperimentalFactor>> getRNames( List<ExperimentalFactor> factors ) {
        final Map<String, Collection<ExperimentalFactor>> label2Factors = new LinkedHashMap<String, Collection<ExperimentalFactor>>();
        for ( ExperimentalFactor experimentalFactor : factors ) {
            label2Factors.put( ExperimentalDesignUtils.nameForR( experimentalFactor ),
                    new HashSet<ExperimentalFactor>() );
            label2Factors.get( ExperimentalDesignUtils.nameForR( experimentalFactor ) ).add( experimentalFactor );
        }
        return label2Factors;
    }

    /**
     * @param bioAssaySet
     * @param config
     * @param label2Factors
     * @param baselineConditions
     * @param interceptFactor
     * @param interactionFactorLists
     * @param oneSampleTtest
     * @param resultLists
     * @param subsetFactorValue
     * @return Analysis (nonpersistent)
     */
    private DifferentialExpressionAnalysis makeAnalysisEntity( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, ExperimentalFactor interceptFactor,
            List<String[]> interactionFactorLists, boolean oneSampleTtest,
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists,
            FactorValue subsetFactorValue ) {

        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( bioAssaySet );
        /*
         * Complete analysis config
         */
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "Linear model with "
                + config.getFactorsToInclude().size()
                + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" )
                + ( interactionFactorLists.isEmpty() ? "" : " with interaction" )
                + ( subsetFactorValue == null ? "" : "Using subset " + bioAssaySet + " subset value= "
                        + subsetFactorValue ) );
        expressionAnalysis.setSubsetFactorValue( subsetFactorValue );

        Collection<ExpressionAnalysisResultSet> resultSets = makeResultSets( label2Factors, baselineConditions,
                oneSampleTtest, expressionAnalysis, resultLists );

        expressionAnalysis.setResultSets( resultSets );

        return expressionAnalysis;
    }

    /**
     * Add a contrast to the given result.
     * 
     * @param probeAnalysisResult
     * @param experimentalFactor
     * @param term
     * @param factorName
     * @param contrastPvalue
     * @param tstats
     * @param coeffs
     */
    private void makeContrast( DifferentialExpressionAnalysisResult probeAnalysisResult,
            Collection<ExperimentalFactor> experimentalFactors, String term, String factorName, Double contrastPvalue,
            Map<String, Double> tstats, Map<String, Double> coeffs ) {

        assert experimentalFactors.size() == 1 || experimentalFactors.size() == 2;

        Double contrastTstat = tstats.get( term );
        Double coefficient = coeffs.get( term );

        // no reason to store this. Note: Tiny coefficient could also be treated as a missing value, but hopefully such
        // situations would have been dealt with at a lower level.
        if ( coefficient == null || contrastTstat == null ) {
            return;
        }

        ContrastResult contrast = ContrastResult.Factory.newInstance();
        contrast.setPvalue( nan2Null( contrastPvalue ) );
        contrast.setTstat( nan2Null( contrastTstat ) );
        contrast.setCoefficient( nan2Null( coefficient ) );

        List<ExperimentalFactor> factorList = new ArrayList<ExperimentalFactor>( experimentalFactors );
        boolean isInteraction = false;
        if ( factorList.size() == 2 ) {
            isInteraction = true;
        }

        /*
         * The coefficient can be treated as fold-change if the data are log-transformed. This is because the
         * coefficient in the contrast is the (fitted;estimated) difference between the means, and log(x) - log(y) =
         * log(x/y). Limma uses this same trick.
         */
        contrast.setLogFoldChange( nan2Null( coefficient ) );

        if ( term.contains( ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX ) ) {
            // otherwise, it's continuous, and
            // we don't put in a
            // factorvalue.

            String[] terms = new String[2];
            String[] factorNames = new String[2];
            if ( term.contains( ":" ) ) {
                terms = StringUtils.split( term, ":" );
                factorNames = StringUtils.split( factorName, ":" );
            } else {
                terms[0] = term;
                factorNames[0] = factorName;
            }

            String firstTerm = terms[0];
            String secondTerm = terms[1];

            Long factorValueId = null;

            try {
                factorValueId = Long.parseLong( firstTerm.replace( factorNames[0]
                        + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Failed to parse: " + firstTerm + " into a factorvalue id" );
            }

            for ( ExperimentalFactor f : factorList ) {
                for ( FactorValue fv : f.getFactorValues() ) {
                    if ( fv.getId().equals( factorValueId ) ) {
                        contrast.setFactorValue( fv );
                        break;
                    }
                }
            }

            assert contrast.getFactorValue() != null;

            if ( isInteraction ) {
                log.debug( "Interaction term" );
                assert secondTerm != null;

                try {
                    factorValueId = Long.parseLong( secondTerm.replace( factorNames[1]
                            + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
                } catch ( NumberFormatException e ) {
                    throw new RuntimeException( "Failed to parse: " + secondTerm + " into a factorvalue id" );
                }

                for ( ExperimentalFactor f : factorList ) {
                    for ( FactorValue fv : f.getFactorValues() ) {
                        if ( fv.getId().equals( factorValueId ) ) {
                            contrast.setSecondFactorValue( fv );
                            break;
                        }
                    }
                }

                if ( contrast.getSecondFactorValue() == null ) {
                    throw new IllegalStateException( "Failed to get interaction contrast second factorvalue" );
                }
            }

            if ( contrast.getFactorValue() == null ) {
                throw new IllegalStateException( "Failed to get contrast factorvalue" );
            }

            if ( contrast.getSecondFactorValue() != null
                    && contrast.getSecondFactorValue().equals( contrast.getFactorValue() ) ) {
                throw new IllegalStateException(
                        "Contrast for interactions must be for two different factor values, got the same one twice" );
            }
        }

        probeAnalysisResult.getContrasts().add( contrast );

    }

    /**
     * @param designMatrix
     * @param namedMatrix
     * @return
     */
    private DoubleMatrix<String, String> makeDataMatrix( ObjectMatrix<String, String, Object> designMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {
        /*
         * Convert the data into a string-keyed matrix.
         */
        DoubleMatrix<String, String> sNamedMatrix = new DenseDoubleMatrix<String, String>( namedMatrix.asArray() );
        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            sNamedMatrix.addRowName( namedMatrix.getRowName( i ).getId().toString() );
        }
        sNamedMatrix.setColumnNames( designMatrix.getRowNames() );
        return sNamedMatrix;
    }

    /**
     * @param designMatrix
     * @param interactionFactorLists
     * @param baselineConditions
     * @return
     */
    private DesignMatrix makeDesignMatrix( ObjectMatrix<String, String, Object> designMatrix,
            List<String[]> interactionFactorLists, Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        /*
         * Determine the factors and interactions to include.
         */
        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        if ( !( interactionFactorLists == null ) && !interactionFactorLists.isEmpty() ) {
            for ( String[] in : interactionFactorLists ) {
                // we actually only support (tested etc.) one interaction at a time, but this should actually work for
                // multiple
                properDesignMatrix.addInteraction( in );
            }
        }

        for ( ExperimentalFactor ef : baselineConditions.keySet() ) {
            if ( ExperimentalDesignUtils.isContinuous( ef ) ) {
                continue;
            }
            String factorName = ExperimentalDesignUtils.nameForR( ef );
            String baselineFactorValue = ExperimentalDesignUtils.nameForR( baselineConditions.get( ef ), true );

            /*
             * If this is a subset, it is possible the baseline chosen is not eligible for the subset.
             */
            log.info( ef );

            assert baselineConditions.get( ef ).getExperimentalFactor().equals( ef ) : baselineConditions.get( ef )
                    + " is not a value of " + ef;
            properDesignMatrix.setBaseline( factorName, baselineFactorValue );
        }
        return properDesignMatrix;
    }

    /**
     * @param label2Factors
     * @param baselineConditions
     * @param oneSampleTtest
     * @param expressionAnalysis
     * @param resultLists
     * @return
     */
    private Collection<ExpressionAnalysisResultSet> makeResultSets(
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, boolean oneSampleTtest,
            DifferentialExpressionAnalysis expressionAnalysis,
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists ) {

        Map<CompositeSequence, Collection<Gene>> probeToGeneMap = getProbeToGeneMap( resultLists );

        /*
         * Result sets
         */
        log.info( "Processing " + resultLists.size() + " resultSets" );
        // StopWatch timer = new StopWatch();
        // timer.start();
        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        for ( String fName : resultLists.keySet() ) {
            Collection<DifferentialExpressionAnalysisResult> results = resultLists.get( fName );

            Collection<ExperimentalFactor> factorsUsed = new HashSet<ExperimentalFactor>();
            factorsUsed.addAll( label2Factors.get( fName ) );

            FactorValue baselineGroup = null;
            if ( !oneSampleTtest && factorsUsed.size() == 1 /* not interaction */) {
                ExperimentalFactor factor = factorsUsed.iterator().next();
                assert baselineConditions.containsKey( factor );
                baselineGroup = baselineConditions.get( factor );
            }

            Collection<HitListSize> hitListSizes = computeHitListSizes( results, probeToGeneMap );

            int numberOfProbesTested = results.size();
            int numberOfGenesTested = getNumberOfGenesTested( results, probeToGeneMap );

            // make List into Set
            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( factorsUsed,
                    numberOfProbesTested, numberOfGenesTested, null, baselineGroup,
                    new HashSet<DifferentialExpressionAnalysisResult>( results ), expressionAnalysis, null /*
                                                                                                            * pvalue
                                                                                                            * dists
                                                                                                            */,
                    hitListSizes );
            resultSets.add( resultSet );

            log.info( "Finished with result set for " + fName );

        }
        return resultSets;
    }

    /**
     * @param config
     * @param dmatrix
     * @param samplesUsed
     * @param subsetFactor
     * @return
     */
    private Map<FactorValue, ExpressionDataDoubleMatrix> makeSubSets( DifferentialExpressionAnalysisConfig config,
            ExpressionDataDoubleMatrix dmatrix, List<BioMaterial> samplesUsed, ExperimentalFactor subsetFactor ) {
        if ( subsetFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            throw new IllegalArgumentException( "You cannot subset on a continuous factor (has a Measurement)" );
        }

        if ( config.getFactorsToInclude().contains( subsetFactor ) ) {
            throw new IllegalArgumentException(
                    "You cannot analyze a factor and use it for subsetting at the same time." );
        }

        Map<FactorValue, List<BioMaterial>> subSetSamples = new HashMap<FactorValue, List<BioMaterial>>();
        for ( FactorValue fv : subsetFactor.getFactorValues() ) {
            assert fv.getMeasurement() == null;
            subSetSamples.put( fv, new ArrayList<BioMaterial>() );
        }

        for ( BioMaterial sample : samplesUsed ) {
            boolean ok = false;
            for ( FactorValue fv : sample.getFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( subsetFactor ) ) {
                    subSetSamples.get( fv ).add( sample );
                    ok = true;
                    break;
                }
            }
            if ( !ok ) {
                throw new IllegalArgumentException(
                        "Cannot subset on a factor unless each sample has a value for it. Missing value for: " + sample
                                + " " + sample.getBioAssaysUsedIn() );
            }
        }

        Map<FactorValue, ExpressionDataDoubleMatrix> subMatrices = new HashMap<FactorValue, ExpressionDataDoubleMatrix>();
        for ( FactorValue fv : subSetSamples.keySet() ) {
            List<BioMaterial> samplesInSubset = subSetSamples.get( fv );

            if ( samplesInSubset.isEmpty() ) {
                throw new IllegalArgumentException( "The subset was empty for fv: " + fv );
            }
            assert samplesInSubset.size() < samplesUsed.size();

            samplesInSubset = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesInSubset,
                    config.getFactorsToInclude() );
            ExpressionDataDoubleMatrix subMatrix = new ExpressionDataDoubleMatrix( samplesInSubset, dmatrix );
            subMatrices.put( fv, subMatrix );
        }

        return subMatrices;

    }

    /**
     * Important bit. Run the analysis
     * 
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param interactionFactorLists
     * @param interceptFactor
     * @param designMatrix
     * @param baselineConditions
     * @param quantitationType
     * @return results
     */
    private Map<String, LinearModelSummary> runAnalysis(
            final DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix,
            final DoubleMatrix<String, String> sNamedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, final String modelFormula,
            DesignMatrix designMatrix, ExperimentalFactor interceptFactor, List<String[]> interactionFactorLists,
            Map<ExperimentalFactor, FactorValue> baselineConditions, QuantitationType quantitationType,
            final DoubleMatrix1D librarySize ) {

        final Map<String, LinearModelSummary> rawResults = new ConcurrentHashMap<String, LinearModelSummary>();

        Future<?> f = runAnalysisFuture( designMatrix, sNamedMatrix, rawResults, quantitationType, librarySize );

        StopWatch timer = new StopWatch();
        timer.start();
        long lasttime = 0;

        // this analysis should take just 10 or 20 seconds for most data sets.
        double MAX_ANALYSIS_TIME = 60 * 1000 * 20; // 20 minutes.
        double updateIntervalMillis = 60 * 1000;// 1 minute
        while ( !f.isDone() ) {
            try {
                Thread.sleep( 1000 );

                if ( timer.getTime() - lasttime > updateIntervalMillis ) {
                    log.info( String.format( "Analysis running, %.1f minutes elapsed ...", timer.getTime() / 60000.00 ) );
                    lasttime = timer.getTime();
                }

            } catch ( InterruptedException e ) {
                log.warn( "Analysis interrupted!" );
                return rawResults;
            }

            if ( timer.getTime() > MAX_ANALYSIS_TIME ) {
                log.error( "Analysis is taking too long, something bad must have happened; cancelling" );
                f.cancel( true );
                throw new RuntimeException( "Analysis was taking too long, it was cancelled" );
            }
        }

        if ( timer.getTime() > updateIntervalMillis ) {
            log.info( String.format( "Analysis finished in %.1f minutes.", timer.getTime() / 60000.00 ) );
        }

        try {
            f.get();
        } catch ( InterruptedException e ) {
            log.warn( "Job was interrupted" );
            return rawResults;
        } catch ( ExecutionException e ) {
            throw new RuntimeException( e );
        }

        assert rawResults.size() == namedMatrix.rows() : "expected " + namedMatrix.rows() + " results, got "
                + rawResults.size();
        return rawResults;
    }

    /**
     * Linear models solved
     * 
     * @param designMatrix
     * @param data
     * @param rawResults
     * @param quantitationType
     * @return
     */
    private Future<?> runAnalysisFuture( final DesignMatrix designMatrix, final DoubleMatrix<String, String> data,
            final Map<String, LinearModelSummary> rawResults, final QuantitationType quantitationType,
            final DoubleMatrix1D librarySize ) {
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> f = service.submit( new Runnable() {
            @Override
            public void run() {
                StopWatch timer = new StopWatch();
                timer.start();
                LeastSquaresFit fit = null;
                if ( quantitationType.getScale().equals( ScaleType.COUNT ) ) {
                    MeanVarianceEstimator mv = new MeanVarianceEstimator( designMatrix, data, librarySize );
                    log.info( "Model weights from mean-variance model: " + timer.getTime() + "ms" );
                    timer.reset();
                    timer.start();
                    fit = new LeastSquaresFit( designMatrix, data, mv.getWeights() );
                } else {
                    fit = new LeastSquaresFit( designMatrix, data );
                }
                log.info( "Model fit data matrix " + data.rows() + " x " + data.columns() + ": " + timer.getTime()
                        + "ms" );
                timer.reset();
                timer.start();
                Map<String, LinearModelSummary> res = fit.summarizeByKeys( true );
                log.info( "Model summarize/ANOVA: " + timer.getTime() + "ms" );
                rawResults.putAll( res );
                log.info( "Analysis phase done ..." );
            }
        } );

        service.shutdown();
        return f;
    }
}
