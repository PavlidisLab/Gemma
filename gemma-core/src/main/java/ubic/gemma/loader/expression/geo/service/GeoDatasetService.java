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
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoSubset;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
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

    /**
     * 
     */
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
    public Collection fetchAndLoad( String geoAccession ) {
        this.geoConverter.clear();
        geoDomainObjectGenerator.intialize();
        geoDomainObjectGenerator.setProcessPlatformsOnly( this.loadPlatformOnly );

        Collection<DatabaseEntry> projectedAccessions = geoDomainObjectGenerator.getProjectedAccessions( geoAccession );
        checkForExisting( projectedAccessions );

        if ( this.loadPlatformOnly ) {
            Collection<?> platforms = geoDomainObjectGenerator.generate( geoAccession );
            Collection<Object> arrayDesigns = geoConverter.convert( platforms );
            return persisterHelper.persist( arrayDesigns );
        }

        Collection<?> results = geoDomainObjectGenerator.generate( geoAccession );

        if ( results == null || results.size() == 0 ) {
            throw new RuntimeException( "Could not get domain objects for " + geoAccession );
        }

        Object obj = results.iterator().next();
        if ( !( obj instanceof GeoSeries ) ) {
            throw new RuntimeException( "Got a " + obj.getClass().getName() + " instead of a "
                    + GeoSeries.class.getName() + " (you may need to load platforms only)." );
        }

        GeoSeries series = ( GeoSeries ) obj;

        checkPlatformUniqueness( series );

        checkSamplesAreNew( series );

        log.info( "Generated GEO domain objects for " + geoAccession );

        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series );

        getPubMedInfo( result );

        log.info( "Converted " + series.getGeoAccession() );
        assert persisterHelper != null;
        Collection persistedResult = persisterHelper.persist( result );
        log.info( "Persisted " + series.getGeoAccession() );
        this.geoConverter.clear();
        return persistedResult;
    }

    /**
     * Another rare case, typified by samples in GSE3193. We must confirm that all samples included in the data set are
     * not included in other data sets.
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

        // log.info( series.getSampleCorrespondence() );

    }

    /**
     * @param result
     */
    private void getPubMedInfo( Collection<ExpressionExperiment> result ) {
        for ( ExpressionExperiment experiment : result ) {
            BibliographicReference pubmed = experiment.getPrimaryPublication();
            if ( pubmed == null ) continue;
            PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
            pubmed = fetcher.retrieveByHTTP( Integer.parseInt( pubmed.getPubAccession().getAccession() ) );
            if ( pubmed == null ) continue;
            experiment.setPrimaryPublication( pubmed );

        }
    }

    /**
     * Check if all the data sets are on different platforms. This is a rare case in GEO. The right thing to do would be
     * to merge the data sets.
     */
    private void checkPlatformUniqueness( GeoSeries series ) {
        Set<GeoPlatform> platforms = new HashSet<GeoPlatform>();
        for ( GeoDataset dataset : series.getDatasets() ) {
            platforms.add( dataset.getPlatform() );
        }
        if ( platforms.size() != series.getDatasets().size() ) {
            throw new UnsupportedOperationException(
                    "Some of the data sets use the same platform, this is not currently supported." );
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
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

}
