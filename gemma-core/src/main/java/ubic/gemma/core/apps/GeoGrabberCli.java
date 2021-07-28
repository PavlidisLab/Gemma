/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans GEO for ALL experiments that are not in Gemma.
 *
 * @author paul
 */
public class GeoGrabberCli extends AbstractCLIContextCLI {

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "listGEOData";
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws Exception {

    }

    @Override
    protected void doWork() throws Exception {
        Set<String> seen = new HashSet<>();
        GeoBrowserService gbs = this.getBean( GeoBrowserService.class );
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        TaxonService ts = this.getBean( TaxonService.class );
        ArrayDesignService ads = this.getBean( ArrayDesignService.class );

        int start = 0;
        int numfails = 0;
        int chunksize = 100;

        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd" );

        System.out.println( "Acc\tReleaseDate\tTaxa\tPlatforms\tAllPlatformsInGemma\tNumSamples\tType\tSuperSeries\tSubSeriesOf"
                + "\tPubMed\tTitle\tSummary" );

        while ( true ) {
            List<GeoRecord> recs = gbs.searchGeoRecords( null, start, chunksize, true );

            if ( recs.isEmpty() ) {
                AbstractCLI.log.info( "No records received for start=" + start );
                numfails++;

                if ( numfails > 10 ) {
                    AbstractCLI.log.info( "Giving up" );
                    break;
                }

                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException ignored ) {
                }

                start++;
                continue;
            }

            start++;

            for ( GeoRecord geoRecord : recs ) {
                if ( seen.contains( geoRecord.getGeoAccession() ) ) {
                    continue;
                }

                if ( ees.isBlackListed( geoRecord.getGeoAccession() ) ) {
                    continue;
                }

                boolean anyTaxonAcceptable = false;
                for ( String o : geoRecord.getOrganisms() ) {
                    if ( ts.findByScientificName( o ) != null ) {
                        anyTaxonAcceptable = true;
                        break;
                    }
                }

                if ( !geoRecord.getSeriesType().contains( "Expression profiling" ) ) {
                    continue;
                }

                if ( !anyTaxonAcceptable ) {
                    continue;
                }

                if ( ees.findByShortName( geoRecord.getGeoAccession() ) != null ) {
                    continue;
                }

                if ( !ees.findByAccession( geoRecord.getGeoAccession() ).isEmpty() ) {
                    continue;
                }

                boolean platformIsInGemma = true;
                boolean anyNonBlacklistedPlatforms = false;
                String[] platforms = geoRecord.getPlatform().split( ";" );
                for ( String p : platforms ) {
                    if ( ads.findByShortName( p ) == null ) {
                        platformIsInGemma = false;
                        break;
                    }
                    if ( !ees.isBlackListed( p ) ) {
                        anyNonBlacklistedPlatforms = true;
                    }
                }

                // we skip if all the platforms for the GSE are blacklisted
                if ( !anyNonBlacklistedPlatforms ) {
                    continue;
                }

                System.out.println(
                        geoRecord.getGeoAccession()
                                + "\t" + dateFormat.format( geoRecord.getReleaseDate() )
                                + "\t" + StringUtils.join( geoRecord.getOrganisms(), "," )
                                + "\t" + geoRecord.getPlatform()
                                + "\t" + platformIsInGemma
                                + "\t" + geoRecord.getNumSamples()
                                + "\t" + geoRecord.getSeriesType()
                                + "\t" + geoRecord.isSuperSeries()
                                + "\t" + geoRecord.getSubSeriesOf()
                                + "\t" + geoRecord.getPubMedIds()
                                + "\t" + geoRecord.getTitle()
                                + "\t" + geoRecord.getSummary() );
                seen.add( geoRecord.getGeoAccession() );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Grab information on GEO data sets not yet in the system";
    }
}
