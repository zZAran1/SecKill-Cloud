/** 金额格式化：保留两位小数 + 千分位 */
export function formatPrice(v: number | null | undefined): string {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return '--'
  return Number(v).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

/** ISO 时间 → YYYY-MM-DD HH:mm */
export function formatTime(iso: string | null | undefined): string {
  if (!iso) return '--'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 手机号脱敏：138****8000 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone || phone.length !== 11) return phone ?? '--'
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}
