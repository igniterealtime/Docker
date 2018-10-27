String.prototype.hashCode = function()
{
  var hash = 0, i, chr;
  if (this.length === 0) return hash;
  for (i = 0; i < this.length; i++) {
    chr   = this.charCodeAt(i);
    hash  = ((hash << 5) - hash) + chr;
    hash |= 0; // Convert to 32bit integer
  }
  return hash.toString();
};

window.addEventListener("unload", function()
{

});

window.addEventListener("load", function()
{
    function urlParam(name)
    {
        var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
        if (!results) { return undefined; }
        return unescape(results[1] || undefined);
    };

    var url = urlParam("url");
    var title = urlParam("title");
    var username = urlParam("username");
    var name = urlParam("name");
    var pos = url.lastIndexOf(".");


    if (url && username && name && pos > -1)
    {
        if (!title)
        {
            var pos = url.lastIndexOf("/");
            title = url.substring(pos + 1);
        }

        var type = url.substring(pos + 1);

        new DocsAPI.DocEditor("placeholder",
        {
            "document": {
                "fileType": type,
                "key": url.hashCode(),
                "title": title ? title : url,
                "url": url
            },
            "documentType": type == "xslx" || type == "xls" || type == "csv" ? "spreadsheet" : (type == "pptx" || type == "ppt" ? "presentation" : "text"),
            "editorConfig": {
                "user": {
                    "id": username,
                    "name": name
                }
            }
        });

        setTimeout(function()
        {
            document.title = title;

        }, 1000);
    }
});



