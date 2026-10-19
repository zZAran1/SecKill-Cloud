<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '@/api/product'
import { ApiError } from '@/types'
import { useToast } from '@/utils/toast'
import { safeImageUrl } from '@/utils/security'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const productId = computed(() => Number(route.params.id))

const form = reactive({
  name: '',
  price: null as number | null,
  stock: null as number | null,
  imageUrl: '',
  description: '',
})

const errors = reactive({ name: '', price: '', stock: '', imageUrl: '', description: '' })
const loading = ref(true)
const saving = ref(false)
const serverError = ref('')
const previewFailed = ref(false)

const PRICE_RE = /^\d+(\.\d{1,2})?$/

const previewUrl = computed(() => safeImageUrl(form.imageUrl))
const showPreview = computed(() => !!previewUrl.value && !previewFailed.value)

async function load() {
  loading.value = true
  serverError.value = ''
  try {
    const res = await productApi.detail(productId.value)
    const p = res.data
    form.name = p.name
    form.price = p.price
    form.stock = p.stock
    form.imageUrl = p.imageUrl ?? ''
    form.description = p.description ?? ''
  } catch (e) {
    serverError.value = e instanceof ApiError && e.code === 2001 ? '商品不存在' : e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function validate(): boolean {
  errors.name = ''
  errors.price = ''
  errors.stock = ''
  errors.imageUrl = ''
  errors.description = ''

  const name = form.name.trim()
  if (!name) errors.name = '请输入商品名称'
  else if (name.length > 100) errors.name = '商品名称不能超过 100 个字符'

  if (form.price === null || form.price === undefined) errors.price = '请输入价格'
  else if (!PRICE_RE.test(String(form.price))) errors.price = '价格最多保留两位小数'
  else if (form.price < 0.01) errors.price = '价格不能低于 0.01 元'

  if (form.stock === null || form.stock === undefined) errors.stock = '请输入库存'
  else if (!Number.isInteger(form.stock) || form.stock < 0) errors.stock = '库存需为不小于 0 的整数'

  const imageUrl = form.imageUrl.trim()
  if (imageUrl) {
    if (imageUrl.length > 255) errors.imageUrl = '图片链接不能超过 255 个字符'
    else if (!safeImageUrl(imageUrl)) errors.imageUrl = '仅支持 http:// 或 https:// 开头的图片地址'
  }

  if (form.description.trim().length > 1000) errors.description = '商品描述不能超过 1000 个字符'

  return (
    !errors.name && !errors.price && !errors.stock && !errors.imageUrl && !errors.description
  )
}

async function submit() {
  serverError.value = ''
  if (!validate()) return

  saving.value = true
  try {
    await productApi.update(productId.value, {
      name: form.name.trim(),
      price: form.price as number,
      stock: form.stock as number,
      imageUrl: form.imageUrl.trim() || undefined,
      description: form.description.trim() || undefined,
    })
    toast.show('商品已更新', 'success')
    router.push({ name: 'product-detail', params: { id: productId.value } })
  } catch (e) {
    serverError.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="form-page">
    <div class="container">
      <div class="form-card">
        <h1>编辑商品</h1>
        <p class="form-sub">修改商品信息，保存后缓存会自动失效</p>

        <div v-if="serverError" class="form-alert">{{ serverError }}</div>

        <!-- 加载中 -->
        <div v-if="loading" class="order-list">
          <div v-for="i in 3" :key="i" class="skel-card order-skel">
            <div class="skel skel-line" style="width: 40%"></div>
            <div class="skel skel-line short"></div>
          </div>
        </div>

        <form v-else novalidate @submit.prevent="submit">
          <div class="field">
            <label class="field-label" for="ename">
              <span><i class="req">*</i>商品名称</span>
              <span class="field-count">{{ form.name.length }}/100</span>
            </label>
            <input
              id="ename"
              v-model="form.name"
              class="input"
              :class="{ err: errors.name }"
              maxlength="100"
              @input="errors.name = ''"
            />
            <p v-if="errors.name" class="field-error">{{ errors.name }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="eprice"><span><i class="req">*</i>价格（元）</span></label>
            <input
              id="eprice"
              v-model.number="form.price"
              class="input"
              :class="{ err: errors.price }"
              type="number"
              min="0.01"
              step="0.01"
              @input="errors.price = ''"
            />
            <p v-if="errors.price" class="field-error">{{ errors.price }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="estock">
              <span><i class="req">*</i>库存（件）</span>
              <em class="field-count">用于盘点调整</em>
            </label>
            <input
              id="estock"
              v-model.number="form.stock"
              class="input"
              :class="{ err: errors.stock }"
              type="number"
              min="0"
              step="1"
              @input="errors.stock = ''"
            />
            <p v-if="errors.stock" class="field-error">{{ errors.stock }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="eimg">
              <span>图片链接</span>
              <span class="field-count">{{ form.imageUrl.length }}/255</span>
            </label>
            <input
              id="eimg"
              v-model="form.imageUrl"
              class="input"
              :class="{ err: errors.imageUrl }"
              maxlength="255"
              placeholder="http(s):// 开头的图片地址，选填"
              @input="errors.imageUrl = ''; previewFailed = false"
            />
            <p v-if="errors.imageUrl" class="field-error">{{ errors.imageUrl }}</p>
            <div v-if="showPreview" class="img-preview">
              <img :src="previewUrl ?? ''" alt="商品图片预览" @error="previewFailed = true" />
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="edesc">
              <span>商品描述</span>
              <span class="field-count">{{ form.description.length }}/1000</span>
            </label>
            <textarea
              id="edesc"
              v-model="form.description"
              class="textarea"
              :class="{ err: errors.description }"
              maxlength="1000"
              @input="errors.description = ''"
            ></textarea>
            <p v-if="errors.description" class="field-error">{{ errors.description }}</p>
          </div>

          <div class="form-actions">
            <button class="btn btn-primary" type="submit" :disabled="saving">
              <span v-if="saving" class="spinner"></span>{{ saving ? '保存中' : '保存修改' }}
            </button>
            <button class="btn btn-line" type="button" @click="router.back()">取消</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
