package ubic.gemma.model.common.auditAndSecurity.eventType;

import org.junit.Test;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchProblemsUpdateEvent;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditEventTypeTest {

    @Test
    public void test() {
        BatchProblemsUpdateEvent event1 = new BatchProblemsUpdateEvent();
        BatchProblemsUpdateEvent event2 = new BatchProblemsUpdateEvent();
        assertThat( event1 ).isEqualTo( event2 );
        assertThat( event1 ).hasSameHashCodeAs( BatchProblemsUpdateEvent.class );
        assertThat( event1 ).hasSameHashCodeAs( event2 );
        assertThat( event1 ).hasToString( BatchProblemsUpdateEvent.class.getName() );
    }

}