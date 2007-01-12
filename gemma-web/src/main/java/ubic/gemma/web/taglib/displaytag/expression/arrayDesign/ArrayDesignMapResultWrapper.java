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
package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 */
public class ArrayDesignMapResultWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    public String getGeneList() {
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        Collection gVos= object.getGenes().values();
        Collection gpVos = object.getGeneProducts().values();
        
        // get unique ID - compositeSequenceId
        String compositeSequenceId = "cs" + object.getCompositeSequenceId();
        
/*        <span name="datasetList" onclick="return toggleVisibility('datasetList')">
        <img src="/Gemma/images/chart_organisation_add.png" />
    </span>
    <span name="datasetList" style="display:none" onclick="return toggleVisibility('datasetList')">
        <img src="/Gemma/images/chart_organisation_delete.png" />
    </span>*/
        
        // associate genes with geneProducts
        HashMap<Long,Collection> geneProducts = new HashMap<Long,Collection>();
        
        for ( Object o2 : gpVos ) {

            GeneProductValueObject gpVo = (GeneProductValueObject) o2;
            
            if (!geneProducts.containsKey( gpVo.getGeneId() )) {
                Collection geneProductVos = new ArrayList<GeneProductValueObject>();
                geneProductVos.add( gpVo );
                geneProducts.put( gpVo.getGeneId(), geneProductVos );     
            }
            else {
                Collection geneProductVos = geneProducts.get( gpVo.getGeneId() );
                geneProductVos.add( gpVo);
            }
        }
        
        StringBuffer retVal = new StringBuffer();
        
        for ( Object o2 : gVos ) {
            GeneValueObject gVo = (GeneValueObject) o2;
            
            String fullName = gVo.getOfficialSymbol();
            String shortName = StringUtils.abbreviate( fullName, 20 );
            
            if (gVo.getNcbiId() != null) {
                retVal.append( "<span title='"+ fullName+"'>" + shortName + "</span><a target='_blank' href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=" + gVo.getNcbiId() + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a>" +
                        "<a target='_blank' href='/Gemma/gene/showGene.html?id=" + gVo.getId().toString() + "'><img height=10 width=10 src='/Gemma/images/logo/gemmaTiny.gif'></a><BR>");
            }
            else {
                retVal.append( "<span title='"+ fullName+"'>" + shortName + "</span><BR>");
            }

            // add expansion code - RNA
            
            retVal.append( "<span name=\"" + compositeSequenceId + fullName + "RNA\" onclick=\"return toggleVisibility('" + compositeSequenceId+ fullName + "RNA')\">" + 
                "<img src=\"/Gemma/images/chart_organisation_add.png\" />" + 
                "</span>" + 
                "<span name=\"" + compositeSequenceId + fullName + "RNA\" style=\"display:none\" onclick=\"return toggleVisibility('" + compositeSequenceId + fullName + "RNA')\">" + 
                "<img src=\"/Gemma/images/chart_organisation_delete.png\" />" + 
                "</span>");

            Collection geneProductVos = geneProducts.get( gVo.getId() );
            int rnaCount = 0;
            StringBuffer rnaStrings = new StringBuffer();
            rnaStrings.append( "<span style='display:none' name=\""+ compositeSequenceId + fullName + "RNA\">" );
            for ( Object gpVo : geneProductVos ) {
                if (((GeneProductValueObject)gpVo).getType().equalsIgnoreCase( "RNA" )) {
                    String gpStr = generateGeneProductLink( (GeneProductValueObject) gpVo );
                    rnaStrings.append( (String)gpStr );
                    rnaCount++;
                }
            }
            retVal.append( "RNA (" + rnaCount+ ")<BR>" );
            retVal.append( rnaStrings );
            retVal.append( "</span>" );
            
            
            // add expansion code - Protein
            retVal.append( "<span name=\"" + compositeSequenceId + fullName + "Protein\" onclick=\"return toggleVisibility('" + compositeSequenceId+ fullName + "Protein')\">" + 
                    "<img src=\"/Gemma/images/chart_organisation_add.png\" />" + 
                    "</span>" + 
                    "<span name=\"" + compositeSequenceId + fullName + "Protein\" style=\"display:none\" onclick=\"return toggleVisibility('" + compositeSequenceId + fullName + "Protein')\">" + 
                    "<img src=\"/Gemma/images/chart_organisation_delete.png\" />" + 
                    "</span>");
            
            // append the list of geneProducts
            // grouped by gene and geneProductType



            int proteinCount = 0;
            StringBuffer proteinStrings = new StringBuffer();
            proteinStrings.append( "<span style='display:none' name=\""+ compositeSequenceId + fullName + "Protein\">" );
            for ( Object gpVo : geneProductVos ) {
                if (((GeneProductValueObject)gpVo).getType().equalsIgnoreCase( "Protein" )) {
                    String gpStr = generateGeneProductLink( (GeneProductValueObject) gpVo );
                    proteinStrings.append( (String)gpStr );
                    proteinCount++;
                }
            }
            retVal.append( "Protein (" + proteinCount+ ")<BR>" );
            retVal.append( proteinStrings );
            retVal.append( "</span>" );


        }
        
        return retVal.toString();
        
    }

    private String generateGeneProductLink( GeneProductValueObject gpVo ) {
        StringBuffer gpStr = new StringBuffer();
        String fullName = gpVo.getName();
        String shortName = StringUtils.abbreviate( fullName, 20 );
        
        String ncbiLink = "";
        if (gpVo.getType().equalsIgnoreCase( "RNA" ) ) {
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
        }
        else {
            // assume protein
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";
            
        }
        if (gpVo.getNcbiId() != null) {
            gpStr.append( "&nbsp;&nbsp;<span title='"+ fullName+"'>" + shortName + "</span><a target='_blank' href='" +  ncbiLink + gpVo.getNcbiId() + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a><BR>");
        }
        else {
            gpStr.append( "&nbsp;&nbsp;<span title='"+ fullName+"'>" + shortName + "</span><BR>");
        }
        return gpStr.toString();
    }
    
    public String getGeneProductList() {
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        Collection gpVos= object.getGeneProducts().values();
        StringBuffer retVal = new StringBuffer();
        for ( Object o2 : gpVos ) {
            GeneProductValueObject gpVo = (GeneProductValueObject) o2;
            String fullName = gpVo.getName();
            String shortName = StringUtils.abbreviate( fullName, 20 );
            
            String ncbiLink = "";
            if (gpVo.getType().equalsIgnoreCase( "RNA" ) ) {
                ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
            }
            else {
                // assume protein
                ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";
                
            }
            if (gpVo.getNcbiId() != null) {
                retVal.append( "<span title='"+ fullName+"'>" + shortName + "</span><a target='_blank' href='" +  ncbiLink + gpVo.getNcbiId() + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a><BR>");
            }
            else {
                retVal.append( "<span title='"+ fullName+"'>" + shortName + "</span><BR>");
            }
        }
        
        return retVal.toString();
        
    }
}
