package ubic.gemma.rest.util.args;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneReferenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformArgService extends AbstractEntityArgService<ArrayDesign, ArrayDesignService> {

    /**
     * Upper bound on the number of composite-sequence ids composed into a {@code ?gene=} filter's
     * {@code IN} clause. Sized well above any realistic gene-to-probe fan-out on a single platform
     * (a gene maps to a handful of probes on an expression array, low hundreds on an exon array)
     * so that hitting it means something unusual, not a normal query.
     */
    private static final int MAX_GENE_FILTER_PROBES = 20000;

    private final ExpressionExperimentService eeService;
    private final CompositeSequenceService csService;
    private final CompositeSequenceArgService csArgService;

    @Autowired
    public PlatformArgService( ArrayDesignService service, ExpressionExperimentService eeService, CompositeSequenceService csService, CompositeSequenceArgService csArgService ) {
        super( service );
        this.eeService = eeService;
        this.csService = csService;
        this.csArgService = csArgService;
    }

    /**
     * Cursor-mode counterpart to {@link ArrayDesignService#loadValueObjects(Filters, Sort, int, int)}.
     * Always sorts by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1c. The user's {@code ?filter=} arg
     * still applies; the user's {@code ?sort=} arg is intentionally not honoured in cursor
     * mode because the DAO currently restricts cursors to single-component id sorts
     * (recce sec. 3.4 — to be lifted in phase B once the index audit is complete).
     */
    public CursorPage<ArrayDesignValueObject> getPlatformsByCursor( @Nullable Filters filters, @Nullable Cursor cursor, int limit ) {
        return service.loadValueObjectsByCursor( filters, service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }

    /**
     * Retrieves the Datasets of the Platform that this argument represents.
     *
     * @return a collection of Datasets that the platform represented by this argument contains.
     */
    public Slice<ExpressionExperimentValueObject> getExperiments( PlatformArg<?> arg, int limit, int offset ) {
        ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( eeService.getFilter( "bioAssays.arrayDesignUsed.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return eeService.loadValueObjects( filters, eeService.getSort( "bioAssays.arrayDesignUsed.id", null, Sort.NullMode.LAST ), offset, limit );
    }

    /**
     * Retrieves the design elements of the platform that this argument represents.
     *
     * @return a collection of design element VOs that the platform represented by this argument contains.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, int limit, int offset ) {
        return getElements( arg, limit, offset, false );
    }

    /**
     * Variant that opt-in hydrates the probe-sequence projection (sequence + length)
     * via a single batch HQL after the main page is fetched. Default {@code false}
     * preserves the legacy default response size — sequences are 25-300bp per probe
     * and would inflate a 22k-element listing by ~1 MB.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, int limit, int offset, boolean withSequence ) {
        return getElements( arg, limit, offset, withSequence, false, null );
    }

    /**
     * Full-projection variant: adds the opt-in gene column ({@code withGenes}) and the opt-in
     * gene restriction ({@code geneIds}).
     * <p>
     * When {@code geneIds} is non-null the page is restricted to the elements on this platform that
     * map to one of those genes, resolved through {@code GENE2CS} and composed as an extra
     * {@code cs.id in (...)} conjunct on top of the platform-scope filter. Composing rather than
     * replacing means the existing offset pagination, sorting and count queries all apply
     * unchanged. A non-null {@code geneIds} that matches no element on this platform short-circuits
     * to an empty page — never to an unrestricted one.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, int limit, int offset, boolean withSequence, boolean withGenes, @Nullable Collection<Long> geneIds ) {
        return getElements( arg, null, geneIds, limit, offset, withSequence, withGenes );
    }

    /**
     * Widest offset-mode variant: user-supplied {@code filter=} conjunct on top of everything else.
     * <p>
     * The platform scope is composed FIRST and the user filter ANDed onto it, so a user filter can
     * narrow the listing but never widen it past the platform in the path.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, @Nullable Filters userFilters, @Nullable Collection<Long> geneIds, int limit, int offset, boolean withSequence, boolean withGenes ) {
        final ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( csService.getFilter( "arrayDesign.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        if ( userFilters != null && !userFilters.isEmpty() ) {
            filters.and( userFilters );
        }
        if ( geneIds != null ) {
            Collection<Long> probeIds = probeIdsForGenes( ad, geneIds );
            if ( probeIds.isEmpty() ) {
                return new Slice<>( Collections.emptyList(), null, offset, limit, 0L );
            }
            filters.and( csService.getFilter( "id", Long.class, Filter.Operator.in, probeIds ) );
        }
        Slice<CompositeSequenceValueObject> page = csService.loadValueObjects( filters, null, offset, limit );
        hydrateProjections( page, withSequence, withGenes );
        return page;
    }

    /**
     * Hydrate {@code sequence} + {@code sequenceLength} on each VO in {@code page}
     * via one batch query. Probes with no biological characteristic are absent
     * from the lookup map and left with null sequence fields (which then elide
     * from the JSON via {@code @JsonInclude(NON_NULL)}). No-op on an empty page.
     */
    /**
     * Filters for the {@code /{platform}/elements/{probes}} endpoints: the platform scope AND the
     * {@code {probes}} id/name set restriction.
     * <p>
     * Both conjuncts matter. {@link CompositeSequenceArrayArg#getPlatformFilter()} encodes ONLY the
     * platform scope, so composing it alone — which is what this endpoint did until 2026-08-22 —
     * silently answered {@code /platforms/GPL96/elements/1007_s_at} with the first page of GPL96
     * rather than with the one probe asked for. The id/name clause comes from
     * {@link AbstractEntityArgService#getFilters(AbstractEntityArrayArg)}, the same builder every
     * other array-arg endpoint uses.
     */
    public Filters getElementFilters( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg ) {
        probesArg.setPlatform( this.getEntity( arg ) );
        return Filters.by( probesArg.getPlatformFilter() ).and( csArgService.getFilters( probesArg ) );
    }

    /**
     * Composite-sequence ids on {@code ad} mapping to any of {@code geneIds}, capped.
     * <p>
     * The cap exists because the result is spliced into an {@code IN} clause: a gene query that
     * resolves to a family, on a platform with many probes per gene, can otherwise produce an
     * IN-list large enough to dominate the query plan. Truncation is logged rather than silent —
     * a capped page is a wrong answer, and the log line is what makes it diagnosable.
     */
    private Collection<Long> probeIdsForGenes( ArrayDesign ad, Collection<Long> geneIds ) {
        Set<Long> probeIds = csService.findIdsByGeneIds( geneIds, ad.getId() );
        if ( probeIds.size() > MAX_GENE_FILTER_PROBES ) {
            log.warn( String.format( "Gene filter on %s matched %d elements, capping the id restriction at %d;"
                            + " the resulting page is a truncation, not the full match set.",
                    ad.getShortName(), probeIds.size(), MAX_GENE_FILTER_PROBES ) );
            return probeIds.stream().limit( MAX_GENE_FILTER_PROBES ).collect( Collectors.toCollection( LinkedHashSet::new ) );
        }
        return probeIds;
    }

    /**
     * Apply whichever opt-in projections the caller asked for. Both are one batch query over the
     * page's ids, so a page costs at most two extra statements regardless of its size.
     */
    private void hydrateProjections( Iterable<CompositeSequenceValueObject> page, boolean withSequence, boolean withGenes ) {
        if ( withSequence ) {
            hydrateSequences( page );
        }
        if ( withGenes ) {
            hydrateGenes( page );
        }
    }

    /**
     * Hydrate {@code genes} on each VO in {@code page} via one batch query over {@code GENE2CS}.
     * <p>
     * Unlike {@link #hydrateSequences}, elements absent from the lookup map are set to an EMPTY
     * list rather than left null: with {@code withGenes=true} the caller has asked the question, so
     * "this element maps to no gene" has to be distinguishable on the wire from "not requested",
     * which is what a null (elided by {@code NON_NULL}) would say. No-op on an empty page.
     */
    private void hydrateGenes( Iterable<CompositeSequenceValueObject> page ) {
        List<Long> ids = new ArrayList<>();
        for ( CompositeSequenceValueObject vo : page ) {
            if ( vo.getId() != null ) ids.add( vo.getId() );
        }
        if ( ids.isEmpty() ) return;
        Map<Long, List<GeneReferenceValueObject>> data = csService.getGeneData( ids );
        for ( CompositeSequenceValueObject vo : page ) {
            List<GeneReferenceValueObject> genes = data.get( vo.getId() );
            vo.setGenes( genes != null ? genes : Collections.emptyList() );
        }
    }

    private void hydrateSequences( Iterable<CompositeSequenceValueObject> page ) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for ( CompositeSequenceValueObject vo : page ) {
            if ( vo.getId() != null ) ids.add( vo.getId() );
        }
        if ( ids.isEmpty() ) return;
        java.util.Map<Long, ubic.gemma.persistence.service.expression.designElement.CompositeSequenceDao.BioSequenceLite> data =
                csService.getSequenceData( ids );
        for ( CompositeSequenceValueObject vo : page ) {
            ubic.gemma.persistence.service.expression.designElement.CompositeSequenceDao.BioSequenceLite lite = data.get( vo.getId() );
            if ( lite != null ) {
                vo.setSequence( lite.sequence() );
                vo.setSequenceLength( lite.length() );
            }
        }
    }

    /**
     * Cursor-mode counterpart to {@link #getElements(PlatformArg, int, int)}: keyset pagination
     * over the {@link CompositeSequence design elements} of a single platform, always sorted by
     * ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1e. The path-derived
     * {@code arrayDesign.id = ?} constraint is preserved (composed into the {@link Filters}
     * before the DAO call) so the cursor-mode result is restricted to the same platform that
     * the offset-mode result would be. The legacy offset-mode {@code Sort} was {@code null}
     * (DAO default); cursor mode tightens this to an explicit id-asc sort because the
     * cursor DAO currently restricts cursors to single-component id sorts (recce sec. 3.4 —
     * to be lifted in phase B once the index audit is complete).
     */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, @Nullable Cursor cursor, int limit ) {
        return getElementsByCursor( arg, cursor, limit, false );
    }

    /** Cursor-mode variant of {@link #getElements(PlatformArg, int, int, boolean)}. */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, @Nullable Cursor cursor, int limit, boolean withSequence ) {
        return getElementsByCursor( arg, cursor, limit, withSequence, false, null );
    }

    /**
     * Cursor-mode variant of {@link #getElements(PlatformArg, int, int, boolean, boolean, Collection)}.
     * The gene restriction is composed as an extra conjunct exactly as in offset mode, so the keyset
     * walk stays on the same {@code id}-asc ordering and the cursor tokens remain interchangeable
     * across pages of the same query.
     */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, @Nullable Cursor cursor, int limit, boolean withSequence, boolean withGenes, @Nullable Collection<Long> geneIds ) {
        return getElementsByCursor( arg, null, geneIds, cursor, limit, withSequence, withGenes );
    }

    /** Widest cursor-mode variant; see {@link #getElements(PlatformArg, Filters, Collection, int, int, boolean, boolean)}. */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, @Nullable Filters userFilters, @Nullable Collection<Long> geneIds, @Nullable Cursor cursor, int limit, boolean withSequence, boolean withGenes ) {
        final ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( csService.getFilter( "arrayDesign.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        if ( userFilters != null && !userFilters.isEmpty() ) {
            filters.and( userFilters );
        }
        if ( geneIds != null ) {
            Collection<Long> probeIds = probeIdsForGenes( ad, geneIds );
            if ( probeIds.isEmpty() ) {
                return new CursorPage<>( Collections.emptyList(), csService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), limit, null, null, 0L );
            }
            filters.and( csService.getFilter( "id", Long.class, Filter.Operator.in, probeIds ) );
        }
        CursorPage<CompositeSequenceValueObject> page = csService.loadValueObjectsByCursor( filters, csService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
        hydrateProjections( page, withSequence, withGenes );
        return page;
    }

    /**
     * Cursor-mode counterpart to {@link CompositeSequenceService#loadValueObjects(Filters, Sort, int, int)}
     * scoped to a single platform AND a fixed set of probe identifiers (the
     * {@code /{platform}/elements/{probes}} endpoint) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1j. Always sorted by ascending {@code id}
     * (the primary key, indexed and unique) because the cursor DAO currently restricts cursors
     * to single-component id sorts (recce sec. 3.4 — to be lifted in phase B once the index
     * audit is complete).
     * <p>
     * Filter composition matches the offset variant exactly: {@code Filters.by(probesArg.getPlatformFilter())}.
     * {@link CompositeSequenceArrayArg#getPlatformFilter()} already encodes both the
     * {@code arrayDesign.id = ?} platform-scope and the {@code id IN (...)} / {@code name IN (...)}
     * probe-set restriction, so we don't need to compose them separately here.
     */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg, @Nullable Cursor cursor, int limit ) {
        return getElementsByCursor( arg, probesArg, cursor, limit, false );
    }

    /**
     * Offset-mode variant of {@link #getElementsByCursor(PlatformArg, CompositeSequenceArrayArg, Cursor, int, boolean)}.
     * Mirrors the inline call at the legacy {@code /{platform}/elements/{probes}} offset path
     * so {@code withSequence} works there too, not just in cursor mode.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg, int limit, int offset, boolean withSequence ) {
        return getElements( arg, probesArg, limit, offset, withSequence, false );
    }

    /** Probe-set variant with the opt-in gene column. */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg, int limit, int offset, boolean withSequence, boolean withGenes ) {
        Filters filters = getElementFilters( arg, probesArg );
        Slice<CompositeSequenceValueObject> page = csService.loadValueObjects( filters, csService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), offset, limit );
        hydrateProjections( page, withSequence, withGenes );
        return page;
    }

    /** Cursor-mode + probe-set variant of {@link #getElements(PlatformArg, int, int, boolean)}. */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg, @Nullable Cursor cursor, int limit, boolean withSequence ) {
        return getElementsByCursor( arg, probesArg, cursor, limit, withSequence, false );
    }

    /** Cursor-mode + probe-set variant with the opt-in gene column. */
    public CursorPage<CompositeSequenceValueObject> getElementsByCursor( PlatformArg<?> arg, CompositeSequenceArrayArg probesArg, @Nullable Cursor cursor, int limit, boolean withSequence, boolean withGenes ) {
        Filters filters = getElementFilters( arg, probesArg );
        CursorPage<CompositeSequenceValueObject> page = csService.loadValueObjectsByCursor( filters, csService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
        hydrateProjections( page, withSequence, withGenes );
        return page;
    }

    /**
     * Cursor-mode counterpart to {@link #getExperiments(PlatformArg, int, int)}: keyset pagination
     * over the {@link ExpressionExperimentValueObject datasets} that use a single platform,
     * always sorted by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1f. The path-derived
     * {@code bioAssays.arrayDesignUsed.id = ?} constraint is preserved (composed into the
     * {@link Filters} before the DAO call) so the cursor-mode result is restricted to the same
     * platform that the offset-mode result would be. The legacy offset-mode {@code Sort} keyed
     * off {@code bioAssays.arrayDesignUsed.id} (not stable for keyset pagination); cursor mode
     * tightens this to an explicit id-asc sort because the cursor DAO currently restricts
     * cursors to single-component id sorts (recce sec. 3.4 — to be lifted in phase B once the
     * index audit is complete).
     */
    public CursorPage<ExpressionExperimentValueObject> getExperimentsByCursor( PlatformArg<?> arg, @Nullable Cursor cursor, int limit ) {
        ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( eeService.getFilter( "bioAssays.arrayDesignUsed.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return eeService.loadValueObjectsByCursor( filters, eeService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }

    /**
     * Cursor-mode counterpart to {@link ArrayDesignService#loadBlacklistedValueObjects(Filters, Sort, int, int)}.
     * Always sorts by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1h. The user's {@code ?filter=} arg still
     * applies; the user's {@code ?sort=} arg is intentionally not honoured in cursor mode because
     * the DAO currently restricts cursors to single-component id sorts (recce sec. 3.4 — to be
     * lifted in phase B once the index audit is complete). The blacklist short-name/accession
     * predicate is composed inside the DAO ({@code ArrayDesignDaoImpl#composeBlacklistFilters})
     * so the same blacklist scope is enforced identically in both modes.
     */
    public CursorPage<ArrayDesignValueObject> getBlacklistedPlatformsByCursor( @Nullable Filters filters, @Nullable Cursor cursor, int limit ) {
        return service.loadBlacklistedValueObjectsByCursor( filters, service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }
}
