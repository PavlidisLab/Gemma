package ubic.gemma.core.loader.expression.sra;

import lombok.extern.apachecommons.CommonsLog;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.loader.expression.sra.model.SraContact;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackage;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackageSet;
import ubic.gemma.core.loader.expression.sra.model.SraPlatform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parses SRA XML format.
 * @author poirigui
 */
@CommonsLog
public class SraXmlParser {

    public SraExperimentPackageSet parse( InputStream in ) throws IOException {
        try {
            Document doc = EntrezXmlUtils.parse( in );
            JAXBContext jc = JAXBContext.newInstance( SraExperimentPackageSet.class );
            Unmarshaller um = jc.createUnmarshaller();
            SraExperimentPackageSet result = ( SraExperimentPackageSet ) um.unmarshal( doc );
            // parse stuff that JAXB cannot handle well
            fillPlatforms( result, doc );
            fillContacts( result, doc );
            return result;
        } catch ( JAXBException e ) {
            throw new RuntimeException( e );
        }
    }

    private void fillPlatforms( SraExperimentPackageSet result, Document doc ) {
        NodeList ln = doc.getElementsByTagName( "PLATFORM" );
        if ( ln.getLength() != result.getExperimentPackages().size() ) {
            log.warn( "Number of platforms does not match number of experiment packages." );
            return;
        }
        int i = 0;
        for ( SraExperimentPackage ps : result.getExperimentPackages() ) {
            fillPlatform( ps.getExperiment().getPlatform(), ln.item( i++ ) );
        }
    }

    private void fillPlatform( SraPlatform platform, Node item ) {
        platform.setInstrumentPlatform( item.getFirstChild().getNodeName() );
        platform.setInstrumentModel( item.getFirstChild().getTextContent() );
    }

    private void fillContacts( SraExperimentPackageSet result, Document doc ) {
        NodeList c = doc.getElementsByTagName( "Contact" );
        if ( c.getLength() != result.getExperimentPackages().size() ) {
            log.warn( "Number of contacts does not match number of experiment packages." );
            return;
        }
        int j = 0;
        for ( SraExperimentPackage ps : result.getExperimentPackages() ) {
            fillContact( ps.getOrganization().getContact(), c.item( j++ ) );
        }
    }

    private void fillContact( SraContact e, Node item ) {
        for ( Node n = item.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if ( n.getNodeName().equals( "Name" ) ) {
                fillName( e, n );
            } else if ( n.getNodeName().equals( "Address" ) ) {
                fillAddress( e, n );
            }
        }
    }

    private void fillName( SraContact e, Node n ) {
        for ( Node n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling() ) {
            switch ( n2.getNodeName() ) {
                case "First":
                    e.setFirstName( n2.getTextContent() );
                    break;
                case "Last":
                    e.setLastName( n2.getTextContent() );
                    break;
            }
        }
    }

    private void fillAddress( SraContact e, Node n ) {
        for ( Node n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling() ) {
            switch ( n2.getNodeName() ) {
                case "Street":
                    e.setStreet( n2.getTextContent() );
                    break;
                case "City":
                    e.setCity( n2.getTextContent() );
                    break;
                case "Country":
                    e.setCountry( n2.getTextContent() );
                    break;
            }
        }
    }
}
