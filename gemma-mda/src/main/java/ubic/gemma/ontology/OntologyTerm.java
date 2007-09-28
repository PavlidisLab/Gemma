/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.ontology;

import java.util.Collection;

import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * @author Paul
 * @version $Id$
 */
public interface OntologyTerm extends OntologyResource {

    public String getTerm();

    public String getUri();

    public Object getModel();

    public ExternalDatabase getSourceOntology();

    /**
     * Note that any restriction superclasses are not returned.
     * 
     * @param direct
     * @return
     */
    public Collection<OntologyTerm> getParents( boolean direct );

    public Collection<OntologyTerm> getChildren( boolean direct );

    public Collection<OntologyIndividual> getIndividuals( boolean direct );

    public Collection<OntologyRestriction> getRestrictions();

    public Collection<AnnotationProperty> getAnnotations();

    public Collection<OntologyIndividual> getIndividuals();
    
    public Collection<String> getAlternativeIds();
    
    public String getComment();

    public boolean isRoot();
}
