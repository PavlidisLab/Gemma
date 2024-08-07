/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.expression.geo.*;
import ubic.gemma.core.loader.expression.geo.model.*;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentUpdateFromGEOEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentPrePersistService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.persister.ArrayDesignsForExperimentCache;

import java.io.File;
import java.util.*;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 *
 * @author pavlidis
 */
@Component
public class GeoServiceImpl extends AbstractGeoService {

    private static final String GEO_DB_NAME = "GEO";

    @Value("${geo.minimumSamplesToLoad}")
    private int MINIMUM_SAMPLE_COUNT_TO_LOAD;

    private final ArrayDesignReportService arrayDesignReportService;
    private final BioAssayService bioAssayService;
    private final ExpressionExperimentReportService expressionExperimentReportService;
    private final ExpressionExperimentService expressionExperimentService;
    private final ExpressionExperimentPrePersistService expressionExperimentPrePersistService;
    private final TaxonService taxonService;

    private final CharacteristicService characteristicService;

    private final BioMaterialService bioMaterialService;
    private BibliographicReferenceService bibliographicReferenceService;

    private AuditTrailService auditTrailService;


    @Autowired
    public GeoServiceImpl( ArrayDesignReportService arrayDesignReportService, BioAssayService bioAssayService,
            ExpressionExperimentReportService expressionExperimentReportService,
            ExpressionExperimentService expressionExperimentService,
            ExpressionExperimentPrePersistService expressionExperimentPrePersistService,
            TaxonService taxonService,
            CharacteristicService characteristicService,
            BioMaterialService bioMaterialSerivce, BibliographicReferenceService bibliographicReferenceService, AuditTrailService auditTrailService ) {
        this.arrayDesignReportService = arrayDesignReportService;
        this.bioAssayService = bioAssayService;
        this.expressionExperimentReportService = expressionExperimentReportService;
        this.expressionExperimentService = expressionExperimentService;
        this.expressionExperimentPrePersistService = expressionExperimentPrePersistService;
        this.characteristicService = characteristicService;
        this.bioMaterialService = bioMaterialSerivce;
        this.bibliographicReferenceService = bibliographicReferenceService;
        this.taxonService = taxonService;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public ArrayDesign addElements( ArrayDesign targetPlatform ) {

        if ( !targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Only call this if you are filling in an empty platform" );
        }

        String geoAccession = targetPlatform.getExternalReferences().iterator().next().getAccession();
        Collection<? extends GeoData> platforms = geoDomainObjectGenerator.generate( geoAccession );
        if ( platforms.size() == 0 ) {
            throw new IllegalStateException();
        }

        /*
         * We do this to get a fresh instantiation of GeoConverter (prototype scope)
         */
        GeoConverter geoConverter = ( GeoConverter ) this.beanFactory.getBean( "geoConverter" );

        if ( this.geoDomainObjectGenerator == null ) {
            this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        } else {
            this.geoDomainObjectGenerator.initialize();
        }

        geoDomainObjectGenerator.setProcessPlatformsOnly( true );

        geoConverter.setForceConvertElements( true );
        Collection<Object> arrayDesigns = geoConverter.convert( platforms );

        Collection<CompositeSequence> els = ( ( ArrayDesign ) arrayDesigns.iterator().next() ).getCompositeSequences();

        for ( CompositeSequence cs : els ) {
            cs.setArrayDesign( targetPlatform );
            cs.setBiologicalCharacteristic(
                    ( BioSequence ) persisterHelper.persist( cs.getBiologicalCharacteristic() ) );
        }

        AbstractGeoService.log.info( "Adding " + els.size() + " elements to " + targetPlatform );

        targetPlatform.getCompositeSequences().addAll( els );

        arrayDesignService.update( targetPlatform );

        this.arrayDesignReportService.generateArrayDesignReport( targetPlatform.getId() );

        return targetPlatform;

    }

    @Override
    @Transactional
    public Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitByPlatform ) {
        return this.fetchAndLoad( geoAccession, loadPlatformOnly, doSampleMatching, splitByPlatform, true, true );
    }

    /**
     * Given a GEO GSE or GDS (or GPL, but support might not be complete)
     * <ol>
     * <li>Check that it doesn't already exist in the system, filter samples</li>
     * <li>Download and parse GDS files and GSE file needed</li>
     * <li>Convert the GDS and GSE into a ExpressionExperiment (or just the ArrayDesigns)
     * <li>Load the resulting ExpressionExperiment and/or ArrayDesigns into Gemma</li>
     * </ol>
     */
    @Override
    @Transactional
    public Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitByPlatform, boolean allowSuperSeriesImport, boolean allowSubSeriesImport ) {

        if ( expressionExperimentService.isBlackListed( geoAccession ) ) {
            throw new IllegalArgumentException( "Entity with accession " + geoAccession + " is blacklisted" );
        }

        /*
         * We do this to get a fresh instantiation of GeoConverter (prototype scope)
         */
        GeoConverter geoConverter = ( GeoConverter ) this.beanFactory.getBean( "geoConverter" );

        if ( this.geoDomainObjectGenerator == null ) {
            this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        } else {
            this.geoDomainObjectGenerator.initialize();
        }

        geoDomainObjectGenerator.setProcessPlatformsOnly( geoAccession.startsWith( "GPL" ) || loadPlatformOnly );
        geoDomainObjectGenerator.setDoSampleMatching( doSampleMatching && !splitByPlatform );

        Collection<DatabaseEntry> projectedAccessions = geoDomainObjectGenerator.getProjectedAccessions( geoAccession );
        this.checkForExisting( projectedAccessions );

        if ( loadPlatformOnly ) {
            Collection<? extends GeoData> platforms = geoDomainObjectGenerator.generate( geoAccession );
            if ( platforms.size() == 0 ) {
                AbstractGeoService.log
                        .warn( "GeoService.fetchAndLoad( targetPlatformAcc, true, false, false, false );t no results" );
                return null;
            }
            geoConverter.setForceConvertElements( true );

            for ( GeoData d : platforms ) {
                if ( expressionExperimentService.isBlackListed( d.getGeoAccession() ) ) {
                    throw new IllegalArgumentException(
                            "Entity with accession " + d.getGeoAccession() + " is blacklisted" );
                }
            }

            Collection<Object> arrayDesigns = geoConverter.convert( platforms );
            return persisterHelper.persist( arrayDesigns );
        }

        Collection<? extends GeoData> parseResult = geoDomainObjectGenerator.generate( geoAccession );
        if ( parseResult.size() == 0 ) {
            AbstractGeoService.log.warn( "Got no results" );
            return null;
        }
        AbstractGeoService.log.debug( "Generated GEO domain objects for " + geoAccession );

        Object obj = parseResult.iterator().next();
        if ( !( obj instanceof GeoSeries ) ) {
            throw new RuntimeException(
                    "Got a " + obj.getClass().getName() + " instead of a " + GeoSeries.class.getName()
                            + " (you may need to load platforms only)." );
        }

        GeoSeries series = ( GeoSeries ) obj;
        String seriesAccession = series.getGeoAccession();

        if ( series.isSuperSeries() ) {
            if ( allowSuperSeriesImport ) {
                AbstractGeoService.log.info( " ========= SuperSeries Detected! =========" );
                AbstractGeoService.log
                        .info( "Please make sure you want to import this as a superseries and not the individual subseries" );
            } else {
                throw new IllegalStateException(
                        "SuperSeries detected, set 'allowSuperSeriesImport' to 'true' to allow this dataset to load" );
            }
        }

        if ( series.isSubSeries() ) {
            if ( allowSubSeriesImport ) {
                AbstractGeoService.log.info( " ========= Subseries Detected! =========" );
                AbstractGeoService.log
                        .info( "Please make sure you want to import this as a subseries and not the superseries" );
            } else {
                throw new IllegalStateException(
                        "SubSeries detected, set 'allowSubSeriesImport' to 'true' to allow this dataset to load" );
            }
        }

        this.confirmPlatformUniqueness( series, doSampleMatching && !splitByPlatform );

        ArrayDesignsForExperimentCache c = new ArrayDesignsForExperimentCache();

        this.matchToExistingPlatforms( geoConverter, series, c );

        this.checkForSupportedTaxa( series );
        this.checkSamplesAreNew( series );

        this.getSubSeriesInformation( series );

        geoConverter.clear();
        geoConverter.setSplitByPlatform( splitByPlatform );

        //noinspection unchecked
        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series );

        this.check( result );

        this.getPubMedInfo( result );

        AbstractGeoService.log.debug( "Converted " + seriesAccession );
        assert persisterHelper != null;

        Collection<ExpressionExperiment> persistedResult = new HashSet<>();
        for ( ExpressionExperiment ee : result ) {
            c = expressionExperimentPrePersistService.prepare( ee, c );
            ee = persisterHelper.persist( ee, c );
            persistedResult.add( ee );
            AbstractGeoService.log.debug( "Persisted " + seriesAccession );

        }
        this.updateReports( persistedResult );

        return persistedResult;
    }

    @Override
    @Transactional
    public void updateFromGEO( String geoAccession ) {
        // load the experiment locally and complain if it doesn't exist. Note: might be split into parts in Gemma, in which case we do all.
        Collection<ExpressionExperiment> ees = this.expressionExperimentService.findByAccession( geoAccession );

        if ( ees.isEmpty() ) {
            throw new IllegalArgumentException( "No experiment with accession " + geoAccession + " found in Gemma" );
        } else {
            log.info( "Updating " + ees.size() + " experiments from accession " + geoAccession );
        }

        // other complications arise if this is a multiplatform data set that was switched/merged etc, but we will take the data for the corresponding GSMs.

        // fetch the experiment from GEO
        GeoConverter geoConverter = ( GeoConverter ) this.beanFactory.getBean( "geoConverter" );

        if ( this.geoDomainObjectGenerator == null ) {
            this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        } else {
            this.geoDomainObjectGenerator.initialize();
        }

        Collection<? extends GeoData> parseResult = geoDomainObjectGenerator.generate( geoAccession );
        Object obj = parseResult.iterator().next();
        GeoSeries series = ( GeoSeries ) obj;
        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series, true );

        /*
         * We're never splitting by platform, so we should have only one result.
         */
        if ( result.isEmpty() ) {
            throw new IllegalStateException( "No result from fetching and coversion of " + geoAccession );
        } else {
            log.info( "Got " + result.size() + " experiments from GEO for " + geoAccession );
        }

        this.getPubMedInfo( result );

        // multi-species automatically split by Gemma at conversion stage so we may have >1
        for ( ExpressionExperiment freshFromGEO : result ) {

            // make map of characteristics by GSM ID for biassays
            Map<String, BioAssay> freshBAsByGSM = new HashMap<>();
            for ( BioAssay ba : freshFromGEO.getBioAssays() ) {
                String acc = ba.getAccession().getAccession();
                freshBAsByGSM
                        .put( acc, ba );
            }

            BibliographicReference primaryPublication = freshFromGEO.getPrimaryPublication();

            // update the experiment in Gemma:
            // 1) publication
            // 2) BioMaterial Characteristics (mostly for backporting)
            // 3) Original platform (this is for backporting, and may be removed in the future)
            for ( ExpressionExperiment ee : ees ) { // because it could be a split by us.

                /*
                 * If we have more than one result from GEO due to species-splitting,
                 * then we could try to match to the existing one instead of looping over all the ones in Gemma,
                 * but it doesn't really matter, the loop over bioassays won't do anything for the mismatched one.
                 */

                int numNewCharacteristics = 0;
                boolean pubUpdate = false;

                ee = expressionExperimentService.thawLite( ee );

                if ( ee.getPrimaryPublication() == null && primaryPublication != null ) {
                    log.info( "Found new primary publication for " + geoAccession + ": " + primaryPublication.getPubAccession() );
                    primaryPublication = ( BibliographicReference ) persisterHelper.persist( primaryPublication );
                    ee.setPrimaryPublication( primaryPublication ); // persist first?
                    pubUpdate = true;
                }

                for ( BioAssay ba : ee.getBioAssays() ) {
                    BioMaterial bm = ba.getSampleUsed();
                    String gsmID = ba.getAccession().getAccession();

                    if ( !freshBAsByGSM.containsKey( gsmID ) ) { // sanity check ...
                        // suppress this warning, because if we split up front, then this is expected.
                        //log.warn( "GEO didn't have " + gsmID + " associated with " + ee );
                        continue;
                    }

                    // fill in the original paltform, if we need to.
                    ArrayDesign arrayDesignUsed = freshBAsByGSM.get( gsmID ).getArrayDesignUsed();
                    if ( ba.getOriginalPlatform() == null ) {
                        ArrayDesign ad = arrayDesignService.findByShortName( arrayDesignUsed.getShortName() );
                        if ( ad != null ) {
                            log.info( "Updating original platform for " + gsmID + " in " + ee.getShortName() + " = " + arrayDesignUsed );
                            ba.setOriginalPlatform( ad );
                            bioAssayService.update( ba );
                        } else {
                            log.warn( "Could not update original platform for " + gsmID + " in " + ee.getShortName() + ", it was not in the system: " + arrayDesignUsed );
                        }
                    }

                /*
                 delete the old characteristics and start over. Trying to match them up will often fail, and adding them instead of deleting them just creats a mess.
                 */
                    Set<Characteristic> bmchars = bm.getCharacteristics();
                    int numOldChars = bmchars.size();
                    bmchars.clear();
                    characteristicService.remove( bmchars );
                    Collection<Characteristic> freshCharacteristics = freshBAsByGSM.get( gsmID ).getSampleUsed().getCharacteristics();
                    if ( log.isDebugEnabled() )
                        log.debug( "Found " + freshCharacteristics.size() + " characteristics for " + gsmID + " replacing " + numOldChars + " old ones ..." );
                    bmchars.addAll( freshCharacteristics );
                    numNewCharacteristics += freshCharacteristics.size();
                    bioMaterialService.update( bm );
                }


                if (numNewCharacteristics == 0 ) {
                    // that's okay but probably shouldn't do anything
                } else {
                    expressionExperimentService.update( ee );
                    String message = " Updated from GEO; " + numNewCharacteristics + " characteristics added/replaced" + ( pubUpdate ? "; Publication added" : "" );
                    log.info( ee.getShortName() + message );
                    auditTrailService.addUpdateEvent( ee, ExpressionExperimentUpdateFromGEOEvent.class, message );
                }

            }
        }

    }

    @Override
    @Transactional
    public Collection<?> loadFromSoftFile( String accession, String softFile, boolean loadPlatformOnly, boolean doSampleMatching, boolean splitByPlatform ) {
        File f = new File( softFile );
        this.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( f.getParent() ) );
        return fetchAndLoad( accession, loadPlatformOnly, doSampleMatching, splitByPlatform );
    }

    private void check( Collection<ExpressionExperiment> result ) {
        for ( ExpressionExperiment expressionExperiment : result ) {
            this.check( expressionExperiment );
        }
    }

    private void check( ExpressionExperiment ee ) {
        if ( ee.getBioAssays().isEmpty() ) {
            throw new IllegalStateException( "Experiment has no bioassays " + ee.getShortName() );
        }

        if ( ee.getBioAssays().size() < MINIMUM_SAMPLE_COUNT_TO_LOAD ) {
            throw new IllegalStateException( "Experiment has too few bioassays "
                    + ee.getShortName() + ", has " + ee.getBioAssays().size() + ", minimum is  " + MINIMUM_SAMPLE_COUNT_TO_LOAD );
        }

        if ( ee.getRawExpressionDataVectors().size() == 0 ) {
            /*
             * This is okay if the platform is MPSS or Exon arrays for which we load data later.
             */
            AbstractGeoService.log.warn( "Experiment has no data vectors (this might be expected): " + ee.getShortName() );
        }

    }

    private void checkForExisting( Collection<DatabaseEntry> projectedAccessions ) {
        if ( projectedAccessions == null || projectedAccessions.size() == 0 ) {
            return; // that's okay, it might have been a GPL.
        }
        for ( DatabaseEntry entry : projectedAccessions ) {
            Collection<ExpressionExperiment> existing = expressionExperimentService.findByAccession( entry );
            if ( !existing.isEmpty() ) {
                String message = "There is already an expression experiment that matches " + entry.getAccession();
                AbstractGeoService.log.info( message );
                throw new AlreadyExistsInSystemException( message, existing );
            }
        }
    }

    private void checkForSupportedTaxa( GeoSeries series ) {
        Collection<GeoSample> toSkip = new HashSet<>();
        Collection<String> unsupportedTaxa = new HashSet<>();

        for ( GeoSample sample : series.getSamples() ) {

            // the GEO sample organism is the binomial e.g. Mus musculus, hopefully this is consistent
            if ( taxonService.findByScientificName( sample.getOrganism() ) == null ) {
                toSkip.add( sample );
                unsupportedTaxa.add( sample.getOrganism() );
            }

        }

        for ( GeoSample gs : toSkip ) {
            series.getSamples().remove( gs );
            series.getSampleCorrespondence().removeSample( gs.getGeoAccession() );
        }

        for ( GeoDataset gds : series.getDataSets() ) {
            for ( GeoSubset gSub : gds.getSubsets() ) {
                for ( GeoSample gs : toSkip ) {
                    gSub.getSamples().remove( gs );
                }
            }
        }

        if ( series.getSamples().isEmpty() ) {
            throw new IllegalStateException(
                    "Data set is for unsupported taxa (" + StringUtils.join( unsupportedTaxa, ";" ) + ")" + series );
        }

        if ( series.getSamples().size() < MINIMUM_SAMPLE_COUNT_TO_LOAD ) {
            throw new IllegalStateException(
                    "After removing samples from unsupported taxa, this data set is too small to load: " + series
                            .getSamples().size() + " left (removed " + toSkip.size() + ")" );
        }

        if ( !toSkip.isEmpty() ) {
            AbstractGeoService.log.info( "Found " + toSkip.size()
                    + " samples from unsupported taxa: " + StringUtils.join( unsupportedTaxa, "; " ) );
            AbstractGeoService.log
                    .info( "Series now contains " + series.getSamples().size() );
        }

    }

    /**
     * Another common case, typified by samples in GSE3193. We must confirm that all samples included in the data set
     * are not included in other data sets. In GEO this primarily occurs in 'superseries' that combine other series.
     */
    private void checkSamplesAreNew( GeoSeries series ) {
        Collection<GeoSample> toSkip = new HashSet<>();

        for ( GeoSample sample : series.getSamples() ) {
            if ( !sample.appearsInMultipleSeries() ) {
                // nothing to worry about: if this series is not loaded, then we're guaranteed to be new.
                continue;
            }

            Collection<BioAssay> existingBioAssays = bioAssayService.findByAccession( sample.getGeoAccession() );
            for ( BioAssay ba : existingBioAssays ) {
                DatabaseEntry acc = ba.getAccession();
                if ( acc == null )
                    continue;

                String sampleId = sample.getGeoAccession();
                String existingAcc = acc.getAccession();
                if ( existingAcc.equals( sampleId ) && ba.getAccession().getExternalDatabase().getName()
                        .equals( GeoServiceImpl.GEO_DB_NAME ) ) {
                    AbstractGeoService.log
                            .debug( sampleId + " appears in an expression experiment already in the system, skipping" );
                    toSkip.add( sample );
                }
            }
        }

        if ( !toSkip.isEmpty() ) {
            AbstractGeoService.log.info( "Found " + toSkip.size()
                    + " samples that are already in the system; they will be removed from the new set (example: "
                    + toSkip.iterator().next().getGeoAccession() + ")" );
        }

        for ( GeoSample gs : toSkip ) {
            series.getSamples().remove( gs );
            series.getSampleCorrespondence().removeSample( gs.getGeoAccession() );
        }

        for ( GeoDataset gds : series.getDataSets() ) {
            for ( GeoSubset gSub : gds.getSubsets() ) {
                for ( GeoSample gs : toSkip ) {
                    gSub.getSamples().remove( gs );
                }
            }
        }

        // update the description, so we keep some kind of record.
        if ( toSkip.size() > 0 ) {
            series.setSummaries( series.getSummaries() + ( StringUtils.isBlank( series.getSummaries() ) ? "" : "\n" ) + "Note: " + toSkip.size()
                    + " samples from this series, which appear in other Expression Experiments in Gemma, "
                    + "were not imported from the GEO source. The following samples were removed: " + StringUtils
                    .join( toSkip, "," ) );
        }

        if ( series.getSamples().size() == 0 ) {
            throw new AlreadyExistsInSystemException(
                    "All the samples in " + series + " are in the system already (in other ExpressionExperiments)" );
        }

        if ( series.getSamples().size() < 2 /* we don't really have a lower limit set anywhere else */ ) {
            throw new IllegalStateException(
                    "After removing samples already in the system, this data set is too small to load: " + series
                            .getSamples().size() + " left (removed " + toSkip.size() + ")" );
        }

        AbstractGeoService.log
                .info( "Series now contains " + series.getSamples().size() + " (removed " + toSkip.size() + ")" );

    }

    /**
     * @param  datasets all of which must use the same platform.
     * @return one data set, which contains all the samples and subsets.
     */
    private GeoDataset combineDatasets( Collection<GeoDataset> datasets ) {
        if ( datasets.size() == 1 )
            return datasets.iterator().next();

        // defensive programming
        GeoPlatform lastPlatform = null;
        for ( GeoDataset dataset : datasets ) {
            GeoPlatform platform = dataset.getPlatform();
            if ( lastPlatform != null ) {
                if ( !platform.equals( lastPlatform ) ) {
                    throw new IllegalArgumentException( "All datasets to be collapsed must use the same platform" );
                }
            }
            lastPlatform = platform;
        }

        // arbitrarily use the first.
        GeoDataset result = datasets.iterator().next();
        for ( GeoDataset dataset : datasets ) {
            if ( dataset.equals( result ) )
                continue;
            assert dataset.getPlatform().equals( lastPlatform );
            AbstractGeoService.log
                    .info( "Collapsing " + dataset + " into " + result + " Platform=" + dataset.getPlatform() );
            result.setDescription(
                    result.getDescription() + " Note: this dataset " + "includes the samples from " + dataset );
            result.getSubsets().addAll( dataset.getSubsets() );
            result.setNumSamples( result.getNumSamples() + dataset.getNumSamples() );
            result.getColumnNames().addAll( dataset.getColumnNames() );
            result.getColumnDescriptions().addAll( dataset.getColumnDescriptions() );
        }

        return result;
    }

    /**
     * Check if all the data sets are on different platforms. This is a rare case in GEO (example: GSE18). When it
     * happens we merge the datasets.
     */
    private void confirmPlatformUniqueness( GeoSeries series, boolean doSampleMatching ) {
        Set<GeoPlatform> platforms = this.getPlatforms( series );
        if ( platforms.size() == series.getDataSets().size() ) {
            return;
        }
        Collection<GeoDataset> collapsed = this.potentiallyCombineDatasets( series.getDataSets() );
        series.setDataSets( collapsed );
        DatasetCombiner combiner = new DatasetCombiner( doSampleMatching );
        GeoSampleCorrespondence corr = combiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( corr );
    }

    /**
     * Build the map between the existing probe names and the ones in gemma. The information is stored in the
     * GeoPlatform object
     */
    private void fillExistingProbeNameMap( GeoPlatform pl, String columnWithGemmaNames, String columnWithGeoNames ) {

        assert StringUtils.isNotBlank( columnWithGemmaNames );
        assert StringUtils.isNotBlank( columnWithGeoNames );

        List<String> gemmaNames = pl.getColumnData( columnWithGemmaNames );
        if ( gemmaNames == null ) {
            // only if we are not loading the data here.
            AbstractGeoService.log.warn( "Not associating data from GEO for this platform." );
            return;
        }
        List<String> geoNames = pl.getColumnData( columnWithGeoNames );
        assert gemmaNames.size() == geoNames.size();
        AbstractGeoService.log.debug( "Matching up " + geoNames.size() + " probe names" );
        for ( int i = 0; i < gemmaNames.size(); i++ ) {
            String gemmaName = gemmaNames.get( i );
            String geoName = geoNames.get( i );
            if ( AbstractGeoService.log.isDebugEnabled() )
                AbstractGeoService.log.debug( "GEO name:" + geoName + "; name to be used for Gemma:" + gemmaName );
            pl.getProbeNamesInGemma().put( geoName, gemmaName );
        }
    }

    /**
     * If the names in Gemma are not the same as the ID in GEO, we have to switch the new data over. This only works if
     * the new names are given in another column in the GEO data. It also happens that sometimes some names in Gemma
     * match multiple columns in GEO (for example, blank spots). We need to find the column with the unique match.
     * The default column is 'ID' as per GEO specifications. We always use this unless the platform probes have been
     * renamed (which we don't do routinely, any more; this was a practice in response to an annoying feature of GEO).
     */
    private void getGemmaIDColumnNameInGEO( GeoPlatform rawGEOPlatform, Map<CompositeSequence, BioSequence> m,
            String columnWithGeoNames ) {

        /*
         * This can happen if there is a corrupt version of the array design in the system -- can occur in tests for
         * example.
         */
        if ( m.isEmpty() ) {
            // use the same column name.
            AbstractGeoService.log.warn( "Array design had no probes (corrupt or test?" );
            this.fillExistingProbeNameMap( rawGEOPlatform, columnWithGeoNames, columnWithGeoNames );
            return;
        }

        Map<String, Integer> countMatches = new HashMap<>();
        for ( String geoColName : rawGEOPlatform.getColumnNames() ) {
            List<String> columnData = rawGEOPlatform.getColumnData( geoColName );

            if ( columnData == null ) {
                AbstractGeoService.log.warn( "No column data for " + geoColName ); // ok
                continue;
            }

            /*
             * Never bother with columns like "COL" or "ROW" as these are positions.
             */
            if ( geoColName.equalsIgnoreCase( "ROW" ) || geoColName.equalsIgnoreCase( "COL" ) ) {
                continue;
            }

            // figure out the best column;if not ID, then usually NAME or SPOT_ID etc
            Set<String> colDataSet = new HashSet<>( columnData );

            int numMatchesInColumn = 0;
            for ( CompositeSequence cs : m.keySet() ) {
                String gemmaProbeName = cs.getName();
                if ( colDataSet.contains( gemmaProbeName ) ) {
                    numMatchesInColumn++;
                }
            }

            if ( numMatchesInColumn == m.size() ) {
                AbstractGeoService.log.info( "Exact Gemma probe names were found in GEO column=" + geoColName );
                this.fillExistingProbeNameMap( rawGEOPlatform, geoColName, columnWithGeoNames );
                return;
            }

            countMatches.put( geoColName, numMatchesInColumn );
        }

        String bestCol = "";
        int bestMatchSize = 0;
        for ( String colName : countMatches.keySet() ) {
            if ( countMatches.get( colName ) > bestMatchSize ) {
                bestMatchSize = countMatches.get( colName );
                bestCol = colName;
            }
        }

        if ( bestMatchSize == 0 ) {
            AbstractGeoService.log
                    .warn( "The best-matching column, " + bestCol + " matched too few: " + bestMatchSize + "/" + m
                            .size() + " probe names for " + rawGEOPlatform );
            throw new IllegalStateException(
                    "Could not figure out which column the Gemma probe names came (e.g.: " + m.keySet().iterator()
                            .next().getName() + ") from for platform=" + rawGEOPlatform );
        }

        AbstractGeoService.log
                .warn( "Using the best-matching column, " + bestCol + "  matched " + bestMatchSize + "/" + m.size()
                        + " probe names for " + rawGEOPlatform );
        this.fillExistingProbeNameMap( rawGEOPlatform, bestCol, columnWithGeoNames );

    }

    /**
     * @param rawGEOPlatform Our representation of the original GEO platform
     * @param geoArrayDesign Our conversion
     */
    private String getGEOIDColumnName( GeoPlatform rawGEOPlatform, ArrayDesign geoArrayDesign ) {

        if ( rawGEOPlatform.getDesignElements().isEmpty() ) {
            AbstractGeoService.log.info( "Platform has no elements: " + rawGEOPlatform );
            return null;
        }

        // This should always be "ID", so this is just defensive programming.
        for ( CompositeSequence cs : geoArrayDesign.getCompositeSequences() ) {
            String geoProbeName = cs.getName();
            for ( String colName : rawGEOPlatform.getColumnNames() ) {
                List<String> columnData = rawGEOPlatform.getColumnData( colName );
                if ( columnData != null && columnData.contains( geoProbeName ) ) {
                    if ( !colName.equals( "ID" ) ) {
                        // this would be unusual
                        AbstractGeoService.log.info( "GEO probe names were found in GEO column=" + colName );
                    }
                    return colName;
                }
            }
        }

        throw new IllegalStateException( "Could not figure out which column the GEO probe names came from!" );

    }

    private Set<GeoPlatform> getPlatforms( GeoSeries series ) {
        Set<GeoPlatform> platforms = new HashSet<>();

        if ( series.getDataSets().size() > 0 ) {
            for ( GeoDataset dataset : series.getDataSets() ) {
                platforms.add( dataset.getPlatform() );
            }
        } else {
            for ( GeoSample sample : series.getSamples() ) {
                Collection<GeoPlatform> samplePlatforms = sample.getPlatforms();
                platforms.addAll( samplePlatforms );
            }
        }
        return platforms;
    }

    private void getPubMedInfo( Collection<ExpressionExperiment> result ) {
        for ( ExpressionExperiment experiment : result ) {
            BibliographicReference pubmed = experiment.getPrimaryPublication();
            if ( pubmed == null )
                continue;
            PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
            try {
                pubmed = fetcher.retrieveByHTTP( Integer.parseInt( pubmed.getPubAccession().getAccession() ) );
            } catch ( Exception e ) {
                AbstractGeoService.log.warn( "Filed to get data from pubmed, continuing without it." );
                AbstractGeoService.log.error( e, e );
            }
            if ( pubmed == null )
                continue;
            experiment.setPrimaryPublication( pubmed );

            // don't spam NCBI. > 3 per second is a no-no without an API key
            // see https://ncbiinsights.ncbi.nlm.nih.gov/2017/11/02/new-api-keys-for-the-e-utilities/
            try {
                Thread.sleep( 350 );
            } catch ( InterruptedException e ) {
                //
            }
        }
    }

    /**
     * Populate the series information based on the sub-series.
     */
    private void getSubSeriesInformation( GeoSeries superSeries ) {
        for ( String subSeriesAccession : superSeries.getSubSeries() ) {
            AbstractGeoService.log.info( "Processing subseries " + subSeriesAccession );
            geoDomainObjectGenerator.initialize();
            Collection<? extends GeoData> parseResult = geoDomainObjectGenerator.generate( subSeriesAccession );
            if ( parseResult.size() == 0 ) {
                AbstractGeoService.log.warn( "Got no results for " + subSeriesAccession );
                continue;
            }
            AbstractGeoService.log.debug( "Generated GEO domain objects for SubSeries " + subSeriesAccession );

            Object obj = parseResult.iterator().next();
            if ( !( obj instanceof GeoSeries ) ) {
                throw new RuntimeException(
                        "Got a " + obj.getClass().getName() + " instead of a " + GeoSeries.class.getName()
                                + " (you may need to load platforms only)." );
            }
            GeoSeries subSeries = ( GeoSeries ) obj;

            /*
             * Copy basic information
             */
            superSeries.getKeyWords().addAll( subSeries.getKeyWords() );
            superSeries.getContributers().addAll( subSeries.getContributers() );
            superSeries.getPubmedIds().addAll( subSeries.getPubmedIds() );
            String seriesSummary = superSeries.getSummaries();
            seriesSummary = seriesSummary + ( StringUtils.isBlank( seriesSummary ) ? "" : "\n" ) + "Summary from subseries "
                    + subSeries.getGeoAccession() + ": " + subSeries
                    .getSummaries();
            superSeries.setSummaries( seriesSummary );
        }
    }

    private void matchToExistingPlatform( GeoConverter geoConverter, GeoPlatform rawGEOPlatform,
            ArrayDesignsForExperimentCache c ) {
        // we have to populate this.
        Map<String, String> probeNamesInGemma = rawGEOPlatform.getProbeNamesInGemma();

        // do a partial conversion. We will throw this away;
        ArrayDesign geoArrayDesign = ( ArrayDesign ) geoConverter.convert( rawGEOPlatform );

        if ( geoArrayDesign == null ) {
            if ( !rawGEOPlatform.useDataFromGeo() ) {
                // MPSS, exon arrays
                return;
            }
            throw new IllegalStateException( "Platform is missing" );
        }

        // find in our system. Note we only use the short name. The full name can change in GEO, causing trouble.
        ArrayDesign existing = arrayDesignService.findByShortName( geoArrayDesign.getShortName() );

        if ( existing == null ) {
            AbstractGeoService.log.info( rawGEOPlatform + " looks new to Gemma" );
            for ( CompositeSequence cs : geoArrayDesign.getCompositeSequences() ) {
                String geoProbeName = cs.getName();
                probeNamesInGemma.put( geoProbeName, geoProbeName );
                // no mapping needed. NB the converter fills
                // this in already, we're just being defensive
                // here.
            }
        } else {
            AbstractGeoService.log.info( "Platform " + rawGEOPlatform.getGeoAccession()
                    + " exists in Gemma, checking for correct probe names and re-matching if necessary ..." );

            String columnWithGeoNames;
            columnWithGeoNames = this.getGEOIDColumnName( rawGEOPlatform, geoArrayDesign );

            if ( columnWithGeoNames == null ) {
                // no problem: this means the design has no elements, so it is actually a placeholder (e.g., MPSS)
                return;
            }

            AbstractGeoService.log.info( "Loading probes ..." );
            Map<CompositeSequence, BioSequence> m = arrayDesignService.getBioSequences( existing );
            c.add( existing, m );

            this.getGemmaIDColumnNameInGEO( rawGEOPlatform, m, columnWithGeoNames );

        }
    }

    /**
     * If platforms used exist in Gemma already, make sure the probe names match the ones in our system, and if not, try
     * to figure out the correct mapping. This is necessary because we sometimes rename probes to match other data
     * sources. Often GEO platforms just have integer ids.
     * In addition, we test if platforms are blacklisted.
     */
    private void matchToExistingPlatforms( GeoConverter geoConverter, GeoSeries series,
            ArrayDesignsForExperimentCache c ) {

        Set<GeoPlatform> platforms = this.getPlatforms( series );
        if ( platforms.size() == 0 )
            throw new IllegalStateException( "Series has no platforms" );
        for ( GeoPlatform pl : platforms ) {

            if ( expressionExperimentService.isBlackListed( pl.getGeoAccession() ) ) {
                throw new IllegalArgumentException(
                        "A platform used by " + series.getGeoAccession() + " is blacklisted: " + pl.getGeoAccession() );
            }

            /*
             * We suppress the analysis of the array design if it is not supported (i.e. MPSS or exon arrays)
             */
            if ( !pl.useDataFromGeo() ) {
                continue;
            }

            this.matchToExistingPlatform( geoConverter, pl, c );
        }
    }

    /**
     * Combine data sets that use the same platform into one.
     *
     * @param datasets, some of which will be combined.
     */
    private Collection<GeoDataset> potentiallyCombineDatasets( Collection<GeoDataset> datasets ) {
        if ( datasets.size() == 1 )
            return datasets;
        Map<GeoPlatform, Collection<GeoDataset>> seenPlatforms = new HashMap<>();
        for ( GeoDataset dataset : datasets ) {
            GeoPlatform platform = dataset.getPlatform();
            if ( !seenPlatforms.containsKey( platform ) ) {
                seenPlatforms.put( platform, new HashSet<>() );
            }
            seenPlatforms.get( platform ).add( dataset );
        }

        Collection<GeoDataset> finishedDatasets = new HashSet<>();
        for ( GeoPlatform platform : seenPlatforms.keySet() ) {
            Collection<GeoDataset> datasetsForPlatform = seenPlatforms.get( platform );
            if ( datasetsForPlatform.size() > 1 ) {
                GeoDataset combined = this.combineDatasets( datasetsForPlatform );
                finishedDatasets.add( combined );
            } else {
                finishedDatasets.add( datasetsForPlatform.iterator().next() );
            }
        }
        return finishedDatasets;

    }

    private void updateReports( Collection<?> entities ) {
        Collection<ArrayDesign> adsToUpdate = new HashSet<>();
        for ( Object entity : entities ) {
            if ( entity instanceof ExpressionExperiment ) {
                ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) entity;
                expressionExperiment = this.expressionExperimentService.thaw( expressionExperiment );
                this.expressionExperimentReportService.generateSummary( expressionExperiment.getId() );

                expressionExperiment = this.expressionExperimentService.thaw( expressionExperiment );
                for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
                    adsToUpdate.add( ba.getArrayDesignUsed() );
                }

            } else if ( entity instanceof ArrayDesign ) {
                adsToUpdate.add( ( ArrayDesign ) entity );
            }
        }

        for ( ArrayDesign arrayDesign : adsToUpdate ) {
            this.arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        }

    }

}
