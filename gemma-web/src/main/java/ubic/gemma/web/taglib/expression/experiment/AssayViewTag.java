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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.*;

/**
 * Used to display table of biomaterials and bioassays. In edit mode this displays allows dragging bioassays around to
 * match up across platforms.
 *
 * @author joseph
 */
@CommonsLog
public class AssayViewTag extends TagSupport {
    private static final long serialVersionUID = 8754490187937841260L;
    /**
     * How many 'extra' biomaterials to add to the editing table, so the user can assing bioassays to new biomaterials.
     */
    private static final int NUM_EXTRA_BIOMATERIALS = 12;
    private boolean edit = false;
    private Collection<BioAssayValueObject> bioAssays;

    /**
     * Jackson serializer to map objects to JSON.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    @Override
    public int doStartTag() throws JspException {
        String contextPath = pageContext.getServletContext().getContextPath();
        StringBuilder buf = new StringBuilder();

        buf.append( "<div>" );

        // create table
        Map<BioMaterialValueObject, Map<ArrayDesignValueObject, Collection<BioAssayValueObject>>> bioAssayMap = new HashMap<>();
        Set<ArrayDesignValueObject> designs = new HashSet<>();
        Map<ArrayDesignValueObject, Long> arrayMaterialCount = new HashMap<>();

        // package all of this information into JSON for javascript dynamic retrieval
        Map<String, String> assayToMaterial = new HashMap<>();
        for ( BioAssayValueObject assay : bioAssays ) {
            // map for bioassays linked to a specific arraydesign
            // map for the bioassays linked to a specific biomaterial
            BioMaterialValueObject material = assay.getSample();
            ArrayDesignValueObject design = assay.getArrayDesign();
            designs.add( design );

            // check if the assay list is initialized yet
            Map<ArrayDesignValueObject, Collection<BioAssayValueObject>> assayMap;
            if ( bioAssayMap.containsKey( material ) ) {
                assayMap = bioAssayMap.get( material );
            } else {
                assayMap = new HashMap<>();
                bioAssayMap.put( material, assayMap );
            }

            if ( assayMap.containsKey( design ) ) {
                assayMap.get( design ).add( assay );
            } else {
                Collection<BioAssayValueObject> assayList = new ArrayList<>();
                assayList.add( assay );
                assayMap.put( design, assayList );
            }

            if ( arrayMaterialCount.containsKey( design ) ) {
                Long count = arrayMaterialCount.get( design );
                count++;
                arrayMaterialCount.put( design, count );
            } else {
                Long count = new Long( 1 );
                arrayMaterialCount.put( design, count );
            }

        }
        int materialCount = bioAssayMap.keySet().size();
        buf.append( "<table class='detail row-separated odd-gray'><tr>" );
        buf.append( "<th>" + materialCount + " BioMaterials</th>" );
        // display arraydesigns
        for ( ArrayDesignValueObject design : designs ) {
            Long count = arrayMaterialCount.get( design );
            buf.append( "<th>" + count
                    + " BioAssays on<br /><a target='_blank' href=\"" + contextPath + "/arrays/showArrayDesign.html?id=" + design
                    .getId() + "\" title=\"" + design.getName() + "\" >" + ( design.getShortName() == null ?
                    design.getName() :
                    design.getShortName() ) + "</a></th>" );
        }
        buf.append( "</tr>" );

        // display bioMaterials and the corresponding bioAssays
        int count = 1;

        Iterator<BioMaterialValueObject> iter = bioAssayMap.keySet().iterator();
        List<BioMaterialValueObject> materials = new ArrayList<>();
        while ( iter.hasNext() ) {
            materials.add( iter.next() );
        }
        Comparator<BioMaterialValueObject> comparator = new BioMaterialComparator();
        Collections.sort( materials, comparator );
        int elementCount = 1;
        int emptyCount = 0;
        for ( BioMaterialValueObject material : materials ) {
            if ( count % 2 == 0 ) {
                buf.append( "<tr class='even' align=justify>" );
            } else {
                buf.append( "<tr class='odd' align=justify>" );
            }

            String bmLink = "<a href='" + contextPath + "/bioMaterial/showBioMaterial.html?id=" + material.getId() + "'> " + material
                    .getName() + "</a>";
            buf.append( "<td>" + bmLink + "</td>" );

            Map<ArrayDesignValueObject, Collection<BioAssayValueObject>> assayMap = bioAssayMap.get( material );

            String image = "&nbsp;&nbsp;&nbsp;<img height=16 width=16 src='" + contextPath + "/images/icons/arrow_switch.png' />&nbsp;&nbsp;&nbsp;";
            for ( ArrayDesignValueObject design : designs ) {
                if ( assayMap.containsKey( design ) ) {
                    Collection<BioAssayValueObject> assays = assayMap.get( design );
                    Collection<Long> ids = new ArrayList<>();
                    Collection<String> tooltips = new ArrayList<>();
                    for ( BioAssayValueObject assay : assays ) {
                        ids.add( assay.getId() );
                        tooltips.add( StringUtils.abbreviate( assay.getName() + assay.getDescription(), 120 ) );
                        this.addMaterial( assayToMaterial, assay.getId(), material.getId() );
                    }

                    if ( assayMap.get( design ).size() > 1 ) {
                        String link = "<a title='" + StringUtils.join( tooltips.toArray(), "\n" )
                                + "' href='" + contextPath + "/bioAssay/showAllBioAssays.html?id=" + StringUtils
                                .join( ids.toArray(), "," ) + "'> (list) </a>";
                        buf.append(
                                "<td>" + assayMap.get( design ).size() + link + "&nbsp;" + elementCount + "</td>\n" );

                    } else {

                        /*
                         * Each bioassay has a unique id; the div it sits in is identified by the class 'dragitem'. See
                         * expressionExperiment.edit.jsp.
                         */

                        BioAssayValueObject assay = ( ( List<BioAssayValueObject> ) assayMap.get( design ) ).get( 0 );
                        String shortDesc = StringUtils.abbreviate( assay.getDescription(), 60 );
                        String link = "<a target=\"_blank\" title='" + shortDesc
                                + "' href='" + contextPath + "/bioAssay/showBioAssay.html?id=" + assay.getId() + "'>" + assay
                                .getName() + "</a>";
                        String editAttributes =
                                " align='left' class='dragItem' id='bioassay." + assay.getId() + "' material='"
                                        + material.getId() + "' assay='" + assay.getId() + "' arrayDesign='" + design
                                        .getId() + "'";
                        if ( edit && designs.size() > 1 ) {
                            buf.append( "\n<td><span " + editAttributes + ">" + image + link );
                        } else {
                            buf.append( "\n<td ><span>" + link + "&nbsp;" );
                        }
                        buf.append( "</span></td>\n" );
                    }

                } else {
                    emptyCount = addEmpty( buf, assayToMaterial, emptyCount, material, image, design );
                }
            }

            buf.append( "</tr>" );
            count++;
            elementCount++;
        }

        // add a few blanks, but only if we are editing.
        if ( edit ) {
            addNovelBiomaterialSlots( buf, designs, assayToMaterial, count, emptyCount );
        }

        buf.append( "</table>" );

        if ( edit ) {
            // append JSON serialization
            try {
                String jsonSerialization = objectMapper.writeValueAsString( assayToMaterial );
                buf.append( "<input type='hidden' id='assayToMaterialMap' name='assayToMaterialMap' value='"
                        + StringEscapeUtils.escapeHtml4( jsonSerialization ) + "'/>" );
            } catch ( JsonProcessingException e ) {
                log.error( "Failed to serialize assayToMaterial to JSON.", e );
            }

        }

        buf.append( "</div>" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "assayViewTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    public void setEdit( String edit ) {
        this.edit = edit.equalsIgnoreCase( "true" );
    }

    public void setBioAssays( Collection<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    /**
     * Add a 'unused' biomaterial/bioassay combination to the table.
     *
     * @return revised count of number of empty items.
     */
    private int addEmpty( StringBuilder buf, Map<String, String> assayToMaterial, int emptyCount,
            BioMaterialValueObject material, String image, ArrayDesignValueObject design ) {
        // put empty space in table if the bioMaterial does not
        // use this array design
        emptyCount++;
        String editAttributes = "class='dragItem' id='bioassay.empty." + emptyCount + "' material='" + material.getId()
                + "' assay='nullElement' arrayDesign='" + design.getId() + "'";

        if ( edit ) {
            buf.append( "\n<td><span " + editAttributes + ">" + image );
        } else {
            buf.append( "\n<td><span>&nbsp;" );
        }
        this.addMaterial( assayToMaterial, null, material.getId() );
        buf.append( "</span></td>\n" );
        return emptyCount;
    }

    private void addMaterial( Map<String, String> assayToMaterial, Long bioAssayId, Long bioMaterialId ) {
        String bioAssayStr = "";
        if ( bioAssayId == null ) {
            bioAssayStr = "nullElement";
        } else {
            bioAssayStr = bioAssayId.toString();
        }

        assayToMaterial.put( bioAssayStr, bioMaterialId.toString() );

    }

    /**
     * Add places for completely new biomaterials to be added. These are the row labels.
     */
    private void addNovelBiomaterialSlots( StringBuilder buf, Set<ArrayDesignValueObject> designs,
            Map<String, String> assayToMaterial, int count, int emptyCount ) {
        if ( designs.size() == 1 ) {
            return;
        }
        String contextPath = pageContext.getServletContext().getContextPath();
        for ( int i = 1; i <= NUM_EXTRA_BIOMATERIALS; i++ ) {

            if ( count % 2 == 0 ) {
                buf.append( "<tr class='even' align=justify>" );
            } else {
                buf.append( "<tr class='odd' align=justify>" );
            }

            // FIXME this is a kludge: use negative ids to distinguish the new biomaterials.
            BioMaterialValueObject material = new BioMaterialValueObject( 0L - i );

            material.setName( "[New biomaterial " + i + "]" );
            buf.append( "<td>" + material.getName() + "</td>" );
            String image = "<img height=10 width=20 src='" + contextPath + "/images/arrow_out.png' />";
            for ( ArrayDesignValueObject design : designs ) {
                emptyCount = addEmpty( buf, assayToMaterial, emptyCount, material, image, design );
            }
            buf.append( "</tr>" );
            count++;
        }
    }

    /**
     * @author pavlidis
     */
    static class BioMaterialComparator implements Comparator<BioMaterialValueObject> {

        @Override
        public int compare( BioMaterialValueObject arg0, BioMaterialValueObject arg1 ) {

            return arg0.getName().compareTo( arg1.getName() );
        }
    }
}
