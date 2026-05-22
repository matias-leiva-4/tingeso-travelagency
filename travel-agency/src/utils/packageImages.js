// Selección de imágenes para cada paquete turístico.
//
// Estrategia:
//   1. Buscamos primero un match por destino conocido (ver `destinationImages`).
//      Comparamos contra destino + nombre en minúsculas para que un paquete
//      "Tour Tokio" con destino "Japón" matchee tanto por "japón" como por "tokio".
//   2. Si ningún match, usamos un hash determinístico sobre las imágenes
//      genéricas de fallback. Esto garantiza que el mismo paquete siempre
//      muestre la misma foto (no parpadea entre renders) sin necesitar
//      almacenar la URL en la BD.
//
// Si alguna URL deja de cargar, basta con reemplazarla por otra de Unsplash
// (busca el destino en https://unsplash.com y copia el ID del photo-...).

const destinationImages = [
  {
    // Japón / Tokio / Kioto — calle de Tokio con neones
    keywords: ['japon', 'japón', 'japan', 'tokio', 'tokyo', 'kyoto', 'kioto', 'osaka'],
    url: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // París / Francia — Torre Eiffel al atardecer
    keywords: ['paris', 'parís', 'francia', 'france'],
    url: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Orlando / Disney / Florida — castillo y parque temático
    keywords: ['orlando', 'disney', 'florida', 'universal'],
    url: 'https://images.unsplash.com/photo-1597466599360-3b9775841aec?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Miami — Art Deco / playa con palmeras
    keywords: ['miami', 'south beach'],
    url: 'https://images.unsplash.com/photo-1535498730771-e735b998cd64?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Nueva York — skyline de Manhattan
    keywords: ['new york', 'newyork', 'nueva york', 'nyc', 'manhattan', 'brooklyn'],
    url: 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Torres del Paine / Patagonia — cumbres y lago
    keywords: ['patagonia', 'paine', 'torres del paine', 'puerto natales'],
    url: 'https://images.unsplash.com/photo-1531366936337-7c912a4589a7?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Londres / Reino Unido — Big Ben / Tower Bridge
    keywords: ['londres', 'london', 'reino unido', 'inglaterra', 'uk'],
    url: 'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Roma / Italia — Coliseo
    keywords: ['roma', 'rome', 'italia', 'italy', 'florencia', 'venecia'],
    url: 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Atacama / Chile — desierto y cielo estrellado
    keywords: ['atacama', 'san pedro', 'desierto'],
    url: 'https://images.unsplash.com/photo-1517900014419-ffa6c3eef7f2?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Bali / Indonesia — arrozales y templos
    keywords: ['bali', 'indonesia', 'ubud'],
    url: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Río de Janeiro / Brasil — Cristo Redentor / Pan de Azúcar
    keywords: ['rio', 'río', 'brasil', 'brazil', 'copacabana'],
    url: 'https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=80',
  },
  {
    // Cusco / Machu Picchu / Perú — ruinas incas
    keywords: ['cusco', 'cuzco', 'machu picchu', 'peru', 'perú'],
    url: 'https://images.unsplash.com/photo-1526392060635-9d6019884377?auto=format&fit=crop&w=1200&q=80',
  },
]

// Imágenes de fallback (las 6 originales) para destinos que no matcheen.
const fallbackImages = [
  'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1526772662000-3f88f10405ff?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80',
]

function hashText(text) {
  return [...text].reduce((hash, char) => hash + char.charCodeAt(0), 0)
}

export function getPackageImage(packageItem) {
  const destination = (packageItem?.destination ?? '').toLowerCase()
  const name = (packageItem?.name ?? '').toLowerCase()
  const haystack = `${destination} ${name}`

  // 1. Intentar matchear por destino conocido
  const match = destinationImages.find(({ keywords }) =>
    keywords.some((keyword) => haystack.includes(keyword)),
  )
  if (match) {
    return match.url
  }

  // 2. Fallback determinístico
  const key = `${destination}-${name}`
  const index = hashText(key) % fallbackImages.length
  return fallbackImages[index]
}
