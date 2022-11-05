/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface AnnotationAssociationService extends BaseService<AnnotationAssociation> {

    @Override
    @Secured({ "GROUP_USER" })
    void create( AnnotationAssociation annotationAssociation );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Collection<AnnotationAssociation> anCollection );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( AnnotationAssociation annotationAssociation );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<AnnotationAssociation> anCollection );

    @Override
    @Secured({ "GROUP_USER" })
    void update( AnnotationAssociation annotationAssociation );

    Collection<AnnotationAssociation> find( BioSequence bioSequence );

    Collection<AnnotationAssociation> find( Gene gene );

    void thaw( AnnotationAssociation annotationAssociation );

    void thaw( Collection<AnnotationAssociation> anCollection );

    Collection<AnnotationValueObject> removeRootTerms( Collection<AnnotationValueObject> associations );
}
