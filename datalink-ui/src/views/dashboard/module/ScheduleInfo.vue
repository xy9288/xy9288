<template>
  <a-skeleton active :loading='loading' :paragraph='{ rows: 17 }'>
      <a-table
        ref='table'
        rowKey='resourceName'
        :columns='columns'
        :data-source='dataSource'
        :pagination='false'
        style="margin-top: 20px"
      >

        <span slot='initialDelayUnit' slot-scope='text, record, index'>
          {{ text }} {{ timeUnitMap[record.initialDelayUnit] }}
        </span>

        <span slot='intervalUnit' slot-scope='text, record, index'>
          {{ text }} {{ timeUnitMap[record.intervalUnit] }}
        </span>

      </a-table>
  </a-skeleton>
</template>

<script>

import { postAction } from '@/api/manage'
import { timeUnitMap } from '@/config/time.config'

export default {
  name: 'ScheduleList',
  components: {},
  data() {
    return {
      columns: [
        {
          title: '调度资源',
          dataIndex: 'resourceName'
        },
        {
          title: '启动延迟',
          dataIndex: 'initialDelay',
          scopedSlots: { customRender: 'initialDelayUnit' }
        },
        {
          title: '调度间隔',
          dataIndex: 'interval',
          scopedSlots: { customRender: 'intervalUnit' }
        },
        {
          title: '启动时间',
          dataIndex: 'createTime'
        }
      ],
      dataSource: [],
      loading: true,
      queryParam: {},
      timeUnitMap: timeUnitMap
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      postAction('/api/schedule/list', this.queryParam).then(res => {
        this.dataSource = res.data
        this.loading = false
      })
    },
  }
}
</script>
