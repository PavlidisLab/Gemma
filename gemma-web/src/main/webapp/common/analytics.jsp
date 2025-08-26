<%@ include file="/common/taglibs.jsp" %>
<%--@elvariable id="appConfig" type="java.util.Map"--%>

<c:if test='${ appConfig["ga.tracker"] != null}'>
    <!-- Google tag (gtag.js) -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=${appConfig["ga.tracker"]}"></script>
    <script>
    window.dataLayer = window.dataLayer || [];

    function gtag() {
       dataLayer.push( arguments );
    }

    gtag( 'js', new Date() );

    gtag( 'config', '${appConfig["ga.tracker"]}' );
    </script>
</c:if>
