<template>
  <div>
    <SettingsItem label="IP">
      <el-input v-model:model-value="props.config.proxyHost" :disabled="!props.config.proxy"
                placeholder="192.168.0.x"/>
    </SettingsItem>
    <SettingsItem label="端口">
      <el-input-number v-model:model-value="props.config.proxyPort" :disabled="!props.config.proxy" :min="1"
                       :max="65535"/>
    </SettingsItem>
    <SettingsItem label="用户名">
      <el-input v-model:model-value="props.config.proxyUsername" :disabled="!props.config.proxy"
                placeholder="可以为空">
        <template #prefix>
          <el-icon class="el-input__icon">
            <User/>
          </el-icon>
        </template>
      </el-input>
    </SettingsItem>
    <SettingsItem label="密码">
      <el-input v-model:model-value="props.config.proxyPassword" :disabled="!props.config.proxy"
                placeholder="可以为空">
        <template #prefix>
          <el-icon class="el-input__icon">
            <Key/>
          </el-icon>
        </template>
      </el-input>
    </SettingsItem>
    <SettingsItem label="代理列表">
      <el-input
          class="full-width"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 3}"
          v-model="props.config.proxyList"
          :disabled="!props.config.proxy"/>
    </SettingsItem>
    <SettingsItem label="启用">
      <el-switch v-model:model-value="props.config.proxy"/>
    </SettingsItem>
    <SettingsItem label="代理测试">
      <div class="proxy-test-container">
        <div class="proxy-test-controls">
          <el-select
              v-model:model-value="url"
              class="proxy-test-select"
              filterable
              allow-create
              default-first-option
              clearable
              placeholder="测试地址"
              :disabled="testLoading">
            <el-option :value="it" :label="formatUrlLabel(it)" :key="it" v-for="it in urls">
              <div class="proxy-test-option">
                <span>{{ formatUrlLabel(it) }}</span>
                <el-text truncated size="small" type="info">{{ it }}</el-text>
              </div>
            </el-option>
          </el-select>
          <el-button
              bg
              text
              type="primary"
              :disabled="!url"
              :loading="testLoading"
              @click="test"
              icon="Odometer">
            测试
          </el-button>
        </div>
        <div
            v-if="resultVisible"
            class="proxy-test-result"
            :class="{'is-success': resultOk, 'is-error': !resultOk}">
          <div class="proxy-test-result-main">
            <el-icon class="proxy-test-result-icon">
              <CircleCheck v-if="resultOk"/>
              <Warning v-else/>
            </el-icon>
            <div class="proxy-test-result-content">
              <div class="proxy-test-result-title">{{ resultTitle }}</div>
              <el-text class="proxy-test-result-url" truncated size="small" type="info">{{ testedUrl }}</el-text>
            </div>
          </div>
          <div class="proxy-test-metrics">
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
            <el-tag type="info" size="small">{{ timeLabel }}</el-tag>
          </div>
        </div>
      </div>
    </SettingsItem>
  </div>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";
import {computed, onMounted, ref} from "vue";
import {ElMessage, ElText} from "element-plus";
import {CircleCheck, Key, User, Warning} from "@element-plus/icons-vue";
import {testProxy} from "@/js/http.js";
import {base64Encode} from "@/js/global.js";

let urls = ref([
  'https://mikanani.me',
  'https://mikanime.tv',
  'https://nyaa.si',
  'https://acg.rip',
  'https://github.com',
  'https://www.google.com',
  'https://bgm.tv',
  'https://www.themoviedb.org'
])

let url = ref('')
const testResult = ref(null)
const testError = ref('')
const testedUrl = ref('')

onMounted(() => {
  url.value = urls.value[0]
})

let testLoading = ref(false)

const resultVisible = computed(() => Boolean(testResult.value || testError.value))
const resultStatus = computed(() => Number(testResult.value?.status || 0))
const hasStatus = computed(() => resultStatus.value > 0)
const resultOk = computed(() => !testError.value && hasStatus.value && resultStatus.value < 400)
const statusTagType = computed(() => resultOk.value ? 'success' : 'danger')
const resultTitle = computed(() => {
  if (testError.value) {
    return testError.value
  }
  if (!hasStatus.value) {
    return '连接失败'
  }
  return testResult.value?.title || (resultOk.value ? '连接成功' : '响应异常')
})
const statusLabel = computed(() => hasStatus.value ? `HTTP ${resultStatus.value}` : '无状态')
const timeLabel = computed(() => {
  const time = testResult.value?.time
  return Number.isFinite(Number(time)) ? `${time} ms` : '-- ms'
})

const formatUrlLabel = value => {
  try {
    return new URL(normalizeUrl(value)).hostname
  } catch {
    return value
  }
}

const normalizeUrl = value => {
  const text = (value || '').trim()
  if (!text) {
    return ''
  }
  return /^https?:\/\//i.test(text) ? text : `https://${text}`
}

let test = () => {
  const testUrl = normalizeUrl(url.value)
  if (!testUrl) {
    return
  }

  testLoading.value = true
  testResult.value = null
  testError.value = ''
  url.value = testUrl
  testedUrl.value = testUrl

  testProxy(base64Encode(testUrl), props.config)
      .then(res => {
        testResult.value = res.data || {}
        if (resultOk.value) {
          ElMessage.success('测试成功')
          return
        }
        ElMessage.warning('测试异常')
      })
      .catch(error => {
        testError.value = error.message || '测试失败'
      })
      .finally(() => {
        testLoading.value = false
      })
}

let props = defineProps(['config'])

</script>

<style scoped>
.proxy-test-container {
  width: 100%;
  min-width: 0;
}

.proxy-test-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.proxy-test-select {
  flex: 1;
  min-width: 220px;
  max-width: 420px;
}

.proxy-test-option {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.proxy-test-result {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-fill-color-light);
}

.proxy-test-result.is-success {
  border-color: var(--el-color-success-light-5);
  background-color: var(--el-color-success-light-9);
}

.proxy-test-result.is-error {
  border-color: var(--el-color-danger-light-5);
  background-color: var(--el-color-danger-light-9);
}

.proxy-test-result-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.proxy-test-result-icon {
  flex-shrink: 0;
  font-size: 18px;
}

.is-success .proxy-test-result-icon {
  color: var(--el-color-success);
}

.is-error .proxy-test-result-icon {
  color: var(--el-color-danger);
}

.proxy-test-result-content {
  min-width: 0;
}

.proxy-test-result-title {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.proxy-test-result-url {
  max-width: 100%;
}

.proxy-test-metrics {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 700px) {
  .proxy-test-controls,
  .proxy-test-result {
    align-items: stretch;
    flex-direction: column;
  }

  .proxy-test-select {
    width: 100%;
    max-width: none;
  }

  .proxy-test-metrics {
    align-self: flex-start;
  }
}
</style>
