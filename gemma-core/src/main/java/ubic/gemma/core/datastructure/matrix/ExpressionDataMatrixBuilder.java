/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ChannelUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Utility methods for taking an ExpressionExperiment and returning various types of ExpressionDataMatrices, such as the
 * processed data, preferred data, background, etc. This class is not database aware; use the
 * ExpressionDataMatrixService to get ready-to-use matrices starting from an ExpressionExperiment.
 * This handles complexities such as experiments that contain multiple array designs with differing quantitation types.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class ExpressionDataMatrixBuilder {

    private static final Log log = LogFactory.getLog( ExpressionDataMatrixBuilder.class.getName() );
    private static final double LOGARITHM_BASE = 2.0;
    private final Map<ArrayDesign, BioAssayDimension> dimMap = new HashMap<>();
    private final Map<QuantitationType, Integer> numMissingValues = new HashMap<>();
    private final Collection<BulkExpressionDataVector> vectors;
    private ExpressionExperiment expressionExperiment;
    private Collection<ProcessedExpressionDataVector> processedDataVectors = new HashSet<>();
    private QuantitationTypeData dat = null;
    private boolean anyMissing = false;

    /**
     * @param vectors collection of vectors. They should be thawed first.
     */
    public ExpressionDataMatrixBuilder( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() )
            throw new IllegalArgumentException( "No vectors" );
        this.vectors = new HashSet<>();
        this.vectors.addAll( vectors );

        for ( DesignElementDataVector vec : vectors ) {
            if ( vec instanceof ProcessedExpressionDataVector ) {
                this.processedDataVectors.add( ( ProcessedExpressionDataVector ) vec );
            }
        }

        this.expressionExperiment = vectors.iterator().next().getExpressionExperiment();
    }

    public ExpressionDataMatrixBuilder( Collection<ProcessedExpressionDataVector> processedVectors,
            Collection<? extends BulkExpressionDataVector> otherVectors ) {
        this.vectors = new HashSet<>();
        this.vectors.addAll( otherVectors );
        this.processedDataVectors = processedVectors;
    }

    /**
     * Create a matrix using all the vectors, which are assumed to all be of the same quantitation type.
     *
     * @param vectors raw vectors
     * @return matrix of appropriate type.
     */
    public static BulkExpressionDataMatrix<?> getMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() )
            throw new IllegalArgumentException( "No vectors" );
        PrimitiveType representation = vectors.iterator().next().getQuantitationType().getRepresentation();
        return getMatrix( representation, vectors );
    }

    /**
     * @param representation PrimitiveType
     * @param vectors        raw vectors
     * @return matrix of appropriate type.
     */
    private static BulkExpressionDataMatrix<?> getMatrix( PrimitiveType representation,
            Collection<? extends BulkExpressionDataVector> vectors ) {
        BulkExpressionDataMatrix<?> expressionDataMatrix;
        if ( representation.equals( PrimitiveType.DOUBLE ) ) {
            expressionDataMatrix = new ExpressionDataDoubleMatrix( vectors );
        } else if ( representation.equals( PrimitiveType.STRING ) ) {
            expressionDataMatrix = new ExpressionDataStringMatrix( vectors );
        } else if ( representation.equals( PrimitiveType.INT ) ) {
            expressionDataMatrix = new ExpressionDataIntegerMatrix( vectors );
        } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
            expressionDataMatrix = new ExpressionDataBooleanMatrix( vectors );
        } else {
            throw new UnsupportedOperationException( "Don't know how to deal with matrices of type " + representation );
        }
        return expressionDataMatrix;
    }

    /**
     * @param expressionExperiment (should be lightly thawed)
     * @return a collection of QTs
     */
    public static Collection<QuantitationType> getMissingValueQuantitationTypes(
            ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<>();

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.isEmpty() )
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );

        ExpressionDataMatrixBuilder.log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        for ( QuantitationType qType : eeQtTypes ) {
            if ( qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                ExpressionDataMatrixBuilder.log.debug( "Present/absent=" + qType );
                neededQtTypes.add( qType );
            }
        }

        return neededQtTypes;
    }

    public static Collection<QuantitationType> getPreferredAndMissingQuantitationTypes(
            ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<>();
        neededQtTypes.addAll( ExpressionDataMatrixBuilder.getPreferredQuantitationTypes( expressionExperiment ) );
        neededQtTypes.addAll( ExpressionDataMatrixBuilder.getMissingValueQuantitationTypes( expressionExperiment ) );
        return neededQtTypes;
    }

    public static Collection<QuantitationType> getPreferredQuantitationTypes(
            ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<>();

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.isEmpty() )
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );

        ExpressionDataMatrixBuilder.log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        for ( QuantitationType qType : eeQtTypes ) {
            if ( qType.getIsPreferred() ) {
                ExpressionDataMatrixBuilder.log.debug( "Preferred=" + qType );
                neededQtTypes.add( qType );
            }
        }

        return neededQtTypes;
    }

    /**
     * @param eeQtTypes the QTs
     * @return just the quantitation types that are likely to be 'useful': Preferred, present/absent, signals
     * and background
     * from both channels (if present).
     */
    public static Collection<QuantitationType> getUsefulQuantitationTypes( Collection<QuantitationType> eeQtTypes ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<>();

        for ( QuantitationType qType : eeQtTypes ) {

            String name = qType.getName();
            if ( qType.getIsPreferred() ) {
                ExpressionDataMatrixBuilder.log.info( "Preferred=" + qType );
                neededQtTypes.add( qType );
            } else if ( qType.getIsMaskedPreferred() ) {
                ExpressionDataMatrixBuilder.log.info( "Masked preferred=" + qType );
                neededQtTypes.add( qType );
            } else if ( ChannelUtils.isBackgroundChannelA( name ) ) {
                neededQtTypes.add( qType );
                ExpressionDataMatrixBuilder.log.info( "Background A=" + qType );
            } else if ( ChannelUtils.isBackgroundChannelB( name ) ) {
                neededQtTypes.add( qType );
                ExpressionDataMatrixBuilder.log.info( "Background B=" + qType );
            } else if ( ChannelUtils.isSignalChannelA( name ) ) {
                neededQtTypes.add( qType );
                ExpressionDataMatrixBuilder.log.info( "Signal A=" + qType );
            } else if ( ChannelUtils.isSignalChannelB( name ) ) {
                neededQtTypes.add( qType );
                ExpressionDataMatrixBuilder.log.info( "Signal B=" + qType );
            } else if ( name.matches( "CH1D_MEAN" ) ) {
                /*
                 * Special case. This is the background subtracted channel 1 for GenePix data. It is only needed to
                 * reconstruct CH1 if it isn't present, which is surprisingly common in Stanford data sets
                 */
                neededQtTypes.add( qType );
            } else if ( qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                ExpressionDataMatrixBuilder.log.info( "Present/absent=" + qType );
                neededQtTypes.add( qType );
            }
        }
        return neededQtTypes;
    }

    /**
     * @param expressionExperiment the EE to get the QTs for
     * @return just the quantitation types that are likely to be 'useful': Preferred,
     * present/absent, signals and background
     * from both channels (if present).
     */
    public static Collection<QuantitationType> getUsefulQuantitationTypes( ExpressionExperiment expressionExperiment ) {

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.isEmpty() ) {
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );
        }

        ExpressionDataMatrixBuilder.log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        return ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( eeQtTypes );
    }

    public ExpressionDataDoubleMatrix getBackgroundChannelA() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        List<QuantitationType> qTypes = new ArrayList<>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelA( dimension );
            if ( qType != null )
                qTypes.add( qType );
        }

        if ( !qTypes.isEmpty() ) {
            return this.makeMatrix( qTypes );
        }
        return null;
    }

    public ExpressionDataDoubleMatrix getBackgroundChannelB() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        List<QuantitationType> qTypes = new ArrayList<>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelB( dimension );
            if ( qType != null )
                qTypes.add( qType );
        }

        if ( !qTypes.isEmpty() ) {
            return this.makeMatrix( qTypes );
        }
        return null;
    }

    /**
     * @return a single BioAssayDimension except if there are multiple array designs used in the experiment.
     */
    public List<BioAssayDimension> getBioAssayDimensions() {

        List<BioAssayDimension> result = new ArrayList<>();

        if ( !dimMap.keySet().isEmpty() ) {
            result.addAll( dimMap.values() );
            return result;
        }

        if ( this.vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors, no bioassay dimensions" );
        }

        ExpressionDataMatrixBuilder.log.debug( "Checking all vectors to get bioAssayDimensions" );
        Collection<BioAssayDimension> dimensions = new HashSet<>();
        for ( BulkExpressionDataVector vector : vectors ) {
            ArrayDesign adUsed = this.arrayDesignForVector( vector );
            if ( !dimMap.containsKey( adUsed ) ) {
                dimMap.put( adUsed, vector.getBioAssayDimension() );
            }
            dimensions.add( vector.getBioAssayDimension() );
        }

        ExpressionDataMatrixBuilder.log.debug( "got " + dimensions.size() + " bioAssayDimensions" );
        result.addAll( dimensions );
        return result;
    }

    public ExpressionDataDoubleMatrix getBkgSubChannelA() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBkgSubChannelA( dimension );
            if ( qType != null )
                qTypes.add( qType );
        }

        if ( !qTypes.isEmpty() ) {
            return this.makeMatrix( qTypes );
        }
        return null;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @return Compute an intensity matrix. For two-channel arrays, this is the geometric mean of the
     * background-subtracted
     * signals on the two channels. For two-color arrays, if one channel is missing (as happens sometimes) the
     * intensities returned are just from the one channel. For one-color arrays, this is the same as the
     * preferred data
     * matrix.
     */
    public ExpressionDataDoubleMatrix getIntensity() {
        if ( this.isTwoColor() ) {

            ExpressionDataDoubleMatrix signalA = this.getSignalChannelA();
            ExpressionDataDoubleMatrix signalB = this.getSignalChannelB();
            ExpressionDataDoubleMatrix backgroundA = this.getBackgroundChannelA();
            ExpressionDataDoubleMatrix backgroundB = this.getBackgroundChannelB();

            if ( signalA == null && signalB == null ) {
                ExpressionDataMatrixBuilder.log.warn( "Cannot get signal for either channel" );
                return null;
            }

            if ( backgroundA != null && signalA != null ) {
                subtractMatrices( signalA, backgroundA );
            }

            if ( backgroundB != null && signalB != null ) {
                subtractMatrices( signalB, backgroundB );
            }

            if ( signalA != null ) {
                logTransformMatrix( signalA );
            }

            if ( signalB != null ) {
                logTransformMatrix( signalB );
            }

            if ( signalA != null && signalB != null ) {
                addMatrices( signalA, signalB );
                scalarDivideMatrix( signalA, 2.0 );
            }

            if ( signalA == null ) {
                return signalB;
            }
            return signalA; // now this contains the answer

        }
        return this.getPreferredData();

    }

    /**
     * @return a matrix of booleans, or null if a missing value quantitation type ("absent/present", which may have been
     * computed by our system) is not found. This will return the values whether the array design is two-color
     * or not.
     */
    @Nullable
    public ExpressionDataBooleanMatrix getMissingValueData() {
        List<QuantitationType> qtypes = this.getMissingValueQTypes();
        if ( qtypes.isEmpty() )
            return null;
        return new ExpressionDataBooleanMatrix( vectors, qtypes );
    }

    public Integer getNumMissingValues( QuantitationType qt ) {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();
        return numMissingValues.get( qt );
    }

    /**
     * @return The matrix for the preferred data - NOT the processed data (though they may be the same, in fact)
     */
    public ExpressionDataDoubleMatrix getPreferredData() {

        List<QuantitationType> qtypes = this.getPreferredQTypes();

        if ( qtypes.isEmpty() ) {
            ExpressionDataMatrixBuilder.log.warn( "Could not find a 'preferred' quantitation type" );
            return null;
        }

        return new ExpressionDataDoubleMatrix( this.getPreferredDataVectors(), qtypes );
    }

    public List<QuantitationType> getPreferredQTypes() {
        List<QuantitationType> result = new ArrayList<>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        if ( dimensions.isEmpty() ) {
            throw new IllegalArgumentException( "No bioAssayDimensions!" );
        }

        for ( BioAssayDimension dimension : dimensions ) {
            for ( BulkExpressionDataVector vector : vectors ) {
                if ( !vector.getBioAssayDimension().equals( dimension ) )
                    continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getIsPreferred() && !qType.getIsMaskedPreferred() )
                    continue;
                // if we get here, we're in the right place.
                result.add( qType );
                break; // on to the next dimension.
            }
        }

        return result;
    }

    public ExpressionDataDoubleMatrix getProcessedData() {

        List<QuantitationType> qtypes = this.getPreferredQTypes();

        if ( qtypes.isEmpty() ) {
            ExpressionDataMatrixBuilder.log.warn( "Could not find a 'preferred' quantitation type" );
            return null;
        }

        return new ExpressionDataDoubleMatrix( this.getProcessedDataVectors(), qtypes );
    }

    public Map<CompositeSequence, Double> getRanksByMean() {
        Collection<QuantitationType> qtypes = this.getPreferredQTypes();
        Map<CompositeSequence, Double> ranks = new HashMap<>();

        for ( DesignElementDataVector v : this.vectors ) {
            if ( qtypes.contains( v.getQuantitationType() ) && v instanceof ProcessedExpressionDataVector ) {
                ranks.put( v.getDesignElement(), ( ( ProcessedExpressionDataVector ) v ).getRankByMean() );
            }
        }

        return ranks;
    }

    public ExpressionDataDoubleMatrix getSignalChannelA() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<>();

        for ( BioAssayDimension dimension : dimensions ) {

            QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
            QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
            QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
            QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

            boolean channelANeedsReconstruction = this
                    .checkChannelA( signalChannelA, signalChannelB, backgroundChannelA, bkgSubChannelA );

            if ( channelANeedsReconstruction ) {
                return this.getSignalChannelAFancy( dimension );
            }
            if ( signalChannelA != null )
                qTypes.add( signalChannelA );
        }

        if ( !qTypes.isEmpty() ) {
            return this.makeMatrix( qTypes );
        }
        return null;
    }

    public ExpressionDataDoubleMatrix getSignalChannelB() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getSignalChannelB( dimension );
            if ( qType != null )
                qTypes.add( qType );
        }

        if ( !qTypes.isEmpty() ) {
            return this.makeMatrix( qTypes );
        }
        return null;
    }

    public boolean isAnyMissing() {
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();
        return anyMissing;
    }

    /**
     * add the background values back on.
     *
     * @param signalDataA - already background subtracted
     */
    private void addBackgroundBack( ExpressionDataDoubleMatrix signalDataA, ExpressionDataDoubleMatrix bkgDataA ) {
        for ( int i = 0; i < signalDataA.rows(); i++ ) {
            for ( int j = 0; j < signalDataA.columns(); j++ ) {
                double oldVal = signalDataA.getAsDouble( i, j );
                double bkg = bkgDataA.getAsDouble( i, j );
                signalDataA.set( i, j, oldVal + bkg );
            }
        }
    }

    private ArrayDesign arrayDesignForVector( DesignElementDataVector vector ) {
        return vector.getDesignElement().getArrayDesign();
    }

    /**
     * @return true if channelA needs reconstruction
     */
    private boolean checkChannelA( @Nullable QuantitationType signalChannelA, @Nullable QuantitationType signalChannelB,
            @Nullable QuantitationType backgroundChannelA, @Nullable QuantitationType bkgSubChannelA ) {
        if ( signalChannelA == null || signalChannelB == null ) {

            /*
             * This can happen for some Stanford data sets where the CH1 data was not submitted. But we can sometimes
             * reconstruct the values from the background
             */

            if ( signalChannelB != null && bkgSubChannelA != null && backgroundChannelA != null ) {
                ExpressionDataMatrixBuilder.log.info( "Invoking work-around for missing channel 1 intensities" );
                return true;
            }
            ExpressionDataMatrixBuilder.log
                    .warn( "Could not find signals for both channels: " + "Channel A =" + signalChannelA
                            + ", Channel B=" + signalChannelB + " and backgroundChannelA =" + backgroundChannelA
                            + " and background-subtracted channel A =" + bkgSubChannelA );
            return false;

        }
        return false;
    }

    private List<QuantitationType> getMissingValueQTypes() {
        List<QuantitationType> result = new ArrayList<>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        for ( BioAssayDimension dim : dimensions ) {
            for ( BulkExpressionDataVector vector : vectors ) {

                if ( !vector.getBioAssayDimension().equals( dim ) )
                    continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) )
                    continue;

                // ArrayDesign adUsed = arrayDesignForVector( vector );
                // if ( arrayDesign != null && !adUsed.equals( arrayDesign ) ) continue;

                // if we get here, we're in the right place.
                result.add( qType );
                break; // on to the next dimension.

            }
        }

        return result;
    }

    /**
     * @return The 'preferred' data vectors - NOT the processed data vectors!
     */
    private Collection<BulkExpressionDataVector> getPreferredDataVectors() {
        Collection<BulkExpressionDataVector> result = new HashSet<>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qtypes = this.getPreferredQTypes();

        for ( BulkExpressionDataVector vector : vectors ) {
            if ( !( vector instanceof ProcessedExpressionDataVector ) && dimensions
                    .contains( vector.getBioAssayDimension() ) && qtypes.contains( vector.getQuantitationType() ) )
                result.add( vector );
        }

        return result;
    }

    /**
     * @return Collection of <em>ProcessedExpressionDataVector</em>s.
     */
    private Collection<ProcessedExpressionDataVector> getProcessedDataVectors() {

        if ( this.processedDataVectors != null ) {
            return this.processedDataVectors;
        }

        Collection<ProcessedExpressionDataVector> result = new HashSet<>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qtypes = this.getPreferredQTypes();

        for ( BulkExpressionDataVector vector : vectors ) {
            if ( vector instanceof ProcessedExpressionDataVector && dimensions.contains( vector.getBioAssayDimension() )
                    && qtypes.contains( vector.getQuantitationType() ) )
                result.add( ( ProcessedExpressionDataVector ) vector );
        }
        return result;
    }

    /**
     * @return If there are multiple valid choices, we choose the first one seen, unless a later one has fewer missing
     * value.
     */
    private QuantitationTypeData getQuantitationTypesNeeded() {

        Collection<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        QuantitationTypeData result = new QuantitationTypeData();

        this.populateMissingValueInfo();

        for ( BioAssayDimension targetDimension : dimensions ) {

            Collection<QuantitationType> checkedQts = new HashSet<>();

            for ( BulkExpressionDataVector vector : vectors ) {

                BioAssayDimension dim = vector.getBioAssayDimension();

                if ( !dim.equals( targetDimension ) )
                    continue;

                QuantitationType qType = vector.getQuantitationType();

                if ( checkedQts.contains( qType ) )
                    continue;

                checkedQts.add( qType );

                String name = qType.getName();
                if ( qType.getIsPreferred() && result.getPreferred( dim ) == null ) {
                    result.addPreferred( dim, qType );
                    ExpressionDataMatrixBuilder.log.info( "Preferred=" + qType );
                } else if ( ChannelUtils.isBackgroundChannelA( name ) ) {

                    if ( result.getBackgroundChannelA( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getBackgroundChannelA( dim ) );
                        if ( i < j ) {
                            ExpressionDataMatrixBuilder.log.info( "Found better background A=" + qType );
                            result.addBackgroundChannelA( dim, qType );
                        }
                    } else {
                        result.addBackgroundChannelA( dim, qType );
                        ExpressionDataMatrixBuilder.log.info( "Background A=" + qType );
                    }

                } else if ( ChannelUtils.isBackgroundChannelB( name ) ) {
                    if ( result.getBackgroundChannelB( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getBackgroundChannelB( dim ) );
                        if ( i < j ) {
                            ExpressionDataMatrixBuilder.log.info( "Found better background B=" + qType );
                            result.addBackgroundChannelB( dim, qType );
                        }
                    } else {
                        result.addBackgroundChannelB( dim, qType );
                        ExpressionDataMatrixBuilder.log.info( "Background B=" + qType );
                    }
                } else if ( ChannelUtils.isSignalChannelA( name ) ) {
                    if ( result.getSignalChannelA( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getSignalChannelA( dim ) );
                        if ( i < j ) {
                            ExpressionDataMatrixBuilder.log.info( "Found better Signal A=" + qType );
                            result.addSignalChannelA( dim, qType );
                        }
                    } else {
                        result.addSignalChannelA( dim, qType );
                        ExpressionDataMatrixBuilder.log.info( "Signal A=" + qType );
                    }
                } else if ( ChannelUtils.isSignalChannelB( name ) ) {
                    if ( result.getSignalChannelB( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getSignalChannelB( dim ) );
                        if ( i < j ) {
                            ExpressionDataMatrixBuilder.log.info( "Found better Signal B=" + qType );
                            result.addSignalChannelB( dim, qType );
                        }
                    } else {
                        result.addSignalChannelB( dim, qType );
                        ExpressionDataMatrixBuilder.log.info( "Signal B=" + qType );
                    }
                } else if ( name.matches( "CH1D_MEAN" ) ) {
                    result.addBkgSubChannelA( dim, qType ); // specific for SGD data bug
                }

                if ( !anyMissing && result.getSignalChannelA( dim ) != null && result.getSignalChannelB( dim ) != null
                        && result.getBackgroundChannelA( dim ) != null && result.getBackgroundChannelB( dim ) != null
                        && result.getPreferred( dim ) != null ) {
                    break; // no need to go through them all.
                }
            }
        }
        return result;
    }

    private ExpressionDataDoubleMatrix getSignalChannelAFancy( BioAssayDimension dimension ) {
        boolean channelANeedsReconstruction;

        /*
         * This is made messy by data sets where the non-background-subtracted data has been omitted, but the background
         * values are available.
         */
        if ( dat == null )
            dat = this.getQuantitationTypesNeeded();

        QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
        QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
        QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
        QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

        channelANeedsReconstruction = this
                .checkChannelA( signalChannelA, signalChannelB, backgroundChannelA, bkgSubChannelA );

        ExpressionDataDoubleMatrix signalDataA;
        if ( channelANeedsReconstruction ) {
            ExpressionDataDoubleMatrix bkgDataA = null;
            if ( backgroundChannelA != null ) {
                bkgDataA = new ExpressionDataDoubleMatrix( vectors, backgroundChannelA );
            }

            // use background-subtracted data and add bkg back on
            assert bkgDataA != null;
            assert bkgSubChannelA != null;
            signalDataA = new ExpressionDataDoubleMatrix( vectors, bkgSubChannelA );
            this.addBackgroundBack( signalDataA, bkgDataA );
            return signalDataA;

        }
        return this.getSignalChannelA();

    }

    private boolean isTwoColor() {
        for ( DesignElementDataVector v : vectors ) {
            CompositeSequence d = v.getDesignElement();
            TechnologyType technologyType = d.getArrayDesign().getTechnologyType();

            if ( !technologyType.equals( TechnologyType.TWOCOLOR ) && !technologyType
                    .equals( TechnologyType.DUALMODE ) ) {
                continue;
            }

            QuantitationType qt = v.getQuantitationType();

            if ( ( qt.getIsPreferred() || qt.getIsMaskedPreferred() ) && qt.getIsRatio() ) {
                return true;
            }

        }
        return false;
    }

    @Nullable
    private ExpressionDataDoubleMatrix makeMatrix( List<QuantitationType> qTypes ) {
        if ( !qTypes.isEmpty() ) {
            return new ExpressionDataDoubleMatrix( vectors, qTypes );
        }
        return null;
    }

    private void populateMissingValueInfo() {
        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType qt = vector.getQuantitationType();
            if ( !numMissingValues.containsKey( qt ) ) {
                numMissingValues.put( qt, 0 );
            }

            for ( double d : vector.getDataAsDoubles() ) {
                if ( Double.isNaN( d ) ) {
                    anyMissing = true;
                    numMissingValues.put( qt, numMissingValues.get( qt ) + 1 );
                }
            }
        }
    }

    /**
     * Log-transform the values in the matrix (base 2). Non-positive values (which have no logarithm defined) are
     * entered as NaN.
     *
     * @param matrix matrix
     */
    public static void logTransformMatrix( ExpressionDataDoubleMatrix matrix ) {
        int columns = matrix.columns();
        double log2 = Math.log( LOGARITHM_BASE );
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                if ( valA <= 0 ) {
                    matrix.set( del, bm, Double.NaN );
                } else {
                    matrix.set( del, bm, Math.log( valA ) / log2 );
                }
            }
        }
    }

    /**
     * Subtract two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes
     * missing for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns
     * do not have to be in the same order, but they do have to have the same column keys and row keys (with the
     * exception of missing rows). The result is stored in a. (a - b).
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get subtracted and a warning will be issued.
     *
     * @param a matrix a
     * @param b matrix b
     * @throws IllegalArgumentException if the matrices are not column-conformant.
     */
    private void subtractMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );

        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            int rowNum = el.getIndex();
            CompositeSequence del = el.getDesignElement();

            if ( b.getRowAsDoubles( del ) == null ) {
                log.warn( "Matrix 'b' is missing a row for " + del + ", it will not be subtracted" );
                continue;
            }

            for ( int i = 0; i < columns; i++ ) {
                BioAssay assay = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, assay );
                double valB = b.get( del, assay );
                a.set( rowNum, i, valA - valB );
            }
        }
    }


    /**
     * Add two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes missing
     * for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns do not
     * have to be in the same order, but they do have to have the same column keys and row keys (with the exception of
     * missing rows). The result is stored in a.
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get added and a warning will be issued.
     *
     * @param a matrix a
     * @param b matrix b
     * @throws IllegalArgumentException if the matrices are not column-conformant.
     */
    public void addMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );
        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();

            if ( b.getRowAsDoubles( del ) == null ) {
                log.warn( "Matrix 'b' is missing a row for " + del + ", this row will not be added" );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, bm );
                double valB = b.get( del, bm );
                a.set( del, bm, valA + valB );
            }
        }
    }

    /**
     * Divide all values by the dividend
     *
     * @param matrix matrix
     * @param dividend dividend
     * @throws IllegalArgumentException if dividend == 0.
     */
    public void scalarDivideMatrix( ExpressionDataDoubleMatrix matrix, double dividend ) {
        if ( dividend == 0 ) throw new IllegalArgumentException( "Can't divide by zero" );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                matrix.set( del, bm, valA / dividend );

            }
        }
    }

    /**
     * Helper class that keeps track of which QTs are background, signal and preferred.
     */
    private static class QuantitationTypeData {

        private final Map<BioAssayDimension, QuantitationType> backgroundChannelA = new HashMap<>();
        private final Map<BioAssayDimension, QuantitationType> backgroundChannelB = new HashMap<>();
        private final Map<BioAssayDimension, QuantitationType> bkgSubChannelA = new HashMap<>();
        private final Map<BioAssayDimension, QuantitationType> preferred = new HashMap<>();
        private final Map<BioAssayDimension, QuantitationType> signalChannelA = new HashMap<>();
        private final Map<BioAssayDimension, QuantitationType> signalChannelB = new HashMap<>();

        void addBackgroundChannelA( BioAssayDimension dim, QuantitationType qt ) {
            this.backgroundChannelA.put( dim, qt );
        }

        void addBackgroundChannelB( BioAssayDimension dim, QuantitationType qt ) {
            this.backgroundChannelB.put( dim, qt );
        }

        void addBkgSubChannelA( BioAssayDimension dim, QuantitationType qt ) {
            this.bkgSubChannelA.put( dim, qt );
        }

        void addPreferred( BioAssayDimension dim, QuantitationType qt ) {
            this.preferred.put( dim, qt );
        }

        void addSignalChannelA( BioAssayDimension dim, QuantitationType qt ) {
            this.signalChannelA.put( dim, qt );
        }

        void addSignalChannelB( BioAssayDimension dim, QuantitationType qt ) {
            this.signalChannelB.put( dim, qt );
        }

        QuantitationType getBackgroundChannelA( BioAssayDimension dim ) {
            return backgroundChannelA.get( dim );
        }

        QuantitationType getBackgroundChannelB( BioAssayDimension dim ) {
            return backgroundChannelB.get( dim );
        }

        QuantitationType getBkgSubChannelA( BioAssayDimension dim ) {
            return bkgSubChannelA.get( dim );
        }

        QuantitationType getPreferred( BioAssayDimension dim ) {
            return preferred.get( dim );
        }

        QuantitationType getSignalChannelA( BioAssayDimension dim ) {
            return signalChannelA.get( dim );
        }

        QuantitationType getSignalChannelB( BioAssayDimension dim ) {
            return signalChannelB.get( dim );
        }
    }
}

