/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.expression.arrayDesign;

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an assembly of design elements that are assayed all at once.
 *
 * @author Paul
 */
@Indexed
public class ArrayDesign extends AbstractAuditable implements Curatable, SecuredNotChild {

    public static final class Factory {

        public static ArrayDesign newInstance() {
            return new ArrayDesign();
        }

        public static ArrayDesign newInstance( String shortName, Taxon taxon ) {
            ArrayDesign ad = new ArrayDesign();
            ad.setShortName( shortName );
            ad.setPrimaryTaxon( taxon );
            return ad;
        }
    }

    private Integer advertisedNumberOfDesignElements;
    private Set<AlternateName> alternateNames = new HashSet<>();
    private ArrayDesign alternativeTo; // for affymetrix
    private Set<CompositeSequence> compositeSequences = new HashSet<>();
    private CurationDetails curationDetails = new CurationDetails();
    private Contact designProvider;
    private Set<DatabaseEntry> externalReferences = new HashSet<>();
    private ArrayDesign mergedInto;
    private Set<ArrayDesign> mergees = new HashSet<>();
    private Taxon primaryTaxon;
    private String shortName;
    private Set<ArrayDesign> subsumedArrayDesigns = new HashSet<>();
    private ArrayDesign subsumingArrayDesign;

    private TechnologyType technologyType;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ArrayDesign() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * @return The number of design elements, according to the manufactuerer or determined at the time the array design
     *         was
     *         entered into the system. The actual number of design elements can only be determined by looking at the
     *         associated
     *         Set of DesignElements.
     */
    public Integer getAdvertisedNumberOfDesignElements() {
        return this.advertisedNumberOfDesignElements;
    }

    @IndexedEmbedded
    public Set<AlternateName> getAlternateNames() {
        return this.alternateNames;
    }

    /**
     *
     * @return true if this is an Affymetrix platform that has a related "canonical" platform we use instead.
     */
    public ArrayDesign getAlternativeTo() {
        return alternativeTo;
    }

    public Set<CompositeSequence> getCompositeSequences() {
        return this.compositeSequences;
    }

    @Override
    public CurationDetails getCurationDetails() {
        return this.curationDetails;
    }

    public Contact getDesignProvider() {
        return this.designProvider;
    }

    /**
     * @return Accessions for this array design in other databases, e.g., GEO, ArrayExpression.
     */
    @IndexedEmbedded
    public Set<DatabaseEntry> getExternalReferences() {
        return this.externalReferences;
    }

    public ArrayDesign getMergedInto() {
        return this.mergedInto;
    }

    public Set<ArrayDesign> getMergees() {
        return this.mergees;
    }

    /**
     * @return The taxon the array design is for. This could be a non-specific taxon ("salmonid"). It may not match the
     *         sequences on the array exactly. For example, a mouse array might have some non-mouse sequences as
     *         controls, but
     *         the primary taxon is still mouse.
     */
    public Taxon getPrimaryTaxon() {
        return this.primaryTaxon;
    }

    /**
     * @return A brief unique (but optional) human-readable name for the expression experiment. For example in the past
     *         we often
     *         used names like "HG-U95A".
     */
    @Field(analyze = Analyze.NO)
    public String getShortName() {
        return this.shortName;
    }

    /**
     * @return Array designs that this array design "covers". For example, the HG-U133_Plus_2 array includes all the
     *         elements
     *         that are on the HG-U133A and HG-U133B, so they are subsumed by the HG-U133_Plus_2.
     */
    public Set<ArrayDesign> getSubsumedArrayDesigns() {
        return this.subsumedArrayDesigns;
    }

    /**
     * @return An array design that subsumes this one (contains DesignElements that are equivalent to the ones on this
     *         arraydesign).
     */
    public ArrayDesign getSubsumingArrayDesign() {
        return this.subsumingArrayDesign;
    }

    public TechnologyType getTechnologyType() {
        return this.technologyType;
    }

    public void setAdvertisedNumberOfDesignElements( Integer advertisedNumberOfDesignElements ) {
        this.advertisedNumberOfDesignElements = advertisedNumberOfDesignElements;
    }

    public void setAlternateNames( Set<AlternateName> alternateNames ) {
        this.alternateNames = alternateNames;
    }

    public void setAlternativeTo( ArrayDesign alternativeTo ) {
        this.alternativeTo = alternativeTo;
    }

    public void setCompositeSequences(
            Set<CompositeSequence> compositeSequences ) {
        this.compositeSequences = compositeSequences;
    }

    @Override
    public void setCurationDetails( CurationDetails curationDetails ) {
        this.curationDetails = curationDetails;
    }

    public void setDesignProvider( Contact designProvider ) {
        this.designProvider = designProvider;
    }

    public void setExternalReferences(
            Set<DatabaseEntry> externalReferences ) {
        this.externalReferences = externalReferences;
    }

    public void setMergedInto( ArrayDesign mergedInto ) {
        this.mergedInto = mergedInto;
    }

    public void setMergees( Set<ArrayDesign> mergees ) {
        this.mergees = mergees;
    }

    public void setPrimaryTaxon( Taxon primaryTaxon ) {
        this.primaryTaxon = primaryTaxon;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSubsumedArrayDesigns(
            Set<ArrayDesign> subsumedArrayDesigns ) {
        this.subsumedArrayDesigns = subsumedArrayDesigns;
    }

    public void setSubsumingArrayDesign( ArrayDesign subsumingArrayDesign ) {
        this.subsumingArrayDesign = subsumingArrayDesign;
    }

    public void setTechnologyType( TechnologyType technologyType ) {
        this.technologyType = technologyType;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getShortName() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ArrayDesign ) )
            return false;
        ArrayDesign that = ( ArrayDesign ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else if ( getShortName() != null && that.getShortName() != null ) {
            return getShortName().equals( that.getShortName() );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString() + ( shortName != null ? " Short Name=" + shortName : "" );
    }

}