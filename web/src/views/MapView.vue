<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { loadAmap } from "../utils/amapLoader";

const mapRef = ref(null);
const panelRef = ref(null);
const loading = ref(false);
const sites = ref([]);
const selectedSiteId = ref(null);
const routeSummary = ref("");
const sdkError = ref("");

let AMapSdk = null;
let mapInstance = null;
let drivingInstance = null;
let currentPosition = null;
let siteMarkers = [];
let currentMarker = null;

const tableColumns = [
  { title: "考场", dataIndex: "name", key: "name" },
  { title: "地址", dataIndex: "address", key: "address" },
  { title: "经纬度", key: "geo" },
  { title: "路线推荐", dataIndex: "routeGuide", key: "routeGuide" },
  { title: "操作", key: "action", width: 120 }
];

const validSites = computed(() =>
  sites.value
    .map((s) => ({ ...s, _lng: Number(s.longitude), _lat: Number(s.latitude) }))
    .filter((s) => Number.isFinite(s._lng) && Number.isFinite(s._lat))
);

const tableData = computed(() =>
  sites.value.map((s) => ({
    ...s,
    geo: s.longitude && s.latitude ? `${s.longitude}, ${s.latitude}` : "-"
  }))
);

const loadSites = async () => {
  const res = await api.listExamSites({ page: 0, size: 200 });
  if (!res.data.success) {
    message.error(res.data.message || "考场数据加载失败");
    return;
  }
  sites.value = res.data.data.content || [];
};

const initMap = async () => {
  AMapSdk = await loadAmap();
  await nextTick();
  if (!mapRef.value) {
    throw new Error("地图容器未准备完成，请刷新页面重试");
  }
  mapInstance = new AMapSdk.Map(mapRef.value, {
    zoom: 11,
    center: [116.397428, 39.90923]
  });

  mapInstance.addControl(new AMapSdk.Scale());
  mapInstance.addControl(new AMapSdk.ToolBar());

  drivingInstance = new AMapSdk.Driving({
    map: mapInstance,
    panel: panelRef.value,
    policy: AMapSdk.DrivingPolicy.LEAST_TIME
  });
};

const renderSiteMarkers = () => {
  siteMarkers.forEach((m) => m.setMap(null));
  siteMarkers = [];

  validSites.value.forEach((site) => {
    const marker = new AMapSdk.Marker({
      position: [site._lng, site._lat],
      title: site.name
    });
    marker.on("click", () => {
      selectedSiteId.value = site.id;
      planRoute();
    });
    marker.setMap(mapInstance);
    siteMarkers.push(marker);
  });

  if (siteMarkers.length) {
    mapInstance.setFitView(siteMarkers);
  }
};

const locateCurrentPosition = () => {
  if (!AMapSdk || !mapInstance) return;
  const geolocation = new AMapSdk.Geolocation({
    enableHighAccuracy: true,
    timeout: 8000
  });
  geolocation.getCurrentPosition((status, result) => {
    if (status === "complete" && result?.position) {
      currentPosition = [result.position.lng, result.position.lat];
      if (currentMarker) {
        currentMarker.setMap(null);
      }
      currentMarker = new AMapSdk.Marker({
        position: currentPosition,
        title: "当前位置"
      });
      currentMarker.setMap(mapInstance);
      mapInstance.setCenter(currentPosition);
      return;
    }
    currentPosition = mapInstance.getCenter()?.toArray?.() || [116.397428, 39.90923];
    message.warning("定位失败，已使用地图中心点作为起点");
  });
};

const selectedSite = computed(() => sites.value.find((x) => x.id === selectedSiteId.value) || null);

const planRoute = () => {
  const site = selectedSite.value;
  if (!site) {
    return message.warning("请先选择考场");
  }
  const lng = Number(site.longitude);
  const lat = Number(site.latitude);
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    return message.error("该考场未配置有效经纬度");
  }
  if (!drivingInstance) {
    return;
  }
  if (!currentPosition) {
    currentPosition = mapInstance.getCenter()?.toArray?.() || [116.397428, 39.90923];
  }

  routeSummary.value = "";
  panelRef.value.innerHTML = "";
  drivingInstance.clear();
  drivingInstance.search(currentPosition, [lng, lat], (status, result) => {
    if (status !== "complete" || !result?.routes?.length) {
      message.error("路线规划失败，请稍后重试");
      return;
    }
    const best = result.routes[0];
    const km = (best.distance / 1000).toFixed(1);
    const min = Math.ceil(best.time / 60);
    routeSummary.value = `推荐路线：约 ${km} 公里，预计 ${min} 分钟`;
  });
};

onMounted(async () => {
  loading.value = true;
  try {
    await loadSites();
    await initMap();
    renderSiteMarkers();
    locateCurrentPosition();
  } catch (e) {
    sdkError.value = e.message || "地图初始化失败";
    message.error(sdkError.value);
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy();
  }
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="考场地图与路线规划（高德）">
      <a-alert
        v-if="sdkError"
        type="error"
        show-icon
        :message="sdkError"
        description="请在 web/.env.local 中配置 VITE_AMAP_KEY 后重启前端。"
        style="margin-bottom: 12px;"
      />
      <a-row :gutter="[12,12]" style="margin-bottom: 12px;">
        <a-col :xs="24" :lg="10">
          <a-select
            v-model:value="selectedSiteId"
            style="width:100%;"
            placeholder="请选择考场"
            :options="validSites.map(x => ({ label: `${x.name}（${x.address}）`, value: x.id }))"
          />
        </a-col>
        <a-col :xs="24" :lg="14">
          <a-space>
            <a-button @click="locateCurrentPosition">重新定位起点</a-button>
            <a-button type="primary" @click="planRoute">规划驾车路线</a-button>
            <a-tag v-if="routeSummary" color="blue">{{ routeSummary }}</a-tag>
          </a-space>
        </a-col>
      </a-row>
      <a-spin :spinning="loading">
        <div class="map-wrap">
        <div ref="mapRef" class="amap-canvas" />
        <div ref="panelRef" class="route-panel" />
        </div>
      </a-spin>
    </a-card>

    <a-card title="考场数据">
      <a-table :columns="tableColumns" :data-source="tableData" row-key="id" :pagination="{ pageSize: 8 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="selectedSiteId = record.id; planRoute()">规划路线</a-button>
          </template>
          <template v-else-if="column.key === 'geo'">
            {{ record.geo }}
          </template>
        </template>
      </a-table>
    </a-card>
  </a-space>
</template>
