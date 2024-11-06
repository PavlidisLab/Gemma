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

import ubic.gemma.core.loader.expression.geo.util.GeoUtil;
import ubic.gemma.core.loader.util.fetcher.AbstractFetcher;
import ubic.gemma.core.loader.util.fetcher.FtpFetcher;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author pavlidis
 */
abstract public class GeoFetcher extends FtpFetcher {

    static final String SOFT_GZ = ".soft.gz";

    @Override
    public final void setNetDataSourceUtil() {
        this.netDataSourceUtil = new GeoUtil();
    }

    @Override
    protected final String formLocalFilePath( String identifier, File newDir ) {
        return newDir.getAbsolutePath() + File.separator + identifier + GeoFetcher.SOFT_GZ;
    }

    Collection<File> getFile( String accession, String seekFileName ) {
        File file = this.fetchedFile( seekFileName );
        AbstractFetcher.log.info( "Found " + seekFileName + " for experiment(set) " + accession + "." );
        Collection<File> result = new HashSet<>();
        result.add( file );
        return result;
    }
}
