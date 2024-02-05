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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.expression.AnalysisUtilService;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.analysis.service.ExpressionExperimentVectorManipulatingService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;

import java.util.*;

/**
 * Switch an expression experiment from one array design to another. This is valuable when the EE uses more than on AD,
 * and a merged AD exists. The following steps are needed:
 * <ul>
 *
 * <li>Delete old analyses of this experiment and processeddata vectors
 * <li>For each array design, for each probe, identify the matching probe on the merged AD. Have to deal with situation
 * <li>where more than one occurrence of each sequence is found.
 * <li>all DEDVs must be switched to use the new AD's design elements
 * <li>all bioassays must be switched to the new AD.
 * <li>update the EE description
 * <li>commit changes.
 * <li>Computed processed data vectors
 * </ul>
 * This also handles the case of multisamples-per-platform - NOT the case of one-sample-per-platform but multiple
 * platforms; for that you have to run VectorMerging. For more nutty situations this will probably create a mess.
 *
 * @author pavlidis
 * @see    VectorMergingService
 */
@Service
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
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    private ExpressionExperimentSubSetService subsetService;

    @Autowired
    private AnalysisUtilService analysisUtilService;

    @Autowired
    private AuditTrailService auditTrailService;

    /**
     * If you know the array designs are already in a merged state, you should use switchExperimentToMergedPlatform
     *
     * @param  ee          ee
     * @param  arrayDesign The array design to switch to. If some samples already use that array design, nothing will be
     *                     changed for them.
     */
    @Transactional
    public void switchExperimentToArrayDesign( ExpressionExperiment ee, ArrayDesign arrayDesign ) {
        assert arrayDesign != null;

        ee = expressionExperimentService.thaw( ee );
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        // remove stuff that will be in the way.
        processedExpressionDataVectorService.removeProcessedDataVectors( ee );
        sampleCoexpressionAnalysisService.removeForExperiment( ee );
        for ( ExpressionExperimentSubSet subset : expressionExperimentService.getSubSets( ee ) ) {
            subsetService.remove( subset );
        }
        analysisUtilService.deleteOldAnalyses( ee );

        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );

        // get relation between sequence and designelements.
        Map<BioSequence, Collection<CompositeSequence>> designElementMap = new HashMap<>();
        Collection<CompositeSequence> elsWithNoSeq = new HashSet<>();
        this.populateCSeq( arrayDesign, designElementMap, elsWithNoSeq );

        if ( !elsWithNoSeq.isEmpty() ) {
            ExpressionExperimentPlatformSwitchService.log
                    .info( elsWithNoSeq.size() + " elements on the new platform have no associated sequence." );
            designElementMap.put( ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE, elsWithNoSeq );
        }

        boolean multiPlatformPerSample = this.switchPlatform( ee, arrayDesign );
        /*
         * For a multiplatform-per-sample case ("Case 2", platforms are disjoint): (note that some samples might just be
         * on one platform...)
         * 1. Pick a BAD that can be used for all DataVectors (it has all BioAssays in it).
         * 2. Switch vectors to use it - may require adding NaNs and reordering the vectors (switchDataForPlatform)
         * 3. Delete the Bioassays that are using other BADs (cleanupUnused)
         *
         * For Case 1 (each sample run on one platform, all platforms are related/similar) Simply switch the vectors
         * (switchDataForPlatform)
         */

        /*
         * Now we have to get the BADs. Problem to watch out for: they might not be the same length, we need one that
         * includes all BioMaterials.
         */
        Collection<BioAssayDimension> unusedBADs = new HashSet<>();
        BioAssayDimension targetBioAssayDimension = null;
        if ( multiPlatformPerSample ) {
            targetBioAssayDimension = this.doMultiSample( ee, unusedBADs );
        }
        if ( multiPlatformPerSample && targetBioAssayDimension == null ) {
            throw new RuntimeException( "Data set cannot be switched to merged platform: no suitable bioassaydimension found" );
        }

        /*
         * To account for cases where we have no data loaded yet.
         */
        boolean hasData = !ee.getQuantitationTypes().isEmpty() && !ee.getRawExpressionDataVectors().isEmpty();

        Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements = new HashMap<>();
        int totalVectorsSwitched = 0;
        for ( ArrayDesign oldAd : oldArrayDesigns ) {
            log.info( String.format( "Switching vectors from %s to %s", oldAd.getShortName(), arrayDesign.getShortName() ) );
            totalVectorsSwitched += this.switchDataForPlatform( ee, arrayDesign, designElementMap,
                    targetBioAssayDimension, usedDesignElements, oldAd );
        }

        if ( totalVectorsSwitched == 0 && hasData ) {
            throw new RuntimeException( "No vectors were switched" );
        }

        // a redundant check, but there have been problems.
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            if ( !arrayDesign.equals( v.getDesignElement().getArrayDesign() ) ) {
                throw new IllegalStateException( "A raw vector for QT =" + v.getQuantitationType()
                        + " was not correctly switched to the target platform " + arrayDesign );
            }
        }

        String descriptionUpdate = "[Switched to use " + arrayDesign.getShortName() + " by Gemma]";
        if ( !ee.getDescription().contains( descriptionUpdate ) ) {
            ee.setDescription( ee.getDescription() + " " + descriptionUpdate );
        }

        if ( targetBioAssayDimension != null && !unusedBADs.isEmpty() ) {
            log.info( "Cleaning up unused BioAssays from previous platforms..." );
            this.cleanupUnused( ee, unusedBADs, targetBioAssayDimension );
        }

        expressionExperimentService.update( ee );
        auditTrailService.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class,
                "Switch to use " + arrayDesign.getShortName() );
        log.info( "Completing switching " + ee ); // flush of transaction happens after this, can take a while.

        if ( hasData ) {
            log.info( ee + " has data, regenerating processed data vectors..." );
            processedExpressionDataVectorService.createProcessedDataVectors( ee ); // this still fails sometimes? works fine if run later by cli
        }
    }

    /**
     * Automatically identify an appropriate merged platform
     * @param expExp the experiment to switch to a merged platform
     * @return the selected merged platform the experiment was switched to
     */
    @Transactional
    public ArrayDesign switchExperimentToMergedPlatform( ExpressionExperiment expExp ) {
        ArrayDesign arrayDesign = this.locateMergedDesign( expExp );
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Experiment has no merged design to switch to" );
        this.switchExperimentToArrayDesign( expExp, arrayDesign );
        return arrayDesign;
    }

    private boolean switchPlatform( ExpressionExperiment ee, ArrayDesign arrayDesign ) {
        boolean multiPlatformPerSample = false;
        for ( BioAssay assay : ee.getBioAssays() ) {

            // Switch the assay to use the desired platform. However, if this is a second switch, don't lose the original value
            if ( assay.getOriginalPlatform() == null ) {
                assay.setOriginalPlatform( assay.getArrayDesignUsed() );
            }
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

    /**
     * Remove bioassays that are no longer needed
     *
     */
    private void cleanupUnused( ExpressionExperiment ee, Collection<BioAssayDimension> unusedBADs, BioAssayDimension maxBAD ) {
        ExpressionExperimentPlatformSwitchService.log.info( "Checking for unused BioAssays after merge" );

        for ( BioAssayDimension bioAssayDimension : unusedBADs ) {
            bioAssayDimensionService.remove( bioAssayDimension );
            log.info( "Removed unused BioAssayDimension: " + bioAssayDimension.getId() );
        }

        // remove any BioAssays that are not kept in the newly created dimension
        Collection<BioAssay> removed = new HashSet<>();
        for ( BioAssayDimension bioAssayDimension : unusedBADs ) {
            List<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
            for ( BioAssay ba : bioAssays ) {
                if ( !maxBAD.getBioAssays().contains( ba ) && !removed.contains( ba ) ) {
                    ee.getBioAssays().remove( ba );
                    ba.getSampleUsed().getBioAssaysUsedIn().remove( ba );
                    bioAssayService.remove( ba );
                    ExpressionExperimentPlatformSwitchService.log.info( "Removed unused BioAssay: " + ba );
                    removed.add( ba );
                }
            }
        }

        log.info( "Removed " + removed.size() + " unused bioassays" );
    }

    /**
     * Find the bioassaydimension that covers all the biomaterials.
     *
     */
    private BioAssayDimension doMultiSample( ExpressionExperiment ee, Collection<BioAssayDimension> unusedBADs ) {

        int maxSize = 0;
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
         * to make a new BAD.
         */
        if ( maxBAD != null ) {
            Collection<BioMaterial> bmsInmaxBAD = new HashSet<>();
            for ( BioAssay ba : maxBAD.getBioAssays() ) {
                bmsInmaxBAD.add( ba.getSampleUsed() );
            }

            for ( BioAssay ba : ee.getBioAssays() ) {
                if ( !bmsInmaxBAD.contains( ba.getSampleUsed() ) ) {

                    ExpressionExperimentPlatformSwitchService.log
                            .debug( "This experiment looked like it had samples run on more than one platform, "
                                    + "but it also has no BioAssayDimension that is eligible to accomodate all samples (Example: "
                                    + ba.getSampleUsed() );
                    maxBAD = null;
                    break;
                }
            }

        }

        if ( maxBAD == null ) {
            log.info( "Creating new bioassaydimension to accomodate merged data as no existing one was suitable" );
            maxBAD = BioAssayDimension.Factory.newInstance( "For " + ee.getBioAssays().size() +
                    " bioMaterials", "Created to accomodate platform switch", new ArrayList<>( ee.getBioAssays() ) );
            maxBAD = bioAssayDimensionService.create( maxBAD );
        }


        return maxBAD;
    }

    /**
     * Determine whether the two bioassaydimensions are the same, based on the samples used. Note it is inefficient to
     * call this over and over but it's not a big deal so far.
     *
     * @param  currentOrder current order
     * @param  desiredOrder desired order
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
            }

            if ( !mergedInto.equals( arrayDesign ) ) {
                throw new IllegalArgumentException(
                        design + " used by " + expExp + " is not merged into " + arrayDesign );
            }

        }
        return arrayDesign;
    }

    /**
     * Set up a map of sequences to elements for the platform we're switching to
     *
     */
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
     * @param  designElementMap      Mapping of sequences to probes for the platform that is being switch from. This is
     *                               used
     *                               to identify new candidates.
     * @param  usedDesignElements    probes from the new design that have already been assigned to probes from the old
     *                               design. If things are done correctly (the old design was merged into the new) then
     *                               there should be enough.
     *                               Map is of the new design probe to the old design probe it was used for (this is
     *                               debugging information)
     * @param  vector                vector
     * @param  bad                   BioAssayDimension to use, if necessary. If this is null or already the one used,
     *                               it's ignored.
     *                               Otherwise the vector data will be rewritten to match it.
     * @return true if a switch was made
     * @throws IllegalStateException if there is no (unused) design element matching the vector's biosequence
     */
    private boolean processVector( Map<BioSequence, Collection<CompositeSequence>> designElementMap,
            Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements, RawExpressionDataVector vector,
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
            return false;
            //            throw new IllegalStateException(
            //                    "There are no candidates probes for sequence: " + seq + "('null' should be okay)" );
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
        return true;
    }

    /**
     * Switch the vectors from one platform to the other, using the bioassaydimension passed if non-null
     *
     * @param  targetBioAssayDimension - if null we keep the current one; otherwise we rewrite data datavectors to use
     *                                 the new one.
     * @return how many vectors were switched
     */
    private int switchDataForPlatform( ExpressionExperiment ee, ArrayDesign arrayDesign,
            Map<BioSequence, Collection<CompositeSequence>> designElementMap, BioAssayDimension targetBioAssayDimension,
            Map<CompositeSequence, Collection<BioAssayDimension>> usedDesignElements, ArrayDesign oldAd ) {
        if ( oldAd.equals( arrayDesign ) )
            return 0;

        if ( oldAd.getCompositeSequences().isEmpty() && !oldAd.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
            /*
             * Bug 3451 - this is okay if it is a RNA-seq experiment etc. prior to data upload.
             */
            throw new IllegalStateException( oldAd + " has no elements" );
        }

        int totalSwitched = 0;
        Collection<QuantitationType> qts = ee.getQuantitationTypes();
        ExpressionExperimentPlatformSwitchService.log
                .info( "Processing " + qts.size() + " quantitation types for vectors on " + oldAd );
        for ( QuantitationType type : qts ) {

            // use each design element only once per quantitation type + bioassaydimension per array design
            usedDesignElements.clear();

            // assumption: we have no processed data. They should have been deleted at this point.
            Collection<RawExpressionDataVector> vecsForQt = new HashSet<>();

            //

            int count = 0;
            for ( RawExpressionDataVector vector : ee.getRawExpressionDataVectors() ) {

                if ( !vector.getQuantitationType().equals( type ) ) {
                    continue;
                }

                CompositeSequence oldDe = vector.getDesignElement();

                if ( !oldDe.getArrayDesign().equals( oldAd ) ) {
                    continue;
                }
                vecsForQt.add( vector );

            }

            if ( vecsForQt.isEmpty() ) {
                /*
                 * This can happen when the quantitation types vary for the array designs.
                 */
                log.info( "No vectors for " + type + " on " + oldAd + " (Might be okay; not all QTs might be on all platforms)" );
                continue;
            }

            log.info( "Switching " + vecsForQt.size() + " vectors for " + type + " from " + oldAd.getShortName()
                    + " to " + arrayDesign.getShortName()
                    + ( targetBioAssayDimension == null ? "" : ", BioAssayDimension=" + targetBioAssayDimension ) );

            int numwarns = 0;
            int maxwarns = 30;
            for ( RawExpressionDataVector vector : vecsForQt ) {
                if ( this.processVector( designElementMap, usedDesignElements, vector, targetBioAssayDimension ) ) {
                    count++;
                } else {
                    if ( numwarns++ < maxwarns ) {
                        log.warn( "No matching element found on target platform for " + vector.getDesignElement() );
                    }
                    if ( numwarns == maxwarns ) {
                        log.warn( "[Further no-match warnings suppressed]" );
                    }
                }
            }

            if ( count != vecsForQt.size() ) {
                throw new IllegalStateException(
                        "Found matches for only " + count + "/" + vecsForQt.size() + " vectors for " + type + " from " + oldAd.getShortName()
                                + ( targetBioAssayDimension == null ? "" : ", BioAssayDimension=" + targetBioAssayDimension ) );
            }

            // sanity check. this is all fine.
            //            for ( RawExpressionDataVector v : vecsForQt ) {
            //                if ( v.getQuantitationType().equals( type )
            //                        && !arrayDesign.equals( v.getDesignElement().getArrayDesign() ) ) {
            //                    throw new IllegalStateException( "A raw vector for QT =" + v.getQuantitationType()
            //                            + " was not correctly switched to the target platform " + arrayDesign + ", it was on "
            //                            + v.getDesignElement().getArrayDesign() + " while switching from " + oldAd );
            //                }
            //            }

            //this check shouldn't be necessary
            for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
                if ( v.getQuantitationType().equals( type )
                        && !arrayDesign.equals( v.getDesignElement().getArrayDesign() ) && v.getDesignElement().getArrayDesign().equals( oldAd ) ) {
                    throw new IllegalStateException( "A raw vector " + v + "for QT =" + v.getQuantitationType()
                            + " was not correctly switched to the target platform " + arrayDesign + ", it was still on "
                            + v.getDesignElement().getArrayDesign() );
                }
            }

            totalSwitched += count;

        }
        return totalSwitched;
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
