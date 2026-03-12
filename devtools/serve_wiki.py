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
from urllib.parse import parse_qs, quote, unquote, urlparse
from pathlib import Path


def serve_wiki(port=8000):
    """Start HTTP server to serve the wiki page."""

    # Change to project root directory
    project_dir = Path(__file__).resolve().parent.parent
    os.chdir(project_dir)

    # Create handler
    handler = http.server.SimpleHTTPRequestHandler

    def build_markdown_viewer_html(doc_path):
        safe_title = (
            doc_path.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        )
        encoded_doc = quote(doc_path)
        return f"""<!doctype html>
<html lang=\"en\">
    <head>
        <meta charset=\"UTF-8\" />
        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
        <title>{safe_title} - Local Wiki Viewer</title>
        <style>
            body {{
                margin: 0;
                font-family: -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif;
                background: #0f172a;
                color: #e2e8f0;
            }}
            .topbar {{
                position: sticky;
                top: 0;
                z-index: 10;
                padding: 0.75rem 1rem;
                background: rgba(15, 23, 42, 0.96);
                border-bottom: 1px solid #334155;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }}
            .topbar a {{
                color: #7dd3fc;
                text-decoration: none;
            }}
            .container {{
                max-width: 1100px;
                margin: 1.5rem auto;
                padding: 0 1rem 2rem;
            }}
            .markdown-body {{
                line-height: 1.65;
            }}
            .markdown-body pre {{
                background: #111827;
                padding: 1rem;
                border-radius: 8px;
                overflow: auto;
            }}
            .markdown-body code {{
                font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
            }}
            .markdown-body table {{
                width: 100%;
                border-collapse: collapse;
            }}
            .markdown-body th,
            .markdown-body td {{
                border: 1px solid #334155;
                padding: 0.5rem;
            }}
            .error {{
                color: #fca5a5;
                background: #3f1d1d;
                border: 1px solid #7f1d1d;
                padding: 1rem;
                border-radius: 8px;
            }}
        </style>
        <script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>
        <script src=\"https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js\"></script>
    </head>
    <body>
        <div class=\"topbar\">
            <div>Local Doc: {safe_title}</div>
            <div>
                <a href=\"/web/index.html\">Wiki Home</a>
            </div>
        </div>
        <div class=\"container\">
            <article id=\"content\" class=\"markdown-body\"></article>
        </div>
        <script>
            mermaid.initialize({{
                startOnLoad: false,
                securityLevel: "loose",
                flowchart: {{ htmlLabels: true }}
            }});

            async function renderDoc() {{
                const contentEl = document.getElementById("content");
                try {{
                    const response = await fetch("/{encoded_doc}");
                    if (!response.ok) {{
                        throw new Error(`Unable to load markdown (status: ${{response.status}})`);
                    }}

                    const markdown = await response.text();
                    contentEl.innerHTML = marked.parse(markdown);

                    document.querySelectorAll("pre code.language-mermaid").forEach((code) => {{
                        const diagram = document.createElement("div");
                        diagram.className = "mermaid";
                        diagram.textContent = code.textContent;
                        code.parentElement.replaceWith(diagram);
                    }});

                    await mermaid.run({{ querySelector: ".mermaid" }});
                }} catch (error) {{
                    contentEl.innerHTML = `<div class=\"error\">${{error.message}}</div>`;
                    console.error(error);
                }}
            }}

            renderDoc();
        </script>
    </body>
</html>
"""

    def resolve_doc_path(raw_doc_path):
        candidate = unquote(raw_doc_path).lstrip("/")
        resolved = (project_dir / candidate).resolve()

        # Prevent path traversal outside project root.
        try:
            resolved.relative_to(project_dir.resolve())
        except ValueError:
            return None

        if not resolved.is_file() or resolved.suffix.lower() != ".md":
            return None

        return resolved.relative_to(project_dir).as_posix()

    # Handle default route to web/index.html
    class WikiHandler(handler):
        def end_headers(self):
            # Add CORS headers
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type")
            super().end_headers()

        def do_GET(self):
            parsed = urlparse(self.path)

            if parsed.path == "/":
                self.path = "/web/index.html"
                return super().do_GET()

            if parsed.path == "/wiki":
                query = parse_qs(parsed.query)
                requested_doc = query.get("doc", ["docs/ARCHITECTURE.MD"])[0]
                resolved_doc = resolve_doc_path(requested_doc)
                if not resolved_doc:
                    self.send_error(404, "Markdown document not found")
                    return

                html = build_markdown_viewer_html(resolved_doc).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(html)))
                self.end_headers()
                self.wfile.write(html)
                return

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
