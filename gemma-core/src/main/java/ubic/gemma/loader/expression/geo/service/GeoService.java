/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;

/**
 * @author paul
 * @version $Id$
 */
public interface GeoService {

    /**
     * @param geoAccession
     * @param loadPlatformOnly
     * @param doSampleMatching
     * @param aggressiveQuantitationTypeRemoval
     * @param splitIncompatiblePlatforms
     * @param allowSuperSeriesImport Allow loading if the Series is a SuperSeries
     * @param allowSubSeriesImport Allow loading if the Series is a SubSeries
     * @return
     */
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms,
            boolean allowSuperSeriesImport, boolean allowSubSeriesImport );

    /**
     * Load data, no restrictions on superseries or subseries
     * 
     * @param geoAccession
     * @param loadPlatformOnly
     * @param doSampleMatching
     * @param aggressiveQuantitationTypeRemoval
     * @param splitIncompatiblePlatforms
     * @return
     */
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms );

    /**
     * This is supplied to allow clients to check that the generator has been set correctly.
     * 
     * @return
     */
    public abstract GeoDomainObjectGenerator getGeoDomainObjectGenerator();

    public abstract void setGeoDomainObjectGenerator( GeoDomainObjectGenerator generator );

}