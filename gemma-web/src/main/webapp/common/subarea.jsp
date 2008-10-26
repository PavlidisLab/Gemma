<%-- Other notifications and...stuff --%>
<%@ include file="/common/taglibs.jsp"%>

<div id="left-bar-messages">
	<h2>
		Welcome!
	</h2>
	<p style="font-size: 0.90em">
		Gemma is a database and software system for the
		<strong>meta-analysis of gene expression data</strong>. Gemma contains data from hundreds of public
		<a href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">microarray data sets</a>,
		referencing hundreds of
		<a href="<c:url value="/bibRef/showAllEeBibRefs.html"/>">published papers</a>. Users can search, access and visualize
		<a href="<c:url value="/searchCoexpression.html"/>">coexpression</a> and
		<a href="<c:url value="/diff/diffExpressionSearch.html"/>">differential expression</a> results.
	</p>
	<p style="font-size: 0.9em">
		More information about the project is
		<a href="<c:url value="/static/about.html"/>">here</a>. Gemma also has a
		<a href="http://bioinformatics.ubc.ca/confluence/display/gemma">Wiki</a> where you can read additional documentation,
		in addition to the in-line help.
	</p>
</div>

<div id="coexpression-area">
	<div id="coexpression-messages" style="font-size: smaller; width: 250px;">
		<h3>
			Coexpression query
		</h3>
	</div>

	<div id="coexpression-form"></div>

	<div id="sampleQueries" style="padding: 4px; width: 250px;">
		Examples: rat
		<a href='<c:url value="/searchCoexpression.html?g=938103&amp;a=776" />'>Ddn</a>; mouse
		<a href='<c:url value="/searchCoexpression.html?g=598735&amp;s=3&amp;a=708" />'>Mapk3</a>
	</div>
</div>


