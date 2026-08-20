package ubic.gemma.core.ontology.providers.chebi;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CHEBI slim is only as good as its seed set, and the seed set used to be built from
 * {@code Characteristic.valueUri} alone. A Statement has three annotatable slots, and a drug or treatment is
 * most often the OBJECT of one ("treated with" → object) rather than its subject — so the slot CHEBI terms most
 * commonly occupy was the slot the seeding could not see, and the slim silently could not resolve compounds the
 * corpus demonstrably uses.
 * <p>
 * This pins all three slots against a real database. It is deliberately behavioural rather than a check on the
 * query text: the failure mode here is silence, and a seed set that quietly comes back short looks exactly like
 * a corpus that quietly has fewer terms.
 */
@ContextConfiguration
public class ChebiSeedResolverSlotsTest extends BaseDatabaseTest5 {

    private static final String CHEBI = "http://purl.obolibrary.org/obo/CHEBI_";

    @Configuration
    @TestComponent
    static class ChebiSeedResolverSlotsTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ChebiSeedResolver chebiSeedResolver( SessionFactory sessionFactory ) {
            return new ChebiSeedResolver( sessionFactory );
        }
    }

    @Autowired
    private ChebiSeedResolver chebiSeedResolver;

    @Test
    public void testSeedsCoverSubjectObjectAndSecondObject() {
        Characteristic subject = Characteristic.Factory.newInstance();
        subject.setValue( "caffeine" );
        subject.setValueUri( CHEBI + "27732" );
        sessionFactory.getCurrentSession().persist( subject );

        Statement treated = new Statement();
        treated.setSubject( "cell" );
        treated.setObject( "colchicine" );
        treated.setObjectUri( CHEBI + "23359" );
        treated.setSecondObject( "nocodazole" );
        treated.setSecondObjectUri( CHEBI + "34892" );
        sessionFactory.getCurrentSession().persist( treated );

        sessionFactory.getCurrentSession().flush();

        Set<String> seeds = chebiSeedResolver.resolveCorpusSeeds();

        assertThat( seeds )
                .as( "a compound in the object slot is still a compound the corpus uses" )
                .contains( CHEBI + "27732", CHEBI + "23359", CHEBI + "34892" );
    }

    /**
     * Widening the slots must not widen the seed set beyond what the corpus actually annotates — that is the
     * whole bargain of a slim. Nothing that is not a CHEBI URI may leak in.
     */
    @Test
    public void testSeedsStayBoundedToChebiUrisTheCorpusUses() {
        Characteristic chebi = Characteristic.Factory.newInstance();
        chebi.setValue( "caffeine" );
        chebi.setValueUri( CHEBI + "27732" );
        sessionFactory.getCurrentSession().persist( chebi );

        Characteristic other = Characteristic.Factory.newInstance();
        other.setValue( "disease" );
        other.setValueUri( "http://purl.obolibrary.org/obo/MONDO_0000001" );
        sessionFactory.getCurrentSession().persist( other );

        Statement freeText = new Statement();
        freeText.setSubject( "cell" );
        freeText.setObject( "something nobody grounded" );
        sessionFactory.getCurrentSession().persist( freeText );

        sessionFactory.getCurrentSession().flush();

        assertThat( chebiSeedResolver.resolveCorpusSeeds() )
                .containsExactly( CHEBI + "27732" );
    }
}
