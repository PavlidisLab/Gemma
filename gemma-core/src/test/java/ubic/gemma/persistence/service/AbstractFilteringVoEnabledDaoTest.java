package ubic.gemma.persistence.service;

import lombok.Data;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.HibernateSessionFactoryBean;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.core.context.TestComponent;

import org.springframework.lang.Nullable;
import javax.sql.DataSource;
import jakarta.persistence.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class AbstractFilteringVoEnabledDaoTest extends BaseTest {

    @TestComponent
    @Configuration
    static class AbstractFilteringVoEnabledDaoTestContextConfiguration {

        @Bean
        public DataSource dataSource() {
            return new SimpleDriverDataSource( new Driver(), "jdbc:h2:mem:fakedaotest;DB_CLOSE_DELAY=-1" );
        }

        @Bean
        public HibernateSessionFactoryBean sessionFactory( DataSource dataSource ) {
            HibernateSessionFactoryBean factory = new HibernateSessionFactoryBean();
            factory.setDataSource( dataSource );
            factory.setAnnotatedClasses( FakeModel.class, FakeRelatedModel.class );
            Properties props = new Properties();
            props.setProperty( "hibernate.dialect", H2Dialect.class.getName() );
            props.setProperty( "hibernate.hbm2ddl.auto", "create" );
            props.setProperty( "hibernate.cache.use_second_level_cache", "false" );
            props.setProperty( "hibernate.cache.use_query_cache", "false" );
            // ManagedSessionContext: required so that getCurrentSession() resolves in the lightweight
            // (no Spring TM) test fixture used below for the JPA-Criteria filtering DAO tests.
            props.setProperty( "hibernate.current_session_context_class", "managed" );
            factory.setHibernateProperties( props );
            return factory;
        }

        @Bean
        public FakeDao fakeDao( SessionFactory sessionFactory ) {
            return new FakeDao( sessionFactory );
        }

        @Bean
        public FakeCriteriaDao fakeCriteriaDao( SessionFactory sessionFactory ) {
            return new FakeCriteriaDao( sessionFactory );
        }
    }

    public enum FakeEnum {
        FOO, BAR, JOHN, DOE
    }

    @Data
    @Entity
    static class FakeModel implements Identifiable {

        @Id
        private Long id;

        private String name;

        @Enumerated(EnumType.ORDINAL)
        private FakeEnum enumByOrdinal;

        @Enumerated(EnumType.STRING)
        private FakeEnum enumByName;

        @ElementCollection
        private Collection<String> collectionOfStrings;

        @OneToOne
        private FakeRelatedModel fakeRelatedModel;
    }

    @Data
    @Entity
    static class FakeRelatedModel implements Identifiable {

        @Id
        private Long id;
        private String name;
    }

    static class FakeModelVo extends IdentifiableValueObject<FakeModel> {
    }

    static class FakeDao extends AbstractFilteringVoEnabledDao<FakeModel, FakeModelVo> {

        @Autowired
        public FakeDao( SessionFactory sessionFactory ) {
            super( "fake", FakeModel.class, sessionFactory );
        }

        @Override
        protected FakeModelVo doLoadValueObject( FakeModel entity ) {
            return null;
        }

        @Override
        public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
            return null;
        }

        @Override
        public List<FakeModel> load( @Nullable Filters filters, @Nullable Sort sort ) {
            return null;
        }

        @Override
        public Slice<FakeModel> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
            return null;
        }

        @Override
        public long count( @Nullable Filters filters ) {
            return 0;
        }

        @Override
        public Slice<FakeModelVo> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
            return null;
        }

        @Override
        public List<FakeModelVo> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
            return null;
        }
    }

    /**
     * Minimal subclass that exercises the JPA-Criteria filtering path (subquery + .size filters).
     */
    static class FakeCriteriaDao extends AbstractCriteriaFilteringVoEnabledDao<FakeModel, FakeModelVo> {

        @Autowired
        public FakeCriteriaDao( SessionFactory sessionFactory ) {
            super( FakeModel.class, sessionFactory );
        }

        @Override
        protected FakeModelVo doLoadValueObject( FakeModel entity ) {
            return null;
        }
    }

    @Autowired
    private FakeDao fakeDao;

    @Autowired
    private FakeCriteriaDao fakeCriteriaDao;

    @Test
    public void test() {
        assertThat( fakeDao.getFilterableProperties() )
                .contains( "id", "name", "enumByOrdinal", "enumByName", "collectionOfStrings.size", "fakeRelatedModel.name" );
        assertThat( fakeDao.getFilterablePropertyMeta( "id" ) )
                .hasFieldOrPropertyWithValue( "propertyType", Long.class );
        assertThat( fakeDao.getFilterablePropertyMeta( "enumByOrdinal" ) )
                .hasFieldOrPropertyWithValue( "propertyType", FakeEnum.class );
        assertThat( fakeDao.getFilterablePropertyMeta( "enumByOrdinal" ).getAllowedValues() )
                .containsExactlyInAnyOrderElementsOf( EnumSet.allOf( FakeEnum.class ) );
        assertThat( fakeDao.getFilterablePropertyMeta( "enumByName" ) )
                .hasFieldOrPropertyWithValue( "propertyType", FakeEnum.class );
        assertThat( fakeDao.getFilterablePropertyMeta( "enumByName" ).getAllowedValues() )
                .containsExactlyInAnyOrderElementsOf( EnumSet.allOf( FakeEnum.class ) );
        assertThat( fakeDao.getFilterablePropertyMeta( "collectionOfStrings.size" ) )
                .hasFieldOrPropertyWithValue( "propertyType", Integer.class );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUndefinedProperty() {
        fakeDao.getFilterablePropertyMeta( "missing" );
    }

    /**
     * Exercises the .size-suffix filter path on the JPA-Criteria filtering DAO. Pre-Phase-2 this
     * went through the deleted Hibernate Criteria API; Phase 2 round 6 restored it via
     * {@code cb.size(...)} on a Path<Collection<?>>.
     * <p>
     * Uses a manually-opened session bound to the thread (no Spring TM in this lightweight test
     * context) so the DAO's {@code getCurrentSession()} call resolves.
     */
    @Test
    public void testSizeFilterOnCriteriaDao() {
        runInSession( () -> {
            Filters filters = Filters.by( Filter.by( null, "collectionOfStrings.size", Integer.class, Filter.Operator.greaterThan, 0 ) );
            assertThat( fakeCriteriaDao.count( filters ) ).isEqualTo( 0L );
            assertThat( fakeCriteriaDao.load( filters, null ) ).isEmpty();
        } );
    }

    /**
     * Exercises null-precedence (FIRST/LAST) via the Hibernate-6 JpaOrder vendor extension.
     */
    @Test
    public void testNullPrecedenceOnCriteriaDao() {
        runInSession( () -> {
            Sort nullsFirst = Sort.by( null, "name", Sort.Direction.ASC, Sort.NullMode.FIRST );
            assertThat( fakeCriteriaDao.load( null, nullsFirst ) ).isEmpty();
            Sort nullsLast = Sort.by( null, "name", Sort.Direction.ASC, Sort.NullMode.LAST );
            assertThat( fakeCriteriaDao.load( null, nullsLast ) ).isEmpty();
        } );
    }

    @Autowired
    private SessionFactory sessionFactory;

    private void runInSession( Runnable r ) {
        org.hibernate.Session s = sessionFactory.openSession();
        org.hibernate.context.internal.ManagedSessionContext.bind( s );
        try {
            r.run();
        } finally {
            org.hibernate.context.internal.ManagedSessionContext.unbind( sessionFactory );
            s.close();
        }
    }
}