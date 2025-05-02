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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pavlidis
 * @deprecated should not be part of Gemma main code
 */
@Deprecated
public class MeshTermFetcherCli extends AbstractCLI {

    private static final int CHUNK_SIZE = 10;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private String file;
    private boolean majorTopicsOnly = false;

    @Override
    public String getCommandName() {
        return "fetchMeshTerms";
    }

    @Override
    public String getShortDesc() {
        return "Gets MESH headings for a set of pubmed ids";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
        Option fileOption = Option.builder( "f" )
                .longOpt( "file" )
                .required()
                .hasArg()
                .argName( "Id file" )
                .build();
        options.addOption( fileOption );
        options.addOption( "m", "Use major subjects only" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            this.file = commandLine.getOptionValue( 'f' );
        }
        if ( commandLine.hasOption( 'm' ) ) {
            this.majorTopicsOnly = true;
        }
    }

    @Override
    protected void doWork() throws Exception {
        PubMedSearch fetcher = new PubMedSearch( ncbiApiKey );
        List<String> ids = this.readIdsFromFile( file ).stream().distinct().collect( Collectors.toList() );
        for ( List<String> chunk : ListUtils.partition( ids, MeshTermFetcherCli.CHUNK_SIZE ) ) {
            this.processChunk( fetcher, chunk );
        }
    }

    private Collection<String> readIdsFromFile( String inFile ) throws IOException {
        log.info( "Reading " + inFile );
        Collection<String> ids = new ArrayList<>();
        try ( BufferedReader in = new BufferedReader( new FileReader( file ) ) ) {
            String line;
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( "#" ) )
                    continue;
                ids.add( StringUtils.strip( line ) );
            }
        }
        return ids;
    }

    private void processChunk( PubMedSearch fetcher, Collection<String> pubMedIds ) throws IOException {
        Collection<BibliographicReference> refs = fetcher.retrieve( pubMedIds );

        for ( BibliographicReference r : refs ) {
            getCliContext().getOutputStream().print( r.getPubAccession().getAccession() + "\t" );
            Collection<MedicalSubjectHeading> meshTerms = r.getMeshTerms();
            List<String> t = new ArrayList<>();
            for ( MedicalSubjectHeading mesh : meshTerms ) {
                String term = mesh.getTerm();
                if ( majorTopicsOnly && !mesh.getIsMajorTopic() )
                    continue;
                t.add( term );
            }

            Collections.sort( t );
            getCliContext().getOutputStream().print( StringUtils.join( t, "|" ) );

            getCliContext().getOutputStream().print( "\n" );
        }
    }

}
