/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.util.AbstractCLI;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MeshTermFetcherCli extends AbstractCLI {

    private static final int CHUNK_SIZE = 10;

    /**
     * @param args
     */
    public static void main( String[] args ) {
        MeshTermFetcherCli p = new MeshTermFetcherCli();
        Exception exception = p.doWork( args );
        if ( exception != null ) {
            log.error( exception, exception );
        }
    }

    private String file;
    private boolean majorTopicsOnly = false;

    @Override
    public String getShortDesc() {
        return "Gets MESH headings for a set of pubmed ids";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.isRequired().hasArg().withArgName( "Id file" )
                .withDescription( "List of pubmed ids" ).withLongOpt( "file" ).create( 'f' );
        this.addOption( fileOption );

        this.addOption( OptionBuilder.withDescription( "Use major subjects only" ).create( 'm' ) );

    }

    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( "Load PubMed records", args );
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();

        try {
            Collection<Integer> ids = readIdsFromFile( file );
            Collection<Integer> chunk = new ArrayList<Integer>();
            for ( Integer i : ids ) {

                chunk.add( i );

                if ( chunk.size() == CHUNK_SIZE ) {

                    processChunk( fetcher, chunk );
                    chunk.clear();
                }
            }

            if ( !chunk.isEmpty() ) {
                processChunk( fetcher, chunk );
            }

        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    @Override
    protected void processOptions() {
        if ( this.hasOption( 'f' ) ) {
            this.file = this.getOptionValue( 'f' );
        }
        if ( this.hasOption( 'm' ) ) {
            this.majorTopicsOnly = true;
        }
    }

    protected Collection<Integer> readIdsFromFile( String inFile ) throws IOException {
        log.info( "Reading " + inFile );

        Collection<Integer> ids = new ArrayList<Integer>();
        BufferedReader in = new BufferedReader( new FileReader( file ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) continue;

            ids.add( Integer.parseInt( line ) );

        }
        return ids;
    }

    private Collection<BibliographicReference> processChunk( PubMedXMLFetcher fetcher, Collection<Integer> ids )
            throws IOException {
        Collection<BibliographicReference> refs = fetcher.retrieveByHTTP( ids );

        for ( BibliographicReference r : refs ) {
            System.out.print( r.getPubAccession().getAccession() + "\t" );
            Collection<MedicalSubjectHeading> meshTerms = r.getMeshTerms();
            List<String> t = new ArrayList<String>();
            for ( MedicalSubjectHeading mesh : meshTerms ) {
                String term = mesh.getTerm();
                if ( majorTopicsOnly && !mesh.getIsMajorTopic() ) continue;
                t.add( term );
            }

            Collections.sort( t );
            System.out.print( StringUtils.join( t, "|" ) );

            System.out.print( "\n" );
        }
        return refs;
    }

}
