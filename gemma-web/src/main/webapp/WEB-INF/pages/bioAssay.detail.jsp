<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioAssay" scope="request"
    class="ubic.gemma.model.expression.bioAssay.BioAssayImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="bioAssay.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioAssay.name" />
                    </b>
                </td>
                <td>
                	<%if (bioAssay.getName() != null){%>
                    	<jsp:getProperty name="bioAssay" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioAssay.description" />
                    </b>
                </td>
                <td>
                	<%if (bioAssay.getDescription() != null){%>
                    	<jsp:getProperty name="bioAssay" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
         </table>  
        <h3>
            <fmt:message key="bioMaterials.title" />
        </h3>
        <display:table name="bioAssay.samplesUsed" class="list" requestURI="" id="bioMaterialList"
        export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">
            <display:column property="name" maxWords="20" href="/Gemma/bioMaterial/showBioMaterial.html" paramId="id" paramProperty="id"/>
            <display:column property="description" maxWords="100" />
        </display:table>
        
        <h3>
            <fmt:message key="bioAssay.arrayDesigns" />
        </h3>
        <display:table name="bioAssay.arrayDesignsUsed" class="list" requestURI="" id="arrayDesignList"
        export="true" pagesize="10" >
            <display:column property="name" maxWords="20" sortable="true" href="/Gemma/arrayDesign/showArrayDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" maxWords="100" />
            <display:column property="advertisedNumberOfDesignElements" maxWords="100" />
        </display:table>
		
		<h3>
            <fmt:message key="databaseEntry.title" />
        </h3>
        <Gemma:databaseEntry
            databaseEntry="<%=bioAssay.getAccession()%>" />
        <br/>
        
        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=bioAssay.getAuditTrail()%>" />
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
        <authz:acl domainObject="${bioAssay}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='/Gemma/bioAssay/editBioAssay.html?id=<%=bioAssay.getId()%>'"
            value="Edit"></DIV>
            </TD>
        </authz:acl>
    </TR>
    </table>
    </body>
</html>
