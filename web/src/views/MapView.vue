<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { loadAmap } from "../utils/amapLoader";

const mapRef = ref(null);
const panelRef = ref(null);
const loading = ref(false);
const sites = ref([]);
const selectedSubject = ref(null);
const selectedSiteId = ref(null);
const routeSummary = ref("");
const sdkError = ref("");
const sdkErrorDescription = ref("");
const geocodeLoading = ref(false);

const subjectOptions = [
  { label: "科目一", value: "科目一" },
  { label: "科目二", value: "科目二" },
  { label: "科目三", value: "科目三" },
  { label: "科目四", value: "科目四" }
];

const filteredSites = computed(() => {
  if (!selectedSubject.value) return sites.value;
  return sites.value.filter(s => s.subjectType && s.subjectType.includes(selectedSubject.value));
});

let AMapSdk = null;
let mapInstance = null;
let drivingInstance = null;
let currentPosition = null;
let siteMarkers = [];
let currentMarker = null;
let geocoderInstance = null;

// 使用 localStorage 持久化缓存，避免刷新页面后重复解析
const GEOCODE_CACHE_KEY = "amap_geocode_cache";
const getGeocodeCache = () => {
  try {
    return JSON.parse(localStorage.getItem(GEOCODE_CACHE_KEY)) || {};
  } catch (e) {
    return {};
  }
};
const saveGeocodeCache = (cacheObj) => {
  localStorage.setItem(GEOCODE_CACHE_KEY, JSON.stringify(cacheObj));
};
let geocodeCache = getGeocodeCache();

const tableColumns = [
  { title: "考场", dataIndex: "name", key: "name" },
  { title: "科目类型", dataIndex: "subjectType", key: "subjectType" },
  { title: "可考车型", dataIndex: "vehicleTypes", key: "vehicleTypes" },
  { title: "地址", dataIndex: "address", key: "address" },
  { title: "定位状态", dataIndex: "geoStatus", key: "geoStatus" },
  { title: "操作", key: "action", width: 120 }
];

const tableData = computed(() =>
  filteredSites.value.map((s) => ({
    ...s,
    geoStatus: s._lng && s._lat ? "已定位" : "待解析"
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
  geocoderInstance = new AMapSdk.Geocoder();

  drivingInstance = new AMapSdk.Driving({
    map: mapInstance,
    panel: panelRef.value,
    policy: AMapSdk.DrivingPolicy.LEAST_TIME
  });
};

const geocodeAddress = (address) =>
  new Promise((resolve) => {
    if (!address) {
      resolve(null);
      return;
    }
    if (geocodeCache[address]) {
      resolve(geocodeCache[address]);
      return;
    }
    geocoderInstance.getLocation(address, (status, result) => {
      if (status !== "complete" || !result?.geocodes?.length) {
        resolve(null);
        return;
      }
      const location = result.geocodes[0].location;
      const point = [location.lng, location.lat];
      geocodeCache[address] = point;
      saveGeocodeCache(geocodeCache);
      resolve(point);
    });
  });

const enrichSitesWithGeo = async () => {
  geocodeLoading.value = true;
  const updatedSites = [...sites.value];

  const batchSize = 10;
  for (let i = 0; i < updatedSites.length; i += batchSize) {
    const batch = updatedSites.slice(i, i + batchSize);
    
    await Promise.all(
      batch.map(async (site) => {
        const point = await geocodeAddress(site.address);
        site._lng = point?.[0] ?? null;
        site._lat = point?.[1] ?? null;
      })
    );
    
    sites.value = [...updatedSites];
    renderSiteMarkers(false); // 渲染时不自动缩放视图，避免打断用户操作
    
    if (i + batchSize < updatedSites.length) {
      // 增加延时避免触发高德 API 并发限制
      await new Promise(resolve => setTimeout(resolve, 300));
    }
  }

  // 所有地址解析完成后，统一调整一次视野
  if (siteMarkers.length > 0) {
    mapInstance.setFitView(siteMarkers);
  } else {
    message.warning("考场地址均无法解析定位，请检查地址信息");
  }

  geocodeLoading.value = false;
};

const renderSiteMarkers = (fitView = true) => {
  siteMarkers.forEach((m) => m.setMap(null));
  siteMarkers = [];

  filteredSites.value.filter((site) => site._lng && site._lat).forEach((site) => {
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

  if (fitView && siteMarkers.length > 0) {
    mapInstance.setFitView(siteMarkers);
  }
};

watch(selectedSubject, () => {
  selectedSiteId.value = null;
  renderSiteMarkers(true);
});

const findNearestSite = () => {
  if (!AMapSdk || !currentPosition) {
    return message.warning("当前定位未就绪，请先等待定位或手动重定位");
  }
  
  let nearest = null;
  let minDistance = Infinity;
  const p1 = new AMapSdk.LngLat(currentPosition[0], currentPosition[1]);

  filteredSites.value.forEach(site => {
    if (site._lng && site._lat) {
      const p2 = new AMapSdk.LngLat(site._lng, site._lat);
      const distance = p1.distance(p2);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = site;
      }
    }
  });

  if (nearest) {
    selectedSiteId.value = nearest.id;
    planRoute();
    message.success(`已自动选择最近考场：${nearest.name}，距离约 ${(minDistance / 1000).toFixed(1)} km`);
  } else {
    message.warning("当前没有可用的考场坐标，无法计算距离");
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
  if (!site._lng || !site._lat) {
    return message.error("该考场地址暂未解析成功，无法规划路线");
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
  drivingInstance.search(currentPosition, [site._lng, site._lat], (status, result) => {
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
    loading.value = false; // 解除加载遮罩，让用户先看到地图和列表
    
    // 异步定位用户当前位置
    locateCurrentPosition();
    
    // 异步解析考场地址，不阻塞页面加载
    enrichSitesWithGeo();
  } catch (e) {
    if (e.message && e.message.includes("status code")) {
      sdkError.value = "无法连接到后端服务";
      sdkErrorDescription.value = "请检查 Spring Boot 后端项目是否已在 IDE 中成功启动运行。";
    } else {
      sdkError.value = e.message || "地图初始化失败";
      sdkErrorDescription.value = "请检查 web/.env.local 中是否已正确配置 VITE_AMAP_KEY 后重启前端。";
    }
    message.error(sdkError.value);
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
        :description="sdkErrorDescription"
        style="margin-bottom: 12px;"
      />
      <a-row :gutter="[12,12]" style="margin-bottom: 12px;">
        <a-col :xs="24" :lg="4">
          <a-select
            v-model:value="selectedSubject"
            style="width:100%;"
            placeholder="筛选科目"
            allow-clear
            :options="subjectOptions"
          />
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-select
            v-model:value="selectedSiteId"
            style="width:100%;"
            placeholder="请选择考场"
            :options="filteredSites.map(x => ({ label: `${x.name}（${x.address || '未填写地址'}）`, value: x.id }))"
          />
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-space>
            <a-button @click="findNearestSite" type="dashed">找最近考场</a-button>
            <a-button @click="locateCurrentPosition">重新定位起点</a-button>
            <a-button type="primary" @click="planRoute">规划驾车路线</a-button>
            <a-tag v-if="routeSummary" color="blue">{{ routeSummary }}</a-tag>
            <a-tag v-if="geocodeLoading" color="processing">地址解析中</a-tag>
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
          <template v-else-if="column.key === 'geoStatus'">
            {{ record.geoStatus }}
          </template>
        </template>
      </a-table>
    </a-card>
  </a-space>
</template>
