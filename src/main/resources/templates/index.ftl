<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
	<div class="container">
	    <div>
	        <div id="title">
	            <h1>Spring WebFlux Server Sent-Events</h1>
	        </div>
	        <div id="sse"></div>
	    </div>
	</div>
</body>

<script>
    let source;
    let path = '${street}';
    console.log(path)
    function loadScript () {
        source = new EventSource("http://localhost:8080/delivery/event/"+path);
    }

    function start() {
        source.onmessage = event => {
            let data = event.data;
            let div = document.getElementById('sse');
            div.innerHTML += "<div> Server Sent-Event Info : " + data + "</div>";
        };

        source.onerror = () => {
            this.close();
        };
        source.stop = () => {
            this.source.close();
        };

    }

    window.onload = () => {
        loadScript();
        start();
    };
</script>

</html>