/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.preprocess.filter;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.analysis.preprocess.filter.InsufficientSamplesException;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.arrayDesign.TechnologyTypeEnum;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Methods to handle filtering expression experiments for analysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionExperimentFilter {

    private static Log log = LogFactory.getLog( ExpressionExperimentFilter.class.getName() );
    private final FilterConfig config;

    /**
     * @param ee
     * @param arrayDesignsUsed
     */
    public ExpressionExperimentFilter( ExpressionExperiment ee, Collection<ArrayDesign> arrayDesignsUsed,
            FilterConfig config ) {
        this.ee = ee;
        this.arrayDesignsUsed = arrayDesignsUsed;
        this.config = config;
    }

    /**
     * Apply filters as configured by the command line parameters and technology type.
     * 
     * @param dataMatrix
     * @param eeDoubleMatrix
     * @param ee
     * @return A data matrix in which filters have been applied and missing values (in the PRESENTABSENT quantitation
     *         type, if present) are masked
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix,
            ExpressionDataMatrixBuilder builder ) {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        boolean twoColor = isTwoColor();
        if ( config.isMinPresentFractionIsSet() && twoColor ) {
            /* Apply two color missing value filter */
            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData( null );
            filteredMatrix = minPresentFilter( filteredMatrix, missingValues );
            builder.maskMissingValues( filteredMatrix, null );
        }

        if ( !twoColor ) {

            if ( config.isMinPresentFractionIsSet() ) filteredMatrix = minPresentFilter( filteredMatrix, null );

            if ( config.isLowExpressionCutIsSet() ) filteredMatrix = lowExpressionFilter( eeDoubleMatrix );

            if ( usesAffymetrix() ) filteredMatrix = affyControlProbeFilter( filteredMatrix );
        }
        return filteredMatrix;
    }

    Collection<ArrayDesign> arrayDesignsUsed;
    ExpressionExperiment ee;

    /**
     * Determine if the expression experiment uses two-color arrays. This is not guaranteed to give the right answer if
     * the experiment uses both types of technologies.F
     * 
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean isTwoColor() {
        ArrayDesign arrayDesign = ( ArrayDesign ) arrayDesignsUsed.iterator().next();
        TechnologyType techType = arrayDesign.getTechnologyType();
        return techType.equals( TechnologyTypeEnum.TWOCOLOR ) || techType.equals( TechnologyType.DUALMODE );
    }

    @SuppressWarnings("unchecked")
    private boolean usesAffymetrix() {
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        return arrayDesign.getName().toUpperCase().contains( "AFFYMETRIX" );
    }

    /**
     * @param ee
     * @param filteredMatrix
     * @param arrayDesign
     * @return
     */
    private ExpressionDataDoubleMatrix affyControlProbeFilter( ExpressionDataDoubleMatrix matrix ) {
        AffyProbeNameFilter affyProbeNameFilter = new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } );
        return affyProbeNameFilter.filter( matrix );
    }

    /**
     * @param ee
     * @param builder
     * @param eeDoubleMatrix
     * @param arrayDesignsUsed
     * @return
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataMatrixBuilder builder,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( eeDoubleMatrix.rows() == 0 ) throw new IllegalStateException( "No data found!" );

        if ( eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER )
            throw new IllegalArgumentException( "To few rows in " + ee.getShortName() + " (" + eeDoubleMatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + FilterConfig.MINIMUM_ROWS_TO_BOTHER
                    + " rows" );

        if ( eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE )
            throw new InsufficientSamplesException( "Not enough samples " + ee.getShortName() + ", must have at least "
                    + FilterConfig.MINIMUM_SAMPLE + " to be eligble for link analysis." );

        eeDoubleMatrix = this.filter( eeDoubleMatrix, builder );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix, it was null " + ee.getShortName() );
        return eeDoubleMatrix;
    }

    /**
     * @param ee
     * @param dataVectors
     * @return
     */
    public ExpressionDataDoubleMatrix getFilteredMatrix( Collection<DesignElementDataVector> dataVectors ) {
        log.info( "Getting expression data..." );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( dataVectors );

        ExpressionDataDoubleMatrix eeDoubleMatrix = builder.getPreferredData();

        eeDoubleMatrix = filter( builder, eeDoubleMatrix );
        return eeDoubleMatrix;
    }

    /**
     * @param matrix
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setLowCut( config.getLowExpressionCut() );
        rowLevelFilter.setHighCut( config.getHighExpressionCut() );
        rowLevelFilter.setRemoveAllNegative( true );
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * @param matrix
     * @return
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix matrix,
            ExpressionDataBooleanMatrix absentPresent ) {
        log.info( "Filtering out genes that are missing too many values" );

        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        if ( absentPresent != null ) {
            if ( absentPresent.rows() != matrix.rows() ) {
                log.warn( "Missing value matrix has " + absentPresent.rows() + " rows (!=" + matrix.rows() + ")" );
            }

            if ( absentPresent.columns() != matrix.columns() ) {
                throw new IllegalArgumentException( "Missing value matrix has " + absentPresent.columns()
                        + " columns (!=" + matrix.columns() + ")" );
            }

            rowMissingFilter.setAbsentPresentCalls( absentPresent );
        }
        rowMissingFilter.setMinPresentFraction( config.getMinPresentFraction() );
        return ( ExpressionDataDoubleMatrix ) rowMissingFilter.filter( matrix );
    }

}
