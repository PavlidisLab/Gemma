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
        <%=expressionExperiment.getOwner() %>
        <br />

        <%=expressionExperiment.getBioAssays() %>
        <br />

        <%=expressionExperiment.getExperimentalDesigns() %>
        <br />

        <%=expressionExperiment.getInvestigators() %>
        <br />

        <%=expressionExperiment.getPrimaryPublication() %>
        <br />

        <%=expressionExperiment.getSubsets() %>
        <br />

        <%=expressionExperiment.getAuditTrail() %>
        <br />

        <%=expressionExperiment.getAnalyses() %>
        <br />

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
        </TABLE>
    </body>
</html>
