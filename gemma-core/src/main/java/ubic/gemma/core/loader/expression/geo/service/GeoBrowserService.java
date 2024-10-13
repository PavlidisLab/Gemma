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

import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * @author paul
 */
public interface GeoBrowserService {

    /**
     * Get details from GEO about an accession.
     *
     * @param  accession   accession
     * @return details
     * @throws IOException if there is a problem while manipulating the file
     */
    String getDetails( String accession, String contextPath ) throws IOException;

    /**
     * @param start page number, not starting record
     * @param count page size
     * @return geo records
     * @throws IOException    if there is a problem while manipulating the file
     * @throws ParseException if there is a problem with parsing
     */
    List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException, ParseException;

    /**
     * @param searchString can be null
     * @param start        first record to retrieve
     * @param count        how many records to retrieve
     * @param detailed     if true, more information is retrieved (slow)
     * @return collection of GeoRecords
     */
    List<GeoRecord> searchGeoRecords( @Nullable String searchString, int start, int count, boolean detailed ) throws IOException;

    boolean toggleUsability( String accession );

}