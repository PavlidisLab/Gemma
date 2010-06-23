<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>System stats</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/monitoring.js' />

</head>
<body>
	<h2>
		Hibernate statistics
	</h2>

	<div>
		<input id="hibstat-reset-button" type="button" name="Clear Hibernate stats" value="Clear Hibernate stats"
			onClick="resetHibernateStats();" />
	</div>
	<pre>
		<div id="hibernateStats">Waiting ...</div>
		</pre>
	<h2>
		Cache stats
	</h2>

	<div id="cache-pause-stats-checkbox-div" ></div>

	<div id="cacheStats">
		Waiting ...
	</div>

	<h2>
		Space stats
	</h2>
	<div id="spaceStats">
		Waiting ...
	</div>

</body>
