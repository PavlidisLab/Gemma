<%@ include file="/common/taglibs.jsp"%>

<HTML>
	<BODY>
		<DIV align="left">Edit Array Design Information</DIV>
		<HR>
		<DIV align="left">
			<FORM name="arrayDesignForm" method="post" action="arrayDesignEdit.htm">
				<INPUT type="hidden" name="_flowExecutionId" value="<c:out value="${flowExecutionId}"/>">
				<INPUT type="hidden" name="_eventId" value="submit">
				
				Name:
				<spring:bind path="arrayDesign.name">
					<INPUT
						type="text"
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>">
					<c:if test="${status.error}">
						<DIV style="color: red"><c:out value="${status.errorMessage}"/></DIV>
					</c:if>
				</spring:bind>
				
				<BR>
				
				Description:
				<spring:bind path="arrayDesign.description">
					<INPUT
						type="text"
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>">
					<c:if test="${status.error}">
						<DIV style="color: red"><c:out value="${status.errorMessage}"/></DIV>
					</c:if>
				</spring:bind>
			</FORM>
		</DIV>
		<HR>
		<DIV align="right">
			<INPUT type="button" onclick="javascript:document.arrayDesignForm.submit()" value="Submit">
		</DIV>
	</BODY>
</HTML>