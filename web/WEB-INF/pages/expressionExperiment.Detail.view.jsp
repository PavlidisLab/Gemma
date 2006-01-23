<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
    class="edu.columbia.gemma.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <SCRIPT LANGUAGE="JavaScript">
	function selectButton(target){
		if(target == 0){
			document.detailsForm._eventId.value="back"
			document.detailsForm.action="expressionExperiments.htm"
		}
		if(target == 1){
		<%-- confirm deletion of item --%>
			if (confirm("Are you sure you want to delete this?")){
				document.detailsForm._eventId.value="delete"
				document.detailsForm.action="expressionExperimentDetails.htm"
				<%--
				Not yet implemented for webflows
				document.backForm._flowId.value="pubMed.Delete" 
				document.backForm.action="flowController.htm"
				--%>
				}
		}
		if(target == 2){
			document.detailsForm._eventId.value="edit"
			document.detailsForm._flowId.value="expressionExperiment.Edit"
			document.detailsForm.action="flowController.htm"
		}
		document.detailsForm.submit();
	}
	</SCRIPT>
    </head>
    <body>
        <h2>
            <fmt:message key="expressionExperiment.details" />
        </h2>
        <TABLE width="100%">
            <tr>
                <td>
                    <B>
                        <fmt:message key="name" />
                    </B>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="name" />
                </td>
            </tr>
            <tr>
                <td>
                    <B>
                        <fmt:message key="description" />
                    </B>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="description" />
                </td>
            </tr>
            <tr>
                <td>
                    <B>
                        <fmt:message key="source" />
                    </B>
                </td>
                <td>
                    <jsp:getProperty name="expressionExperiment" property="source" />
                </td>
            </tr>
            <tr>
                <td>
                    <B>
                        <fmt:message key="expressionExperiment.accession" />
                    </B>
                </td>
                <td>
                    <%=expressionExperiment.getAccession().getExternalDatabase().getName()%>
                    :&nbsp;
                    <%=expressionExperiment.getAccession().getAccession()%>
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
        <display:table name="expressionExperiment.bioAssays" class="list" pagesize="10" >
         <display:column property="id" sort="true" />
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
        </display:table>

        <h3>
            Experimental designs
        </h3>
        <display:table name="expressionExperiment.experimentalDesigns" class="list">

            <display:column property="name" sort="true" maxWords="20" />
            <display:column property="description" sort="true" maxWords="100"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>

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
        <%=expressionExperiment.getAuditTrail() %>
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
        <table>
            <tr>
                <TD>
                    <DIV align="right">
                        <INPUT type="button" onclick="javascript:selectButton(0)" value="Back">
                    </DIV>
                </td>

                <authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
                    <TD>
                        <DIV align="right">
                            <INPUT type="button" onclick="javascript:selectButton(1)" value="Delete">
                        </DIV>
                    </td>
                </authz:acl>

                <authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
                    <TD>
                        <DIV align="right">
                            <INPUT type="button" onclick="javascript:selectButton(2)" value="Edit">
                        </DIV>
                    </td>
                </authz:acl>
            </tr>
        </table>

    </body>
</html>
