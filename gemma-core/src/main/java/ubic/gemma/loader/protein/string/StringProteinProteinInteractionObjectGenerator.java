/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein.string;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.loader.util.fetcher.HttpArchiveFetcherInterface;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Taxon;

/**
 * Handle fetching and parsing of string protein files. These string files can be processed either from local or remote
 * files. If the file is remote then the StringProteinProteinInteractionFileFetcher is called. The file path name of the
 * remote file can be provided by the user or the default used as configured in the properties file of the fetcher.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinProteinInteractionObjectGenerator {

    protected HttpArchiveFetcherInterface stringProteinFileFetcher;

    protected StringProteinProteinInteractionFileParser stringProteinInteractionParser;

    /** File to parse on local system */
    private File stringProteinInteractionFileLocal = null;

    /** File to retrieve from string remote site */
    private String stringProteinInteractionFileRemote = null;

    private static Log log = LogFactory.getLog( StringProteinProteinInteractionObjectGenerator.class );

    /**
     * Constructor that sets the string file to process whether local or remote. Also ensures fetcher set (this is
     * needed even for ). Provide either a local or remote path (?)
     * 
     * @param stringProteinInteractionFileLocal Name of local file to process
     * @param stringProteinInteractionFileRemote Name of remote file to process
     */
    public StringProteinProteinInteractionObjectGenerator( File stringProteinInteractionFileLocal,
            String stringProteinInteractionFileRemote ) {
        stringProteinFileFetcher = new StringProteinFileFetcher();
        this.setStringProteinInteractionFileLocal( stringProteinInteractionFileLocal );
        this.setStringProteinInteractionFileRemote( stringProteinInteractionFileRemote );
    }

    /**
     * Main method to call to generate StringProteinProteinInteraction objects. If the file is remote fetch it and
     * unarchive it and then set the local file stringProteinInteractionFileLocal to the newly downloaded file.
     * 
     * @param validTaxa Taxon to generate StringProteinProteinInteraction from string (String has many taxon).
     * @return Collection of StringProteinProteinInteraction objects specific for the taxa that were provided, held in a
     *         may keyed on taxon.
     */
    public Map<Taxon, Collection<StringProteinProteinInteraction>> generate( Collection<Taxon> validTaxa ) {

        log.debug( "Starting to get StringProteinProteinInteraction data" );
        Collection<StringProteinProteinInteraction> stringProteinProteinInteractions = null;

        if ( stringProteinInteractionFileLocal == null ) {
            log.info( "stringProteinInteractionFile is remote file fetching remote site" );
            fetchProteinStringFileFromRemoteSiteUnArchived();
        }

        Map<Taxon, Collection<StringProteinProteinInteraction>> map = new HashMap<Taxon, Collection<StringProteinProteinInteraction>>();

        // this is a bit ugly as reads string file for every taxon
        // however when I did it in one big go I got java.lang.OutOfMemoryError: Java heap space
        for ( Taxon taxon : validTaxa ) {
            log.info( "calling taxon " + taxon );
            Collection<Taxon> taxa = new ArrayList<Taxon>();
            taxa.add( taxon );
            stringProteinProteinInteractions = this.parseProteinStringFileInteraction( taxa );
            map.put( taxon, stringProteinProteinInteractions );
        }

        log.debug( "Starting to get StringProteinProteinInteraction data" );
        return map;
    }

    /**
     * Fetches files from remote string site and unpacks the file.
     * 
     * @throws Exception
     */
    public void fetchProteinStringFileFromRemoteSiteUnArchived() {
        // the file can be null the fetcher determines that
        Collection<LocalFile> stringProteinInteractionFileArchived = stringProteinFileFetcher
                .fetch( stringProteinInteractionFileRemote );
        // set the string protein interaction local file
        this.setStringProteinInteractionFileLocal( stringProteinFileFetcher
                .unPackFile( stringProteinInteractionFileArchived ) );

    }

    /**
     * Parse the downloaded file, selecting those taxa that are supported in gemma and returning the value objects
     * StringProteinProteinInteraction
     * 
     * @param taxa Taxa to find records for.
     * @return StringProteinProteinInteraction representing lines in the string file.
     */
    public Collection<StringProteinProteinInteraction> parseProteinStringFileInteraction( Collection<Taxon> taxa ) {
        try {
            stringProteinInteractionParser = new StringProteinProteinInteractionFileParser();
            stringProteinInteractionParser.setTaxa( taxa );
            stringProteinInteractionParser.parse( stringProteinInteractionFileLocal );
            return stringProteinInteractionParser.getResults();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @return the stringProteinInteractionFileRemote
     */
    public String getStringProteinInteractionFileRemote() {
        return stringProteinInteractionFileRemote;
    }

    /**
     * @param stringProteinInteractionFileRemote the stringProteinInteractionFileRemote to set
     */
    public void setStringProteinInteractionFileRemote( String stringProteinInteractionFileRemote ) {
        this.stringProteinInteractionFileRemote = stringProteinInteractionFileRemote;
    }

    /**
     * @return the stringProteinInteractionFileLocal
     */
    public File getStringProteinInteractionFileLocal() {
        return stringProteinInteractionFileLocal;
    }

    /**
     * @param stringProteinInteractionFileLocal the stringProteinInteractionFileLocal to set
     */
    public void setStringProteinInteractionFileLocal( File stringProteinInteractionFileLocal ) {
        this.stringProteinInteractionFileLocal = stringProteinInteractionFileLocal;
    }

    /**
     * @return the stringProteinFileFetcher
     */
    public HttpArchiveFetcherInterface getStringProteinFileFetcher() {
        return stringProteinFileFetcher;
    }

    /**
     * @param stringProteinFileFetcher the stringProteinFileFetcher to set
     */
    public void setStringProteinFileFetcher( HttpArchiveFetcherInterface stringProteinFileFetcher ) {
        this.stringProteinFileFetcher = stringProteinFileFetcher;
    }

}
