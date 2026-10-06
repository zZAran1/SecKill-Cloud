import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  msg: string
  type: ToastType
}

const state = reactive<{ list: ToastItem[] }>({ list: [] })
let seq = 0

function dismiss(id: number) {
  const i = state.list.findIndex((t) => t.id === id)
  if (i > -1) state.list.splice(i, 1)
}

function show(msg: string, type: ToastType = 'info', duration = 2600) {
  const id = ++seq
  state.list.push({ id, msg, type })
  window.setTimeout(() => dismiss(id), duration)
}

/** 全局轻提示，单例 */
export function useToast() {
  return { state, show, dismiss }
}
