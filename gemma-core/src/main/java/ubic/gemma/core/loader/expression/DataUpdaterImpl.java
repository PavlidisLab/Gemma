/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.expression;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.arrayDesign.AffyChipTypeExtractor;
import ubic.gemma.core.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Update or fill in the data associated with an experiment. Cases include reprocessing data from CEL files (Affymetrix,
 * GEO only), inserting data for RNA-seq data sets but also generic cases where data didn't come from GEO and we need to
 * add or replace data.
 * For loading experiments from flat files, see SimpleExpressionDataLoaderService
 *
 * @author paul
 */
@Service
public class DataUpdaterImpl implements DataUpdater {

    private static final Log log = LogFactory.getLog( DataUpdaterImpl.class );

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioAssayDimensionService assayDimensionService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentPlatformSwitchService experimentPlatformSwitchService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private PrincipalComponentAnalysisService pcaService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Autowired
    private QuantitationTypeService qtService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCorService;

    @Autowired
    private VectorMergingService vectorMergingService;

    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;

    /**
     * Affymetrix: Use to bypass the automated running of apt-probeset-summarize. For example if GEO doesn't have
     * them and we ran apt-probeset-summarize ourselves, or if some GEO files were corrupted (in which case the file
     * used here must have blank columns added with headers for the unused samples).
     * Must be single-platform. Will switch the data set to use the "right" platform when the one originally used was
     * an alt CDF or exon-level, so be sure never to use an alt CDF
     * for processing raw data.
     *
     * @param  ee                  ee
     * @param  pathToAptOutputFile file, presumed to be analyzed using the "right" platform (not an alt CDF or
     *                             exon-level)
     * @throws IOException         when IO problems occur.
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void addAffyDataFromAPTOutput( ExpressionExperiment ee, String pathToAptOutputFile ) throws IOException {

        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Can't handle experiments with more than one platform when passing APT output file" );
        }

        ArrayDesign originalPlatform = ads.iterator().next();

        ee = experimentService.thawLite( ee );

        ArrayDesign targetPlatform = this.getAffymetrixTargetPlatform( originalPlatform );
        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();

        Collection<RawExpressionDataVector> vectors = apt
                .processData( ee, pathToAptOutputFile, targetPlatform, originalPlatform, ee.getBioAssays() );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        experimentService.replaceAllRawDataVectors( ee, vectors );

        if ( !targetPlatform.equals( originalPlatform ) ) {

            // Switch ALL bioassays to the target platform.
            int numSwitched = this.switchBioAssaysToTargetPlatform( ee, targetPlatform, null );

            auditTrailService.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class,
                    "Switched " + numSwitched + " bioassays in course of updating vectors using AffyPowerTools (from "
                            + originalPlatform.getShortName() + " to " + targetPlatform.getShortName() + ")" );
        }

        this.audit( ee, "Data vector input from APT output file " + pathToAptOutputFile + " on " + targetPlatform,
                true );

        this.postprocess( ee );
    }

    /**
     * RNA-seq: Replaces data. Starting with the count data, we compute the log2cpm, which is the preferred quantitation
     * type we use internally. Counts and FPKM (if provided) are stored in addition.
     *
     * Rows (genes) that have all zero counts are ignored entirely.
     *
     * @param ee                  ee
     * @param targetArrayDesign   - this should be one of the "Generic" gene-based platforms. The data set will be
     *                            switched to use it.
     * @param countMatrix         Representing 'raw' counts (added after rpkm, if provided).
     * @param rpkmMatrix          Representing per-gene normalized data, optional (RPKM or FPKM)
     * @param allowMissingSamples if true, samples that are missing data will be deleted from the experiment.
     * @param isPairedReads       is paired reads
     * @param readLength          read length
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void addCountData( ExpressionExperiment ee, ArrayDesign targetArrayDesign,
            DoubleMatrix<String, String> countMatrix, DoubleMatrix<String, String> rpkmMatrix, @Nullable Integer readLength,
            @Nullable Boolean isPairedReads, boolean allowMissingSamples ) {

        if ( countMatrix == null )
            throw new IllegalArgumentException( "You must provide count matrix (rpkm is optional)" );

        ee = experimentService.thaw( ee );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            /*
             * FIXME: gracefully handle the case of multiplatform RNA-seq. We can switch the data set to the merged
             * platform
             * so it can be run through replaceData() without issues, while recording the originalPlatform.
             * Then it will be switched to the 'generic' gene-level platform.
             */
        }

        this.dealWithMissingSamples( ee, countMatrix, allowMissingSamples );

        DoubleMatrix<CompositeSequence, BioMaterial> properCountMatrix = this
                .matchElementsToRowNames( targetArrayDesign, countMatrix );
        this.matchBioMaterialsToColNames( ee, countMatrix, properCountMatrix );

        assert !properCountMatrix.getColNames().isEmpty();
        assert !properCountMatrix.getRowNames().isEmpty();

        Collection<QuantitationType> oldQts = ee.getQuantitationTypes();

        //    countEEMatrix = this.removeNoDataRows( countEEMatrix );

        QuantitationType log2cpmQt = this.makelog2cpmQt();
        for ( QuantitationType oldqt : oldQts ) { // use old QT if possible
            if ( oldqt.getName().equals( log2cpmQt.getName() ) ) {
                log2cpmQt = oldqt;
                break;
            }
        }

        DoubleMatrix1D librarySize = MatrixStats.colSums( countMatrix );
        DoubleMatrix<CompositeSequence, BioMaterial> log2cpmMatrix = MatrixStats
                .convertToLog2Cpm( properCountMatrix, librarySize );

        ExpressionDataDoubleMatrix log2cpmEEMatrix = new ExpressionDataDoubleMatrix( ee, log2cpmQt, log2cpmMatrix );

        // important: replaceData takes care of the platform switch if necessary; call first. It also deletes old QTs, so from here we have to remake them.
        this.replaceData( ee, targetArrayDesign, log2cpmEEMatrix );

        QuantitationType countqt = this.makeCountQt();
        for ( QuantitationType oldqt : oldQts ) { // use old QT if possible
            if ( oldqt.getName().equals( countqt.getName() ) ) {
                countqt = oldqt;
                break;
            }
        }
        ExpressionDataDoubleMatrix countEEMatrix = new ExpressionDataDoubleMatrix( ee, countqt, properCountMatrix );

        this.addData( ee, targetArrayDesign, countEEMatrix );

        this.addTotalCountInformation( ee, countEEMatrix, readLength, isPairedReads );

        if ( rpkmMatrix != null ) {

            DoubleMatrix<CompositeSequence, BioMaterial> properRPKMMatrix = this
                    .matchElementsToRowNames( targetArrayDesign, rpkmMatrix );
            this.matchBioMaterialsToColNames( ee, rpkmMatrix, properRPKMMatrix );

            assert !properRPKMMatrix.getColNames().isEmpty();
            assert !properRPKMMatrix.getRowNames().isEmpty();

            QuantitationType rpkmqt = this.makeRPKMQt();
            for ( QuantitationType oldqt : oldQts ) { // use old QT if possible
                if ( oldqt.getName().equals( rpkmqt.getName() ) ) {
                    rpkmqt = oldqt;
                    break;
                }
            }

            ExpressionDataDoubleMatrix rpkmEEMatrix = new ExpressionDataDoubleMatrix( ee, rpkmqt, properRPKMMatrix );

            this.addData( ee, targetArrayDesign, rpkmEEMatrix );
        }

    }

    /**
     * RNA-seq: For back filling log2cpm when only counts are available. This wouldn't be used routinely, because new
     * experiments
     * get log2cpm computed when loaded.
     *
     * @param ee ee
     * @param qt qt
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void log2cpmFromCounts( ExpressionExperiment ee, QuantitationType qt ) {
        ee = experimentService.thawLite( ee );

        /*
         * Get the count data; Make sure it is currently preferred (so we don't do this twice by accident)
         * We need to do this from the Raw data, not the data that has been normalized etc.
         */
        Collection<RawExpressionDataVector> counts = rawExpressionDataVectorService.find( qt );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( counts );

        try {
            /*
             * Get the count data quantitation type and make it non-preferred
             */
            qt.setIsPreferred( false );
            qtService.update( qt );
            ee = experimentService.thawLite( ee ); // so updated QT is attached.

            QuantitationType log2cpmQt = this.makelog2cpmQt();
            DoubleMatrix1D librarySize = MatrixStats.colSums( countMatrix.getMatrix() );
            DoubleMatrix<CompositeSequence, BioMaterial> log2cpmMatrix = MatrixStats
                    .convertToLog2Cpm( countMatrix.getMatrix(), librarySize );

            ExpressionDataDoubleMatrix log2cpmEEMatrix = new ExpressionDataDoubleMatrix( ee, log2cpmQt, log2cpmMatrix );

            assert log2cpmEEMatrix.getQuantitationTypes().iterator().next().getIsPreferred();

            Collection<ArrayDesign> platforms = experimentService.getArrayDesignsUsed( ee );

            if ( platforms.size() > 1 )
                throw new IllegalArgumentException( "Cannot apply to multiplatform data sets" );

            this.addData( ee, platforms.iterator().next(), log2cpmEEMatrix );
        } catch ( Exception e ) {
            DataUpdaterImpl.log.error( e, e );
            // try to recover.
            qt.setIsPreferred( true );
            qtService.update( qt );
        }

    }

    /**
     * Replace the data associated with the experiment (or add it if there is none). These data become the 'preferred'
     * quantitation type. Note that this replaces the "raw" data. Similar to
     * AffyPowerToolsProbesetSummarize.convertDesignElementDataVectors and code in
     * SimpleExpressionDataLoaderService.
     * This method exists in addition to the other replaceData to allow more direct reading of data from files, allowing
     * sample- and element-matching to happen here.
     *
     * @param  ee             ee
     * @param  targetPlatform (this only works for a single-platform data set)
     * @param  qt             qt
     * @param  data           data
     * @return ee
     */
    @Override
    @SuppressWarnings("UnusedReturnValue") // Possible external use
    @Transactional(propagation = Propagation.NEVER)
    public void replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform, QuantitationType qt,
            DoubleMatrix<String, String> data ) {
        targetPlatform = this.arrayDesignService.thaw( targetPlatform );
        ee = this.experimentService.thaw( ee );

        DoubleMatrix<CompositeSequence, BioMaterial> rdata = this.matchElementsToRowNames( targetPlatform, data );
        this.matchBioMaterialsToColNames( ee, data, rdata );
        ExpressionDataDoubleMatrix eematrix = new ExpressionDataDoubleMatrix( ee, qt, rdata );

        this.replaceData( ee, targetPlatform, eematrix );
    }

    /**
     * Affymetrix only: Provide or replace data for an Affymetrix-based experiment, using CEL files. CEL files are
     * downloaded from GEO, apt-probeset-summarize is executed to get the data, and then the experiment is updated. One
     * side-effect is that the data set may end up being on a different platform than originally.
     * A complication is the CEL file type
     * may not match the platform we want the experiment to end up being one. A further complication is when this is
     * re-run on a data set, or if the data set is on a merged platform.
     * Therefore, some of the steps involve inspecting the CEL files to determine the chip type used so we can run
     * apt-probset-summarize correctly; replacing the vectors.
     * Exceptions will be thrown if CEL files can't be located, or the experiments is set up in a way we can't support.
     *
     * @param ee the experiment (already lightly thawed)
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void reprocessAffyDataFromCel( ExpressionExperiment ee ) {
        DataUpdaterImpl.log.info( "------  Begin processing: " + ee + " -----" );
        Collection<ArrayDesign> associatedPlats = experimentService.getArrayDesignsUsed( ee );

        if ( ee.getAccession() == null || ee.getAccession().getAccession() == null ) {
            throw new UnsupportedOperationException(
                    "Can only process from CEL for data sets with an external accession" );
        }

        boolean isOnMergedPlatform = false;
        Map<Long, Boolean> merged = arrayDesignService.isMerged( IdentifiableUtils.getIds( associatedPlats ) );
        for ( ArrayDesign ad : associatedPlats ) {
            isOnMergedPlatform = merged.get( ad.getId() );
            if ( isOnMergedPlatform && associatedPlats.size() > 1 ) {
                // should be rare; normally after merge we have just one platform
                auditTrailService.addUpdateEvent( ee, FailedDataReplacedEvent.class, "Cannot reprocess datasets that include a "
                        + "merged platform and is still on multiple platforms" );

                throw new IllegalArgumentException( "Cannot reprocess datasets that include a "
                        + "merged platform and is still on multiple platforms" );
            }
        }

        boolean vectorsWereMerged = isOnMergedPlatform && this.hasVectorMergeEvent( ee );

        RawDataFetcher f = new RawDataFetcher();

        Collection<File> files = f.fetch( ee.getAccession().getAccession() );

        if ( files == null || files.isEmpty() ) {
            auditTrailService.addUpdateEvent( ee, FailedDataReplacedEvent.class, "Data was apparently not available" );
            throw new RuntimeException( "Data was apparently not available" );
        }
        ee = experimentService.thawLite( ee );

        Map<ArrayDesign, Collection<BioAssay>> targetPlats2BioAssays = this.determinePlatformsFromCELs( ee, files );

        QuantitationType qt = AffyPowerToolsProbesetSummarize.makeAffyQuantitationType();
        qt = qtService.create( qt );
        Collection<RawExpressionDataVector> vectors = new HashSet<>();
        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize( qt );

        Collection<ArrayDesign> workingPlatforms;
        if ( isOnMergedPlatform ) {
            workingPlatforms = targetPlats2BioAssays.keySet();
        } else {
            workingPlatforms = associatedPlats;
        }

        /*
         * Collect these so we can clean up. TODO make this part of the bioassaydimension service, make more efficient
         * and complete
         */
        Collection<BioAssayDimension> allOldBioAssayDims = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            Collection<BioAssayDimension> oldBioAssayDims = bioAssayService.findBioAssayDimensions( ba );
            allOldBioAssayDims.addAll( oldBioAssayDims );
        }

        for ( ArrayDesign originalPlatform : workingPlatforms ) {

            ArrayDesign targetPlatform = this.getAffymetrixTargetPlatform( originalPlatform );
            Collection<BioAssay> bioAssays = targetPlats2BioAssays.get( targetPlatform );

            if ( bioAssays == null || bioAssays.isEmpty() )
                throw new IllegalStateException( "Something went wrong, no bioAssays matched expected target platform=" + targetPlatform
                        + " for original platform = " + originalPlatform );

            DataUpdaterImpl.log.info( "Processing " + bioAssays.size() + " samples for " + targetPlatform + "; "
                    + "BioAssays are currently recorded as platform=" + originalPlatform
                    + ( isOnMergedPlatform ? " (Via merged platform " + associatedPlats.iterator().next() + ")" : "" ) );

            Collection<RawExpressionDataVector> v = apt
                    .processData( ee, targetPlatform, originalPlatform, bioAssays, files );

            if ( v.isEmpty() ) {
                throw new IllegalStateException(
                        "No vectors were returned for " + ee + "; Original platform=" + originalPlatform
                                + "; target platform=" + targetPlatform );
            }

            vectors.addAll( v ); // not yet persistent

            /*
             * Switch the bioassays. We do this even when are using a merged platform, effectively de-merging them. We
             * just need to remerge. This is easier than trying to keep it merged, sinc
             */
            if ( !targetPlatform.equals( originalPlatform ) || isOnMergedPlatform ) {

                /*
                 * This is a little dangerous, since we're not in a transaction and replacing the vectors comes later.
                 * But if something goes wrong, it shouldn't actually
                 * matter that much - so long as we try again and succeed.
                 * If concerned we can make all of the updates to the EE separated into
                 * a single transaction, with some code complexity added.
                 */
                int numSwitched = this.switchBioAssaysToTargetPlatform( ee, targetPlatform, bioAssays );

                DataUpdaterImpl.log
                        .info( "Switched " + numSwitched + " bioassays from " + originalPlatform.getShortName() + " to "
                                + targetPlatform.getShortName() );

                auditTrailService.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class, "Switched " + numSwitched
                        + " bioassays in course of updating vectors using AffyPowerTools (from " + originalPlatform
                        .getShortName()
                        + " to " + targetPlatform.getShortName() + ")" );
            }
        }

        experimentService.replaceAllRawDataVectors( ee, vectors );
        this.audit( ee, "Data vector computation from CEL files using AffyPowerTools", true );

        DataUpdaterImpl.log.info( "------  Done with reanalyzed data; cleaning up and postprocessing -----" );

        /*
         * Postprocessing, all of which is non-serious if it fails.
         */

        /*
         * Clean up any unused bioassaydimensions. We always make new ones here. At this point they should be freed up.
         */
        try {
            sampleCorService.removeForExperiment( ee );
            pcaService.removeForExperiment( ee );
            for ( BioAssayDimension bad : allOldBioAssayDims ) {
                try {
                    bioAssayDimensionService.remove( bad );
                    DataUpdaterImpl.log.info( "Removed bioAssayDimension ID=" + bad.getId() );
                } catch ( Exception e ) {
                    DataUpdaterImpl.log.warn( "Failed to clean up old bioassaydimension with ID=" + bad.getId() + ": " + e
                            .getMessage() );
                }
            }
        } catch ( Exception e ) {
            DataUpdaterImpl.log.warn( "Error during cleanup: " + e.getMessage() );
        }

        boolean needsPost = true;
        if ( isOnMergedPlatform ) {
            try {
                ArrayDesign mergedPlat = associatedPlats.iterator().next();
                DataUpdaterImpl.log.info( "------- Restoring platform/merge status: Switch to " + mergedPlat );
                mergedPlat = arrayDesignService.thaw( mergedPlat );
                experimentPlatformSwitchService.switchExperimentToArrayDesign( ee, mergedPlat );

                if ( vectorsWereMerged ) {
                    DataUpdaterImpl.log.info( "------ Restoring vector merge" );
                    vectorMergingService.mergeVectors( ee );
                    needsPost = false; // since that does it.
                }
            } catch ( Exception e ) {
                needsPost = false; // it would fail anyway.
                DataUpdaterImpl.log
                        .error( "Failed to restore merge status, please attempt to run separately and proceed with any postprocessing" );
            }
        }

        if ( needsPost )
            this.postprocess( ee );
    }

    /**
     * Generic but in practice used for RNA-seq. Add an additional data (with associated quantitation type) to the
     * selected experiment. Will do postprocessing if the data quantitationType is 'preferred', but if there is already
     * a preferred quantitation type, an error will be thrown.
     *
     * @param  ee             ee
     * @param  targetPlatform optional; if null, uses the platform already used (if there is just one; you can't use
     *                        this
     *                        for a multi-platform dataset)
     * @param  data           to slot in
     * @return ee
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void addData( ExpressionExperiment ee, ArrayDesign targetPlatform, ExpressionDataDoubleMatrix data ) {

        if ( data.rows() == 0 ) {
            throw new IllegalArgumentException( "Data had no rows" );
        }

        Collection<QuantitationType> qts = data.getQuantitationTypes();

        if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( "Only support a single quantitation type" );
        }

        if ( qts.isEmpty() ) {
            throw new IllegalArgumentException( "Please supply a quantitation type with the data" );
        }

        QuantitationType qt = qts.iterator().next();

        if ( qt.getIsPreferred() ) {
            for ( QuantitationType existingQt : ee.getQuantitationTypes() ) {
                if ( existingQt.getIsPreferred() ) {
                    // this is okay if there is not actually any data associated with the QT.
                    if ( this.rawAndProcessedExpressionDataVectorService.find( existingQt ).size() > 0 ) {
                        throw new IllegalArgumentException(
                                "You cannot add 'preferred' data to an experiment that already has it. "
                                        + "You should first delete the existing data or make it non-preferred." );
                    }
                }
            }
        }

        Collection<RawExpressionDataVector> vectors = this.makeNewVectors( ee, targetPlatform, data, qt );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "no vectors!" );
        }

        experimentService.addRawDataVectors( ee, qt, vectors );

        this.audit( ee, "Data vectors added for " + targetPlatform + ", " + qt, false );

        experimentService.update( ee );

        if ( qt.getIsPreferred() ) {
            DataUpdaterImpl.log.info( "Postprocessing preferred data" );
            this.postprocess( ee );
            assert ee.getNumberOfDataVectors() != null;
        }
    }

    /**
     * Replace the data associated with the experiment (or add it if there is none). These data become the 'preferred'
     * quantitation type. Note that this replaces the "raw" data.
     * Similar to AffyPowerToolsProbesetSummarize.convertDesignElementDataVectors and code in
     * SimpleExpressionDataLoaderService.
     *
     * @param  ee             the experiment to be modified
     * @param  targetPlatform the platform for the new data (this can only be used for single-platform data sets). The
     *                        experiment will be switched to it if necessary.
     * @param  data           the data to be used
     * @return ee
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data ) {

        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException( "Can only replace data for an experiment that uses one platform; "
                    + "you must switch/merge first and then provide appropriate replacement data." );
        }

        if ( data.rows() == 0 ) {
            throw new IllegalArgumentException( "Data had no rows" );
        }

        ArrayDesign originalArrayDesign = ads.iterator().next();

        Collection<QuantitationType> qts = data.getQuantitationTypes();

        if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( "Only supports a single quantitation type" );
        }

        if ( qts.isEmpty() ) {
            throw new IllegalArgumentException( "Please supply a quantitation type with the data" );
        }

        QuantitationType qt = qts.iterator().next();
        qt.setIsPreferred( true );

        Collection<RawExpressionDataVector> vectors = this.makeNewVectors( ee, targetPlatform, data, qt );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "no vectors!" );
        }

        experimentService.replaceAllRawDataVectors( ee, vectors );

        if ( !targetPlatform.equals( originalArrayDesign ) ) {

            this.switchBioAssaysToTargetPlatform( ee, targetPlatform, null );

            auditTrailService.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class,
                    "Switched in course of updating vectors using data input (from " + originalArrayDesign
                            .getShortName() + " to " + targetPlatform.getShortName() + ")" );
        }

        this.audit( ee, "Data vector replacement for " + targetPlatform, true );
        experimentService.update( ee );
        this.postprocess( ee );

        assert ee.getNumberOfDataVectors() != null;
    }

    /**
     * RNA-seq
     *
     * @param ee            experiment
     * @param countEEMatrix count ee matrix
     * @param readLength    read length
     * @param isPairedReads is paired reads
     */
    private void addTotalCountInformation( ExpressionExperiment ee, ExpressionDataDoubleMatrix countEEMatrix,
            @Nullable Integer readLength, @Nullable Boolean isPairedReads ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            Double[] col = countEEMatrix.getColumn( ba );
            long librarySize = ( long ) Math.floor( DescriptiveWithMissing.sum( new DoubleArrayList( ArrayUtils.toPrimitive( col ) ) ) );

            if ( librarySize <= 0 ) {
                // unlike readLength and isPairedReads, we might want to use this value! Sanity check, anyway.
                throw new IllegalStateException( ba + " had no reads" );
            }
            DataUpdaterImpl.log.info( ba + " total library size=" + librarySize );

            ba.setSequenceReadLength( readLength );
            ba.setSequencePairedReads( isPairedReads );
            ba.setSequenceReadCount( librarySize );

            bioAssayService.update( ba );

        }
    }

    /**
     * Generic
     *
     * @param replace if true, use a DataReplacedEvent; otherwise DataAddedEvent.
     * @param ee      ee
     * @param note    note
     */
    private void audit( ExpressionExperiment ee, String note, boolean replace ) {
        Class<? extends AuditEventType> eventType;

        if ( replace ) {
            eventType = DataReplacedEvent.class;
        } else {
            eventType = DataAddedEvent.class;
        }

        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * RNA-seq
     *
     * @param  ee                  experiment
     * @param  countMatrix         count matrix
     * @param  allowMissingSamples allow missing samples
     * @return experiment
     */
    private void dealWithMissingSamples( ExpressionExperiment ee,
            DoubleMatrix<String, String> countMatrix, boolean allowMissingSamples ) {
        if ( ee.getBioAssays().size() > countMatrix.columns() ) {
            if ( allowMissingSamples ) {

                Map<String, BioMaterial> bmMap = this.makeBioMaterialNameMap( ee );
                List<BioAssay> usedBioAssays = new ArrayList<>();
                for ( String colName : countMatrix.getColNames() ) {
                    BioMaterial bm = bmMap.get( colName );
                    if ( bm == null ) {
                        throw new IllegalStateException( "Could not match a column name to a biomaterial: " + colName );
                    }
                    usedBioAssays.addAll( bm.getBioAssaysUsedIn() );
                }

                assert usedBioAssays.size() == countMatrix.columns();

                Collection<BioAssay> toRemove = new HashSet<>();
                for ( BioAssay ba : ee.getBioAssays() ) {
                    if ( !usedBioAssays.contains( ba ) ) {
                        toRemove.add( ba );
                        DataUpdaterImpl.log.info( "Will remove unused bioassay from experiment: " + ba );
                    }
                }

                if ( !toRemove.isEmpty() ) {
                    ee.getBioAssays().removeAll( toRemove );
                    experimentService.update( ee );

                    if ( ee.getBioAssays().size() != countMatrix.columns() ) {
                        throw new IllegalStateException( "Something went wrong, could not remove unused samples" );
                    }

                    // this should already be done...
                    for ( BioAssay b : toRemove ) {
                        bioAssayService.remove( b );
                    }

                }

            } else {
                throw new IllegalArgumentException(
                        "Too little data provided: The experiment has " + ee.getBioAssays().size()
                                + " samples but the data has " + countMatrix.columns() + " columns." );
            }
        } else if ( ee.getBioAssays().size() < countMatrix.columns() ) {
            throw new IllegalArgumentException(
                    "Extra data provided: The experiment has " + ee.getBioAssays().size() + " samples but the data has "
                            + countMatrix.columns() + " columns." );
        }
    }

    /**
     * Affymetrix
     *
     * @param  ee    (lightly thawed)
     * @param  files CEL files
     * @return Map of the targetplatform to the bioassays that were run on it. Note that this is not necessarily
     *               the
     *               "original platform".
     */
    private Map<ArrayDesign, Collection<BioAssay>> determinePlatformsFromCELs( ExpressionExperiment ee,
            Collection<File> files ) {
        Map<BioAssay, String> bm2chips = AffyChipTypeExtractor.getChipTypes( ee, files );
        /*
         * Reverse the map (probably should just make this part of getChipTypes)
         */
        Map<String, Collection<BioAssay>> chip2bms = new HashMap<>();
        for ( BioAssay ba : bm2chips.keySet() ) {
            String c = bm2chips.get( ba );
            if ( !chip2bms.containsKey( c ) ) {
                chip2bms.put( c, new HashSet<BioAssay>() );
            }
            chip2bms.get( c ).add( ba );
        }
        Map<String, String> chipNames2GPL = AffyPowerToolsProbesetSummarize
                .loadMapFromConfig( AffyPowerToolsProbesetSummarize.AFFY_CHIPNAME_PROPERTIES_FILE_NAME );
        Map<ArrayDesign, Collection<BioAssay>> targetPlatform2BioAssays = new HashMap<>();
        for ( String chipname : chip2bms.keySet() ) {

            /*
             * Original.
             */
            String originalPlatName = chipNames2GPL.get( chipname );
            if ( originalPlatName == null ) {
                throw new IllegalStateException( "Couldn't figure out the GPL for " + chipname );
            }

            ArrayDesign originalPlatform = arrayDesignService.findByShortName( originalPlatName );
            ArrayDesign targetPlatform = this.getAffymetrixTargetPlatform( originalPlatform );

            log.info( targetPlatform + " associated with " + chip2bms.get( chipname ).size() + " samples based on CEL files ('" + chipname + "')" );
            targetPlatform2BioAssays.put( targetPlatform, chip2bms.get( chipname ) );
        }
        return targetPlatform2BioAssays;
    }

    /**
     * Affymetrix: Determine the target array design (the one we'll switch to). We use official CDFs and gene-level
     * versions of exon
     * arrays - no custom CDFs!
     *
     * @param  ad array design we are starting with
     * @return platform we should actually use. It can be the same as the input (thawed)
     */
    private ArrayDesign getAffymetrixTargetPlatform( ArrayDesign ad ) {

        /*
         * See also GeoPlatform.useDataFromGeo
         */
        String targetPlatformAcc = GeoPlatform.alternativeToProperAffyPlatform( ad.getShortName() );
        if ( targetPlatformAcc == null ) {
            throw new IllegalArgumentException( "There was no target platform available for " + ad.getShortName() );
        }

        ArrayDesign targetPlatform = arrayDesignService.findByShortName( targetPlatformAcc );

        if ( targetPlatform != null ) {

            if ( arrayDesignService.getCompositeSequenceCount( targetPlatform ) == 0 ) {
                DataUpdaterImpl.log.warn( "The target platform " + targetPlatformAcc
                        + " is incomplete in the system, getting from GEO ... " );
                /*
                 * Ok, we have to 'reload it' and add the compositeSequences. RARE
                 */
                geoService.addElements( targetPlatform );
            }
        } else {
            // RARE
            DataUpdaterImpl.log.warn( "The target platform " + targetPlatformAcc
                    + " could not be found in the system. Loading it from GEO ..." );

            Collection<?> r = geoService.fetchAndLoad( targetPlatformAcc, true, false, false );

            if ( r.isEmpty() )
                throw new IllegalStateException( "Loading target platform failed." );

            targetPlatform = ( ArrayDesign ) r.iterator().next();

        }

        // we need to thaw it at some point
        return arrayDesignService.thaw( targetPlatform );
    }

    /**
     * @param  a auditable
     * @return true if auditable has vector merge event
     */
    private boolean hasVectorMergeEvent( Auditable a ) {

        for ( AuditEvent event : this.auditEventService.getEvents( a ) ) {
            if ( event == null )
                continue; // just in case; should not happen
            if ( event.getEventType() instanceof ExpressionExperimentVectorMergeEvent ) {
                return true;
            }
        }
        return false;

    }

    /**
     * Generic (non-Affymetrix)
     *
     * @param  ee ee
     * @return map of strings to biomaterials, where the keys are likely column names used in the input files.
     */
    private Map<String, BioMaterial> makeBioMaterialNameMap( ExpressionExperiment ee ) {
        Map<String, BioMaterial> bmMap = new HashMap<>();

        Collection<BioAssay> bioAssays = ee.getBioAssays();
        for ( BioAssay bioAssay : bioAssays ) {

            BioMaterial bm = bioAssay.getSampleUsed();
            if ( bmMap.containsKey( bm.getName() ) ) {
                // this might not actually be an error (more than one bioassay per biomaterial is possible) - but just in case...
                throw new IllegalStateException(
                        "More than one biomaterial with the same name: '" + bm.getName() + "'\n" + bmMap.get( bm.getName() ) + "\n" + bm );
            }

            bmMap.put( bm.getName(), bm );
            bmMap.put( bioAssay.getName(), bm ); // this is okay, if we have only one platform, which should be the case.

            if ( bioAssay.getAccession() != null ) {
                // e.g. GSM123455
                String accession = bioAssay.getAccession().getAccession();
                if ( bmMap.containsKey( accession ) ) {
                    throw new IllegalStateException( "Two bioassays with the same accession: " + accession );
                }
                bmMap.put( accession, bm );
            }

            // I think it will always be null, if it is from GEO anyway.
            if ( bm.getExternalAccession() != null ) {
                String accession = bm.getExternalAccession().getAccession();
                if ( bmMap.containsKey( accession ) ) {
                    throw new IllegalStateException( "Two biomaterials with the same accession: " + accession );
                }
                bmMap.put( accession, bm );
            }

        }
        return bmMap;
    }

    /**
     * RNA-seq
     *
     * @return QT
     */
    private QuantitationType makeCountQt() {
        QuantitationType countqt = this.makeQt( false );
        countqt.setName( "Counts" );
        countqt.setType( StandardQuantitationType.COUNT );
        countqt.setScale( ScaleType.COUNT );
        countqt.setDescription( "Read counts for gene model" );
        countqt.setIsBackgroundSubtracted( false );
        countqt.setIsNormalized( false );
        countqt.setIsRecomputedFromRawData( true ); // assume this is true...
        return countqt;
    }

    /**
     * RNA-seq
     *
     * @return QT
     */
    private QuantitationType makelog2cpmQt() {
        QuantitationType qt = this.makeQt( true );
        qt.setName( "log2cpm" );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setDescription( "log-2 transformed read counts per million" );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsRecomputedFromRawData( true ); // assume this is true...
        return qt;
    }

    /**
     * Non-Affymetrix
     *
     * @param  ee             experiment
     * @param  targetPlatform target platform
     * @param  data           data
     * @param  qt             QT
     * @return raw vectors
     */
    private Collection<RawExpressionDataVector> makeNewVectors( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data, QuantitationType qt ) {
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        BioAssayDimension bioAssayDimension = data.getBestBioAssayDimension();

        assert bioAssayDimension != null;
        assert !bioAssayDimension.getBioAssays().isEmpty();

        bioAssayDimension = assayDimensionService.findOrCreate( bioAssayDimension );

        assert !bioAssayDimension.getBioAssays().isEmpty();

        for ( int i = 0; i < data.rows(); i++ ) {
            CompositeSequence cs = data.getRowElement( i ).getDesignElement();
            if ( cs == null ) {
                continue;
            }
            if ( !cs.getArrayDesign().equals( targetPlatform ) ) {
                throw new IllegalArgumentException(
                        "Input data must use the target platform (was: " + cs.getArrayDesign() + ", expected: "
                                + targetPlatform );
            }
            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setDesignElement( cs );
            vector.setQuantitationType( qt );
            vector.setExpressionExperiment( ee );
            vector.setBioAssayDimension( bioAssayDimension );
            vector.setDataAsDoubles( data.getRawRow( i ) );
            vectors.add( vector );
        }
        return vectors;
    }

    /**
     * Generic
     *
     * @param  preferred preffered
     * @return QT
     */
    private QuantitationType makeQt( boolean preferred ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsRatio( false );
        qt.setIsBackgroundSubtracted( true );
        qt.setIsNormalized( true );
        qt.setIsMaskedPreferred( true );
        qt.setIsPreferred( preferred );
        qt.setIsBatchCorrected( false );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return qt;
    }

    /**
     * RNA-seq
     *
     * @return QT
     */
    private QuantitationType makeRPKMQt() {
        QuantitationType rpkmqt = this.makeQt( false );
        rpkmqt.setIsRatio( false );
        rpkmqt.setName( "RPKM" );
        rpkmqt.setIsPreferred( false );
        rpkmqt.setDescription( "Reads (or fragments) per kb of gene model per million reads" );
        rpkmqt.setIsBackgroundSubtracted( false );
        rpkmqt.setIsNormalized( true );
        rpkmqt.setIsRecomputedFromRawData( true ); // assume this is true...
        return rpkmqt;
    }

    /**
     * Generic
     *
     * @param ee          experiment
     * @param rawMatrix   raw matrix
     * @param finalMatrix final matrix
     */
    private void matchBioMaterialsToColNames( ExpressionExperiment ee, DoubleMatrix<String, String> rawMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> finalMatrix ) {
        // match column names to the samples. can have any order so be careful.
        List<String> colNames = rawMatrix.getColNames();
        Map<String, BioMaterial> bmMap = this.makeBioMaterialNameMap( ee );

        List<BioMaterial> newColNames = new ArrayList<>();
        for ( String colName : colNames ) {
            BioMaterial bm = bmMap.get( colName );
            if ( bm == null ) {
                throw new IllegalStateException(
                        "Could not match a column name to a biomaterial: " + colName + "; Available keys were:\n"
                                + StringUtils.join( bmMap.keySet(), "\n" ) );
            }
            newColNames.add( bm );
        }

        finalMatrix.setColumnNames( newColNames );
    }

    /**
     * Generic
     *
     * @param  rawMatrix         matrix
     * @param  targetArrayDesign ad
     * @return matrix with row names fixed up. ColumnNames still need to be done.
     */
    private DoubleMatrix<CompositeSequence, BioMaterial> matchElementsToRowNames( ArrayDesign targetArrayDesign,
            DoubleMatrix<String, String> rawMatrix ) {

        Map<String, CompositeSequence> pnmap = new HashMap<>();

        for ( CompositeSequence cs : targetArrayDesign.getCompositeSequences() ) {
            pnmap.put( cs.getName(), cs );
        }
        int failedMatch = 0;
        int timesWarned = 0;
        List<CompositeSequence> newRowNames = new ArrayList<>();
        List<String> usableRowNames = new ArrayList<>();
        assert !rawMatrix.getRowNames().isEmpty();
        for ( String rowName : rawMatrix.getRowNames() ) {
            CompositeSequence cs = pnmap.get( rowName );
            if ( cs == null ) {
                /*
                 * This might be okay, but not too much
                 */
                failedMatch++;
                if ( timesWarned < 20 ) {
                    DataUpdaterImpl.log.warn( "No platform match to element named: " + rowName );
                }
                if ( timesWarned == 20 ) {
                    DataUpdaterImpl.log.warn( "Further warnings suppressed" );
                }
                timesWarned++;
                continue;
            }
            usableRowNames.add( rowName );
            newRowNames.add( cs );
        }

        if ( usableRowNames.isEmpty() || newRowNames.isEmpty() ) {
            throw new IllegalArgumentException( "None of the rows matched the given platform elements" );
        }
        DoubleMatrix<CompositeSequence, BioMaterial> finalMatrix;
        if ( failedMatch > 0 ) {
            DataUpdaterImpl.log.warn( failedMatch + "/" + rawMatrix.rows()
                    + " elements could not be matched to the platform. Lines that did not match will be ignored." );
            DoubleMatrix<String, String> useableData = rawMatrix.subsetRows( usableRowNames );
            finalMatrix = new DenseDoubleMatrix<>( useableData.getRawMatrix() );

        } else {
            finalMatrix = new DenseDoubleMatrix<>( rawMatrix.getRawMatrix() );

        }
        finalMatrix.setRowNames( newRowNames );
        if ( finalMatrix.getRowNames().isEmpty() ) {
            throw new IllegalStateException( "Failed to get row names" );
        }

        return finalMatrix; // not actually final.
    }

    /**
     * Generic
     *
     * @param  ee experiment
     * @return experiment
     */
    private void postprocess( ExpressionExperiment ee ) {
        // several transactions
        try {
            preprocessorService.process( ee );
        } catch ( PreprocessingException e ) {
            DataUpdaterImpl.log.error( "Error during postprocessing", e );
        }
    }

    //    /**
    //     * For a RNA-seq count matrix, remove rows that have only zeros.
    //     *
    //     * @param  countEEMatrix
    //     * @return               filtered matrix
    //     */
    //    private ExpressionDataDoubleMatrix removeNoDataRows( ExpressionDataDoubleMatrix countEEMatrix ) {
    //        RowLevelFilter filter = new RowLevelFilter();
    //        filter.setMethod( Method.MAX );
    //        filter.setLowCut( 0.0 ); // rows whose maximum value is greater than zero will be kept.
    //        return filter.filter( countEEMatrix );
    //    }

    /**
     * Affymetrix: Switches bioassays on the original platform to the target platform (if they are the same, nothing
     * will be done)
     *
     * @param  ee             presumed thawed
     * @param  targetPlatform target platform
     * @param  toBeSwitched   if necessary, specific which bioassays need to be switched (case: merged and re-run); or
     *                        null
     * @return how many were switched
     */
    private int switchBioAssaysToTargetPlatform( ExpressionExperiment ee, ArrayDesign targetPlatform, @Nullable Collection<BioAssay> toBeSwitched ) {
        int i = 0;
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( toBeSwitched != null && !toBeSwitched.contains( ba ) )
                continue;
            // don't clobber the original value if this is getting switched "again"
            if ( ba.getOriginalPlatform() == null ) {
                ba.setOriginalPlatform( ba.getArrayDesignUsed() );
            }
            ba.setArrayDesignUsed( targetPlatform );
            bioAssayService.update( ba );
            i++;
        }
        return i;
    }
}
