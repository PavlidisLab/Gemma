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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialReadService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.Thaws.thawBioAssay;

/**
 * Implementation of {@link BioAssayReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link BioAssayService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see BioAssayService
 */
@Service("bioAssayReadService")
public class BioAssayReadServiceImpl implements BioAssayReadService {

    private final BioAssayDao bioAssayDao;
    private final ArrayDesignDao arrayDesignDao;
    private final BioMaterialReadService bioMaterialReadService;

    @Autowired
    public BioAssayReadServiceImpl( BioAssayDao bioAssayDao, ArrayDesignDao arrayDesignDao, BioMaterialReadService bioMaterialReadService ) {
        this.bioAssayDao = bioAssayDao;
        this.arrayDesignDao = arrayDesignDao;
        this.bioMaterialReadService = bioMaterialReadService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay ) {
        if ( bioAssay.getId() == null )
            throw new IllegalArgumentException( "BioAssay must be persistent" );
        return this.bioAssayDao.findBioAssayDimensions( bioAssay );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public BioAssay findByShortName( String shortName ) {
        return bioAssayDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findByAccession( String accession ) {
        return this.bioAssayDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findSubBioAssays( BioAssay bioAssay, boolean direct ) {
        Collection<BioMaterial> bms = bioMaterialReadService.findSubBioMaterials( bioAssay.getSampleUsed(), direct );
        return bms.stream().map( BioMaterial::getBioAssaysUsedIn ).flatMap( Collection::stream ).collect( Collectors.toSet() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findSiblings( BioAssay bioAssay ) {
        Collection<BioMaterial> bms = bioMaterialReadService.findSiblings( bioAssay.getSampleUsed() );
        return bms.stream().map( BioMaterial::getBioAssaysUsedIn ).flatMap( Collection::stream ).collect( Collectors.toSet() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay ) {
        return bioAssayDao.getBioAssaySets( bioAssay );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssay thaw( BioAssay ba ) {
        ba = ensureInSession( ba );
        thawBioAssay( ba );
        return ba;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        bioAssays = ensureInSession( bioAssays );
        for ( BioAssay ba : bioAssays ) {
            thawBioAssay( ba );
        }
        return bioAssays;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues ) {
        Map<Long, ArrayDesign> arrayDesigns = new HashMap<>();
        for ( BioAssay ba : entities ) {
            arrayDesigns.put( ba.getArrayDesignUsed().getId(), ba.getArrayDesignUsed() );
            if ( ba.getOriginalPlatform() != null ) {
                arrayDesigns.put( ba.getOriginalPlatform().getId(), ba.getOriginalPlatform() );
            }
        }
        Map<ArrayDesign, ArrayDesignValueObject> ba2vo = arrayDesignDao.loadValueObjects( arrayDesigns.values() )
                .stream()
                .collect( Collectors.toMap( vo -> arrayDesigns.get( vo.getId() ), Function.identity() ) );
        return bioAssayDao.loadValueObjects( entities, ba2vo, assay2sourceAssayMap, basic, allFactorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<BioAssayValueObject> loadValueObjectsByCursorForExpressionExperiment(
            ExpressionExperiment ee, @Nullable Cursor cursor, int limit ) {
        return bioAssayDao.loadValueObjectsByCursorForExpressionExperiment( ee, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<BioAssayValueObject> loadValueObjectsByCursorForSubSet(
            ExpressionExperimentSubSet subset, @Nullable Cursor cursor, int limit ) {
        return bioAssayDao.loadValueObjectsByCursorForSubSet( subset, cursor, limit );
    }

    /**
     * Local re-implementation of {@code AbstractService#ensureInSession} so this service
     * doesn't have to extend the heavier base class. Matches the deprecated base-class
     * semantics: null-tolerant, transient-tolerant, otherwise re-loads by ID from the DAO.
     */
    private BioAssay ensureInSession( BioAssay entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( bioAssayDao.load( id ),
                String.format( "No BioAssay with ID %d.", id ) );
    }

    private Collection<BioAssay> ensureInSession( Collection<BioAssay> entities ) {
        if ( entities == null ) {
            return Collections.emptyList();
        }
        Collection<BioAssay> result = new ArrayList<>( entities.size() );
        for ( BioAssay e : entities ) {
            BioAssay se = ensureInSession( e );
            if ( se != null ) {
                result.add( se );
            }
        }
        return result;
    }
}
