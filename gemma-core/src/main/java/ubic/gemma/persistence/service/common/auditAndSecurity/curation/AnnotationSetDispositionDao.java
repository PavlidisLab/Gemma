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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetDisposition;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DAO for {@link AnnotationSetDisposition} rows — a curator's rulings on the
 * individual findings inside an audit set.
 *
 * <p>These rows are append-only, so every read here comes in two flavours: the
 * full log, and the latest-wins fold that answers "what is the standing ruling
 * on this finding". Callers that want the second must not reach for the first
 * and take its head per target by hand — the ordering tiebreaker lives in one
 * place for a reason.</p>
 */
public interface AnnotationSetDispositionDao extends BaseDao<AnnotationSetDisposition> {

    /**
     * Every ruling on one set, most recent first — the full log, including
     * rulings that have since been superseded.
     */
    List<AnnotationSetDisposition> findBySet( AnnotationSet annotationSet );

    /**
     * Every ruling on one finding, most recent first. The head is the standing
     * ruling.
     */
    List<AnnotationSetDisposition> findBySetAndTarget( AnnotationSet annotationSet, String targetId );

    /**
     * The standing ruling for each finding in one set.
     *
     * @return target id -> its latest ruling; findings nobody has ruled on are
     *         absent rather than mapped to null.
     */
    Map<String, AnnotationSetDisposition> findLatestBySet( AnnotationSet annotationSet );

    /**
     * Batched {@link #findLatestBySet}, for a list view that would otherwise
     * ask once per set.
     * <p>
     * One query for the whole page rather than N: the per-dataset annotation
     * set list routinely returns every set on a dataset, and folding
     * dispositions in one set at a time turns that list into an N+1.
     *
     * @return annotation set id -> (target id -> latest ruling). Sets with no
     *         rulings are absent from the outer map.
     */
    Map<Long, Map<String, AnnotationSetDisposition>> findLatestBySetIds( Collection<Long> annotationSetIds );
}
