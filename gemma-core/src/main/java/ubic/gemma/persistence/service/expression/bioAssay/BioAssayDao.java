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
package ubic.gemma.persistence.service.expression.bioAssay;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see BioAssay
 */
public interface BioAssayDao extends FilteringVoEnabledDao<BioAssay, BioAssayValueObject> {

    @Nullable
    BioAssay findByShortName( String shortName );

    Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    Collection<BioAssay> findByAccession( String accession );

    Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay );

    /**
     * @see BioAssayValueObject#BioAssayValueObject(BioAssay, Map, BioAssay, boolean, boolean)
     */
    List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities,
            @Nullable Map<ArrayDesign, ArrayDesignValueObject> ad2vo,
            @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap,
            boolean basic, boolean allFactorValues );

    /**
     * Cursor-paged listing of {@link BioAssayValueObject}s for a single
     * {@link ExpressionExperiment}, sorted by ascending {@code id} — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1k.
     * <p>
     * {@link BioAssayDao} extends the no-op filtering base class
     * ({@code AbstractNoopFilteringVoEnabledDao}) and therefore cannot reuse the standard
     * {@link FilteringVoEnabledDao#loadValueObjectsByCursor(ubic.gemma.persistence.util.Filters,
     * ubic.gemma.persistence.util.Sort, Cursor, int)} machinery — the unified Filter→HQL
     * compiler isn't wired through for BioAssays. We instead emit a focused keyset HQL
     * query that walks {@code ee.bioAssays} (the EE-scope is the path-derived constraint
     * for {@code GET /datasets/{dataset}/samples}). The cursor predicate is appended as
     * {@code ba.id > :cursor} (ASC; reversed for BACKWARD cursors); we fetch
     * {@code limit + 1} rows to detect the next page; {@code totalElements} is left
     * {@code null} (cursor mode skips the {@code COUNT(*)} per request, matching the rest
     * of the cursor surface).
     */
    CursorPage<BioAssayValueObject> loadValueObjectsByCursorForExpressionExperiment(
            ExpressionExperiment ee, @Nullable Cursor cursor, int limit );

    /**
     * Cursor-paged listing of {@link BioAssayValueObject}s for a single
     * {@link ExpressionExperimentSubSet}, sorted by ascending {@code id} — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1u (the subset-scoped twin of step
     * 1k).
     * <p>
     * Walks {@code subset.bioAssays} directly via a focused keyset HQL
     * ({@code from ExpressionExperimentSubSet bas join bas.bioAssays ba where bas.id = ?}),
     * matching the EE variant's structure and the same single-column ascending {@code id}
     * cursor restriction. The {@code assay2sourceAssayMap} (used to populate the VO's
     * {@code sourceBioAssayId}) is built post-hoc by the service layer against the
     * subset's source experiment so the VO shape matches the legacy offset-mode caller
     * exactly.
     */
    CursorPage<BioAssayValueObject> loadValueObjectsByCursorForSubSet(
            ExpressionExperimentSubSet subset, @Nullable Cursor cursor, int limit );
}
