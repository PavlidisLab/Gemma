<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalFactor" scope="request"
    class="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title> <fmt:message key="experimentalFactor.details" /> </title>
        <h2>
            <fmt:message key="experimentalFactor.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalFactor.name" />
                    </b>
                </td>
                <td>
                	<%if (experimentalFactor.getName() != null){%>
                    	<jsp:getProperty name="experimentalFactor" property="name" />
                    <%}else{
                    	out.print("Name unavailable");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="experimentalFactor.description" />
                    </b>
                </td>
                <td>
                	<%if (experimentalFactor.getDescription() != null){%>
						<jsp:getProperty name="experimentalFactor" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>

        </table>

        <h3>
            Factor Values
        </h3>
        <display:table name="experimentalFactor.factorValues" class="list" requestURI="" id="factorValueList"
         pagesize="10">
            <display:column property="value" sortable="true" />
        </display:table>

        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=experimentalFactor.getAuditTrail()%>" />
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
        <security:acl domainObject="${experimentalFactor}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='editExperimentalFactor.html?id=<%=request.getAttribute("id")%>'"
            value="Edit"></DIV>
            </TD>
        </security:acl>
    </TR>
    </table>
