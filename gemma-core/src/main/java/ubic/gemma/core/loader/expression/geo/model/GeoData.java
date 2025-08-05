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
package ubic.gemma.core.loader.expression.geo.model;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * Abstract class from which other GEO objects are descended.
 *
 * @author pavlidis
 */
@Getter
@Setter
public abstract class GeoData implements Serializable {

    @Nullable
    private String geoAccession;
    private String title = "";
    private GeoContact contact = new GeoContact();
    private Map<String, Collection<String>> relations = new HashMap<>();

    /**
     * The column names mean different things in different subclasses. For samples, the column names are the
     * "quantitation types". For platforms, they are descriptor names.
     */
    private final List<String> columnNames = new ArrayList<>();
    private final List<String> columnDescriptions = new ArrayList<>();

    public void addRelation( String key, String value ) {
        this.relations
                .computeIfAbsent( key, k -> new HashSet<>() )
                .add( value );
    }

    public void addColumnName( String columnName ) {
        assert columnName != null;
        this.columnNames.add( columnName );
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getGeoAccession() == null ? 0 : this.getGeoAccession().hashCode() );
        return hashCode;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( !( obj instanceof GeoData ) )
            return false;
        return Objects.equals( ( ( GeoData ) obj ).getGeoAccession(), this.getGeoAccession() );
    }

    @Override
    public String toString() {
        return this.geoAccession;
    }
}
