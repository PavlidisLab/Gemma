package ubic.gemma.rest.util;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Visits an {@link OpenAPI} specification and perform string value resolution.
 * @author poirigui
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor
 */
public class OpenAPIVisitor extends BeanDefinitionVisitor {

    private final Map<Class<?>, PropertyDescriptor[]> cachedDescriptors = new HashMap<>();

    public OpenAPIVisitor( StringValueResolver stringValueResolver ) {
        super( stringValueResolver );
    }

    public void visit( OpenAPI openAPI ) {
        visitInfo( openAPI.getInfo() );
        if ( openAPI.getServers() != null ) {
            openAPI.getServers().forEach( this::visitServer );
        }
        visitPaths( openAPI.getPaths() );
        visitComponents( openAPI.getComponents() );
        visitExternalDocs( openAPI.getExternalDocs() );
        visitExtensions( openAPI.getExtensions() );
    }

    private void visitServer( Server server ) {
        visitStringProperties( server, Server.class );
        visitServerVariables( server.getVariables() );
        visitExtensions( server.getExtensions() );
    }

    private void visitServerVariables( ServerVariables variables ) {
        if ( variables == null )
            return;
        variables.values().forEach( this::visitServerVariable );
        visitExtensions( variables.getExtensions() );
    }

    private void visitServerVariable( ServerVariable serverVariable ) {
        visitStringProperties( serverVariable, ServerVariable.class );
        visitExtensions( serverVariable.getExtensions() );
    }

    private void visitInfo( Info info ) {
        visitStringProperties( info, Info.class );
        visitLicense( info.getLicense() );
    }

    private void visitLicense( License license ) {
        if ( license == null )
            return;
        visitStringProperties( license, License.class );
        visitExtensions( license.getExtensions() );
    }

    private void visitPaths( Paths paths ) {
        paths.values().forEach( this::visitPathItem );
    }

    private void visitPathItem( PathItem pathItem ) {
        visitOperation( pathItem.getGet() );
        visitExtensions( pathItem.getExtensions() );
    }

    private void visitOperation( Operation operation ) {
        visitStringProperties( operation, Operation.class );
        visitApiResponses( operation.getResponses() );
        visitExternalDocs( operation.getExternalDocs() );
        visitExtensions( operation.getExtensions() );
    }

    private void visitApiResponses( ApiResponses responses ) {
        responses.values().forEach( this::visitApiResponse );
        visitExtensions( responses.getExtensions() );
    }

    private void visitApiResponse( ApiResponse apiResponse ) {
        visitStringProperties( apiResponse, ApiResponse.class );
        visitContent( apiResponse.getContent() );
        visitExtensions( apiResponse.getExtensions() );
    }

    private void visitComponents( Components components ) {
        if ( components.getSchemas() != null ) {
            components.getSchemas().values().forEach( this::visitSchema );
        }
        if ( components.getSecuritySchemes() != null ) {
            components.getSecuritySchemes().values().forEach( this::visitSecurityScheme );
        }
        if ( components.getPathItems() != null ) {
            components.getPathItems().values().forEach( this::visitPathItem );
        }
        if ( components.getParameters() != null ) {
            components.getParameters().values().forEach( this::visitParameter );
        }
        visitExtensions( components.getExtensions() );
    }

    private void visitSecurityScheme( SecurityScheme securityScheme ) {
        visitStringProperties( securityScheme, SecurityScheme.class );
    }

    private void visitParameter( Parameter parameter ) {
        visitStringProperties( parameter, Parameter.class );
        visitSchema( parameter.getSchema() );
        visitContent( parameter.getContent() );
    }

    private void visitContent( Content content ) {
        if ( content == null )
            return;
        content.values().forEach( this::visitMediaType );
    }

    private void visitMediaType( MediaType mediaType ) {
        visitSchema( mediaType.getSchema() );
        if ( mediaType.getExample() instanceof String ) {
            String newVal = resolveStringValue( ( String ) mediaType.getExample() );
            if ( !mediaType.getExample().equals( newVal ) ) {
                mediaType.setExample( newVal );
            }
        }
        visitExtensions( mediaType.getExtensions() );
    }

    private void visitSchema( Schema<?> schema ) {
        if ( schema == null )
            return;
        visitStringProperties( schema, Schema.class );
        if ( schema.getAllOf() != null ) {
            schema.getAllOf().forEach( this::visitSchema );
        }
        if ( schema.getAnyOf() != null ) {
            schema.getAnyOf().forEach( this::visitSchema );
        }
        if ( schema.getOneOf() != null ) {
            schema.getOneOf().forEach( this::visitSchema );
        }
        if ( schema.getProperties() != null ) {
            schema.getProperties().values().forEach( this::visitSchema );
        }
        if ( schema.getContentSchema() != null ) {
            visitSchema( schema.getContentSchema() );
        }
        visitExternalDocs( schema.getExternalDocs() );
        visitExtensions( schema.getExtensions() );
    }

    private void visitExternalDocs( ExternalDocumentation externalDocs ) {
        if ( externalDocs == null )
            return;
        visitStringProperties( externalDocs, ExternalDocumentation.class );
        visitExtensions( externalDocs.getExtensions() );
    }

    private void visitExtensions( Map<String, Object> extensions ) {
        if ( extensions == null )
            return;
        visitMap( extensions );
    }

    private <T> void visitStringProperties( T obj, Class<T> clazz ) {
        PropertyDescriptor[] descriptors = cachedDescriptors.computeIfAbsent( clazz, BeanUtils::getPropertyDescriptors );
        for ( PropertyDescriptor descriptor : descriptors ) {
            if ( descriptor.getName().startsWith( "$" ) ) {
                // internal properties, i.e. $ref or $schema
                continue;
            }
            if ( !descriptor.getPropertyType().equals( String.class ) ) {
                continue;
            }
            try {
                String rawVal = ( String ) descriptor.getReadMethod().invoke( obj );
                if ( rawVal != null ) {
                    String newVal = resolveStringValue( rawVal );
                    if ( !rawVal.equals( newVal ) ) {
                        descriptor.getWriteMethod().invoke( obj, newVal );
                    }
                }
            } catch ( IllegalAccessException | InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }
    }
}
