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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.expression.biomaterial.Compound;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Hibernate Search 7 indexed root.
 */
@Entity
@Table(name = "BIBLIOGRAPHIC_REFERENCE")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Indexed
public class BibliographicReference extends AbstractDescribable {

    @Lob
    @Column(name = "AUTHOR_LIST", columnDefinition = "text")
    private String authorList;

    @Lob
    @Column(name = "TITLE", columnDefinition = "text")
    private String title;

    @Column(name = "PUBLISHER", columnDefinition = "VARCHAR(255)")
    private String publisher;

    @Column(name = "EDITOR", columnDefinition = "VARCHAR(255)")
    private String editor;

    @Column(name = "VOLUME", columnDefinition = "VARCHAR(255)")
    private String volume;

    @Column(name = "ISSUE", columnDefinition = "VARCHAR(255)")
    private String issue;

    @Column(name = "PAGES", columnDefinition = "VARCHAR(255)")
    private String pages;

    @Column(name = "PUBLICATION", columnDefinition = "VARCHAR(255)")
    private String publication;

    @Column(name = "FULL_TEXT_URI", columnDefinition = "VARCHAR(255)")
    private String fullTextUri;

    @Lob
    @Column(name = "ABSTRACT_TEXT", columnDefinition = "text")
    private String abstractText;

    @Lob
    @Column(name = "CITATION", columnDefinition = "text")
    private String citation;

    @Column(name = "PUBLICATION_DATE", columnDefinition = "DATE")
    private Date publicationDate;

    @Deprecated
    @Lob
    @Column(name = "ANNOTATED_ABSTRACT", columnDefinition = "text")
    private String annotatedAbstract;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "PUB_ACCESSION_FK", unique = true, columnDefinition = "BIGINT")
    private DatabaseEntry pubAccession;

    @Column(name = "RETRACTED", columnDefinition = "TINYINT")
    private Boolean retracted = false;

    /**
     * @deprecated never used
     */
    @Deprecated
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "BIBLIOGRAPHIC_REFERENCE_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "CHARACTERISTIC_BIBLIOGRAPHIC_REFERENCE_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Characteristic> annotations = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "MESH_BIB_REF_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "MEDICAL_SUBJECT_HEADING_MESH_BIB_REF_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<MedicalSubjectHeading> meshTerms = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "KEYWORD_BIB_REF_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "KEYWORD_KEYWORD_BIB_REF_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Keyword> keywords = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "BIBLIOGRAPHIC_REFERENCE_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "COMPOUND_BIBLIOGRAPHIC_REFERENCE_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Compound> chemicals = new HashSet<>();

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BibliographicReference ) )
            return false;
        BibliographicReference that = ( BibliographicReference ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return false;
    }

    @Override
    @FullTextField
    public String getName() {
        return super.getName();
    }

    @FullTextField(projectable = Projectable.YES)
    public String getAbstractText() {
        return this.abstractText;
    }

    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }

    /**
     * @return A version of the abstract with inserted markup (e.g., abbreviation expansions, part-of-speech)
     */
    @Deprecated
    public String getAnnotatedAbstract() {
        return this.annotatedAbstract;
    }

    @Deprecated
    public void setAnnotatedAbstract( String annotatedAbstract ) {
        this.annotatedAbstract = annotatedAbstract;
    }

    @Deprecated
    public Set<Characteristic> getAnnotations() {
        return this.annotations;
    }

    @Deprecated
    public void setAnnotations( Set<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    @FullTextField(projectable = Projectable.YES)
    public String getAuthorList() {
        return this.authorList;
    }

    public void setAuthorList( String authorList ) {
        this.authorList = authorList;
    }

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<Compound> getChemicals() {
        return this.chemicals;
    }

    public void setChemicals( Set<ubic.gemma.model.expression.biomaterial.Compound> chemicals ) {
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

    /**
     * @return URI of the full text on the publisher's web site.
     */
    @KeywordField
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

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<Keyword> getKeywords() {
        return this.keywords;
    }

    public void setKeywords( Set<Keyword> keywords ) {
        this.keywords = keywords;
    }

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<MedicalSubjectHeading> getMeshTerms() {
        return this.meshTerms;
    }

    public void setMeshTerms( Set<MedicalSubjectHeading> meshTerms ) {
        this.meshTerms = meshTerms;
    }

    public String getPages() {
        return this.pages;
    }

    public void setPages( String pages ) {
        this.pages = pages;
    }

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
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

    public String getPublisher() {
        return this.publisher;
    }

    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    @FullTextField(projectable = Projectable.YES)
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

    /**
     * @return true if article is recorded as retracted
     */
    public Boolean getRetracted() {
        return retracted;
    }

    /**
     * @param retracted the retracted to set
     */
    public void setRetracted( Boolean retracted ) {
        this.retracted = retracted;
    }

    public static final class Factory {
        public static BibliographicReference newInstance() {
            return new BibliographicReference();
        }
    }

}