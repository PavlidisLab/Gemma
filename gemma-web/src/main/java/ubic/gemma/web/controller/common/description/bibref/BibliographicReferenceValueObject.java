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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.common.description.PublicationType;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceValueObject {

    BibliographicReference bibRef;

    Collection<ExpressionExperiment> experiments = new HashSet<ExpressionExperiment>();

    public BibliographicReferenceValueObject() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( bibRef == null ) ? 0 : bibRef.hashCode() );
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        BibliographicReferenceValueObject other = ( BibliographicReferenceValueObject ) obj;
        if ( bibRef == null ) {
            if ( other.bibRef != null ) return false;
        } else if ( !bibRef.equals( other.bibRef ) ) return false;
        return true;
    }

    public BibliographicReferenceValueObject( BibliographicReference bibRef ) {
        super();
        this.bibRef = bibRef;
    }

    public String getAbstractText() {
        return bibRef.getAbstractText();
    }

    public String getAnnotatedAbstract() {
        return bibRef.getAnnotatedAbstract();
    }

    public Collection<Characteristic> getAnnotations() {
        return bibRef.getAnnotations();
    }

    public AuditTrail getAuditTrail() {
        return bibRef.getAuditTrail();
    }

    public String getAuthorList() {
        return bibRef.getAuthorList();
    }

    public Collection<Person> getAuthors() {
        return bibRef.getAuthors();
    }

    public BibliographicReference getBibRef() {
        return bibRef;
    }

    public Collection<Compound> getChemicals() {
        return bibRef.getChemicals();
    }

    public String getCitation() {
        return bibRef.getCitation();
    }

    public String getDescription() {
        return bibRef.getDescription();
    }

    public String getEditor() {
        return bibRef.getEditor();
    }

    public Collection<ExpressionExperiment> getExperiments() {
        return experiments;
    }

    public LocalFile getFullTextPDF() {
        return bibRef.getFullTextPDF();
    }

    public String getFullTextURI() {
        return bibRef.getFullTextURI();
    }

    public Long getId() {
        return bibRef.getId();
    }

    public String getIssue() {
        return bibRef.getIssue();
    }

    public Collection<Keyword> getKeywords() {
        return bibRef.getKeywords();
    }

    public Collection<MedicalSubjectHeading> getMeshTerms() {
        return bibRef.getMeshTerms();
    }

    public String getName() {
        return bibRef.getName();
    }

    public String getPages() {
        return bibRef.getPages();
    }

    public DatabaseEntry getPubAccession() {
        return bibRef.getPubAccession();
    }

    public String getPublication() {
        return bibRef.getPublication();
    }

    public Date getPublicationDate() {
        return bibRef.getPublicationDate();
    }

    public Collection<PublicationType> getPublicationTypes() {
        return bibRef.getPublicationTypes();
    }

    public String getPublisher() {
        return bibRef.getPublisher();
    }

    public String getTitle() {
        return bibRef.getTitle();
    }

    public String getVolume() {
        return bibRef.getVolume();
    }

    public void setBibRef( BibliographicReference bibRef ) {
        this.bibRef = bibRef;
    }

    public void setExperiments( Collection<ExpressionExperiment> experiments ) {
        this.experiments = experiments;
    }

}
