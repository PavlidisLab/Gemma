<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Maintenance Mode</title>
</head>

<div class="padded">
    <p>
        Maintenance mode simply puts a notice on every page that things might
        be broken.
    </p>
    <form name="maintenanceForm" method="POST">
        <input type="submit" style="color: #BB3333" name="start"
                value="Enter Maintenance Mode">

        <input type="submit" style="color: #BB3333" name="stop"
                value="Exit Maintenance Mode">
    </form>
</div>