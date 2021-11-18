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

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import ubic.gemma.model.IdentifiableValueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Gene group gene set
 *
 * @author kelsey
 */
public class GeneSetValueObject extends IdentifiableValueObject<GeneSet> implements SecureValueObject {

    private static final long serialVersionUID = 6212231006289412683L;

    /**
     * Create a lightweight wrapper that can be used for security filtering
     *
     * @param ids ids
     * @return collection of VOs
     */
    @SuppressWarnings("unused") // Possible external use
    public static Collection<GeneSetValueObject> fromIds( Collection<Long> ids ) {
        Collection<GeneSetValueObject> result = new ArrayList<>();
        for ( Long id : ids ) {
            result.add( new GeneSetValueObject( id ) );
        }
        return result;
    }

    private boolean currentUserIsOwner = false;
    private String description;
    private Set<Long> geneIds = new HashSet<>();

    private boolean isPublic;
    private boolean isShared;
    private String name;
    private Integer size; // only used if we're not populating geneIds

    private Long taxonId;
    private String taxonName;
    private boolean userOwned = false;

    /**
     * default constructor to satisfy java bean contract
     */
    public GeneSetValueObject() {
        super();
    }

    /**
     * Create a lightweight wrapper that can be used for security filtering
     *
     * @param id id
     */
    public GeneSetValueObject( Long id ) {
        super( id );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( this.getClass() != obj.getClass() ) {
            return false;
        }
        GeneSetValueObject other = ( GeneSetValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

    public String getDescription() {
        return this.description;
    }

    public Set<Long> getGeneIds() {
        return this.geneIds;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return GeneSet.class;
    }

    /**
     * @return the number of members in the group
     */
    public Integer getSize() {
        if ( this.getGeneIds() != null && !this.getGeneIds().isEmpty() )
            return this.getGeneIds().size();
        else if ( this.size > 0 )
            return this.size;
        return 0;
    }

    public Long getTaxonId() {
        return this.taxonId;
    }

    public String getTaxonName() {
        return this.taxonName;
    }

    @Override
    public boolean getUserCanWrite() {
        return this.userOwned;
    }

    @Override
    public boolean getUserOwned() {
        return this.currentUserIsOwner;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setGeneIds( Set<Long> geneMembers ) {
        this.geneIds = geneMembers;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;

    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;

    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setSize( Integer size ) {
        if ( this.getGeneIds() != null && !this.getGeneIds().isEmpty() && size != this.getGeneIds().size() ) {
            throw new IllegalArgumentException( "Invalid 'size'" );
        }
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
    public void setUserOwned( boolean isUserOwned ) {
        this.currentUserIsOwner = isUserOwned;
    }

    @Override
    public String toString() {
        return "GeneSetValueObject [id=" + id + ", name=" + name + ", taxonName=" + taxonName + "]";
    }

}
