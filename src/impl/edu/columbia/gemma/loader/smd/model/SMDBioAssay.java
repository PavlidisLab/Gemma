package edu.columbia.gemma.loader.smd.model;

import edu.columbia.gemma.common.description.Description;
import edu.columbia.gemma.common.description.DescriptionImpl;
import edu.columbia.gemma.common.description.FileFormat;
import edu.columbia.gemma.common.description.FileFormatImpl;
import edu.columbia.gemma.common.protocol.Protocol;
import edu.columbia.gemma.common.protocol.ProtocolImpl;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssay.BioAssayImpl;

/**
 * Just to be clear, this represents a single microarray. SMD calls that an "experiment", MAGE calls it a BioAssay.
 * <p>
 * Besides the information in the meta-data files from the ftp site, this object can also hold information from the data
 * file.
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
 *         
 *          
 *           
 *            
 *             
 *              
 *               
 *                
 *                 
 *                  
 *                   !SlideName=dtp2780
 *                   !Printname=10k_Print3
 *                   !Tip Configuration=Standard 4-tip
 *                   !Columns per Sector=50
 *                   !Rows per Sector=50
 *                   !Column Spacing=137                                                                                                                                                                                                                 
 *                   !Row Spacing=137                                                                                                                                                                                                                 
 *                   !Channel 1 Description=Reference_Pool                                                                                                                                                                                                                 
 *                   !Channel 2 Description=ADR-RES_CL5002_UNKNOWN                                                                                                                                                                                                                  
 *                   !Scanning Software=ScanAlyze                                                                                                                                                                                                                 
 *                   !Software version=2.44                                                                                                                                                                                                                 
 *                   !Scanning parameters=   
 *                   
 *                  
 *                 
 *                
 *               
 *              
 *             
 *            
 *           
 *          
 *         
 *        
 *       
 *      
 *     
 *    
 *   
 *  
 * </pre>
 * 
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SMDBioAssay {

   private int id = -1;
   private String name;
   private String category;
   private String subCategory;
   private String slideName;
   private String printName;
   private String tipConfiguration;
   private int columnsPerSector;
   private int rowsPerSector;
   private int columnSpacing;
   private String organism;
   private int rowSpacing;
   private String channel1Description;
   private String channel2Description;
   private String scanningSoftware;
   private String softwareVersion;
   private String scanningParameters;

   public FileFormat toFileFormat() {
      FileFormat f = new FileFormatImpl();

      f.setName( scanningSoftware + " version " + softwareVersion );
      f.setIdentifier( "SMD:BioAssay:FileFormat" + id );
      return f;
   }

   /**
    * @return
    */
   public Protocol toProtocol() {
      Protocol p = new ProtocolImpl();
      p.setIdentifier( "SMD:BioAssay:Protocol" + id );
      p.setText( scanningSoftware + " version " + softwareVersion + ", Scanning parameters:" + scanningParameters );
      return p;
   }

   /**
    * @return
    */
   public Description toDescription() {
      Description d = new DescriptionImpl();

      d.setIdentifier( "SMD:BioAssay:Description:" + id );
      d.setText( "Category: " + category + ", SubCategory: " + ", Channel 1:" + channel1Description + ", Channel 2"
            + channel2Description + ", Slide:" + slideName + ", Print: " + printName );
      return d;
   }

   /**
    * @param d
    * @return
    */
   public BioAssay toBioAssay( Description d ) {
      BioAssay result = new BioAssayImpl();

      result.setIdentifier( "SMD:BioAssay:" + id );
      result.setName( name );
      result.setDescription( d );
      return result;
   }

   public int getId() {
      return id;
   }

   public void setId( int id ) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName( String name ) {
      this.name = name;
   }

   public String toString() {
      return id + "\t" + name + "\t";
   }

   public String getCategory() {
      return category;
   }

   public void setCategory( String category ) {
      this.category = category;
   }

   public String getChannel1Description() {
      return channel1Description;
   }

   public void setChannel1Description( String channel1Description ) {
      this.channel1Description = channel1Description;
   }

   public String getChannel2Description() {
      return channel2Description;
   }

   public void setChannel2Description( String channel2Description ) {
      this.channel2Description = channel2Description;
   }

   public int getColumnsPerSector() {
      return columnsPerSector;
   }

   public void setColumnsPerSector( int columnsPerSector ) {
      this.columnsPerSector = columnsPerSector;
   }

   public String getPrintName() {
      return printName;
   }

   public void setPrintName( String printName ) {
      this.printName = printName;
   }

   public int getColumnSpacing() {
      return columnSpacing;
   }

   public void setColumnSpacing( int columnSpacing ) {
      this.columnSpacing = columnSpacing;
   }

   public String getOrganism() {
      return organism;
   }

   public void setOrganism( String organism ) {
      this.organism = organism;
   }

   public int getRowSpacing() {
      return rowSpacing;
   }

   public void setRowSpacing( int rowSpacing ) {
      this.rowSpacing = rowSpacing;
   }

   public int getRowsPerSector() {
      return rowsPerSector;
   }

   public void setRowsPerSector( int rowsPerSector ) {
      this.rowsPerSector = rowsPerSector;
   }

   public String getScanningParameters() {
      return scanningParameters;
   }

   public void setScanningParameters( String scanningParameters ) {
      this.scanningParameters = scanningParameters;
   }

   public String getScanningSoftware() {
      return scanningSoftware;
   }

   public void setScanningSoftware( String scanningSoftware ) {
      this.scanningSoftware = scanningSoftware;
   }

   public String getSlideName() {
      return slideName;
   }

   public void setSlideName( String slideName ) {
      this.slideName = slideName;
   }

   public String getSoftwareVersion() {
      return softwareVersion;
   }

   public void setSoftwareVersion( String softwareVersion ) {
      this.softwareVersion = softwareVersion;
   }

   public String getSubCategory() {
      return subCategory;
   }

   public void setSubCategory( String subCategory ) {
      this.subCategory = subCategory;
   }

   public String getTipConfiguration() {
      return tipConfiguration;
   }

   public void setTipConfiguration( String tipConfiguration ) {
      this.tipConfiguration = tipConfiguration;
   }
}

