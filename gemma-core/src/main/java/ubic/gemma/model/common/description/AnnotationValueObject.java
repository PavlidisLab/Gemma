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

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;

/**
 * @author luke
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Data
@EqualsAndHashCode(of = { "className", "classUri", "objectClass", "termUri", "termName" }, callSuper = true)
public class AnnotationValueObject extends IdentifiableValueObject<Characteristic> {

    private String classUri;
    private String className;
    private String termUri;
    private String termName;
    @GemmaWebOnly
    private String description;
    private String evidenceCode;
    private String objectClass;
    @GemmaWebOnly
    private String parentName;
    @GemmaWebOnly
    private String parentDescription;
    @GemmaWebOnly
    private String parentLink;
    @GemmaWebOnly
    private String parentOfParentName;
    @GemmaWebOnly
    private String parentOfParentDescription;
    @GemmaWebOnly
    private String parentOfParentLink;

    public AnnotationValueObject() {
        super();
    }

    public AnnotationValueObject( Long id ) {
        super( id );
    }

    public AnnotationValueObject( Characteristic c ) {
        super( c );
        classUri = c.getCategoryUri();
        className = c.getCategory();
        termUri = c.getValueUri();
        termName = c.getValue();
        // FIXME: should we use null instead of "" for missing evidence code?
        evidenceCode = c.getEvidenceCode() != null ? c.getEvidenceCode().toString() : "";
    }

    public AnnotationValueObject( Characteristic c, Class<?> objectClass ) {
        this( c );
        if ( ExpressionExperiment.class.isAssignableFrom( objectClass ) ) {
            this.objectClass = "ExperimentTag";
        } else {
            this.objectClass = objectClass.getSimpleName();
        }
    }

    @Override
    public String toString() {
        return "AnnotationValueObject{" +
                "classUri='" + classUri + '\'' +
                ", className='" + className + '\'' +
                ", termUri='" + termUri + '\'' +
                ", termName='" + termName + '\'' +
                '}';
    }
}
