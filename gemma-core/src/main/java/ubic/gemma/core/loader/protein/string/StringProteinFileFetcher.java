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
package ubic.gemma.core.loader.protein.string;

import org.apache.commons.configuration.ConfigurationException;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.util.fetcher.AbstractFetcher;
import ubic.gemma.core.loader.util.fetcher.HttpArchiveFetcherInterface;
import ubic.gemma.core.loader.util.fetcher.HttpFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Downloads protein interaction Files from from the STRING website. STRING makes available gzipped files of data, which
 * can be downloaded from <a href="http://string.embl.de/newstring_cgi/show_download_page.pl">the STRING download
 * page</a>
 * The particular file that contains protein protein interaction data can be downloaded from (for example):
 * <a href="http://string.embl.de/newstring_download/protein.links.detailed.v8.2.txt.gz">Protein Links Detailed file</a>
 * Care must be taken to ensure the file name does not change it is version dependent.
 *
 * @author ldonnison
 */
@Deprecated
public class StringProteinFileFetcher extends HttpFetcher implements HttpArchiveFetcherInterface {

    private final static String INTERACTION = "protein.string.linksdetailed.remotepath";
    /**
     * Name of string protein file to download
     */
    private String stringProteinFileName = null;

    /**
     * If the full path to the string file to download is known then this method is called and will download the given
     * file name. If the file is not provided then the file name is retrieved from the properties file.
     *
     * @param stringProteinFileNameToFetch The full path name to the string file to retrieve
     * @return Collection of local files detailing the files downloaded.
     */
    @Override
    public Collection<LocalFile> fetch( String stringProteinFileNameToFetch ) {
        if ( stringProteinFileNameToFetch == null ) {
            this.initConfig();
        } else {
            this.setStringProteinLinksDetailedFileName( stringProteinFileNameToFetch );
        }

        AbstractFetcher.log.info( "Starting download of protein STRING File at " + stringProteinFileName );
        return super.fetch( stringProteinFileName );
    }

    /**
     * Sets the paths of the remote files to download as set in the project properties files
     *
     * @throws RuntimeException (ConfigurationException) one of the file download paths in the properties file was not configured
     *                          correctly.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    @Override
    public void initConfig() {
        stringProteinFileName = Settings.getString( StringProteinFileFetcher.INTERACTION );
        if ( stringProteinFileName == null || stringProteinFileName.length() == 0 )
            throw new RuntimeException(
                    new ConfigurationException( StringProteinFileFetcher.INTERACTION + " was null or empty" ) );
    }

    /**
     * This is optional a specific file name can be set for download. This is to make the code more robust as not sure
     * how often string updates its file names on its download page.
     *
     * @param stringProteinFileName The full path of the file to download
     */
    public void setStringProteinLinksDetailedFileName( String stringProteinFileName ) {
        this.stringProteinFileName = stringProteinFileName;
    }

    /**
     * Method to un-archive downloaded file.
     *
     * @param localFile Collection of File details relating to string download
     */
    @Override
    public File unPackFile( Collection<LocalFile> localFile ) {
        File stringFile = null;
        for ( LocalFile file : localFile ) {
            String localFileName = file.getLocalURL().getFile();
            try {
                FileTools.unGzipFile( file.getLocalURL().getFile() );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
            stringFile = new File( FileTools.chompExtension( localFileName ) );
            // test file there
            if ( !stringFile.canRead() ) {
                throw new RuntimeException( "Problem unpacking file: not readable: " + stringFile.getName() );
            }
        }
        return stringFile;
    }
}
