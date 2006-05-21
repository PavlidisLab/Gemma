<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioMaterial" scope="request"
    class="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="bioMaterial.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioMaterial.name" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getName() != null){%>
                    	<jsp:getProperty name="bioMaterial" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioMaterial.description" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getDescription() != null){%>
                    	<jsp:getProperty name="bioMaterial" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
                 
         </table>  
        <h3>
            <fmt:message key="treatments.title" />
        </h3>
        <display:table name="bioMaterial.treatments" class="list" requestURI="" id="treatmentList"
        export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
       	    <display:column property="id" sortable="true" href="/Gemma/bioMaterial/showBioMaterial.html" paramId="id" paramProperty="id"/>
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:column property="orderApplied" maxWords="100" />
        </display:table>
        
        <h3>
            <fmt:message key="characteristics.title" />
        </h3>
        <display:table name="bioMaterial.characteristics" class="list" requestURI="" id="characteristicList"
        export="true" pagesize="10" >
            <display:column property="id" maxWords="20" sortable="true" href="/Gemma/bioMaterial/showBioMaterial.html" paramId="id" paramProperty="id"/>
            <display:column property="category" maxWords="100" />
            <display:column property="value" maxWords="100" />
        </display:table>
		
		<h3>
            <fmt:message key="taxon.title" />
        </h3>
        <Gemma:taxon
            taxon="<%=bioMaterial.getSourceTaxon()%>" />
        <br />
        		
		<h3>
            <fmt:message key="databaseEntry.title" />
        </h3>
        <Gemma:databaseEntry
            databaseEntry="<%=bioMaterial.getExternalAccession()%>" />
        <br/>
        
        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=bioMaterial.getAuditTrail()%>" />
        <br />

        <hr />
        <hr />
    
    <table>
    <TR>
    <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
            value="Back"></DIV>
            </TD>
        <authz:acl domainObject="${bioMaterial}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='/Gemma/bioMaterial/editBioMaterial.html?id=<%=bioMaterial.getId()%>'"
            value="Edit"></DIV>
            </TD>
        </authz:acl>
    </TR>
    </table>
    </body>
</html>
