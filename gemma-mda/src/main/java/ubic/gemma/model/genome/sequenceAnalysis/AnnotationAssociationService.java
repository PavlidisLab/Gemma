/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author paul
 * @version $Id$
 */
public interface AnnotationAssociationService {

    @Secured( { "GROUP_USER" })
    public AnnotationAssociation create( AnnotationAssociation annotationAssociation );

    @Secured( { "GROUP_USER" })
    public Collection<AnnotationAssociation> create( Collection<AnnotationAssociation> anCollection );

    public Collection<AnnotationAssociation> find( BioSequence bioSequence );

    public Collection<AnnotationAssociation> find( Gene gene );

    public Collection<AnnotationAssociation> load( Collection<Long> id );

    public AnnotationAssociation load( Long id );

    @Secured( { "GROUP_USER" })
    public void remove( AnnotationAssociation annotationAssociation );

    @Secured( { "GROUP_USER" })
    public void remove( Collection<AnnotationAssociation> anCollection );

    public void thaw( AnnotationAssociation annotationAssociation );

    public void thaw( Collection<AnnotationAssociation> anCollection );

    @Secured( { "GROUP_USER" })
    public void update( AnnotationAssociation annotationAssociation );

    @Secured( { "GROUP_USER" })
    public void update( Collection<AnnotationAssociation> anCollection );

    /**
     * Remove root terms, like "molecular_function", "biological_process" and "cellular_component"
     * 
     * @param associations
     */
    Collection<AnnotationValueObject> removeRootTerms( Collection<AnnotationValueObject> associations );
}
