import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import Dashboard from '../views/rice/dashboard/Dashboard.vue'
import FieldList from '../views/rice/fields/FieldList.vue'
import FieldDetail from '../views/rice/fields/FieldDetail.vue'
import PlantingBatchList from '../views/rice/planting/PlantingBatchList.vue'
import PlantingBatchDetail from '../views/rice/planting/PlantingBatchDetail.vue'
import StorageReceiptList from '../views/rice/storage/StorageReceiptList.vue'
import StorageReceiptDetail from '../views/rice/storage/StorageReceiptDetail.vue'
import MillingBatchList from '../views/rice/milling/MillingBatchList.vue'
import MillingBatchDetail from '../views/rice/milling/MillingBatchDetail.vue'
import ProductBatchList from '../views/rice/product/ProductBatchList.vue'
import TraceCodeList from '../views/rice/trace/TraceCodeList.vue'
import ChannelWarnings from '../views/rice/trace/ChannelWarnings.vue'
import YieldBalance from '../views/rice/regulation/YieldBalance.vue'
import RiskWarnings from '../views/rice/regulation/RiskWarnings.vue'
import AuditLogs from '../views/rice/regulation/AuditLogs.vue'
import MiniTraceIndex from '../views/mini/TraceIndex.vue'
import MiniVerify from '../views/mini/VerifyResult.vue'
import MiniCertificates from '../views/mini/Certificates.vue'
import MiniChainProof from '../views/mini/ChainProof.vue'

const routes = [
  {
    path: '/rice',
    component: MainLayout,
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'fields', component: FieldList },
      { path: 'fields/detail/:fieldId', component: FieldDetail },
      { path: 'planting-batches', component: PlantingBatchList },
      { path: 'planting-batches/detail/:plantingBatchId', component: PlantingBatchDetail },
      { path: 'storage-receipts', component: StorageReceiptList },
      { path: 'storage-receipts/detail/:storageReceiptId', component: StorageReceiptDetail },
      { path: 'milling-batches', component: MillingBatchList },
      { path: 'milling-batches/detail/:millingBatchId', component: MillingBatchDetail },
      { path: 'product-batches', component: ProductBatchList },
      { path: 'trace-codes', component: TraceCodeList },
      { path: 'channel-warnings', component: ChannelWarnings },
      { path: 'regulation/yield-balance', component: YieldBalance },
      { path: 'regulation/risk-warnings', component: RiskWarnings },
      { path: 'regulation/audit-logs', component: AuditLogs }
    ]
  },
  { path: '/pages/rice/trace/index', component: MiniTraceIndex },
  { path: '/pages/rice/trace/verify', component: MiniVerify },
  { path: '/pages/rice/trace/certificates', component: MiniCertificates },
  { path: '/pages/rice/trace/chain-proof', component: MiniChainProof },
  { path: '/', redirect: '/rice/dashboard' }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
