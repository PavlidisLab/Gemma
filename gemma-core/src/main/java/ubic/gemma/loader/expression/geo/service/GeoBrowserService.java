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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ubic.gemma.loader.entrez.EutilFetch;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;

/**
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geoBrowserService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 */
public class GeoBrowserService {
    ExpressionExperimentService expressionExperimentService;
    TaxonService taxonService;
    ExternalDatabaseService externalDatabaseService;

    /**
     * @param start
     * @param count
     * @return
     */
    public List<GeoRecord> getRecentGeoRecords( int start, int count ) {
        GeoBrowser browser = new GeoBrowser();
        List<GeoRecord> records = browser.getRecentGeoRecords( start, count );
        ExternalDatabase geo = externalDatabaseService.find( "GEO" );
        Collection<GeoRecord> toRemove = new HashSet<GeoRecord>();
        assert geo != null;
        rec: for ( GeoRecord record : records ) {

            Collection<String> organisms = record.getOrganisms();
            if ( organisms == null || organisms.size() == 0 ) {
                continue rec;
            }
            int i = 1;
            for ( String string : organisms ) {
                Taxon t = taxonService.findByCommonName( string );
                if ( t == null ) {
                    t = taxonService.findByScientificName( string );
                    if ( t == null ) {
                        toRemove.add( record );
                        continue rec;
                    }
                }
                String acc = record.getGeoAccession();
                if ( organisms.size() > 1 ) {
                    acc = acc + "." + i;
                }
                DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                de.setExternalDatabase( geo );
                de.setAccession( acc );

                ExpressionExperiment ee = expressionExperimentService.findByAccession( de );
                if ( ee != null ) {
                    record.getCorrespondingExperiments().add( ee );
                }

            }
            i++;
        }

        for ( GeoRecord record : toRemove ) {
            records.remove( record );
        }

        return records;
    }

    /**
     * Get details from GEO about an accession.
     * 
     * @param accession
     * @return
     */
    public String getDetails( String accession ) {
        /*
         * The maxrecords is > 1 because it return platforms as well (and there are series with as many as 13 platforms
         * ... leaving some headroom)
         */
        String details = EutilFetch.fetch( "gds", accession, 25 );

        return formatDetails( details );

    }

    /**
     * @param details
     * @return
     */
    private String formatDetails( String details ) {
        /*
         * TODO: parse this into something easier to read /htmlize.
         */
        return details;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

}
