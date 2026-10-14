/** 后端席别编码与前端中文名称的统一映射。 */
export const SEAT_TYPE_LABELS: Readonly<Record<string, string>> = {
  BUSINESS_CLASS: '商务座',
  FIRST_CLASS: '一等座',
  SECOND_CLASS: '二等座',
}

/** 管理端使用的席别选项；提交给后端的 value 始终保持原编码。 */
export const SEAT_TYPE_OPTIONS = Object.entries(SEAT_TYPE_LABELS).map(([value, name]) => ({
  value,
  label: `${name}（${value}）`,
}))

/** 将稳定的后端编码格式化为适合界面显示的“中文名（编码）”。 */
export function formatSeatType(code: string): string {
  const name = SEAT_TYPE_LABELS[code]
  return name ? `${name}（${code}）` : code
}
