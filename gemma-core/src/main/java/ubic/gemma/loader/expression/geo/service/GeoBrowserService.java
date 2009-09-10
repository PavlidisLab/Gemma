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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.entrez.EutilFetch;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
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
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 */
public class GeoBrowserService {
    private static final int MIN_SAMPLES = 5;
    ExpressionExperimentService expressionExperimentService;
    TaxonService taxonService;
    ExternalDatabaseService externalDatabaseService;
    ArrayDesignService arrayDesignService;

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    BioAssayService bioAssayService;

    private static Log log = LogFactory.getLog( GeoBrowserService.class.getName() );

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

            if ( record.getNumSamples() < MIN_SAMPLES ) {
                toRemove.add( record );
            }

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
     * Take the details string from GEO and make it nice. Add links to series and platforms that are already in gemma.
     * 
     * @param details
     * @return
     */
    private String formatDetails( String details ) {

        Pattern accPatt = Pattern.compile( "(G(PL|SE|SM|DS)[0-9]+)" );
        Matcher matcher = accPatt.matcher( details );
        details = matcher.replaceAll( "<br /><strong>$1</strong>" );
        matcher.reset();
        boolean result = matcher.find();
        if ( result ) {
            StringBuffer sb = new StringBuffer();
            do {

                String match = matcher.group();

                if ( match.startsWith( "GPL" ) ) {
                    ArrayDesign arrayDesign = arrayDesignService.findByShortName( match );

                    if ( arrayDesign != null ) {
                        matcher.appendReplacement( sb,
                                "<br />\n<strong><a href=\"/Gemma/arrays/showArrayDesign.html?id="
                                        + arrayDesign.getId() + "\">" + match + "</a></strong>" );
                    } else {
                        matcher.appendReplacement( sb, "<br />\n<strong>$1 [New to Gemma]</strong>" );
                    }

                } else if ( match.startsWith( "GSM" ) ) {

                    /*
                     * TODO: check if sample is already in the system. NOte that we only get a partial list of the
                     * samples anyway.
                     */

                    matcher.appendReplacement( sb, "<br />\n<strong>$1</strong>" );
                } else if ( match.startsWith( "GSE" ) ) {
                    ExpressionExperiment ee = this.expressionExperimentService.findByShortName( match );

                    if ( ee != null ) {
                        matcher.appendReplacement( sb,
                                "<br />\n<strong><a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + ee.getId() + "\">" + match + "</a></strong>" );
                    } else {
                        matcher.appendReplacement( sb, "<br />\n<strong>$1 [new to Gemma]</strong>" );
                    }
                } else {
                    matcher.appendReplacement( sb, "<br />\n<strong>$1</strong>" );
                }

                result = matcher.find();
            } while ( result );

            matcher.appendTail( sb );

            details = sb.toString();
        }

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
