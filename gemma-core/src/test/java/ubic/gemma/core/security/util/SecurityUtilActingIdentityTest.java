package ubic.gemma.core.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.security.AuthorityConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Who {@link SecurityUtil#resolveActingIdentity(String)} records as having directed an action, and when an agent must
 * name that person.
 * <p>
 * Paul, 2026-09-15: "the agent is the authenticated user the edit is attributed to, 'on behalf of' is the person
 * (administrator for example) who directed the agent", "on_behalf_of=gemmaAgent is always going to be wrong", and, of
 * a ruling's {@code decidedBy}, "it should be the person".
 *
 * @author gembro
 */
public class SecurityUtilActingIdentityTest {

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testAnAgentNamingItselfIsRefused() {
        runAs( "gemmaAgent", AuthorityConstants.AGENT_GROUP_AUTHORITY );
        assertThatThrownBy( () -> SecurityUtil.resolveActingIdentity( "gemmaAgent" ) )
                .isInstanceOf( ActingIdentityRefusedException.class )
                .hasMessageContaining( "onBehalfOf" );
    }

    @Test
    public void testAnAgentNamingThePersonWhoDirectedIt() {
        runAs( "gemmaAgent", AuthorityConstants.AGENT_GROUP_AUTHORITY );
        assertThat( SecurityUtil.resolveActingIdentity( "administrator" ) ).isEqualTo( "administrator" );
    }

    /**
     * A person naming themselves records the same identity as naming no one, so it stays allowed.
     */
    @Test
    public void testAnAdministratorNamingThemselves() {
        runAs( "administrator", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        assertThat( SecurityUtil.resolveActingIdentity( "administrator" ) ).isEqualTo( "administrator" );
    }

    @Test
    public void testAnAgentRulingWithoutNamingAnyoneIsRefused() {
        runAs( "gemmaAgent", AuthorityConstants.AGENT_GROUP_AUTHORITY );
        assertThatThrownBy( () -> SecurityUtil.requireOnBehalfOfFromAgent( null ) )
                .isInstanceOf( ActingIdentityRefusedException.class );
        assertThatThrownBy( () -> SecurityUtil.requireOnBehalfOfFromAgent( " " ) )
                .isInstanceOf( ActingIdentityRefusedException.class );
    }

    @Test
    public void testACuratorRulingForThemselvesNeedsNoOnBehalfOf() {
        runAs( "someCurator", AuthorityConstants.CURATOR_GROUP_AUTHORITY );
        assertThatCode( () -> SecurityUtil.requireOnBehalfOfFromAgent( null ) ).doesNotThrowAnyException();
    }

    private static void runAs( String userName, String authority ) {
        SecurityContextHolder.getContext()
                .setAuthentication( new TestingAuthenticationToken( userName, userName, authority ) );
    }
}
