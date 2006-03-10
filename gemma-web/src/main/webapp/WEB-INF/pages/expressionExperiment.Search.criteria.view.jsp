<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE HTML PUBLIC "-//W3C//Dtd HTML 4.01 transitional//EN">
<HTML>
    <SCRIPT LANGUAGE="JavaScript">
	function selectButton(target){	
		if(target == 1){
			document.searchForm._eventId.value="getExpressionExperiments"
	
 			document.searchForm.submit();
		}
	}
	</SCRIPT>

    <HEAD>
    </HEAD>
    <BODY>
        <DIV align="left">
            <FORM name="searchForm" action="expressionExperimentSearch.htm">
                <INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
                <INPUT type="hidden" name="_currentStateId" value="criteria.view">
                <INPUT type="hidden" name="_eventId" value="">
                <INPUT type="hidden" name="_flowId" value="">

                <tr>
                    <td>
                        <DIV align="left">
                            <b>View All Gemma ExpressionExperiments</b>
                        </DIV>
                    </td>
                </tr>
                <tr>
                    <td COLSPAN="2">
                        <HR>
                    </td>
                </tr>
                <tr>
                    <td COLSPAN="2">
                        <DIV align="left">
                            <INPUT type="button" onclick="javascript:selectButton(1)" value="Select">
                        </DIV>
                    </td>
                </tr>
                <tr>
                    <td COLSPAN="2">
                        <HR>
                    </td>
                </tr>


                </TABLE>
        </DIV>
    </BODY>
</HTML>
