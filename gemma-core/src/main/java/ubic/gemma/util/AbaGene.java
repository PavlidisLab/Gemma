/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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


package ubic.gemma.util;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author kelsey
 *
 * Represents a gene in the  alan brain atals
 */
public class AbaGene {

    
     Integer geneId;
     String geneSymbol;
     String geneName;
     Integer entrezGeneId;
     String ncbiAccession;
     Collection<ImageSeries> imageSeries;
    
    public AbaGene(
                    Integer geneId, 
                    String geneSymbol, 
                    String geneName,               
                    Integer entrezGeneId,
                    String ncbiAccession, 
                    Collection<ImageSeries> imageSeries) {
                                     
        this.geneId = geneId;
        this.geneSymbol = geneSymbol;
        this.geneName = geneName;
        this.entrezGeneId = entrezGeneId;
        this.ncbiAccession = ncbiAccession;
        this.imageSeries = imageSeries;
    }

    public Integer getGeneId() {
        return geneId;
    }

    public void setGeneId( Integer geneId ) {
        this.geneId = geneId;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol( String geneSymbol ) {
        this.geneSymbol = geneSymbol;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }


    public Integer getEntrezGeneId() {
        return entrezGeneId;
    }

    public void setEntrezGeneId( Integer entrezGeneId ) {
        this.entrezGeneId = entrezGeneId;
    }

    public String getNcbiAccession() {
        return ncbiAccession;
    }

    public void setNcbiAccession( String ncbiAccession ) {
        this.ncbiAccession = ncbiAccession;
    }

    public Collection<ImageSeries> getImageSeries() {
        return imageSeries;
    }

    public void setImageSeries( Collection<ImageSeries> imageSeries ) {
        this.imageSeries = imageSeries;
    }
    
    public void addImageSeries(ImageSeries is){
        if (imageSeries == null)
            imageSeries = new HashSet<ImageSeries>();
        
        imageSeries.add( is );
        
    }
    public String toString(){
      StringBuffer sb = new StringBuffer();
      sb.append( "GeneId: " + this.geneId + "\n");
      sb.append( "GeneSymbol: " + this.geneSymbol + "\n");
      sb.append( "GeneName: " + this.geneName + "\n");
      sb.append( "EntrezId: " + this.entrezGeneId + "\n");
      sb.append( "NCBI Accession: " + this.ncbiAccession + "\n");

      for(ImageSeries is : imageSeries){
          sb.append( "==> ");
          sb.append( "\t image series id: " + is.getImageSeriesId() );
          sb.append(" \t plane: " + is.getPlane());
          sb.append( "\n" );
          
          if (is.getImages() == null)
              continue;
          
          for(Image img : is.getImages()){
              sb.append( "\t ==> ");
              sb.append( "\t \t image id: " + img.getId() );
              sb.append(" \t \t Display Name: " + img.getDisplayName());
              sb.append(" \t \t Expression Image Path Url: " + img.getDownloadExpressionPath());
              sb.append(" \t \t Image path url: " + img.getDownloadImagePath());
              sb.append(" \t \t Expression thumbnail url: " + img.getExpressionThumbnailUrl());
              sb.append(" \t \t thumbnail url: " + img.getThumbnailUrl());
              sb.append(" \t \t zoomified Nissl Url: " + img.getZoomifiedNisslUrl());
              sb.append(" \t \t Posisiton: " + img.getPosition());
              sb.append(" \t \t Reference Atlas Index: " + img.getReferenceAtlasIndex());
              sb.append( "\n" );
          }

      }
      
      return  sb.toString();
    }
    
}
