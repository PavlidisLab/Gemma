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

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.expression.geo.model.GeoData;
import ubic.gemma.loader.expression.geo.model.GeoSubset;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.common.description.ExternalDatabaseService;
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

    /**
     * Converts a supplementary file to a LocalFile object. If the supplementary file is null, the LocalFile=null is
     * returned.
     * 
     * @param object
     * @return LocalFile
     */
    public abstract LocalFile convertSupplementaryFileToLocalFile( Object object );

    /**
     * Convert a vector of strings into a byte[] for saving in the database. . Blanks(missing values) are treated as NAN
     * (double), 0 (integer), false (booleans) or just empty strings (strings). Other invalid values are treated the
     * same way as missing data (to keep the parser from failing when dealing with strange GEO files that have values
     * like "Error" for an expression value).
     * 
     * @param vector of Strings to be converted to primitive values (double, int etc)
     * @param qt The quantitation type for the values to be converted.
     * @return
     */
    public abstract byte[] convertData( List<Object> vector, QuantitationType qt );

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
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public abstract void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService );

    /**
     * @param taxonService the taxonService to set
     */
    public abstract void setTaxonService( TaxonService taxonService );

    /**
     * If true, and the series uses more than one platform, split it up. This often isn't necessary/desirable. This is
     * overridden if the series uses more than one species, in which case it is always split up.
     * 
     * @param splitByPlatform
     */
    public abstract void setSplitByPlatform( boolean splitByPlatform );

    public abstract Collection<Object> convert( Collection<? extends GeoData> geoObjects );

    public abstract Taxon getPrimaryArrayTaxon( Collection<Taxon> platformTaxa, Collection<String> probeTaxa )
            throws IllegalArgumentException;

    public abstract Object convert( GeoData geoObject );

}