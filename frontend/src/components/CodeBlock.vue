<template>
  <pre class="code-block" :class="`language-${normalizedLanguage}`"><code v-html="highlightedCode"></code></pre>
</template>

<script setup>
import { computed } from 'vue';
import { highlightCode } from '../utils/codeHighlight';

const props = defineProps({
  code: {
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
  }
});

const normalizedLanguage = computed(() => String(props.language || 'text').toLowerCase());
const displayedCode = computed(() => props.code || props.placeholder || '');

const highlightedCode = computed(() => {
  return highlightCode(displayedCode.value, normalizedLanguage.value);
});
</script>

<style scoped>
.code-block {
  background: #141419;
  border: 1px solid #565661;
  color: #f3f3f5;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
  margin: 0;
  min-height: 120px;
  overflow: auto;
  padding: 10px;
  white-space: pre;
}

.code-block :deep(.code-tag) {
  color: #7dd3fc;
}

.code-block :deep(.code-attr) {
  color: #f0d58b;
}

.code-block :deep(.code-string) {
  color: #ce9178;
}

.code-block :deep(.code-keyword),
.code-block :deep(.code-command) {
  color: #c586c0;
}

.code-block :deep(.code-type) {
  color: #4ec9b0;
}

.code-block :deep(.code-number) {
  color: #b5cea8;
}

.code-block :deep(.code-comment) {
  color: #7a7a82;
}

.code-block :deep(.code-flag) {
  color: #9cdcfe;
}
</style>
