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
package ubic.gemma.core.loader.expression.geo.service;

import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * @author paul
 */
public interface GeoService {

    /**
     * For the rare cases (Exon arrays) where we load the platform in two stages.
     *
     * @param targetPlatform already persistent array design.
     * @return updated (persistent) array design
     */
    @SuppressWarnings("UnusedReturnValue")
    // Better reusability
    ArrayDesign addElements( ArrayDesign targetPlatform );

    /**
     * Load data, no restrictions on superseries or subseries
     *
     * @param doSampleMatching           do sample matching
     * @param geoAccession               accession
     * @param loadPlatformOnly           load platforms only
     * @param splitIncompatiblePlatforms split incompatible platforms
     * @return collection
     */
    Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitIncompatiblePlatforms );

    /**
     * @param allowSuperSeriesImport     Allow loading if the Series is a SuperSeries
     * @param allowSubSeriesImport       Allow loading if the Series is a SubSeries
     * @param doSampleMatching           do sample matching
     * @param geoAccession               accession
     * @param loadPlatformOnly           load platforms only
     * @param splitIncompatiblePlatforms split incompatible platforms
     * @return collection
     */
    Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitIncompatiblePlatforms, boolean allowSuperSeriesImport, boolean allowSubSeriesImport );


    /**
     * Refetch and reprocess the GEO series, updating select information. Currently only implemented for experiments (GSEs)
     * @param geoAccession
     */
    void updateFromGEO( String geoAccession );

    /**
     * This is supplied to allow clients to check that the generator has been set correctly.
     * @return generator
     */
    @SuppressWarnings("unused")
    // Possible external use
    GeoDomainObjectGenerator getGeoDomainObjectGenerator();

    void setGeoDomainObjectGenerator( GeoDomainObjectGenerator generator );

    /**
     * Load from a SOFT file. This can be used for testing but maybe there are other situations it is useful.
     *
     * @param accession e.g GSE1234
     * @param softFile the full path to the SOFT file. The file name has to be [accession].soft.gz
     * @return a single experiment.
     */
    Collection<?> loadFromSoftFile( String accession, String softFile, boolean loadPlatformOnly, boolean doSampleMatching, boolean splitByPlatform  );
}