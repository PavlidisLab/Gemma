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

import org.hibernate.search.annotations.Indexed;

import java.util.HashSet;
import java.util.Set;

@Indexed
public class MedicalSubjectHeading extends BibRefAnnotation {

    private Set<MedicalSubjectHeading> qualifiers = new HashSet<>();

    public Set<MedicalSubjectHeading> getQualifiers() {
        return this.qualifiers;
    }

    public void setQualifiers( Set<MedicalSubjectHeading> qualifiers ) {
        this.qualifiers = qualifiers;
    }

    public static final class Factory {
        public static MedicalSubjectHeading newInstance() {
            return new MedicalSubjectHeading();
        }

    }

}