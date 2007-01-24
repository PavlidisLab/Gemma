<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request"
    class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
  <title> <fmt:message key="compositeSequence.title" /> </title>
  <script type="text/javascript"
	src="<c:url value="/scripts/scrolltable.js"/>"></script>
<link rel="stylesheet" type="text/css" href="<c:url value='/styles/scrolltable.css'/>" />	



  		<aa:zone name="csTable">
  
  <table id="csTableList" class="searchTable">
  <tr>
  <td>
        <h2>
            <fmt:message key="compositeSequence.title" /> Details 
            (click on a composite sequence link to update this area)
            
        </h2>


        
    	<a href="/Gemma/compositeSequence/showCompositeSequence.html?id=<jsp:getProperty name="compositeSequence" property="id" />">(bookmarkable link)</a>    

        <table width="100%">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="compositeSequence.name" />
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getName() != null){%>
                    	<jsp:getProperty name="compositeSequence" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>      
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="compositeSequence.description" />
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getDescription() != null){%>
                    	<jsp:getProperty name="compositeSequence" property="description" />
                    <%}else{
                    	out.print("No description available");
                    }%>
                </td>
            </tr>        
            <tr>
                <td valign="top">
                    <b>
                    	Taxon
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getTaxon() != null){%>
                    	${ compositeSequence.biologicalCharacteristic.taxon.scientificName}
                    <%}else{
                    	out.print("No taxon information available");
                    }%>
                </td>
            </tr>     
            <tr>
                <td valign="top">
                    <b>
                        Biosequence
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getName() != null){%>
                		${compositeSequence.biologicalCharacteristic.name }
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>   
            <tr>
                <td valign="top">
                    <b>
                        Biosequence accession
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null){
                        String organism = compositeSequence.getBiologicalCharacteristic().getTaxon().getCommonName();
                        String database = "hg18";
                        if (organism.equalsIgnoreCase( "Human" )) {
                            database = "hg18";
                        }
                        else if (organism.equalsIgnoreCase( "Rat" )) {
                            database = "rn4";
                        }
                        else if (organism.equalsIgnoreCase( "Mouse" )){
                            database = "mm8";
                        }
                        // build position if the biosequence has an accession
                        // otherwise point to location
                        String position = compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry().getAccession();
                        String link = position + " <a href='http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org=" + organism + "&db=" + database + "&position=+" + position + "&pix=620'>(Search UCSC Genome Browser)</a>";                	
                		
                        out.print(link);
                	
                    }else{
                    	out.print("No accession available");
                    }%>
                </td>
            </tr>       
            <tr>
                <td valign="top">
                    <b>
                        Sequence length
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getSequence() != null) {
                		out.print(compositeSequence.getBiologicalCharacteristic().getSequence().length());
                    }else{
                    	out.print("No sequence available");
                    }%>
                </td>
            </tr>  
            <tr>
                <td valign="top">
                    <b>
						Sequence
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getSequence() != null ){
                		String sequence = compositeSequence.getBiologicalCharacteristic().getSequence();
                		String formattedSequence = "";
                		int nextIndex = 0;
                		for (int i = 0; i < sequence.length() - 80; i += 80) {
                		 	formattedSequence += sequence.substring(i,i+80);  
                		 	formattedSequence += "<br />";
                		 	nextIndex = i+80;
                		}
                		if ( (sequence.length() % 80) != 0) {
                		 	formattedSequence += sequence.substring(nextIndex, sequence.length());
                		 	formattedSequence += "<br />";
                		}
                	%>
                	<div class="clob">
                	<% 
                		out.print(formattedSequence);
                	%>
                	</div>
                    <%}else{
                    	out.print("No sequence available");
                    }%>
                </td>
            </tr>      
        </table>
 </td>
 </tr>
 <tr>
 <td>
 <div id="tableContainer" class="tableContainer">
 		<script type="text/javascript">
 			initBodyTag();
 		</script>
 		<div>&nbsp;</div>
 		<br />
		<display:table name="blatResults"  requestURI="" id="blatResult" style="width:100%;"
             pagesize="2000"
             decorator="ubic.gemma.web.taglib.displaytag.expression.designElement.CompositeSequenceWrapper"
             class="scrollTable"
             defaultsort="2"
             defaultorder="descending"
             >		 
			<display:column property="blatResult" title="Alignment" headerClass="fixedHeader"/>
			<display:column property="blatScore" title="S" headerClass="fixedHeader"/>		
			<display:column property="blatIdentity" title="I" headerClass="fixedHeader"/>	
			<display:column property="geneProducts" title="GeneProducts" headerClass="fixedHeader"/>
			<display:column property="genes" title="Genes" headerClass="fixedHeader"/>	
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>
</div>
</td>
</tr>
</table>		
</aa:zone>

    </body>
</html>
