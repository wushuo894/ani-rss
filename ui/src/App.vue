<template>
  <el-dialog v-model="addDialogVisible" title="添加订阅" width="500" center>
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
            <el-input-number v-model:model-value="ani.off"></el-input-number>
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
  <div style="display: flex;justify-content: space-between;width: 100%;">
    <div style="margin: 10px;">
      <el-input v-model:model-value="title"></el-input>
    </div>
    <div style="margin: 10px;">
      <el-button @click="()=>{
        ani.url = ''
        ani.title = ''
        addDialogVisible = true
      }">添加
      </el-button>
    </div>
  </div>
  <div style="margin: 0 10px">
    <el-card shadow="never" v-for="(item,index) in list.filter(it => it.title.indexOf(title) >-1)"
             style="margin: 3px 0;">
      <div style="display: flex;justify-content: space-between;">
        <div style="display: flex;align-items: start;">
          <img :src="item.cover" height="130">
          <div style="margin-left: 10px">
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
        </div>
        <div style="display: flex;align-items: end;">
          <el-popconfirm title="你确定要删除吗?" @confirm="delAni(item)">
            <template #reference>
              <el-button>删除</el-button>
            </template>
          </el-popconfirm>

        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from 'element-plus'

const title = ref('')

const addDialogVisible = ref(false)
const ani = ref({
  'url': 'https://mikanime.tv/RSS/Bangumi?bangumiId=3359&subgroupid=583',
  'season': 1,
  'off': 0,
  'title': ''
})

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

const list = ref([])

const getList = () => {
  fetch('/api/ani', {'method': 'GET'})
      .then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          getList()
          ElMessage.error(res.message)
          return
        }
        list.value = res.data
        getList()
      })
}

getList()

</script>