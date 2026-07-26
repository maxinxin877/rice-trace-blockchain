<template>
  <div class="map-viewer">
    <div ref="mapContainer" class="map-container" :style="{ height: height }"></div>
    <div class="map-info" v-if="centerText">
      <span>{{ centerText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import L from 'leaflet'
import type { GisPoint } from '@/types/field'

const props = defineProps<{
  points: GisPoint[]
  height?: string
  editable?: boolean
}>()

const emit = defineEmits<{
  'update:points': [points: GisPoint[]]
}>()

const mapContainer = ref<HTMLElement>()
const centerText = ref('')
let map: L.Map | null = null
let polygon: L.Polygon | null = null

const height = props.height || '300px'

function getCenter(points: GisPoint[]): [number, number] {
  if (points.length === 0) return [45.8, 126.5] // 默认哈尔滨
  const lats = points.map((p) => p.lat)
  const lngs = points.map((p) => p.lng)
  return [
    (Math.min(...lats) + Math.max(...lats)) / 2,
    (Math.min(...lngs) + Math.max(...lngs)) / 2,
  ]
}

function initMap() {
  if (!mapContainer.value) return

  if (!map) {
    map = L.map(mapContainer.value).setView(getCenter(props.points), 13)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)
  }

  drawPolygon()

  if (props.editable) {
    map.on('click', (e: L.LeafletMouseEvent) => {
      const newPoints = [...props.points, { lat: e.latlng.lat, lng: e.latlng.lng }]
      emit('update:points', newPoints)
      drawPolygon()
    })
  }
}

function drawPolygon() {
  if (!map) return

  if (polygon) {
    map.removeLayer(polygon)
  }

  if (props.points.length >= 3) {
    const latlngs = props.points.map((p) => [p.lat, p.lng] as [number, number])
    polygon = L.polygon(latlngs, {
      color: '#409EFF',
      fillColor: '#409EFF',
      fillOpacity: 0.2,
      weight: 2,
    }).addTo(map)

    map.fitBounds(polygon.getBounds())
  }

  // 添加标记点
  props.points.forEach((p, idx) => {
    L.circleMarker([p.lat, p.lng], {
      radius: 6,
      color: '#409EFF',
      fillColor: '#fff',
      fillOpacity: 1,
      weight: 2,
    })
      .bindPopup(`坐标点 ${idx + 1}: (${p.lat.toFixed(4)}, ${p.lng.toFixed(4)})`)
      .addTo(map!)
  })

  const center = getCenter(props.points)
  centerText.value = `中心坐标: (${center[0].toFixed(4)}, ${center[1].toFixed(4)}) | 坐标点数: ${props.points.length}`
}

onMounted(() => {
  nextTick(initMap)
})

watch(() => props.points, () => {
  nextTick(() => {
    if (map) {
      drawPolygon()
    }
  })
}, { deep: true })
</script>

<style scoped>
.map-viewer {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.map-container {
  width: 100%;
  z-index: 1;
}

.map-info {
  padding: 8px 12px;
  background: #fafafa;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #e4e7ed;
}
</style>
