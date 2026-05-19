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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Thaws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.Thaws.thawBioMaterial;

/**
 * Implementation of {@link BioMaterialReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link BioMaterialService} interface — this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see BioMaterialService
 */
@Service("bioMaterialReadService")
public class BioMaterialReadServiceImpl implements BioMaterialReadService {

    private final BioMaterialDao bioMaterialDao;

    @Autowired
    public BioMaterialReadServiceImpl( BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    @Override
    @Transactional(readOnly = true)
    public BioMaterial copy( BioMaterial bioMaterial ) {
        return this.bioMaterialDao.copy( bioMaterial );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct ) {
        return bioMaterialDao.findSubBioMaterials( bioMaterial, direct );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findSiblings( BioMaterial bioMaterial ) {
        if ( bioMaterial.getSourceBioMaterial() == null ) {
            return Collections.emptySet();
        }
        Collection<BioMaterial> siblings = findSubBioMaterials( bioMaterial.getSourceBioMaterial(), true );
        siblings.remove( bioMaterial );
        return siblings;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return this.bioMaterialDao.findByExperiment( experiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor ) {
        return this.bioMaterialDao.findByFactor( experimentalFactor );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> BioMaterial loadAndThawOrFail( Long bmId, Function<String, T> exceptionSupplier, String message ) throws T {
        BioMaterial bm = bioMaterialDao.load( bmId );
        if ( bm == null ) {
            throw exceptionSupplier.apply( message );
        }
        thawBioMaterial( bm );
        return bm;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm ) {
        // source biomaterials need to be visited, so this must be in the session
        bm = ensureInSession( bm );
        return this.bioMaterialDao.getExpressionExperiments( bm );
    }

    @Override
    @Transactional(readOnly = true)
    public BioMaterial thaw( BioMaterial bioMaterial ) {
        bioMaterial = ensureInSession( bioMaterial );
        thawBioMaterial( bioMaterial );
        return bioMaterial;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        bioMaterials = ensureInSession( bioMaterials );
        bioMaterials.forEach( Thaws::thawBioMaterial );
        return bioMaterials;
    }

    /**
     * Local re-implementation of {@code AbstractService#ensureInSession} so this service
     * doesn't have to extend the heavier base class. Matches the deprecated base-class
     * semantics: null-tolerant, transient-tolerant, otherwise re-loads by ID from the DAO.
     */
    private BioMaterial ensureInSession( BioMaterial entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( bioMaterialDao.load( id ),
                String.format( "No BioMaterial with ID %d.", id ) );
    }

    private Collection<BioMaterial> ensureInSession( Collection<BioMaterial> entities ) {
        if ( entities == null ) {
            return Collections.emptyList();
        }
        Collection<BioMaterial> result = new ArrayList<>( entities.size() );
        for ( BioMaterial e : entities ) {
            BioMaterial se = ensureInSession( e );
            if ( se != null ) {
                result.add( se );
            }
        }
        return result;
    }
}
