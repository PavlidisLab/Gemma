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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import ubic.gemma.core.lang.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingServiceImpl.DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR;

/**
 * Put new names on probes. This is needed in some cases where probes were given generic names that weren't helpful for
 * matching them to sequences.
 *
 * @author pavlidis
 * @deprecated renaming probes is not a good idea in general
 */
@Deprecated
public class ArrayDesignProbeRenamerCli extends ArrayDesignSequenceManipulatingCli {

    private static String FILE_OPT = "f";

    private String fileName;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        //noinspection AccessStaticViaInstance
        options.addOption( Option.builder( FILE_OPT )
                .longOpt( "file" )
                .required()
                .hasArg()
                .desc( "Two-column file with old and new identifiers (additional columns ignored)" )
                .build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        this.fileName = commandLine.getOptionValue( FILE_OPT );
    }

    @Override
    public String getCommandName() {
        return "probeRename";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    protected void doWork() throws Exception {
        if ( fileName == null ) {
            throw new IllegalArgumentException( "filename cannot be null" );
        }

        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();
        arrayDesign = getArrayDesignService().thaw( arrayDesign );

        File file = new File( fileName );
        if ( !file.canRead() ) {
            throw new IOException( "Cannot read from " + fileName );
        }
        try ( InputStream newIdFile = new FileInputStream( file ) ) {

            this.rename( arrayDesign, newIdFile );
            newIdFile.close();
            this.audit( arrayDesign, "Probes renamed using file " + fileName );
        } catch ( Exception ex ) {
            throw ex;
        }
    }

    private void rename( ArrayDesign arrayDesign, InputStream newIdFile ) {
        Map<String, String> old2new;
        try {
            old2new = this.parseIdFile( newIdFile );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        log.info( old2new.size() + " potential renaming items read" );

        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            if ( old2new.containsKey( cs.getName() ) ) {
                String descriptionAddendum = " [Renamed by Gemma from " + cs.getName() + "]";
                if ( StringUtils.isNotBlank( cs.getDescription() ) ) {
                    cs.setDescription( cs.getDescription() + descriptionAddendum );
                } else {
                    cs.setDescription( descriptionAddendum );
                }

                cs.setName( old2new.get( cs.getName() ) );

                if ( ++count % 2000 == 0 ) {
                    log.info( "Renamed " + count + " composite sequences, last to be renamed was " + cs );
                }
            }

        }

        getArrayDesignService().update( arrayDesign );
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        super.getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignProbeRenamingEvent.class, note );
    }

    private Map<String, String> parseIdFile( InputStream newIdFile ) throws IOException {
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( newIdFile ) ) ) {

            String line;

            Map<String, String> old2new = new HashMap<>();
            while ( ( line = br.readLine() ) != null ) {
                String[] fields = line.split( "\t" );
                if ( fields.length < 2 )
                    continue;
                String originalProbeName = fields[0];
                String newProbeName = fields[1];

                if ( old2new.containsKey( newProbeName ) ) {
                    log.warn( newProbeName + " is a duplicate, will mangle to make unique" );
                    String candidateNewProbeName = newProbeName;
                    int i = 1;
                    while ( old2new.containsKey( newProbeName ) ) {
                        newProbeName = candidateNewProbeName + DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR + "Dup" + i;
                        i++;
                        // just in case...
                        if ( i > 100 ) {
                            log.warn( "Was unable to create unique probe name for " + originalProbeName );
                        }
                    }
                }

                old2new.put( originalProbeName, newProbeName );
            }

            return old2new;
        }
    }

}
