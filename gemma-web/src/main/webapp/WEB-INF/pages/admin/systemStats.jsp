<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>System stats</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />

	<security:authorize ifAnyGranted="GROUP_ADMIN">
		<jwr:script src='/scripts/app/monitoring.js' />
	</security:authorize>


</head>
<body>
	<security:authorize ifNotGranted="GROUP_ADMIN">
		<p>
			Sorry, you do not have permissions to view this page.
		</p>
	</security:authorize>
	<security:authorize ifAnyGranted="GROUP_ADMIN">
		<h2>
			Hibernate statistics
		</h2>
		<pre>
		<div id="hibernateStats">Waiting ...</div>
		</pre>
		<h2>
			Cache stats
		</h2>
		<div id="cacheStats">
			Waiting ...
		</div>

		<h2>
			Space stats
		</h2>
		<pre>
		<div id="spaceStats">Waiting ...</div>
</pre>
	</security:authorize>
</body>
