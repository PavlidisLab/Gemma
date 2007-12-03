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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Utility methods for taking an ExpressionExperiment and returning various types of ExpressionDataMatrices, such as the
 * preferred data, background, etc. This class is not database aware; use the ExpressionDataMatrixService to get
 * ready-to-use matrices starting from an ExpressionExperiment.
 * <p>
 * This handles complexities such as experiments that contain multiple array designs with differing quantitation types.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixBuilder {

    private static Log log = LogFactory.getLog( ExpressionDataMatrixBuilder.class.getName() );
    private ExpressionExperiment expressionExperiment;

    private Map<ArrayDesign, BioAssayDimension> dimMap = new HashMap<ArrayDesign, BioAssayDimension>();

    Collection<DesignElementDataVector> vectors;

    /**
     * @param collection of vectors. They should be thawed first.
     */
    public ExpressionDataMatrixBuilder( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) throw new IllegalArgumentException( "No vectors" );
        this.vectors = vectors;
        this.expressionExperiment = vectors.iterator().next().getExpressionExperiment();
    }

    /**
     * @param arrayDesign can be null
     * @return
     */
    public ExpressionDataDoubleMatrix getBackgroundChannelA( ArrayDesign arrayDesign ) {
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );

        List<BioAssayDimension> dimensions = getBioAssayDimensions( arrayDesign );

        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelA( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( dimensions, qTypes );
        }
        return null;
    }

    /**
     * @param arrayDesign can be null
     * @return
     */
    public ExpressionDataDoubleMatrix getBackgroundChannelB( ArrayDesign arrayDesign ) {
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );

        List<BioAssayDimension> dimensions = getBioAssayDimensions( arrayDesign );

        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBackgroundChannelB( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( dimensions, qTypes );
        }
        return null;
    }

    /**
     * This will return a single BioAssayDimension except if the arrayDesign is null AND there are multiple array
     * designs used in the experiment.
     * 
     * @param arrayDesign can be null
     * @return
     */
    public List<BioAssayDimension> getBioAssayDimensions( ArrayDesign arrayDesign ) {

        List<BioAssayDimension> result = new ArrayList<BioAssayDimension>();
        if ( arrayDesign == null && dimMap.keySet().size() > 0 ) {
            result.addAll( dimMap.values() );
            return result;
        } else if ( arrayDesign != null && dimMap.containsKey( arrayDesign ) ) {
            result.add( dimMap.get( arrayDesign ) );
            return result;
        }

        log.debug( "Checking all vectors to get bioAssayDimensions" );
        Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : vectors ) {
            ArrayDesign adUsed = arrayDesignForVector( vector );
            if ( !dimMap.containsKey( adUsed ) ) {
                dimMap.put( adUsed, vector.getBioAssayDimension() );
            }
            if ( arrayDesign == null || adUsed.equals( arrayDesign ) ) {
                assert vector.getBioAssayDimension() != null;
                dimensions.add( vector.getBioAssayDimension() );
            }
        }

        log.debug( "got " + dimensions.size() + " bioassaydimensions" );
        result.addAll( dimensions );
        return result;
    }

    /**
     * @return a matrix of booleans, or null if a missing value quantitation type ("absent/present", which may have been
     *         computed by our system) is not found. This will return the values whether the array design is two-color
     *         or not.
     */
    public ExpressionDataBooleanMatrix getMissingValueData( ArrayDesign arrayDesign ) {
        List<QuantitationType> qtypes = this.getMissingValueQTypes( arrayDesign );
        if ( qtypes == null || qtypes.size() == 0 ) return null;
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );
        return new ExpressionDataBooleanMatrix( vectors, dimensions, qtypes );
    }

    /**
     * @return Matrix of the 'preferred' data.
     */
    public ExpressionDataDoubleMatrix getPreferredData() {
        return this.getPreferredData( null );
    }

    /**
     * arrayDesign The array design to consider; this can be null to get results for all the array designs used.
     * 
     * @param arrayDesign, which can be null.
     * @return Collection of vectors with the 'preferred' quantitation type, for the array design selected.
     */
    public Collection<DesignElementDataVector> getPreferredDataVectors( ArrayDesign arrayDesign ) {
        Collection<DesignElementDataVector> result = new HashSet<DesignElementDataVector>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );
        List<QuantitationType> qtypes = this.getPreferredQTypes( arrayDesign );

        for ( DesignElementDataVector vector : vectors ) {
            if ( dimensions.contains( vector.getBioAssayDimension() ) && qtypes.contains( vector.getQuantitationType() ) )
                result.add( vector );
        }
        return result;
    }

    /**
     * @param arrayDesign The array design to consider; this can be null to get results for all the array designs used.
     * @return
     */
    public ExpressionDataDoubleMatrix getPreferredData( ArrayDesign arrayDesign ) {
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );

        if ( dimensions.size() == 0 ) {
            throw new IllegalStateException( "Could not find any BioAssayDimensions" );
        }

        List<QuantitationType> qtypes = this.getPreferredQTypes( arrayDesign );

        if ( qtypes.size() == 0 ) {
            log.warn( "Could not find a 'preferred' quantitation type" );
            return null;
        }

        log.info( qtypes.size() + " preferred quantitation types" );
        log.info( dimensions.size() + " bioassay dimensions" );
        log.info( vectors.size() + " vectors" );

        return new ExpressionDataDoubleMatrix( vectors, dimensions, qtypes );
    }

    /**
     * @param arrayDesign. Unlike other methods, the array design cannot be null under all conditions.
     * @return
     */
    private ExpressionDataDoubleMatrix getSignalChannelAFancy( ArrayDesign arrayDesign ) {
        boolean channelANeedsReconstruction = false;

        /*
         * This is made messy by data sets where the non-background-subtracted data has been omitted, but the background
         * values are available.
         */
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );

        BioAssayDimension dimension = dimensions.iterator().next();

        QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
        QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
        QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
        QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

        channelANeedsReconstruction = checkChannelA( signalChannelA, signalChannelB, backgroundChannelA, bkgSubChannelA );

        if ( channelANeedsReconstruction && arrayDesign == null && dimensions.size() > 1 ) {
            throw new UnsupportedOperationException(
                    "Cannot create channel A signal matrix for multiple array designs at once when the signal needs the background added back." );
        }

        ExpressionDataDoubleMatrix signalDataA = null;
        if ( channelANeedsReconstruction ) {

            ExpressionDataDoubleMatrix bkgDataA = null;
            if ( backgroundChannelA != null ) {
                bkgDataA = new ExpressionDataDoubleMatrix( vectors, dimension, backgroundChannelA );
            }

            // use background-subtracted data and add bkg back on
            assert bkgDataA != null;
            assert bkgSubChannelA != null;
            signalDataA = new ExpressionDataDoubleMatrix( vectors, dimension, bkgSubChannelA );
            addBackgroundBack( signalDataA, bkgDataA );
            return signalDataA;

        }
        return getSignalChannelA( arrayDesign );

    }

    /**
     * @param arrayDesign
     * @return
     */
    public ExpressionDataDoubleMatrix getBkgSubChannelA( ArrayDesign arrayDesign ) {
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getBkgSubChannelA( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( dimensions, qTypes );
        }
        return null;
    }

    /**
     * @param arrayDesign
     * @return
     */
    public ExpressionDataDoubleMatrix getSignalChannelB( ArrayDesign arrayDesign ) {
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {
            QuantitationType qType = dat.getSignalChannelB( dimension );
            if ( qType != null ) qTypes.add( qType );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( dimensions, qTypes );
        }
        return null;
    }

    /**
     * @param arrayDesign, Can be null. However, in some cases this will fail if the array Design is null. Specifically,
     *        if there are multiple array designs and one of them has data that needs the signal reconstructed by adding
     *        back the background, an exception will be thrown. Therefore it is recommended to call this method with a
     *        non-null array design.
     * @return
     */
    public ExpressionDataDoubleMatrix getSignalChannelA( ArrayDesign arrayDesign ) {
        QuantitationTypeData dat = getQuantitationTypesNeeded( arrayDesign );
        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );
        List<QuantitationType> qTypes = new ArrayList<QuantitationType>();

        for ( BioAssayDimension dimension : dimensions ) {

            QuantitationType signalChannelA = dat.getSignalChannelA( dimension );
            QuantitationType signalChannelB = dat.getSignalChannelB( dimension );
            QuantitationType backgroundChannelA = dat.getBackgroundChannelA( dimension );
            QuantitationType bkgSubChannelA = dat.getBkgSubChannelA( dimension );

            boolean channelANeedsReconstruction = checkChannelA( signalChannelA, signalChannelB, backgroundChannelA,
                    bkgSubChannelA );

            if ( channelANeedsReconstruction ) {
                return getSignalChannelAFancy( arrayDesign );
            }
            if ( signalChannelA != null ) qTypes.add( signalChannelA );
        }

        if ( qTypes.size() != 0 ) {
            return makeMatrix( dimensions, qTypes );
        }
        return null;
    }

    /**
     * Compute an intensity matrix. For two-channel arrays, this is the geometric mean of the background-subtracted
     * signals on the two channels. For two-color arrays, if one channel is missing (as happens sometimes) the
     * intensities returned are just from the one channel. For one-color arrays, this is the same as the preferred data
     * matrix.
     * 
     * @param arrayDesign
     * @return
     */
    public ExpressionDataDoubleMatrix getIntensity( ArrayDesign arrayDesign ) {
        if ( arrayDesign != null && arrayDesign.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {

            ExpressionDataDoubleMatrix signalA = this.getSignalChannelA( arrayDesign );
            ExpressionDataDoubleMatrix signalB = this.getSignalChannelB( arrayDesign );
            ExpressionDataDoubleMatrix backgroundA = this.getBackgroundChannelA( arrayDesign );
            ExpressionDataDoubleMatrix backgroundB = this.getBackgroundChannelB( arrayDesign );

            if ( signalA == null && signalB == null ) {
                log.warn( "Cannot get signal for either channel" );
                return null;
            }

            if ( backgroundA != null && signalA != null )
                ExpressionDataDoubleMatrixUtil.subtractMatrices( signalA, backgroundA );

            if ( backgroundB != null && signalB != null )
                ExpressionDataDoubleMatrixUtil.subtractMatrices( signalB, backgroundB );

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
        return getPreferredData( arrayDesign );

    }

    /**
     * Returns the {@link ExpressionDataDoubleMatrix}. For two color arrays, the missing values are masked. If
     * arrayDesign is null, all array designs will be used to get the intensity results.
     * 
     * @param arrayDesign
     * @return ExpressionDataDoubleMatrix - For two color arrays, the missing values are masked.
     */
    public ExpressionDataDoubleMatrix getMaskedPreferredData( ArrayDesign arrayDesign ) {

        ExpressionDataDoubleMatrix preferredData = this.getPreferredData( arrayDesign );
        if ( preferredData == null ) return null;
        if ( arrayDesign != null && arrayDesign.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {
            maskMissingValues( preferredData, arrayDesign );
        }
        return preferredData;
    }

    /**
     * Masking is done even if the array design is not two-color, so the decision whether to mask or not must be done
     * elsewhere.
     * 
     * @param inMatrix
     * @param missingValueMatrix
     */
    public void maskMissingValues( ExpressionDataDoubleMatrix inMatrix, ArrayDesign arrayDesign ) {
        ExpressionDataBooleanMatrix missingValueMatrix = this.getMissingValueData( arrayDesign );
        if ( missingValueMatrix != null ) ExpressionDataDoubleMatrixUtil.maskMatrix( inMatrix, missingValueMatrix );
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
    public ArrayDesign arrayDesignForVector( DesignElementDataVector vector ) {
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
     * @param arrayDesign
     * @param dimensions
     * @return
     */
    private List<QuantitationType> getMissingValueQTypes( ArrayDesign arrayDesign ) {
        List<QuantitationType> result = new ArrayList<QuantitationType>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );

        for ( BioAssayDimension dim : dimensions ) {
            for ( DesignElementDataVector vector : vectors ) {

                if ( !vector.getBioAssayDimension().equals( dim ) ) continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) continue;

                ArrayDesign adUsed = arrayDesignForVector( vector );
                if ( arrayDesign != null && !adUsed.equals( arrayDesign ) ) continue;

                // if we get here, we're in the right place.
                result.add( qType );
                break; // on to the next dimension.

            }
        }

        return result;
    }

    /**
     * @param arrayDesign Can be null
     * @return
     */
    public List<QuantitationType> getPreferredQTypes( ArrayDesign arrayDesign ) {
        List<QuantitationType> result = new ArrayList<QuantitationType>();

        List<BioAssayDimension> dimensions = this.getBioAssayDimensions( arrayDesign );

        if ( dimensions.size() == 0 ) {
            throw new IllegalArgumentException( "No bioassaydimensions!" );
        }

        for ( BioAssayDimension dimension : dimensions ) {
            for ( DesignElementDataVector vector : vectors ) {

                if ( !vector.getBioAssayDimension().equals( dimension ) ) continue;

                QuantitationType qType = vector.getQuantitationType();
                if ( !qType.getIsPreferred() ) continue;

                ArrayDesign adUsed = arrayDesignForVector( vector );
                if ( arrayDesign != null && !adUsed.equals( arrayDesign ) ) continue;

                // if we get here, we're in the right place.
                result.add( qType );
                break; // on to the next dimension.

            }
        }

        return result;
    }

    /**
     * Get just the quantitation types that are likely to be 'useful': Preferred, present/absent, signals and background
     * from both channels (if present).
     * 
     * @return
     */
    public static Collection<QuantitationType> getUsefulQuantitationTypes( ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> neededQtTypes = new HashSet<QuantitationType>();

        Collection<QuantitationType> eeQtTypes = expressionExperiment.getQuantitationTypes();

        if ( eeQtTypes.size() == 0 )
            throw new IllegalArgumentException( "No quantitation types for " + expressionExperiment );

        log.info( "Experiment has " + eeQtTypes.size() + " quantitation types" );

        for ( QuantitationType qType : eeQtTypes ) {

            String name = qType.getName();
            if ( qType.getIsPreferred() ) {
                log.info( "Preferred=" + qType );
                neededQtTypes.add( qType );
            } else if ( isBackgroundChannelA( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Background A=" + qType );
            } else if ( isBackgroundChannelB( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Background B=" + qType );
            } else if ( isSignalChannela( name ) ) {
                neededQtTypes.add( qType );
                log.info( "Signal A=" + qType );
            } else if ( isSignalChannelB( name ) ) {
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
     * @param arrayDesign (can be null to get all)
     */
    private QuantitationTypeData getQuantitationTypesNeeded( ArrayDesign arrayDesign ) {

        Collection<BioAssayDimension> dimensions = getBioAssayDimensions( arrayDesign );

        QuantitationTypeData result = new QuantitationTypeData();
        for ( BioAssayDimension targetdimension : dimensions ) {

            for ( DesignElementDataVector vector : vectors ) {
                ArrayDesign adUsed = arrayDesignForVector( vector );
                if ( arrayDesign != null && !adUsed.equals( arrayDesign ) ) continue;

                BioAssayDimension dim = vector.getBioAssayDimension();

                if ( !dim.equals( targetdimension ) ) continue;

                QuantitationType qType = vector.getQuantitationType();
                String name = qType.getName();
                if ( qType.getIsPreferred() && result.getPreferred( dim ) == null ) {
                    result.setPreferred( dim, qType );
                    log.info( "Preferred=" + qType );
                } else if ( result.getBackgroundChannelA( dim ) == null && isBackgroundChannelA( name ) ) {
                    result.setBackgroundChannelA( dim, qType );
                    log.info( "Background A=" + qType );
                } else if ( result.getBackgroundChannelB( dim ) == null && isBackgroundChannelB( name ) ) {
                    result.setBackgroundChannelB( dim, qType );
                    log.info( "Background B=" + qType );
                } else if ( result.getSignalChannelA( dim ) == null && isSignalChannela( name ) ) {
                    result.setSignalChannelA( dim, qType );
                    log.info( "Signal A=" + qType );
                } else if ( result.getSignalChannelB( dim ) == null && isSignalChannelB( name ) ) {
                    result.setSignalChannelB( dim, qType );
                    log.info( "Signal B=" + qType );
                } else if ( result.getBkgSubChannelA( dim ) == null && name.matches( "CH1D_MEAN" ) ) {
                    result.setBkgSubChannelA( dim, qType ); // specific for SGD data bug
                }

                if ( result.getSignalChannelA( dim ) != null && result.getSignalChannelB( dim ) != null
                        && result.getBackgroundChannelA( dim ) != null && result.getBackgroundChannelB( dim ) != null
                        && result.getPreferred( dim ) != null ) {
                    break; // no need to go through them all.
                }
            }
        }
        return result;
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel A background.
     * 
     * @param name
     * @return
     */
    private static boolean isBackgroundChannelA( String name ) {
        return name.equals( "CH1B_MEDIAN" ) || name.equals( "CH1_BKD" )
                || name.toLowerCase().matches( "b532[\\s_\\.](mean|median)" )
                || name.equals( "BACKGROUND_CHANNEL 1MEDIAN" ) || name.equals( "G_BG_MEDIAN" )
                || name.equals( "Ch1BkgMedian" ) || name.equals( "ch1.Background" ) || name.equals( "CH1_BKG_MEAN" )
                || name.equals( "CH1_BKD_ Median" ) || name.equals( "BKG1Mean" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel B background.
     * 
     * @param name
     * @return
     */
    private static boolean isBackgroundChannelB( String name ) {
        return name.equals( "CH2B_MEDIAN" ) || name.equals( "CH2_BKD" )
                || name.toLowerCase().matches( "b635[\\s_\\.](mean|median)" )
                || name.equals( "BACKGROUND_CHANNEL 2MEDIAN" ) || name.equals( "R_BG_MEDIAN" )
                || name.equals( "Ch2BkgMedian" ) || name.equals( "ch2.Background" ) || name.equals( "CH2_BKG_MEAN" )
                || name.equals( "CH2_BKD_ Median" ) || name.equals( "BKG2Mean" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel A signal. (by
     * convention, green)
     * 
     * @param name
     * @return
     */
    private static boolean isSignalChannela( String name ) {
        return name.matches( "CH1(I)?_MEDIAN" ) || name.matches( "CH1(I)?_MEAN" ) || name.equals( "RAW_DATA" )
                || name.toLowerCase().matches( "f532[\\s_\\.](mean|median)" ) || name.equals( "SIGNAL_CHANNEL 1MEDIAN" )
                || name.toLowerCase().matches( "ch1_smtm" ) || name.equals( "G_MEAN" ) || name.equals( "Ch1SigMedian" )
                || name.equals( "ch1.Intensity" ) || name.equals( "CH1_SIG_MEAN" ) || name.equals( "CH1_ Median" )
                || name.toUpperCase().matches( "\\w{2}\\d{3}_CY3" ) || name.toUpperCase().matches( "NORM(.*)CH1" )
                || name.equals( "CH1Mean" ) || name.equals( "CH1_SIGNAL" ) || name.equals( "\"log2(532), gN\"" )
                || name.equals( "gProcessedSignal" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel B signal.(by
     * convention, red)
     * 
     * @param name
     * @return
     */
    private static boolean isSignalChannelB( String name ) {
        return name.matches( "CH2(I)?_MEDIAN" ) || name.matches( "CH2(I)?_MEAN" ) || name.equals( "RAW_CONTROL" )
                || name.toLowerCase().matches( "f635[\\s_\\.](mean|median)" ) || name.equals( "SIGNAL_CHANNEL 2MEDIAN" )
                || name.toLowerCase().matches( "ch2_smtm" ) || name.equals( "R_MEAN" ) || name.equals( "Ch2SigMedian" )
                || name.equals( "ch2.Intensity" ) || name.equals( "CH2_SIG_MEAN" ) || name.equals( "CH2_ Median" )
                || name.toUpperCase().matches( "\\w{2}\\d{3}_CY5" ) || name.toUpperCase().matches( "NORM(.*)CH2" )
                || name.equals( "CH2Mean" ) || name.equals( "CH2_SIGNAL" ) || name.equals( "\"log2(635), gN\"" )
                || name.equals( "rProcessedSignal" );
    }

    /**
     * @param representation PrimitiveType
     * @param vectors raw vectors
     * @return matrix of appropriate type.
     */
    public static ExpressionDataMatrix getMatrix( PrimitiveType representation,
            Collection<DesignElementDataVector> vectors ) {
        ExpressionDataMatrix expressionDataMatrix = null;
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
     * @param dimensions
     * @param qTypes
     * @return
     */
    private ExpressionDataDoubleMatrix makeMatrix( List<BioAssayDimension> dimensions, List<QuantitationType> qTypes ) {
        if ( qTypes.size() > 0 ) {
            return new ExpressionDataDoubleMatrix( vectors, dimensions, qTypes );
        }
        return null;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
 * Helper class
 */
class QuantitationTypeData {

    Map<BioAssayDimension, QuantitationType> signalChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> signalChannelB = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> backgroundChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> backgroundChannelB = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> bkgSubChannelA = new HashMap<BioAssayDimension, QuantitationType>();
    Map<BioAssayDimension, QuantitationType> preferred = new HashMap<BioAssayDimension, QuantitationType>();

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

    public void setBackgroundChannelA( BioAssayDimension dim, QuantitationType backgroundChannelA ) {
        this.backgroundChannelA.put( dim, backgroundChannelA );
    }

    public void setBackgroundChannelB( BioAssayDimension dim, QuantitationType backgroundChannelB ) {
        this.backgroundChannelB.put( dim, backgroundChannelB );
    }

    public void setBkgSubChannelA( BioAssayDimension dim, QuantitationType bkgSubChannelA ) {
        this.bkgSubChannelA.put( dim, bkgSubChannelA );
    }

    public void setPreferred( BioAssayDimension dim, QuantitationType preferred ) {
        this.preferred.put( dim, preferred );
    }

    public void setSignalChannelA( BioAssayDimension dim, QuantitationType signalChannelA ) {
        this.signalChannelA.put( dim, signalChannelA );
    }

    public void setSignalChannelB( BioAssayDimension dim, QuantitationType signalChannelB ) {
        this.signalChannelB.put( dim, signalChannelB );
    }

}
