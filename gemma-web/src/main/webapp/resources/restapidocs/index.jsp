<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>RESTful API documentation | Gemma</title>
    <link rel="stylesheet" type="text/css" href="swagger-ui.css"/>
</head>
<body>
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