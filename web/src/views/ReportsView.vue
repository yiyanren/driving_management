<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import VChart from "vue-echarts";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { BarChart, FunnelChart, LineChart } from "echarts/charts";
import { TooltipComponent, LegendComponent, GridComponent } from "echarts/components";
import { api } from "../api";

use([CanvasRenderer, BarChart, FunnelChart, LineChart, TooltipComponent, LegendComponent, GridComponent]);

const query = reactive({ from: "", to: "" });
const overview = ref({});
const funnel = ref({});

const toPercentNumber = (v) => Number.parseFloat(String(v || 0).replace("%", "")) || 0;

const overviewOption = computed(() => {
  const leadTotal = Number(overview.value.leadTotal || 0);
  const studentTotal = Number(overview.value.studentTotal || 0);
  const examTotal = Number(overview.value.examApplicationTotal || 0);
  const passRate = toPercentNumber(funnel.value.examPassRate);
  return {
    tooltip: { trigger: "axis" },
    legend: { top: 0 },
    grid: { left: 40, right: 56, top: 42, bottom: 30 },
    xAxis: { type: "category", data: ["意向客户", "学员", "报考申请"] },
    yAxis: [
      { type: "value", name: "数量", minInterval: 1 },
      { type: "value", name: "通过率(%)", min: 0, max: 100 }
    ],
    series: [
      {
        name: "数量",
        type: "bar",
        yAxisIndex: 0,
        data: [leadTotal, studentTotal, examTotal],
        itemStyle: { color: "#69b1ff" },
        barWidth: 36
      },
      {
        name: "考试通过率",
        type: "line",
        yAxisIndex: 1,
        data: [null, null, passRate],
        itemStyle: { color: "#52c41a" },
        smooth: true
      }
    ]
  };
});

const funnelOption = computed(() => {
  const leadTotal = Number(overview.value.leadTotal || 0);
  const studentTotal = Number(overview.value.studentTotal || 0);
  const examTotal = Number(overview.value.examApplicationTotal || 0);
  return {
    tooltip: { trigger: "item", formatter: "{b}: {c}" },
    legend: { top: 0 },
    series: [
      {
        name: "业务漏斗",
        type: "funnel",
        top: 30,
        bottom: 20,
        left: "10%",
        width: "80%",
        minSize: "20%",
        maxSize: "100%",
        sort: "descending",
        gap: 4,
        label: { show: true, position: "inside", formatter: "{b}\n{c}" },
        data: [
          { name: "意向客户", value: leadTotal },
          { name: "学员", value: studentTotal },
          { name: "报考申请", value: examTotal }
        ]
      }
    ]
  };
});

const loadData = async () => {
  const [a, b] = await Promise.all([api.reportOverview(query), api.reportFunnel(query)]);
  if (a.data.success) overview.value = a.data.data;
  if (b.data.success) funnel.value = b.data.data;
};

const exportCsv = async () => {
  const res = await api.exportOverviewCsv(query);
  if (!res.data) return message.error("导出失败");
  const blob = new Blob([res.data], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "overview_report.csv";
  a.click();
  URL.revokeObjectURL(url);
  message.success("导出成功");
};

onMounted(loadData);
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="时间筛选">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="8"><a-date-picker style="width:100%;" @change="(_,s)=>query.from=s" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8"><a-date-picker style="width:100%;" @change="(_,s)=>query.to=s" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-space>
            <a-button type="primary" @click="loadData">查询报表</a-button>
            <a-button @click="exportCsv">导出CSV</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>
    <a-card title="总览报表（数量 + 通过率）">
      <v-chart class="biz-chart" :option="overviewOption" autoresize />
    </a-card>
    <a-card title="漏斗报表">
      <v-chart class="biz-chart" :option="funnelOption" autoresize />
    </a-card>
  </a-space>
</template>
