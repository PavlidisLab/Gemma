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

import ubic.gemma.core.loader.expression.geo.model.*;
import ubic.gemma.core.loader.util.converter.Converter;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author paul
 */
public interface GeoConverter extends Converter<GeoData, Identifiable> {

    /**
     * Clear the state of the converter.
     * <p>
     * Call this prior to starting conversion of a full dataset.
     */
    void clear();

    @Override
    Collection<Identifiable> convert( Collection<? extends GeoData> geoObjects );

    /**
     * Convert a collection of GeoData objects, retaining only elements of the specified data type.
     */
    <T extends Identifiable> Collection<T> convert( Collection<? extends GeoData> geoObjects, Class<T> dataType );

    @Override
    Identifiable convert( GeoData geoObject );

    ArrayDesign convert( GeoPlatform geoPlatform );

    Collection<ExpressionExperiment> convert( GeoSeries geoSeries );

    Collection<ExpressionExperiment> convert( GeoSeries geoSeries, boolean skipDataVectors );

    ExpressionExperiment convert( GeoDataset geoDataset, boolean skipDataVectors );

    /**
     * Converts Geo subsets to experimental factors. This adds a new factor value to the experimental factor of an
     * experimental design, and adds the factor value to each BioMaterial of a specific BioAssay.
     *
     * @param expExp experiment
     * @param geoSubSet geo subset
     */
    void convertSubsetToExperimentalFactor( ExpressionExperiment expExp, GeoSubset geoSubSet );

    @Nullable
    Taxon getPrimaryArrayTaxon( Collection<Taxon> platformTaxa, Collection<String> probeTaxa )
            throws IllegalArgumentException;

    /**
     * @param splitByPlatform If true, and the series uses more than one platform, split it up. This often isn't
     *        necessary/desirable. This is
     *        overridden if the series uses more than one species, in which case it is always split up.
     */
    void setSplitByPlatform( boolean splitByPlatform );

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