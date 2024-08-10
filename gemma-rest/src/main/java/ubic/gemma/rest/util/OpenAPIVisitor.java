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
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Visits an {@link OpenAPI} specification and perform string value resolution.
 * @author poirigui
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor
 */
public class OpenAPIVisitor {

    private final StringValueResolver stringValueResolver;
    private final Map<Class<?>, PropertyDescriptor[]> cachedDescriptors = new HashMap<>();

    public OpenAPIVisitor( StringValueResolver stringValueResolver ) {
        this.stringValueResolver = stringValueResolver;
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
        visitExtensions( variables.getExtensions() );
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
            String newVal = stringValueResolver.resolveStringValue( ( String ) mediaType.getExample() );
            if ( !Objects.equals( newVal, mediaType.getExample() ) ) {
                mediaType.setExample( newVal );
            }
        }
        visitExtensions( mediaType.getExtensions() );
    }

    private void visitSchema( Schema<?> schema ) {
        if ( schema == null )
            return;
        visitStringProperties( schema, Schema.class );
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
    }

    private void visitStringProperties( Object obj, Class<?> clazz ) {
        PropertyDescriptor[] descriptors = cachedDescriptors.computeIfAbsent( clazz, BeanUtils::getPropertyDescriptors );
        for ( PropertyDescriptor descriptor : descriptors ) {
            if ( descriptor.getName().equals( "$ref" ) ) {
                continue;
            }
            if ( descriptor.getPropertyType().equals( String.class ) ) {
                try {
                    String rawVal = ( String ) descriptor.getReadMethod().invoke( obj );
                    String newVal = stringValueResolver.resolveStringValue( rawVal );
                    if ( !Objects.equals( newVal, rawVal ) ) {
                        descriptor.getWriteMethod().invoke( obj, newVal );
                    }
                } catch ( IllegalAccessException | InvocationTargetException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
    }
}
