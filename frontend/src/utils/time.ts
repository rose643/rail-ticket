/** 将相对运行日零点的分钟数显示为当日/次日时刻。 */
export function formatOffsetTime(minutes: number | null): string {
  if (minutes === null) return '—'
  const dayOffset = Math.floor(minutes / 1440)
  const minutesOfDay = minutes % 1440
  const time = `${String(Math.floor(minutesOfDay / 60)).padStart(2, '0')}:${String(minutesOfDay % 60).padStart(2, '0')}`
  if (dayOffset === 0) return `当日 ${time}`
  if (dayOffset === 1) return `次日 ${time}`
  return `第 ${dayOffset + 1} 天 ${time}`
}

export function formatClock(dateTime: string): string {
  return dateTime?.slice(11, 16) || '—'
}

export function formatDuration(minutes: number | string): string {
  const value = Number(minutes)
  return `${Math.floor(value / 60)} 小时 ${value % 60} 分`
}

export function formatDate(date: string): string {
  const [year, month, day] = date.split('-')
  return `${year}年${month}月${day}日`
}
