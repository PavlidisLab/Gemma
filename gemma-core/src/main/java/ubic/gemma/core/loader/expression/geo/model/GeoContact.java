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

import java.io.Serializable;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeoContact implements Serializable {

    private static final long serialVersionUID = -2042747972349661568L;
    private String city;
    private String department;
    private String email;
    private String fax;
    private String institute;
    private String name;
    private String phone;
    private String postCode;
    private String state;
    private String webLink;
    private String country;

    /**
     * @return Returns the country.
     */
    public String getCountry() {
        return this.country;
    }

    /**
     * @param country The country to set.
     */
    public void setCountry( String country ) {
        this.country = country;
    }

    /**
     * @return Returns the city.
     */
    public String getCity() {
        return this.city;
    }

    /**
     * @return Returns the department.
     */
    public String getDepartment() {
        return this.department;
    }

    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * @return Returns the fax.
     */
    public String getFax() {
        return this.fax;
    }

    /**
     * @return Returns the institute.
     */
    public String getInstitute() {
        return this.institute;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return Returns the phone.
     */
    public String getPhone() {
        return this.phone;
    }

    /**
     * @return Returns the postCode.
     */
    public String getPostCode() {
        return this.postCode;
    }

    /**
     * @return Returns the state.
     */
    public String getState() {
        return this.state;
    }

    /**
     * @return Returns the webLink.
     */
    public String getWebLink() {
        return this.webLink;
    }

    /**
     * @param city The city to set.
     */
    public void setCity( String city ) {
        this.city = city;
    }

    /**
     * @param department The department to set.
     */
    public void setDepartment( String department ) {
        this.department = department;
    }

    /**
     * @param email The email to set.
     */
    public void setEmail( String email ) {
        this.email = email;
    }

    /**
     * @param fax The fax to set.
     */
    public void setFax( String fax ) {
        this.fax = fax;
    }

    /**
     * @param institute The institute to set.
     */
    public void setInstitute( String institute ) {
        this.institute = institute;
    }

    /**
     * @param name The name to set.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param phone The phone to set.
     */
    public void setPhone( String phone ) {
        this.phone = phone;
    }

    /**
     * @param postCode The postCode to set.
     */
    public void setPostCode( String postCode ) {
        this.postCode = postCode;
    }

    /**
     * @param state The state to set.
     */
    public void setState( String state ) {
        this.state = state;
    }

    /**
     * @param webLink The webLink to set.
     */
    public void setWebLink( String webLink ) {
        this.webLink = webLink;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( email == null ) ? 0 : email.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final GeoContact other = ( GeoContact ) obj;
        if ( email == null ) {
            if ( other.email != null ) return false;
        } else if ( !email.equals( other.email ) ) return false;
        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;
        return true;
    }

}
