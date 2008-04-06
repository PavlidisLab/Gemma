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
<h3>
	News
</h3>
<div class="latestNews" style="background: #FFF47E; padding-bottom: 0.8em">
	<h4 style="margin-left: 10px">
		April 7, 2008 - Gemma 1.1
	</h4>
	<p>
		Gemma 1.1 introduces improved interfaces for coexpression and differential expression analysis. You should notice some
		speed improvements. We have also included several new 'presets' for analyses of coexpression of data sets relating to the
		nervous system. For a more complete list of changes, see the
		<a href="<c:url value='/resources/dist/RELEASE-NOTES-1.1.txt' />">release notes</a>.
	</p>
</div>


