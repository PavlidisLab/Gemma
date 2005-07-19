<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="arrayDesign" scope="request" class="edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>

<HEAD>
</HEAD>
	<BODY>
		<DIV align="left">
			<FORM name="searchForm" action="">
				<INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
				<INPUT type="hidden" name="_currentStateId" value="criteria.view">
				<INPUT type="hidden" name="_eventId" value="getArrayDesigns">
				<INPUT type="hidden" name="_flowId" value="">

					<TR>
						<TD>
							<DIV align="left"><b>View All Gemma ArrayDesigns</b></DIV>
						</TD>
					</TR>
					<TR>
						<TD COLSPAN="2"><HR></TD>
					</TR>
					<TR>
  					    <TD COLSPAN="2">
						    <DIV align="left">
							    <INPUT type="submit" value="Select">
						    </DIV>
						</TD>
					</TR>
					<TR>
						<TD COLSPAN="2"><HR></TD>
					</TR>


				</TABLE>
		</DIV>	
	</BODY>
</HTML>
