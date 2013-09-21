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

import java.util.Collection;

/** 
 */
public abstract class BibliographicReference extends ubic.gemma.model.common.Auditable {

    /**
     * Constructs new instances of {@link BibliographicReference}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link BibliographicReference}.
         */
        public static BibliographicReference newInstance() {
            return new BibliographicReferenceImpl();
        }

    }

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

    private java.util.Date publicationDate;

    private String annotatedAbstract;

    private DatabaseEntry pubAccession;

    private LocalFile fullTextPdf;

    private Collection<Characteristic> annotations = new java.util.HashSet<>();

    private Collection<MedicalSubjectHeading> meshTerms = new java.util.HashSet<>();

    private Collection<PublicationType> publicationTypes = new java.util.HashSet<>();

    private Collection<Keyword> keywords = new java.util.HashSet<Keyword>();

    private Collection<ubic.gemma.model.expression.biomaterial.Compound> chemicals = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BibliographicReference() {
    }

    /**
     * 
     */
    public String getAbstractText() {
        return this.abstractText;
    }

    /**
     * <p>
     * A version of the abstrace with inserted markup (e.g., abbreviation expansions, part-of-speech)
     * </p>
     */
    public String getAnnotatedAbstract() {
        return this.annotatedAbstract;
    }

    /**
     * 
     */
    public Collection<Characteristic> getAnnotations() {
        return this.annotations;
    }

    /**
     * 
     */
    public String getAuthorList() {
        return this.authorList;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.biomaterial.Compound> getChemicals() {
        return this.chemicals;
    }

    /**
     * <p>
     * <p>
     * The citation as a pre-composed string
     * </p>
     * </p>
     */
    public String getCitation() {
        return this.citation;
    }

    /**
     * 
     */
    public String getEditor() {
        return this.editor;
    }

    /**
     * 
     */
    public LocalFile getFullTextPdf() {
        return this.fullTextPdf;
    }

    /**
     * <p>
     * URI of the full text on the publisher's web site.
     * </p>
     */
    public String getFullTextUri() {
        return this.fullTextUri;
    }

    /**
     * 
     */
    public String getIssue() {
        return this.issue;
    }

    /**
     * 
     */
    public Collection<Keyword> getKeywords() {
        return this.keywords;
    }

    /**
     * 
     */
    public Collection<MedicalSubjectHeading> getMeshTerms() {
        return this.meshTerms;
    }

    /**
     * 
     */
    public String getPages() {
        return this.pages;
    }

    /**
     * 
     */
    public DatabaseEntry getPubAccession() {
        return this.pubAccession;
    }

    /**
     * 
     */
    public String getPublication() {
        return this.publication;
    }

    /**
     * 
     */
    public java.util.Date getPublicationDate() {
        return this.publicationDate;
    }

    /**
     * 
     */
    public Collection<PublicationType> getPublicationTypes() {
        return this.publicationTypes;
    }

    /**
     * 
     */
    public String getPublisher() {
        return this.publisher;
    }

    /**
     * 
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * 
     */
    public String getVolume() {
        return this.volume;
    }

    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }

    public void setAnnotatedAbstract( String annotatedAbstract ) {
        this.annotatedAbstract = annotatedAbstract;
    }

    public void setAnnotations( Collection<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    public void setAuthorList( String authorList ) {
        this.authorList = authorList;
    }

    public void setChemicals( Collection<ubic.gemma.model.expression.biomaterial.Compound> chemicals ) {
        this.chemicals = chemicals;
    }

    public void setCitation( String citation ) {
        this.citation = citation;
    }

    public void setEditor( String editor ) {
        this.editor = editor;
    }

    public void setFullTextPdf( LocalFile fullTextPdf ) {
        this.fullTextPdf = fullTextPdf;
    }

    public void setFullTextUri( String fullTextUri ) {
        this.fullTextUri = fullTextUri;
    }

    public void setIssue( String issue ) {
        this.issue = issue;
    }

    public void setKeywords( Collection<Keyword> keywords ) {
        this.keywords = keywords;
    }

    public void setMeshTerms( Collection<MedicalSubjectHeading> meshTerms ) {
        this.meshTerms = meshTerms;
    }

    public void setPages( String pages ) {
        this.pages = pages;
    }

    public void setPubAccession( DatabaseEntry pubAccession ) {
        this.pubAccession = pubAccession;
    }

    public void setPublication( String publication ) {
        this.publication = publication;
    }

    public void setPublicationDate( java.util.Date publicationDate ) {
        this.publicationDate = publicationDate;
    }

    public void setPublicationTypes( Collection<PublicationType> publicationTypes ) {
        this.publicationTypes = publicationTypes;
    }

    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public void setVolume( String volume ) {
        this.volume = volume;
    }

}