package ubic.gemma.rest.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;

import java.util.ArrayList;
import java.util.Collections;

@Configuration
public class OpenApiConfig {

    private final Server[] additionalServers = {
            new Server().url( "https://gemma.msl.ubc.ca/rest/v2" ),
            new Server().url( "https://gemma-staging.msl.ubc.ca/rest/v2" ),
            new Server().url( "https://dev.gemma.msl.ubc.ca/rest/v2" ),
            new Server().url( "http://localhost:8080/rest/v2" )
    };

    @Value("${gemma.hosturl}")
    private String hostUrl;

    @Bean
    public FactoryBean<OpenAPI> openApi( CustomModelResolver customModelResolver, Environment environment ) {
        OpenApiFactory factory = new OpenApiFactory( "ubic.gemma.rest" );
        ArrayList<Server> servers = new ArrayList<>();
        String mainServerUrl = hostUrl + "/rest/v2";
        servers.add( new Server().url( mainServerUrl ) );
        if ( environment.acceptsProfiles( EnvironmentProfiles.DEV ) ) {
            // provide additional servers for development
            for ( Server additionalServer : additionalServers ) {
                if ( !additionalServer.getUrl().equals( mainServerUrl ) ) {
                    servers.add( additionalServer );
                }
            }
        }
        factory.setServers( servers );
        factory.setModelConverters( Collections.singletonList( customModelResolver ) );
        return factory;
    }
}
