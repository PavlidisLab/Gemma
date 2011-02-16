package ubic.gemma.loader.expression.smd.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ubic.gemma.loader.expression.smd.util.SmdUtil;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;

/**
 * <p>
 * A set of microarrays in one SMDExperiment corresponds to a MAGE::Experiment.
 * <p>
 * Information comes from a SMD "expset_XXX.meta" file. These are sorta-XML files that look like this:
 * 
 * <pre>
 *         &lt;experiment_set&gt;
 *         !Name=PLA2G2A and human gastric cancers
 *                                      !ExptSetNo=1743
 *                                      !Description=We analyzed gene expression patterns in human gastric cancers by using cDNA microarrays representing approximately 30,300 genes. Expression of PLA2G2A, a gene previously implicated as a modifier of the Apc(Mi\
 *                                      n/+) (multiple intestinal neoplasia 1) mutant phenotype in the mouse, was significantly correlated with patient survival. We confirmed this observation in an independent set of patient samples by using quantitative RT-PCR. Beyond\
 *                                      its potential diagnostic and prognostic significance, this result suggests the intriguing possibility that the activity of PLA2G2A may suppress progression or metastasis of human gastric cancer.
 *                                      &lt;experiment&gt;
 *                                      !Name=GC (HKG10L)
 *                                      !Exptid=16709
 *                                      &lt;/experiment&gt;
 *                                      &lt;experiment&gt;
 *                                      !Name=GC (HKG10N)
 *                                      !Exptid=16253
 *                                      &lt;/experiment&gt;
 *                                     ....
 * </pre>
 * <p>
 * (Ending with a &lt;/experiment_set&gt;). Note that SMD uses a naming convention that deviates from MAGE. An
 * "experiment_set" is like a MAGE experiment. A "experiment" is a BioAssay.
 * </p>
 * <p>
 * This file contains:
 * <ul>
 * <li>Experiment set name
 * <li>Experiment set number
 * <li>Description
 * <li>List of specific experiments (microarrays) that make up the data set.
 * </ul>
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SMDExperiment {

    private String name;
    private int number;
    private String description;
    private List<SMDBioAssay> experiments; // a vector of experiment (er, bioassay) objects.
    private int publicationId;

    public ExpressionExperiment toExperiment( String d ) {
        ExpressionExperiment result = new ExpressionExperimentImpl();

        result.setSource( ( new SmdUtil() ).getHost() );
        result.setDescription( d );
        result.setName( name );

        return result;
    }

    public void setNumber( int number ) {
        this.number = number;
    }

    public SMDExperiment() {
        experiments = new Vector<SMDBioAssay>();
    }

    /**
     * @throws IOException
     * @throws SAXException
     * @param fileName
     * @throws IOException
     */
    public void read( String fileName ) throws IOException, SAXException {
        File infile = new File( fileName );
        if ( !infile.exists() || !infile.canRead() ) {
            throw new IOException( "Could not read from file " + fileName );
        }
        FileInputStream stream = new FileInputStream( infile );
        this.read( stream );
        stream.close();
    }

    /**
     * @throws SAXException
     * @param inputStream
     * @throws IOException
     */
    public void read( InputStream stream ) throws IOException, SAXException {
        System.setProperty( "org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser" );

        XMLReader xr = XMLReaderFactory.createXMLReader();
        ExptMetaHandler handler = new ExptMetaHandler();
        xr.setFeature( "http://xml.org/sax/features/validation", false );
        xr.setFeature( "http://xml.org/sax/features/external-general-entities", false );
        xr.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
        xr.setContentHandler( handler );
        xr.setErrorHandler( handler );
        xr.setEntityResolver( handler );
        xr.setDTDHandler( handler );
        xr.parse( new InputSource( stream ) );

    }

    class ExptMetaHandler extends DefaultHandler {

        boolean inSet = false;
        boolean inExp = false;
        StringBuilder expSetBuf;
        StringBuilder expBuf;

        @Override
        public void startElement( String uri, String value, String qName, Attributes atts ) {

            if ( value.equals( "experiment_set" ) ) {
                inSet = true;
                expSetBuf = new StringBuilder();
            } else if ( value.equals( "experiment" ) ) {
                inExp = true;
                expBuf = new StringBuilder();
            } else {
                throw new IllegalStateException( "Unexpected tag '" + value + "' encountered." );
            }
        }

        @Override
        @SuppressWarnings( { "synthetic-access" })
        public void endElement( String uri, String tagName, String qName ) {
            if ( tagName.equals( "experiment_set" ) && !inExp ) {
                inSet = false;
                String expSetStuff = expSetBuf.toString();
                String[] expSetString = expSetStuff.split( SmdUtil.SMD_DELIM );
                for ( int i = 0; i < expSetString.length; i++ ) {
                    String k = expSetString[i];

                    String[] vals = SmdUtil.smdSplit( k );

                    if ( vals == null ) continue;

                    String key = vals[0];
                    String value = "";
                    if ( vals.length > 1 ) {
                        value = vals[1];
                    }

                    if ( key.equals( "Name" ) ) {
                        name = value;
                    } else if ( key.equals( "Description" ) ) {
                        description = value;
                    } else if ( key.equals( "ExptSetNo" ) ) {
                        number = Integer.parseInt( value );
                    } else {
                        throw new IllegalStateException( "Invalid key '" + key + "' found in metadata file" );
                    }

                }

            } else if ( tagName.equals( "experiment" ) ) {
                inExp = false;
                String expStuff = expBuf.toString();
                String[] expString = expStuff.split( SmdUtil.SMD_DELIM );
                SMDBioAssay newExp = new SMDBioAssay();

                for ( int i = 0; i < expString.length; i++ ) {
                    String k = expString[i];

                    String[] vals = SmdUtil.smdSplit( k );
                    if ( vals == null ) continue;

                    String key = vals[0];
                    String value = "";
                    if ( vals.length > 1 ) {
                        value = vals[1];
                    }

                    if ( key.equals( "Name" ) ) {
                        newExp.setName( value );
                    } else if ( key.equals( "Exptid" ) ) {
                        newExp.setId( Integer.parseInt( value ) );
                    } else {
                        throw new IllegalStateException( "Invalid key '" + key + "' found in metadata file" );
                    }

                }

                experiments.add( newExp );

            }
        }

        @Override
        public void characters( char ch[], int start, int length ) {

            if ( inSet ) {
                if ( inExp ) {
                    expBuf.append( ch, start, length );
                } else {
                    expSetBuf.append( ch, start, length );
                }
            }
        }

    }

    @Override
    public String toString() {
        return number + "\t" + name + "\t" + description;
    }

    public int getPublicationId() {
        return publicationId;
    }

    public void setPublicationId( int publicationId ) {
        this.publicationId = publicationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public List<SMDBioAssay> getExperiments() {
        return experiments;
    }

    public void setExperiments( List<SMDBioAssay> experiments ) {
        this.experiments = experiments;
    }
}
