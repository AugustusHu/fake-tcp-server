<template>
  <pre class="code-block" :class="`language-${normalizedLanguage}`"><code v-html="highlightedCode"></code></pre>
</template>

<script setup>
import { computed } from 'vue';

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
  const escaped = escapeHtml(displayedCode.value);
  if (normalizedLanguage.value === 'xml') return highlightXml(escaped);
  if (normalizedLanguage.value === 'java') return highlightJava(escaped);
  if (normalizedLanguage.value === 'json') return highlightJson(escaped);
  if (['bash', 'shell', 'sh'].includes(normalizedLanguage.value)) return highlightBash(escaped);
  return escaped;
});

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function withProtectedStrings(source, pattern, className, highlighter) {
  const placeholders = [];
  const protectedSource = source.replace(pattern, (match) => {
    const token = `@@CODE_TOKEN_${placeholders.length}@@`;
    placeholders.push(`<span class="${className}">${highlighter ? highlighter(match) : match}</span>`);
    return token;
  });
  return { protectedSource, placeholders };
}

function restore(source, placeholders) {
  return placeholders.reduce((result, value, index) => result.replaceAll(`@@CODE_TOKEN_${index}@@`, value), source);
}

function highlightXml(source) {
  const protectedStrings = withProtectedStrings(source, /&quot;[^&]*(?:&(?!quot;)[^&]*)*&quot;/g, 'code-string');
  let output = protectedStrings.protectedSource
    .replace(/(&lt;\/?)([A-Za-z_][\w:.-]*)/g, '$1<span class="code-tag">$2</span>')
    .replace(/\s([A-Za-z_:][\w:.-]*)(=)/g, ' <span class="code-attr">$1</span>$2')
    .replace(/(&lt;!--[\s\S]*?--&gt;)/g, '<span class="code-comment">$1</span>');
  return restore(output, protectedStrings.placeholders);
}

function highlightJava(source) {
  const protectedStrings = withProtectedStrings(source, /(&quot;[\s\S]*?&quot;|&#39;[\s\S]*?&#39;)/g, 'code-string');
  const protectedComments = withProtectedStrings(protectedStrings.protectedSource, /(\/\/.*|\/\*[\s\S]*?\*\/)/g, 'code-comment');
  const keywords = [
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue',
    'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'if',
    'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'new', 'package', 'private',
    'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized',
    'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while'
  ].join('|');
  let output = protectedComments.protectedSource
    .replace(new RegExp(`\\b(${keywords})\\b`, 'g'), '<span class="code-keyword">$1</span>')
    .replace(/\b([A-Z][A-Za-z0-9_]*)(?=\s*[({])/g, '<span class="code-type">$1</span>')
    .replace(/\b(\d+(?:\.\d+)?)\b/g, '<span class="code-number">$1</span>');
  output = restore(output, protectedComments.placeholders);
  return restore(output, protectedStrings.placeholders);
}

function highlightBash(source) {
  const protectedStrings = withProtectedStrings(source, /(&quot;[\s\S]*?&quot;|&#39;[\s\S]*?&#39;)/g, 'code-string');
  const protectedComments = withProtectedStrings(protectedStrings.protectedSource, /(^|\s)(#.*)$/gm, 'code-comment', (match) => match);
  let output = protectedComments.protectedSource
    .replace(/\b(curl|nc|ncat|openssl|printf|echo|cat|java|mvn|npm|docker|docker-compose|mysql|export)\b/g, '<span class="code-command">$1</span>')
    .replace(/(\s--?[A-Za-z0-9][\w-]*)/g, '<span class="code-flag">$1</span>');
  output = restore(output, protectedComments.placeholders);
  return restore(output, protectedStrings.placeholders);
}

function highlightJson(source) {
  const protectedStrings = withProtectedStrings(source, /&quot;(?:\\.|[^&])*?&quot;/g, 'code-string');
  let output = protectedStrings.protectedSource
    .replace(/\b(true|false|null)\b/g, '<span class="code-keyword">$1</span>')
    .replace(/\b-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b/g, '<span class="code-number">$&</span>');
  output = restore(output, protectedStrings.placeholders);
  return output.replace(/<span class="code-string">(&quot;[^<]*?&quot;)<\/span>(\s*:)/g, '<span class="code-attr">$1</span>$2');
}
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
