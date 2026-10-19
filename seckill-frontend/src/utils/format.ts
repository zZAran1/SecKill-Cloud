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

/** 邮箱脱敏：ab****@qq.com */
export function maskEmail(email: string | null | undefined): string {
  if (!email) return '--'
  const at = email.indexOf('@')
  if (at <= 0) return email
  const name = email.slice(0, at)
  const kept = name.slice(0, Math.min(2, name.length))
  return `${kept}${'*'.repeat(Math.max(name.length - kept.length, 2))}${email.slice(at)}`
}

/** 订单状态文案：0 待支付 / 1 已支付 / 2 已取消 */
export function orderStatusText(status: number | null | undefined): string {
  switch (status) {
    case 0:
      return '待支付'
    case 1:
      return '已支付'
    case 2:
      return '已取消'
    default:
      return '未知状态'
  }
}

/** 秒杀活动进度：0 未开始 / 1 进行中 / 2 已结束，按开始结束时间推导 */
export function activityPhase(
  startTime: string | null | undefined,
  endTime: string | null | undefined,
  now: number = Date.now(),
): 0 | 1 | 2 {
  const start = startTime ? new Date(startTime).getTime() : NaN
  const end = endTime ? new Date(endTime).getTime() : NaN
  if (Number.isNaN(start) || Number.isNaN(end)) return 2
  if (now < start) return 0
  if (now > end) return 2
  return 1
}

/** 活动进度文案 */
export function activityPhaseText(phase: 0 | 1 | 2): string {
  return phase === 0 ? '未开始' : phase === 1 ? '进行中' : '已结束'
}

/** ISO 时间 → <input type="datetime-local"> 的取值（YYYY-MM-DDTHH:mm） */
export function toDateTimeInput(iso: string | null | undefined): string {
  return iso ? iso.slice(0, 16) : ''
}

