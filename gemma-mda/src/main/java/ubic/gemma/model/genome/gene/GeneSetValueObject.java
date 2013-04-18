/*

 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecureValueObject;

/**
 * Represents a Gene group gene set.
 * 
 * @author kelsey
 * @version $Id$
 */
public class GeneSetValueObject implements SecureValueObject {

    private static final long serialVersionUID = 6212231006289412683L;

    private boolean userOwned = false;
    private boolean currentUserIsOwner = false;
    private String description;
    private Collection<Long> geneIds = new HashSet<Long>();
    private Long id;
    private boolean isPublic;
    private boolean isShared;
    private String name;
    private Integer size;
    private Long taxonId;
    private String taxonName;

    /**
     * default constructor to satisfy java bean contract
     */
    public GeneSetValueObject() {
        super();
    }

    public boolean equals( GeneSetValueObject ervo ) {
        if ( ervo.getClass().equals( this.getClass() ) && ervo.getId().equals( this.getId() ) ) {
            return true;
        }
        return false;
    }

    /**
     * @return
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return
     */
    public Collection<Long> getGeneIds() {
        return this.geneIds;
    }

    /**
     * @return
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * @return
     */
    public String getName() {
        return this.name;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return GeneSetImpl.class;
    }

    /**
     * returns the number of members in the group
     * 
     * @return
     */
    public Integer getSize() {
        return this.size;
    }

    public Long getTaxonId() {
        return this.taxonId;
    }

    public String getTaxonName() {
        return this.taxonName;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

    @Override
    public boolean getUserOwned() {
        return this.currentUserIsOwner;
    }

    /**
     * @param description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param geneMembers
     */
    public void setGeneIds( Collection<Long> geneMembers ) {
        this.geneIds = geneMembers;
    }

    /**
     * @param id
     */
    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;

    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;

    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.currentUserIsOwner = isUserOwned;

    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param size
     */
    public void setSize( Integer size ) {
        this.size = size;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.userOwned = userCanWrite;
    }

    @Override
    public boolean getUserCanWrite() {
        return this.userOwned;
    }
}
