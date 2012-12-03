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

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import ubic.gemma.loader.expression.geo.model.GeoRecord;

/**
 * @author paul
 * @version $Id$
 */
public interface GeoBrowserService {

    /**
     * Get details from GEO about an accession.
     * 
     * @param accession
     * @return
     * @throws IOException
     */
    public abstract String getDetails( String accession ) throws IOException;

    /**
     * @param start page number, not starting record
     * @param count page size
     * @return
     * @throws IOException
     * @throws ParseException 
     */
    public abstract List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException, ParseException;

    /**
     * @param accession
     * @param currentState
     */
    public abstract boolean toggleUsability( String accession );

}