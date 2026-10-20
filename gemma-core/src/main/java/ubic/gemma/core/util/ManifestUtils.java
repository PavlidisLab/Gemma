package ubic.gemma.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ManifestUtils {

    private static final Map<String, String> gemmaManifestProperties = new HashMap<>();

    static {
        gemmaManifestProperties.put( "Gemma-Version", "gemma.version" );
        gemmaManifestProperties.put( "Gemma-Build-Timestamp", "gemma.build.timestamp" );
        gemmaManifestProperties.put( "Gemma-Build-GitHash", "gemma.build.gitHash" );
    }

    private static Properties cachedProps = null;

    /**
     * Read all the Gemma-related properties from the manifest files.
     */
    public static synchronized Properties readGemmaPropertiesFromManifest() throws IOException {
        if ( cachedProps != null ) {
            return cachedProps;
        }
        Properties gemmaProps = new Properties();
        Enumeration<URL> urls = ManifestUtils.class.getClassLoader().getResources( JarFile.MANIFEST_NAME );
        while ( urls.hasMoreElements() ) {
            URL url = urls.nextElement();
            try ( InputStream is = url.openStream() ) {
                Manifest manifest = new Manifest( is );
                for ( Map.Entry<String, String> e : gemmaManifestProperties.entrySet() ) {
                    String val = manifest.getMainAttributes().getValue( e.getKey() );
                    if ( val != null ) {
                        gemmaProps.setProperty( e.getValue(), val );
                    }
                }
            }
        }
        cachedProps = gemmaProps;
        return gemmaProps;
    }
}
