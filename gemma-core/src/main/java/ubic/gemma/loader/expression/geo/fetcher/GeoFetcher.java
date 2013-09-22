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
package ubic.gemma.loader.expression.geo.fetcher;

import java.io.File;

import ubic.gemma.loader.expression.geo.util.GeoUtil;
import ubic.gemma.loader.util.fetcher.FtpFetcher;

/**
 * @author pavlidis
 * @version $Id$
 */
abstract public class GeoFetcher extends FtpFetcher {
    /**
     * 
     */
    protected static final String SOFT_GZ = ".soft.gz";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.FtpFetcher#setNetDataSourceUtil()
     */
    @Override
    public final void setNetDataSourceUtil() {
        this.netDataSourceUtil = new GeoUtil();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formLocalFilePath(java.lang.String, java.io.File)
     */
    @Override
    protected final String formLocalFilePath( String identifier, File newDir ) {
        return newDir.getAbsolutePath() + File.separator + identifier + SOFT_GZ;
    }
}
