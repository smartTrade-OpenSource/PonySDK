<?php

$page = basename ($_SERVER['PHP_SELF']);

echo "<h1 id=\"logo\" class=\"grid_4\">PonySDK</h1>";

echo "<ul id=\"navigation\" class=\"grid_8\">";

echo "<li>";
if(strcmp($page,"discuss.php") == 0){
	print "<a href=\"discuss.php\" class=\"current\"><span class=\"meta\">Groups</span><br/>Discuss</a>";
}else{
	print "<a href=\"discuss.php\"><span class=\"meta\">Groups</span><br/>Discuss</a>";
}
echo "</li>";


echo "<li>";
if(strcmp($page,"download.php") == 0){
	print "<a href=\"download.php\" class=\"current\"><span class=\"meta\">Packaging</span><br/>Download</a>";
}else{
	print "<a href=\"download.php\"><span class=\"meta\">Packaging</span><br/>Download</a>";
}
echo "</li>";

echo "<li>";
if(strcmp($page,"developer.php") == 0){
	print "<a href=\"developer.php\" class=\"current\"><span class=\"meta\">Guides</span><br/>Developer</a>";
}else{
	print "<a href=\"developer.php\"><span class=\"meta\">Guides</span><br/>Developer</a>";
}
echo "</li>";

echo "<li>";
if(strcmp($page,"showcase.php") == 0){
	print "<a href=\"showcase.php\" class=\"current\"><span class=\"meta\">Online</span><br/>ShowCase</a>";
}else{
	print "<a href=\"showcase.php\"><span class=\"meta\">Online</span><br/>ShowCase</a>";
}
echo "</li>";

echo "<li>";
if(strcmp($page,"index.php") == 0){
	print "<a href=\"index.php\" class=\"current\"><span class=\"meta\">Homepage</span><br/>Home</a>";
}else{
	print "<a href=\"index.php\"><span class=\"meta\">Homepage</span><br/>Home</a>";
}
echo "</li>";

echo "</ul>";
?>
