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

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
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

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geoDatasetService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 */
public class GeoDatasetService extends AbstractGeoService {

    private static final String GEO_DB_NAME = "GEO";
    ExpressionExperimentService expressionExperimentService;
    BioAssayService bioAssayService;

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
            boolean aggressiveQuantitationTypeRemoval ) {
        this.geoConverter.clear();
        geoDomainObjectGenerator.intialize();
        geoDomainObjectGenerator.setProcessPlatformsOnly( loadPlatformOnly );
        geoDomainObjectGenerator.setDoSampleMatching( doSampleMatching );
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

        confirmPlatformUniqueness( series, doSampleMatching );

        matchToExistingPlatforms( series );

        checkSamplesAreNew( series );

        geoConverter.clear();

        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series );

        series = null; // hopefully free memory...
        parseResult = null;

        getPubMedInfo( result );

        log.debug( "Converted " + seriesAccession );
        assert persisterHelper != null;
        Collection persistedResult = persisterHelper.persist( result );
        log.debug( "Persisted " + seriesAccession );
        this.geoConverter.clear();
        return persistedResult;
    }

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param projectedAccessions
     */
    private void checkForExisting( Collection<DatabaseEntry> projectedAccessions ) {
        if ( projectedAccessions == null || projectedAccessions.size() == 0 ) {
            return; // that's okay, it might have been a GPL.
        }
        for ( DatabaseEntry entry : projectedAccessions ) {
            ExpressionExperiment existing = expressionExperimentService.findByAccession( entry );
            if ( existing != null ) {
                String message = "There is already an expression experiment that matches " + entry.getAccession()
                        + ", " + existing.getName();
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
    @SuppressWarnings("unchecked")
    private void checkSamplesAreNew( GeoSeries series ) {
        Collection<BioAssay> bioAssays = null;
        Collection<GeoSample> toSkip = new HashSet<GeoSample>();
        for ( GeoSample sample : series.getSamples() ) {
            if ( sample.appearsInMultipleSeries() ) {
                String sampleId = sample.getGeoAccession();

                if ( bioAssays == null ) {
                    log.info( "Loading all bioassays to check for duplication..." );
                    bioAssays = bioAssayService.loadAll();
                }

                for ( BioAssay ba : bioAssays ) {
                    DatabaseEntry acc = ba.getAccession();
                    if ( acc == null ) continue;

                    String existingAcc = acc.getAccession();
                    if ( existingAcc.equals( sampleId )
                            && ba.getAccession().getExternalDatabase().getName().equals( GEO_DB_NAME ) ) {
                        log.info( sampleId + " appears in an expression experiment already in the system, skipping" );
                        toSkip.add( sample );
                    }
                }
            }
        }

        StringBuilder buf = new StringBuilder();
        for ( GeoSample gs : toSkip ) {
            series.getSamples().remove( gs );
            series.getSampleCorrespondence().removeSample( gs.getGeoAccession() );
            buf.append( gs + ", " );
        }

        for ( GeoDataset gds : series.getDatasets() ) {
            for ( GeoSubset gsub : gds.getSubsets() ) {
                for ( GeoSample gs : toSkip ) {
                    gsub.getSamples().remove( gs );
                }
            }
        }

        log.info( "Series now contains " + series.getSamples().size() + " (removed " + toSkip.size() + ")" );

        // update the description, so we keep some kind of record.
        if ( toSkip.size() > 0 ) {
            series.setSummaries( series.getSummaries() + "\nNote: " + toSkip.size()
                    + " samples from this series, which appear in other Expression Experiments in Gemma, "
                    + "were not imported from the GEO source. The following samples were removed: " + buf.toString() );
        }

        if ( series.getSamples().size() == 0 ) {
            throw new AlreadyExistsInSystemException( "All the samples in " + series
                    + " are in the system already (in other ExpressionExperiments" );
        }
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
     * Build the map between the existing probe names and the ones in gemma.
     * 
     * @param pl
     * @param columnWithGemmaNames
     * @param columnWithGeoNames
     */
    private void fillExistingProbeNameMap( GeoPlatform pl, String columnWithGemmaNames, String columnWithGeoNames ) {

        List<String> gemmaNames = pl.getColumnData( columnWithGemmaNames );
        List<String> geoNames = pl.getColumnData( columnWithGeoNames );
        assert gemmaNames.size() == geoNames.size();
        for ( int i = 0; i < gemmaNames.size(); i++ ) {
            String gemmaName = gemmaNames.get( i );
            String geoName = geoNames.get( i );
            if ( log.isDebugEnabled() )
                log.debug( "GEO name:" + geoName + "; name to be used for Gemma:" + gemmaName );
            pl.getProbeNamesInGemma().put( geoName, gemmaName );
        }
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
                platforms.addAll( sample.getPlatforms() );
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
     * @param pl
     */
    private void matchToExistingPlatform( GeoPlatform pl ) {
        // we have to populate this.
        Map<String, String> probeNamesInGemma = pl.getProbeNamesInGemma();

        // do a partial conversion. We will throw this away;
        ArrayDesign rawad = ( ArrayDesign ) geoConverter.convert( pl );

        // find in our system
        ArrayDesign existing = arrayDesignService.find( rawad );

        if ( existing == null ) {
            log.info( pl + " looks new to Gemma" );
            for ( CompositeSequence cs : rawad.getCompositeSequences() ) {
                String geoProbeName = cs.getName();
                probeNamesInGemma.put( geoProbeName, geoProbeName ); // no mapping needed. NB the converter fills
                // this in already, we're just being defensive
                // here.
            }
        } else {
            log.info( "Platform " + pl + " exists in Gemma, aligning ..." );
            arrayDesignService.thawLite( existing );

            String columnWithGeoNames = null;
            Set<String> geoProbeNames = new HashSet<String>();
            for ( CompositeSequence cs : rawad.getCompositeSequences() ) {
                String geoProbeName = cs.getName();
                geoProbeNames.add( geoProbeName );
                if ( columnWithGeoNames == null ) {
                    for ( String colName : pl.getColumnNames() ) {
                        if ( pl.getColumnData( colName ).contains( geoProbeName ) ) {
                            columnWithGeoNames = colName;
                            log.info( "GEO probe names were found in GEO column=" + columnWithGeoNames );
                            break;
                        }
                    }
                }
            }

            if ( columnWithGeoNames == null ) {
                throw new IllegalStateException( "Could not figure out which column the GEO probe names came from!" );
            }

            String columnWithGemmaNames = null;
            allofit: for ( CompositeSequence cs : existing.getCompositeSequences() ) {
                String gemmaProbeName = cs.getName();
                // search the other columns
                for ( String colName : pl.getColumnNames() ) {
                    if ( pl.getColumnData( colName ).contains( gemmaProbeName ) ) {
                        columnWithGemmaNames = colName;
                        log.info( "Gemma probe names were found in GEO column=" + columnWithGemmaNames );

                        fillExistingProbeNameMap( pl, columnWithGemmaNames, columnWithGeoNames );
                        break allofit;
                    }
                }
            }

            if ( columnWithGemmaNames == null ) {
                throw new IllegalStateException(
                        "Could not figure out which column the Gemma probe names came from for platform=" + pl );
            }

        }
    }

    /**
     * If platforms used exist in Gemma already, make sure the probe names match the ones in our system, and if not, try
     * to figure out the correct mapping. This is necessary because we sometimes rename probes to match other data
     * sources. Often GEO platforms just have integer ids.
     * 
     * @param series
     */
    private void matchToExistingPlatforms( GeoSeries series ) {
        Set<GeoPlatform> platforms = getPlatforms( series );
        if ( platforms.size() == 0 ) throw new IllegalStateException( "Series has no platforms" );
        for ( GeoPlatform pl : platforms ) {
            matchToExistingPlatform( pl );
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

}
