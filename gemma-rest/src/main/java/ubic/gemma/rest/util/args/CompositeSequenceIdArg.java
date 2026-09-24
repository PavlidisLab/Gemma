package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import jakarta.ws.rs.BadRequestException;
import java.util.Objects;

/**
 * Composite Sequence argument for CS ID.
 */
@Schema(type = "integer", format = "int64", description = "A composite sequence numerical identifier.")
public class CompositeSequenceIdArg extends CompositeSequenceArg<Long> {

    /**
     * The identifier exactly as it was written in the request.
     * <p>
     * The name fallback in {@link #getEntityWithPlatform} has to look up that original spelling
     * rather than a re-rendering of the parsed long: a probe named {@code 007} would be missed by a
     * lookup for {@code 7}.
     */
    private final String originalValue;

    CompositeSequenceIdArg( long s, String originalValue ) {
        super( "id", Long.class, s );
        this.originalValue = originalValue;
    }

    @Override
    CompositeSequence getEntity( CompositeSequenceService service ) {
        return service.load( this.getValue() );
    }

    /**
     * Resolves by id, then falls through to a name lookup scoped to the platform.
     * <p>
     * A numeric path segment is read as an id first, but a probe NAME can be entirely numeric --
     * Agilent feature-number arrays (GPL890, GPL891, GPL1406, GPL560, GPL962, NHGRI-13.8k,
     * UMich-10k and others: 20 of the first 100 platforms) name every probe with a bare integer. For
     * those, the id lookup either misses outright or lands on some unrelated probe on another
     * platform, so without this fallback every probe on such an array is unreachable through this
     * path. Verified on gemma2: the probe NAMED {@code 22575} on GPL890 has id 209787, and asking
     * for {@code 22575} resolved id 22575 -- a probe on a different platform -- and produced a 400.
     * <p>
     * The id keeps precedence, so nothing that resolves today changes its answer; the fallback only
     * runs where the request currently fails. The residual ambiguity is a probe whose name equals
     * another probe's id ON THE SAME PLATFORM, where the id still wins and the name stays shadowed.
     * No such collision was found when sampling the at-risk platforms -- ids are allocated in a
     * contiguous per-platform block while numeric names come from a separate numbering scheme -- but
     * nothing enforces that.
     */
    @Override
    @Nullable
    CompositeSequence getEntityWithPlatform( CompositeSequenceService service, ArrayDesign platform ) {
        CompositeSequence cs = getEntity( service );
        if ( cs != null && Objects.equals( cs.getArrayDesign().getId(), platform.getId() ) ) {
            return cs;
        }
        CompositeSequence byName = service.findByName( platform, originalValue );
        if ( byName != null ) {
            return byName;
        }
        if ( cs != null ) {
            throw new BadRequestException( "Platform does not match the sequence's platform." );
        }
        // null becomes a 404 in AbstractEntityArgService.checkEntity
        return null;
    }
}
