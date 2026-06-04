export function highlightCode(code, language = 'text') {
  const normalizedLanguage = String(language || 'text').toLowerCase();
  const escaped = escapeHtml(code || '');
  if (normalizedLanguage === 'xml') return highlightXml(escaped);
  if (normalizedLanguage === 'java') return highlightJava(escaped);
  if (normalizedLanguage === 'json') return highlightJson(escaped);
  if (['bash', 'shell', 'sh'].includes(normalizedLanguage)) return highlightBash(escaped);
  return escaped;
}

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
  const protectedComments = withProtectedStrings(protectedStrings.protectedSource, /&lt;!--[\s\S]*?--&gt;/g, 'code-comment');
  let output = protectedComments.protectedSource.replace(
    /(&lt;\/?)([A-Za-z_][\w:.-]*)([\s\S]*?)(\/?&gt;)/g,
    (match, open, tagName, attributes, close) => {
      const highlightedAttributes = attributes.replace(/\s([A-Za-z_:][\w:.-]*)(=)/g, ' <span class="code-attr">$1</span>$2');
      return `${open}<span class="code-tag">${tagName}</span>${highlightedAttributes}${close}`;
    }
  );
  output = restore(output, protectedComments.placeholders);
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
