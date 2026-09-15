package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the Ticket-layer migration of {@link FactorValueNeedsAttentionService}
 * (see {@code CURATION_DETAILS_RETIREMENT.md}). Asserts that
 * {@code markAsNeedsAttention} opens a {@link TicketType#GENERIC} ticket
 * targeting BOTH the FV and the owning EE, and that
 * {@code clearNeedsAttentionFlag} transitions all open FV-targeted tickets
 * to {@link TicketState#RESOLVED}.
 */
@ContextConfiguration
public class FactorValueNeedsAttentionServiceTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class FactorValueNeedsAttentionServiceTestContextConfiguration {

        @Bean
        public FactorValueNeedsAttentionService factorValueNeedsAttentionService() {
            return new FactorValueNeedsAttentionServiceImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }

        @Bean
        public TicketService ticketService() {
            return mock();
        }

        @Bean
        public UserManager userManager() {
            return mock();
        }
    }

    @Autowired
    private FactorValueNeedsAttentionService factorValueNeedsAttentionService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private final User actor = new User();

    @AfterEach
    public void tearDown() {
        reset( expressionExperimentService, ticketService, userManager );
    }

    @Test
    public void testMarkAsNeedsAttention_opensTicketTargetingBothFvAndEe() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( userManager.getCurrentUser() ).thenReturn( actor );
        when( ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, 11L ) )
                .thenReturn( Collections.emptyList() );

        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "needs xyz" );

        assertTrue( fv.getNeedsAttention() );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<TicketTarget>> targets = ArgumentCaptor.forClass( Collection.class );
        verify( ticketService ).openTicket( eq( actor ), eq( TicketType.GENERIC ),
                contains( "needs xyz" ), targets.capture() );

        Set<TicketTargetType> types = targets.getValue().stream()
                .map( TicketTarget::getTargetType )
                .collect( Collectors.toSet() );
        assertEquals( Set.of( TicketTargetType.FACTOR_VALUE, TicketTargetType.EXPRESSION_EXPERIMENT ),
                types,
                "ticket must target both FV and EE" );

        // FV target carries FV id, EE target carries EE id.
        for ( TicketTarget t : targets.getValue() ) {
            if ( t.getTargetType() == TicketTargetType.FACTOR_VALUE ) {
                assertEquals( 11L, ( long ) t.getTargetId() );
            } else {
                assertEquals( 99L, ( long ) t.getTargetId() );
            }
        }
    }

    @Test
    public void testMarkAsNeedsAttention_idempotentWhenOpenTicketAlreadyExists() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );

        Ticket already = new Ticket();
        already.setType( TicketType.GENERIC );
        already.setState( TicketState.OPEN );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( userManager.getCurrentUser() ).thenReturn( actor );
        when( ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, 11L ) )
                .thenReturn( Collections.singletonList( already ) );

        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "second attempt" );

        // FV's own flag still flipped (the flip-on-FV bit is idempotent itself),
        // but no new ticket should be opened.
        assertTrue( fv.getNeedsAttention() );
        verify( ticketService, never() ).openTicket( any(), any(), any(), any() );
    }

    @Test
    public void testClearNeedsAttentionFlag_resolvesOpenFvTickets() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );
        fv.setNeedsAttention( true );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );

        Ticket open = new Ticket();
        open.setType( TicketType.GENERIC );
        open.setState( TicketState.OPEN );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( userManager.getCurrentUser() ).thenReturn( actor );
        when( ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, 11L ) )
                .thenReturn( Collections.singletonList( open ) );

        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "all fixed" );

        assertFalse( fv.getNeedsAttention() );
        verify( ticketService ).transition( open, TicketState.RESOLVED, actor, "all fixed" );
    }

    @Test
    public void testClearNeedsAttentionFlag_skipsAlreadyResolvedAndCancelledTickets() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );
        fv.setNeedsAttention( true );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );

        // Give each ticket a unique id so Ticket.equals() can distinguish them
        // (default Ticket.equals falls back to (name,type,createdAt) which would
        // collide here).
        Ticket resolved = new Ticket();
        resolved.setId( 1L );
        resolved.setType( TicketType.GENERIC );
        resolved.setState( TicketState.RESOLVED );

        Ticket cancelled = new Ticket();
        cancelled.setId( 2L );
        cancelled.setType( TicketType.GENERIC );
        cancelled.setState( TicketState.CANCELLED );

        Ticket inProgress = new Ticket();
        inProgress.setId( 3L );
        inProgress.setType( TicketType.GENERIC );
        inProgress.setState( TicketState.IN_PROGRESS );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( userManager.getCurrentUser() ).thenReturn( actor );
        when( ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, 11L ) )
                .thenReturn( List.of( resolved, cancelled, inProgress ) );

        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "fix" );

        verify( ticketService, times( 1 ) ).transition( inProgress, TicketState.RESOLVED, actor, "fix" );
        verify( ticketService, never() ).transition( eq( resolved ), any(), any(), any() );
        verify( ticketService, never() ).transition( eq( cancelled ), any(), any(), any() );
    }

    @Test
    public void testClearNeedsAttentionFlag_noOpWhenNoOpenTickets() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );
        fv.setNeedsAttention( true );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( userManager.getCurrentUser() ).thenReturn( actor );
        when( ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, 11L ) )
                .thenReturn( Collections.emptyList() );

        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "fix" );

        assertFalse( fv.getNeedsAttention() );
        verify( ticketService, never() ).transition( any(), any(), any(), any() );
    }

    @Test
    public void testMarkAsNeedsAttention_noEeNoTicket() {
        FactorValue fv = createFactorValue();
        fv.setId( 11L );

        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( null );
        when( userManager.getCurrentUser() ).thenReturn( actor );

        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "x" );

        assertTrue( fv.getNeedsAttention() );
        verify( ticketService, never() ).openTicket( any(), any(), any(), any() );
        verify( ticketService, never() ).findOpenForTarget( any(), any() );
    }

    private FactorValue createFactorValue() {
        return createFactorValue( Collections.emptySet() );
    }

    private FactorValue createFactorValue( Set<Statement> statements ) {
        ExperimentalDesign ed = new ExperimentalDesign();
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.getCharacteristics().addAll( statements );
        return fv;
    }

    private static org.mockito.ArgumentMatcher<String> containsMatcher( String needle ) {
        return s -> s != null && s.contains( needle );
    }

    private static String contains( String needle ) {
        return org.mockito.ArgumentMatchers.argThat( containsMatcher( needle ) );
    }
}
