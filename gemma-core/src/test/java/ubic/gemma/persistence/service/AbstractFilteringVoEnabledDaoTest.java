package ubic.gemma.persistence.service;

import lombok.Data;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.hibernate.LocalSessionFactoryBean;
import ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.persistence.*;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class AbstractFilteringVoEnabledDaoTest extends AbstractJUnit4SpringContextTests {

    @TestComponent
    @Configuration
    static class AbstractFilteringVoEnabledDaoTestContextConfiguration {

        @Bean
        public FactoryBean<SessionFactory> sessionFactory() {
            LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
            factoryBean.getHibernateProperties().setProperty( "hibernate.dialect", MySQL57InnoDBDialect.class.getName() );
            factoryBean.setAnnotatedClasses(
                    FakeModel.class, FakeEnum.class, FakeRelatedModel.class
            );
            return factoryBean;
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
            return Collections.emptyList();
        }

        @Override
        public List<FakeModel> load( @Nullable Filters filters, @Nullable Sort sort ) {
            return Collections.emptyList();
        }

        @Override
        public Slice<FakeModel> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
            return new Slice<>( Collections.emptyList(), null, null, null, null );
        }

        @Override
        public long count( @Nullable Filters filters ) {
            return 0;
        }

        @Override
        public Slice<FakeModelVo> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
            return new Slice<>( Collections.emptyList(), null, null, null, null );
        }

        @Override
        public List<FakeModelVo> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
            return Collections.emptyList();
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