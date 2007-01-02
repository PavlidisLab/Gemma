/*
 * The Gemma project.
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

package ubic.gemma.expression.arrayDesign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.util.ConfigUtils;


/**
 * @author jsantos
 * @spring.bean name="arrayDesignReportService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignReportService
{
    private Log log = LogFactory.getLog( this.getClass() );
    
    private String ARRAY_DESIGN_SUMMARY = "AllArrayDesignsSummary";
    private String ARRAY_DESIGN_REPORT_DIR = "ArrayDesignReports";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private ArrayDesignService arrayDesignService;
    
    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }
    
    @SuppressWarnings("unchecked")
    public void generateArrayDesignReport()
    {
        initDirectories();
        generateAllArrayDesignReport();
        Collection<ArrayDesignValueObject> ads = arrayDesignService.loadAllValueObjects();
        for ( ArrayDesignValueObject ad : ads ) {
            generateArrayDesignReport(ad.getId());
        }
    }
    
    public void generateArrayDesignReport(long id)
    {

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( id );
        
        log.info( "Generating report for array design " + id + "\n" );
        
        long numCsBioSequences = arrayDesignService.numCompositeSequenceWithBioSequences( ad );
        long numCsBlatResults = arrayDesignService.numCompositeSequenceWithBlatResults( ad );
        long numCsGenes = arrayDesignService.numCompositeSequenceWithGenes( ad );
        long numCsPredictedGenes = arrayDesignService.numCompositeSequenceWithPredictedGenes( ad );
        long numCsProbeAlignedRegions = arrayDesignService.numCompositeSequenceWithProbeAlignedRegion( ad );
        long numCsPureGenes = numCsGenes - numCsPredictedGenes  - numCsProbeAlignedRegions;
        long numGenes = arrayDesignService.numGenes( ad );
        
        String report = this.generateReportString( numCsBioSequences, numCsBlatResults, numCsGenes, numGenes, numCsPredictedGenes, numCsProbeAlignedRegions, numCsPureGenes );

        // write into file
        File f = new File(HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY + "." + id);
        f.delete();
        try {
            f.createNewFile();
            Writer writer = new FileWriter(f);
            writer.write( report );
            writer.flush();
        } catch ( IOException e ) {
            // cannot write to file. Just fail gracefully.
            log.error( "Cannot write to file." );
        }
    }

    public void generateAllArrayDesignReport()
    {
        log.info( "Generating report for all array designs\n" );
        
        long numCsBioSequences = arrayDesignService.numAllCompositeSequenceWithBioSequences(  );
        long numCsBlatResults = arrayDesignService.numAllCompositeSequenceWithBlatResults(  );
        long numCsGenes = arrayDesignService.numAllCompositeSequenceWithGenes(  );
        long numGenes = arrayDesignService.numAllGenes(  );
        
        String report = this.generateReportString( numCsBioSequences, numCsBlatResults, numCsGenes, numGenes );
        // write into file
        File f = new File(HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY);
        f.delete();
        try {
            f.createNewFile();
            Writer writer = new FileWriter(f);
            writer.write( report.toString() );
            writer.flush();
        } catch ( IOException e ) {
            // cannot write to file. Just fail gracefully.
            log.error( "Cannot write to file." );
        }
    }


    public java.lang.String getArrayDesignReport(Long id)
    {
         // read file into return string
        
        InputStream istr = null;
        try {
            istr = FileTools.getInputStreamFromPlainOrCompressedFile(HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY + "." + id);
        } 
        catch (Exception e) {
            return "No summary available";
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(istr) );
        StringBuffer report = new StringBuffer();
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                report.append( str );
            }
        } catch ( IOException e ) {
            return "";
        }
        return report.toString();
    }
    
    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignReportService#getArrayDesignReport()
     */
    public java.lang.String getArrayDesignReport()
    {
         // read file into return string
        
        InputStream istr = null;
        try {
            istr = FileTools.getInputStreamFromPlainOrCompressedFile(HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY);
        } 
        catch (Exception e) {
            return "";
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(istr) );
        StringBuffer report = new StringBuffer();
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                report.append( str );
            }
        } catch ( IOException e ) {
            return "";
        }
        return report.toString();
    }

    private void initDirectories() {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR );
        File f = new File(HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR);
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        FileTools.deleteFiles( files );
    }
    
    private String generateReportString(long numCsBioSequences, long numCsBlatResults, long numCsGenes, long numGenes) {
        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd hh:mm" );
        // write into table format
        StringBuffer s = new StringBuffer();
        s.append("<table class='datasummary'>" +
                "<tr>" +
                "<td colspan=2 align=center>" +
                "</td></tr>" +
                "<authz:authorize ifAnyGranted=\"admin\"><tr><td>" +
                "Probes with sequences" +
                "</td><td>" +
                numCsBioSequences + 
                "</td></tr>" +
                "<tr><td>" +
                "Probes with genome alignments" +
                "</td>" +
                "<td>" + numCsBlatResults + 
                "</td></tr></authz:authorize>" +
                "<tr><td>" +
                "Probes mapping to gene(s)" +
                "</td><td>" +
                numCsGenes +
                "</td></tr>" +
                "<tr><td>" +
                "Unique genes represented" +
                "</td><td>" +
                numGenes + 
                "</td></tr>" +
                "<tr><td colspan=2 align='center' class='small'>" +
                "(as of " + timestamp + ")" +
                "</td></tr>" + 
                "</table>");
        return s.toString();
    }
    
    private String generateReportString(long numCsBioSequences, long numCsBlatResults, long numCsGenes, long numGenes, long numCsPredictedGenes, long numCsProbeAlignedRegions, long numCsPureGenes) {
        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd hh:mm" );
        // write into table format
        StringBuffer s = new StringBuffer();
        s.append("<table class='datasummary'>" +
                "<tr>" +
                "<td colspan=2 align=center>" +
                "</td></tr>" +
                "<authz:authorize ifAnyGranted=\"admin\"><tr><td>" +
                "Probes with sequences" +
                "</td><td>" +
                numCsBioSequences + 
                "</td></tr>" +
                "<tr><td>" +
                "Probes with genome alignments" +
                "</td>" +
                "<td>" + numCsBlatResults + 
                "</td></tr></authz:authorize>" +
                "<tr><td>" +
                "Probes mapping to gene(s)" +
                "</td><td>" +
                numCsGenes +
                "</td></tr>" +
                
                "<tr><td>" +
                "&nbsp;&nbsp;Probes mapping to probe-aligned region(s)" +
                "</td><td>" +
                numCsProbeAlignedRegions +
                "</td></tr>" +
                
                "<tr><td>" +
                "&nbsp;&nbsp;Probes mapping to predicted genes" +
                "</td><td>" +
                numCsPredictedGenes +
                "</td></tr>" +
                
                "<tr><td>" +
                "&nbsp;&nbsp;Probes mapping to known genes" +
                "</td><td>" +
                numCsPureGenes +
                "</td></tr>" +
                
                "<tr><td>" +
                "Unique genes represented" +
                "</td><td>" +
                numGenes + 
                "</td></tr>" +
                "<tr><td colspan=2 align='center' class='small'>" +
                "(as of " + timestamp + ")" +
                "</td></tr>" + 
                "</table>");
        return s.toString();
    }


}