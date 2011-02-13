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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.datastructure.matrix.ExpressionDataIntegerMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataStringMatrix;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ChannelUtils;

/**
 * Utility methods for taking an ExpressionExperiment and returning various types of ExpressionDataMatrices, such as the
 * processed data, preferred data, background, etc. This class is not database aware; use the
 * ExpressionDataMatrixService to get ready-to-use matrices starting from an ExpressionExperiment.
 * <p>
 * This handles complexities such as experiments that contain multiple array designs with differing quantitation types.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixBuilder {

    private static Log log = LogFactory.getLog( ExpressionDataMatrixBuilder.class.getName() );

    /**
     * @param representation PrimitiveType
     * @param vectors raw vectors
     * @return matrix of appropriate type.
     */
    public static ExpressionDataMatrix<?> getMatrix( PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors ) {
        ExpressionDataMatrix<?> expressionDataMatrix = null;
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
     * @return
     */
    public static Collection<QuantitationType> getMissingValueQuantitationTypes(
            ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<QuantitationType>();

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.size() == 0 )
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );

        log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        for ( QuantitationType qType : eeQtTypes ) {
            if ( qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                log.debug( "Present/absent=" + qType );
                neededQtTypes.add( qType );
            }
        }

        return neededQtTypes;
    }

    /**
     * @param expressionExperiment
     * @return
     */
    public static Collection<QuantitationType> getPreferredAndMissingQuantitationTypes(
            ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<QuantitationType>();
        neededQtTypes.addAll( getPreferredQuantitationTypes( expressionExperiment ) );
        neededQtTypes.addAll( getMissingValueQuantitationTypes( expressionExperiment ) );
        return neededQtTypes;
    }

    /**
     * @param expressionExperiment
     * @return
     */
    public static Collection<QuantitationType> getPreferredQuantitationTypes( ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<QuantitationType>();

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.size() == 0 )
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );

        log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        for ( QuantitationType qType : eeQtTypes ) {
            if ( qType.getIsPreferred() ) {
                log.debug( "Preferred=" + qType );
                neededQtTypes.add( qType );
            }
        }

        return neededQtTypes;
    }

    /**
     * Get just the quantitation types that are likely to be 'useful': Preferred, present/absent, signals and background
     * from both channels (if present).
     * 
     * @return
     */
    public static Collection<QuantitationType> getUsefulQuantitationTypes( Collection<QuantitationType> eeQtTypes ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<QuantitationType>();

        for ( QuantitationType qType : eeQtTypes ) {

            String name = qType.getName();
            if ( qType.getIsPreferred() ) {
                log.info( "Preferred=" + qType );
                neededQtTypes.add( qType );
            } else if ( qType.getIsMaskedPreferred() ) {
                log.info( "Masked preferred=" + qType );
                neededQtTypes.add( qType );
            } else if ( ChannelUtils.isBackgroundChannelA( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Background A=" + qType );
            } else if ( ChannelUtils.isBackgroundChannelB( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Background B=" + qType );
            } else if ( ChannelUtils.isSignalChannelA( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Signal A=" + qType );
            } else if ( ChannelUtils.isSignalChannelB( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Signal B=" + qType );
            } else if ( name.matches( "CH1D_MEAN" ) ) {
                /*
                 * Special case. This is the background subtracted channel 1 for Genepix data. It is only needed to
                 * reconstruct CH1 if it isn't present, which is surprisingly common in Stanford data sets
                 */
                neededQtTypes.add( qType );
            } else if ( qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                log.info( "Present/absent=" + qType );
                neededQtTypes.add( qType );
            }
        }
        return neededQtTypes;
    }

    /**
     * Get just the quantitation types that are likely to be 'useful': Preferred, present/absent, signals and background
     * from both channels (if present).
     * 
     * @return
     */
    public static Collection<QuantitationType> getUsefulQuantitationTypes( ExpressionExperiment expressionExperiment ) {

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.size() == 0 ) {
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );
        }

        log.debug( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        Collection<QuantitationType> neededQtTypes = getUsefulQuantitationTypes( eeQtTypes );

        return neededQtTypes;
    }

    private Collection<DesignElementDataVector> vectors;

    private Map<ArrayDesign, BioAssayDimension> dimMap = new HashMap<ArrayDesign, BioAssayDimension>();

    private ExpressionExperiment expressionExperiment;

    private Collection<ProcessedExpressionDataVector> processedDataVectors = null;

    private QuantitationTypeData dat = null;

    private Map<QuantitationType, Integer> numMissingValues = new HashMap<QuantitationType, Integer>();

    private boolean anyMissing = false;

    /**
     * @param collection of vectors. They should be thawed first.
     */
    public ExpressionDataMatrixBuilder( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) throw new IllegalArgumentException( "No vectors" );
        this.vectors = new HashSet<DesignElementDataVector>();
        this.vectors.addAll( vectors );
        this.expressionExperiment = vectors.iterator().next().getExpressionExperiment();
    }

    /**
     * @param processedVectors
     * @param otherVectors
     */
    public ExpressionDataMatrixBuilder( Collection<ProcessedExpressionDataVector> processedVectors,
            Collection<? extends DesignElementDataVector> otherVectors ) {
        this.vectors = new HashSet<DesignElementDataVector>();
        this.vectors.addAll( otherVectors );
        this.processedDataVectors = processedVectors;
    }

    /**
     * @return
     */
    public ExpressionDataDoubleMatrix getBackgroundChannelA() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();

        List<BioAssayDimension> dimensions = getBioAssayDimensions();

        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelA( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( qTypes );
        }
        return null;
    }

    /**
     * @return
     */
    public ExpressionDataDoubleMatrix getBackgroundChannelB() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();

        List<BioAssayDimension> dimensions = getBioAssayDimensions();

        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelB( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( qTypes );
        }
        return null;
    }

    /**
     * This will return a single BioAssayDimension except if there are multiple array designs used in the experiment.
     * 
     * @return
     */
    public List<BioAssayDimension> getBioAssayDimensions() {

        List<BioAssayDimension> result = new ArrayList<BioAssayDimension>();

        if ( dimMap.keySet().size() > 0 ) {
            result.addAll( dimMap.values() );
            return result;
        }

        if ( this.vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors, no bioassay dimensions" );
        }

        log.debug( "Checking all vectors to get bioAssayDimensions" );
        Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : vectors ) {
            ArrayDesign adUsed = arrayDesignForVector( vector );
            if ( !dimMap.containsKey( adUsed ) ) {
                dimMap.put( adUsed, vector.getBioAssayDimension() );
            }
            // if ( arrayDesign == null || adUsed.equals( arrayDesign ) ) {
            assert vector.getBioAssayDimension() != null;
            dimensions.add( vector.getBioAssayDimension() );
            // }
        }

        log.debug( "got " + dimensions.size() + " bioassaydimensions" );
        result.addAll( dimensions );
        return result;
    }

    /**
     * @return
     */
    public ExpressionDataDoubleMatrix getBkgSubChannelA() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBkgSubChannelA( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( qTypes );
        }
        return null;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * Compute an intensity matrix. For two-channel arrays, this is the geometric mean of the background-subtracted
     * signals on the two channels. For two-color arrays, if one channel is missing (as happens sometimes) the
     * intensities returned are just from the one channel. For one-color arrays, this is the same as the preferred data
     * matrix.
     * 
     * @return
     */
    public ExpressionDataDoubleMatrix getIntensity() {
        if ( this.isTwoColor() ) {

            ExpressionDataDoubleMatrix signalA = this.getSignalChannelA();
            ExpressionDataDoubleMatrix signalB = this.getSignalChannelB();
            ExpressionDataDoubleMatrix backgroundA = this.getBackgroundChannelA();
            ExpressionDataDoubleMatrix backgroundB = this.getBackgroundChannelB();

            if ( signalA == null && signalB == null ) {
                log.warn( "Cannot get signal for either channel" );
                return null;
            }

            if ( backgroundA != null && signalA != null ) {
                ExpressionDataDoubleMatrixUtil.subtractMatrices( signalA, backgroundA );
            }

            if ( backgroundB != null && signalB != null ) {
                ExpressionDataDoubleMatrixUtil.subtractMatrices( signalB, backgroundB );
            }

            if ( signalA != null ) {
                ExpressionDataDoubleMatrixUtil.logTransformMatrix( signalA );
            }

            if ( signalB != null ) {
                ExpressionDataDoubleMatrixUtil.logTransformMatrix( signalB );
            }

            if ( signalA != null && signalB != null ) {
                ExpressionDataDoubleMatrixUtil.addMatrices( signalA, signalB );
                ExpressionDataDoubleMatrixUtil.scalarDivideMatrix( signalA, 2.0 );
            }

            if ( signalA == null ) {
                return signalB;
            }
            return signalA; // now this contains the answer

        }
        return getPreferredData();

    }

    /**
     * @return a matrix of booleans, or null if a missing value quantitation type ("absent/present", which may have been
     *         computed by our system) is not found. This will return the values whether the array design is two-color
     *         or not.
     */
    public ExpressionDataBooleanMatrix getMissingValueData() {
        List<QuantitationType> qtypes = this.getMissingValueQTypes();
        if ( qtypes == null || qtypes.size() == 0 ) return null;
        return new ExpressionDataBooleanMatrix( vectors, qtypes );
    }

    public Integer getNumMissingValues( QuantitationType qt ) {
        if ( dat == null ) dat = getQuantitationTypesNeeded();
        return numMissingValues.get( qt );
    }

    /**
     * @return The matrix for the preferred data - NOT the processed data (though they may be the same, in fact)
     */
    public ExpressionDataDoubleMatrix getPreferredData() {

        List<QuantitationType> qtypes = this.getPreferredQTypes();

        if ( qtypes.size() == 0 ) {
            log.warn( "Could not find a 'preferred' quantitation type" );
            return null;
        }

        return new ExpressionDataDoubleMatrix( getPreferredDataVectors(), qtypes );
    }

    /**
     * @param arrayDesign Can be null
     * @return
     */
    public List<QuantitationType> getPreferredQTypes() {
        List<QuantitationType> result = new ArrayList<QuantitationType>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        if ( dimensions.size() == 0 ) {
            throw new IllegalArgumentException( "No bioassaydimensions!" );
        }

        for ( BioAssayDimension dimension : dimensions ) {
            for ( DesignElementDataVector vector : vectors ) {

                if ( !vector.getBioAssayDimension().equals( dimension ) ) continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getIsPreferred() ) continue;

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
     * @return
     */
    public ExpressionDataDoubleMatrix getProcessedData() {

        List<QuantitationType> qtypes = this.getPreferredQTypes();

        if ( qtypes.size() == 0 ) {
            log.warn( "Could not find a 'preferred' quantitation type" );
            return null;
        }

        return new ExpressionDataDoubleMatrix( getProcessedDataVectors(), qtypes );
    }

    /**
     * @return
     */
    public Map<DesignElement, Double> getRanksByMean() {
        Collection<QuantitationType> qtypes = this.getPreferredQTypes();
        Map<DesignElement, Double> ranks = new HashMap<DesignElement, Double>();

        for ( DesignElementDataVector v : this.vectors ) {
            if ( qtypes.contains( v.getQuantitationType() ) && v instanceof ProcessedExpressionDataVector ) {
                ranks.put( v.getDesignElement(), ( ( ProcessedExpressionDataVector ) v ).getRankByMean() );
            }
        }

        return ranks;
    }

    /**
     * @return
     */
    public ExpressionDataDoubleMatrix getSignalChannelA() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {

            QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
            QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
            QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
            QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

            boolean channelANeedsReconstruction = checkChannelA( signalChannelA, signalChannelB, backgroundChannelA,
                    bkgSubChannelA );

            if ( channelANeedsReconstruction ) {
                return getSignalChannelAFancy( dimension );
            }
            if ( signalChannelA != null ) qTypes.add( signalChannelA );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( qTypes );
        }
        return null;
    }

    /**
     * @return
     */
    public ExpressionDataDoubleMatrix getSignalChannelB() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getSignalChannelB( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( qTypes );
        }
        return null;
    }

    public boolean isAnyMissing() {
        if ( dat == null ) dat = getQuantitationTypesNeeded();
        return anyMissing;
    }

    /**
     * add the background values back on.
     * 
     * @param signalDataA - already background subtracted
     * @param bkgDataA
     */
    private void addBackgroundBack( ExpressionDataDoubleMatrix signalDataA, ExpressionDataDoubleMatrix bkgDataA ) {
        for ( int i = 0; i < signalDataA.rows(); i++ ) {
            for ( int j = 0; j < signalDataA.columns(); j++ ) {
                double oldVal = signalDataA.get( i, j );
                double bkg = bkgDataA.get( i, j );
                signalDataA.set( i, j, oldVal + bkg );
            }
        }
    }

    /**
     * @param vector
     * @return
     */
    private ArrayDesign arrayDesignForVector( DesignElementDataVector vector ) {
        Collection<BioAssay> bioAssays = vector.getBioAssayDimension().getBioAssays();
        if ( bioAssays.size() == 0 ) throw new IllegalArgumentException( "No bioassays for vector." );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( BioAssay ba : bioAssays ) {
            ads.add( ba.getArrayDesignUsed() );
        }
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException( "Can't handle vectors with multiple array design represented" );
        }
        ArrayDesign adUsed = ads.iterator().next();
        return adUsed;
    }

    /**
     * @param signalChannelA
     * @param signalChannelB
     * @param backgroundChannelA
     * @param bkgSubChannelA
     * @return true if channelA needs reconstruction
     */
    private boolean checkChannelA( QuantitationType signalChannelA, QuantitationType signalChannelB,
            QuantitationType backgroundChannelA, QuantitationType bkgSubChannelA ) {
        if ( signalChannelA == null || signalChannelB == null ) {

            /*
             * This can happen for some Stanford data sets where the CH1 data was not submitted. But we can sometimes
             * reconstruct the values from the background
             */

            if ( signalChannelB != null && bkgSubChannelA != null && backgroundChannelA != null ) {
                log.info( "Invoking work-around for missing channel 1 intensities" );
                return true;
            }
            log.warn( "Could not find signals for both channels: " + "Channel A =" + signalChannelA + ", Channel B="
                    + signalChannelB + " and backgroundChannelA =" + backgroundChannelA
                    + " and background-subtracted channel A =" + bkgSubChannelA );
            return false;

        }
        return false;
    }

    /**
     * @return
     */
    private List<QuantitationType> getMissingValueQTypes() {
        List<QuantitationType> result = new ArrayList<QuantitationType>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();

        for ( BioAssayDimension dim : dimensions ) {
            for ( DesignElementDataVector vector : vectors ) {

                if ( !vector.getBioAssayDimension().equals( dim ) ) continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) continue;

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
    private Collection<DesignElementDataVector> getPreferredDataVectors() {
        Collection<DesignElementDataVector> result = new HashSet<DesignElementDataVector>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qtypes = this.getPreferredQTypes();

        for ( DesignElementDataVector vector : vectors ) {
            if ( !( vector instanceof ProcessedExpressionDataVector )
                    && dimensions.contains( vector.getBioAssayDimension() )
                    && qtypes.contains( vector.getQuantitationType() ) ) result.add( vector );
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

        Collection<ProcessedExpressionDataVector> result = new HashSet<ProcessedExpressionDataVector>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions();
        List<QuantitationType> qtypes = this.getPreferredQTypes();

        for ( DesignElementDataVector vector : vectors ) {
            if ( vector instanceof ProcessedExpressionDataVector && dimensions.contains( vector.getBioAssayDimension() )
                    && qtypes.contains( vector.getQuantitationType() ) )
                result.add( ( ProcessedExpressionDataVector ) vector );
        }
        return result;
    }

    /**
     * If there are multiple valid choices, we choose the first one seen, unless a later one has fewer missing value.
     * 
     * @return
     */
    private QuantitationTypeData getQuantitationTypesNeeded() {

        Collection<BioAssayDimension> dimensions = getBioAssayDimensions();

        QuantitationTypeData result = new QuantitationTypeData();

        populateMissingValueInfo();

        for ( BioAssayDimension targetdimension : dimensions ) {

            Collection<QuantitationType> checkedQts = new HashSet<QuantitationType>();

            for ( DesignElementDataVector vector : vectors ) {

                BioAssayDimension dim = vector.getBioAssayDimension();

                if ( !dim.equals( targetdimension ) ) continue;

                QuantitationType qType = vector.getQuantitationType();

                if ( checkedQts.contains( qType ) ) continue;

                checkedQts.add( qType );

                String name = qType.getName();
                if ( qType.getIsPreferred() && result.getPreferred( dim ) == null ) {
                    result.addPreferred( dim, qType );
                    log.info( "Preferred=" + qType );
                } else if ( ChannelUtils.isBackgroundChannelA( name ) ) {

                    if ( result.getBackgroundChannelA( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getBackgroundChannelA( dim ) );
                        if ( i < j ) {
                            log.info( "Found better background A=" + qType );
                            result.addBackgroundChannelA( dim, qType );
                        }
                    } else {
                        result.addBackgroundChannelA( dim, qType );
                        log.info( "Background A=" + qType );
                    }

                } else if ( ChannelUtils.isBackgroundChannelB( name ) ) {
                    if ( result.getBackgroundChannelB( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getBackgroundChannelB( dim ) );
                        if ( i < j ) {
                            log.info( "Found better background B=" + qType );
                            result.addBackgroundChannelB( dim, qType );
                        }
                    } else {
                        result.addBackgroundChannelB( dim, qType );
                        log.info( "Background B=" + qType );
                    }
                } else if ( ChannelUtils.isSignalChannelA( name ) ) {
                    if ( result.getSignalChannelA( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getSignalChannelA( dim ) );
                        if ( i < j ) {
                            log.info( "Found better Signal A=" + qType );
                            result.addSignalChannelA( dim, qType );
                        }
                    } else {
                        result.addSignalChannelA( dim, qType );
                        log.info( "Signal A=" + qType );
                    }
                } else if ( ChannelUtils.isSignalChannelB( name ) ) {
                    if ( result.getSignalChannelB( dim ) != null ) {
                        int i = numMissingValues.get( qType );
                        int j = numMissingValues.get( result.getSignalChannelB( dim ) );
                        if ( i < j ) {
                            log.info( "Found better Signal B=" + qType );
                            result.addSignalChannelB( dim, qType );
                        }
                    } else {
                        result.addSignalChannelB( dim, qType );
                        log.info( "Signal B=" + qType );
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

    /**
     * @param dimension
     * @return
     */
    private ExpressionDataDoubleMatrix getSignalChannelAFancy( BioAssayDimension dimension ) {
        boolean channelANeedsReconstruction = false;

        /*
         * This is made messy by data sets where the non-background-subtracted data has been omitted, but the background
         * values are available.
         */
        if ( dat == null ) dat = getQuantitationTypesNeeded();

        QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
        QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
        QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
        QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

        channelANeedsReconstruction = checkChannelA( signalChannelA, signalChannelB, backgroundChannelA, bkgSubChannelA );

        ExpressionDataDoubleMatrix signalDataA = null;
        if ( channelANeedsReconstruction ) {
            ExpressionDataDoubleMatrix bkgDataA = null;
            if ( backgroundChannelA != null ) {
                bkgDataA = new ExpressionDataDoubleMatrix( vectors, backgroundChannelA );
            }

            // use background-subtracted data and add bkg back on
            assert bkgDataA != null;
            assert bkgSubChannelA != null;
            signalDataA = new ExpressionDataDoubleMatrix( vectors, bkgSubChannelA );
            addBackgroundBack( signalDataA, bkgDataA );
            return signalDataA;

        }
        return getSignalChannelA();

    }

    /**
     * @return
     */
    private boolean isTwoColor() {
        for ( DesignElementDataVector v : vectors ) {
            DesignElement d = v.getDesignElement();
            TechnologyType technologyType = d.getArrayDesign().getTechnologyType();

            if ( technologyType.equals( TechnologyType.ONECOLOR ) ) {
                continue;
            }

            QuantitationType qt = v.getQuantitationType();

            if ( ( qt.getIsPreferred() || qt.getIsMaskedPreferred() ) && qt.getIsRatio() ) {
                return true;
            }

        }
        return false;
    }

    /**
     * @param dimensions
     * @param qTypes
     * @return
     */
    private ExpressionDataDoubleMatrix makeMatrix( List<QuantitationType> qTypes ) {
        if ( qTypes.size() > 0 ) {
            return new ExpressionDataDoubleMatrix( vectors, qTypes );
        }
        return null;
    }

    private void populateMissingValueInfo() {
        ByteArrayConverter bac = new ByteArrayConverter();

        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType qt = vector.getQuantitationType();
            if ( !numMissingValues.containsKey( qt ) ) {
                numMissingValues.put( qt, 0 );
            }

            for ( Double d : bac.byteArrayToDoubles( vector.getData() ) ) {
                if ( d.isNaN() ) {
                    anyMissing = true;
                    numMissingValues.put( qt, numMissingValues.get( qt ) + 1 );
                }
            }
        }
    }

}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Helper class that keeps track of which QTs are background, signal and preferred.
 */
class QuantitationTypeData {

    Map<BioAssayDimension, QuantitationType> backgroundChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> backgroundChannelB = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> bkgSubChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> preferred = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> signalChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> signalChannelB = new HashMap<BioAssayDimension, QuantitationType>();

    public void addBackgroundChannelA( BioAssayDimension dim, QuantitationType qt ) {
        this.backgroundChannelA.put( dim, qt );
    }

    public void addBackgroundChannelB( BioAssayDimension dim, QuantitationType qt ) {
        this.backgroundChannelB.put( dim, qt );
    }

    public void addBkgSubChannelA( BioAssayDimension dim, QuantitationType qt ) {
        this.bkgSubChannelA.put( dim, qt );
    }

    public void addPreferred( BioAssayDimension dim, QuantitationType qt ) {
        this.preferred.put( dim, qt );
    }

    public void addSignalChannelA( BioAssayDimension dim, QuantitationType qt ) {
        this.signalChannelA.put( dim, qt );
    }

    public void addSignalChannelB( BioAssayDimension dim, QuantitationType qt ) {
        this.signalChannelB.put( dim, qt );
    }

    public QuantitationType getBackgroundChannelA( BioAssayDimension dim ) {
        return backgroundChannelA.get( dim );
    }

    public QuantitationType getBackgroundChannelB( BioAssayDimension dim ) {
        return backgroundChannelB.get( dim );
    }

    public QuantitationType getBkgSubChannelA( BioAssayDimension dim ) {
        return bkgSubChannelA.get( dim );
    }

    public QuantitationType getPreferred( BioAssayDimension dim ) {
        return preferred.get( dim );
    }

    public QuantitationType getSignalChannelA( BioAssayDimension dim ) {
        return signalChannelA.get( dim );
    }

    public QuantitationType getSignalChannelB( BioAssayDimension dim ) {
        return signalChannelB.get( dim );
    }

}
