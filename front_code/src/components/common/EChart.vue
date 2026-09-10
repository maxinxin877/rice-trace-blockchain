<template><div ref=chartRef class=echart /></template>

<script setup lang=ts>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import type { EChartsOption } from 'echarts'
import { BarChart, GaugeChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, GaugeChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ option: EChartsOption }>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

function render() {
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption(props.option, true)
}

watch(() => props.option, render, { deep: true })
onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  if (chartRef.value) observer.observe(chartRef.value)
})
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>

<style scoped>.echart{width:100%;height:100%;min-height:280px}</style>
