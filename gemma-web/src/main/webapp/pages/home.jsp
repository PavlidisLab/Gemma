<%--@elvariable id="appConfig" type="java.util.List"--%>
<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Home</title>

<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<%-- <jwr:script src='/scripts/app/HomePageAnalysisSearch.js' /> --%>
<jwr:script src='/scripts/scriptsnonjawr/arbor.js' />
</head>

<script type="text/javascript">
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.onReady( function() {

   Ext.QuickTips.init();

   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
   var summaryPanel = new Gemma.ExpressionExperimentsSummaryPanel( {
      height : 200,
      flex : '0'
   } );
   summaryPanel.render( "summaryPanel-div" );

   var generalSearchPanel = new Gemma.Search.GeneralSearchSimple();
   generalSearchPanel.render( "generalSearchSimple-div" );


} );
</script>

<input type="hidden" id="reloadOnLogout" value="false">

<%-- jshowoff.css is included in the bundle --%>
<div id="sloganText" class="container">
    <h5 style="text-align: center;" class="gray">
        Database of curated and re-analyzed gene expression studies
    </h5>
</div>

<div id="frontPageSlideShow">
    <div class="container"
            style="display: flex; flex-wrap: wrap; justify-content: space-between; gap: 20px; margin-top: 40px; margin-bottom: 40px;">
        <div style="width:330px;" class="small">
            Gemma provides data, experimental design annotations, and differential expression analysis results for
            thousands
            of microarray and RNA-seq experiments. We re-analyze raw data from public sources (primarily NCBI <a
                href="https://www.ncbi.nlm.nih.gov/geo/">GEO</a>),
            annotate experimental conditions, conduct quality control and compute differential expression using
            standardized
            procedures. We have especially good coverage of experiments relevant to the nervous system. See the <a
                href="https://pavlidislab.github.io/Gemma/">documentation</a> for more information.
            Gemma was and developed and is maintained by the <a href="https://pavlab.msl.ubc.ca/" target="_blank">Pavlidis
            group at UBC</a>.
        </div>

        <%-- Width here needs to be enough to fit the labels at left and right edges --%>
        <%-- <img src="https://chart.apis.google.com/chart?chs=340x240&cht=p&chco=224499&chd=${ googleData}
                                     &chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
                                     &chdlp=b|l&chdls=2f2f2f,13"
                 width="340" height="240"
                 alt="A pie chart representing proportion of taxa among Gemma datasets." />--%>
        <img style="box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.5);"
                src="${pageContext.request.contextPath}/images/showoff.png"
                width="300"
                alt="Example of a dataset view overlaid with a heatmap of top differentially expressed probes." />
        <%--  <img style="position: absolute; top: 180px; left: 435px;"
                  src="${pageContext.request.contextPath}/images/slideShow/humanOutline_35_3.png"
                  alt="An overlay of a human on top of its section of the pie chart."
                  width="35" height="53" />
          <img style="position: absolute; top: 300px; left: 420px;"
                  src="${pageContext.request.contextPath}/images/slideShow/mouseOutline65_simplified.png"
                  alt="An overlay of a mouse on top of its section of the pie chart."
                  width="65" height="17" />--%>
        <!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
        <%--   <div id="featuredNumberOfDatasets" class="slideTextTD"></div>--%>

        <div style="width:250px;" id="summaryPanel-div"></div>
    </div>
</div>
<%--
<script type="text/javascript">
$.getJSON( '${pageContext.request.contextPath}' + '/rest/v2/datasets/count', function( data ) {
      var numberFormatter = new Intl.NumberFormat();
      var featuredNumberOfDatasets = 1000 * Math.floor( data.data / 1000 );
      document.getElementById( 'featuredNumberOfDatasets' ).innerHTML = 'Over <b>' + numberFormatter.format( featuredNumberOfDatasets ) + '</b> curated data sets';
   }
)
</script>--%>

<%@ include file="/common/messages.jsp" %>

<div class="container" style="margin-top: 40px; margin-bottom: 40px;">
    <div id="generalSearchSimple-div" style="margin-bottom: 40px;"></div>
    <%--    <div style="width:250px;padding-left:40px;" id="summaryPanel-div"></div>--%>
    <div id="news-updates">
        <div style="display:flex; flex-wrap: wrap; justify-content: space-between; gap: 10px; margin-bottom: 40px;">
            <p style="width: 795px;">
                Convenient programmatic access to Gemma's data and analyses is available via the software packages
                <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">gemma.R</a> (R/Bioconductor)
                and <a href="https://github.com/PavlidisLab/gemmapy">gemmapy</a> (Python).
            </p>
            <div style="display: flex; justify-content: space-around; width: 395px;">
                <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">
                    <img src="${pageContext.request.contextPath}/images/slideShow/bioconductor-logo.png"
                            alt="Bioconductor Logo"
                            width="175" />
                </a>
                <a href="https://pypi.org/project/gemmapy/">
                    <img src="${pageContext.request.contextPath}/images/slideShow/pypi-logo.svg"
                            alt="PyPi Logo"
                            width="75" />
                </a>
            </div>
        </div>
        <div style="display:flex; flex-wrap: wrap; justify-content: space-between; gap: 10px; margin-bottom: 40px;">
            <p style="width: 795px;">
                We invite you to try out the new <a href="${appConfig['gemma.gemBrow.url']}">Gemma
                Browser</a>, our new interface for exploring and searching Gemma's data holdings. It's still in beta,
                and more features and improvements are planned, but we'd love to hear your feedback.
            </p>
            <a href="${appConfig['gemma.gemBrow.url']}" style="align-self: center;">
                <img
                        src="${pageContext.request.contextPath}/images/slideShow/gemma-browser-preview.png"
                        alt="A screenshot of the new Gemma Browser."
                        width="395" />
            </a>
        </div>
    </div>
    <p>Questions? Feel free to <a href="mailto:pavlab-support@msl.ubc.ca?subject=Gemma">reach out</a>.</p>
</div>

<div id="footer" style="position: fixed; bottom: 0; height: 24px; background: white;">
    <div id="divider"></div>
    <div class="footer" style="display: flex; align-items: baseline; gap: 10px; padding-left: 10px; padding-right: 10px;">
        <div>
            Gemma ${appConfig["gemma.version"] != null ? appConfig["gemma.version"] : "?"}
            <security:authorize access="hasAuthority('GROUP_ADMIN')">
                <span class="d-xl-inline">
                <c:if test="${appConfig['gemma.build.timestamp'] != null or appConfig['gemma.build.gitHash'] != null}">
                    built
                </c:if>
                <c:if test="${appConfig['gemma.build.timestamp'] != null}">
                    on ${appConfig["gemma.build.timestamp"]}
                </c:if>
                <c:if test="${appConfig['gemma.build.gitHash'] != null}">
                    from <a href="https://github.com/PavlidisLab/Gemma/commits/${appConfig['gemma.build.gitHash']}"
                        target="_blank" rel="noopener noreferrer">${appConfig["gemma.build.gitHash"]}</a>
                </c:if>
                </span>
            </security:authorize>
        </div>
        <div>Copyright &copy; 2007-2023</div>
        <div class="d-lg">
            Our <a href='<c:url value="https://pavlidislab.github.io/Gemma/terms.html" />'>Terms and conditions</a> have
            been updated!
        </div>
        <!-- <div><jsp:include page="/common/social.jsp"/></div> -->
        <div style="flex-grow: 1"></div>
        <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/" style="align-self: center;">
            <img alt="Creative Commons License"
                    src="https://i.creativecommons.org/l/by-nc/4.0/80x15.png" width="80" height="15" />
        </a>
    </div>
</div>