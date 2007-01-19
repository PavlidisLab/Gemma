<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request"
    class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
  <title> <fmt:message key="compositeSequence.title" /> </title>
  <script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>

  		<aa:zone name="csTable">
  
  <table id="csTableList">
  <tr>
  <td>
        <h2>
            <fmt:message key="compositeSequence.title" />
        </h2>
        <table width="100%" >
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
						Sequence
                    </b>
                </td>
                <td>
                	<%if (compositeSequence.getBiologicalCharacteristic().getSequence() != null ){%>
                		<div class="clob">${compositeSequence.biologicalCharacteristic.sequence }</div>
                		
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
		<display:table name="blatResults" class="list" requestURI="" id="blatResult"
             pagesize="20"
             decorator="ubic.gemma.web.taglib.displaytag.expression.designElement.CompositeSequenceWrapper"
             >		 
			<display:column property="blatResult" sortable="true" title="Alignment" />
			<display:column property="blatScore" sortable="true" title="S" />		
			<display:column property="blatIdentity" sortable="true" title="I" />	
			<display:column property="geneProducts" title="GeneProducts" />
			<display:column property="genes" title="Genes" />	
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>
</td>
</tr>
</table>		
</aa:zone>
<script type="text/javascript" src="<c:url value="/scripts/aa-init.js"/>"></script>
    </body>
</html>
