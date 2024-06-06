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

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.config.Settings;

/**
 * Fetch GEO "GPLXXX_family.soft.gz" files
 *
 * @author pavlidis
 */
public class PlatformFetcher extends GeoFetcher {

    @Override
    protected String formRemoteFilePath( String identifier ) {
        String idroot = identifier.replaceFirst( "(GPL[0-9]*?)[0-9]{1,3}$", "$1nnn" );
        return remoteBaseDir + "/" + idroot + "/" + identifier + "/soft/" + identifier + "_family.soft.gz";
    }

    @Override
    protected void initConfig() {
        this.localBasePath = Settings.getString( "geo.local.datafile.basepath" );
        this.remoteBaseDir = Settings.getString( "geo.remote.platformDir" );
        if ( StringUtils.isBlank( remoteBaseDir ) ) {
            throw new RuntimeException(
                    new ConfigurationException( "geo.remote.platformDir was not defined in resource bundle" ) );
        }

    }

}
