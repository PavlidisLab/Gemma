<%@ include file="/common/taglibs.jsp"%>
<head>
<title>System stats</title>
<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
<Gemma:script src='/scripts/app/monitoring.js' />

<style>
.padded {
	line-height: 120%;
}

input[type=button] {
	padding: 0 4px;
	padding-top: 4px;
	padding-bottom: 3px;
}
</style>

</head>
<body>
	<security:authorize access="hasAuthority('GROUP_ADMIN')">
		<div class="padded">

			<h2>Hibernate statistics</h2>
			<div class="v-padded">
				<input id="hibstat-reset-button" type="button" name="Clear Hibernate stats" value="Clear Hibernate stats"
					onClick="resetHibernateStats();" />
			</div>
			<pre id="hibernateStats">
				Waiting ...
			</pre>

			<hr class="normal">

			<h2>Cache stats</h2>
			<div id="cache-pause-stats-checkbox-div" class="v-padded"></div>
			<div id="cacheStats" class="table-nice-wrapper odd-gray" class="v-padded">Waiting ...</div>

			<hr class="normal">


		</div>
	</security:authorize>
</body>
