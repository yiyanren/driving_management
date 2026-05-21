<script setup>
import { ref, onMounted, nextTick } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { loadAmap } from "../utils/amapLoader";

const list = ref([]);
const total = ref(0);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const keyword = ref("");

const modalVisible = ref(false);
const modalTitle = ref("新增驾校");
const currentId = ref(null);
const formRef = ref();
const formData = ref({
  name: "",
  address: "",
  phone: ""
});

const mapContainerRef = ref(null);
const searchPanelRef = ref(null);
let mapInstance = null;
let markerInstance = null;
let placeSearchInstance = null;
const searchKeyword = ref("");

const rules = {
  name: [{ required: true, message: "请输入驾校名称" }]
};

const columns = [
  { title: "驾校名称", dataIndex: "name" },
  { title: "地址", dataIndex: "address" },
  { title: "联系电话", dataIndex: "phone" },
  { title: "操作", key: "action", width: 150 }
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await api.listDrivingSchools({
      page: page.value - 1,
      size: size.value,
      keyword: keyword.value
    });
    if (res.data.success) {
      list.value = res.data.data.content;
      total.value = res.data.data.totalElements;
    }
  } catch (e) {
    message.error("加载数据失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadData();
};

const initMap = async () => {
  try {
    const AMapSdk = await loadAmap();
    await nextTick();
    if (!mapContainerRef.value) return;

    mapInstance = new AMapSdk.Map(mapContainerRef.value, {
      zoom: 13,
      center: [116.397428, 39.90923] // 默认北京
    });

    markerInstance = new AMapSdk.Marker({
      map: mapInstance
    });

    AMapSdk.plugin(["AMap.PlaceSearch", "AMap.Geolocation", "AMap.Geocoder"], () => {
      placeSearchInstance = new AMapSdk.PlaceSearch({
        pageSize: 10,
        pageIndex: 1,
        map: mapInstance,
        panel: searchPanelRef.value,
        autoFitView: true,
        extensions: 'all',
        type: '驾校|汽车驾驶|培训|驾校报名'
      });

      // 监听搜索结果点击事件（列表项点击和地图Marker点击都会触发）
      placeSearchInstance.on("listElementClick", (e) => {
        fillFormWithPoi(e.data);
      });
      placeSearchInstance.on("markerClick", (e) => {
        fillFormWithPoi(e.data);
      });

      const fillFormWithPoi = (poi) => {
        formData.value.name = poi.name;
        formData.value.address = poi.address || `${poi.pname}${poi.cityname}${poi.adname}${poi.address}`;
        if (poi.tel) {
          formData.value.phone = poi.tel.split(";")[0]; // 有些可能返回多个电话
        }
        message.success(`已选中：${poi.name}，信息已自动填充`);
      };

      // 获取当前定位并设置为搜索范围的中心
      const geolocation = new AMapSdk.Geolocation({
        enableHighAccuracy: true,
        timeout: 5000,
        buttonPosition: 'RB',
        showButton: true
      });
      mapInstance.addControl(geolocation);
      
      geolocation.getCurrentPosition((status, result) => {
        if (status === "complete" && result.position) {
          const centerPoint = [result.position.lng, result.position.lat];
          mapInstance.setCenter(centerPoint);
          // 动态设置 PlaceSearch 的中心点城市，并重新初始化 PlaceSearch 以确保生效
          if (result.addressComponent && result.addressComponent.city) {
             placeSearchInstance.setCity(result.addressComponent.city);
          }
        } else {
          message.warning("浏览器定位失败，将使用默认城市进行搜索。");
        }
      });
    });
  } catch (e) {
    message.error("地图加载失败，请检查配置");
  }
};

const handleMapSearch = () => {
  if (!placeSearchInstance) {
    return message.warning("地图插件未加载完毕");
  }
  if (!searchKeyword.value) {
    return message.warning("请输入搜索关键字");
  }
  
  // 以当前地图中心点为圆心进行周边搜索，搜索半径为 50000 米（50公里）
  const centerPoint = mapInstance.getCenter();
  placeSearchInstance.searchNearBy(searchKeyword.value, centerPoint, 50000);
};

const handleAdd = () => {
  modalTitle.value = "新增驾校";
  currentId.value = null;
  formData.value = { name: "", address: "", phone: "" };
  searchKeyword.value = "";
  modalVisible.value = true;
  nextTick(() => {
    initMap();
  });
};

const handleEdit = (record) => {
  modalTitle.value = "编辑驾校";
  currentId.value = record.id;
  formData.value = { ...record };
  searchKeyword.value = record.name;
  modalVisible.value = true;
  nextTick(() => {
    initMap().then(() => {
      if (placeSearchInstance && searchKeyword.value) {
        placeSearchInstance.search(searchKeyword.value);
      }
    });
  });
};

const handleDelete = async (id) => {
  try {
    await api.deleteDrivingSchool(id);
    message.success("删除成功");
    loadData();
  } catch (e) {
    message.error("删除失败");
  }
};

const handleSave = () => {
  formRef.value.validate().then(async () => {
    try {
      if (currentId.value) {
        await api.updateDrivingSchool(currentId.value, formData.value);
        message.success("更新成功");
      } else {
        await api.createDrivingSchool(formData.value);
        message.success("新增成功");
      }
      modalVisible.value = false;
      loadData();
    } catch (e) {
      message.error(currentId.value ? "更新失败" : "新增失败");
    }
  });
};

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="view-container">
    <div class="toolbar">
      <a-space>
        <a-input v-model:value="keyword" placeholder="搜索名称/地址/电话" allowClear />
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button type="primary" @click="handleAdd">新增驾校</a-button>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="{
        current: page,
        pageSize: size,
        total: total,
        onChange: (p, s) => {
          page = p;
          size = s;
          loadData();
        }
      }"
      row-key="id"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleEdit(record)">编辑</a>
            <a-popconfirm title="确定删除吗？" @confirm="handleDelete(record.id)">
              <a class="text-danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:visible="modalVisible" :title="modalTitle" @ok="handleSave" width="1000px" :destroyOnClose="true">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical">
            <a-form-item label="驾校名称" name="name">
              <a-input v-model:value="formData.name" placeholder="可通过右侧地图搜索填充" />
            </a-form-item>
            <a-form-item label="地址" name="address">
              <a-input v-model:value="formData.address" placeholder="可通过右侧地图搜索填充" />
            </a-form-item>
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formData.phone" placeholder="可通过右侧地图搜索填充" />
            </a-form-item>
          </a-form>
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c;">
            提示：在右侧搜索结果列表中，或点击地图上的蓝色标记点，即可自动将信息填充至上方表单。
          </div>
        </a-col>
        <a-col :span="16">
          <div style="margin-bottom: 8px; display: flex; gap: 8px;">
            <a-input-search 
              v-model:value="searchKeyword" 
              placeholder="在地图周边搜索驾校" 
              enter-button 
              @search="handleMapSearch" 
            />
          </div>
          <div class="map-wrapper">
            <div class="search-panel" ref="searchPanelRef"></div>
            <div class="map-container" ref="mapContainerRef"></div>
          </div>
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<style scoped>
.view-container {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}
.toolbar {
  margin-bottom: 16px;
}
.text-danger {
  color: #ff4d4f;
}
.map-wrapper {
  display: flex;
  width: 100%;
  height: 380px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}
.search-panel {
  width: 240px;
  height: 100%;
  overflow-y: auto;
  border-right: 1px solid #d9d9d9;
}
.map-container {
  flex: 1;
  height: 100%;
}
</style>