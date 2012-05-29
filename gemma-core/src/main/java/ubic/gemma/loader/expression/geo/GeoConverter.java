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
package ubic.gemma.loader.expression.geo;

import java.util.Collection;
import java.util.List;

import ubic.gemma.loader.expression.geo.model.GeoData;
import ubic.gemma.loader.expression.geo.model.GeoSubset;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public interface GeoConverter extends Converter<GeoData, Object> {

    /**
     * Remove old results. Call this prior to starting conversion of a full dataset.
     */
    public abstract void clear();

    @Override
    public abstract Collection<Object> convert( Collection<? extends GeoData> geoObjects );

    @Override
    public abstract Object convert( GeoData geoObject );

    /**
     * Converts Geo subsets to experimental factors. This adds a new factor value to the experimental factor of an
     * experimental design, and adds the factor value to each BioMaterial of a specific BioAssay.
     * 
     * @param expExp
     * @param geoSubSet
     * @return ExperimentalFactor
     */
    public abstract void convertSubsetToExperimentalFactor( ExpressionExperiment expExp, GeoSubset geoSubSet );

    /**
     * Converts a supplementary file to a LocalFile object. If the supplementary file is null, the LocalFile=null is
     * returned.
     * 
     * @param object
     * @return LocalFile
     */
    public abstract LocalFile convertSupplementaryFileToLocalFile( Object object );

    public abstract Taxon getPrimaryArrayTaxon( Collection<Taxon> platformTaxa, Collection<String> probeTaxa )
            throws IllegalArgumentException;

    /**
     * If true, and the series uses more than one platform, split it up. This often isn't necessary/desirable. This is
     * overridden if the series uses more than one species, in which case it is always split up.
     * 
     * @param splitByPlatform
     */
    public abstract void setSplitByPlatform( boolean splitByPlatform );

    byte[] convertData( List<Object> vector, QuantitationType qt );

    // this is here for tests only. The default value should be okay otherwise.
    void setElementLimitForStrictness( int i );

}