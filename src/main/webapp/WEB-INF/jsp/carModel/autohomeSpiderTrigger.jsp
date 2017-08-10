<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
%><!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>汽车之家爬虫触发器</title>
<script type="text/javascript" src="../js/jquery.min.js"></script>
</head>
<body>
<ul>
  <li><input type="button" value="爬取品牌数据" onclick="drawBrands();"></li>
  <li><input type="button" value="爬取车系数据" onclick="drawSerieses();"></li>
  <li><input type="button" value="爬取车型数据" onclick="drawModels();"></li>
  <li><input type="button" value="爬取品牌图标" onclick="drawBrandLogos();"></li>
</ul>
<script type="text/javascript">
function drawBrands() {
    if (confirm("爬取品牌数据可能会刷新现有品牌的数据.确定操作吗？")) {
        $.ajax({
            url : "drawBrands",
            method : "POST",
            success : function(data, textStatus, jqXHR) {
                if ("Succeeded" === data) {
                    alert("操作成功");
                    location.reload();
                } else {
                    alert("操作失败");
                }
            }
        });
    }
}

function drawSerieses() {
    if (confirm("爬取车系数据可能会刷新现有车系的数据.确定操作吗？")) {
        $.ajax({
            url : "drawSerieses",
            method : "POST",
            success : function(data, textStatus, jqXHR) {
                if ("Succeeded" === data) {
                    alert("操作成功");
                    location.reload();
                } else {
                    alert("操作失败");
                }
            }
        });
    }
}

function drawModels() {
    if (confirm("爬取车系数据可能会刷新现有车型的数据.确定操作吗？")) {
        $.ajax({
            url : "drawModels",
            method : "POST",
            success : function(data, textStatus, jqXHR) {
                if ("Succeeded" === data) {
                    alert("操作成功");
                    location.reload();
                } else {
                    alert("操作失败");
                }
            }
        });
    }
}

function drawBrandLogos() {
    if (confirm("爬取品牌图标可能会刷新现有车型的数据.确定操作吗？")) {
        $.ajax({
            url : "drawBrandLogos",
            method : "POST",
            success : function(data, textStatus, jqXHR) {
                if ("Succeeded" === data) {
                    alert("操作成功");
                    location.reload();
                } else {
                    alert("操作失败");
                }
            }
        });
    }
}
</script>
</body>
</html>