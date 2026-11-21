<template>
  <div>
    <PageHeader title="课表" subtitle="按周查看我的全部课程安排" />
    <WeeklySchedule :schedules="schedules" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyCourses, getCourseSchedule } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'
import WeeklySchedule from '@/components/WeeklySchedule.vue'

const schedules = ref([])

const fetchData = async () => {
  try {
    const courses = (await getMyCourses()).data || []
    const lists = await Promise.all(
      courses.map(c => getCourseSchedule(c.enrollmentId).catch(() => ({ data: [] })))
    )
    const seen = new Set()
    const all = []
    lists.forEach(l => {
      ;(l.data || []).forEach(s => {
        if (!seen.has(s.scheduleId)) {
          seen.add(s.scheduleId)
          all.push(s)
        }
      })
    })
    schedules.value = all
  } catch (e) {
    // 错误已由拦截器处理
  }
}

onMounted(fetchData)
</script>
