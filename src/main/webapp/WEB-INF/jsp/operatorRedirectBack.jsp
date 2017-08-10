<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" session="false"
%><html>
<head>
    <title></title>
</head>
<body>
<form id="formId" action="${actionUrl}" enctype="application/x-www-form-urlencoded">
    <input type="hidden" name="jwt" value="${jwt}">
    <input type="hidden" name="openId" value="${openId}">
    <input type="hidden" name="targetUrl" value="${targetUrl}">
</form>
<script type="text/javascript">
    document.getElementById("formId").submit();
</script>
</body>
</html>
