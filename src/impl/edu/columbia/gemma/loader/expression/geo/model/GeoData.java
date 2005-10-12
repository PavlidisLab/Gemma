/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class from which many Geo objects are descended.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class GeoData {

    private Map<String, List<String>> data = new HashMap<String, List<String>>();;

    protected GeoContact contact = new GeoContact();

    protected String geoAccession;

    private List<String> columnNames = new ArrayList<String>();

    private List<String> columnDescriptions = new ArrayList<String>();

    /**
     * @return Returns the columnNames.
     */
    public List<String> getColumnNames() {
        return this.columnNames;
    }

    /**
     * @return
     */
    public Map<String, List<String>> getData() {
        return this.data;
    }

    /**
     * @return
     */
    public GeoContact getContact() {
        return this.contact;
    }

    /**
     * @return Returns the geoAccesssion.
     */
    public String getGeoAccession() {
        return this.geoAccession;
    }

    /**
     * @param geoAccesssion The geoAccesssion to set.
     */
    public void setGeoAccession( String geoAccesssion ) {
        this.geoAccession = geoAccesssion;
    }

    @Override
    public String toString() {
        return this.geoAccession;
    }

    /**
     * @return Returns the columnDescriptions.
     */
    public List<String> getColumnDescriptions() {
        return this.columnDescriptions;
    }

}
