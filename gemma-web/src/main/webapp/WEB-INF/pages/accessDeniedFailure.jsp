<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>
<%-- $Id$ --%>


		<title>Access Denied</title>

		<content tag="heading">
		Access Denied Failure
		</content>

	<security:authorize access="!hasAnyRole('GROUP_USER','GROUP_ADMIN')">
		<script type="text/javascript">
			Gemma.AjaxLogin.showLoginWindowFn(true);
		</script>
	<p>You do not have permission to access this page.</p>
</security:authorize>
