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

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

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

    private String fileName;

    public static void main( String[] args ) {
        ArrayDesignProbeRenamerCli a = new ArrayDesignProbeRenamerCli();
        try {
            Exception e = a.doWork( args );
            if ( e != null ) {
                AbstractCLI.log.fatal( e, e );
            }
        } catch ( RuntimeException e ) {
            AbstractCLI.log.fatal( e, e );
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        //noinspection AccessStaticViaInstance
        this.addOption( OptionBuilder.isRequired().hasArg().withArgName( "file" )
                .withDescription( "Two-column file with old and new identifiers (additional columns ignored)" )
                .create( 'f' ) );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.fileName = this.getOptionValue( 'f' );
    }

    @Override
    public String getCommandName() {
        return "probeRename";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = this.processCommandLine( args );
        if ( e != null ) {
            return e;
        }

        if ( fileName == null ) {
            throw new IllegalArgumentException( "filename cannot be null" );
        }

        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();
        arrayDesign = this.thaw( arrayDesign );

        File file = new File( fileName );
        if ( !file.canRead() ) {
            return new IOException( "Cannot read from " + fileName );
        }
        try (InputStream newIdFile = new FileInputStream( file )) {

            this.rename( arrayDesign, newIdFile );
            newIdFile.close();
            this.audit( arrayDesign, "Probes renamed using file " + fileName );
        } catch ( Exception ex ) {
            return ex;
        }

        return null;
    }

    private void rename( ArrayDesign arrayDesign, InputStream newIdFile ) {
        Map<String, String> old2new;
        try {
            old2new = this.parseIdFile( newIdFile );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        AbstractCLI.log.info( old2new.size() + " potential renaming items read" );

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
                    AbstractCLI.log.info( "Renamed " + count + " composite sequences, last to be renamed was " + cs );
                }
            }

        }

        getArrayDesignService().update( arrayDesign );
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        super.getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignProbeRenamingEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private Map<String, String> parseIdFile( InputStream newIdFile ) throws IOException {
        try (BufferedReader br = new BufferedReader( new InputStreamReader( newIdFile ) )) {

            String line;

            Map<String, String> old2new = new HashMap<>();
            while ( ( line = br.readLine() ) != null ) {
                String[] fields = line.split( "\t" );
                if ( fields.length < 2 )
                    continue;
                String originalProbeName = fields[0];
                String newProbeName = fields[1];

                if ( old2new.containsKey( newProbeName ) ) {
                    AbstractCLI.log.warn( newProbeName + " is a duplicate, will mangle to make unique" );
                    String candidateNewProbeName = newProbeName;
                    int i = 1;
                    while ( old2new.containsKey( newProbeName ) ) {
                        newProbeName = candidateNewProbeName + DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR + "Dup" + i;
                        i++;
                        // just in case...
                        if ( i > 100 ) {
                            AbstractCLI.log.warn( "Was unable to create unique probe name for " + originalProbeName );
                        }
                    }
                }

                old2new.put( originalProbeName, newProbeName );
            }

            return old2new;
        }
    }

}
