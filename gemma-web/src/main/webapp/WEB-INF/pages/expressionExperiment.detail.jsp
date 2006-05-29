<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
    class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
    <body>
        <h2>
            <fmt:message key="expressionExperiment.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.name" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getName() != null){%>
                    	<jsp:getProperty name="expressionExperiment" property="name"/>
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.description" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getDescription() != null){%>
                    	<jsp:getProperty name="expressionExperiment" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.source" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getSource() != null){%>
                    	<jsp:getProperty name="expressionExperiment" property="source" />
                    <%}else{
                    	out.print("Source unavailable");
                    }%>
                </td>
            </tr>    

        </table>
        
        <br />

        <h3>
            <fmt:message key="bioAssays.title" />
        </h3>
        <display:table name="expressionExperiment.bioAssays" class="list" requestURI="" id="bioAssayList" export="true" pagesize="10">
         	<display:column property="id" sortable="true" href="/Gemma/bioAssay/showBioAssay.html" paramId="id" paramProperty="id"/>
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:setProperty name="export.pdf" value="true" />
        </display:table>

        <h3>
            <fmt:message key="experimentalDesigns.title" />
        </h3>
        <display:table name="expressionExperiment.experimentalDesigns" class="list" requestURI="" id="experimentalDesignList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="id" sortable="true" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="id" paramProperty="id"/>
            <display:column property="name" sortable="true" maxWords="20" />
            <display:column property="description" sortable="true" maxWords="100"  />
            <display:column property="factorsLink" sortable="true" maxWords="100" titleKey="experimentalFactors.title"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
		
		<h3>
            <fmt:message key="investigators.title" />
        </h3>
        <display:table name="expressionExperiment.investigators" class="list" requestURI="" id="contactList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="name" sortable="true" maxWords="20" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="phone" sortable="true" maxWords="100"  />
            <display:column property="fax" sortable="true" maxWords="100"  />
            <display:column property="email" sortable="true" maxWords="100"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="analyses.title" />
        </h3>
        <display:table name="expressionExperiment.analyses" class="list" requestURI="" id="analysisList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="name" sortable="true" maxWords="20" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" sortable="true" maxWords="100"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="expressionExperimentSubsets.title" />
        </h3>
        <display:table name="expressionExperiment.subsets" class="list" requestURI="" id="subsetList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="designElementDataVectors.title" />
        </h3>
        <display:table name="expressionExperiment.designElementDataVectors" class="list" requestURI="" id="dataVectorList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
	
		<h3>
            <fmt:message key="expressionExperiment.owner" />
        </h3>
        <Gemma:contact
            contact="<%=expressionExperiment.getOwner()%>" />
        <br/>
        
        <h3>
            <fmt:message key="databaseEntry.title" />
        </h3>
        <Gemma:databaseEntry
            databaseEntry="<%=expressionExperiment.getAccession()%>" />
        <br/>
        
        <h3>
            <fmt:message key="pubMed.publication" />
        </h3>
        <Gemma:bibref bibliographicReference="<%=expressionExperiment.getPrimaryPublication() %>" />
        <br />
		
        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=expressionExperiment.getAuditTrail()%>" />
        
        <%--  
        <a href="<c:url value="/expressionExperiment/searchExpressionExperiment.html?id="<%=request.getAttribute("id")%>"/>">
            <fmt:message key="menu.searchExpressionExperiment" />
        </a>
        --%>
        
        <br />

        <hr />
        <hr />
        
    <table>
    <tr>
    		<td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='showAllExpressionExperiments.html'"
            value="Back"></div>
            </td>
            
            <td COLSPAN="2">  
            <div align="left"><input type="button"
            onclick="location.href='searchExpressionExperiment.html?id=<%=request.getAttribute("id")%>'"
            value="Visual"></div>
            </td>
            
        <authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
            <td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='editExpressionExperiment.html?id=<%=request.getAttribute("id")%>'"
            value="Edit"></div>
            </td>
        </authz:acl>
        
    </tr>
    </table>
    </body>
</html>
