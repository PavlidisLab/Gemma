package ubic.gemma.persistence.service.common.description;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.TableMaintenanceUtilImpl;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class CharacteristicDaoImplTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CharacteristicDaoImplContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CharacteristicDao characteristicDao( SessionFactory sessionFactory ) {
            return new CharacteristicDaoImpl( sessionFactory );
        }

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return new TableMaintenanceUtilImpl();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public MailEngine mailEngine() {
            return mock( MailEngine.class );
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }
    }

    @Autowired
    private CharacteristicDao characteristicDao;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private MutableAclService aclService;

    /* fixtures */
    private Collection<Characteristic> characteristics;

    @Before
    public void setUp() throws Exception {
        // TODO
        characteristics = Arrays.asList(
                createCharacteristic( "http://test/T0001", "male" ),
                createCharacteristic( "http://test/T0001", "male" ),
                createCharacteristic( "http://test/T0002", "male reproductive system" ),
                createCharacteristic( null, "male reproductive system (unknown term)" ),
                createCharacteristic( "http://test/T0003", "female" ),
                createCharacteristic( "http://test/T0004", "untreated" ),
                createCharacteristic( "http://test/T0005", "treated" ),
                createCharacteristic( "http://test/T0006", "treated with sodium chloride" ) );
        characteristics = characteristicDao.create( characteristics );
    }

    @After
    public void tearDown() {
        characteristicDao.remove( characteristics );
    }

    @Test
    public void testCountByValueLike() {
        Map<String, Characteristic> results = characteristicDao.findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue( "male%" );
        assertThat( results ).containsKeys( "http://test/T0001".toLowerCase(), "http://test/T0002".toLowerCase(), "male reproductive system (unknown term)" );
    }

    @Test
    public void testCountByValueUriIn() {
        Collection<String> uris = Arrays.asList( "http://test/T0006", "http://test/T0002" );
        Map<String, Long> results = characteristicDao.countCharacteristicsByValueUriGroupedByNormalizedValue( uris );
        assertThat( results ).containsKeys( "http://test/T0006".toLowerCase(), "http://test/T0002".toLowerCase() );
    }

    @Test
    public void testNormalizeByValue() {
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( null, "test" ) ) )
                .isEqualTo( "test" );
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( "", "test" ) ) )
                .isEqualTo( "" );
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( "https://EXAMPLE.COM", "test" ) ) )
                .isEqualTo( "https://example.com" );
    }

    @Test
    @WithMockUser(username = "bob")
    public void testFindExperimentsByUris() {
        assertThat( SecurityUtil.isUserAdmin() ).isFalse();
        assertThat( SecurityUtil.isUserLoggedIn() ).isTrue();
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        Characteristic c = createCharacteristic( "http://example.com", "example" );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();

        // add ACLs and read permission to bob
        MutableAcl acl = aclService.createAcl( new AclObjectIdentity( ee ) );
        acl.insertAce( 0, BasePermission.READ, new AclPrincipalSid( "bob" ), false );
        aclService.updateAcl( acl );

        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();
        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), taxon, 100, false );
        assertThat( results ).containsKey( ExpressionExperiment.class );
    }

    @Test
    @WithMockUser("bob")
    public void testFindExperimentsByUrisAsAnonymous() {
        assertThat( SecurityUtil.isUserAdmin() ).isFalse();
        assertThat( SecurityUtil.isUserLoggedIn() ).isTrue();
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        Characteristic c = createCharacteristic( "http://example.com", "example" );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        // add ACLs and read permission to everyone
        MutableAcl acl = aclService.createAcl( new AclObjectIdentity( ee ) );
        acl.insertAce( 0, BasePermission.READ, new AclGrantedAuthoritySid(
                new SimpleGrantedAuthority( AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ), false );
        aclService.updateAcl( acl );
        sessionFactory.getCurrentSession().flush();

        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();

        // run as anonymous
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        TestingAuthenticationToken token = new TestingAuthenticationToken( AuthorityConstants.ANONYMOUS_USER_NAME, null,
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ANONYMOUS_GROUP_AUTHORITY ) } ) );
        context.setAuthentication( token );
        SecurityContextHolder.setContext( context );
        assertThat( SecurityUtil.isUserAdmin() ).isFalse();
        assertThat( SecurityUtil.isUserAnonymous() ).isTrue();

        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), taxon, 100, false );
        assertThat( results ).containsKey( ExpressionExperiment.class );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindExperimentsByUrisAsAdmin() {
        assertThat( SecurityUtil.isUserAdmin() ).isTrue();
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        Characteristic c = createCharacteristic( "http://example.com", "example" );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();
        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), taxon, 100, false );
        assertThat( results ).containsKey( ExpressionExperiment.class );
    }

    @Test
    public void testDiscriminator() {
        Characteristic c = createCharacteristic( "test", "test" );
        sessionFactory.getCurrentSession().persist( c );
        List<String> clazz = ( List<String> ) sessionFactory.getCurrentSession()
                .createSQLQuery( "select C.class from CHARACTERISTIC C where C.ID = :id" )
                .setParameter( "id", c.getId() )
                .list();
        assertThat( clazz )
                .hasSize( 1 )
                .allSatisfy( s -> assertThat( s ).isNull() );
    }

    @Test
    public void testGetParents() {
        Statement c = createStatement( "test", "test" );
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        sessionFactory.getCurrentSession().persist( ef );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( fv );
        sessionFactory.getCurrentSession().flush();
        assertThat( characteristicDao.getParents( Collections.singleton( c ), null, -1 ) )
                .hasSize( 1 )
                .containsEntry( c, fv );
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( FactorValue.class ), -1 ) )
                .hasSize( 1 )
                .containsEntry( c, fv );
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( Investigation.class ), -1 ) ).isEmpty();
        // this is a special case since it does not use a foreign key in the CHARACTERISTIC table
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( Gene2GOAssociation.class ), -1 ) ).isEmpty();
    }

    private Characteristic createCharacteristic( @Nullable String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }

    private Statement createStatement( @Nullable String valueUri, String value ) {
        Statement c = new Statement();
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }
}