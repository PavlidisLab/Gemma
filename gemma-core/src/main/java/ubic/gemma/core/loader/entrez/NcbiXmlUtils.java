package ubic.gemma.core.loader.entrez;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * XML utilities specific to NCBI Entrez.
 */
public class NcbiXmlUtils {

    /**
     * Create a document builder with {@link NcbiEntityResolver} as entity resolver.
     * <p>
     * This will work for most of not all XML files from NCBI Entrez and related services.
     */
    public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = ubic.gemma.core.util.XMLUtils.createDocumentBuilder();
        builder.setEntityResolver( new NcbiEntityResolver() );
        return builder;
    }
}
