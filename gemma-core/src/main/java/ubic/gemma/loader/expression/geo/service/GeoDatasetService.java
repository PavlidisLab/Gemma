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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoData;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoSubset;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class GeoDatasetService extends AbstractGeoService {

    private static final String GEO_DB_NAME = "GEO";

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SecurityService securityService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.AbstractGeoService#fetchAndLoad(java.lang.String, boolean, boolean,
     * boolean, boolean)
     */
    @Override
    public Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean aggressiveQuantitationTypeRemoval, boolean splitByPlatform ) {
        return this.fetchAndLoad( geoAccession, loadPlatformOnly, doSampleMatching, aggressiveQuantitationTypeRemoval,
                splitByPlatform, true, true );
    }

    /**
     * Given a GEO GSE or GDS (or GPL, but support might not be complete)
     * <ol>
     * <li>Check that it doesn't already exist in the system</li>
     * <li>Download and parse GDS files and GSE file needed</li>
     * <li>Convert the GDS and GSE into a ExpressionExperiment (or just the ArrayDesigns)
     * <li>Load the resulting ExpressionExperiment and/or ArrayDesigns into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean aggressiveQuantitationTypeRemoval, boolean splitByPlatform, boolean allowSuperSeriesImport,
            boolean allowSubSeriesImport ) {

        /*
         * We do this to get a fresh instantiation of GeoConverter (prototype scope)
         */
        GeoConverter geoConverter = ( GeoConverter ) this.beanFactory.getBean( "geoConverter" );

        if ( this.geoDomainObjectGenerator == null ) {
            this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        } else {
            this.geoDomainObjectGenerator.initialize();
        }

        geoDomainObjectGenerator.setProcessPlatformsOnly( loadPlatformOnly );
        geoDomainObjectGenerator.setDoSampleMatching( doSampleMatching && !splitByPlatform );
        geoDomainObjectGenerator.setAggressiveQtRemoval( aggressiveQuantitationTypeRemoval );

        Collection<DatabaseEntry> projectedAccessions = geoDomainObjectGenerator.getProjectedAccessions( geoAccession );
        checkForExisting( projectedAccessions );

        if ( loadPlatformOnly ) {
            Collection<? extends GeoData> platforms = geoDomainObjectGenerator.generate( geoAccession );
            if ( platforms.size() == 0 ) {
                log.warn( "Got no results" );
                return null;
            }
            Collection<Object> arrayDesigns = geoConverter.convert( platforms );
            return persisterHelper.persist( arrayDesigns );
        }

        Collection<? extends GeoData> parseResult = geoDomainObjectGenerator.generate( geoAccession );
        if ( parseResult.size() == 0 ) {
            log.warn( "Got no results" );
            return null;
        }
        log.debug( "Generated GEO domain objects for " + geoAccession );

        Object obj = parseResult.iterator().next();
        if ( !( obj instanceof GeoSeries ) ) {
            throw new RuntimeException( "Got a " + obj.getClass().getName() + " instead of a "
                    + GeoSeries.class.getName() + " (you may need to load platforms only)." );
        }

        GeoSeries series = ( GeoSeries ) obj;
        String seriesAccession = series.getGeoAccession();

        if ( series.isSuperSeries() ) {
            if ( allowSuperSeriesImport ) {
                log.info( " ========= SuperSeries Detected! =========" );
                log.info( "Please make sure you want to import this as a superseries and not the individual subseries" );
            } else {
                throw new IllegalStateException(
                        "SuperSeries detected, set 'allowSuperSeriesImport' to 'true' to allow this dataset to load" );
            }
        }

        if ( series.isSubSeries() ) {
            if ( allowSubSeriesImport ) {
                log.info( " ========= Subseries Detected! =========" );
                log.info( "Please make sure you want to import this as a subseries and not the superseries" );
            } else {
                throw new IllegalStateException(
                        "SubSeries detected, set 'allowSubSeriesImport' to 'true' to allow this dataset to load" );
            }
        }

        confirmPlatformUniqueness( series, doSampleMatching && !splitByPlatform );

        matchToExistingPlatforms( geoConverter, series );

        checkSamplesAreNew( series );

        getSubSeriesInformation( series );

        geoConverter.clear();
        geoConverter.setSplitByPlatform( splitByPlatform );

        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series );

        check( result );

        series = null; // hopefully free memory...
        parseResult = null;

        getPubMedInfo( result );

        log.debug( "Converted " + seriesAccession );
        assert persisterHelper != null;
        Collection persistedResult = persisterHelper.persist( result );
        log.debug( "Persisted " + seriesAccession );
        updateReports( persistedResult );

        securityService.makePrivate( persistedResult ); // TODO make this optional?

        return persistedResult;
    }

    /**
     * @param arrayDesignReportService the arrayDesignReportService to set
     */
    public void setArrayDesignReportService( ArrayDesignReportService arrayDesignReportService ) {
        this.arrayDesignReportService = arrayDesignReportService;
    }

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param expressionExperimentReportService the expressionExperimentReportService to set
     */
    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    private void check( Collection<ExpressionExperiment> result ) {
        for ( ExpressionExperiment expressionExperiment : result ) {
            check( expressionExperiment );
        }
    }

    private void check( ExpressionExperiment ee ) {

        if ( ee.getBioAssays().size() == 0 ) {
            throw new IllegalStateException( "Experiment has no bioassays " + ee );
        }

        if ( ee.getRawExpressionDataVectors().size() == 0 ) {
            throw new IllegalStateException( "Experiment has no data vectors " + ee );
        }

    }

    /**
     * @param projectedAccessions
     */
    private void checkForExisting( Collection<DatabaseEntry> projectedAccessions ) {
        if ( projectedAccessions == null || projectedAccessions.size() == 0 ) {
            return; // that's okay, it might have been a GPL.
        }
        for ( DatabaseEntry entry : projectedAccessions ) {
            Collection<ExpressionExperiment> existing = expressionExperimentService.findByAccession( entry );
            if ( !existing.isEmpty() ) {
                String message = "There is already an expression experiment that matches " + entry.getAccession();
                log.info( message );
                throw new AlreadyExistsInSystemException( message, existing );
            }
        }
    }

    /**
     * Another common case, typified by samples in GSE3193. We must confirm that all samples included in the data set
     * are not included in other data sets. In GEO this primarily occurs in 'superseries' that combine other series.
     * 
     * @param series
     */
    private void checkSamplesAreNew( GeoSeries series ) {
        Collection<GeoSample> toSkip = new HashSet<GeoSample>();

        for ( GeoSample sample : series.getSamples() ) {
            if ( !sample.appearsInMultipleSeries() ) {
                // nothing to worry about: if this series is not loaded, then we're guaranteed to be new.
                continue;
            }

            Collection<BioAssay> existingBioAssays = bioAssayService.findByAccession( sample.getGeoAccession() );
            for ( BioAssay ba : existingBioAssays ) {
                DatabaseEntry acc = ba.getAccession();
                if ( acc == null ) continue;

                String sampleId = sample.getGeoAccession();
                String existingAcc = acc.getAccession();
                if ( existingAcc.equals( sampleId )
                        && ba.getAccession().getExternalDatabase().getName().equals( GEO_DB_NAME ) ) {
                    log.debug( sampleId + " appears in an expression experiment already in the system, skipping" );
                    toSkip.add( sample );
                }
            }
        }

        if ( !toSkip.isEmpty() ) {
            log.info( "Found " + toSkip.size()
                    + " samples that are already in the system; they will be removed from the new set (example: "
                    + toSkip.iterator().next().getGeoAccession() + ")" );
        }

        for ( GeoSample gs : toSkip ) {
            series.getSamples().remove( gs );
            series.getSampleCorrespondence().removeSample( gs.getGeoAccession() );
        }

        for ( GeoDataset gds : series.getDatasets() ) {
            for ( GeoSubset gsub : gds.getSubsets() ) {
                for ( GeoSample gs : toSkip ) {
                    gsub.getSamples().remove( gs );
                }
            }
        }

        // update the description, so we keep some kind of record.
        if ( toSkip.size() > 0 ) {
            series.setSummaries( series.getSummaries() + "\nNote: " + toSkip.size()
                    + " samples from this series, which appear in other Expression Experiments in Gemma, "
                    + "were not imported from the GEO source. The following samples were removed: "
                    + StringUtils.join( toSkip, "," ) );
        }

        if ( series.getSamples().size() == 0 ) {
            throw new AlreadyExistsInSystemException( "All the samples in " + series
                    + " are in the system already (in other ExpressionExperiments)" );
        }

        if ( series.getSamples().size() < 2 /* we don't really have a lower limit set anywhere else */) {
            throw new IllegalStateException(
                    "After removing samples already in the system, this data set is too small to load: "
                            + series.getSamples().size() + " left (removed " + toSkip.size() + ")" );
        }

        log.info( "Series now contains " + series.getSamples().size() + " (removed " + toSkip.size() + ")" );

    }

    /**
     * @param datasets all of which must use the same platform.
     * @return one data set, which contains all the samples and subsets.
     */
    private GeoDataset combineDatasets( Collection<GeoDataset> datasets ) {
        if ( datasets.size() == 1 ) return datasets.iterator().next();

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
            if ( dataset.equals( result ) ) continue;
            log.info( "Collapsing " + dataset + " into " + result );
            result.setDescription( result.getDescription() + " Note: this dataset " + "includes the samples from "
                    + dataset );
            result.getSubsets().addAll( dataset.getSubsets() );
            result.setNumSamples( result.getNumSamples() + dataset.getNumSamples() );
            result.getColumnNames().addAll( dataset.getColumnNames() );
            result.getColumnDescriptions().addAll( dataset.getColumnDescriptions() );
        }

        return result;
    }

    /**
     * Check if all the data sets are on different platforms. This is a rare case in GEO. When it happens we merge the
     * datasets.
     */
    private void confirmPlatformUniqueness( GeoSeries series, boolean doSampleMatching ) {
        Set<GeoPlatform> platforms = getPlatforms( series );
        if ( platforms.size() == series.getDatasets().size() ) {
            return;
        }
        Collection<GeoDataset> collapsed = potentiallyCombineDatasets( series.getDatasets() );
        series.setDataSets( collapsed );
        DatasetCombiner combiner = new DatasetCombiner( doSampleMatching );
        GeoSampleCorrespondence corr = combiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( corr );
    }

    /**
     * Build the map between the existing probe names and the ones in gemma. The information is stored in the
     * GeoPlatform object
     * 
     * @param pl
     * @param columnWithGemmaNames
     * @param columnWithGeoNames
     */
    private void fillExistingProbeNameMap( GeoPlatform pl, String columnWithGemmaNames, String columnWithGeoNames ) {
        List<String> gemmaNames = pl.getColumnData( columnWithGemmaNames );
        List<String> geoNames = pl.getColumnData( columnWithGeoNames );
        assert gemmaNames.size() == geoNames.size();
        log.debug( "Matching up " + geoNames.size() + " probe names" );
        for ( int i = 0; i < gemmaNames.size(); i++ ) {
            String gemmaName = gemmaNames.get( i );
            String geoName = geoNames.get( i );
            if ( log.isDebugEnabled() )
                log.debug( "GEO name:" + geoName + "; name to be used for Gemma:" + gemmaName );
            pl.getProbeNamesInGemma().put( geoName, gemmaName );
        }
    }

    /**
     * If the names in Gemma are not the same as the ID in GEO, we have to switch the new data over. This only works if
     * the new names are given in another column in the GEO data. It als happens that sometimes some names in Gemma
     * match multiple columns in GEO (for example, blank spots). We need to find the column with the unique match.
     * 
     * @param rawGEOPlatform
     * @param existing
     * @param columnWithGeoNames
     */
    private void getGemmaIDColumnNameInGEO( GeoPlatform rawGEOPlatform, ArrayDesign existing, String columnWithGeoNames ) {

        String columnWithGemmaNames = null;

        /*
         * This can happen if there is a corrupt version of the array design in the system -- can occur in tests for
         * example.
         */
        if ( existing.getCompositeSequences().isEmpty() ) {
            fillExistingProbeNameMap( rawGEOPlatform, columnWithGeoNames, columnWithGeoNames );
            return;
        }

        for ( CompositeSequence cs : existing.getCompositeSequences() ) {
            String gemmaProbeName = cs.getName();
            // search the other columns
            int numColsMatching = 0;
            for ( String colName : rawGEOPlatform.getColumnNames() ) {
                if ( rawGEOPlatform.getColumnData( colName ).contains( gemmaProbeName ) ) {

                    /*
                     * Note: Spurious matches can happen if the ID is an integer and if there are other columns that
                     * have numbers, so this can fail.
                     */
                    numColsMatching++;
                    columnWithGemmaNames = colName;

                    /*
                     * The default column is 'ID' as per GEO specifications. We always use this unless the platform
                     * probes have been renamed. Since this is the most common case, we stop here. Otherwise we go on to
                     * check for other matching columns.
                     */
                    if ( colName.equals( "ID" ) ) {
                        fillExistingProbeNameMap( rawGEOPlatform, columnWithGemmaNames, columnWithGeoNames );
                        return;
                    }

                }
            }

            // if more than one column matches, keep going until we find a unique match. If none match, then we're
            // probably going to fail.
            if ( numColsMatching == 1 ) {
                log.info( "Gemma probe names were found in GEO column=" + columnWithGemmaNames );
                fillExistingProbeNameMap( rawGEOPlatform, columnWithGemmaNames, columnWithGeoNames );
                return;
            }
        }

        throw new IllegalStateException( "Could not figure out which column the Gemma probe names came (e.g.: "
                + existing.getCompositeSequences().iterator().next().getName() + ") from for platform="
                + rawGEOPlatform );

    }

    /**
     * @param rawGEOPlatform
     * @param geoArrayDesign
     * @param columnWithGeoNames
     * @return
     */
    private String getGEOIDColumnName( GeoPlatform rawGEOPlatform, ArrayDesign geoArrayDesign, String columnWithGeoNames ) {
        Set<String> geoProbeNames = new HashSet<String>();

        // This should always be "ID", so this is just defensive programming.
        for ( CompositeSequence cs : geoArrayDesign.getCompositeSequences() ) {
            String geoProbeName = cs.getName();
            geoProbeNames.add( geoProbeName );
            for ( String colName : rawGEOPlatform.getColumnNames() ) {
                if ( rawGEOPlatform.getColumnData( colName ).contains( geoProbeName ) ) {
                    columnWithGeoNames = colName;
                    log.info( "GEO probe names were found in GEO column=" + columnWithGeoNames );
                    return columnWithGeoNames;
                }
            }
        }

        throw new IllegalStateException( "Could not figure out which column the GEO probe names came from!" );

    }

    /**
     * @param series
     * @return
     */
    private Set<GeoPlatform> getPlatforms( GeoSeries series ) {
        Set<GeoPlatform> platforms = new HashSet<GeoPlatform>();

        if ( series.getDatasets().size() > 0 ) {
            for ( GeoDataset dataset : series.getDatasets() ) {
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

    /**
     * @param result
     */
    private void getPubMedInfo( Collection<ExpressionExperiment> result ) {
        for ( ExpressionExperiment experiment : result ) {
            BibliographicReference pubmed = experiment.getPrimaryPublication();
            if ( pubmed == null ) continue;
            PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
            try {
                pubmed = fetcher.retrieveByHTTP( Integer.parseInt( pubmed.getPubAccession().getAccession() ) );
            } catch ( Exception e ) {
                log.warn( "Filed to get data from pubmed, continuing without it." );
                log.error( e, e );
            }
            if ( pubmed == null ) continue;
            experiment.setPrimaryPublication( pubmed );

        }
    }

    /**
     * Populate the series information based on the subseries.
     * 
     * @param superSeries
     */
    private void getSubSeriesInformation( GeoSeries superSeries ) {
        for ( String subSeriesAccession : superSeries.getSubSeries() ) {
            log.info( "Processing subseries " + subSeriesAccession );
            geoDomainObjectGenerator.initialize();
            Collection<? extends GeoData> parseResult = geoDomainObjectGenerator.generate( subSeriesAccession );
            if ( parseResult.size() == 0 ) {
                log.warn( "Got no results for " + subSeriesAccession );
                continue;
            }
            log.debug( "Generated GEO domain objects for SubSeries " + subSeriesAccession );

            Object obj = parseResult.iterator().next();
            if ( !( obj instanceof GeoSeries ) ) {
                throw new RuntimeException( "Got a " + obj.getClass().getName() + " instead of a "
                        + GeoSeries.class.getName() + " (you may need to load platforms only)." );
            }
            GeoSeries subSeries = ( GeoSeries ) obj;

            /*
             * Copy basic information
             */
            superSeries.getKeyWords().addAll( subSeries.getKeyWords() );
            superSeries.getContributers().addAll( subSeries.getContributers() );
            superSeries.getPubmedIds().addAll( subSeries.getPubmedIds() );
            String seriesSummary = superSeries.getSummaries();
            seriesSummary = seriesSummary + "\nSummary from subseries " + subSeries.getGeoAccession() + ": "
                    + subSeries.getSummaries();
            superSeries.setSummaries( seriesSummary );

            // The following code needs a test case: where the subseries have associated GDS but the superseries does
            // not.
            // /*
            // * Get experimental design information, if available.
            // */
            // for ( GeoDataset ds : subSeries.getDatasets() ) {
            // log.info( "Adding dataset to superseries: " + ds );
            // superSeries.addDataSet( ds );
            // }
            // // Add variable information to the series
            // // copy variable information to the samples
            // for ( Integer k : subSeries.getVariables().keySet() ) {
            // GeoVariable geoVariable = subSeries.getVariables().get( k );
            // log.info( "Adding variable " + geoVariable + " with index " + k + " to superseries" );
            // superSeries.getVariables().put( k, geoVariable );
            // }
            //
            // for ( GeoSample subSeriesSample : subSeries.getSamples() ) {
            // if ( subSeriesSample.getVariables().size() > 0 ) {
            // // find the corresponding sample in the superseries.
            // for ( GeoSample superSeriesSample : superSeries.getSamples() ) {
            // if ( superSeriesSample.equals( subSeriesSample ) ) {
            // for ( GeoVariable v : subSeriesSample.getVariables() ) {
            // log.info( "Adding variable " + v + " to superseries sample " + superSeriesSample );
            // superSeriesSample.addVariable( v );
            // }
            // break;
            // }
            // }
            // }
            // }
        }
    }

    /**
     * @param rawGEOPlatform
     */
    private void matchToExistingPlatform( GeoConverter geoConverter, GeoPlatform rawGEOPlatform ) {
        // we have to populate this.
        Map<String, String> probeNamesInGemma = rawGEOPlatform.getProbeNamesInGemma();

        // do a partial conversion. We will throw this away;
        ArrayDesign geoArrayDesign = ( ArrayDesign ) geoConverter.convert( rawGEOPlatform );

        // find in our system. Note we only use the short name. The full name can change in GEO, causing trouble.
        ArrayDesign existing = arrayDesignService.findByShortName( geoArrayDesign.getShortName() );

        if ( existing == null ) {
            log.info( rawGEOPlatform + " looks new to Gemma" );
            for ( CompositeSequence cs : geoArrayDesign.getCompositeSequences() ) {
                String geoProbeName = cs.getName();
                probeNamesInGemma.put( geoProbeName, geoProbeName ); // no mapping needed. NB the converter fills
                // this in already, we're just being defensive
                // here.
            }
        } else {
            log.info( "Platform " + rawGEOPlatform
                    + " exists in Gemma, checking for correct probe names and re-matching if necessary ..." );
            existing = arrayDesignService.thaw( existing );

            String columnWithGeoNames = null;
            columnWithGeoNames = getGEOIDColumnName( rawGEOPlatform, geoArrayDesign, columnWithGeoNames );

            getGemmaIDColumnNameInGEO( rawGEOPlatform, existing, columnWithGeoNames );

        }
    }

    /**
     * If platforms used exist in Gemma already, make sure the probe names match the ones in our system, and if not, try
     * to figure out the correct mapping. This is necessary because we sometimes rename probes to match other data
     * sources. Often GEO platforms just have integer ids.
     * 
     * @param series
     */
    private void matchToExistingPlatforms( GeoConverter geoConverter, GeoSeries series ) {
        Set<GeoPlatform> platforms = getPlatforms( series );
        if ( platforms.size() == 0 ) throw new IllegalStateException( "Series has no platforms" );
        for ( GeoPlatform pl : platforms ) {
            matchToExistingPlatform( geoConverter, pl );
        }
    }

    /**
     * Combine data sets that use the same platform into one.
     * 
     * @param datasets, some of which will be combined.
     */
    private Collection<GeoDataset> potentiallyCombineDatasets( Collection<GeoDataset> datasets ) {
        if ( datasets.size() == 1 ) return datasets;
        Map<GeoPlatform, Collection<GeoDataset>> seenPlatforms = new HashMap<GeoPlatform, Collection<GeoDataset>>();
        for ( GeoDataset dataset : datasets ) {
            GeoPlatform platform = dataset.getPlatform();
            if ( !seenPlatforms.containsKey( platform ) ) {
                seenPlatforms.put( platform, new HashSet<GeoDataset>() );
            }
            seenPlatforms.get( platform ).add( dataset );
        }

        Collection<GeoDataset> finishedDatasets = new HashSet<GeoDataset>();
        for ( GeoPlatform platform : seenPlatforms.keySet() ) {
            if ( seenPlatforms.get( platform ).size() > 1 ) {
                GeoDataset combined = combineDatasets( seenPlatforms.get( platform ) );
                finishedDatasets.add( combined );
            }
        }
        return finishedDatasets;

    }

    /**
     * @param entities
     */
    private void updateReports( Collection entities ) {
        Collection<ArrayDesign> adsToUpdate = new HashSet<ArrayDesign>();
        for ( Object entity : entities ) {
            if ( entity instanceof ExpressionExperiment ) {
                ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) entity;

                this.expressionExperimentReportService.generateSummary( expressionExperiment.getId() );

                expressionExperiment = this.expressionExperimentService.thawLite( expressionExperiment );

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
