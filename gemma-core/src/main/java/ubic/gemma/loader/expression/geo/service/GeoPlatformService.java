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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;

/**
 * Service to handle GPL files.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoPlatformService extends AbstractGeoService {

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download and parse GPL file</li>
     * <li>Convert the GPL into an ArrayDesign (sample information in the file is ignored</li>
     * <li>Load the resulting ArrayDesign into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     * @deprecated Use {@link #fetchAndLoad(String,boolean,boolean,boolean,boolean,boolean)} instead
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection fetchAndLoad( String geoPlatformAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean ignored, boolean alsoIgnored ) {
                return fetchAndLoad( geoPlatformAccession, loadPlatformOnly, doSampleMatching, ignored, alsoIgnored,
                        true );
            }

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download and parse GPL file</li>
     * <li>Convert the GPL into an ArrayDesign (sample information in the file is ignored</li>
     * <li>Load the resulting ArrayDesign into Gemma</li>
     * </ol>
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection fetchAndLoad( String geoPlatformAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean ignored, boolean alsoIgnored, boolean allowSuperSeriesImport ) {
        if ( this.geoDomainObjectGenerator == null ) this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        this.geoDomainObjectGenerator.setProcessPlatformsOnly( true );

        Collection<GeoPlatform> platforms = ( Collection<GeoPlatform> ) geoDomainObjectGenerator
                .generate( geoPlatformAccession );
        Collection<Object> arrayDesigns = geoConverter.convert( platforms );
        return persisterHelper.persist( arrayDesigns );
    }

}
