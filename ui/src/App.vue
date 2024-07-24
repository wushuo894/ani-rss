<template>
  <Config ref="config"></Config>
  <Add ref="add" @load="getList"></Add>
  <Edit ref="edit" @load="getList"></Edit>
  <div style="display: flex;justify-content: space-between;width: 100%;">
    <div style="margin: 10px;">
      <el-input v-model:model-value="title" placeholder="搜索" @input="currentPage = 1"></el-input>
    </div>
    <div style="margin: 10px;">
      <el-button @click="add?.showAdd">添加</el-button>
      <el-button @click="config?.showConfig">设置</el-button>
    </div>
  </div>
  <div style="margin: 0 10px">
    <el-card shadow="never"
             v-for="(item,index) in list.filter(it => it.title.indexOf(title) > -1 || it['pinyin'].indexOf(title) > -1).slice((currentPage-1)*pageSize,(currentPage-1)*pageSize+pageSize)"
             style="margin: 3px 0;">
      <div style="display: flex;width: 100%;">
        <img :src="item.cover" height="130" width="92" :alt="item.title">
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
            <el-button @click="edit?.showEdit(item)">编辑
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
  <div style="margin: 10px;">
    <el-pagination background layout="prev, pager, next" :total="list.filter(it => it.title.indexOf(title) > -1 || it['pinyin'].indexOf(title) > -1).length" v-model:current-page="currentPage"
                   :page-size="pageSize"/>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from 'element-plus'
import Config from "./Config.vue";
import Edit from "./Edit.vue";
import Add from "./Add.vue";

const title = ref('')

const config = ref()
const add = ref()
const edit = ref()
const currentPage = ref(1)
const pageSize = ref(10)

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
          ElMessage.error(res.message)
          return
        }
        list.value = res.data
      })
}

getList()

</script>


