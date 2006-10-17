<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="gene" scope="request"
    class="ubic.gemma.model.genome.GeneImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="gene.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="gene.name" />
                    </b>
                </td>
                <td>
                	<%if (gene.getName() != null){%>
                    	<jsp:getProperty name="gene" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="gene.description" />
                    </b>
                </td>
                <td>
                	<%if (gene.getDescription() != null){%>
                    	<jsp:getProperty name="gene" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
         </table>
        <%
		if ( gene.getAccessions().size() > 0 ) {
		%>
         <h3>
			<fmt:message key="gene.accessions" />
		</h3>
		<% 
		} 
		%>

         <display:table name="gene.accessions" class="list" requestURI="" id="accessionsList" 
			pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper">
			<display:column property="accession" sortable="true" maxWords="20"/>
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>  
		
    </body>
</html>
