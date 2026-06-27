defmodule Hubble.Api.LandingPage do
  @moduledoc "Static HTML for the GET / landing page."

  def html do
    """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Hubble — Meet people you've actually been near</title>
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0a0a14; }
        #map { position: fixed; inset: 0; z-index: 0; }
        #panel {
          position: fixed; top: 0; left: 0; z-index: 1000;
          padding: 36px 40px 28px;
          background: rgba(8, 8, 18, 0.84);
          backdrop-filter: blur(16px);
          -webkit-backdrop-filter: blur(16px);
          border-right: 1px solid rgba(255,255,255,0.07);
          border-bottom: 1px solid rgba(255,255,255,0.07);
          border-radius: 0 0 20px 0;
          color: #fff;
          width: 300px;
        }
        h1 { font-size: 30px; font-weight: 800; letter-spacing: -1px; }
        h1 span { color: #60a5fa; }
        .tagline {
          margin-top: 8px;
          font-size: 13px;
          color: #64748b;
          line-height: 1.55;
        }
        .divider {
          margin: 20px 0;
          border: none;
          border-top: 1px solid rgba(255,255,255,0.07);
        }
        .stores { display: flex; flex-direction: column; gap: 8px; }
        .store-btn {
          display: flex; align-items: center; gap: 12px;
          padding: 10px 14px;
          border-radius: 10px;
          border: 1px solid rgba(255,255,255,0.10);
          background: rgba(255,255,255,0.04);
          color: #94a3b8;
          text-decoration: none;
          cursor: default;
          opacity: 0.55;
          user-select: none;
        }
        .store-btn svg { flex-shrink: 0; }
        .store-label { display: block; font-size: 10px; color: #475569; line-height: 1; margin-bottom: 2px; }
        .store-name  { display: block; font-size: 13px; font-weight: 600; }
        #status {
          margin-top: 18px;
          font-size: 12px;
          color: #334155;
        }
        #status strong { color: #3b82f6; }
        .leaflet-container { background: #0d1117; }
      </style>
    </head>
    <body>
      <div id="map"></div>

      <div id="panel">
        <h1>Hub<span>ble</span></h1>
        <p class="tagline">
          Meet people you've actually been near.<br>
          Every connection starts with Bluetooth — in person.
        </p>
        <hr class="divider">
        <div class="stores">
          <a class="store-btn" title="Coming soon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="#4ade80">
              <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-1.303a1 1 0 0 1 0 1.192l-1.8 1.041L13.793 12l2.103-2.637 1.802 1.041zM5.864 2.658l10.937 6.333-2.302 2.302L5.864 2.658z"/>
            </svg>
            <div>
              <span class="store-label">Coming soon on</span>
              <span class="store-name">Google Play</span>
            </div>
          </a>
          <a class="store-btn" title="Coming soon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="#e2e8f0">
              <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
            </svg>
            <div>
              <span class="store-label">Coming soon on</span>
              <span class="store-name">App Store</span>
            </div>
          </a>
        </div>
        <div id="status">Loading activity&hellip;</div>
      </div>

      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
      <script>
        const map = L.map('map', { zoomControl: false }).setView([30, 10], 2);
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          attribution: '&copy; <a href="https://openstreetmap.org">OpenStreetMap</a> &copy; <a href="https://carto.com">CARTO</a>',
          subdomains: 'abcd',
          maxZoom: 19
        }).addTo(map);
        L.control.zoom({ position: 'bottomright' }).addTo(map);

        function decodeGeohash(hash) {
          const b32 = '0123456789bcdefghjkmnpqrstuvwxyz';
          let latLo = -90, latHi = 90, lonLo = -180, lonHi = 180, even = true;
          for (const ch of hash) {
            let bits = b32.indexOf(ch);
            for (let i = 4; i >= 0; i--) {
              const bit = (bits >> i) & 1;
              if (even) { const m = (lonLo + lonHi) / 2; if (bit) lonLo = m; else lonHi = m; }
              else       { const m = (latLo + latHi) / 2; if (bit) latLo = m; else latHi = m; }
              even = !even;
            }
          }
          return [(latLo + latHi) / 2, (lonLo + lonHi) / 2];
        }

        const circles = [];
        async function refresh() {
          try {
            const cells = await fetch('/heatmap').then(r => r.json());
            circles.forEach(c => c.remove());
            circles.length = 0;
            cells.forEach(({ cell, count }) => {
              const [lat, lon] = decodeGeohash(cell);
              const opacity = Math.min(0.15 + count * 0.07, 0.65);
              const c = L.circle([lat, lon], {
                radius: 1200,
                color: '#3b82f6',
                fillColor: '#60a5fa',
                fillOpacity: opacity,
                weight: 0,
              }).addTo(map);
              c.bindTooltip(`${count} ${count === 1 ? 'person' : 'people'} nearby`, { sticky: true });
              circles.push(c);
            });
            const total = cells.reduce((s, c) => s + c.count, 0);
            const el = document.getElementById('status');
            el.innerHTML = total > 0
              ? `<strong>${total}</strong> active ${total === 1 ? 'user' : 'users'} right now`
              : 'No activity right now — be the first!';
          } catch {
            document.getElementById('status').textContent = 'Could not load activity.';
          }
        }

        refresh();
        setInterval(refresh, 30_000);
      </script>
    </body>
    </html>
    """
  end
end
