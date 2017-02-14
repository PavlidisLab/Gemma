<%@ include file="/common/taglibs.jsp"%>
<head>
<title>System stats</title>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/monitoring.js' />

<style>	
	.padded{
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
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<div class="padded">
		
			<h2>Hibernate statistics</h2>
			<div class="v-padded">
				<input id="hibstat-reset-button" type="button"
					name="Clear Hibernate stats" value="Clear Hibernate stats"
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

			<h2>Space stats</h2>			
			<div id="spaceStats" class="v-padded">Waiting ...</div>
			
			<hr class="normal">

			<h2>Twitter administration</h2>
			<div class="v-padded">
				<input type="checkbox" onClick="twitterControl(this);" id="chbox-twitter"/>			
				<label for="chbox-twitter">
					Twitter enabled?
				</label>
			</div>
			
			<span id="twitter-admin-div" class=""></span>			
			<span class="inline">
				<input id="test-twitter-button" type="button" name="Test twitter"
					value="Test twitter" onClick="tweetManuallyConfirm();" />
			</span>
			
		</div>
	</security:authorize>
</body>
