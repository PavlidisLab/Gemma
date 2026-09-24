package ubic.gemma.core.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scoping contract for the acting identity. Every property here is one that, if broken, writes a
 * wrong name into the permanent record of who did what — which is worse than writing none.
 */
public class ActingIdentityTest {

    @AfterEach
    public void tearDown() {
        // belt and braces: a leaked value would contaminate the next test on this thread, which is the
        // same failure mode the production code guards against with a pooled request thread.
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( null ) ) {
            assertThat( ActingIdentity.get() ).isNull();
        }
    }

    @Test
    public void unsetByDefault() {
        assertThat( ActingIdentity.get() ).isNull();
    }

    @Test
    public void boundInsideTheScopeAndClearedAfter() {
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( "someCurator" ) ) {
            assertThat( ActingIdentity.get() ).isEqualTo( "someCurator" );
        }
        assertThat( ActingIdentity.get() )
                .as( "🛑 cleared on close — threads are pooled, and a name left behind is attributed to "
                        + "whoever lands on this thread next" )
                .isNull();
    }

    /** A null or blank name binds nothing, so callers do not have to branch before opening a scope. */
    @Test
    public void nullAndBlankBindNothing() {
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( null ) ) {
            assertThat( ActingIdentity.get() ).isNull();
        }
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( "   " ) ) {
            assertThat( ActingIdentity.get() ).isNull();
        }
    }

    /** It is cleared even when the body throws — an exception must not leave the name bound. */
    @Test
    public void clearedWhenTheBodyThrows() {
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( "someCurator" ) ) {
            throw new IllegalStateException( "boom" );
        } catch ( IllegalStateException expected ) {
            // fall through
        }
        assertThat( ActingIdentity.get() ).isNull();
    }

    /**
     * A nested scope RESTORES the outer name rather than clearing it. Nesting is not expected, but
     * clearing would silently mis-attribute the remainder of the outer call, and a wrong attribution is
     * the one outcome worth engineering against here.
     */
    @Test
    public void nestedScopeRestoresTheOuterName() {
        try ( ActingIdentity.Scope outer = ActingIdentity.scope( "curatorA" ) ) {
            try ( ActingIdentity.Scope inner = ActingIdentity.scope( "curatorB" ) ) {
                assertThat( ActingIdentity.get() ).isEqualTo( "curatorB" );
            }
            assertThat( ActingIdentity.get() ).isEqualTo( "curatorA" );
        }
        assertThat( ActingIdentity.get() ).isNull();
    }

    /** Not inherited by another thread: work handed to an executor is attributed to whoever ran it. */
    @Test
    public void notVisibleFromAnotherThread() throws Exception {
        String[] seen = new String[1];
        try ( ActingIdentity.Scope ignored = ActingIdentity.scope( "someCurator" ) ) {
            Thread t = new Thread( () -> seen[0] = ActingIdentity.get() );
            t.start();
            t.join();
        }
        assertThat( seen[0] ).isNull();
    }
}
