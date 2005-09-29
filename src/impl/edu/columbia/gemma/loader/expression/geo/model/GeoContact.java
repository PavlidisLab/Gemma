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

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoContact {

    String name;
    String email;
    String institute;
    String department;
    String city;
    String phone;
    String fax;
    String webLink;

    /**
     * @return Returns the city.
     */
    public String getCity() {
        return this.city;
    }

    /**
     * @param city The city to set.
     */
    public void setCity( String city ) {
        this.city = city;
    }

    /**
     * @return Returns the department.
     */
    public String getDepartment() {
        return this.department;
    }

    /**
     * @param department The department to set.
     */
    public void setDepartment( String department ) {
        this.department = department;
    }

    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * @param email The email to set.
     */
    public void setEmail( String email ) {
        this.email = email;
    }

    /**
     * @return Returns the fax.
     */
    public String getFax() {
        return this.fax;
    }

    /**
     * @param fax The fax to set.
     */
    public void setFax( String fax ) {
        this.fax = fax;
    }

    /**
     * @return Returns the institute.
     */
    public String getInstitute() {
        return this.institute;
    }

    /**
     * @param institute The institute to set.
     */
    public void setInstitute( String institute ) {
        this.institute = institute;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name The name to set.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return Returns the phone.
     */
    public String getPhone() {
        return this.phone;
    }

    /**
     * @param phone The phone to set.
     */
    public void setPhone( String phone ) {
        this.phone = phone;
    }

    /**
     * @return Returns the webLink.
     */
    public String getWebLink() {
        return this.webLink;
    }

    /**
     * @param webLink The webLink to set.
     */
    public void setWebLink( String webLink ) {
        this.webLink = webLink;
    }

}
