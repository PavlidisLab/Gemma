<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalDesign" scope="request"
    class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="experimentalDesign.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalDesign.name" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getName() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="name" />
                    <%}else{
                    	out.print("Name unavailable");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalDesign.description" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getDescription() != null){%>
						<jsp:getProperty name="experimentalDesign" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
         
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalDesign.replicateDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getReplicateDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="replicateDescription" />
                    <%}else{
                    	out.print("Replicate description unavailable");
                    }%>
                </td>
            </tr>    
      
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalDesign.qualityControlDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getQualityControlDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="qualityControlDescription" />
                    <%}else{
                    	out.print("Quality control description unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalDesign.normalizationDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getNormalizationDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="normalizationDescription" />
                    <%}else{
                    	out.print("Normalization description unavailable");
                    }%>
                </td>
            </tr>

        </table>

        <h3>
            <fmt:message key="experimentalFactors.title" />
        </h3>
        <display:table name="experimentalDesign.experimentalFactors" class="list" requestURI="" id="experimentalFactorList"
        export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExperimentalDesignWrapper">
            <display:column property="name" sortable="true" href="/Gemma/experimentalFactor/showExperimentalFactor.html" paramId="id" paramProperty="id" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:column property="factorValuesLink" sortable="true" maxWords="100" titleKey="experimentalDesign.factorValues"  />
        </display:table>

        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=experimentalDesign.getAuditTrail()%>" />
        <br />
        <hr />
        <hr />
        
    <table>
    <tr>
    <td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
            value="Back"></div>
            </td>
        <authz:acl domainObject="${experimentalDesign}" hasPermission="1,6">
            <td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='editExperimentalDesign.html?id=<%=request.getAttribute("id")%>'"
            value="Edit"></div>
            </td>
        </authz:acl>
    </tr>
    </table>
    </body>
</html>
