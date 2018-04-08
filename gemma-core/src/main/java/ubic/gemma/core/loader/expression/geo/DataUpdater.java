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
package ubic.gemma.core.loader.expression.geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.expression.AnalysisUtilService;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.AffyPowerToolsProbesetSummarize;
import ubic.gemma.core.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Update the data associated with an experiment. Primary designed for filling in data that we can't or don't want to
 * get from GEO. For loading experiments from flat files, see SimpleExpressionDataLoaderService
 *
 * @author paul
 */
@Component
public class DataUpdater {

    private static final Log log = LogFactory.getLog( DataUpdater.class );

    @Autowired
    private AnalysisUtilService analysisUtilService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioAssayDimensionService assayDimensionService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Autowired
    private QuantitationTypeService qtService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    /**
     * For 3' arrays. This only works for single-platform experiments.
     *
     * @param ee ee
     * @param pathToAptOutputFile file
     */
    public void addAffyData( ExpressionExperiment ee, String pathToAptOutputFile ) throws IOException {

        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Can't handle experiments with more than one platform when passing APT output file." );
        }

        ArrayDesign ad = ads.iterator().next();

        ad = arrayDesignService.thaw( ad );
        ee = experimentService.thawLite( ee );

        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();

        Collection<RawExpressionDataVector> vectors = apt.processData( ee, pathToAptOutputFile, ad );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        experimentService.replaceRawVectors( ee, vectors );

        this.audit( ee, "Data vector input from APT output file " + pathToAptOutputFile + " on " + ad, true );

        this.postprocess( ee );

    }

    /**
     * For platforms that don't have a CDF and/or for which have exon vs gene level data. Applies to Affymetrix exon and
     * newer gene platforms.
     * 
     * @param ee
     */
    public void addAffyExonArrayData( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException( "Can't handle experiments with more than one platform" );
        }
        this.addAffyExonOrGeneArrayData( ee, ads.iterator().next() );
    }

    /**
     * Replaces any existing "preferred" data. Must be a single-platform study
     *
     * @param ee ee
     * @param ad ad
     */
    private void addAffyExonOrGeneArrayData( ExpressionExperiment ee, ArrayDesign ad ) {

        RawDataFetcher f = new RawDataFetcher();
        Collection<LocalFile> files = f.fetch( ee.getAccession().getAccession() );

        if ( files.isEmpty() ) {
            throw new RuntimeException( "Data was apparently not available" );
        }
        ee = experimentService.thawLite( ee );

        ArrayDesign targetPlatform = this.prepareTargetPlatformForExonArrays( ad );

        assert !targetPlatform.getCompositeSequences().isEmpty();

        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();

        Collection<RawExpressionDataVector> vectors = apt.processExonOrGeneArrayData( ee, targetPlatform, files );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        ee = experimentService.replaceRawVectors( ee, vectors );

        if ( !targetPlatform.equals( ad ) ) {
            AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            auditTrailService.addUpdateEvent( ee, eventType,
                    "Switched in course of updating vectors using AffyPowerTools (from " + ad.getShortName() + " to "
                            + targetPlatform.getShortName() + ")" );
        }

        this.audit( ee, "Data vector computation from CEL files using AffyPowerTools for " + targetPlatform, true );

        this.postprocess( ee );
    }

    /**
     * For platforms that don't have a CDF and/or for which have exon vs gene level data. Applies to Affymetrix exon and
     * newer gene platforms. Use when we want to avoid downloading the CEL files etc. For example if GEO doesn't have
     * them and we ran apt-probeset-summarize ourselves. Must be single-platform.
     *
     * @param ee ee
     * @param pathToAptOutputFile file
     */
    public void addAffyExonArrayData( ExpressionExperiment ee, String pathToAptOutputFile ) throws IOException {

        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Can't handle experiments with more than one platform when passing APT output file" );
        }

        ArrayDesign ad = ads.iterator().next();

        //  ad = arrayDesignService.thaw( ad );
        ee = experimentService.thawLite( ee );

        // Taxon primaryTaxon = ad.getPrimaryTaxon();

        ArrayDesign targetPlatform = this.prepareTargetPlatformForExonArrays( ad );
        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();

        Collection<RawExpressionDataVector> vectors = apt.processData( ee, pathToAptOutputFile, targetPlatform );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        experimentService.replaceRawVectors( ee, vectors );

        if ( !targetPlatform.equals( ad ) ) {
            AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            auditTrailService.addUpdateEvent( ee, eventType,
                    "Switched in course of updating vectors using AffyPowerTools (from " + ad.getShortName() + " to "
                            + targetPlatform.getShortName() + ")" );
        }

        this.audit( ee, "Data vector input from APT output file " + pathToAptOutputFile + " on " + targetPlatform,
                true );

        this.postprocess( ee );

    }

    /**
     * Replaces data. Starting with the count data, we compute the log2cpm, which is the preferred quantitation type we
     * use internally. Counts and FPKM (if provided) are stored in addition.
     *
     * @param ee ee
     * @param targetArrayDesign - this should be one of the "Generic" gene-based platforms. The data set will be
     *        switched to use it.
     * @param countMatrix Representing 'raw' counts (added after rpkm, if provided).
     * @param rpkmMatrix Representing per-gene normalized data, optional (RPKM or FPKM)
     * @param allowMissingSamples if true, samples that are missing data will be deleted from the experiment.
     */
    public void addCountData( ExpressionExperiment ee, ArrayDesign targetArrayDesign,
            DoubleMatrix<String, String> countMatrix, DoubleMatrix<String, String> rpkmMatrix, Integer readLength,
            Boolean isPairedReads, boolean allowMissingSamples ) {

        if ( countMatrix == null )
            throw new IllegalArgumentException( "You must provide count matrix (rpkm is optional)" );

        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        ee = experimentService.thawLite( ee );

        ee = this.dealWithMissingSamples( ee, countMatrix, allowMissingSamples );

        DoubleMatrix<CompositeSequence, BioMaterial> properCountMatrix = this
                .matchElementsToRowNames( targetArrayDesign, countMatrix );
        this.matchBioMaterialsToColNames( ee, countMatrix, properCountMatrix );

        assert !properCountMatrix.getColNames().isEmpty();
        assert !properCountMatrix.getRowNames().isEmpty();

        QuantitationType countqt = this.makeCountQt();
        ExpressionDataDoubleMatrix countEEMatrix = new ExpressionDataDoubleMatrix( ee, countqt, properCountMatrix );

        QuantitationType log2cpmQt = this.makelog2cpmQt();
        DoubleMatrix1D librarySize = MatrixStats.colSums( countMatrix );
        DoubleMatrix<CompositeSequence, BioMaterial> log2cpmMatrix = MatrixStats
                .convertToLog2Cpm( properCountMatrix, librarySize );

        ExpressionDataDoubleMatrix log2cpmEEMatrix = new ExpressionDataDoubleMatrix( ee, log2cpmQt, log2cpmMatrix );

        ee = this.replaceData( ee, targetArrayDesign, log2cpmEEMatrix );
        ee = this.addData( ee, targetArrayDesign, countEEMatrix );

        this.addTotalCountInformation( ee, countEEMatrix, readLength, isPairedReads );

        if ( rpkmMatrix != null ) {

            DoubleMatrix<CompositeSequence, BioMaterial> properRPKMMatrix = this
                    .matchElementsToRowNames( targetArrayDesign, rpkmMatrix );
            this.matchBioMaterialsToColNames( ee, rpkmMatrix, properRPKMMatrix );

            assert !properRPKMMatrix.getColNames().isEmpty();
            assert !properRPKMMatrix.getRowNames().isEmpty();

            QuantitationType rpkmqt = this.makeRPKMQt();
            ExpressionDataDoubleMatrix rpkmEEMatrix = new ExpressionDataDoubleMatrix( ee, rpkmqt, properRPKMMatrix );

            ee = this.addData( ee, targetArrayDesign, rpkmEEMatrix );
        }

        assert !processedExpressionDataVectorService.getProcessedDataVectors( ee ).isEmpty();
    }

    /**
     * Add an additional data (with associated quantitation type) to the selected experiment. Will do postprocessing if
     * the data quantitationType is 'preferred', but if there is already a preferred quantitation type, an error will be
     * thrown.
     *
     * @param ee ee
     * @param targetPlatform optional; if null, uses the platform already used (if there is just one; you can't use this
     *        for a multi-platform dataset)
     * @param data to slot in
     * @return ee
     */
    public ExpressionExperiment addData( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data ) {

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
                    throw new IllegalArgumentException(
                            "You cannot add 'preferred' data to an experiment that already has it. You should first make the existing data non-preferred." );
                }
            }
        }

        Collection<RawExpressionDataVector> vectors = this.makeNewVectors( ee, targetPlatform, data, qt );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "no vectors!" );
        }

        ee = experimentService.addRawVectors( ee, vectors );

        this.audit( ee, "Data vectors added for " + targetPlatform + ", " + qt, false );

        // debug code.
        for ( BioAssay ba : ee.getBioAssays() ) {
            assert ba.getArrayDesignUsed().equals( targetPlatform );
        }

        experimentService.update( ee );

        if ( qt.getIsPreferred() ) {
            DataUpdater.log.info( "Postprocessing preferred data" );
            ee = this.postprocess( ee );
            assert ee.getNumberOfDataVectors() != null;
        }

        return ee;
    }

    @SuppressWarnings("UnusedReturnValue") // Possible external use
    public int deleteData( ExpressionExperiment ee, QuantitationType qt ) {
        return this.experimentService.removeRawVectors( ee, qt );
    }

    /**
     * For back filling log2cpm when only counts are available. This wouldn't be used routinely, because new experiments
     * get log2cpm computed when loaded.
     *
     * @param ee ee
     * @param qt qt
     */
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
            DataUpdater.log.error( e, e );
            // try to recover.
            qt.setIsPreferred( true );
            qtService.update( qt );
        }

    }

    /**
     * Replace the data associated with the experiment (or add it if there is none). These data become the 'preferred'
     * quantitation type. Note that this replaces the "raw" data.
     * Similar to AffyPowerToolsProbesetSummarize.convertDesignElementDataVectors and code in
     * SimpleExpressionDataLoaderService.
     *
     * @param ee the experiment to be modified
     * @param targetPlatform the platform for the new data (this can only be used for single-platform data sets)
     * @param data the data to be used
     * @return ee
     */
    public ExpressionExperiment replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform,
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

        /*
         * remove all analyses, etc.
         */
        analysisUtilService.deleteOldAnalyses( ee );

        ee = experimentService.replaceRawVectors( ee, vectors );

        // audit if we switched platforms.
        if ( !targetPlatform.equals( originalArrayDesign ) ) {
            AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            auditTrailService.addUpdateEvent( ee, eventType,
                    "Switched in course of updating vectors using data input (from " + originalArrayDesign
                            .getShortName() + " to " + targetPlatform.getShortName() + ")" );
        }

        this.audit( ee, "Data vector replacement for " + targetPlatform, true );
        experimentService.update( ee );

        ee = this.postprocess( ee );

        assert ee.getNumberOfDataVectors() != null;

        // debug code.
        for ( BioAssay ba : ee.getBioAssays() ) {
            assert ba.getArrayDesignUsed().equals( targetPlatform );
        }

        return ee;
    }

    /**
     * Replace the data associated with the experiment (or add it if there is none). These data become the 'preferred'
     * quantitation type. Note that this replaces the "raw" data. Similar to
     * AffyPowerToolsProbesetSummarize.convertDesignElementDataVectors and code in
     * SimpleExpressionDataLoaderService.
     * This method exists in addition to the other replaceData to allow more direct reading of data from files, allowing
     * sample- and element-matching to happen here.
     *
     * @param ee ee
     * @param targetPlatform (this only works for a single-platform data set)
     * @param qt qt
     * @param data data
     * @return ee
     */
    @SuppressWarnings("UnusedReturnValue") // Possible external use
    public ExpressionExperiment replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform, QuantitationType qt,
            DoubleMatrix<String, String> data ) {
        targetPlatform = this.arrayDesignService.thaw( targetPlatform );
        ee = this.experimentService.thawLite( ee );

        DoubleMatrix<CompositeSequence, BioMaterial> rdata = this.matchElementsToRowNames( targetPlatform, data );
        this.matchBioMaterialsToColNames( ee, data, rdata );
        ExpressionDataDoubleMatrix eematrix = new ExpressionDataDoubleMatrix( ee, qt, rdata );

        return this.replaceData( ee, targetPlatform, eematrix );
    }

    /**
     * @param ee ee
     * @return This replaces the existing raw data with the CEL file data. CEL file(s) must be found by configuration
     */
    @SuppressWarnings("UnusedReturnValue") // Possible external use
    public ExpressionExperiment reprocessAffyThreePrimeArrayData( ExpressionExperiment ee ) {

        Collection<ArrayDesign> arrayDesignsUsed = this.experimentService.getArrayDesignsUsed( ee );

        if ( arrayDesignsUsed.size() > 1 ) {
            throw new UnsupportedOperationException( "Merge/match platforms/samples before using this." );
        }

        ee = experimentService.thawLite( ee );
        RawDataFetcher f = new RawDataFetcher();
        Collection<LocalFile> files = f.fetch( ee.getAccession().getAccession() );

        if ( files.isEmpty() ) {
            throw new RuntimeException( "Data was apparently not available" );
        }
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        // Use the same QT for each one 
        QuantitationType qt = AffyPowerToolsProbesetSummarize.makeAffyQuantitationType();
        qt = quantitationTypeService.create( qt );

        for ( ArrayDesign ad : arrayDesignsUsed ) { // only allow 1
            DataUpdater.log.info( "Processing data for " + ad );

            ad = arrayDesignService.thaw( ad );

            AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize( qt );

            vectors.addAll( apt.processThreeprimeArrayData( ee, ad, files ) );

        }
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        ee = experimentService.replaceRawVectors( ee, vectors );
        this.audit( ee, "Data vector computation from CEL files using AffyPowerTools for " + StringUtils
                .join( arrayDesignsUsed, "; " ), true );

        if ( arrayDesignsUsed.size() == 1 ) {
            this.postprocess( ee );
        } else {
            DataUpdater.log.warn( "Skipping postprocessing for mult-platform experiment" );
        }

        return ee;
    }

    private void addTotalCountInformation( ExpressionExperiment ee, ExpressionDataDoubleMatrix countEEMatrix,
            Integer readLength, Boolean isPairedReads ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            Double[] col = countEEMatrix.getColumn( ba );
            double librarySize = DescriptiveWithMissing.sum( new DoubleArrayList( ArrayUtils.toPrimitive( col ) ) );

            DataUpdater.log.info( ba + " total library size=" + librarySize );

            ba.setSequenceReadLength( readLength );
            ba.setSequencePairedReads( isPairedReads );
            ba.setSequenceReadCount( ( int ) Math.floor( librarySize ) );

            bioAssayService.update( ba );

        }
    }

    /**
     * @param replace if true, use a DataReplacedEvent; otherwise DataAddedEvent.
     * @param ee ee
     * @param note note
     */
    private void audit( ExpressionExperiment ee, String note, boolean replace ) {
        AuditEventType eventType;

        if ( replace ) {
            eventType = DataReplacedEvent.Factory.newInstance();
        } else {
            eventType = DataAddedEvent.Factory.newInstance();
        }

        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    private ExpressionExperiment dealWithMissingSamples( ExpressionExperiment ee,
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
                        DataUpdater.log.info( "Will remove unused bioassay from experiment: " + ba );
                    }
                }

                if ( !toRemove.isEmpty() ) {
                    ee.getBioAssays().removeAll( toRemove );
                    experimentService.update( ee );
                    ee = experimentService.load( ee.getId() );
                    ee = experimentService.thawLite( ee );

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
        return ee;
    }

    /**
     * @param ee ee
     * @return map of strings to biomaterials, where the keys are likely column names used in the input files.
     */
    private Map<String, BioMaterial> makeBioMaterialNameMap( ExpressionExperiment ee ) {
        Map<String, BioMaterial> bmMap = new HashMap<>();

        Collection<BioAssay> bioAssays = ee.getBioAssays();
        for ( BioAssay bioAssay : bioAssays ) {

            BioMaterial bm = bioAssay.getSampleUsed();
            if ( bmMap.containsKey( bm.getName() ) ) {
                // this might not actually be an error - but just in case...
                throw new IllegalStateException( "Two biomaterials from the same experiment with the same name " );
            }

            bmMap.put( bm.getName(), bm );

            if ( bioAssay.getAccession() != null ) {
                // e.g. GSM123455
                String accession = bioAssay.getAccession().getAccession();
                if ( bmMap.containsKey( accession ) ) {
                    throw new IllegalStateException( "Two bioassays with the same accession" );
                }
                bmMap.put( accession, bm );
            }

            // I think it will always be null, if it is from GEO anyway.
            if ( bm.getExternalAccession() != null ) {
                if ( bmMap.containsKey( bm.getExternalAccession().getAccession() ) ) {
                    throw new IllegalStateException( "Two biomaterials with the same accession" );
                }
                bmMap.put( bm.getExternalAccession().getAccession(), bm );
            }

        }
        return bmMap;
    }

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

    private Collection<RawExpressionDataVector> makeNewVectors( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data, QuantitationType qt ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        BioAssayDimension bioAssayDimension = data.getBestBioAssayDimension();

        assert bioAssayDimension != null;
        assert !bioAssayDimension.getBioAssays().isEmpty();

        bioAssayDimension = assayDimensionService.findOrCreate( bioAssayDimension );

        assert !bioAssayDimension.getBioAssays().isEmpty();

        for ( int i = 0; i < data.rows(); i++ ) {
            byte[] bdata = bArrayConverter.doubleArrayToBytes( data.getRow( i ) );

            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setData( bdata );

            CompositeSequence cs = data.getRowElement( i ).getDesignElement();

            if ( cs == null ) {
                continue;
            }

            if ( !cs.getArrayDesign().equals( targetPlatform ) ) {
                throw new IllegalArgumentException(
                        "Input data must use the target platform (was: " + cs.getArrayDesign() + ", expected: "
                                + targetPlatform );
            }

            vector.setDesignElement( cs );
            vector.setQuantitationType( qt );
            vector.setExpressionExperiment( ee );
            vector.setBioAssayDimension( bioAssayDimension );
            vectors.add( vector );

        }
        return vectors;
    }

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
     * @param rawMatrix matrix
     * @param targetArrayDesign ad
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
                    DataUpdater.log.warn( "No platform match to element named: " + rowName );
                }
                if ( timesWarned == 20 ) {
                    DataUpdater.log.warn( "Further warnings suppressed" );
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
            DataUpdater.log.warn( failedMatch + "/" + rawMatrix.rows()
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

    private ExpressionExperiment postprocess( ExpressionExperiment ee ) {
        // several transactions
        try {
            ee = preprocessorService.process( ee );
            assert ee.getNumberOfDataVectors() != null;
        } catch ( PreprocessingException e ) {
            DataUpdater.log.error( "Error during postprocessing", e );
        }
        return ee;
    }

    /**
     * determine the target array design. We use filtered versions of these platforms from GEO.
     *
     * @param ad array design we are starting with
     * @return platform we should actually use. It can be the same as the input.
     */
    private ArrayDesign prepareTargetPlatformForExonArrays( ArrayDesign ad ) {

        /*
         * See also GeoPlatform.useDataFromGeo
         */
        String targetPlatformAcc = GeoPlatform.exonArrayToGeneLevelArray( ad.getShortName() );
        if ( targetPlatformAcc == null ) {
            throw new IllegalArgumentException( "There was no target platform available for " + ad.getShortName() );
        }

        ArrayDesign targetPlatform = arrayDesignService.findByShortName( targetPlatformAcc );

        if ( targetPlatform != null ) {
            targetPlatform = arrayDesignService.thaw( targetPlatform );

            if ( targetPlatform.getCompositeSequences().isEmpty() ) {
                /*
                 * Ok, we have to 'reload it' and add the compositeSequences.
                 */
                geoService.addElements( targetPlatform );
            }
        } else {
            DataUpdater.log.warn( "The target platform " + targetPlatformAcc
                    + " could not be found in the system. Loading it ..." );

            Collection<?> r = geoService.fetchAndLoad( targetPlatformAcc, true, false, false );

            if ( r.isEmpty() )
                throw new IllegalStateException( "Loading target platform failed." );

            targetPlatform = ( ArrayDesign ) r.iterator().next();

        }

        return targetPlatform;
    }

}
