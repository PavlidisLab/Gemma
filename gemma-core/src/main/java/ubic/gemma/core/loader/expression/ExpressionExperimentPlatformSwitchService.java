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
package ubic.gemma.core.loader.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.core.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.analysis.service.ExpressionExperimentVectorManipulatingService;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;

/**
 * Switch an expression experiment from one array design to another. This is valuable when the EE uses more than on AD,
 * and a merged AD exists. The following steps are needed:
 * <ul>
 * <li>For each array design, for each probe, identify the matching probe on the merged AD. Have to deal with situation
 * <li>where more than one occurrence of each sequence is found.
 * <li>all DEDVs must be switched to use the new AD's design elements
 * <li>all bioassays must be switched to the new AD.
 * <li>Delete old analyses of this experiment
 * <li>update the EE description
 * <li>commit changes.
 * </ul>
 *
 * <p>
 * This also handles the case of multisamples-per-platform - NOT the case of one-sample-per-platform but multiple
 * platforms; for that you have to run VectorMerging. For more nutty situations this will probably create a mess.
 *
 * @author pavlidis
 * @see VectorMergingService
 */
@Component
public class ExpressionExperimentPlatformSwitchService extends ExpressionExperimentVectorManipulatingService {

    private static Log log = LogFactory.getLog( ExpressionExperimentPlatformSwitchService.class.getName() );

    /**
     * Used to identify design elements that have no sequence associated with them.
     */
    public static final BioSequence NULL_BIOSEQUENCE;

    static {
        NULL_BIOSEQUENCE = BioSequence.Factory.newInstance();
        NULL_BIOSEQUENCE.setName( "______NULL______" );
        NULL_BIOSEQUENCE.setId( -1L );
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExperimentPlatformSwitchHelperService helperService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private ExpressionExperimentSubSetService subsetService;

    /**
     * If you know the arraydesigns are already in a merged state, you should use switchExperimentToMergedPlatform
     *
     * @param expExp
     * @param arrayDesign The array design to switch to. If some samples already use that array design, nothing will be
     *        changed for them.
     */
    @SuppressWarnings("unchecked")
    public ExpressionExperiment switchExperimentToArrayDesign( ExpressionExperiment expExp, ArrayDesign arrayDesign ) {
        assert arrayDesign != null;

        // remove stuff that will be in the way.
        processedExpressionDataVectorService.removeProcessedDataVectors( expExp );
        sampleCoexpressionMatrixService.delete( expExp );
        for ( ExpressionExperimentSubSet subset : expressionExperimentService.getSubSets( expExp ) ) {
            subsetService.delete( subset );
        }

        // get relation between sequence and designelements.
        Map<BioSequence, Collection<CompositeSequence>> designElementMap = new HashMap<>();
        Collection<CompositeSequence> elsWithNoSeq = new HashSet<>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                elsWithNoSeq.add( cs );
            } else {
                if ( !designElementMap.containsKey( bs ) ) {
                    designElementMap.put( bs, new HashSet<CompositeSequence>() );
                }
                designElementMap.get( bs ).add( cs );
            }
        }

        expExp = expressionExperimentService.thaw( expExp );

        log.info( elsWithNoSeq.size() + " elements on the new platform have no associated sequence." );
        designElementMap.put( NULL_BIOSEQUENCE, elsWithNoSeq );

        boolean multiPlatformPerSample = false;
        for ( BioAssay assay : expExp.getBioAssays() ) {

            // Switch the assay to use the desired platform
            assay.setArrayDesignUsed( arrayDesign );

            /*
             * Side effect: Detect cases of each-sample-run-on-multiple-platforms - we need to merge the bioassays, too.
             * That means fiddling with the BioAssayDimensions (BADs). We do this check here to avoid unnecessarily
             * inspecting BADs.
             */
            if ( !multiPlatformPerSample && assay.getSampleUsed().getBioAssaysUsedIn().size() > 1 ) {
                multiPlatformPerSample = true;
            }
        }

        /*
         * For a multiplatform-per-sample case: (note that some samples might just be on one platform...)
         * 1. Pick a BAD that can be used for all DataVectors (it has all BioAssays in it).
         * 2. Switch vectors to use it - may require adding NaNs and reordering the vectors
         * 3. Delete the Bioassays that are using other BADs
         */

        /*
         * Now we have to get the BADs. Problem to watch out for: they might not be the same length, we need one that
         * includes all BioMaterials.
         */
        Collection<BioAssayDimension> unusedBADs = new HashSet<>();
        BioAssayDimension maxBAD = null;
        int maxSize = 0;
        if ( multiPlatformPerSample ) {
            for ( BioAssay ba : expExp.getBioAssays() ) {
                Collection<BioAssayDimension> oldBioAssayDims = bioAssayService.findBioAssayDimensions( ba );
                for ( BioAssayDimension bioAssayDim : oldBioAssayDims ) {
                    unusedBADs.add( bioAssayDim );
                    int size = bioAssayDim.getBioAssays().size();

                    if ( size > maxSize ) {
                        maxSize = size;
                        maxBAD = bioAssayDim;
                    }
                }
            }

            assert unusedBADs.size() > 1; // otherwise we shouldn't be here.
            unusedBADs.remove( maxBAD );

            /*
             * Make sure all biomaterials in the study are included in the chosen bioassaydimension. If not, we'd have
             * to make a new BAD. I haven't implemented that case.
             */
            if ( maxBAD != null ) {
                Collection<BioMaterial> bmsInmaxBAD = new HashSet<>();
                for ( BioAssay ba : maxBAD.getBioAssays() ) {
                    bmsInmaxBAD.add( ba.getSampleUsed() );
                }

                for ( BioAssay ba : expExp.getBioAssays() ) {
                    if ( !bmsInmaxBAD.contains( ba.getSampleUsed() ) ) {

                        log.warn(
                                "This experiment looked like it had samples run on more than one platform, "
                                        + "but it also has no BioAssayDimension that is eligible to accomodate all samples (Example: "
                                        + ba.getSampleUsed()
                                        + ") The experiment will be switched to the merged platform, but no BioAssayDimension switch will be done." );
                        multiPlatformPerSample = false;
                        maxBAD = null;
                        break;
                    }
                }

            }
        }

        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );
        Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements = new HashMap<>();
        for ( ArrayDesign oldAd : oldArrayDesigns ) {
            if ( oldAd.equals( arrayDesign ) ) continue;

            oldAd = arrayDesignService.thaw( oldAd );

            if ( oldAd.getCompositeSequences().size() == 0 && !oldAd.getTechnologyType().equals( TechnologyType.NONE ) ) {
                /*
                 * Bug 3451 - this is okay if it is a RNA-seq experiment etc. prior to data upload.
                 */
                throw new IllegalStateException( oldAd + " has no elements" );
            }

            Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp, oldAd );
            log.info( "Processing " + qts.size() + " quantitation types for vectors on " + oldAd );
            for ( QuantitationType type : qts ) {

                // use each design element only once per quantitation type + bioassaydimension per array design
                usedDesignElements.clear();

                Collection<? extends DesignElementDataVector> vectorsForQt = getVectorsForOneQuantitationType( oldAd,
                        type );

                if ( vectorsForQt == null || vectorsForQt.size() == 0 ) {
                    /*
                     * This can happen when the quantitation types vary for the array designs.
                     */
                    log.debug( "No vectors for " + type + " on " + oldAd );
                    continue;
                }

                log.info( "Switching " + vectorsForQt.size() + " vectors for " + type + " from " + oldAd.getShortName()
                        + " to " + arrayDesign.getShortName() );

                int count = 0;
                Class<? extends DesignElementDataVector> vectorClass = null;
                Collection<DesignElementDataVector> unMatched = new HashSet<>();
                for ( DesignElementDataVector vector : vectorsForQt ) {

                    if ( vectorClass == null ) {
                        vectorClass = vector.getClass();
                    }

                    if ( !vector.getClass().equals( vectorClass ) ) {
                        throw new IllegalStateException( "Two types of vector for one quantitationtype: " + type );
                    }

                    assert RawExpressionDataVector.class.isAssignableFrom( vector.getClass() ) : "Unexpected class: "
                            + vector.getClass().getName();

                    CompositeSequence oldDe = vector.getDesignElement();

                    if ( oldDe.getArrayDesign().equals( arrayDesign ) ) {
                        continue;
                    }

                    boolean ok = processVector( designElementMap, usedDesignElements, vector, maxBAD );

                    if ( !ok ) {
                        log.warn( "No new design element available to match " + oldDe + " (seq="
                                + oldDe.getBiologicalCharacteristic() + "; array=" + oldDe.getArrayDesign() );
                        unMatched.add( vector );
                        /*
                         * This can happen if there is no biosequence associated with the element.
                         */
                    }

                    if ( ++count % 20000 == 0 ) {
                        log.info( "Found matches for " + count + " vectors for " + type );
                    }
                }

                /*
                 * This is bad.
                 */
                if ( unMatched.size() > 0 ) {
                    throw new IllegalStateException( "There were " + unMatched.size()
                            + " vectors that couldn't be matched to the new design for: " + type + ", example: "
                            + unMatched.iterator().next() );
                }

                if ( RawExpressionDataVector.class.isAssignableFrom( vectorClass ) ) {
                    int s = expExp.getRawExpressionDataVectors().size();
                    expExp.getRawExpressionDataVectors().removeAll( vectorsForQt );
                    assert s > expExp.getRawExpressionDataVectors().size();
                    expExp.getRawExpressionDataVectors().addAll(
                            ( Collection<? extends RawExpressionDataVector> ) vectorsForQt );
                    assert s == expExp.getRawExpressionDataVectors().size();
                } else {
                    int s = expExp.getProcessedExpressionDataVectors().size();
                    expExp.getProcessedExpressionDataVectors().removeAll( vectorsForQt );
                    assert s > expExp.getProcessedExpressionDataVectors().size();
                    expExp.getProcessedExpressionDataVectors().addAll(
                            ( Collection<? extends ProcessedExpressionDataVector> ) vectorsForQt );
                    assert s == expExp.getProcessedExpressionDataVectors().size();
                }
            }
        }

        expExp.setDescription( expExp.getDescription() + " [Switched to use " + arrayDesign.getShortName() + " by Gemma]" );

        helperService.persist( expExp, arrayDesign );

        if ( maxBAD != null && !unusedBADs.isEmpty() ) {
            log.info( "Cleaning up unused BioAssayDimensions and BioAssays after merge" );
            // Delete them and the bioassays associated with them.
            for ( BioAssayDimension bioAssayDimension : unusedBADs ) {
                List<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
                bioAssayDimensionService.remove( bioAssayDimension );
                bioAssayService.remove( bioAssays );
            }
        }

        return expExp;
    }

    /**
     * Automatically identify an appropriate merged platform
     * 
     * @param expExp
     * @return
     */
    public ExpressionExperiment switchExperimentToMergedPlatform( ExpressionExperiment expExp ) {
        ArrayDesign arrayDesign = locateMergedDesign( expExp );
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Experiment has no merged design to switch to" );
        return this.switchExperimentToArrayDesign( expExp, arrayDesign );
    }

    /**
     * Determine whether the two bioassaydimensions are the same, based on the samples used. Note it is inefficient to
     * call this over and over but it's not a big deal so far.
     *
     * @param currentOrder
     * @param desiredOrder
     * @return
     */
    private boolean equivalent( List<BioAssay> currentOrder, List<BioAssay> desiredOrder ) {
        if ( currentOrder.size() != desiredOrder.size() ) {
            return false;
        }

        for ( int i = 0; i < currentOrder.size(); i++ ) {
            if ( !currentOrder.get( i ).getSampleUsed().equals( desiredOrder.get( i ).getSampleUsed() ) ) {
                return false;
            }
        }

        return true;

    }

    private ArrayDesign locateMergedDesign( ExpressionExperiment expExp ) {
        // get the array designs for this EE
        ArrayDesign arrayDesign = null;
        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        // find the AD they have been merged into, make sure it is exists and they are all merged into the same AD.
        for ( ArrayDesign design : oldArrayDesigns ) {
            ArrayDesign mergedInto = design.getMergedInto();
            mergedInto = arrayDesignService.thaw( mergedInto );

            if ( mergedInto == null ) {
                throw new IllegalArgumentException( design + " used by " + expExp
                        + " is not merged into another design" );
            }

            // TODO: go up the merge tree to find the root. This is too slow.
            // while ( mergedInto.getMergedInto() != null ) {
            // mergedInto = arrayDesignService.thaw( mergedInto.getMergedInto() );
            // }

            if ( arrayDesign == null ) {
                arrayDesign = mergedInto;
                arrayDesign = arrayDesignService.thaw( arrayDesign );
            }

            if ( !mergedInto.equals( arrayDesign ) ) {
                throw new IllegalArgumentException( design + " used by " + expExp + " is not merged into "
                        + arrayDesign );
            }

        }
        return arrayDesign;
    }

    /**
     * @param designElementMap Mapping of sequences to probes for the platform that is being switch from. This is used
     *        to identify new candidates.
     * @param usedDesignElements probes from the new design that have already been assigned to probes from the old
     *        design. If things are done correctly (the old design was merged into the new) then there should be enough.
     *        Map is of the new design probe to the old design probe it was used for (this is debugging information)
     * @param vector
     * @param bad BioAssayDimension to use, if necessary. If this is null or already the one used, it's igored.
     *        Otherwise the vector data will be rewritten to match it.
     * @throw IllegalStateException if there is no (unused) design element matching the vector's biosequence
     */
    private boolean processVector( Map<BioSequence, Collection<CompositeSequence>> designElementMap,
            Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements, DesignElementDataVector vector, BioAssayDimension bad ) {
        CompositeSequence oldDe = vector.getDesignElement();

        Collection<CompositeSequence> newElCandidates = null;
        BioSequence seq = oldDe.getBiologicalCharacteristic();
        if ( seq == null ) {
            newElCandidates = designElementMap.get( NULL_BIOSEQUENCE );
        } else {
            newElCandidates = designElementMap.get( seq );
        }

        if ( newElCandidates == null || newElCandidates.isEmpty() ) {
            throw new IllegalStateException( "There are no candidates probes for sequence: " + seq
                    + "('null' should be okay)" );
        }

        for ( CompositeSequence newEl : newElCandidates ) {
            if ( !usedDesignElements.containsKey( newEl ) ) {

                vector.setDesignElement( newEl );
                usedDesignElements.put( newEl, new HashSet<BioAssayDimension>() );
                usedDesignElements.get( newEl ).add( vector.getBioAssayDimension() );
                //         assert !newEl.getArrayDesign().equals( oldDe.getArrayDesign() );
                break;
            }

            if ( !usedDesignElements.get( newEl ).contains( vector.getBioAssayDimension() ) ) {
                /*
                 * Then it's okay to use it.
                 */
                vector.setDesignElement( newEl );
                usedDesignElements.get( newEl ).add( vector.getBioAssayDimension() );
                //       assert !newEl.getArrayDesign().equals( oldDe.getArrayDesign() );
                break;
            }
        }

        if ( bad != null && !vector.getBioAssayDimension().equals( bad ) ) {

            /*
             * 1. Check if they are already the same; then just switch it to the desired BAD
             * 2. If not, then the vector data has to be rewritten.
             */
            vectorReWrite( vector, bad );
        }

        return true;
    }

    /**
     * Rearrange/expand a vector as necessary to use the given BioAssayDimension. Only used for multiplatform case of
     * samples run on multiple platforms.
     *
     * @param vector
     * @param bad to be used as the replacement.
     */
    private void vectorReWrite( DesignElementDataVector vector, BioAssayDimension bad ) {
        List<BioAssay> desiredOrder = bad.getBioAssays();
        List<BioAssay> currentOrder = vector.getBioAssayDimension().getBioAssays();
        if ( equivalent( currentOrder, desiredOrder ) ) {
            // Easy, we can just switch it.
            vector.setBioAssayDimension( bad );
            return;
        }

        /*
         * We remake the data vector following the new ordering.
         */
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        Object missingVal = null;
        if ( representation.equals( PrimitiveType.DOUBLE ) ) {
            missingVal = Double.NaN;
        } else if ( representation.equals( PrimitiveType.STRING ) ) {
            missingVal = "";
        } else if ( representation.equals( PrimitiveType.INT ) ) {
            missingVal = 0;
        } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
            missingVal = false;
        } else {
            throw new UnsupportedOperationException( "Missing values in data vectors of type " + representation
                    + " not supported (when processing " + vector );
        }

        List<Object> oldData = new ArrayList<>();
        super.convertFromBytes( oldData, vector.getQuantitationType().getRepresentation(), vector );

        /*
         * Now data has the old data, so we need to rearrange it to match, inserting missings as necessary.
         */
        Map<BioMaterial, Integer> bm2loc = new HashMap<>();
        int i = 0;
        List<Object> newData = new ArrayList<>();
        // initialize
        for ( BioAssay ba : desiredOrder ) {
            bm2loc.put( ba.getSampleUsed(), i++ );
            newData.add( missingVal );
        }

        // Put data into new locations
        int j = 0;
        for ( BioAssay ba : currentOrder ) {
            Integer loc = bm2loc.get( ba.getSampleUsed() );
            assert loc != null;
            newData.set( loc, oldData.get( j++ ) );
        }

        byte[] newDataAr = converter.toBytes( newData.toArray() );
        vector.setData( newDataAr );
        vector.setBioAssayDimension( bad );

    }
}
