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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import gemma.gsec.model.SecureValueObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Represents a Gene group gene set
 *
 * @author kelsey
 */
@Data
@EqualsAndHashCode(of = {}, callSuper = true)
public class GeneSetValueObject extends IdentifiableValueObject<GeneSet> implements SecureValueObject {

    private static final long serialVersionUID = 6212231006289412683L;

    private String name;
    private String description;
    /**
     * Gene IDs part of this gene set.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<Long> geneIds;
    private Long size; // only used if we're not populating geneIds

    @Nullable
    private TaxonValueObject taxon;

    /* gsec stuff */
    private boolean userOwned = false;
    private boolean isPublic;
    private boolean isShared;

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

    public GeneSetValueObject( GeneSet geneSet, @Nullable Taxon taxon, @Nullable Long size ) {
        super( geneSet );
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.taxon = taxon != null ? new TaxonValueObject( taxon ) : null;
        this.size = size;
    }

    @GemmaWebOnly
    public Long getTaxonId() {
        return this.taxon != null ? this.taxon.getId() : null;
    }

    @GemmaWebOnly
    public String getTaxonName() {
        return this.taxon != null ? this.taxon.getCommonName() : null;
    }

    /**
     * @return the number of members in the group
     */
    public Long getSize() {
        if ( geneIds != null ) {
            return ( long ) geneIds.size();
        } else {
            return size;
        }
    }

    public void setSize( long size ) {
        if ( geneIds != null && size != geneIds.size() ) {
            throw new IllegalArgumentException( String.format( "Invalid size: the gene IDs has a size of %d",
                    geneIds.size() ) );
        }
        this.size = size;
    }

    @GemmaWebOnly
    public boolean getCurrentUserIsOwner() {
        return userOwned;
    }

    @Override
    @JsonIgnore
    public boolean getUserOwned() {
        return userOwned;
    }

    @Override
    @JsonIgnore
    public boolean getUserCanWrite() {
        return userOwned;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.userOwned = userCanWrite;
    }

    @Override
    @JsonIgnore
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Override
    @JsonIgnore
    public boolean getIsShared() {
        return this.isShared;
    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;
    }

    @Override
    @JsonIgnore
    public Class<? extends Securable> getSecurableClass() {
        return GeneSet.class;
    }

    @Override
    public String toString() {
        return "GeneSetValueObject [id=" + id + ", name=" + name + ", taxonName=" + ( taxon != null ? taxon.getCommonName() : null ) + "]";
    }
}
