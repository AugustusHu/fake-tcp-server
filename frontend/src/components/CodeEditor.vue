<template>
  <div class="code-editor" :class="`language-${normalizedLanguage}`">
    <pre ref="highlightLayer" class="code-editor-highlight" aria-hidden="true"><code v-html="highlightedCode"></code><span v-if="!displayedCode" class="code-editor-placeholder">{{ placeholder }}</span></pre>
    <textarea
      ref="textarea"
      :value="displayedCode"
      :placeholder="placeholder"
      :readonly="readonly"
      wrap="off"
      spellcheck="false"
      @input="emitUpdate"
      @scroll="syncScroll"
    ></textarea>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { highlightCode } from '../utils/codeHighlight';

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  readonly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue']);
const textarea = ref(null);
const highlightLayer = ref(null);
const normalizedLanguage = computed(() => String(props.language || 'text').toLowerCase());
const displayedCode = computed(() => props.modelValue || '');
const highlightedCode = computed(() => highlightCode(displayedCode.value, normalizedLanguage.value));

function emitUpdate(event) {
  emit('update:modelValue', event.target.value);
  syncScroll();
}

function syncScroll() {
  if (!textarea.value || !highlightLayer.value) return;
  highlightLayer.value.scrollTop = textarea.value.scrollTop;
  highlightLayer.value.scrollLeft = textarea.value.scrollLeft;
}
</script>

<style scoped>
.code-editor {
  background: #0f1014;
  border: 1px solid #303038;
  display: grid;
  min-height: 220px;
  overflow: hidden;
  position: relative;
}

.code-editor-highlight,
.code-editor textarea {
  background: transparent !important;
  border: 0 !important;
  box-sizing: border-box;
  font-family: "JetBrains Mono", "Fira Code", "SFMono-Regular", Consolas, monospace !important;
  font-size: 12px;
  inset: 0;
  line-height: 1.5;
  margin: 0;
  min-height: 100%;
  overflow: auto;
  padding: 10px;
  position: absolute;
  resize: none;
  tab-size: 2;
  white-space: pre;
  width: 100%;
}

.code-editor-highlight {
  color: #dcdcdc;
  pointer-events: none;
}

.code-editor textarea {
  caret-color: #f3f3f5;
  color: transparent !important;
  outline: none;
  z-index: 1;
}

.code-editor textarea::placeholder {
  color: transparent;
}

.code-editor textarea::selection {
  background: rgba(86, 156, 214, 0.35);
}

.code-editor-placeholder {
  color: #7a7a82;
}

.code-editor :deep(.code-tag) {
  color: #7dd3fc;
}

.code-editor :deep(.code-attr) {
  color: #f0d58b;
}

.code-editor :deep(.code-string) {
  color: #ce9178;
}

.code-editor :deep(.code-keyword),
.code-editor :deep(.code-command) {
  color: #c586c0;
}

.code-editor :deep(.code-type) {
  color: #4ec9b0;
}

.code-editor :deep(.code-number) {
  color: #b5cea8;
}

.code-editor :deep(.code-comment) {
  color: #7a7a82;
}

.code-editor :deep(.code-flag) {
  color: #9cdcfe;
}
</style>
