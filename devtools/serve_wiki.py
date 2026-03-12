#!/usr/bin/env python3
"""
Simple HTTP server to serve the Modern Data Stack wiki page.

Usage:
    python devtools/serve_wiki.py [port]

Default port: 8000

Examples:
    python devtools/serve_wiki.py          # Serve on port 8000
    python devtools/serve_wiki.py 3000     # Serve on port 3000
"""

import http.server
import socketserver
import sys
import os
import webbrowser
from pathlib import Path


def serve_wiki(port=8000):
    """Start HTTP server to serve the wiki page."""

    # Change to project root directory
    project_dir = Path(__file__).resolve().parent.parent
    os.chdir(project_dir)

    # Create handler
    handler = http.server.SimpleHTTPRequestHandler

    # Handle default route to web/index.html
    class WikiHandler(handler):
        def end_headers(self):
            # Add CORS headers
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type")
            super().end_headers()

        def do_GET(self):
            if self.path == "/":
                self.path = "/web/index.html"
            return super().do_GET()

    try:
        with socketserver.TCPServer(("", port), WikiHandler) as httpd:
            url = f"http://localhost:{port}"
            print("=" * 70)
            print("🚀 Modern Data Stack Wiki Server")
            print("=" * 70)
            print(f"\n✓ Server started successfully!")
            print(f"\n📝 Wiki URL: {url}")
            print(f"\n💡 Access the wiki at:")
            print(f"   • {url}")
            print(f"   • http://127.0.0.1:{port}")
            print(f"   • http://<your-ip>:{port}")
            print("\n⌨️  Press Ctrl+C to stop the server")
            print("=" * 70)

            # Try to open browser
            try:
                print("\n🌐 Opening wiki in your default browser...")
                webbrowser.open(url)
            except Exception as e:
                print(f"\n⚠️  Could not open browser automatically: {e}")
                print(f"   Please open {url} manually in your browser")

            # Serve forever
            httpd.serve_forever()

    except OSError as e:
        if "Address already in use" in str(e):
            print(f"\n❌ Error: Port {port} is already in use!")
            print(f"   Try a different port: python devtools/serve_wiki.py <port>")
            sys.exit(1)
        else:
            raise
    except KeyboardInterrupt:
        print("\n\n👋 Shutting down server...")
        print("   Server stopped successfully!")
        sys.exit(0)


if __name__ == "__main__":
    # Get port from command line or use default
    port = 8000
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"❌ Error: Invalid port number '{sys.argv[1]}'")
            print("   Usage: python devtools/serve_wiki.py [port]")
            sys.exit(1)

    # Start server
    serve_wiki(port)
