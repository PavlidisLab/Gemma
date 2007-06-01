<%@ include file="/common/taglibs.jsp"%>

		<title><fmt:message key="testdata.title" /></title>
		
		<content tag="heading">
		<fmt:message key="testdata.heading" />
		</content>

		<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type='text/javascript' src="<c:url value="scripts/progressbar.js"/>"></script>
		<style type="text/css">
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
</style>


		<p>
			<fmt:message key="testdata.instructions" />
		</p>

		<form method="post" action="addTestData.html" onsubmit="startProgress()">
			<input type="submit" value="Add some data" name="submit" />
		</form>

		<script type="text/javascript">createIndeterminateProgressBar();</script>

