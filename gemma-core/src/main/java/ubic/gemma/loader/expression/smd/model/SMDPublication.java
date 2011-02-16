/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.loader.expression.smd.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ubic.gemma.loader.expression.smd.util.SmdUtil;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryImpl;

/**
 * A publication in SMD has a special role: it refers to SMD experiment sets (MAGE::Experiment's). Otherwise it has much
 * in common with a BibliographicReference.
 * <p>
 * Example of the file format:
 * 
 * <pre>
 * 
 *      &lt;publication&gt;
 *      !Citation=Garber ME, et al. (2001) Proc Natl Acad Sci USA 98(24):13784-13789
 *      !Title=Diversity of gene expression in adenocarcinoma of the lung.
 *      !PubMedID=11707590
 *        &lt;experiment_set&gt;
 *          !Name=Garber ME, et al. (2001) Proc Natl Acad Sci USA 98(24):13784-13789
 *          !ExptSetNo=810
 *          !Description=The global gene expression profiles for 67 human lung tumors representing 
 *          56 patients were examined by using 24,000-element cDNA microarrays. 
 *          Subdivision of the tumors based on gene expression patterns faithfully recapitulated
 *           morphological classification of the tumors into squamous, large cell, small cell, and 
 *           adenocarcinoma. The gene expression patterns made possible the subclassification of 
 *           adenocarcinoma into subgroups that correlated with the degree of tumor differentiation
 *            as well as patient survival. Gene expression analysis thus promises to extend and refine
 *             standard pathologic analysis.
 *        &lt;/experiment_set&gt;
 *      &lt;/publication&gt;
 * 
 * 
 * </pre>
 * 
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SMDPublication {

    String citation;
    String title;
    String pubMedId;
    List<SMDExperiment> experimentSets; // vector of ExptMetas (Experiment_sets)...but without the individual
    // experiments filled in
    // until later.
    private int id;

    public DatabaseEntry toDataBaseEntry() {
        DatabaseEntry d = new DatabaseEntryImpl();
        d.setAccession( pubMedId );
        return d;
    }

    /**
     * @return an object of type BibliographicReference representing this object.
     */
    public BibliographicReference toBiblioGraphicReference( DatabaseEntry d ) {
        BibliographicReference result = new BibliographicReferenceImpl();

        result.setCitation( this.citation );
        result.setTitle( this.title );
        result.setPubAccession( d );
        return result;
    }

    /**
     * 
     */
    public SMDPublication() {
        experimentSets = new ArrayList<SMDExperiment>();
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

        if ( stream == null ) {
            throw new IllegalArgumentException( "Input stream is null" );
        }

        if ( stream.available() == 0 ) {
            // throw new IOException( "Input stream has no bytes available" );
        }

        System.setProperty( "org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser" );

        XMLReader xr = XMLReaderFactory.createXMLReader();
        PublicationMetaHandler handler = new PublicationMetaHandler();
        xr.setFeature( "http://xml.org/sax/features/validation", false );
        xr.setFeature( "http://xml.org/sax/features/external-general-entities", false );
        xr.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
        xr.setContentHandler( handler );
        xr.setErrorHandler( handler );
        xr.setEntityResolver( handler );
        xr.setDTDHandler( handler );
        xr.parse( new InputSource( stream ) );

    }

    class PublicationMetaHandler extends DefaultHandler {
        boolean inSet = false;
        boolean inPub = false;
        private StringBuilder expSetBuf;
        private StringBuilder pubBuf;

        @Override
        public void startElement( String uri, String name, String qName, Attributes atts ) {

            if ( name.equals( "experiment_set" ) ) {
                inSet = true;
                expSetBuf = new StringBuilder();
            } else if ( name.equals( "publication" ) ) {
                inPub = true;
                pubBuf = new StringBuilder();
            } else {
                throw new IllegalStateException( "Unexpected tag '" + name + "' encountered." );
            }
        }

        @Override
        public void endElement( String uri, String tagName, String qName ) {
            if ( tagName.equals( "publication" ) && !inSet ) {
                inPub = false;
                String expSetStuff = pubBuf.toString();
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

                    if ( key.equals( "Citation" ) ) {
                        citation = value;
                    } else if ( key.equals( "Title" ) ) {
                        title = value;
                    } else if ( key.equals( "PubMedID" ) ) {
                        pubMedId = value;
                    } else {
                        throw new IllegalStateException( "Invalid key '" + key + "' found in publication metadata file" );
                    }

                }

            } else if ( tagName.equals( "experiment_set" ) ) {
                inSet = false;
                String expStuff = expSetBuf.toString();
                String[] expString = expStuff.split( SmdUtil.SMD_DELIM );
                SMDExperiment newExpSet = new SMDExperiment();

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
                        newExpSet.setName( value );
                    } else if ( key.equals( "ExptSetNo" ) ) {
                        newExpSet.setNumber( Integer.parseInt( value ) );
                    } else if ( key.equals( "Description" ) ) {
                        newExpSet.setDescription( value );
                    } else {
                        throw new IllegalStateException( "Invalid key '" + key + "' found in publication metadata file" );
                    }

                }

                experimentSets.add( newExpSet );

            }
        }

        @Override
        public void characters( char ch[], int start, int length ) {

            if ( inPub ) {
                if ( inSet ) {
                    expSetBuf.append( ch, start, length );
                } else {
                    pubBuf.append( ch, start, length );
                }
            }
        }
    }

    /**
     * Returns info in tab-delimited string.
     */
    @Override
    public String toString() {
        return id + "\t" + citation + "\t" + title + "\t" + pubMedId;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation( String citation ) {
        this.citation = citation;
    }

    public List<SMDExperiment> getExperimentSets() {
        return experimentSets;
    }

    public void setExperimentSets( List<SMDExperiment> experimentSets ) {
        this.experimentSets = experimentSets;
    }

    public String getPubMedId() {
        return pubMedId;
    }

    public void setPubMedId( String pubMedId ) {
        this.pubMedId = pubMedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @return
     */
    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }
}