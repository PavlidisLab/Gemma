package ubic.gemma.persistence.service;

import lombok.Data;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.core.context.TestComponent;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import jakarta.persistence.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        public LocalContainerEntityManagerFactoryBean entityManagerFactory( DataSource dataSource ) {
            // Phase 2: JPA EMF replaces the legacy LocalSessionFactoryBean.setAnnotatedClasses path.
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource( dataSource );
            emf.setPersistenceUnitName( "fakedao" );
            emf.setJpaVendorAdapter( new HibernateJpaVendorAdapter() );
            emf.setManagedTypes( PersistenceManagedTypes.of(
                    FakeModel.class.getName(),
                    FakeRelatedModel.class.getName() ) );
            Map<String, Object> props = new HashMap<>();
            props.put( "hibernate.dialect", H2Dialect.class.getName() );
            props.put( "hibernate.hbm2ddl.auto", "create" );
            props.put( "hibernate.cache.use_second_level_cache", "false" );
            props.put( "hibernate.cache.use_query_cache", "false" );
            emf.setJpaPropertyMap( props );
            return emf;
        }

        @Bean
        public SessionFactory sessionFactory( jakarta.persistence.EntityManagerFactory entityManagerFactory ) {
            return entityManagerFactory.unwrap( SessionFactory.class );
        }

        @Bean
        public FakeDao fakeDao( SessionFactory sessionFactory ) {
            return new FakeDao( sessionFactory );
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

    @Autowired
    private FakeDao fakeDao;

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
}