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
package ubic.gemma.web.taglib.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @jsp.tag name="assayView" body-content="empty"
 * @author joseph
 * @version $Id $
 */
public class AssayViewTag extends TagSupport {
    /**
     * 
     */
    private static final long serialVersionUID = 8754490187937841260L;

    private Log log = LogFactory.getLog( this.getClass() );
    
    private ExpressionExperiment expressionExperiment;
    
    /**
     * @param expressionExperiment
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExpressionExperiment (ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @SuppressWarnings("unchecked")
    @Override
    public int doStartTag() throws JspException {
        log.debug( "in Start Tag" );
        StringBuilder buf = new StringBuilder();

        buf.append( "<div>" );

        // create table
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Map<BioMaterial,Map<ArrayDesign,Collection<BioAssay>>> bioAssayMap = new HashMap<BioMaterial,Map<ArrayDesign,Collection<BioAssay>>>();
        Set<ArrayDesign> designs = new HashSet<ArrayDesign>();
        for ( BioAssay assay : bioAssays ) {
            // map for bioassays linked to a specific arraydesign
            // map for the bioassays linked to a specific biomaterial
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            ArrayDesign design = assay.getArrayDesignUsed();
            designs.add(design);
            for ( BioMaterial material : materials ) {
                // check if the assay list is initialized yet
                Map<ArrayDesign,Collection<BioAssay>> assayMap;
                if (bioAssayMap.containsKey( material )) {
                    assayMap = bioAssayMap.get( material );
                }
                else {
                    assayMap = new HashMap<ArrayDesign,Collection<BioAssay>>();
                    bioAssayMap.put( material, assayMap );
                }
                
                if (assayMap.containsKey( design )) {
                    assayMap.get( design ).add( assay );
                }
                else {
                    Collection<BioAssay> assayList = new ArrayList<BioAssay>();
                    assayList.add( assay );
                    assayMap.put( design, assayList);
                }
            }
        } 
        buf.append( "<table><tr>" );
        buf.append( "<td>BioMaterial</td>" );
        // display arraydesigns
        for ( ArrayDesign design : designs  ) {
            buf.append( "<td>" + design.getShortName() + "</td>" );
        }
        buf.append( "</tr>" );
        
        // display bioMaterials and the corresponding bioAssays
        int count = 1;
        
        Iterator iter = bioAssayMap.keySet().iterator();
        ArrayList<BioMaterial> materials = new ArrayList<BioMaterial>();
        while (iter.hasNext()) {
            materials.add( (BioMaterial) iter.next() );
        }
        Comparator comparator = (Comparator) new BioMaterialComparator();
        Collections.sort( materials, comparator );
        for ( BioMaterial material : materials ) {
            if (count % 2 == 0) {
                buf.append("<tr class='even' align=center>");              
            }
            else {
                buf.append("<tr class='odd' align=center>");          
            }
            buf.append("<td>" + material.getName() + "</td>");
            
            Map<ArrayDesign,Collection<BioAssay>> assayMap = bioAssayMap.get( material ); 
            
            for ( ArrayDesign design : designs ) {
                if (assayMap.containsKey( design )) {
                    Collection<BioAssay> assays = assayMap.get( design );
                    Collection<Long> ids = new ArrayList<Long>();
                    for ( BioAssay assay : assays ) {
                        ids.add( assay.getId() );
                    }
                    String link = "<a href='/Gemma/bioAssay/showAllBioAssays.html?id=" + 
                        StringUtils.join(ids.toArray()) + "'> (list) </a>";
                    buf.append( "<td>" +  assayMap.get( design ).size() + link + "</td>" );
                }
            }          
            buf.append( "</tr>" );
            count++;
        }
        buf.append("</table>");
        buf.append( "</div>" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "assayViewTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
    
    private class BioMaterialComparator implements Comparator {

        public int compare( Object arg0, Object arg1 ) {
            BioMaterial obj0 = (BioMaterial) arg0;
            BioMaterial obj1 = (BioMaterial) arg1;
            
            return obj0.getName().compareTo( obj1.getName() );
        }        
    }
}
