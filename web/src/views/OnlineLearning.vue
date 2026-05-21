<script setup>
import { ref, onMounted, watch, computed, onUnmounted } from "vue";
import { message, Modal } from "ant-design-vue";
import { useRoute } from "vue-router";
import { api } from "../api";

const route = useRoute();
const mode = computed(() => route.params.mode || 'mock'); // order, mock, mistakes

const activeTab = ref("1"); // 1 for 科目一, 4 for 科目四
const loading = ref(false);
const questions = ref([]);
const currentIndex = ref(0);
const showAnswer = ref(false);
const isCorrect = ref(false);

const userAnswers = ref([]);
const questionStatus = ref([]);

// 模拟考试相关状态
const examStarted = ref(false);
const examFinished = ref(false);
const examScore = ref(0);
const timeLeft = ref(45 * 60);
let timer = null;

const answerMap = {
  "1": "A", "2": "B", "3": "C", "4": "D",
  "7": "AB", "8": "AC", "9": "AD", "10": "BC", "11": "BD",
  "12": "CD", "13": "ABC", "14": "ABD", "15": "ACD", "16": "BCD", "17": "ABCD"
};

const storageKeyProgress = computed(() => `quiz_progress_${activeTab.value}`);
const storageKeyMistakes = computed(() => `quiz_mistakes_${activeTab.value}`);

const formattedTime = computed(() => {
  const m = Math.floor(timeLeft.value / 60).toString().padStart(2, '0');
  const s = (timeLeft.value % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
});

const resetState = () => {
  if (timer) clearInterval(timer);
  examStarted.value = false;
  examFinished.value = false;
  examScore.value = 0;
  timeLeft.value = 45 * 60;
  userAnswers.value = [];
  questionStatus.value = [];
  showAnswer.value = false;
  currentIndex.value = 0;
};

const initArrays = () => {
  userAnswers.value = questions.value.map(() => []);
  questionStatus.value = questions.value.map(() => 'unanswered');
};

const loadQuestions = async () => {
  loading.value = true;
  resetState();
  
  if (mode.value === 'mistakes') {
    const stored = localStorage.getItem(storageKeyMistakes.value);
    if (stored) {
      questions.value = JSON.parse(stored);
      initArrays();
    } else {
      questions.value = [];
    }
    loading.value = false;
    return;
  }
  
  try {
    const testType = mode.value === 'order' ? 'order' : 'rand';
    const res = await api.getQuestions({ 
      subject: Number(activeTab.value), 
      model: "c1", 
      testType
    });
    
    if (res.data.success) {
      let fetchedQuestions = res.data.data || [];
      // 如果是模拟考试且是科目四，只需要 50 题
      if (mode.value === 'mock' && activeTab.value === '4' && fetchedQuestions.length > 50) {
        fetchedQuestions = fetchedQuestions.slice(0, 50);
      }
      
      questions.value = fetchedQuestions;
      initArrays();
      
      if (mode.value === 'order') {
        const savedIndex = localStorage.getItem(storageKeyProgress.value);
        if (savedIndex && Number(savedIndex) < questions.value.length) {
          currentIndex.value = Number(savedIndex);
          syncQuestionState();
        }
      }
    } else {
      message.error(res.data.message || "加载题库失败");
    }
  } catch (e) {
    message.error("请求失败，请检查后端或API Key配置");
  } finally {
    loading.value = false;
  }
};

const currentQ = computed(() => questions.value[currentIndex.value] || null);

const isTrueFalse = computed(() => {
  if (!currentQ.value) return false;
  return !currentQ.value.item3 && !currentQ.value.item4;
});

const correctText = computed(() => {
  if (!currentQ.value) return "";
  const chars = answerMap[currentQ.value.answer] || currentQ.value.answer;
  if (isTrueFalse.value) {
    return chars.includes('A') ? '正确' : '错误';
  }
  return chars;
});

const checkIsCorrect = (index) => {
  const q = questions.value[index];
  if (!q) return false;
  const correctChars = answerMap[q.answer] || q.answer;
  const userAns = userAnswers.value[index] || [];
  const userChars = userAns
    .sort()
    .map(v => ({"1":"A", "2":"B", "3":"C", "4":"D"})[v])
    .join('');
  return correctChars === userChars;
};

const syncQuestionState = () => {
  if (mode.value === 'mock') {
    showAnswer.value = false;
  } else {
    showAnswer.value = questionStatus.value[currentIndex.value] !== 'unanswered';
    isCorrect.value = questionStatus.value[currentIndex.value] === 'correct';
  }
};

const nextQ = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++;
    syncQuestionState();
  }
};

const prevQ = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    syncQuestionState();
  }
};

const goToQuestion = (i) => {
  if (mode.value === 'mock') return; // 模拟考试不能通过点击网格跳转
  currentIndex.value = i;
  syncQuestionState();
};

const saveToMistakes = (q) => {
  const stored = localStorage.getItem(storageKeyMistakes.value);
  let mistakes = stored ? JSON.parse(stored) : [];
  if (!mistakes.find(m => m.id === q.id)) {
    mistakes.push(q);
    localStorage.setItem(storageKeyMistakes.value, JSON.stringify(mistakes));
  }
};

const removeFromMistakes = () => {
  if (mode.value !== 'mistakes' || !currentQ.value) return;
  const stored = localStorage.getItem(storageKeyMistakes.value);
  if (!stored) return;
  let mistakes = JSON.parse(stored);
  mistakes = mistakes.filter(m => m.id !== currentQ.value.id);
  localStorage.setItem(storageKeyMistakes.value, JSON.stringify(mistakes));
  
  questions.value.splice(currentIndex.value, 1);
  userAnswers.value.splice(currentIndex.value, 1);
  questionStatus.value.splice(currentIndex.value, 1);
  
  if (currentIndex.value >= questions.value.length && currentIndex.value > 0) {
    currentIndex.value--;
  }
  syncQuestionState();
  message.success("已从错题本移除");
};

// 顺序练习/错题本：确认答案
const doSubmit = () => {
  const currentAns = userAnswers.value[currentIndex.value];
  if (!currentAns || currentAns.length === 0) {
    return message.warning("请先选择一个答案！");
  }
  isCorrect.value = checkIsCorrect(currentIndex.value);
  questionStatus.value[currentIndex.value] = isCorrect.value ? 'correct' : 'wrong';
  showAnswer.value = true;
  
  if (!isCorrect.value && mode.value === 'order') {
    saveToMistakes(currentQ.value);
  }
};

// 模拟考试：开始考试
const startExam = () => {
  examStarted.value = true;
  timeLeft.value = 45 * 60;
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--;
    } else {
      submitExam();
      message.warning("考试时间到，已自动交卷！");
    }
  }, 1000);
};

// 模拟考试：执行交卷
const submitExam = () => {
  if (timer) clearInterval(timer);
  let correctCount = 0;
  questions.value.forEach((q, i) => {
    if (checkIsCorrect(i)) {
      correctCount += 1;
    } else {
      saveToMistakes(q);
    }
  });
  
  // 科目一 100题，每题1分。科目四 50题，每题2分。
  const pointsPerQuestion = activeTab.value === '4' ? 2 : 1;
  examScore.value = correctCount * pointsPerQuestion;
  
  examFinished.value = true;
  examStarted.value = false;
};

// 模拟考试：点击交卷按钮
const handleMockSubmit = () => {
  const unanswered = userAnswers.value.filter(ans => !ans || ans.length === 0).length;
  if (unanswered > 0) {
    Modal.confirm({
      title: '提示',
      content: `您还有 ${unanswered} 道题未作答，确定要交卷吗？`,
      okText: '确定交卷',
      cancelText: '继续答题',
      onOk: submitExam
    });
  } else {
    submitExam();
  }
};

watch(currentIndex, (newVal) => {
  if (mode.value === 'order') {
    localStorage.setItem(storageKeyProgress.value, newVal);
  }
});

watch(mode, () => {
  loadQuestions();
});

watch(activeTab, () => {
  loadQuestions();
});

onMounted(() => {
  loadQuestions();
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <a-card :title="`在线学习 - ${mode === 'order' ? '顺序练习' : mode === 'mock' ? '模拟考试' : '错题本'}`" class="learning-card">
    <template #extra>
      <a-radio-group v-model:value="activeTab" button-style="solid">
        <a-radio-button value="1">科目一 (交规)</a-radio-button>
        <a-radio-button value="4">科目四 (安全文明)</a-radio-button>
      </a-radio-group>
    </template>
    
    <a-spin :spinning="loading" tip="正在拉取题库...">
      <div class="layout-wrapper" v-if="questions.length > 0">
        
        <!-- 左侧主内容区 -->
        <div class="main-content">
          
          <!-- 模拟考试前导页 -->
          <div v-if="mode === 'mock' && !examStarted && !examFinished" class="mock-intro">
            <h2>模拟考试须知</h2>
            <div class="intro-box">
              <p><strong>1. 考试科目：</strong>{{ activeTab === '1' ? '科目一' : '科目四' }}</p>
              <p><strong>2. 考试题量：</strong>{{ questions.length }} 题</p>
              <p><strong>3. 考试时间：</strong>45分钟</p>
              <p><strong>4. 合格标准：</strong>满分100分，90分及格</p>
              <p><strong>5. 交卷规则：</strong>倒计时结束系统将自动交卷，也可手动点击“交卷”按钮。</p>
            </div>
            <div class="intro-action">
              <a-button type="primary" size="large" @click="startExam" class="start-btn">开始考试</a-button>
            </div>
          </div>

          <!-- 模拟考试结果页 -->
          <div v-else-if="mode === 'mock' && examFinished" class="mock-result">
            <h2>考试结束</h2>
            <div class="score-display" :class="{ pass: examScore >= 90, fail: examScore < 90 }">
              {{ examScore }} 分
            </div>
            <p class="result-msg">{{ examScore >= 90 ? '恭喜您，通过考试！' : '很遗憾，未通过考试，错题已加入错题本！' }}</p>
            <a-button type="primary" size="large" @click="loadQuestions">再考一次</a-button>
          </div>
          
          <!-- 答题区 -->
          <div v-else-if="currentQ" class="quiz-container">
            <div class="quiz-header">
              <div class="quiz-type-tag">
                <a-tag color="blue">{{ isTrueFalse ? '判断题' : (answerMap[currentQ.answer]?.length > 1 ? '多选题' : '单选题') }}</a-tag>
                <a-tag v-if="mode === 'order' || mode === 'mock'" color="cyan">进度: {{ currentIndex + 1 }} / {{ questions.length }}</a-tag>
                <a-tag v-if="mode === 'mistakes'" color="orange">共 {{ questions.length }} 道错题</a-tag>
              </div>
              <h3>{{ currentIndex + 1 }}. {{ currentQ.question }}</h3>
              <img v-if="currentQ.url" :src="currentQ.url" alt="题目配图" class="quiz-img" />
            </div>
            
            <div class="quiz-options">
              <a-checkbox-group v-model:value="userAnswers[currentIndex]" class="options-group" :disabled="showAnswer">
                <a-checkbox value="1" class="option-item">A. {{ currentQ.item1 || '正确' }}</a-checkbox>
                <a-checkbox value="2" class="option-item">B. {{ currentQ.item2 || '错误' }}</a-checkbox>
                <a-checkbox v-if="currentQ.item3" value="3" class="option-item">C. {{ currentQ.item3 }}</a-checkbox>
                <a-checkbox v-if="currentQ.item4" value="4" class="option-item">D. {{ currentQ.item4 }}</a-checkbox>
              </a-checkbox-group>
            </div>
            
            <div class="quiz-actions">
              <a-button @click="prevQ" :disabled="currentIndex === 0">上一题</a-button>
              <a-button v-if="mode !== 'mock'" type="primary" @click="doSubmit" :disabled="showAnswer">确认答案</a-button>
              <a-button @click="nextQ" :disabled="currentIndex === questions.length - 1">下一题</a-button>
              <a-button v-if="mode === 'mock'" type="primary" danger @click="handleMockSubmit" style="margin-left:auto;">交卷</a-button>
              <a-button v-if="mode === 'mistakes'" danger @click="removeFromMistakes" style="margin-left:auto;">移除错题</a-button>
            </div>
            
            <div v-if="showAnswer" class="quiz-explain" :class="{ correct: isCorrect, incorrect: !isCorrect }">
              <div class="result-text">
                <span v-if="isCorrect">✅ 回答正确！</span>
                <span v-else>❌ 回答错误！正确答案是：{{ correctText }}</span>
              </div>
              <div class="explain-text">
                <strong>题目解析：</strong>{{ currentQ.explains }}
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧答题卡 (仅在非模拟考试介绍/结果页时显示) -->
        <div class="side-panel" v-if="mode !== 'mock' || examStarted">
          <!-- 倒计时模块 -->
          <a-card size="small" class="timer-card" v-if="mode === 'mock' && examStarted">
            <div class="timer-wrap">
              <div class="timer-label">剩余时间</div>
              <div class="timer-value" :class="{ warning: timeLeft < 300 }">{{ formattedTime }}</div>
            </div>
          </a-card>
          
          <!-- 答题网格 -->
          <a-card size="small" title="答题卡">
            <div class="grid-container" :class="{ 'mock-mode': mode === 'mock' }">
              <div class="grid-item" 
                   v-for="(q, i) in questions" :key="i"
                   :class="{
                     active: currentIndex === i,
                     answered: userAnswers[i]?.length > 0 && mode === 'mock',
                     correct: questionStatus[i] === 'correct' && mode !== 'mock',
                     wrong: questionStatus[i] === 'wrong' && mode !== 'mock'
                   }"
                   @click="goToQuestion(i)">
                {{ i + 1 }}
              </div>
            </div>
          </a-card>
        </div>

      </div>
      
      <a-empty v-else-if="!loading" :description="mode === 'mistakes' ? '太棒了，目前没有错题！' : '暂无题目数据，请检查 API Key 配置'" />
    </a-spin>
  </a-card>
</template>

<style scoped>
.learning-card {
  min-height: calc(100vh - 120px);
}
.layout-wrapper {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.main-content {
  flex: 1;
  min-width: 0;
  background: #fff;
}
.side-panel {
  width: 320px;
  flex-shrink: 0;
}

/* 模拟考试前导页 & 结果页 */
.mock-intro, .mock-result {
  text-align: center;
  padding: 60px 20px;
}
.mock-intro h2, .mock-result h2 {
  font-size: 24px;
  margin-bottom: 24px;
}
.intro-box {
  text-align: left;
  display: inline-block;
  background: #fafafa;
  padding: 24px 40px;
  border-radius: 8px;
  margin-bottom: 30px;
  font-size: 16px;
  line-height: 2;
}
.intro-action {
  text-align: center;
}
.start-btn {
  min-width: 150px;
}
.score-display {
  font-size: 64px;
  font-weight: bold;
  margin-bottom: 16px;
}
.score-display.pass { color: #52c41a; }
.score-display.fail { color: #ff4d4f; }
.result-msg {
  font-size: 18px;
  margin-bottom: 30px;
}

/* 题目区 */
.quiz-container {
  padding: 10px;
}
.quiz-type-tag {
  margin-bottom: 8px;
}
.quiz-header h3 {
  font-size: 18px;
  margin-bottom: 16px;
  line-height: 1.5;
}
.quiz-img {
  max-width: 100%;
  max-height: 250px;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #eee;
}
.quiz-options {
  margin-bottom: 30px;
}
.options-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.option-item {
  font-size: 16px;
  margin-left: 0 !important;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fafafa;
  transition: all 0.3s;
}
.option-item:hover {
  background: #f0f0f0;
}
.quiz-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}
.quiz-explain {
  padding: 20px;
  border-radius: 8px;
  background: #f9f9f9;
}
.quiz-explain.correct {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}
.quiz-explain.incorrect {
  background: #fff2f0;
  border: 1px solid #ffccc7;
}
.result-text {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 12px;
}
.explain-text {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
}

/* 右侧面板样式 */
.timer-card {
  margin-bottom: 16px;
  text-align: center;
}
.timer-label {
  color: #888;
  font-size: 14px;
  margin-bottom: 4px;
}
.timer-value {
  font-size: 28px;
  font-weight: bold;
  color: #52c41a;
}
.timer-value.warning {
  color: #ff4d4f;
  animation: blink 1s infinite;
}
@keyframes blink {
  50% { opacity: 0.5; }
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 6px;
  max-height: 450px;
  overflow-y: auto;
  padding: 4px;
}
.grid-item {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  transition: all 0.3s;
}
.grid-item:hover {
  border-color: #1890ff;
}
.grid-item.active {
  background: #1890ff;
  color: #fff;
  border-color: #1890ff;
}
/* 模拟考试中已答题 */
.grid-item.answered {
  background: #e6f7ff;
  border-color: #91d5ff;
}
/* 顺序练习中正确/错误 */
.grid-item.correct {
  background: #f6ffed;
  border-color: #b7eb8f;
}
.grid-item.wrong {
  background: #fff2f0;
  border-color: #ffccc7;
}
/* 模拟考试中禁止点击网格跳转 */
.grid-container.mock-mode .grid-item {
  pointer-events: none;
}
</style>