package ubic.gemma.core.image.aba;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.core.config.Settings;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Provides the hooks to load data from aba
 */
class AbaLoader {

    /**
     * see
     * http://help.brain-map.org/display/api/Allen+Brain+Atlas+API and
     * http://help.brain-map.org/display/mousebrain/API
     * for documentation
     */
    private static final String API_BASE_URL = "http://api.brain-map.org/api/v2";

    /**
     * 1st parameter: Gene NCBI ID
     */
    private static final String GET_GENE_URL = "/data/Gene/query.xml?criteria=rma::criteria,[entrez_id$eq@]";

    /**
     * 1st parameter: Image ID, get through the IMAGESERIES ABA API
     * 2nd parameter: Downsampling factor
     * 3rd parameter: View type
     * - leave blank for normal picture
     * - "=expression" for expression highlight
     */
    private static final String GET_IMAGE_URL = "/image_download/@?downsample=@&view@";

    /**
     * 1st parameter: Gene NCBI ID
     * 2nd parameter: Image space
     * - 9 for coronal
     * - 10 for sagittal
     */
    private static final String GET_IMAGE_SERIES_URL = "/data/SectionDataSet/query.xml?criteria=rma::criteria,genes[entrez_id$eq@],reference_space[id$eq@],rma::include,section_images";

    private static final Log log = LogFactory.getLog( AbaLoader.class.getName() );

    String getGeneUrl( Gene gene ) {
        String args[] = { gene.getNcbiGeneId().toString() };
        return this.buildUrlString( AbaLoader.GET_GENE_URL, args );
    }

    String getImageUrl(int id){
        String args[] = { String.valueOf( id ), "5", "" };
        return this.buildUrlString( AbaLoader.GET_IMAGE_URL, args );
    }

    Document getAbaGeneXML( Gene gene ) {
        return this.getDocument( gene, "gene" );
    }

    Document getAbaGeneSagittalImages( Gene gene ) {
        return this.getDocument( gene, "sagittal" );
    }

    private Document getDocument( Gene gene, String type ) {
        File outputFile = this.getFile( type + "_" + gene.getNcbiGeneId().toString() );
        Document document;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            switch ( type ) {
                default:
                case "gene":
                    this.writeGene( gene, out );
                    break;
                case "sagittal":
                    this.writeSagittalImageSeries( gene, out );
                    break;
            }
        } catch ( IOException e ) {
            AbaLoader.log.error( e.getMessage(), e.getCause() );
            e.printStackTrace();
            return null;
        }

        try (FileInputStream input = new FileInputStream( outputFile )) {
            document = XMLUtils.openAndParse( input );
        } catch ( ParserConfigurationException | IOException | SAXException e ) {
            AbaLoader.log.error( e );
            return null;
        }

        return document;
    }

    private void writeGene( Gene gene, OutputStream out ) throws IOException {

        String args[] = { gene.getNcbiGeneId().toString() };
        String getGeneUrl = this.buildUrlString( AbaLoader.GET_GENE_URL, args );

        this.writeUrlResponse( getGeneUrl, out );
    }

    private void writeSagittalImageSeries( Gene gene, OutputStream out ) throws IOException {

        String args[] = { gene.getNcbiGeneId().toString(), "10" };
        String getImageSeriesUrl = this.buildUrlString( AbaLoader.GET_IMAGE_SERIES_URL, args );

        this.writeUrlResponse( getImageSeriesUrl, out );
    }

    private File getFile( String fileName ) {

        String dir = Settings.getString( "gemma.appdata.home" ) + "/abaCache/";
        File outputFile = new File( dir + "aba_" + fileName + ".xml" );

        if ( outputFile.exists() ) {
            EntityUtils.deleteFile( outputFile );

            // wait for file to be deleted before proceeding
            int i = 5;
            while ( ( i > 0 ) && ( outputFile.exists() ) ) {
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException ie ) {
                    AbaLoader.log.error( ie );
                }
                i--;
            }

        }
        return outputFile;

    }

    /**
     * Given a predefined URL (one of the constants declared in the AllenBrainAtlasService) and a list of arguments will
     * return the correct REST URL for the desired method call
     *
     * @param urlPattern url pattern
     * @param args       arguments
     * @return full url
     */
    private String buildUrlString( String urlPattern, String args[] ) {

        for ( String arg : args )
            urlPattern = urlPattern.replaceFirst( "@", arg );

        return ( AbaLoader.API_BASE_URL + urlPattern );
    }

    private DataInputStream getInput( URL url ) throws IOException {

        HttpURLConnection conn = ( HttpURLConnection ) url.openConnection();
        conn.connect();
        DataInputStream in = new DataInputStream( conn.getInputStream() );

        if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK ) {
            AbaLoader.log.error( conn.getResponseMessage() );
            return ( null );
        }

        return ( in );
    }

    private void writeUrlResponse( String urlString, OutputStream out ) throws IOException {

        URL url = new URL( urlString );
        try (DataInputStream in = this.getInput( url )) {
            if ( in == null )
                return;
            this.transferData( in, out );
        }
    }

    private void transferData( DataInputStream in, OutputStream out ) throws IOException {
        // This is whacked. There must be a better way than throwing an exception.
        boolean EOF = false;
        while ( !EOF ) {
            try {
                out.write( in.readUnsignedByte() );
            } catch ( EOFException eof ) {
                EOF = true;
            }
        }
    }

}
