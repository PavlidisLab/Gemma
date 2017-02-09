<%@ include file="/common/taglibs.jsp"%>
<head>
<title>System stats</title>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/monitoring.js' />

</head>
<body>
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<h2>Hibernate statistics</h2>

		<div>
			<input id="hibstat-reset-button" type="button" name="Clear Hibernate stats" value="Clear Hibernate stats"
				onClick="resetHibernateStats();" />
		</div>

		<div id="hibernateStats">
			<pre>Waiting ...</pre>
		</div>

		<h2>Cache stats</h2>

		<div id="cache-pause-stats-checkbox-div"></div>

		<div id="cacheStats">Waiting ...</div>

		<h2>Space stats</h2>
		<div id="spaceStats">Waiting ...</div>

		<h2>Twitter administration</h2>
		<label><input type="checkbox" onClick="twitterControl(this);" />Twitter enabled?</label>

		<hr />
		<div id="twitter-admin-div"></div>
		<div>
			<input id="test-twitter-button" type="button" name="Test twitter" value="Test twitter"
				onClick="tweetManuallyConfirm();" />
		</div>
	</security:authorize>
</body>
