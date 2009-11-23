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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.loader.entrez.EutilFetch;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
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
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
@Service
public class GeoBrowserService implements InitializingBean {
    private static final int MIN_SAMPLES = 5;
    private static final String GEO_DATA_STORE_FILE_NAME = "GEODataStore";
    @Autowired
    ExpressionExperimentService expressionExperimentService;
    @Autowired
    TaxonService taxonService;
    @Autowired
    ExternalDatabaseService externalDatabaseService;
    @Autowired
    ArrayDesignService arrayDesignService;
    @Autowired
    AuditTrailService auditTrailService;

    @Autowired
    BioAssayService bioAssayService;

    Map<String, GeoRecord> localInfo;

    private static Log log = LogFactory.getLog( GeoBrowserService.class.getName() );

    public void afterPropertiesSet() throws Exception {
        initializeLocalInfo();
    }

    /**
     * Take the details string from GEO and make it nice. Add links to series and platforms that are already in gemma.
     * 
     * @param details
     * @return
     */
    private String formatDetails( String details ) {

        // log.info( "====\n" + details + "\n======" );

        /*
         * Remove redundant information about the series that is listed with the dataset.
         */
        Pattern refPattern = Pattern.compile( "(Reference Series: GSE.+)(?=GSE)" );
        Matcher m = refPattern.matcher( details );
        details = m.replaceAll( "" );

        details = details.replaceFirst( "(Samples: [0-9]+)", "<p>$1&nbsp&nbsp" );

        // replace 1: ; leave GSM12114: alone.
        Pattern recordPattern = Pattern.compile( "(?<!GSM)(?<![0-9])[0-9]+:" );
        m = recordPattern.matcher( details );
        details = m.replaceAll( "" );

        Pattern accPatt = Pattern.compile( "(?<!Parent Platform: )(?<!accessioned in GEO as )(G(PL|SE|SM|DS)[0-9]+)" );
        Matcher matcher = accPatt.matcher( details );

        boolean result = matcher.find();
        if ( result ) {
            StringBuffer sb = new StringBuffer();
            sb.append( "<div class=\"small\">" );
            do {

                String match = matcher.group();

                if ( match.startsWith( "GPL" ) ) {
                    ArrayDesign arrayDesign = arrayDesignService.findByShortName( match );

                    if ( arrayDesign != null ) {

                        String trouble = "";
                        AuditEvent lastTroubleEvent = auditTrailService.getLastTroubleEvent( arrayDesign );
                        if ( lastTroubleEvent != null ) {
                            trouble = "&nbsp;<img src='/Gemma/images/icons/warning.png' height='16' width='16' alt=\"troubled\" title=\""
                                    + lastTroubleEvent.getNote() + "\"/>";
                        }

                        matcher.appendReplacement( sb,
                                "<p><strong><a target=\"_blank\" href=\"/Gemma/arrays/showArrayDesign.html?id="
                                        + arrayDesign.getId() + "\">" + match + "</a></strong>" + trouble );
                    } else {
                        matcher.appendReplacement( sb, "<p><strong>$1 [New to Gemma]</strong>" );
                    }

                } else if ( match.startsWith( "GSM" ) ) {

                    /*
                     * TODO: perhaps check if sample is already in the system. NOte that we only get a partial list of
                     * the samples anyway.
                     */

                    matcher.appendReplacement( sb, "\n&nbsp;&nbsp;&nbsp;$1" );
                } else if ( match.startsWith( "GSE" ) ) {
                    ExpressionExperiment ee = this.expressionExperimentService.findByShortName( match );

                    if ( ee != null ) {
                        matcher.appendReplacement( sb,
                                "\n<p><strong><a target=\"_blank\" href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + ee.getId() + "\">" + match + "</a></strong>" );
                    } else {
                        matcher.appendReplacement( sb, "\n<p><strong>$1 [new to Gemma]</strong>" );
                    }
                } else {
                    // GDS, etc.
                    matcher.appendReplacement( sb, "\n<p><strong>$1</strong>" );
                }

                result = matcher.find();
            } while ( result );

            matcher.appendTail( sb );
            sb.append( "</div>" );
            details = sb.toString();

        }

        return details;
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

        initLocalRecord( accession );

        /*
         * increment click counts.
         */
        localInfo.get( accession ).setPreviousClicks( localInfo.get( accession ).getPreviousClicks() + 1 );

        saveLocalInfo();

        return formatDetails( details );

    }

    private File getInfoStoreFile() {
        String path = ConfigUtils.getDownloadPath();
        File f = new File( path + File.separatorChar + GEO_DATA_STORE_FILE_NAME );
        return f;
    }

    /**
     * @param start
     * @param count
     * @return
     * @throws IOException
     */
    public List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException {
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
                    record.getCorrespondingExperiments().add( ee.getId() );
                }

                record.setPreviousClicks( localInfo.containsKey( acc ) ? localInfo.get( acc ).getPreviousClicks() : 0 );

                record.setUsable( localInfo.containsKey( acc ) ? localInfo.get( acc ).isUsable() : true );

            }
            i++;
        }

        for ( GeoRecord record : toRemove ) {
            records.remove( record );
        }

        return records;
    }

    @SuppressWarnings("unchecked")
    private void initializeLocalInfo() {
        File f = getInfoStoreFile();
        try {
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                this.localInfo = ( Map<String, GeoRecord> ) ois.readObject();
                ois.close();
                fis.close();
            } else {
                this.localInfo = new HashMap<String, GeoRecord>();
            }
        } catch ( Exception e ) {
            log.error( "Failed to load local GEO info, reinitializing..." );
            this.localInfo = new HashMap<String, GeoRecord>();
        }
        assert this.localInfo != null;
    }

    private void initLocalRecord( String accession ) {
        assert localInfo != null;
        if ( !localInfo.containsKey( accession ) ) {
            localInfo.put( accession, new GeoRecord() );
            localInfo.get( accession ).setGeoAccession( accession );
        }
    }

    private void saveLocalInfo() {
        if ( this.localInfo == null ) return;
        try {
            FileOutputStream fos = new FileOutputStream( getInfoStoreFile() );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( this.localInfo );
            oos.flush();
            oos.close();
        } catch ( Exception e ) {
            log.error( "Failed to save local GEO info", e );
        }
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
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

    /**
     * @param accession
     * @param currentState
     */
    public boolean toggleUsability( String accession ) {

        initLocalRecord( accession );

        localInfo.get( accession ).setUsable( !localInfo.get( accession ).isUsable() );

        saveLocalInfo();

        return localInfo.get( accession ).isUsable();
    }

}
