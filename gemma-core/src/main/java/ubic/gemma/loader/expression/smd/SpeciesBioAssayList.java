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
package ubic.gemma.loader.expression.smd;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import ubic.gemma.loader.expression.smd.util.SmdUtil;
import ubic.gemma.util.ConfigUtils;

/**
 * The list of all BioAssays (SMD "experiments") for a given species.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SpeciesBioAssayList {

    protected static final Log log = LogFactory.getLog( SpeciesBioAssayList.class );
    private String baseDir = "smd/organisms/";
    private SMDSpeciesMapper speciesMap;
    private Set<String> bioAssays;
    private FTPClient f;

    /**
     * @param species
     * @throws IOException
     */
    public SpeciesBioAssayList() {
        speciesMap = new SMDSpeciesMapper();
        bioAssays = new HashSet<String>();
        baseDir = ( String ) ConfigUtils.getProperty( "smd.organism.baseDir" );
    }

    /**
     * Retrieve the list of experiments (bioassays) for a given species. The species can be something like "human" or
     * "Homo sapiens".
     * 
     * @param species
     * @throws IOException
     */
    public void retrieveByFTP( String species ) throws IOException {
        if ( !f.isConnected() ) f = ( new SmdUtil() ).connect( FTP.ASCII_FILE_TYPE );

        String work = baseDir + speciesMap.getCode( species );
        FTPFile[] files = f.listFiles( work );

        for ( int i = 0; i < files.length; i++ ) {
            if ( !files[i].isDirectory() ) {
                String name = files[i].getName();
                name = name.replaceAll( "\\.xls\\.gz", "" );
                bioAssays.add( name );
            }
        }
        log.info( bioAssays.size() + " experiments found for " + species );
        f.disconnect();
    }

    /**
     * @return
     */
    public Set<String> getBioAssays() {
        return bioAssays;
    }

    /**
     * @param experiments
     */
    public void setBioAssays( Set<String> experiments ) {
        this.bioAssays = experiments;
    }

    public static void main( String[] args ) {
        try {
            SpeciesBioAssayList foo = new SpeciesBioAssayList();
            foo.retrieveByFTP( "human" );
            log.debug( foo.getBioAssays() );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}