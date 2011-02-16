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
package ubic.gemma.loader.expression.smd.model;

import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.PersonImpl;

/**
 * <pre>
 * 
 * 
 * 
 * 
 * 
 *       !Experimenter=Douglas Ross
 *       !Contact email=dross@cmgm.stanford.edu
 *       !Contact Address1=Genetics
 *       !Contact Address2=M309
 *       !State=CA
 *       !Postal Code=94305
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SMDPerson {

    /**
     * @return representation of this object as a Person.
     */
    public Person toPerson() {
        Person result = new PersonImpl();

        result.setEmail( this.email );
        result.setLastName( this.name ); // FIXME.
        result.setAddress( this.address1 + " " + this.address2 + " " + this.address3 + " " + this.address4 + " "
                + this.state + " " + this.postalCode + " " + this.country );

        return result;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1( String address1 ) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2( String address2 ) {
        this.address2 = address2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode( String postalCode ) {
        this.postalCode = postalCode;
    }

    public String getState() {
        return state;
    }

    public void setState( String state ) {
        this.state = state;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3( String address3 ) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4( String address4 ) {
        this.address4 = address4;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry( String country ) {
        this.country = country;
    }

    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String country;
    private String state;
    private String postalCode;
    private String email;
    private String name;

}