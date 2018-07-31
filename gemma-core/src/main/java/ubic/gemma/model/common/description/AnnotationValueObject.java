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

    @Override public String toString() {
        return "AnnotationValueObject{" +
                "classUri='" + classUri + '\'' +
                ", className='" + className + '\'' +
                ", termUri='" + termUri + '\'' +
                ", termName='" + termName + '\'' +
                '}';
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        if ( this.id != null ) return id.hashCode();

//        result = prime * result + ( ( className == null ) ? 0 : className.hashCode() );
//        result = prime * result + ( ( classUri == null ) ? 0 : classUri.hashCode() );
 //       result = prime * result + ( ( objectClass == null ) ? 0 : objectClass.hashCode() );
        result = prime * result + ( ( termUri == null ) ? 0 : termUri.hashCode() );
        result = prime * result + ( ( termName == null ) ? 0 : termName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        AnnotationValueObject other = ( AnnotationValueObject ) obj;

        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        } else if ( !id.equals( other.id ) ) {
            return false;
        }

//        if ( className == null ) {
//            if ( other.className != null ) {
//                return false;
//            }
//        } else if ( !className.equals( other.className ) ) {
//            return false;
//        }
//        if ( classUri == null ) {
//            if ( other.classUri != null ) {
//                return false;
//            }
//        } else if ( !classUri.equals( other.classUri ) ) {
//            return false;
//        }

//        if ( objectClass == null ) {
//            if ( other.objectClass != null ) {
//                return false;
//            }
//        } else if ( !objectClass.equals( other.objectClass ) ) {
//            return false;
//        }
        if ( termUri == null ) {
            if ( other.termUri != null ) {
                return false;
            }
        } else if ( !termUri.equals( other.termUri ) ) {
            return false;
        }

        if ( termName == null ) {
            if ( other.termName != null ) {
                return false;
            }
        } else if ( !termName.equals( other.termName ) ) {
            return false;
        }

        return true;
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
