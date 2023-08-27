package ubic.gemma.core.security.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import gemma.gsec.model.Securable;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.annotations.SecuredField;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jackson module that registers a special serializer to handle {@link SecuredField} annotations.
 * @see SecuredField
 * @author poirigui
 */
@CommonsLog
public class SecuredFieldModule extends Module {

    private static final String SECURABLE_OWNER_ATTRIBUTE = "_securable_owner";

    private final AccessDecisionManager accessDecisionManager;

    public SecuredFieldModule( AccessDecisionManager accessDecisionManager ) {
        this.accessDecisionManager = accessDecisionManager;
    }

    @Override
    public String getModuleName() {
        return SecuredFieldModule.class.getName();
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule( SetupContext context ) {
        context.addBeanSerializerModifier( new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer( SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer ) {
                //noinspection unchecked
                return new SecuredFieldSerializer( ( JsonSerializer<Object> ) serializer, accessDecisionManager );
            }
        } );
    }

    /**
     * Jackson serializer for fields annotated with {@link ubic.gemma.model.annotations.SecuredField}.
     * @see ubic.gemma.model.annotations.SecuredField
     * @author poirigui
     */
    private static class SecuredFieldSerializer extends JsonSerializer<Object> implements ContextualSerializer {

        private final JsonSerializer<Object> fallbackSerializer;
        private final AccessDecisionManager accessDecisionManager;

        public SecuredFieldSerializer( JsonSerializer<Object> fallbackSerializer, AccessDecisionManager accessDecisionManager ) {
            this.fallbackSerializer = fallbackSerializer;
            this.accessDecisionManager = accessDecisionManager;
        }

        @Override
        public JsonSerializer<?> createContextual( SerializerProvider prov, BeanProperty property ) {
            return new JsonSerializer<Object>() {
                @Override
                public void serialize( Object value, JsonGenerator generator, SerializerProvider provider ) throws IOException {
                    if ( value instanceof Securable ) {
                        Object previousValue = provider.getAttribute( SECURABLE_OWNER_ATTRIBUTE );
                        try {
                            provider.setAttribute( SECURABLE_OWNER_ATTRIBUTE, value );
                            fallbackSerializer.serialize( value, generator, provider );
                        } finally {
                            provider.setAttribute( SECURABLE_OWNER_ATTRIBUTE, previousValue );
                        }
                        return;
                    }
                    if ( property == null || property.getAnnotation( SecuredField.class ) == null ) {
                        fallbackSerializer.serialize( value, generator, provider );
                        return;
                    }
                    SecuredField securedField = property.getAnnotation( SecuredField.class );
                    List<ConfigAttribute> configAttributes = Arrays.stream( securedField.value() )
                            .map( SecurityConfig::new )
                            .collect( Collectors.toList() );
                    Securable owner;
                    if ( provider.getAttribute( SECURABLE_OWNER_ATTRIBUTE ) instanceof Securable ) {
                        owner = ( Securable ) provider.getAttribute( SECURABLE_OWNER_ATTRIBUTE );
                    } else {
                        owner = null;
                    }
                    try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        // lookup any Securable entity
                        accessDecisionManager.decide( authentication, owner, configAttributes );
                        provider.defaultSerializeValue( value, generator );
                    } catch ( AccessDeniedException e ) {
                        log.trace( String.format( "Not authorized to access %s, the field will be omitted", value ) );
                        switch ( securedField.policy() ) {
                            case OMIT:
                                break;
                            case SET_NULL:
                                provider.defaultSerializeNull( generator );
                                break;
                            case RAISE_EXCEPTION:
                                throw e;
                        }
                    }
                }
            };
        }

        @Override
        public void serialize( Object value, JsonGenerator gen, SerializerProvider serializers ) throws IOException {
            // handled in createContextual() above
        }
    }
}