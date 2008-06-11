<%-- Other notifications and...stuff --%>
<%@ include file="/common/taglibs.jsp"%>

<h2>
	Welcome to Gemma
</h2>
<p>
	Gemma is a database and software system for the
	<strong>meta-analysis of gene expression data</strong>. Gemma contains data from hundreds of
	<a href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">public microarray data sets</a>,
	referencing hundreds of
	<a href="<c:url value="/bibRef/showAllEeBibRefs.html"/>">published papers</a>. More information about the project is
	<a href="<c:url value="/static/about.html"/>">here</a>.
</p>

<script type="text/JavaScript">
  Ext.onReady( function() {
      settings = {
          tl: { radius: 10 },
          tr: { radius: 10 },
          bl: { radius: 10 },
          br: { radius: 10 },
          antiAlias: true,
          autoPad: true,
          validTags: ["div"]
      }
     var newsbox = new curvyCorners(settings, "latestNews");
     newsbox.applyCornersToAll(); 
  } );
</script>

<div class="latestNews" style="background: #FFF47E; padding-bottom: 1.8em">
	<h4 style="margin-left: 10px">
		Gemma software updates
	</h4>
	<p>
		Gemma 1.2 introduces more features for differential expression, including meta-analysis. Many parts of the user interface
		feature improvements. For more details see the
		<a href="<c:url value='/resources/dist/RELEASE-NOTES-1.2.txt' />">release notes</a>.

	</p>
</div>

