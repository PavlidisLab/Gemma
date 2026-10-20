/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation
 */
public interface BlatAssociationDao extends BaseDao<BlatAssociation> {

    Collection<BlatAssociation> find( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    Collection<BlatAssociation> find( ubic.gemma.model.genome.Gene gene );

    void thaw( Collection<BlatAssociation> blatAssociations );

    void thaw( BlatAssociation blatAssociation );

    Collection<BlatAssociation> find( Collection<GeneProduct> toRemove );

}
