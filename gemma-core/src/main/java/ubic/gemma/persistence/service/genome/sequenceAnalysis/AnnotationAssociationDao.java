/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.BaseDao;

/**
 * @author paul
 */
public interface AnnotationAssociationDao extends BaseDao<AnnotationAssociation>{

    Collection<AnnotationAssociation> find( BioSequence bioSequence );

    Collection<AnnotationAssociation> find( Gene gene );

    void thaw( Collection<AnnotationAssociation> anCollection );

    Collection<AnnotationAssociation> find( Collection<GeneProduct> gps );

}
