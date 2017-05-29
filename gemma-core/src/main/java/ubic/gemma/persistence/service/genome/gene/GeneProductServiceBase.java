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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationDao;

import java.util.Collection;

/**
 * <p>
 * Spring Service base class for <code>GeneProductService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 *
 * @see GeneProductService
 */
public abstract class GeneProductServiceBase extends VoEnabledService<GeneProduct, GeneProductValueObject> implements GeneProductService {

    final AnnotationAssociationDao annotationAssociationDao;
    final BioSequenceDao bioSequenceDao;
    final BlatAssociationDao blatAssociationDao;
    final GeneProductDao geneProductDao;

    @Autowired
    public GeneProductServiceBase( AnnotationAssociationDao annotationAssociationDao, BioSequenceDao bioSequenceDao,
            BlatAssociationDao blatAssociationDao, GeneProductDao geneProductDao ) {
        super( geneProductDao );
        this.annotationAssociationDao = annotationAssociationDao;
        this.bioSequenceDao = bioSequenceDao;
        this.blatAssociationDao = blatAssociationDao;
        this.geneProductDao = geneProductDao;
    }

    /**
     * @see GeneProductService#getGenesByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );

    }

    /**
     * @see GeneProductService#getGenesByNcbiId(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByNcbiId( final java.lang.String search ) {
        return this.handleGetGenesByNcbiId( search );

    }

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByNcbiId( java.lang.String search );

}