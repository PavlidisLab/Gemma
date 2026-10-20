package ubic.gemma.model.expression.bioAssay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The platform pair on a sample: what it is run on now, and what it was submitted on.
 * <p>
 * A dataset that has been switched to a generic platform keeps the submitted one in
 * {@code originalPlatform}, and the two must not be conflated — telling a reader their RNA-seq
 * dataset was submitted on {@code Generic_mouse_ncbiIds} erases the instrument it actually came from.
 *
 * @author gemma
 */
public class BioAssayValueObjectTest {

    /**
     * {@link ArrayDesignValueObject}'s constructor asks the security context what to expose, so a bare unit
     * test needs an authentication even though nothing here is about authorization.
     */
    @BeforeEach
    public void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken( "tester", "tester", "GROUP_USER" ) );
    }

    @AfterEach
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static ArrayDesign platform( long id, String shortName ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( id );
        ad.setShortName( shortName );
        ad.setName( shortName + " name" );
        return ad;
    }

    private static BioAssay assayOn( ArrayDesign used, ArrayDesign original ) {
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 3000L );
        ba.setName( "GSM900001" );
        ba.setArrayDesignUsed( used );
        ba.setOriginalPlatform( original );
        return ba;
    }

    /**
     * The regression. The VO takes a cache of already-built platform VOs, and the original-platform branch
     * looked itself up under the USED platform's key: with a cache that held the used platform — which is what
     * the samples route passes — every sample reported the platform it was switched TO as the one it was
     * switched FROM. The DB was right the whole time, so {@code ?filter=bioAssays.originalPlatform.*}
     * contradicted the serialized VO for the same dataset (uib, GSE217927, 2026-08-26).
     */
    @Test
    public void testOriginalPlatformIsNotTakenFromTheUsedPlatformsCacheEntry() {
        ArrayDesign used = platform( 736L, "Generic_mouse_ncbiIds" );
        ArrayDesign original = platform( 12L, "GPL24247" );

        // the shape that broke it: the cache knows the USED platform and nothing else
        Map<ArrayDesign, ArrayDesignValueObject> ad2vo =
                Collections.singletonMap( used, new ArrayDesignValueObject( used ) );

        BioAssayValueObject vo = new BioAssayValueObject( assayOn( used, original ), ad2vo, null, false, false );

        assertThat( vo.getArrayDesign().getShortName() ).isEqualTo( "Generic_mouse_ncbiIds" );
        assertThat( vo.getOriginalPlatform() ).isNotNull();
        assertThat( vo.getOriginalPlatform().getShortName() )
                .withFailMessage( "the sample reported the platform it was switched TO as its original" )
                .isEqualTo( "GPL24247" );
    }

    /** And when the cache does know the original, it is reused rather than rebuilt. */
    @Test
    public void testOriginalPlatformComesFromTheCacheWhenItIsThere() {
        ArrayDesign used = platform( 736L, "Generic_mouse_ncbiIds" );
        ArrayDesign original = platform( 12L, "GPL24247" );
        ArrayDesignValueObject usedVo = new ArrayDesignValueObject( used );
        ArrayDesignValueObject originalVo = new ArrayDesignValueObject( original );
        Map<ArrayDesign, ArrayDesignValueObject> ad2vo = new HashMap<>();
        ad2vo.put( used, usedVo );
        ad2vo.put( original, originalVo );

        BioAssayValueObject vo = new BioAssayValueObject( assayOn( used, original ), ad2vo, null, false, false );

        assertThat( vo.getArrayDesign() ).isSameAs( usedVo );
        assertThat( vo.getOriginalPlatform() ).isSameAs( originalVo );
    }

    /** No switch, no original — the field stays absent rather than echoing the used platform. */
    @Test
    public void testNoOriginalPlatformLeavesTheFieldNull() {
        ArrayDesign used = platform( 736L, "Generic_mouse_ncbiIds" );
        BioAssayValueObject vo = new BioAssayValueObject( assayOn( used, null ), null, null, false, false );

        assertThat( vo.getArrayDesign().getShortName() ).isEqualTo( "Generic_mouse_ncbiIds" );
        assertThat( vo.getOriginalPlatform() ).isNull();
    }
}
