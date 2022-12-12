/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.io.Serializable;

/**
 * @author jleong
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class DumpsValueObject implements Serializable {

    private String name;
    private String url;
    private String lastModified;

    public DumpsValueObject() {
        super();
        name = "";
        url = "";
        lastModified = "";
    }

    public DumpsValueObject( String paramName, String paramUrl, String paramLastModified ) {
        super();
        name = paramName;
        url = paramUrl;
        lastModified = paramLastModified;
    }

    public String getName() {
        return name;
    }

    public void setName( String paramName ) {
        name = paramName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String paramUrl ) {
        url = paramUrl;
    }

    public String getModified() {
        return lastModified;
    }

    public void setModified( String paramModified ) {
        lastModified = paramModified;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( url == null ) ? 0 : url.hashCode() );
        result = prime * result + ( ( lastModified == null ) ? 0 : lastModified.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        DumpsValueObject other = ( DumpsValueObject ) obj;
        if ( name == null ) {
            if ( other.getName() != null )
                return false;
        } else if ( !name.equals( other.getName() ) )
            return false;
        if ( url == null ) {
            return other.getUrl() == null;
        } else
            return url.equals( other.getUrl() );
    }

    @Override
    public String toString() {
        return "DumpsVO [name=" + name + " : " + url + lastModified + "]";
    }
}
