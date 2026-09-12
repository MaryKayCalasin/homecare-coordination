import L from 'leaflet'

export function pinIcon(color) {
  return L.divIcon({
    className: '',
    html: `<span style="
      display:block;
      width:16px;
      height:16px;
      border-radius:9999px;
      background:${color};
      border:2px solid white;
      box-shadow:0 0 0 1px rgba(0,0,0,0.25);
    "></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    popupAnchor: [0, -8],
  })
}
