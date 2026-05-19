/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.designElement;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link CompositeSequenceReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link CompositeSequenceService} interface — this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see CompositeSequenceService
 */
@Service("compositeSequenceReadService")
public class CompositeSequenceReadServiceImpl implements CompositeSequenceReadService {

    private static final Logger log = LoggerFactory.getLogger( CompositeSequenceReadServiceImpl.class );

    private final CompositeSequenceDao compositeSequenceDao;

    @Autowired
    public CompositeSequenceReadServiceImpl( CompositeSequenceDao compositeSequenceDao ) {
        this.compositeSequenceDao = compositeSequenceDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence ) {
        return this.compositeSequenceDao.findByBioSequence( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequenceName( String name ) {
        return this.compositeSequenceDao.findByBioSequenceName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( Gene gene, boolean useGene2Cs ) {
        return this.compositeSequenceDao.findByGene( gene, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.compositeSequenceDao.findByGene( gene, arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, boolean useGene2Cs ) {
        return this.compositeSequenceDao.findByGenes( genes, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.compositeSequenceDao.findByGenes( genes, arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByName( String name ) {
        return this.compositeSequenceDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequence findByName( ArrayDesign arrayDesign, String name ) {
        return this.compositeSequenceDao.findByName( arrayDesign, name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns ) {
        LinkedHashMap<String, CompositeSequence> compositeSequencesMap = new LinkedHashMap<>();

        for ( ArrayDesign arrayDesign : arrayDesigns ) {
            for ( String obj : compositeSequenceNames ) {
                String name = StringUtils.strip( obj );
                log.debug( "entered: {}", name );
                CompositeSequence cs = this.compositeSequenceDao.findByName( arrayDesign, name );
                if ( cs != null && !compositeSequencesMap.containsKey( cs.getName() ) ) {
                    compositeSequencesMap.put( cs.getName(), cs );
                } else {
                    log.warn( "Composite sequence {} does not exist.  Discarding ... ", name );
                }
            }
        }

        if ( compositeSequencesMap.isEmpty() )
            return null;

        return compositeSequencesMap.values();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> sequences, boolean useGene2Cs ) {
        return this.compositeSequenceDao.getGenes( sequences, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenes( CompositeSequence compositeSequence, boolean useGene2Cs ) {
        return this.compositeSequenceDao.getGenes( compositeSequence, 0, -1, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit, boolean useGene2Cs ) {
        return this.compositeSequenceDao.getGenes( compositeSequence, offset, limit, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Gene> getGenesByCursor( CompositeSequence compositeSequence, @Nullable Cursor cursor, int limit, boolean useGene2Cs ) {
        return this.compositeSequenceDao.getGenesByCursor( compositeSequence, cursor, limit, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences ) {
        return this.compositeSequenceDao.getGenesWithSpecificity( compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences ) {
        return this.compositeSequenceDao.getRawSummary( compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults ) {
        return this.compositeSequenceDao.getRawSummary( arrayDesign, numResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> thaw( Collection<CompositeSequence> compositeSequences ) {
        compositeSequences = this.compositeSequenceDao.load(
                compositeSequences.stream().map( CompositeSequence::getId ).collect( Collectors.toSet() ) );
        this.compositeSequenceDao.thaw( compositeSequences );
        return compositeSequences;
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequence thaw( CompositeSequence compositeSequence ) {
        Long id = compositeSequence.getId();
        compositeSequence = requireNonNull( this.compositeSequenceDao.load( id ),
                String.format( "No CompositeSequence with ID %d.", id ) );
        this.compositeSequenceDao.thaw( compositeSequence );
        return compositeSequence;
    }
}
