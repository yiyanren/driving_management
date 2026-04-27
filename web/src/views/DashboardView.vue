<script setup>
import { computed, onMounted, ref } from "vue";
import VChart from "vue-echarts";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { PieChart, BarChart } from "echarts/charts";
import { TooltipComponent, LegendComponent, GridComponent } from "echarts/components";
import { api } from "../api";

use([CanvasRenderer, PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent]);

const overview = ref({});
const funnel = ref({});

const statCards = computed(() => [
  { title: "意向客户总数", value: overview.value.leadTotal ?? 0, sub: "意向客户漏斗起点", color: "#1677ff" },
  { title: "学员总数", value: overview.value.studentTotal ?? 0, sub: "已成功转化", color: "#13c2c2" },
  { title: "报考总数", value: overview.value.examApplicationTotal ?? 0, sub: "累计提交报考", color: "#722ed1" },
  { title: "转化率", value: funnel.value.leadToStudentRate ?? "0.00%", sub: "意向客户转学员", color: "#52c41a" }
]);

const sourcePieOption = computed(() => {
  const stats = overview.value.leadSourceStats || {};
  const data = Object.entries(stats).map(([name, value]) => ({ name, value }));
  return {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["35%", "65%"],
        center: ["50%", "45%"],
        itemStyle: { borderRadius: 8, borderColor: "#fff", borderWidth: 2 },
        label: { formatter: "{b}: {d}%" },
        data
      }
    ]
  };
});

const ownerBarOption = computed(() => {
  const perf = overview.value.ownerPerformance || {};
  const names = Object.keys(perf);
  const intentCounts = names.map((n) => {
    const item = perf[n];
    if (item && typeof item === "object") {
      return Number(item.intentCount ?? item.leadCount ?? item.totalIntents ?? 0);
    }
    return Number(item || 0);
  });
  const convertRate = Number.parseFloat(String(funnel.value.leadToStudentRate || "0").replace("%", "")) || 0;
  const convertedCounts = names.map((n, idx) => {
    const item = perf[n];
    if (item && typeof item === "object") {
      return Number(item.convertCount ?? item.convertedCount ?? item.studentCount ?? 0);
    }
    return Math.round((intentCounts[idx] * convertRate) / 100);
  });
  return {
    tooltip: { trigger: "axis" },
    legend: { top: 0 },
    grid: { left: 40, right: 56, top: 42, bottom: 40 },
    xAxis: { type: "category", data: names, name: "工作人员" },
    yAxis: [
      { type: "value", name: "当月意向人数", minInterval: 1 },
      { type: "value", name: "当月转化人数", minInterval: 1 }
    ],
    series: [
      { name: "当月意向人数", type: "bar", data: intentCounts, yAxisIndex: 0, itemStyle: { color: "#69b1ff" } },
      { name: "当月转化人数", type: "bar", data: convertedCounts, yAxisIndex: 1, itemStyle: { color: "#ff9c6e" } }
    ]
  };
});

const loadData = async () => {
  const [a, b] = await Promise.all([api.reportOverview(), api.reportFunnel()]);
  if (a.data.success) overview.value = a.data.data;
  if (b.data.success) funnel.value = b.data.data;
};

onMounted(loadData);
</script>

<template>
  <a-space direction="vertical" size="large" style="display:flex;">
    <a-row :gutter="[16,16]">
      <a-col v-for="card in statCards" :key="card.title" :xs="24" :sm="12" :lg="6">
        <a-card class="metric-card">
          <div class="metric-title">{{ card.title }}</div>
          <div class="metric-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="metric-sub">{{ card.sub }}</div>
        </a-card>
      </a-col>
    </a-row>
    <a-card title="意向客户来源统计">
      <v-chart class="biz-chart" :option="sourcePieOption" autoresize />
    </a-card>
    <a-card title="意向客户转化业绩（双纵坐标）">
      <v-chart class="biz-chart" :option="ownerBarOption" autoresize />
    </a-card>
  </a-space>
</template>
