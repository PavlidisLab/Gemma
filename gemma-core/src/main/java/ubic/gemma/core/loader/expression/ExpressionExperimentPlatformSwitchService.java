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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
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
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;

import java.util.*;

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
 * This also handles the case of multisamples-per-platform - NOT the case of one-sample-per-platform but multiple
 * platforms; for that you have to run VectorMerging. For more nutty situations this will probably create a mess.
 *
 * @author pavlidis
 * @see VectorMergingService
 */
@Component
public class ExpressionExperimentPlatformSwitchService extends ExpressionExperimentVectorManipulatingService {

    /**
     * Used to identify design elements that have no sequence associated with them.
     */
    public static final BioSequence NULL_BIOSEQUENCE;
    private static final Log log = LogFactory.getLog( ExpressionExperimentPlatformSwitchService.class.getName() );

    static {
        NULL_BIOSEQUENCE = BioSequence.Factory.newInstance();
        ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE.setName( "______NULL______" );
        ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE.setId( -1L );
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExperimentPlatformSwitchHelperService helperService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    private ExpressionExperimentSubSetService subsetService;

    /**
     * If you know the array designs are already in a merged state, you should use switchExperimentToMergedPlatform
     *
     * @param ee          ee
     * @param arrayDesign The array design to switch to. If some samples already use that array design, nothing will be
     *                    changed for them.
     * @return the switched experiment
     */
    public ExpressionExperiment switchExperimentToArrayDesign( ExpressionExperiment ee, ArrayDesign arrayDesign ) {
        assert arrayDesign != null;

        // remove stuff that will be in the way.
        processedExpressionDataVectorService.removeProcessedDataVectors( ee );
        sampleCoexpressionAnalysisService.removeForExperiment( ee );
        for ( ExpressionExperimentSubSet subset : expressionExperimentService.getSubSets( ee ) ) {
            subsetService.remove( subset );
        }

        // get relation between sequence and designelements.
        Map<BioSequence, Collection<CompositeSequence>> designElementMap = new HashMap<>();
        Collection<CompositeSequence> elsWithNoSeq = new HashSet<>();
        this.populateCSeq( arrayDesign, designElementMap, elsWithNoSeq );

        ee = expressionExperimentService.thaw( ee );

        ExpressionExperimentPlatformSwitchService.log
                .info( elsWithNoSeq.size() + " elements on the new platform have no associated sequence." );
        designElementMap.put( ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE, elsWithNoSeq );

        boolean multiPlatformPerSample = this.checkMultiPerSample( ee, arrayDesign );
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
            maxBAD = this.doMultiSample( ee, unusedBADs, maxSize );
        }

        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
        Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements = new HashMap<>();
        for ( ArrayDesign oldAd : oldArrayDesigns ) {
            this.runOldAd( ee, arrayDesign, designElementMap, maxBAD, usedDesignElements, oldAd );
        }

        ee.setDescription( ee.getDescription() + " [Switched to use " + arrayDesign.getShortName() + " by Gemma]" );

        helperService.persist( ee, arrayDesign );

        /*
         * This might need to be done inside the transaction we're using to make the switch.
         */
        if ( maxBAD != null && !unusedBADs.isEmpty() ) {
            this.checkUnused( unusedBADs, maxBAD );
        }

        return ee;
    }

    /**
     * Automatically identify an appropriate merged platform
     *
     * @param expExp ee
     * @return ee
     */
    public ExpressionExperiment switchExperimentToMergedPlatform( ExpressionExperiment expExp ) {
        ArrayDesign arrayDesign = this.locateMergedDesign( expExp );
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Experiment has no merged design to switch to" );
        return this.switchExperimentToArrayDesign( expExp, arrayDesign );
    }

    private void checkUnused( Collection<BioAssayDimension> unusedBADs, BioAssayDimension maxBAD ) {
        ExpressionExperimentPlatformSwitchService.log.info( "Checking for unused BioAssays after merge" );

        for ( BioAssayDimension bioAssayDimension : unusedBADs ) {
            List<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
            for ( BioAssay ba : bioAssays ) {
                if ( !maxBAD.getBioAssays().contains( ba ) ) {
                    ExpressionExperimentPlatformSwitchService.log.info( "Unused bioassay: " + ba );
                }
            }
        }
    }

    private void runOldAd( ExpressionExperiment ee, ArrayDesign arrayDesign,
            Map<BioSequence, Collection<CompositeSequence>> designElementMap, BioAssayDimension maxBAD,
            Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements, ArrayDesign oldAd ) {
        if ( oldAd.equals( arrayDesign ) )
            return;

        oldAd = arrayDesignService.thaw( oldAd );

        if ( oldAd.getCompositeSequences().size() == 0 && !oldAd.getTechnologyType().equals( TechnologyType.NONE ) ) {
            /*
             * Bug 3451 - this is okay if it is a RNA-seq experiment etc. prior to data upload.
             */
            throw new IllegalStateException( oldAd + " has no elements" );
        }

        Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( ee, oldAd );
        ExpressionExperimentPlatformSwitchService.log
                .info( "Processing " + qts.size() + " quantitation types for vectors on " + oldAd );
        for ( QuantitationType type : qts ) {

            // use each design element only once per quantitation type + bioassaydimension per array design
            usedDesignElements.clear();

            Collection<RawExpressionDataVector> rawForQt = this.getRawVectorsForOneQuantitationType( oldAd, type );
            Collection<ProcessedExpressionDataVector> processedForQt = this
                    .getProcessedVectorsForOneQuantitationType( oldAd, type );

            if ( ( rawForQt == null || rawForQt.size() == 0 ) //
                    && ( processedForQt == null || processedForQt.size() == 0 ) ) {
                /*
                 * This can happen when the quantitation types vary for the array designs.
                 */
                ExpressionExperimentPlatformSwitchService.log.debug( "No vectors for " + type + " on " + oldAd );
                continue;
            }

            // This check assures we do not mix raw and processed vectors further down the line
            if ( ( rawForQt != null && rawForQt.size() > 0 ) && ( processedForQt != null
                    && processedForQt.size() > 0 ) ) {
                throw new IllegalStateException( "Two types of vector for quantitationType " + type );
            }

            Collection<DesignElementDataVector> vectors = new HashSet<>();

            if ( rawForQt != null ) {
                vectors.addAll( rawForQt );
            }
            if ( processedForQt != null ) {
                vectors.addAll( processedForQt );
            }

            ExpressionExperimentPlatformSwitchService.log
                    .info( "Switching " + vectors.size() + " vectors for " + type + " from " + oldAd.getShortName()
                            + " to " + arrayDesign.getShortName() );

            int count = 0;
            //noinspection MismatchedQueryAndUpdateOfCollection // Only used for logging
            Collection<DesignElementDataVector> unMatched = new HashSet<>();
            for ( DesignElementDataVector vector : vectors ) {

                assert RawExpressionDataVector.class.isAssignableFrom( vector.getClass() ) :
                        "Unexpected class: " + vector.getClass().getName();

                CompositeSequence oldDe = vector.getDesignElement();

                if ( oldDe.getArrayDesign().equals( arrayDesign ) ) {
                    continue;
                }

                this.processVector( designElementMap, usedDesignElements, vector, maxBAD );

                if ( ++count % 20000 == 0 ) {
                    ExpressionExperimentPlatformSwitchService.log
                            .info( "Found matches for " + count + " vectors for " + type );
                }
            }

            /*
             * This is bad.
             */
            if ( unMatched.size() > 0 ) {
                throw new IllegalStateException(
                        "There were " + unMatched.size() + " vectors that couldn't be matched to the new design for: "
                                + type + ", example: " + unMatched.iterator().next() );
            }

            // Force collection update
            if ( rawForQt != null && rawForQt.size() > 0 ) {
                int s = ee.getRawExpressionDataVectors().size();
                ee.getRawExpressionDataVectors().removeAll( rawForQt );
                assert s > ee.getRawExpressionDataVectors().size();
                ee.getRawExpressionDataVectors().addAll( rawForQt );
                assert s == ee.getRawExpressionDataVectors().size();
            } else if ( processedForQt != null && processedForQt.size() > 0 ) {
                int s = ee.getProcessedExpressionDataVectors().size();
                ee.getProcessedExpressionDataVectors().removeAll( processedForQt );
                assert s > ee.getProcessedExpressionDataVectors().size();
                ee.getProcessedExpressionDataVectors().addAll( processedForQt );
                assert s == ee.getProcessedExpressionDataVectors().size();
            }
        }
    }

    private BioAssayDimension doMultiSample( ExpressionExperiment ee, Collection<BioAssayDimension> unusedBADs,
            int maxSize ) {
        BioAssayDimension maxBAD = null;
        for ( BioAssay ba : ee.getBioAssays() ) {
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

            for ( BioAssay ba : ee.getBioAssays() ) {
                if ( !bmsInmaxBAD.contains( ba.getSampleUsed() ) ) {

                    ExpressionExperimentPlatformSwitchService.log
                            .warn( "This experiment looked like it had samples run on more than one platform, "
                                    + "but it also has no BioAssayDimension that is eligible to accomodate all samples (Example: "
                                    + ba.getSampleUsed()
                                    + ") The experiment will be switched to the merged platform, but no BioAssayDimension switch will be done." );
                    maxBAD = null;
                    break;
                }
            }

        }
        return maxBAD;
    }

    private boolean checkMultiPerSample( ExpressionExperiment ee, ArrayDesign arrayDesign ) {
        boolean multiPlatformPerSample = false;
        for ( BioAssay assay : ee.getBioAssays() ) {

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
        return multiPlatformPerSample;
    }

    private void populateCSeq( ArrayDesign arrayDesign,
            Map<BioSequence, Collection<CompositeSequence>> designElementMap,
            Collection<CompositeSequence> elsWithNoSeq ) {
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
    }

    /**
     * Determine whether the two bioassaydimensions are the same, based on the samples used. Note it is inefficient to
     * call this over and over but it's not a big deal so far.
     *
     * @param currentOrder current order
     * @param desiredOrder desired order
     * @return boolean
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
                throw new IllegalArgumentException(
                        design + " used by " + expExp + " is not merged into another design" );
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
                throw new IllegalArgumentException(
                        design + " used by " + expExp + " is not merged into " + arrayDesign );
            }

        }
        return arrayDesign;
    }

    /**
     * @param designElementMap   Mapping of sequences to probes for the platform that is being switch from. This is used
     *                           to identify new candidates.
     * @param usedDesignElements probes from the new design that have already been assigned to probes from the old
     *                           design. If things are done correctly (the old design was merged into the new) then there should be enough.
     *                           Map is of the new design probe to the old design probe it was used for (this is debugging information)
     * @param vector             vector
     * @param bad                BioAssayDimension to use, if necessary. If this is null or already the one used, it's igored.
     *                           Otherwise the vector data will be rewritten to match it.
     * @throws IllegalStateException if there is no (unused) design element matching the vector's biosequence
     */
    private void processVector( Map<BioSequence, Collection<CompositeSequence>> designElementMap,
            Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements, DesignElementDataVector vector,
            BioAssayDimension bad ) {
        CompositeSequence oldDe = vector.getDesignElement();

        Collection<CompositeSequence> newElCandidates;
        BioSequence seq = oldDe.getBiologicalCharacteristic();
        if ( seq == null ) {
            newElCandidates = designElementMap.get( ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE );
        } else {
            newElCandidates = designElementMap.get( seq );
        }

        if ( newElCandidates == null || newElCandidates.isEmpty() ) {
            throw new IllegalStateException(
                    "There are no candidates probes for sequence: " + seq + "('null' should be okay)" );
        }

        for ( CompositeSequence newEl : newElCandidates ) {
            if ( !usedDesignElements.containsKey( newEl ) ) {

                vector.setDesignElement( newEl );
                usedDesignElements.put( newEl, new HashSet<BioAssayDimension>() );
                usedDesignElements.get( newEl ).add( vector.getBioAssayDimension() );
                break;
            }

            if ( !usedDesignElements.get( newEl ).contains( vector.getBioAssayDimension() ) ) {
                /*
                 * Then it's okay to use it.
                 */
                vector.setDesignElement( newEl );
                usedDesignElements.get( newEl ).add( vector.getBioAssayDimension() );
                break;
            }
        }

        if ( bad != null && !vector.getBioAssayDimension().equals( bad ) ) {
            /*
             * 1. Check if they are already the same; then just switch it to the desired BAD
             * 2. If not, then the vector data has to be rewritten.
             */
            this.vectorReWrite( vector, bad );
        }
    }

    /**
     * Rearrange/expand a vector as necessary to use the given BioAssayDimension. Only used for multiplatform case of
     * samples run on multiple platforms.
     *
     * @param vector vector
     * @param bad    to be used as the replacement.
     */
    private void vectorReWrite( DesignElementDataVector vector, BioAssayDimension bad ) {
        List<BioAssay> desiredOrder = bad.getBioAssays();
        List<BioAssay> currentOrder = vector.getBioAssayDimension().getBioAssays();
        if ( this.equivalent( currentOrder, desiredOrder ) ) {
            // Easy, we can just switch it.
            vector.setBioAssayDimension( bad );
            return;
        }

        /*
         * We remake the data vector following the new ordering.
         */
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        Object missingVal;
        if ( representation.equals( PrimitiveType.DOUBLE ) ) {
            missingVal = Double.NaN;
        } else if ( representation.equals( PrimitiveType.STRING ) ) {
            missingVal = "";
        } else if ( representation.equals( PrimitiveType.INT ) ) {
            missingVal = 0;
        } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
            missingVal = false;
        } else {
            throw new UnsupportedOperationException(
                    "Missing values in data vectors of type " + representation + " not supported (when processing "
                            + vector );
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
