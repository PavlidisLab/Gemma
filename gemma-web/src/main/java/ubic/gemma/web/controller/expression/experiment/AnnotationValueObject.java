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
package ubic.gemma.web.controller.expression.experiment;


/**
 * @author luke
 */
public class AnnotationValueObject  {

    private long   id;
    private String classUri;
    private String className;
    private String termUri;
    private String termName;
    private String description;
    private String evidenceCode;
    
    
    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEvidenceCode() {
		return evidenceCode;
	}

	public void setEvidenceCode(String evidenceCode) {
		this.evidenceCode = evidenceCode;
	}

	public AnnotationValueObject() {
    }
    
    public String getClassName() { return className; }
    
    public void setClassName( String ontologyClass ) {
        this.className = ontologyClass;
    }
    
    public String getTermName() { return termName; }
    
    public void setTermName( String ontologyTerm ) {
        this.termName = ontologyTerm;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId( long id ) {
        this.id = id;
    }

    /**
     * @return the classUri
     */
    public String getClassUri() {
        return classUri;
    }

    /**
     * @param classUri the classUri to set
     */
    public void setClassUri( String classUri ) {
        this.classUri = classUri;
    }

    /**
     * @return the termUri
     */
    public String getTermUri() {
        return termUri;
    }

    /**
     * @param termUri the termUri to set
     */
    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }
}
