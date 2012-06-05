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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeRenamingService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Put new names on probes. This is needed in some cases where probes were given generic names that weren't helpful for
 * matching them to sequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeRenamerCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignProbeRenamerCli a = new ArrayDesignProbeRenamerCli();
        try {
            Exception e = a.doWork( args );
            if ( e != null ) {
                log.fatal( e, e );
            }
        } catch ( RuntimeException e ) {
            log.fatal( e, e );
        }
    }

    private String fileName;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        addOption( OptionBuilder.isRequired().hasArg().withArgName( "file" )
                .withDescription( "Two-column file with old and new identifiers (additional columns ignored)" )
                .create( 'f' ) );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "replace array design probe names", args );
        if ( e != null ) {
            return e;
        }

        if ( fileName == null ) {
            throw new IllegalArgumentException( "filename cannot be null" );
        }

        if ( this.arrayDesignsToProcess.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.arrayDesignsToProcess.iterator().next();
        arrayDesign = unlazifyArrayDesign( arrayDesign );

        ArrayDesignProbeRenamingService arrayDesignProbeRenamingService = this
                .getBean( ArrayDesignProbeRenamingService.class );

        try {
            File file = new File( fileName );
            if ( !file.canRead() ) {
                throw new IOException( "Cannot read from " + fileName );
            }
            InputStream newIdFile = new FileInputStream( file );
            arrayDesignProbeRenamingService.reName( arrayDesign, newIdFile );
            newIdFile.close();
            audit( arrayDesign, "Probes renamed using file " + fileName );
        } catch ( Exception ex ) {
            return ex;
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        this.fileName = getOptionValue( 'f' );

    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        super.arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignProbeRenamingEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
