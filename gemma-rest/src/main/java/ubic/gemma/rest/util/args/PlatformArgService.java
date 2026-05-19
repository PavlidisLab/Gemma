package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
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

@Service
public class PlatformArgService extends AbstractEntityArgService<ArrayDesign, ArrayDesignService> {

    private final ExpressionExperimentService eeService;
    private final CompositeSequenceService csService;

    @Autowired
    public PlatformArgService( ArrayDesignService service, ExpressionExperimentService eeService, CompositeSequenceService csService ) {
        super( service );
        this.eeService = eeService;
        this.csService = csService;
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
        final ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( csService.getFilter( "arrayDesign.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return csService.loadValueObjects( filters, null, offset, limit );
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
        final ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( csService.getFilter( "arrayDesign.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return csService.loadValueObjectsByCursor( filters, csService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }
}
