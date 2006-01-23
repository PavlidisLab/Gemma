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
package edu.columbia.gemma.loader.expression.geo;

import java.util.Collection;

import edu.columbia.gemma.loader.loaderutils.Converter;
import edu.columbia.gemma.loader.loaderutils.Persister;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
import edu.columbia.gemma.loader.loaderutils.SourceDomainObjectGenerator;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetService {

    private SourceDomainObjectGenerator generator;
    private Persister expLoader;
    private Converter converter;

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download and parse GDS file</li>
     * <li>Download, parse and convert the associated GSE family file(s)</li>
     * <li>Load the resulting data into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    public void fetchAndLoad( String geoDataSetAccession ) {

        generator = new GeoDomainObjectGenerator();

        GeoParseResult results = ( GeoParseResult ) generator.generate( geoDataSetAccession ).iterator().next();

        Collection<Object> convertedResults = ( Collection<Object> ) converter.convert( results.getDatasets().values() );

        assert expLoader != null;
        expLoader.persist( convertedResults );
    }

    /**
     * @param expressionLoader
     */
    public void setPersister( PersisterHelper expressionLoader ) {
        this.expLoader = expressionLoader;
    }

    /**
     * @param geoConv to set
     */
    public void setConverter( GeoConverter geoConv ) {
        this.converter = geoConv;
    }

}
