package gemma.gsec.acl;

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import gemma.gsec.model.SecuredChild;
import org.junit.Test;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectIdentityRetrievalStrategyImplTest {

    static class Parent implements Securable {

        @Override
        public Long getId() {
            return 1L;
        }
    }

    static class Child implements SecuredChild {

        @Override
        public Long getId() {
            return 2L;
        }

        @Override
        public Securable getSecurityOwner() {
            return new Parent();
        }
    }

    static class ParentValueObject implements SecureValueObject {

        @Override
        public Long getId() {
            return 3L;
        }

        @Override
        public boolean getIsPublic() {
            return false;
        }

        @Override
        public boolean getIsShared() {
            return false;
        }

        @Override
        public Class<? extends Securable> getSecurableClass() {
            return Parent.class;
        }

        @Override
        public boolean getUserCanWrite() {
            return false;
        }

        @Override
        public boolean getUserOwned() {
            return false;
        }

        @Override
        public void setIsPublic( boolean isPublic ) {

        }

        @Override
        public void setIsShared( boolean isShared ) {

        }

        @Override
        public void setUserCanWrite( boolean userCanWrite ) {

        }

        @Override
        public void setUserOwned( boolean isUserOwned ) {

        }
    }

    private final ObjectIdentityRetrievalStrategy strategy = new ObjectIdentityRetrievalStrategyImpl();

    @Test
    public void test() {
        assertThat( strategy.getObjectIdentity( new Parent() ) )
            .hasFieldOrPropertyWithValue( "identifier", 1L )
            .hasFieldOrPropertyWithValue( "type", Parent.class.getName() );
        assertThat( strategy.getObjectIdentity( new Child() ) )
            .hasFieldOrPropertyWithValue( "identifier", 2L )
            .hasFieldOrPropertyWithValue( "type", Child.class.getName() );
    }

    @Test
    public void testValueObject() {
        assertThat( strategy.getObjectIdentity( new ParentValueObject() ) )
            .hasFieldOrPropertyWithValue( "identifier", 3L )
            .hasFieldOrPropertyWithValue( "type", Parent.class.getName() );
    }
}