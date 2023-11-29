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
package ubic.gemma.core.loader.expression.geo;

import ubic.gemma.core.loader.expression.geo.model.GeoData;
import ubic.gemma.core.loader.expression.geo.model.GeoSubset;
import ubic.gemma.core.loader.util.converter.Converter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface GeoConverter extends Converter<GeoData, Object> {

    /**
     * Remove old results. Call this prior to starting conversion of a full dataset.
     */
    void clear();

    @Override
    Collection<Object> convert( Collection<? extends GeoData> geoObjects );

    Collection<Object> convert( Collection<? extends GeoData> geoObjects, boolean skipDataVectors );

    @Override
    Object convert( GeoData geoObject );

    Object convert(GeoData geoObject, boolean skipDataVectors);

    /**
     * Converts Geo subsets to experimental factors. This adds a new factor value to the experimental factor of an
     * experimental design, and adds the factor value to each BioMaterial of a specific BioAssay.
     *
     * @param expExp experiment
     * @param geoSubSet geo subset
     */
    void convertSubsetToExperimentalFactor( ExpressionExperiment expExp, GeoSubset geoSubSet );

    Taxon getPrimaryArrayTaxon( Collection<Taxon> platformTaxa, Collection<String> probeTaxa )
            throws IllegalArgumentException;

    /**
     * @param splitByPlatform If true, and the series uses more than one platform, split it up. This often isn't
     *        necessary/desirable. This is
     *        overridden if the series uses more than one species, in which case it is always split up.
     */
    void setSplitByPlatform( boolean splitByPlatform );

    byte[] convertData( List<Object> vector, QuantitationType qt );

    /**
     * @param forceConvertElements Set the behaviour when a platform that normally would not be loaded in detail is
     *        encountered, such as an Exon
     *        array.
     */
    void setForceConvertElements( boolean forceConvertElements );

    /**
     * @param i this is here for tests only. The default value should be okay otherwise.
     */
    void setElementLimitForStrictness( int i );

}