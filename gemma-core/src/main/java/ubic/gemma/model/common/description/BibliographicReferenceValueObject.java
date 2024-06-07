/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import org.hibernate.Hibernate;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.*;

/**
 * represents a BibliographicReferenceValueObject when this value object is needed in core, the same value object exists
 * in web
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possibly used in front end
public class BibliographicReferenceValueObject extends IdentifiableValueObject<BibliographicReference> {

    private String abstractText;
    private String authorList;
    private CitationValueObject citation;
    private Collection<ExpressionExperimentValueObject> experiments = new HashSet<>();
    private String issue;
    private String pages;
    private String pubAccession;
    private String publication;
    private java.util.Date publicationDate;
    private String publisher;
    private String title;
    private String volume;
    private Collection<String> meshTerms;
    private Collection<String> chemicalsTerms;
    private boolean retracted = false;

    /**
     * Required when using the class as a spring bean.
     */
    public BibliographicReferenceValueObject() {
        super();
    }

    public BibliographicReferenceValueObject( Long id ) {
        super( id );
    }

    /**
     * does not set related experiments field
     *
     * @param ref bib ref
     */
    public BibliographicReferenceValueObject( BibliographicReference ref ) {
        super( ref );
        this.abstractText = ref.getAbstractText();
        this.authorList = ref.getAuthorList();
        if ( ref.getPubAccession() != null ) {
            this.pubAccession = ref.getPubAccession().getAccession();
        }
        this.publicationDate = ref.getPublicationDate();
        this.publisher = ref.getPublisher();
        this.pages = ref.getPages();
        this.issue = ref.getIssue();
        this.title = ref.getTitle();
        this.publication = ref.getPublication();
        this.volume = ref.getVolume();
        this.citation = constructCitation( ref );

        if ( Hibernate.isInitialized( ref.getMeshTerms() ) ) {
            this.meshTerms = extractTermsFromHeadings( ref.getMeshTerms() );
        }
        if ( Hibernate.isInitialized( ref.getChemicals() ) ) {
            this.chemicalsTerms = extractChemFromHeadings( ref.getChemicals() );
        }
        this.retracted = ref.getRetracted();
    }

    public BibliographicReferenceValueObject( Long id, String abstractText, String authorList, String issue,
            String pages, String pubAccession, String publication, Date publicationDate, String publisher, String title,
            String volume, Collection<ExpressionExperimentValueObject> experiments ) {
        super( id );
        this.abstractText = abstractText;
        this.authorList = authorList;
        this.issue = issue;
        this.pages = pages;
        this.pubAccession = pubAccession;
        this.publication = publication;
        this.publicationDate = publicationDate;
        this.publisher = publisher;
        this.title = title;
        this.volume = volume;
        this.experiments = experiments;
    }

    public static CitationValueObject constructCitation( BibliographicReference bib ) {
        return CitationValueObject.convert2CitationValueObject( bib );
    }

    public static Collection<CitationValueObject> constructCitations( Collection<BibliographicReference> bibs ) {
        return CitationValueObject.convert2CitationValueObjects( bibs );
    }

    /**
     * does not set related experiments field
     *
     * @param refs bib refs
     * @return bib ref VOs
     */
    public static List<BibliographicReferenceValueObject> convert2ValueObjects(
            Collection<BibliographicReference> refs ) {

        List<BibliographicReferenceValueObject> results = new ArrayList<>();

        if ( refs != null ) {
            for ( BibliographicReference ref : refs ) {
                results.add( new BibliographicReferenceValueObject( ref ) );
            }
        }

        return results;
    }

    /**
     * @return the abstractText
     */
    public String getAbstractText() {
        return abstractText;
    }

    /**
     * @param abstractText the abstractText to set
     */
    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }

    /**
     * @return the authorList
     */
    public String getAuthorList() {
        return authorList;
    }

    /**
     * @param authorList the authorList to set
     */
    public void setAuthorList( String authorList ) {
        this.authorList = authorList;
    }


    /**
     * @return the chemicalsTerms
     */
    public Collection<String> getChemicalsTerms() {
        return chemicalsTerms;
    }

    /**
     * @return the citation
     */
    public CitationValueObject getCitation() {
        return citation;
    }

    /**
     * @param citation the citation to set
     */
    public void setCitation( CitationValueObject citation ) {
        this.citation = citation;
    }

    /**
     * @return the experiments
     */
    public Collection<ExpressionExperimentValueObject> getExperiments() {
        return experiments;
    }

    /**
     * @param experiments the experiments to set
     */
    public void setExperiments( Collection<ExpressionExperimentValueObject> experiments ) {
        this.experiments = experiments;
    }

    /**
     * @return the issue
     */
    public String getIssue() {
        return issue;
    }

    /**
     * @param issue the issue to set
     */
    public void setIssue( String issue ) {
        this.issue = issue;
    }

    /**
     * @return the meshTerms
     */
    public Collection<String> getMeshTerms() {
        return meshTerms;
    }

    /**
     * @return the pages
     */
    public String getPages() {
        return pages;
    }

    /**
     * @param pages the pages to set
     */
    public void setPages( String pages ) {
        this.pages = pages;
    }

    /**
     * @return the pubAccession
     */
    public String getPubAccession() {
        return pubAccession;
    }

    /**
     * @param pubAccession the pubAccession to set
     */
    public void setPubAccession( String pubAccession ) {
        this.pubAccession = pubAccession;
    }

    /**
     * @return the publication
     */
    public String getPublication() {
        return publication;
    }

    /**
     * @param publication the publication to set
     */
    public void setPublication( String publication ) {
        this.publication = publication;
    }

    /**
     * @return the publicationDate
     */
    public java.util.Date getPublicationDate() {
        return publicationDate;
    }

    /**
     * @param publicationDate the publicationDate to set
     */
    public void setPublicationDate( java.util.Date publicationDate ) {
        this.publicationDate = publicationDate;
    }

    /**
     * @return the publisher
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @return the volume
     */
    public String getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume( String volume ) {
        this.volume = volume;
    }

    public boolean isRetracted() {
        return retracted;
    }

    public void setRetracted( boolean retracted ) {
        this.retracted = retracted;
    }

    /**
     * Extract the Chemicals terms from the BibliographicReference
     */
    private Collection<String> extractChemFromHeadings( Collection<Compound> chemCollection ) {

        ArrayList<String> chemTermList = new ArrayList<>();

        for ( Compound compound : chemCollection ) {
            chemTermList.add( compound.getName() );
        }
        return chemTermList;
    }

    /**
     * Extract the Mesh terms from the BibliographicReference
     */
    private Collection<String> extractTermsFromHeadings( Collection<MedicalSubjectHeading> mshCollection ) {

        ArrayList<String> meshTermList = new ArrayList<>();

        for ( MedicalSubjectHeading msh : mshCollection ) {
            meshTermList.add( msh.getTerm() );
        }
        return meshTermList;
    }

}
