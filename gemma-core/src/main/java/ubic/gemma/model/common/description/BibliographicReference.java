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
package ubic.gemma.model.common.description;

import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.expression.biomaterial.Compound;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class BibliographicReference extends AbstractAuditable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5823969791153895703L;
    private String authorList;
    private String title;
    private String publisher;
    private String editor;
    private String volume;
    private String issue;
    private String pages;
    private String publication;
    private String fullTextUri;
    private String abstractText;
    private String citation;
    private Date publicationDate;
    private String annotatedAbstract;
    private DatabaseEntry pubAccession;
    private LocalFile fullTextPdf;
    private Collection<Characteristic> annotations = new HashSet<>();
    private Collection<MedicalSubjectHeading> meshTerms = new HashSet<>();
    private Collection<PublicationType> publicationTypes = new HashSet<>();
    private Collection<Keyword> keywords = new HashSet<>();
    private Collection<Compound> chemicals = new HashSet<>();

    public String getAbstractText() {
        return this.abstractText;
    }

    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }

    /**
     * @return A version of the abstract with inserted markup (e.g., abbreviation expansions, part-of-speech)
     */
    public String getAnnotatedAbstract() {
        return this.annotatedAbstract;
    }

    public void setAnnotatedAbstract( String annotatedAbstract ) {
        this.annotatedAbstract = annotatedAbstract;
    }

    public Collection<Characteristic> getAnnotations() {
        return this.annotations;
    }

    public void setAnnotations( Collection<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    public String getAuthorList() {
        return this.authorList;
    }

    public void setAuthorList( String authorList ) {
        this.authorList = authorList;
    }

    public Collection<Compound> getChemicals() {
        return this.chemicals;
    }

    public void setChemicals( Collection<ubic.gemma.model.expression.biomaterial.Compound> chemicals ) {
        this.chemicals = chemicals;
    }

    /**
     * @return The citation as a pre-composed string
     */
    public String getCitation() {
        return this.citation;
    }

    public void setCitation( String citation ) {
        this.citation = citation;
    }

    public String getEditor() {
        return this.editor;
    }

    public void setEditor( String editor ) {
        this.editor = editor;
    }

    public LocalFile getFullTextPdf() {
        return this.fullTextPdf;
    }

    public void setFullTextPdf( LocalFile fullTextPdf ) {
        this.fullTextPdf = fullTextPdf;
    }

    /**
     * @return URI of the full text on the publisher's web site.
     */
    public String getFullTextUri() {
        return this.fullTextUri;
    }

    public void setFullTextUri( String fullTextUri ) {
        this.fullTextUri = fullTextUri;
    }

    public String getIssue() {
        return this.issue;
    }

    public void setIssue( String issue ) {
        this.issue = issue;
    }

    public Collection<Keyword> getKeywords() {
        return this.keywords;
    }

    public void setKeywords( Collection<Keyword> keywords ) {
        this.keywords = keywords;
    }

    public Collection<MedicalSubjectHeading> getMeshTerms() {
        return this.meshTerms;
    }

    public void setMeshTerms( Collection<MedicalSubjectHeading> meshTerms ) {
        this.meshTerms = meshTerms;
    }

    public String getPages() {
        return this.pages;
    }

    public void setPages( String pages ) {
        this.pages = pages;
    }

    public DatabaseEntry getPubAccession() {
        return this.pubAccession;
    }

    public void setPubAccession( DatabaseEntry pubAccession ) {
        this.pubAccession = pubAccession;
    }

    public String getPublication() {
        return this.publication;
    }

    public void setPublication( String publication ) {
        this.publication = publication;
    }

    public Date getPublicationDate() {
        return this.publicationDate;
    }

    public void setPublicationDate( Date publicationDate ) {
        this.publicationDate = publicationDate;
    }

    public Collection<PublicationType> getPublicationTypes() {
        return this.publicationTypes;
    }

    public void setPublicationTypes( Collection<PublicationType> publicationTypes ) {
        this.publicationTypes = publicationTypes;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getVolume() {
        return this.volume;
    }

    public void setVolume( String volume ) {
        this.volume = volume;
    }

    public static final class Factory {
        public static BibliographicReference newInstance() {
            return new BibliographicReference();
        }
    }

}