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

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Read access to the relations Gemma knows between annotation terms.
 *
 * <p>Three consumers, and they want three different shapes of the same rows:</p>
 *
 * <ul>
 * <li><b>Display</b> — an experiment page captioning what its own annotations imply.
 * {@link #findRelationsForExperiment} seeds from the experiment's annotations and answers "what does
 * this dataset's genotype stand for?"</li>
 * <li><b>Browse and search</b> — a disease selector that must still return a dataset annotated only
 * as {@code Chd8} mutant. {@link #findRelatedTermsForSearch} is the expansion primitive.</li>
 * <li><b>Suppression</b> — a producer asking whether the tag it is about to write is already implied.
 * Also {@link #findRelatedTermsForSearch}, read as set membership.</li>
 * </ul>
 *
 * <p>🛑 <b>The suppression consumer must exclude the experiment under test.</b> The curation pipeline
 * is gold-blind: it never sees the current experiment's own curation, and a gate that read the
 * experiment's own statements would be reading the very thing being evaluated. Peer precedent from the
 * rest of the corpus is the honest source, which is what {@code excludedExperimentIds} is for. It is
 * also what makes "this tag is inferable, so it can be dropped" a claim rather than a circle: hold the
 * dataset out and ask whether the rest of the corpus still recovers the disease.</p>
 */
public interface AnnotationRelationService {

    /**
     * The ranked, evidenced read. See {@link AnnotationRelationDao#findRelations}.
     */
    List<AnnotationRelationDao.RelationSummary> findRelations( AnnotationRelationDao.RelationQuery query );

    /**
     * What this experiment's own annotations imply, for display beside them.
     *
     * <p>Seeds from every annotation the experiment carries and reports what each stands in relation
     * to, with the experiment itself held out of the evidence — a dataset shown its own annotation as
     * support for itself is showing a tautology.</p>
     *
     * @param direction which way to read: what the experiment's terms stand for, or what stands for
     *                  them
     */
    List<AnnotationRelationDao.RelationSummary> findRelationsForExperiment( ExpressionExperiment ee,
            AnnotationRelationDao.Direction direction, Set<AnnotationRelationBasis> bases, int maxResults );

    /**
     * Terms related to the seeds, for widening a query or testing membership.
     *
     * <p>No ranking and no ambiguity resolution, on purpose: SURF1's three diseases all belong in the
     * set, and a caller testing membership gets the right answer whichever one is meant. A caller that
     * needs to <i>choose</i> one wants {@link #findRelations} and its evidence instead.</p>
     */
    List<String[]> findRelatedTermsForSearch( Collection<String> seedValueUris, Collection<String> seedValues,
            AnnotationRelationDao.Direction direction, Set<AnnotationRelationBasis> bases,
            @Nullable Long taxonId, Collection<Long> excludedExperimentIds, int maximumObjectBreadth,
            int maxResults );
}
