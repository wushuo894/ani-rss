<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <el-form style="max-width: 600px" label-width="auto">
      <el-form-item label="qBittorrent 地址">
        <el-input v-model:model-value="config.host"></el-input>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model:model-value="config.username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="config.password"></el-input>
      </el-form-item>
      <el-form-item label="间隔">
        <el-input-number v-model:model-value="config.sleep"></el-input-number>
      </el-form-item>
      <el-form-item label="重命名">
        <el-switch v-model:model-value="config.rename"></el-switch>
      </el-form-item>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="configButtonLoading" @click="editConfig">确定</el-button>
      </div>
    </el-form>
  </el-dialog>
  <el-dialog v-model="addDialogVisible" title="添加订阅" center>
    <el-form style="max-width: 600px" label-width="auto">
      <el-form-item label="RSS 地址">
        <el-input v-model:model-value="ani.url"></el-input>
      </el-form-item>
      <div v-if="ani.title">
        <el-form-item label="标题">
          <el-input v-model:model-value="ani.title"></el-input>
        </el-form-item>
        <el-form-item label="季">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number style="max-width: 200px" v-model:model-value="ani.season"></el-input-number>
          </div>
        </el-form-item>
        <el-form-item label="集数偏移">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number v-model:model-value="ani.offset"></el-input-number>
          </div>
        </el-form-item>
        <el-form-item label="排除">
          <div class="flex gap-2">
            <el-tag
                v-for="tag in ani.exclude"
                :key="tag"
                closable
                :disable-transitions="false"
                @close="handleClose(tag)"
                style="margin-right: 4px;"
            >
              {{ tag }}
            </el-tag>
            <el-input
                style="max-width: 80px;"
                v-if="excludeVisible"
                ref="InputRef"
                v-model="excludeValue"
                class="w-20"
                size="small"
                @keyup.enter="handleInputConfirm"
                @blur="handleInputConfirm"
            />
            <el-button v-else class="button-new-tag" size="small" @click="showInput">
              +
            </el-button>
          </div>
        </el-form-item>
        <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
          <el-button :loading="addAniButtonLoading" @click="addAni">确定</el-button>
        </div>
      </div>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;" v-else>
        <el-button :loading="rssButtonLoading" @click="getRss">确定</el-button>
      </div>
    </el-form>
  </el-dialog>
  <el-dialog v-model="editDialogVisible" title="修改订阅" center>
    <el-form style="max-width: 600px" label-width="auto">
      <el-form-item label="标题">
        <el-input v-model:model-value="ani.title"></el-input>
      </el-form-item>
      <el-form-item label="季">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number style="max-width: 200px" v-model:model-value="ani.season"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="集数偏移">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number v-model:model-value="ani.offset"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="排除">
        <div class="flex gap-2">
          <el-tag
              v-for="tag in ani.exclude"
              :key="tag"
              closable
              :disable-transitions="false"
              @close="handleClose(tag)"
              style="margin-right: 4px;"
          >
            {{ tag }}
          </el-tag>
          <el-input
              style="max-width: 80px;"
              v-if="excludeVisible"
              ref="InputRef"
              v-model="excludeValue"
              class="w-20"
              size="small"
              @keyup.enter="handleInputConfirm"
              @blur="handleInputConfirm"
          />
          <el-button v-else class="button-new-tag" size="small" @click="showInput">
            +
          </el-button>
        </div>
      </el-form-item>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="addAniButtonLoading" @click="editAni">确定</el-button>
      </div>
    </el-form>
  </el-dialog>
  <div style="display: flex;justify-content: space-between;width: 100%;">
    <div style="margin: 10px;">
      <el-input v-model:model-value="title"></el-input>
    </div>
    <div style="margin: 10px;">
      <el-button @click="()=>{
          ani = {
                  'url': '',
                  'season': 1,
                  'offset': 0,
                  'title': '',
                  'exclude': []
                }
          addDialogVisible = true
          excludeVisible = false
          excludeValue = ''
      }">添加
      </el-button>
      <el-button @click="showConfig">设置</el-button>
    </div>
  </div>
  <div style="margin: 0 10px">
    <el-card shadow="never" v-for="(item,index) in list.filter(it => it.title.indexOf(title) >-1)"
             style="margin: 3px 0;">
      <div style="display: flex;width: 100%;">
        <img :src="item.cover" height="130" width="92">
        <div style="flex-grow: 1;position: relative;">
          <div style="margin-left: 10px;">
            <div style="
              font-size: 0.97em;
              line-height: 1.6;
              font-weight: 500;
              hyphens: auto;
              letter-spacing: .0125em;
              min-width: 0;">
              {{ item.title }}
            </div>
            <div style="
            color: #9e9e9e !important;
            font-size: .75rem !important;
            font-weight: 400;
            line-height: 1.667;
            letter-spacing: .0333333333em !important;
            font-family: Roboto, sans-serif;
            text-transform: none !important;">{{ item.url }}
            </div>
          </div>
          <div
              style="display: flex;align-items: flex-end;justify-content:flex-end; flex-direction: column;position: absolute;right: 0;bottom: 0;">
            <el-button @click="()=>{
            editDialogVisible = true
            ani = JSON.parse(JSON.stringify(item))
            excludeVisible = false
            excludeValue = ''
          }">编辑
            </el-button>
            <div style="height: 5px;"></div>
            <el-popconfirm title="你确定要删除吗?" @confirm="delAni(item)">
              <template #reference>
                <el-button>删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from 'element-plus'

const title = ref('')

const configDialogVisible = ref(false)
const configButtonLoading = ref(false)

const config = ref({
  'rename': true,
  'host': '',
  'username': '',
  'password': '',
  'sleep': 5
})


const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const ani = ref({
  'url': '',
  'season': 1,
  'offset': 0,
  'title': '',
  'exclude': []
})

const excludeVisible = ref(false)
const excludeValue = ref('')

const handleClose = (tag) => {
  ani.value.exclude.splice(ani.value.exclude.indexOf(tag), 1)
}

const InputRef = ref()

const showInput = () => {
  excludeVisible.value = true
  InputRef.value?.input?.focus()
}

const handleInputConfirm = () => {
  if (excludeValue.value) {
    ani.value.exclude.push(excludeValue.value)
  }
  excludeVisible.value = false
  excludeValue.value = ''
}

const rssButtonLoading = ref(false)

const getRss = () => {
  rssButtonLoading.value = true
  fetch('/api/rss', {
    'method': 'POST',
    'body': JSON.stringify(ani.value)
  }).then(res => res.json())
      .then(res => {
        rssButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        ani.value = res.data
      })
}


const addAniButtonLoading = ref(false)

const addAni = () => {
  addAniButtonLoading.value = true
  fetch('/api/ani', {
    'method': 'POST',
    'body': JSON.stringify(ani.value)
  })
      .then(res => res.json())
      .then(res => {
        addAniButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          getList()
          return
        }
        ElMessage.success(res.message)
        getList()
        addDialogVisible.value = false
      })
}

const editAniButtonLoading = ref(false)

const editAni = () => {
  editAniButtonLoading.value = true
  fetch('/api/ani', {
    'method': 'PUT',
    'body': JSON.stringify(ani.value)
  })
      .then(res => res.json())
      .then(res => {
        editAniButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          getList()
          return
        }
        ElMessage.success(res.message)
        getList()
        editDialogVisible.value = false
      })
}

const delAni = (ani) => {
  fetch('/api/ani', {
    'method': 'DELETE',
    'body': JSON.stringify(ani)
  })
      .then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          getList()
          return
        }
        ElMessage.success(res.message)
        getList()
      })
}

const showConfig = () => {
  fetch('/api/config', {
    'method': 'GET'
  })
      .then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        config.value = res.data
        configDialogVisible.value = true
      })
}
const editConfig = () => {
  configButtonLoading.value = true
  fetch('/api/config', {
    'method': 'POST',
    'body': JSON.stringify(config.value)
  })
      .then(res => res.json())
      .then(res => {
        configButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        ElMessage.success(res.message)
        configDialogVisible.value = false
      })
}


const list = ref([])

const getList = () => {
  fetch('/api/ani', {'method': 'GET'})
      .then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        list.value = res.data
      })
}

getList()

</script>


