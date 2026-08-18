/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Read-only service over {@link AnnotationRelationDao}.
 *
 * <p>No write path, deliberately. Every row in this table is derived, and the only writer is the
 * maintenance rebuild in {@code TableMaintenanceUtil}. A service-level write would let a caller assert
 * a relation that no basis supports, which is the one thing the {@code BASIS} column exists to make
 * impossible.</p>
 */
@Service
public class AnnotationRelationServiceImpl implements AnnotationRelationService {

    private final AnnotationRelationDao annotationRelationDao;

    @Autowired
    public AnnotationRelationServiceImpl( AnnotationRelationDao annotationRelationDao ) {
        this.annotationRelationDao = annotationRelationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationRelationDao.RelationSummary> findRelations( AnnotationRelationDao.RelationQuery query ) {
        return annotationRelationDao.findRelations( query );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationRelationDao.RelationSummary> findRelationsForExperiment( ExpressionExperiment ee,
            AnnotationRelationDao.Direction direction, Set<AnnotationRelationBasis> bases, int maxResults ) {
        if ( ee.getId() == null ) {
            return Collections.emptyList();
        }
        AnnotationRelationDao.RelationQuery q = new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( ee.getId() )
                .seedDirection( direction )
                // Hold the experiment out of its own evidence. Counting a dataset's annotation as
                // support for what that same annotation implies is circular, and it is exactly the
                // number a curator would read as independent corroboration.
                .excludedExperimentIds( Collections.singleton( ee.getId() ) )
                .bases( bases )
                .maxResults( maxResults );
        if ( ee.getTaxon() != null && ee.getTaxon().getId() != null ) {
            q.taxonId( ee.getTaxon().getId() );
        }
        return annotationRelationDao.findRelations( q );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String[]> findRelatedTermsForSearch( Collection<String> seedValueUris, Collection<String> seedValues,
            AnnotationRelationDao.Direction direction, Set<AnnotationRelationBasis> bases,
            @Nullable Long taxonId, Collection<Long> excludedExperimentIds, int maxResults ) {
        return annotationRelationDao.findRelatedTerms( seedValueUris, seedValues, direction, bases,
                Collections.emptySet(), taxonId, excludedExperimentIds, maxResults );
    }
}
