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
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
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

    public void setClassName( String ontologyClass ) {
        this.className = ontologyClass;
    }

    public String getClassUri() {
        return classUri;
    }

    public void setClassUri( String classUri ) {
        this.classUri = classUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass( String objectClass ) {
        this.objectClass = objectClass;
    }

    public String getParentDescription() {
        return parentDescription;
    }

    public void setParentDescription( String parentDescription ) {
        this.parentDescription = parentDescription;
    }

    public String getParentLink() {
        return parentLink;
    }

    public void setParentLink( String parentLink ) {
        this.parentLink = parentLink;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName( String parentName ) {
        this.parentName = parentName;
    }

    public String getParentOfParentDescription() {
        return parentOfParentDescription;
    }

    public void setParentOfParentDescription( String parentOfParentDescription ) {
        this.parentOfParentDescription = parentOfParentDescription;
    }

    public String getParentOfParentLink() {
        return parentOfParentLink;
    }

    public void setParentOfParentLink( String parentOfParentLink ) {
        this.parentOfParentLink = parentOfParentLink;
    }

    public String getParentOfParentName() {
        return parentOfParentName;
    }

    public void setParentOfParentName( String parentOfParentName ) {
        this.parentOfParentName = parentOfParentName;
    }

    public String getTermName() {
        return termName;
    }

    public void setTermName( String ontologyTerm ) {
        this.termName = ontologyTerm;
    }

    public String getTermUri() {
        return termUri;
    }

    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }
}
