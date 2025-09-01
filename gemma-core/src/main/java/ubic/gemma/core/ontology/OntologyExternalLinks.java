package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.LinkedHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Provides external links for ontology resources based on their URIs and other metadata.
 * @author poirigui
 */
@CommonsLog
public class OntologyExternalLinks {

    /**
     * Resource used to load external links patterns.
     */
    private final Resource resource = new ClassPathResource( "ubic/gemma/core/ontology/ontology.externalLinks.txt" );

    private final boolean autoReload;

    private final LinkedHashMap<String, String> externalLinks = new LinkedHashMap<>();

    /**
     *
     * @param autoReload if true, the external links will be reloaded from the classpath for every call, <b>do not use
     *                   this in production!</b>.
     * @throws IOException if loading the links from the classpath fails
     */
    public OntologyExternalLinks( boolean autoReload ) throws IOException {
        this.autoReload = autoReload;
        reload();
    }

    private void reload() throws IOException {
        synchronized ( externalLinks ) {
            externalLinks.clear();
            try ( InputStream is = resource.getInputStream() ) {
                for ( String line : IOUtils.readLines( requireNonNull( is ), StandardCharsets.UTF_8 ) ) {
                    line = StringUtils.strip( line );
                    // ignore comments
                    if ( line.startsWith( "#" ) ) {
                        continue;
                    }
                    String[] parts = line.split( "\t", 2 );
                    if ( parts.length != 2 ) {
                        throw new IllegalArgumentException( "Invalid line for an external link: " + line );
                    }
                    externalLinks.put( parts[0], parts[1] );
                }
            }
            log.debug( "Known ontology external links: " + externalLinks );
        }
    }

    public String getExternalLink( OntologyResource resource ) {
        Assert.notNull( resource.getUri() );
        if ( autoReload ) {
            try {
                reload();
            } catch ( IOException e ) {
                log.error( "Failed to reload ontology externa links, the current ones will be used.", e );
            }
        }
        String prefix = null;
        String pattern = null;
        synchronized ( externalLinks ) {
            for ( String ontologyPrefix : externalLinks.keySet() ) {
                if ( resource.getUri().startsWith( ontologyPrefix ) ) {
                    prefix = ontologyPrefix;
                    pattern = externalLinks.get( ontologyPrefix );
                    break;
                }
            }
        }
        if ( prefix != null && pattern != null ) {
            return MessageFormat.format( pattern,
                    urlEncode( resource.getUri() ),
                    urlEncode( resource.getLocalName() ),
                    // do not URL-encode since the string is already a valid URI
                    resource.getUri().substring( prefix.length() ),
                    urlEncode( getOboId( resource ) ) );
        } else {
            log.warn( "Unsupported ontology prefix for resource: " + resource.getUri() + ", will simply replace http:// with https://" );
            return resource.getUri().replaceFirst( "^http://", "https://" );
        }
    }

    @Nullable
    private String getOboId( OntologyResource resource ) {
        if ( resource instanceof OntologyTerm ) {
            try {
                AnnotationProperty annotationProperty = ( ( OntologyTerm ) resource )
                        .getAnnotation( "http://www.geneontology.org/formats/oboInOwl#id" );
                if ( annotationProperty != null ) {
                    return annotationProperty.getContents();
                }
            } catch ( UnsupportedOperationException e ) {
                // ignore
            }
        }

        // last resort, use ':' instead of '_' in the local name
        if ( resource.getLocalName() != null ) {
            return resource.getLocalName().replace( '_', ':' );
        }

        return null;
    }

    private String urlEncode( String s ) {
        if ( s == null ) {
            return null;
        }
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
