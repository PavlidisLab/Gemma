<%--@elvariable id="appConfig" type="java.util.Map"--%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>RESTful API documentation | Gemma</title>
    <link rel="stylesheet" type="text/css" href="swagger-ui.css"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
<header class="header">
    <a href="${pageContext.request.contextPath}/home.html" class="logo">
        <img src="${pageContext.request.contextPath}/images/logo/gemma-headerlogo.png" height="60" alt="Gemma Logo"/>
    </a>
    <a href="https://www.ubc.ca/" class="right-logo">
        <img src="${pageContext.request.contextPath}/images/logo/ubc-logo-2018-crest-blue-rgb72.jpg" height="40" alt="UBC Logo"/>
    </a>
</header>
<div id="swagger-ui"></div>
<script src="swagger-ui-bundle.js"></script>
<script>
    window.onload = function () {
        window.ui = SwaggerUIBundle({
            url: '${pageContext.request.contextPath}/rest/v2/openapi.json',
            dom_id: '#swagger-ui',
            deepLinking: true,
            presets: [
                SwaggerUIBundle.presets.apis
            ]
        });
    };
</script>
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
</body>
</html>