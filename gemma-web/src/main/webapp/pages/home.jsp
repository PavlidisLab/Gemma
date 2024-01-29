<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Home</title>

<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<%-- <jwr:script src='/scripts/app/HomePageAnalysisSearch.js' /> --%>
<jwr:script src='/scripts/scriptsnonjawr/arbor.js' />
<style>
    /* Additional styles for larger screens */
    @media (min-width: 1080px) { /* Adjust 768px to the appropriate breakpoint for your design */
        .container {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
        }

        #generalSearchSimple-div {
            flex-grow: 1;
            /* Ensure it takes available space, adjust as needed */
        }

        #summaryPanel-div {
            /* This will push the div to the right at larger screens */
            order: 2; /* Flexbox order can be used to visually reorder elements */
            margin-left: auto; /* This pushes the div to the right */
        }
    }
</style>
</head>
<%@ include file="/pages/frontPageSlideShowShowOff.jsp" %>

<div id="main" style="text-align: left">
    <%@ include file="/common/messages.jsp" %>
</div>

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


<div style="display:flex;flex-direction:column;margin-left:100px">

    <div style="margin:50px;display:flex;flex-direction:row;align-items: flex-start">
        <div style="margin-bottom: 10px;width:500px" id="generalSearchSimple-div"></div>
    <%--    <div style="width:250px;padding-left:40px;" id="summaryPanel-div"></div>--%>
    </div>
    <div id="news-updates" style="width:75%;margin-left:50px;margin-right:50px;">
        <div style="display:flex; justify-content: space-between; margin-bottom: 40px;">
            <p style="margin-right: 15px;">
                Convenient programmatic access to Gemma's data and analyses is available via the
                software packages <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">gemma.R</a>
                (R/Bioconductor)
                and <a href="https://github.com/PavlidisLab/gemmapy">gemmapy</a> (Python).
            </p>
            <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">
                <img src="${pageContext.request.contextPath}/images/slideShow/bioconductor-logo.png"
                        alt="Bioconductor Logo"
                        width="175"
                        style="margin-right: 15px;" />
            </a>
            <a href="https://pypi.org/project/gemmapy/">
                <img src="${pageContext.request.contextPath}/images/slideShow/pypi-logo.svg"
                        alt="PyPi Logo"
                        width="75" />
            </a>
        </div>
        <div style="display:flex;justify-content: space-between; margin-bottom: 40px;">
            <p style="margin-right: 15px;">
                We invite you to try out the new <a href="${pageContext.request.contextPath}/browse">Gemma
                Browser</a>, our new interface for exploring and searching Gemma's data holdings. It's still in beta,
                and more features and improvements are planned, but we'd love to hear your feedback.
            </p>
            <a href="${pageContext.request.contextPath}/browse" style="align-self: center;">
                <img
                        src="${pageContext.request.contextPath}/images/slideShow/gemma-browser-preview.png"
                        alt="A screenshot of the new Gemma Browser."
                        width="200" />
            </a>
        </div>
    </div>
    <div style="margin:50px">
        <p>Questions? Feel free to <a href="mailto:pavlab-support@msl.ubc.ca?subject=Gemma">reach out</a>.</p>
    </div>
    <div id="footer" style="position: fixed; bottom: 0; height: 24px; background: white;">
        <div id="divider"></div>
        <div class="footer" style="display: flex; align-items: baseline; padding-left: 10px; padding-right: 10px;">
            <div>Gemma ${appConfig["version"]}</div>
            <div style="margin-left: 10px;">Copyright &copy; 2007-2023</div>
            <div style="margin-left: 10px;">
                Our <a href='<c:url value="https://pavlidislab.github.io/Gemma/terms.html" />'>Terms and conditions</a>
                have
                been updated!
            </div>
            <!-- <div style="margin-left: 10px;"><jsp:include page="/common/social.jsp"/></div> -->
            <div style="flex-grow: 1"></div>
            <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/" style="align-self: center;">
                <img alt="Creative Commons License"
                        src="https://i.creativecommons.org/l/by-nc/4.0/80x15.png" width="80" height="15" />
            </a>
        </div>
    </div>

</div>
<jsp:include page="/common/analytics.jsp"/>

