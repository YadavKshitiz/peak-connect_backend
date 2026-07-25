import http.server
import socketserver
import json
import urllib.parse

PORT = 8081

class MockHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        qs = urllib.parse.parse_qs(parsed.query)
        
        # Check API key
        if qs.get('appid', [''])[0] == 'invalid_key':
            self.send_response(401)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(b'{"cod":401, "message": "Invalid API key."}')
            return
            
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        
        # Return HIGH risk simulation (Wind 20 m/s)
        resp = {
            "weather": [{"main": "Clear", "description": "clear sky"}],
            "main": {"temp": 20.0, "humidity": 50},
            "wind": {"speed": 20.0}
        }
        self.wfile.write(json.dumps(resp).encode('utf-8'))

with socketserver.TCPServer(("", PORT), MockHandler) as httpd:
    print("serving at port", PORT)
    httpd.serve_forever()
