"""Build the app's offline page from the Claude artifact source.

web/claims.html is an exact copy of the artifact. This script wraps it in a
full HTML document, points it at the bundled PDF libraries and fonts, and adds
web/bridge.js so the page saves data on the phone.

Usage: python3 web/make_app.py <assets-dir>
Run after the PDF libraries have been downloaded into <assets-dir>.
"""
import os
import re
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1]

FONT_URL = ("https://fonts.googleapis.com/css2?family=Archivo:wght@500;700;800"
            "&family=Public+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@500;700&display=swap")
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"


def bundle_fonts():
    """Download Google Fonts into assets/fonts so the app needs no internet."""
    try:
        req = urllib.request.Request(FONT_URL, headers={"User-Agent": UA})
        css = urllib.request.urlopen(req, timeout=30).read().decode()
        os.makedirs(os.path.join(OUT, "fonts"), exist_ok=True)
        urls = sorted(set(re.findall(r"url\((https://[^)]+)\)", css)))
        for i, u in enumerate(urls):
            name = "f%02d.woff2" % i
            data = urllib.request.urlopen(urllib.request.Request(u, headers={"User-Agent": UA}), timeout=30).read()
            with open(os.path.join(OUT, "fonts", name), "wb") as fh:
                fh.write(data)
            css = css.replace(u, name)
        with open(os.path.join(OUT, "fonts", "fonts.css"), "w") as fh:
            fh.write(css)
        print("fonts bundled:", len(urls))
        return True
    except Exception as e:  # app still works with system fonts
        print("fonts not bundled:", e)
        return False


def main():
    src = open(os.path.join(HERE, "claims.html"), encoding="utf-8").read()

    def swap(old, new):
        nonlocal src
        if src.count(old) != 1:
            raise SystemExit("expected exactly one: " + old)
        src = src.replace(old, new)

    swap('<script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>',
         '<script src="bridge.js"></script>\n<script src="jspdf.umd.min.js"></script>')
    swap('<script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf-autotable/3.8.2/jspdf.plugin.autotable.min.js"></script>',
         '<script src="jspdf.plugin.autotable.min.js"></script>')
    fonts = bundle_fonts()
    swap('<link rel="preconnect" href="https://fonts.googleapis.com">\n', '')
    font_link = re.search(r'<link rel="stylesheet" href="https://fonts\.googleapis\.com/[^"]+">', src).group(0)
    swap(font_link, '<link rel="stylesheet" href="fonts/fonts.css">' if fonts else '')

    for lib in ("jspdf.umd.min.js", "jspdf.plugin.autotable.min.js"):
        if not os.path.exists(os.path.join(OUT, lib)):
            raise SystemExit("missing library: " + lib)

    page = ('<!doctype html><html lang="ur"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
            '<style>:root{color-scheme:light}body{margin:0;font:14px system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}'
            'img{max-width:100%}[hidden]{display:none!important}</style></head><body>'
            + src + '</body></html>')
    with open(os.path.join(OUT, "index.html"), "w", encoding="utf-8") as fh:
        fh.write(page)
    with open(os.path.join(HERE, "bridge.js"), encoding="utf-8") as fh:
        bridge = fh.read()
    with open(os.path.join(OUT, "bridge.js"), "w", encoding="utf-8") as fh:
        fh.write(bridge)
    print("index.html written:", len(page), "bytes")


main()
