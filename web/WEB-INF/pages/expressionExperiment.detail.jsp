<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
    class="edu.columbia.gemma.expression.experiment.ExpressionExperimentImpl" />
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
                        <fmt:message key="name" />
                    </b>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="name" />
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="description" />
                    </b>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="description" />
                </td>
            </tr>
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="source" />
                    </b>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="source" />
                </td>
            </tr>    
       
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.accession" />
                    </b>
                </td>
                <td>
                <% if (expressionExperiment.getAccession() != null) { %>
                    <%= expressionExperiment.getAccession().getExternalDatabase().getName() %>
                    :&nbsp;
                      <%= expressionExperiment.getAccession().getAccession() %>
                       <% } %>
                </td>
            </tr>

        </table>

        <h3>
            Owner
        </h3>
        <%=expressionExperiment.getOwner() %>
        <br />

        <h3>
            Bio Assays (arrays)
        </h3>
        <display:table name="expressionExperiment.bioAssays" class="list" pagesize="50" >
         <display:column property="id" sort="true" href="showBioAssay.html" paramId="name" paramProperty="name"/>
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
        </display:table>

        <h3>
            Experimental designs
        </h3>
        <display:table name="expressionExperiment.experimentalDesigns" class="list">

            <display:column property="name" sort="true" maxWords="20" href="showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" sort="true" maxWords="100"  />
            <%-- <display:column property="experimentalFactors" sort="true" maxWords="100"  />--%>
            <display:column title="Experimental Factors" sort="true"
                            href="../expressionExperiment/showAllExperimentalFactors.html" paramId="name" paramProperty="name">
                            <c:out value="${fn:length(expressionExperiment.experimentalDesigns)}" />
            </display:column>
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            Experimental factors
        </h3>
        <c:forEach items="${expressionExperiment.experimentalDesigns}" var="experimentalDesign">
          <display:table name="experimentalDesign.experimentalFactors" class="list">
        	<display:column property="category" sort="true" />                      
          </display:table>
	    </c:forEach>
		
        <h3>
            Investigators
        </h3>
        <%=expressionExperiment.getInvestigators() %>
        <br />

        <h3>
            Publication
        </h3>
        <%if ( expressionExperiment.getPrimaryPublication() != null ) {

                %>
        <Gemma:bibref bibliographicReference="   <%=expressionExperiment.getPrimaryPublication() %>" />
        <br />
        <%} else {

            %>
        <p>
            (No publication listed)
        </p>
        <%}

            %>

        <h3>
            Subset
        </h3>
        <%=expressionExperiment.getSubsets() %>
        <br />

        <h3>
            Audit trail
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=expressionExperiment.getAuditTrail()%>" />
        <br />

        <h3>
            Analyses
        </h3>
        <%=expressionExperiment.getAnalyses() %>
        <br />

        <h3>
            Number of design element data vectors
        </h3>
        <%=expressionExperiment.getDesignElementDataVectors().size() %>
        <br />

        <hr />
        
	<TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <table>
    <TR>
    <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='showAllExpressionExperiments.html'"
            value="Back"></DIV>
            </TD>
        <%--<r:isUserInRole role="admin">--%>
        <%--<authz:authorize ifAnyGranted="admin">--%>
        <authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='editExpressionExperiment.html?name=<%=request.getAttribute("name")%>'"
            value="Edit"></DIV>
            </TD>
        </authz:acl>
        <%--</authz:authorize>--%>
        <%--</r:isUserInRole> --%>
    </TR>
    </table>
    </body>
</html>
