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
package ubic.gemma.web.controller.common.description.bibref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceValueObject {

    public static List<BibliographicReferenceValueObject> convert2ValueObjects( Collection<BibliographicReference> refs ) {
        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();

        for ( BibliographicReference ref : refs ) {
            results.add( new BibliographicReferenceValueObject( ref ) );
        }

        return results;
    }

    private String abstractText;

    private String authorList;

    private String citation;

    private Collection<ExpressionExperimentValueObject> experiments = new HashSet<ExpressionExperimentValueObject>();

    private Long id;

    private String issue;

    private String pages;

    private String pubAccession;

    private String publication;

    private java.util.Date publicationDate;

    private String publisher;

    private String title;

    private String volume;

    private Collection<String> meshTerms;

    public BibliographicReferenceValueObject() {
        super();
    }

    public BibliographicReferenceValueObject( BibliographicReference ref ) {
        this.id = ref.getId();
        this.abstractText = ref.getAbstractText();
        this.authorList = ref.getAuthorList();
        this.pubAccession = ref.getPubAccession().getAccession();
        this.publicationDate = ref.getPublicationDate();
        this.publisher = ref.getPublisher();
        this.pages = ref.getPages();
        this.issue = ref.getIssue();
        this.title = ref.getTitle();
        this.publication = ref.getPublication();
        this.volume = ref.getVolume();
        this.citation = ref.getCitation();

        this.meshTerms = extractTermsfromHeadings( ref.getMeshTerms() );
    }

    /**
     * Extract the Mesh terms from the BibliographicReference
     */
    private Collection<String> extractTermsfromHeadings( Collection<MedicalSubjectHeading> mshCollection ) {

        ArrayList<String> meshTermList = new ArrayList<String>();

        for ( MedicalSubjectHeading msh : mshCollection ) {
            meshTermList.add( msh.getTerm() );
        }
        return meshTermList;
    }

    /**
     * @return the meshTerms
     */
    public Collection<String> getMeshTerms() {
        return meshTerms;
    }

    public BibliographicReferenceValueObject( Long id, String abstractText, String authorList, String citation,
            String issue, String pages, String pubAccession, String publication, Date publicationDate,
            String publisher, String title, String volume, Collection<ExpressionExperimentValueObject> experiments ) {
        super();
        this.id = id;
        this.abstractText = abstractText;
        this.authorList = authorList;
        this.citation = citation;
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

    /**
     * @return the abstractText
     */
    public String getAbstractText() {
        return abstractText;
    }

    /**
     * @return the authorList
     */
    public String getAuthorList() {
        return authorList;
    }

    /**
     * @return the citation
     */
    public String getCitation() {
        return citation;
    }

    /**
     * @return the experiments
     */
    public Collection<ExpressionExperimentValueObject> getExperiments() {
        return experiments;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the issue
     */
    public String getIssue() {
        return issue;
    }

    /**
     * @return the pages
     */
    public String getPages() {
        return pages;
    }

    /**
     * @return the pubAccession
     */
    public String getPubAccession() {
        return pubAccession;
    }

    /**
     * @return the publication
     */
    public String getPublication() {
        return publication;
    }

    /**
     * @return the publicationDate
     */
    public java.util.Date getPublicationDate() {
        return publicationDate;
    }

    /**
     * @return the publisher
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the volume
     */
    public String getVolume() {
        return volume;
    }

    /**
     * @param abstractText the abstractText to set
     */
    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }

    /**
     * @param authorList the authorList to set
     */
    public void setAuthorList( String authorList ) {
        this.authorList = authorList;
    }

    /**
     * @param citation the citation to set
     */
    public void setCitation( String citation ) {
        this.citation = citation;
    }

    /**
     * @param experiments the experiments to set
     */
    public void setExperiments( Collection<ExpressionExperimentValueObject> experiments ) {
        this.experiments = experiments;
    }

    /**
     * @param id the id to set
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @param issue the issue to set
     */
    public void setIssue( String issue ) {
        this.issue = issue;
    }

    /**
     * @param pages the pages to set
     */
    public void setPages( String pages ) {
        this.pages = pages;
    }

    /**
     * @param pubAccession the pubAccession to set
     */
    public void setPubAccession( String pubAccession ) {
        this.pubAccession = pubAccession;
    }

    /**
     * @param publication the publication to set
     */
    public void setPublication( String publication ) {
        this.publication = publication;
    }

    /**
     * @param publicationDate the publicationDate to set
     */
    public void setPublicationDate( java.util.Date publicationDate ) {
        this.publicationDate = publicationDate;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    /**
     * @param title the title to set
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume( String volume ) {
        this.volume = volume;
    }

}
