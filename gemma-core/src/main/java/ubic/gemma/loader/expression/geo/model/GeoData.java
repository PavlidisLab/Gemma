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
package ubic.gemma.loader.expression.geo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class from which other GEO objects are descended.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class GeoData implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -859220901359581113L;

    protected GeoContact contact = new GeoContact();

    protected String geoAccession;

    private List<String> columnNames = new ArrayList<String>();

    private List<String> columnDescriptions = new ArrayList<String>();

    private String title = "";

    /**
     * @param columnName
     */
    public void addColumnName( String columnName ) {
        assert columnName != null;
        this.columnNames.add( columnName );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof GeoData ) {
            return ( ( GeoData ) obj ).getGeoAccession().equals( this.getGeoAccession() );
        }
        return false;
    }

    /**
     * @return Returns the columnDescriptions.
     */
    public List<String> getColumnDescriptions() {
        return this.columnDescriptions;
    }

    /**
     * The column names mean different things in different subclasses. For samples, the column names are the
     * "quantitation types". For platforms, they are descriptor names.
     * 
     * @return Returns the columnNames.
     */
    public List<String> getColumnNames() {
        return this.columnNames;
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
     * @return Returns the title.
     */
    public String getTitle() {
        return this.title;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( getGeoAccession() == null ? 0 : getGeoAccession().hashCode() );
        return hashCode;
    }

    /**
     * @param geoAccesssion The geoAccesssion to set.
     */
    public void setGeoAccession( String geoAccesssion ) {
        this.geoAccession = geoAccesssion;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    @Override
    public String toString() {
        return this.geoAccession;
    }

}
