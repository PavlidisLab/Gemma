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

import org.springframework.stereotype.Service;

import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;

/**
 * Service to handle GPL files.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection fetchAndLoad( String geoPlatformAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean ignored, boolean alsoIgnored ) {
        if ( this.geoDomainObjectGenerator == null ) this.geoDomainObjectGenerator = new GeoDomainObjectGenerator();
        this.geoDomainObjectGenerator.setProcessPlatformsOnly( true );

        /*
         * We do this to get a fresh instantiation of GeoConverter (prototype scope)
         */
        GeoConverter geoConverter = ( GeoConverter ) this.beanFactory.getBean( "geoConverter" );

        Collection<GeoPlatform> platforms = ( Collection<GeoPlatform> ) geoDomainObjectGenerator
                .generate( geoPlatformAccession );
        Collection<Object> arrayDesigns = geoConverter.convert( platforms );
        return persisterHelper.persist( arrayDesigns );
    }

    @Override
    public Collection<?> fetchAndLoad( String geoPlatformAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms,
            boolean allowSuperSeriesImport, boolean allowSubSeriesImport ) {
        return this.fetchAndLoad( geoPlatformAccession, loadPlatformOnly, doSampleMatching,
                aggressiveQuantitationTypeRemoval, splitIncompatiblePlatforms, true, true );
    }

}
