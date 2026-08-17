package ubic.gemma.persistence.service.common.description;

import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.MailEngine;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.GenericCellLevelCharacteristics;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtilImpl;

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class CharacteristicDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class CharacteristicDaoImplContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            Path gene2csInfoPath = Files.createTempDirectory( "DBReport" ).resolve( "gene2cs.info" );
            return new TestPropertyPlaceholderConfigurer( "gemma.gene2cs.path=" + gene2csInfoPath );
        }

        /**
         * Needed to convert {@link String} to {@link Path}.
         */
        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

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

    /** Taxa are unique on COMMON_NAME, so the disease-model tests share one per name. */
    private final Map<String, Taxon> taxa = new HashMap<>();

    /* fixtures */
    private Collection<Characteristic> characteristics;

    @BeforeEach
    public void setUp() throws Exception {
        // TODO
        characteristics = Arrays.asList(
                createCharacteristic( Categories.BIOLOGICAL_SEX, "http://test/T0001", "male" ),
                createCharacteristic( Categories.BIOLOGICAL_SEX, "http://test/T0001", "male" ),
                createCharacteristic( Categories.ORGANISM_PART, "http://test/T0002", "male reproductive system" ),
                createCharacteristic( Categories.ORGANISM_PART, null, "male reproductive system (unknown term)" ),
                createCharacteristic( Categories.BIOLOGICAL_SEX, "http://test/T0003", "female" ),
                createCharacteristic( Categories.TREATMENT, "http://test/T0004", "untreated" ),
                createCharacteristic( Categories.TREATMENT, "http://test/T0005", "treated" ),
                createCharacteristic( Categories.TREATMENT, "http://test/T0006", "treated with sodium chloride" ) );
        characteristics = characteristicDao.create( characteristics );
    }

    @AfterEach
    public void tearDown() {
        characteristicDao.remove( characteristics );
    }

    @Test
    public void testFindByUri() {
        assertThat( characteristicDao.findByUri( "http://test/T0001", null, null, true, -1 ) )
                .hasSize( 2 );
        assertThat( characteristicDao.findByUri( "http://test/T0001", null, Collections.singleton( ExpressionExperiment.class ), true, -1 ) )
                .hasSize( 2 );
        assertThat( characteristicDao.findByUri( "http://test/T0001", null, null, false, -1 ) )
                .isEmpty();
    }

    @Test
    public void testFindByValueLike() {
        assertThat( characteristicDao.findByValueLike( "male%", null, null, true, -1 ) )
                .hasSize( 4 );
        assertThat( characteristicDao.findByValueLike( "male%", null, Collections.singleton( ExpressionExperiment.class ), true, -1 ) )
                .hasSize( 4 );
        assertThat( characteristicDao.findByValueLike( "male%", null, null, false, -1 ) )
                .isEmpty();
    }

    @Test
    public void testFindByCategoryLike() {
        assertThat( characteristicDao.findByCategoryLike( "biological%", null, true, -1 ) )
                .hasSize( 3 );
        assertThat( characteristicDao.findByCategoryLike( "biological%", Collections.singleton( ExpressionExperiment.class ), true, -1 ) )
                .hasSize( 3 );
        assertThat( characteristicDao.findByCategoryLike( "biological%", null, false, -1 ) )
                .isEmpty();
    }

    @Test
    @Disabled("FIXME: H2 does not appreciate missing aggregator in group by, but I have not yet figured out how to fix it.")
    public void testCountByValueLike() {
        Map<String, Characteristic> results = characteristicDao.findByValueLikeGroupedByNormalizedValue( "male%", null, false );
        assertThat( results ).containsKeys( "http://test/T0001".toLowerCase(), "http://test/T0002".toLowerCase(), "male reproductive system (unknown term)" );
    }

    @Test
    public void testCountByValueUriIn() {
        Collection<String> uris = Arrays.asList( "http://test/T0006", "http://test/T0002" );
        Map<String, Long> results = characteristicDao.countByValueUriGroupedByNormalizedValue( uris, null, false );
        assertThat( results ).containsKeys( "http://test/T0006".toLowerCase(), "http://test/T0002".toLowerCase() );
    }

    @Test
    public void testNormalizeByValue() {
        assertThat( CharacteristicUtils.getNormalizedValue( createCharacteristic( null, "test" ) ) )
                .isEqualTo( "test" );
        assertThat( CharacteristicUtils.getNormalizedValue( createCharacteristic( "", "test" ) ) )
                .isEqualTo( "" );
        assertThat( CharacteristicUtils.getNormalizedValue( createCharacteristic( "https://EXAMPLE.COM", "test" ) ) )
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

        // Grant bob READ through the real gsec write path. This used to be seeded via raw JDBC
        // because the AclSid `sid` column was mapped read-only during the hbm.xml->JPA conversion,
        // so createAcl could not mint bob's principal sid; with that column insertable again the
        // createAcl + insertAce + updateAcl path works end to end.
        MutableAcl acl = ( MutableAcl ) aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new PrincipalSid( "bob" ), true );
        aclService.updateAcl( acl );

        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();
        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), true, true, true, taxon, 100, false );
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
        sessionFactory.getCurrentSession().flush();
        // grant READ to anonymous through the real gsec write path — see testFindExperimentsByUris.
        MutableAcl acl = ( MutableAcl ) aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ), true );
        aclService.updateAcl( acl );
        sessionFactory.getCurrentSession().flush();

        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();

        // run as anonymous
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken( "test", "anonymousUser",
                Collections.singletonList( new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) );
        context.setAuthentication( token );
        SecurityContextHolder.setContext( context );
        assertThat( SecurityUtil.isUserAdmin() ).isFalse();
        assertThat( SecurityUtil.isUserAnonymous() ).isTrue();

        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), true, true, true, taxon, 100, false );
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
        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();
        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), true, true, true, taxon, 100, false );
        assertThat( results ).containsKey( ExpressionExperiment.class );
    }

    @Test
    @WithMockUser(username = "bob")
    public void testFindRepresentativeUsageByValueUris() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        Characteristic c = createCharacteristic( "http://example.com", "example" );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();

        MutableAcl acl = ( MutableAcl ) aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new PrincipalSid( "bob" ), true );
        aclService.updateAcl( acl );

        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        Map<String, CharacteristicDao.UsageExample> byUri =
                characteristicDao.findRepresentativeUsageByValueUris( Collections.singleton( "http://example.com" ) );
        assertThat( byUri ).containsKey( "http://example.com" );
        CharacteristicDao.UsageExample ex = byUri.get( "http://example.com" );
        assertThat( ex.value ).isEqualTo( "example" );
        assertThat( ex.valueUri ).isEqualTo( "http://example.com" );
        assertThat( ex.level ).isEqualTo( ExpressionExperiment.class );
        assertThat( ex.sourceExperimentId ).isEqualTo( ee.getId() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindEeCountsByUriForOriginalValue() {
        String compound = "http://purl.obolibrary.org/obo/CHEBI_28262";
        String role = "http://purl.obolibrary.org/obo/OBI_0000025";

        // Two experiments whose submitters wrote "DMSO" meaning the compound — one bare, one
        // carrying GEO's field prefix — and one that wrote it meaning the role.
        createExperimentWithOriginalValue( compound, "dimethyl sulfoxide", "DMSO" );
        createExperimentWithOriginalValue( compound, "dimethyl sulfoxide", "treatment: DMSO" );
        createExperimentWithOriginalValue( role, "reference substance role", "dmso" );
        // Not the same string: whoever wrote this wrote a concentration, not a name.
        createExperimentWithOriginalValue( compound, "dimethyl sulfoxide", "0.3% DMSO" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        Set<String> candidates = new HashSet<>( Arrays.asList( compound, role ) );
        Map<String, Long> counts = characteristicDao.findEeCountsByUriForOriginalValue( candidates, "DMSO" );

        assertThat( counts )
                .as( "the GEO field prefix must not hide the string, and the trailing-content row must not count" )
                .containsEntry( compound, 2L )
                .containsEntry( role, 1L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindEeCountsByUriForOriginalValueRefusesQuantities() {
        String uri = "http://example.com/dose";
        createExperimentWithOriginalValue( uri, "some term", "24" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        // A value with no letters is a dose, a timepoint or a replicate number pooled across
        // unrelated experiments, so it must not be counted as evidence for any term.
        assertThat( characteristicDao.findEeCountsByUriForOriginalValue( Collections.singleton( uri ), "24" ) )
                .isEmpty();
        assertThat( characteristicDao.findEeCountsByUriForOriginalValue( Collections.singleton( uri ), "0.3" ) )
                .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindEeCountsByUriForOriginalValueTreatsWildcardsLiterally() {
        String uri = "http://example.com/tnf";
        createExperimentWithOriginalValue( uri, "TNF alpha", "TNF_alpha" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        Set<String> candidates = Collections.singleton( uri );
        assertThat( characteristicDao.findEeCountsByUriForOriginalValue( candidates, "TNF_alpha" ) )
                .containsEntry( uri, 1L );
        // The underscore is a LIKE wildcard; unescaped it would match any character in its place.
        assertThat( characteristicDao.findEeCountsByUriForOriginalValue( candidates, "TNFXalpha" ) )
                .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindPriorCurationByOriginalValue() {
        String role = "http://purl.obolibrary.org/obo/OBI_0000220";
        String substanceRole = "http://purl.obolibrary.org/obo/OBI_0000025";

        // `sham` is the contested case: curators sent it to two different terms. Neither shares a
        // word with the string, so no lexical search would ever return either one.
        createExperimentWithOriginalValue( role, "reference subject role", "sham" );
        createExperimentWithOriginalValue( role, "reference subject role", "treatment: Sham" );
        createExperimentWithOriginalValue( role, "reference subject role", "sham" );
        createExperimentWithOriginalValue( substanceRole, "reference substance role", "sham" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        List<CharacteristicDao.PriorCurationUsage> usages =
                characteristicDao.findPriorCurationByOriginalValue( "sham", -1 );

        assertThat( usages ).as( "most used first" ).hasSize( 2 );
        assertThat( usages.get( 0 ).valueUri ).isEqualTo( role );
        assertThat( usages.get( 0 ).experimentCount ).isEqualTo( 3L );
        assertThat( usages.get( 0 ).value ).isEqualTo( "reference subject role" );
        assertThat( usages.get( 0 ).agreement )
                .as( "3 of 4 — visibly contested, which is the point of the field" )
                .isEqualTo( 0.75, org.assertj.core.data.Offset.offset( 0.001 ) );
        assertThat( usages.get( 1 ).valueUri ).isEqualTo( substanceRole );
        assertThat( usages.get( 1 ).experimentCount ).isEqualTo( 1L );

        // A quantity is refused here for the same reason as in the tally.
        assertThat( characteristicDao.findPriorCurationByOriginalValue( "24", -1 ) ).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPriorCurationHonoursLeaveOneOut() {
        String uri = "http://purl.obolibrary.org/obo/OBI_0000220";
        createExperimentWithOriginalValue( uri, "reference subject role", "sham" );
        createExperimentWithOriginalValue( uri, "reference subject role", "sham" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        List<Long> allIds = ( List<Long> ) sessionFactory.getCurrentSession()
                .createQuery( "select e.id from ExpressionExperiment e" ).list();
        assertThat( allIds ).hasSizeGreaterThanOrEqualTo( 2 );

        // Excluding one of the two experiments must reduce the count. Without this a gold set drawn
        // from the corpus is partly tallying its own answer key.
        assertThat( characteristicDao.findPriorCurationByOriginalValue( "sham", -1, Collections.emptySet() ) )
                .singleElement()
                .satisfies( u -> assertThat( u.experimentCount ).isEqualTo( 2L ) );
        assertThat( characteristicDao.findPriorCurationByOriginalValue( "sham", -1,
                Collections.singleton( allIds.get( 0 ) ) ) )
                .singleElement()
                .satisfies( u -> assertThat( u.experimentCount ).isEqualTo( 1L ) );
        // Excluding every experiment leaves no evidence at all, rather than a stale full-corpus count.
        assertThat( characteristicDao.findPriorCurationByOriginalValue( "sham", -1, new HashSet<>( allIds ) ) )
                .isEmpty();
        assertThat( characteristicDao.findEeCountsByUriForOriginalValue( Collections.singleton( uri ), "sham",
                new HashSet<>( allIds ) ) )
                .isEmpty();
    }

    /**
     * Persist one experiment carrying a single characteristic whose ORIGINAL_VALUE is what the
     * submitter wrote, so the EE2C tally has something to count.
     */
    private void createExperimentWithOriginalValue( @Nullable String valueUri, String value, String originalValue ) {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        Characteristic c = createCharacteristic( valueUri, value );
        c.setOriginalValue( originalValue );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
    }

    @Test
    public void testDiscriminator() {
        Characteristic c = createCharacteristic( "test", "test" );
        sessionFactory.getCurrentSession().persist( c );
        List<String> clazz = ( List<String> ) sessionFactory.getCurrentSession()
                .createNativeQuery( "select C.class from CHARACTERISTIC C where C.ID = :id" )
                .setParameter( "id", c.getId() )
                .list();
        assertThat( clazz )
                .hasSize( 1 )
                .allSatisfy( s -> assertThat( s ).isNull() );
    }

    @Test
    public void testGetParents() {
        Characteristic eeC = createCharacteristic( "test1", "test1" );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.getCharacteristics().add( eeC );
        sessionFactory.getCurrentSession().persist( ee );

        Characteristic subsetC = createCharacteristic( "test1", "test1" );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.getCharacteristics().add( subsetC );
        sessionFactory.getCurrentSession().persist( subset );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        Characteristic edC = createCharacteristic( "test11", "testk12" );
        ed.getTypes().add( edC );
        sessionFactory.getCurrentSession().persist( ed );

        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        Characteristic efC = createCharacteristic( "test11", "testk13" );
        ef.setCategory( efC );
        Characteristic efAnnotationC = createCharacteristic( "test1920", "test091203" );
        ef.getAnnotations().add( efAnnotationC );
        sessionFactory.getCurrentSession().persist( ef );

        FactorValue fv = FactorValue.Factory.newInstance( ef );
        Statement c = createStatement( Categories.UNCATEGORIZED, "test", "test" );
        fv.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( fv );
        sessionFactory.getCurrentSession().flush();

        Characteristic danglingC = createCharacteristic( "test092103", "test01293" );
        sessionFactory.getCurrentSession().persist( danglingC );

        assertThat( characteristicDao.getParentClasses() ).containsExactlyInAnyOrder(
                ExpressionExperiment.class,
                ExpressionExperimentSubSet.class,
                ExperimentalDesign.class,
                ExperimentalFactor.class,
                BibliographicReference.class,
                FactorValue.class,
                BioMaterial.class,
                CellTypeAssignment.class,
                GenericCellLevelCharacteristics.class,
                GeneSet.class,
                Gene2GOAssociation.class );

        // ensure that all declared entities that have a characteristic is handled in getParents()
        // Phase 2: ported off Hibernate ClassMetadata to JPA Metamodel.
        for ( jakarta.persistence.metamodel.EntityType<?> et : sessionFactory.getMetamodel().getEntities() ) {
            for ( jakarta.persistence.metamodel.Attribute<?, ?> attr : et.getAttributes() ) {
                if ( attr.isAssociation() && Characteristic.class.equals( attr.getJavaType() ) ) {
                    assertThat( characteristicDao.getParentClasses() )
                            .contains( ( Class<? extends Identifiable> ) et.getJavaType() );
                }
            }
        }

        assertThatThrownBy( () -> characteristicDao.getParents( Arrays.asList( eeC, subsetC ), Collections.singleton( BioAssaySet.class ), false ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThat( characteristicDao.getParents( Arrays.asList( eeC, subsetC ), null, false ) )
                .hasSize( 2 )
                .containsEntry( eeC, ee )
                .containsEntry( subsetC, subset );
        assertThat( characteristicDao.getParents( Arrays.asList( eeC, subsetC ), Arrays.asList( ExpressionExperiment.class, ExpressionExperimentSubSet.class ), false ) )
                .hasSize( 2 )
                .containsEntry( eeC, ee )
                .containsEntry( subsetC, subset );
        assertThat( characteristicDao.getParents( Collections.singleton( eeC ), Collections.singleton( ExpressionExperiment.class ), false ) )
                .containsEntry( eeC, ee );
        assertThat( characteristicDao.getParents( Collections.singleton( subsetC ), Collections.singleton( ExpressionExperimentSubSet.class ), false ) )
                .containsEntry( subsetC, subset );

        // this is another special case because it has two source of parents: category and annotations
        assertThat( characteristicDao.getParents( Collections.singleton( edC ), Collections.singleton( ExperimentalDesign.class ), false ) )
                .containsEntry( edC, ed );

        // this is a special case since it does not use a foreign key in the CHARACTERISTIC table
        assertThat( characteristicDao.getParents( Collections.singleton( efC ), Collections.singleton( ExperimentalFactor.class ), false ) )
                .hasSize( 1 )
                .containsEntry( efC, ef );
        assertThat( characteristicDao.getParents( Collections.singleton( efAnnotationC ), Collections.singleton( ExperimentalFactor.class ), false ) )
                .hasSize( 1 )
                .containsEntry( efAnnotationC, ef );

        assertThat( characteristicDao.getParents( Collections.singleton( c ), null, false ) )
                .hasSize( 1 )
                .containsEntry( c, fv );
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( FactorValue.class ), false ) )
                .hasSize( 1 )
                .containsEntry( c, fv );

        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( CellTypeAssignment.class ), false ) ).isEmpty();
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( GenericCellLevelCharacteristics.class ), false ) ).isEmpty();

        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( BibliographicReference.class ), false ) )
                .isEmpty();

        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( Gene2GOAssociation.class ), false ) )
                .isEmpty();

        assertThat( characteristicDao.getParents( Collections.singleton( danglingC ), null, true ) )
                .containsEntry( danglingC, null );
        assertThat( characteristicDao.getParents( Collections.singleton( danglingC ), Collections.emptyList(), true ) )
                .containsEntry( danglingC, null );
        assertThat( characteristicDao.getParents( Arrays.asList( eeC, danglingC ), null, true ) )
                .hasSize( 2 )
                .containsEntry( eeC, ee )
                .containsEntry( danglingC, null );
        assertThat( characteristicDao.getParents( Arrays.asList( eeC, danglingC ), Collections.singleton( ExpressionExperiment.class ), true ) )
                .hasSize( 2 )
                .containsEntry( eeC, ee )
                .containsEntry( danglingC, null );
        assertThat( characteristicDao.getParents( Arrays.asList( eeC, edC, danglingC ), Collections.singleton( ExpressionExperiment.class ), true ) )
                .hasSize( 2 )
                .containsEntry( eeC, ee )
                .containsEntry( danglingC, null )
                .doesNotContainEntry( edC, null );
    }

    @Test
    public void testGetParentsForMultiParent() {
        Characteristic c = createCharacteristic( "test", "test" );
        sessionFactory.getCurrentSession().persist( c );

        ExpressionExperiment ee = new ExpressionExperiment();
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );

        ExperimentalDesign ed = new ExperimentalDesign();
        ed.getTypes().add( c );
        sessionFactory.getCurrentSession().persist( ed );

        sessionFactory.getCurrentSession().flush();

        assertThat( characteristicDao.getParents( Collections.singleton( c ), null, false ) )
                .isEmpty();
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( ExpressionExperiment.class ), false ) )
                .containsEntry( c, ee );
        assertThat( characteristicDao.getParents( Collections.singleton( c ), Collections.singleton( ExperimentalDesign.class ), false ) )
                .containsEntry( c, ed );
    }

    @Test
    public void testFindValueGroupedByValueUri() {
        Characteristic c = createCharacteristic( "test", "test" );
        sessionFactory.getCurrentSession().persist( c );
        assertThat( characteristicDao.findValueGroupedByValueUri( null, true, true, true, -1 ) )
                .containsEntry( "test", "test" );
    }

    /**
     * The inference the whole thing exists for: a study annotated with a genotype AND a disease attests that
     * the genotype stands for the disease, so a later disease query can reach studies carrying only the
     * genotype.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindDiseaseModelInferences() {
        String rett = "http://purl.obolibrary.org/obo/MONDO_0010726";
        String mecp2 = "http://purl.org/commons/record/ncbi_gene/17257";
        createExperimentWithGenotypeAndDisease( "mouse", mecp2, "Homozygous negative Mecp2", rett, "Rett syndrome" );
        createExperimentWithGenotypeAndDisease( "mouse", mecp2, "Homozygous negative Mecp2", rett, "Rett syndrome" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        List<CharacteristicDao.DiseaseModelInference> inferences = characteristicDao.findDiseaseModelInferences(
                Collections.singleton( rett ), Collections.emptySet(), Collections.emptySet(),
                Collections.singleton( "genotype" ), Collections.emptySet(), 1, 0, 10 );

        assertThat( inferences ).singleElement().satisfies( i -> {
            assertThat( i.value ).isEqualTo( "Homozygous negative Mecp2" );
            assertThat( i.valueUri ).isEqualTo( mecp2 );
            assertThat( i.diseaseValueUri ).isEqualTo( rett );
            assertThat( i.numberOfExperiments ).isEqualTo( 2 );
            assertThat( i.numberOfExperimentsWithValue ).isEqualTo( 2 );
            assertThat( i.numberOfDiseasesAttested ).isEqualTo( 1 );
            assertThat( i.getSpecificity() ).isEqualTo( 1.0 );
            // a whole-experiment tag, not a factor value
            assertThat( i.numberOfExperimentsAsExperimentTag ).isEqualTo( 2 );
            assertThat( i.numberOfExperimentsAsFactorValue ).isZero();
        } );
    }

    /**
     * Asked from the model side — what an experiment page asks about its own genotype.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindDiseaseModelInferencesFromTheModelSide() {
        String alzheimer = "http://purl.obolibrary.org/obo/MONDO_0004975";
        // the strain-shaped genotypes have no URI at all, which is exactly why the value can seed the query
        createExperimentWithGenotypeAndDisease( "mouse", null, "APP/PS1", alzheimer, "Alzheimer disease" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        assertThat( characteristicDao.findDiseaseModelInferences( Collections.emptySet(), Collections.emptySet(),
                Collections.singleton( "APP/PS1" ), Collections.singleton( "genotype" ), Collections.emptySet(), 1, 0, 10 ) )
                .singleElement()
                .satisfies( i -> {
                    assertThat( i.value ).isEqualTo( "APP/PS1" );
                    assertThat( i.valueUri ).isNull();
                    assertThat( i.diseaseValue ).isEqualTo( "Alzheimer disease" );
                } );
    }

    /**
     * 🛑 The property the whole ranking turns on. A background strain co-occurs with every disease in the
     * corpus and models none of them — the obesity is diet-induced, the stroke surgical — so it must not
     * outrank a real model just by appearing more often. Support alone would put it on top.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testBackgroundStrainDoesNotOutrankAModel() {
        String obesity = "http://purl.obolibrary.org/obo/MONDO_0011122";
        String stroke = "http://purl.obolibrary.org/obo/MONDO_0005098";
        String lep = "http://purl.org/commons/record/ncbi_gene/16846";
        // the strain appears against obesity three times, and against another disease twice more
        for ( int i = 0; i < 3; i++ ) {
            createExperimentWithGenotypeAndDisease( "mouse", null, "C57BL/6J", obesity, "obesity" );
        }
        for ( int i = 0; i < 2; i++ ) {
            createExperimentWithGenotypeAndDisease( "mouse", null, "C57BL/6J", stroke, "ischemic stroke" );
        }
        // the model appears against obesity twice, and nowhere else
        for ( int i = 0; i < 2; i++ ) {
            createExperimentWithGenotypeAndDisease( "mouse", lep, "Homozygous negative Lep", obesity, "obesity" );
        }
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        List<CharacteristicDao.DiseaseModelInference> inferences = characteristicDao.findDiseaseModelInferences(
                Collections.singleton( obesity ), Collections.emptySet(), Collections.emptySet(),
                Collections.singleton( "genotype" ), Collections.emptySet(), 1, 0, 10 );

        assertThat( inferences ).hasSize( 2 );
        assertThat( inferences.get( 0 ).value )
                .as( "the specific model outranks the strain despite less support" )
                .isEqualTo( "Homozygous negative Lep" );
        assertThat( inferences.get( 0 ).numberOfExperiments ).isEqualTo( 2 );
        assertThat( inferences.get( 0 ).getSpecificity() ).isEqualTo( 1.0 );
        assertThat( inferences.get( 1 ).value ).isEqualTo( "C57BL/6J" );
        assertThat( inferences.get( 1 ).numberOfExperiments ).isEqualTo( 3 );
        assertThat( inferences.get( 1 ).numberOfExperimentsWithValue ).isEqualTo( 5 );
        assertThat( inferences.get( 1 ).numberOfDiseasesAttested ).isEqualTo( 2 );
        assertThat( inferences.get( 1 ).getSpecificity() ).isEqualTo( 0.6 );
    }

    /**
     * A human carrying a variant HAS the disease; a mouse carrying the null MODELS it. Same derivation, two
     * different claims, and the taxon is what decides.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testInferredCategoryFollowsTheTaxon() {
        String parkinson = "http://purl.obolibrary.org/obo/MONDO_0005180";
        String lrrk2 = "http://purl.org/commons/record/ncbi_gene/120892";
        createExperimentWithGenotypeAndDisease( "human", lrrk2, "G2019S/? LRRK2", parkinson, "Parkinson disease" );
        createExperimentWithGenotypeAndDisease( "mouse", lrrk2, "G2019S/? LRRK2", parkinson, "Parkinson disease" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        List<CharacteristicDao.DiseaseModelInference> inferences = characteristicDao.findDiseaseModelInferences(
                Collections.singleton( parkinson ), Collections.emptySet(), Collections.emptySet(),
                Collections.singleton( "genotype" ), Collections.emptySet(), 1, 0, 10 );

        // one row per taxon: the taxon is part of the grain because it changes what the inference says
        assertThat( inferences ).hasSize( 2 );
        assertThat( inferences )
                .filteredOn( i -> "human".equals( i.taxonCommonName ) )
                .singleElement()
                .satisfies( i -> assertThat( i.getInferredCategory() ).isEqualTo( Categories.DISEASE ) );
        assertThat( inferences )
                .filteredOn( i -> "mouse".equals( i.taxonCommonName ) )
                .singleElement()
                .satisfies( i -> assertThat( i.getInferredCategory() ).isEqualTo( Categories.DISEASE_MODEL ) );
    }

    /**
     * A control arm models nothing, whichever way it was written.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testWildTypeIsNotAModel() {
        String rett = "http://purl.obolibrary.org/obo/MONDO_0010726";
        createExperimentWithGenotypeAndDisease( "mouse", null, "wild type genotype", rett, "Rett syndrome" );
        createExperimentWithGenotypeAndDisease( "mouse", "http://www.ebi.ac.uk/efo/EFO_0005168", "WT", rett, "Rett syndrome" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        assertThat( characteristicDao.findDiseaseModelInferences( Collections.singleton( rett ),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton( "genotype" ),
                Collections.emptySet(), 1, 0, 10 ) ).isEmpty();
    }

    /**
     * Holding the dataset out is what makes "this tag is inferable, so it could be dropped" an honest claim
     * instead of a restatement of the tag being tested. With the only attesting dataset excluded, nothing is
     * left to infer from.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testExcludedExperimentsDoNotAttest() {
        String rett = "http://purl.obolibrary.org/obo/MONDO_0010726";
        String mecp2 = "http://purl.org/commons/record/ncbi_gene/17257";
        Long onlyWitness = createExperimentWithGenotypeAndDisease( "mouse", mecp2, "Homozygous negative Mecp2", rett, "Rett syndrome" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        assertThat( characteristicDao.findDiseaseModelInferences( Collections.singleton( rett ),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton( "genotype" ),
                Collections.emptySet(), 1, 0, 10 ) ).hasSize( 1 );
        assertThat( characteristicDao.findDiseaseModelInferences( Collections.singleton( rett ),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton( "genotype" ),
                Collections.singleton( onlyWitness ), 1, 0, 10 ) )
                .as( "the dataset carrying the tag cannot be its own evidence" )
                .isEmpty();
    }

    /**
     * The specificity cut has to be taken before the row cap, not after. Applied afterwards it silently turns
     * "the best 2 above this bar" into "however many of the best 2 clear it" — which for a corpus whose top
     * rows are background strains is usually none, and looks to the caller like nothing models the disease.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testSpecificityCutIsTakenBeforeTheLimit() {
        String obesity = "http://purl.obolibrary.org/obo/MONDO_0011122";
        String stroke = "http://purl.obolibrary.org/obo/MONDO_0005098";
        // two strains that out-support everything, each carrying an unrelated second disease so their
        // specificity against obesity is only 0.5
        for ( String strain : new String[] { "C57BL/6J", "BALB/c" } ) {
            for ( int i = 0; i < 3; i++ ) {
                createExperimentWithGenotypeAndDisease( "mouse", null, strain, obesity, "obesity" );
            }
            for ( int i = 0; i < 3; i++ ) {
                createExperimentWithGenotypeAndDisease( "mouse", null, strain, stroke, "ischemic stroke" );
            }
        }
        // ...and one real model, attested less often but against nothing else
        createExperimentWithGenotypeAndDisease( "mouse", null, "Homozygous negative Lep", obesity, "obesity" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();

        // unfiltered, the strains take both of the two slots on raw score
        assertThat( characteristicDao.findDiseaseModelInferences( Collections.singleton( obesity ),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton( "genotype" ),
                Collections.emptySet(), 1, 0, 2 ) )
                .extracting( i -> i.value )
                .containsExactlyInAnyOrder( "C57BL/6J", "BALB/c" );

        // with a bar set, the slots go to what clears it rather than being spent on rows that get dropped
        assertThat( characteristicDao.findDiseaseModelInferences( Collections.singleton( obesity ),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton( "genotype" ),
                Collections.emptySet(), 1, 0.9, 2 ) )
                .as( "the limit must be filled from rows above the threshold, not thinned by it" )
                .singleElement()
                .satisfies( i -> {
                    assertThat( i.value ).isEqualTo( "Homozygous negative Lep" );
                    assertThat( i.getSpecificity() ).isEqualTo( 1.0 );
                } );
    }

    /**
     * Neither side constrained is a request to enumerate the corpus, and is refused.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUnconstrainedInferenceReturnsNothing() {
        assertThat( characteristicDao.findDiseaseModelInferences( Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.singleton( "genotype" ), Collections.emptySet(), 1, 0, 10 ) )
                .isEmpty();
    }

    /**
     * @return the id of the created experiment
     */
    private Long createExperimentWithGenotypeAndDisease( String taxonCommonName, @Nullable String genotypeUri,
            String genotypeValue, String diseaseUri, String diseaseValue ) {
        Taxon taxon = getOrCreateTaxon( taxonCommonName );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( createCharacteristic( Categories.GENOTYPE, genotypeUri, genotypeValue ) );
        ee.getCharacteristics().add( createCharacteristic( Categories.DISEASE, diseaseUri, diseaseValue ) );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        return ee.getId();
    }

    private Taxon getOrCreateTaxon( String commonName ) {
        return taxa.computeIfAbsent( commonName, name -> {
            Taxon t = new Taxon();
            t.setCommonName( name );
            t.setNcbiId( "human".equals( name ) ? 9606 : 10090 );
            sessionFactory.getCurrentSession().persist( t );
            return t;
        } );
    }

    private Characteristic createCharacteristic( @Nullable String valueUri, String value ) {
        return createCharacteristic( Categories.UNCATEGORIZED, valueUri, value );
    }

    private Characteristic createCharacteristic( Category category, @Nullable String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setCategory( category.getCategory() );
        c.setCategoryUri( category.getCategoryUri() );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }

    @SuppressWarnings("SameParameterValue")
    private Statement createStatement( Category category, @Nullable String subjectUri, String subject ) {
        return Statement.Factory.newInstance( category, subject, subjectUri );
    }
}