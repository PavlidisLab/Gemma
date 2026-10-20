package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

@Service
public class CompositeSequenceArgService extends AbstractEntityArgService<CompositeSequence, CompositeSequenceService> {
    @Autowired
    public CompositeSequenceArgService( CompositeSequenceService service ) {
        super( service );
    }

    public CompositeSequence getEntityWithPlatform( CompositeSequenceArg<?> probeArg, ArrayDesign platform ) {
        return checkEntity( probeArg, probeArg.getEntityWithPlatform( service, platform ) );
    }

    /**
     * Cursor-mode counterpart to {@link CompositeSequenceService#getGenes(CompositeSequence, int, int, boolean)}
     * scoped to a single probe on a single platform (the
     * {@code /{platform}/elements/{probe}/genes} endpoint) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1l. Always sorted by ascending
     * {@code gene.id} (the primary key, indexed and unique) because the cursor DAO
     * currently restricts cursors to single-component id sorts (recce sec. 3.4 — to be
     * lifted in phase B once the index audit is complete).
     * <p>
     * Resolves the probe against the platform (mirroring the offset-mode call
     * {@code probeArgService.getEntityWithPlatform(probeArg, platform)} in
     * {@code PlatformsWebService.getPlatformElementGenes}) before delegating to the
     * service. {@code useGene2Cs} is held at {@code true} to match the offset variant.
     */
    public CursorPage<Gene> getGenesByCursor( CompositeSequenceArg<?> probeArg, ArrayDesign platform, @Nullable Cursor cursor, int limit ) {
        CompositeSequence cs = getEntityWithPlatform( probeArg, platform );
        return service.getGenesByCursor( cs, cursor, limit, true );
    }
}
