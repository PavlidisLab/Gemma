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
package ubic.gemma.core.loader.expression.geo.fetcher;

import java.io.File;
import java.util.Collection;

/**
 * A fetcher that only looks locally for "family" files (GPLXXX_family, GSEXXX_family).
 *
 * @author pavlidis
 */
public class LocalSeriesFetcher extends SeriesFetcher {

    private final String localPath;

    public LocalSeriesFetcher( String localPath ) {
        super();
        this.localPath = localPath;
    }

    @Override
    public Collection<File> fetch( String accession ) {
        log.info( "Seeking GSE  file for " + accession );
        assert localPath != null;
        String seekFileName = localPath + File.separatorChar + accession + "_family.soft.gz";
        File seekFile = new File( seekFileName );

        if ( seekFile.canRead() ) {
            return getFile( accession, seekFileName );
        }

        // try alternative naming scheme.
        String altSeekFileName = localPath + File.separatorChar + accession + ".soft.gz";
        seekFile = new File( altSeekFileName );

        if ( seekFile.canRead() ) {
            return getFile( accession, altSeekFileName );
        }

        throw new RuntimeException(
                "Failed to find file for " + accession + "; Checked for " + seekFileName + " and " + altSeekFileName );
    }

}
