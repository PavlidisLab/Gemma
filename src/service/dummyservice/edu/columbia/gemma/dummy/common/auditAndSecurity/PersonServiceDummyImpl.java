/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

/*
 * TODO This is a dummy Service object.  It should be moved into the PersonServiceImpl when the model has been 
 * solidified.  When doing this, you need to delete the personServiceDummyImpl bean and the personService proxy
 * from the action-servlet.xml file.  Don't worry, the code will use the personService in the 
 * applicationContext-hibernate.xml file.
 */

package edu.columbia.gemma.dummy.common.auditAndSecurity;

import org.apache.commons.lang.RandomStringUtils;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 Columbia University
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.auditAndSecurity.PersonService
 */
public class PersonServiceDummyImpl
    extends edu.columbia.gemma.common.auditAndSecurity.PersonServiceBase
{
    private String identifier = null;
    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#savePerson(edu.columbia.gemma.common.auditAndSecurity.Person)
     */
    protected void handleSavePerson(edu.columbia.gemma.common.auditAndSecurity.Person person)
        throws java.lang.Exception
    {
        //@todo implement protected void handleSavePerson(edu.columbia.gemma.common.auditAndSecurity.Person person)
       identifier = RandomStringUtils.randomAlphabetic(11);
       person.setIdentifier(identifier);
        getPersonDao().create(person);
        //throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.common.auditAndSecurity.PersonService.handleSavePerson(edu.columbia.gemma.common.auditAndSecurity.Person person) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#getAllPersons()
     * @return
     */
    protected java.util.Collection handleGetAllPersons()
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleGetAllPersons()
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#removePerson(java.lang.String, java.lang.String, java.lang.String)
     * @param 
     * @param
     * @param
     */
    protected void handleRemovePerson(edu.columbia.gemma.common.auditAndSecurity.Person person)
        throws java.lang.Exception
    {
        //@todo implement protected void handleRemovePerson(java.lang.String firstName, java.lang.String lastName, java.lang.String middleName)
        //throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.common.auditAndSecurity.PersonService.handleRemovePerson(java.lang.String firstName, java.lang.String lastName, java.lang.String middleName) Not implemented!");
       getPersonDao().remove(person);
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#findByName(java.lang.String, java.lang.String, java.lang.String)
     * @return
     * @param
     * @param
     * @param
     * 
     */
    protected edu.columbia.gemma.common.auditAndSecurity.Person handleFindByName(java.lang.String firstName, java.lang.String lastName, java.lang.String middleName)
        throws java.lang.Exception
    {
        //@todo implement protected edu.columbia.gemma.common.auditAndSecurity.Person handleFindByName(java.lang.String firstName, java.lang.String lastName, java.lang.String middleName)
        return null;
    }

}