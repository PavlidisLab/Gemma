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
package ubic.gemma.loader.expression.smd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.SAXException;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.expression.smd.model.SMDBioAssay;
import ubic.gemma.loader.expression.smd.model.SMDPerson;
import ubic.gemma.loader.expression.smd.model.SMDQuantitationType;
import ubic.gemma.loader.expression.smd.util.SmdUtil;

/**
 * Parse a SMD data file (for one microarray assay) to get out information about the BioAssay, and optionally get the
 * data into a String matrix format. Compressed files are fine.
 * <p>
 * From these files we can get the following information (besides basic idenifiers and the expression data)
 * <ul>
 * <li>Some annotations "pharmacogenetics"/"cell line"
 * <li>Contact information for the sample.
 * <li>Information on scanning software and array print configuration.
 * <li>Information on the samples
 * </ul>
 * <p>
 * We're not much interested in the sequence annotations contained in the files, we do our own.
 * <p>
 * The SMD file format has three parts:
 * <h2>Parameters</h2>
 * <p>
 * Note that there can be missing values for these.
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *                          !Exptid=3823
 *                          !Experiment Name=COLO205_CL4010_COLON
 *                          !Organism=Homo sapiens
 *                          !Category=Cell-line
 *                          !Subcategory=Pharmacogenomics
 *                          !Experimenter=Douglas Ross
 *                          !Contact email=dross{@link @}cmgm.stanford.edu
 *                          !Contact Address1=Genetics
 *                          !Contact Address2=M309
 *                          !State=CA
 *                          !Postal Code=94305
 *                          !SlideName=dtp2847
 *                          !Printname=10k_Print3
 *                          !Tip Configuration=Standard 4-tip
 *                          !Columns per Sector=50
 *                          !Rows per Sector=50
 *                          !Column Spacing=137
 *                          !Row Spacing=137
 *                          !Channel 1 Description=Reference_Pool
 *                          !Channel 2 Description=COLO205_CL4010_COLON
 *                          !Scanning Software=ScanAlyze
 *                          !Software version=2.44
 *                          !Scanning parameters=
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * 
 * <h2>Column Header</h2>
 * <p>
 * This is a single line.
 * <p>
 * According to {@link http://genome-www5.stanford.edu/help/FAQ.shtml#Column%20Names}. (the software used is listed in
 * the file). Here are the headings from a test file (and their interpretations). We should not assume these are always
 * the same...but for now we will. 'blank' means it is blank in files we looked at. See also {@link http
 * ://genome-www5.stanford.edu/cgi-bin/tools/viewSchema.pl?table=RESULT}, but the columns in these data files DO NOT
 * NECESSARILY match those. Therefore we really can only use the columns here that we make sense of. Note that data
 * entered before January 2001 does not contain these median result data.
 * <p>
 * Here is a scanalyze example.
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *                           SPOT -- an integer, which goes up as rows go up.
 *                           NAME -- For things other than controls, this seems to be an integer.
 *                           Clone ID -- often IMAGE:XXXX
 *                           Gene Symbol -- official symbol, if known
 *                           Gene Name -- official name, if known 
 *                           Cluster ID -- Unigene cluster
 *                           Accession  -- genbank accession number
 *                           Preferred name -- Seems to be blank mostly, but may be an alternative name for the gene.
 *                           SUID  -- internal identifier of the clone (?)
 *                           CH1I_MEAN -- Mean spot pixel intensity at Channel 1 (usually 532 nm).
 *                           CH1D_MEDIAN -- Median spot pixel intensity at Channel 1 with the median-background subtracted (CH1I_MEDIAN - CH1B_MEDIAN).
 *                           CH1I_MEDIAN
 *                           CH1_PER_SAT
 *                           CH1I_SD -- Standard deviation of the spot intensity at Channel 1
 *                           CH1B_MEAN -- Mean spot background in Channel 1 (usually 532 nm).
 *                           CH1B_MEDIAN
 *                           CH1B_SD
 *                           CH1D_MEAN
 *                           CH2I_MEAN
 *                           CH2D_MEAN
 *                           CH2D_MEDIAN 
 *                           CH2I_MEDIAN
 *                           CH2_PER_SAT -- percentage of saturated pixels in channel 2. Blank?
 *                           CH2I_SD
 *                           CH2B_MEAN
 *                           CH2B_MEDIAN
 *                           CH2B_SD
 *                           CH2BN_MEDIAN
 *                           CH2DN_MEAN -- normalized, background subtracted
 *                           CH2IN_MEAN -- normalized intensity
 *                           CH2DN_MEDIAN -- normalized, bg sub
 *                           CH2IN_MEDIAN -- normalized
 *                           CORR
 *                           DIAMETER -- blank?
 *                           FLAG
 *                           LOG_RAT2N_MEAN -- normalized log base 2 ratio of the mean pixel intensities
 *                           LOG_RAT2N_MEDIAN -- normalized log bas 2 ratio of the median
 *                           PIX_RAT2_MEAN
 *                           PIX_RAT2_MEDIAN
 *                           PERGTBCH1I_1SD
 *                           PERGTBCH1I_2SD
 *                           PERGTBCH2I_1SD
 *                           PERGTBCH2I_2SD
 *                           RAT1_MEAN
 *                           RAT1N_MEAN
 *                           RAT2_MEAN
 *                           RAT2_MEDIAN
 *                           RAT2_SD
 *                           RAT2N_MEAN
 *                           RAT2N_MEDIAN
 *                           REGR
 *                           SUM_MEAN
 *                           SUM_MEDIAN
 *                           TOT_BPIX
 *                           TOT_SPIX
 *                           X_COORD
 *                           Y_COORD
 *                           TOP
 *                           BOT
 *                           LEFT
 *                           RIGHT
 *                           SECTOR
 *                           SECTORROW
 *                           SECTORCOL
 *                           SOURCE
 *                           PLATE
 *                           PROW
 *                           PCOL
 *                           FAILED
 *                           IS_VERIFIED
 *                           IS_CONTAMINATED
 *                           LUID
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * <p>
 * The smd code reveals the following that shows that most columns are not of interest to SMD - note that log ratios are
 * not stored. So they are presumbaly recomputing all of that later.
 * </p>
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *              my %kScanalyze2SMDMapping = (&quot;SPOT&quot;           =&gt; &quot;SPOT&quot;,
 *              &quot;CH1I&quot;           =&gt; &quot;CH1I_MEAN&quot;, 
 *              &quot;CH1BA&quot;          =&gt; &quot;CH1B_MEAN&quot;, 
 *              &quot;CH1B&quot;           =&gt; &quot;CH1B_MEDIAN&quot;, 
 *              &quot;CH1D_MEAN&quot;      =&gt; &quot;CH1D_MEAN&quot;, # Calculated from CH1I_MEAN - CH1B_MEDIAN
 *              &quot;CH2I&quot;           =&gt; &quot;CH2I_MEAN&quot;,
 *              &quot;CH2BA&quot;          =&gt; &quot;CH2B_MEAN&quot;, 
 *              &quot;CH2B&quot;           =&gt; &quot;CH2B_MEDIAN&quot;, 
 *              &quot;CH2D_MEAN&quot;      =&gt; &quot;CH2D_MEAN&quot;, # Calculated from CH2I_MEAN - CH2B_MEDIAN
 *              &quot;CORR&quot;           =&gt; &quot;CORR&quot;, 
 *              &quot;FLAG&quot;           =&gt; &quot;FLAG&quot;,      # Is converted to Genepix style &gt; 0 -&gt; -100 
 *              &quot;MRAT&quot;           =&gt; &quot;PIX_RAT2_MEDIAN&quot;, 
 *              &quot;PERGTBCH1I_1SD&quot; =&gt; &quot;PERGTBCH1I_1SD&quot;, # CH1GTB2 * 100
 *              &quot;PERGTBCH2I_1SD&quot; =&gt; &quot;PERGTBCH2I_1SD&quot;, # CH2GTB2 * 100
 *              &quot;REGR&quot;           =&gt; &quot;REGR&quot;, 
 *              &quot;BGPIX&quot;          =&gt; &quot;TOT_BPIX&quot;, 
 *              &quot;SPIX&quot;           =&gt; &quot;TOT_SPIX&quot;,
 *              &quot;TOP&quot;            =&gt; &quot;TOP&quot;, 
 *              &quot;BOT&quot;            =&gt; &quot;BOT&quot;, 
 *              &quot;LEFT&quot;           =&gt; &quot;LEFT&quot;, 
 *              &quot;RIGHT&quot;          =&gt; &quot;RIGHT&quot;,
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * <p>
 * For Genepix we have:
 * </p>
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *              Spot
 *              Clone ID
 *              Gene Symbol
 *              Gene Name
 *              Cluster ID
 *              Accession
 *              Preferred name
 *              Locuslink ID
 *              Name
 *              Sequence Type
 *              X Grid Coordinate (within sector)
 *              Y Grid Coordinate (within sector)
 *              Sector
 *              Failed
 *              Plate Number
 *              Plate Row
 *              Plate Column
 *              Clone Source
 *              Is Verified
 *              Is Contaminated
 *              Luid
 *              Sum of Ch2 Foreground Pixel Intensities
 *              Sum of Ch1 Foreground Pixel Intensities
 *              Ch2 Signal-to-Noise Ratio
 *              Ch1 Signal-to-Noise Ratio
 *              Ch1 Background Intensity Coefficient of Variation
 *              Ch2 Background Intensity Coefficient of Variation
 *              Ch1 Foreground Intensity Coefficient of Variation
 *              Ch2 Foreground Intensity Coefficient of Variation
 *              Actual Ch1 Background Used in Calculations
 *              Actual Ch2 Background Used in Calculations
 *              Autoflag
 *              Spot Circularity
 *              SUID
 *              Ch1 Intensity (Mean)
 *              Ch1 Net (Median)
 *              Ch1 Intensity (Median)
 *              % of saturated Ch1 pixels
 *              Std Dev of Ch1 Intensity
 *              Channel 1 Background (Mean)
 *              Ch1 Background (Median)
 *              Std Dev of Ch1 Background
 *              Ch1 Net (Mean)
 *              Ch2 Intensity (Mean)
 *              Ch2 Net (Mean)
 *              Ch2 Net (Median)
 *              Ch2 Intensity (Median)
 *              % of saturated Ch2 pixels
 *              Std Dev of Ch2 Intensity
 *              Channel 2 Background (Mean)
 *              Ch2 Background (Median)
 *              Std Dev of Ch2 Background
 *              Ch2 Normalized Background (Median)
 *              Ch2 Normalized Net (Mean)
 *              Ch2 Normalized Intensity (Mean)
 *              Normalized Ch2 Net (Median)
 *              Normalized Ch2 Intensity (Median)
 *              Regression Correlation
 *              Diameter of the spot
 *              Spot Flag
 *              Log(base2) of R/G Normalized Ratio (Mean)
 *              Log(base2) of R/G Normalized Ratio (Median)
 *              R/G Mean (per pixel)
 *              R/G Median (per pixel)
 *              % CH1 PIXELS &gt; BG + 1SD
 *              % CH1 PIXELS &gt; BG + 2SD
 *              % CH2 PIXELS &gt; BG + 1SD
 *              % CH2 PIXELS &gt; BG + 2SD
 *              G/R (Mean)
 *              G/R Normalized (Mean)
 *              R/G (Mean)
 *              R/G (Median)
 *              Std Dev of pixel intensity ratios
 *              R/G Normalized (Mean)
 *              R/G Normalized (Median)
 *              Regression Ratio
 *              Sum of mean intensities
 *              Sum of median intensities
 *              Number of Background Pixels
 *              Number of Spot Pixels
 *              X coordinate (whole array, in microns)
 *              Y coordinate (whole array, in microns)
 *              Box Top
 *              Box Bottom
 *              Box Left
 *              Box Right
 *              Channel 1 Mean Intensity / Median Background Intensity
 *              Channel 2 Normalized (Mean Intensity / Median Background Intensity)
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * 
 * <h2>Data vector rows</h2>
 * <p>
 * These are relatively simple because they just follow the column headings. Missing values can occur.
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class DataFileParser {
    private SMDPerson contact;
    private SMDBioAssay bioAssay;
    private Set<SMDQuantitationType> qTypes;

    public DataFileParser() {
        contact = new SMDPerson();
        bioAssay = new SMDBioAssay();
        qTypes = new HashSet<SMDQuantitationType>();
    }

    public DataFileParser( SMDBioAssay exp ) {
        contact = new SMDPerson();
        bioAssay = exp;
        qTypes = new HashSet<SMDQuantitationType>();
    }

    /**
     * @throws IOException
     * @throws SAXException
     * @param fileName
     * @throws IOException
     */
    public void read( String fileName ) throws IOException {
        File infile = new File( fileName );
        if ( !infile.exists() || !infile.canRead() ) {
            throw new IOException( "Could not read from file " + fileName );
        }
        InputStream stream;

        stream = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );

        this.read( stream );
        stream.close();
    }

    /**
     * @throws SAXException
     * @param inputStream
     * @throws IOException
     */
    public void read( InputStream stream ) throws IOException {
        BufferedReader bis = new BufferedReader( new InputStreamReader( stream ) );
        String line;
        boolean pastHeader = false;
        String[] headings;
        String[] data;
        while ( ( line = bis.readLine() ) != null ) {
            if ( line.startsWith( "!" ) ) {
                String[] vals = SmdUtil.smdSplit( line );
                String key = vals[0];

                String value = "";
                if ( vals.length > 1 ) {
                    value = vals[1];
                }

                if ( key.equals( "Exptid" ) ) {
                    if ( bioAssay.getId() > 0 ) {
                        if ( Integer.parseInt( value ) != bioAssay.getId() ) {
                            throw new IllegalStateException( "SMDExperiment ids did not match" );
                        }
                    } else {
                        bioAssay.setId( Integer.parseInt( value ) );
                    }
                } else if ( key.equals( "Experiment Name" ) ) {
                    if ( bioAssay.getName() != null ) {
                        if ( !bioAssay.getName().equals( value ) ) {
                            throw new IllegalStateException( "SMDExperiment name did not match" );
                        }
                    } else {
                        bioAssay.setName( value );
                    }
                } else if ( key.equals( "Organism" ) ) {
                    if ( bioAssay.getOrganism() != null ) {
                        if ( !bioAssay.getOrganism().equals( value ) ) {
                            throw new IllegalStateException( "SMDExperiment species did not match" );
                        }
                    } else {
                        bioAssay.setOrganism( value );
                    }
                } else if ( key.equals( "Category" ) ) {
                    bioAssay.setCategory( value );
                } else if ( key.equals( "Subcategory" ) ) {
                    bioAssay.setSubCategory( value );
                } else if ( key.equals( "Experimenter" ) ) {
                    contact.setName( value );
                } else if ( key.equals( "Contact email" ) ) {
                    contact.setEmail( value );
                } else if ( key.equals( "Contact Address1" ) ) {
                    contact.setAddress1( value );
                } else if ( key.equals( "Contact Address2" ) ) {
                    contact.setAddress2( value );
                } else if ( key.equals( "Contact Address3" ) ) {
                    contact.setAddress3( value );
                } else if ( key.equals( "Contact Address4" ) ) {
                    contact.setAddress4( value );
                } else if ( key.equals( "Country" ) ) {
                    contact.setCountry( value );
                } else if ( key.equals( "State" ) ) {
                    contact.setState( value );
                } else if ( key.equals( "Postal Code" ) ) {
                    contact.setPostalCode( value );
                } else if ( key.equals( "SlideName" ) ) {
                    bioAssay.setSlideName( value );
                } else if ( key.equals( "Printname" ) ) {
                    bioAssay.setPrintName( value );
                } else if ( key.equals( "Tip Configuration" ) ) {
                    bioAssay.setTipConfiguration( value );
                } else if ( key.equals( "Columns per Sector" ) ) {
                    bioAssay.setColumnsPerSector( Integer.parseInt( value ) );
                } else if ( key.equals( "Rows per Sector" ) ) {
                    bioAssay.setRowsPerSector( Integer.parseInt( value ) );
                } else if ( key.equals( "Column spacing" ) ) {
                    bioAssay.setColumnSpacing( Integer.parseInt( value ) );
                } else if ( key.equals( "Row spacing" ) ) {
                    bioAssay.setRowSpacing( Integer.parseInt( value ) );
                } else if ( key.equals( "Channel 1 Description" ) ) {
                    bioAssay.setChannel1Description( value );
                } else if ( key.equals( "Channel 2 Description" ) ) {
                    bioAssay.setChannel2Description( value );
                } else if ( key.equals( "Scanning Software" ) ) {
                    bioAssay.setScanningSoftware( value );
                } else if ( key.equals( "Software version" ) ) {
                    bioAssay.setSoftwareVersion( value );
                } else {
                    throw new IllegalStateException( "Invalid key '" + key + "' found in data file" );
                }

            } else if ( !pastHeader ) {
                headings = line.split( "\t" );
                for ( int i = 0; i < headings.length; i++ ) {
                    SMDQuantitationType q = new SMDQuantitationType( headings[i] );
                    qTypes.add( q );
                }

                pastHeader = true;
            } else {
                data = line.split( "\t" );
                for ( int i = 0; i < data.length; i++ ) {
                    // TODO
                }
            }
        }

        bis.close();

    }

}