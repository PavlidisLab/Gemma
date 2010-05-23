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

/**
 * @author luke
 * @version $Id$
 */
public class AnnotationValueObject {

    private Long id;
    private String classUri;
    private String className;
    private String termUri;
    private String termName;
    private String parentName;
    private String parentDescription;
    private String parentLink;
    private String parentOfParentName;
    private String parentOfParentDescription;
    private String parentOfParentLink;
    private String description;
    private String evidenceCode;
    private String objectClass;

    public AnnotationValueObject() {
    }

    public String getClassName() {
        return className;
    }

    /**
     * @return the classUri
     */
    public String getClassUri() {
        return classUri;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public String getObjectClass() {
        return objectClass;
    }

    /**
     * @return the parentDescription
     */
    public String getParentDescription() {
        return parentDescription;
    }

    /**
     * @return the parentLink
     */
    public String getParentLink() {
        return parentLink;
    }

    /**
     * @return the parentName
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * @return the parentOfParentDescription
     */
    public String getParentOfParentDescription() {
        return parentOfParentDescription;
    }

    /**
     * @return the parentOfParentLink
     */
    public String getParentOfParentLink() {
        return parentOfParentLink;
    }

    /**
     * @return the parentOfParentName
     */
    public String getParentOfParentName() {
        return parentOfParentName;
    }

    public String getTermName() {
        return termName;
    }

    /**
     * @return the termUri
     */
    public String getTermUri() {
        return termUri;
    }

    public void setClassName( String ontologyClass ) {
        this.className = ontologyClass;
    }

    /**
     * @param classUri the classUri to set
     */
    public void setClassUri( String classUri ) {
        this.classUri = classUri;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    /**
     * @param id the id to set
     */
    public void setId( Long id ) {
        this.id = id;
    }

    public void setObjectClass( String objectClass ) {
        this.objectClass = objectClass;
    }

    /**
     * @param parentDescription the parentDescription to set
     */
    public void setParentDescription( String parentDescription ) {
        this.parentDescription = parentDescription;
    }

    /**
     * @param parentLink the parentLink to set
     */
    public void setParentLink( String parentLink ) {
        this.parentLink = parentLink;
    }

    /**
     * @param parentDescription the parentDescription to set
     */
    public void setParentName( String parentName ) {
        this.parentName = parentName;
    }

    /**
     * @param parentOfParentDescription the parentOfParentDescription to set
     */
    public void setParentOfParentDescription( String parentOfParentDescription ) {
        this.parentOfParentDescription = parentOfParentDescription;
    }

    /**
     * @param parentOfParentLink the parentOfParentLink to set
     */
    public void setParentOfParentLink( String parentOfParentLink ) {
        this.parentOfParentLink = parentOfParentLink;
    }

    /**
     * @param parentOfParentName the parentOfParentName to set
     */
    public void setParentOfParentName( String parentOfParentName ) {
        this.parentOfParentName = parentOfParentName;
    }

    public void setTermName( String ontologyTerm ) {
        this.termName = ontologyTerm;
    }

    /**
     * @param termUri the termUri to set
     */
    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }
}
