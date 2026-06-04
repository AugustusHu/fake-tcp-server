const authStorageKey = 'fakeTcpAuthToken';
const jsonHeaders = { 'Content-Type': 'application/json' };
let authToken = localStorage.getItem(authStorageKey) || '';

async function request(path, options = {}) {
  const response = await fetch(path, withAuth(options));
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    if (response.status === 401) {
      clearAuthToken();
    }
    throw new Error(body.message || body.error || `HTTP ${response.status}`);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function requestText(path, options = {}) {
  const response = await fetch(path, withAuth(options));
  if (!response.ok) {
    const body = await response.text().catch(() => '');
    if (response.status === 401) {
      clearAuthToken();
    }
    throw new Error(body || `HTTP ${response.status}`);
  }
  return response.text();
}

function withAuth(options = {}) {
  const headers = { ...(options.headers || {}) };
  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }
  return { ...options, headers };
}

function setAuthToken(token) {
  authToken = token || '';
  if (authToken) {
    localStorage.setItem(authStorageKey, authToken);
  } else {
    localStorage.removeItem(authStorageKey);
  }
}

function clearAuthToken() {
  setAuthToken('');
}

export const api = {
  hasAuthToken: () => Boolean(authToken),
  setAuthToken,
  clearAuthToken,
  login: (credentials) => request('/api/auth/login', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(credentials)
  }),
  me: () => request('/api/auth/me'),
  saveLastChannel: (channelId) => request('/api/auth/me/last-channel', {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ channelId })
  }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  listChannels: () => request('/api/channels'),
  previewPackager: (channelId) => request(`/api/channels/${channelId}/packager-preview`),
  previewPackagerDraft: (channel) => request('/api/channels/packager-preview', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(channel)
  }),
  getKeySettings: (channelId) => request(`/api/channels/${channelId}/keys`),
  saveKeySettings: (channelId, settings) => request(`/api/channels/${channelId}/keys`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(settings)
  }),
  listDebugEnvironments: (userId) => request(`/api/users/${userId}/debug-environments`),
  createDebugEnvironment: (userId, environment) => request(`/api/users/${userId}/debug-environments`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(environment)
  }),
  updateDebugEnvironment: (userId, environment) => request(`/api/users/${userId}/debug-environments/${environment.id}`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(environment)
  }),
  deleteDebugEnvironment: (userId, environmentId) => request(`/api/users/${userId}/debug-environments/${environmentId}`, {
    method: 'DELETE'
  }),
  listDebugCollections: (userId, environmentId) => request(`/api/users/${userId}/debug-collections?environmentId=${encodeURIComponent(environmentId)}`),
  createDebugCollection: (userId, collection) => request(`/api/users/${userId}/debug-collections`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(collection)
  }),
  updateDebugCollection: (userId, collection) => request(`/api/users/${userId}/debug-collections/${collection.id}`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(collection)
  }),
  deleteDebugCollection: (userId, collectionId) => request(`/api/users/${userId}/debug-collections/${collectionId}`, {
    method: 'DELETE'
  }),
  listDebugHistory: (userId, environmentId) => request(`/api/users/${userId}/debug-history?environmentId=${encodeURIComponent(environmentId)}`),
  createDebugHistory: (userId, history) => request(`/api/users/${userId}/debug-history`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(history)
  }),
  deleteDebugHistory: (userId, environmentId, historyId) => request(`/api/users/${userId}/debug-history/${historyId}?environmentId=${encodeURIComponent(environmentId)}`, {
    method: 'DELETE'
  }),
  clearDebugHistory: (userId, environmentId) => request(`/api/users/${userId}/debug-history?environmentId=${encodeURIComponent(environmentId)}`, {
    method: 'DELETE'
  }),
  listUsers: () => request('/api/users'),
  createUser: (user) => request('/api/users', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(user)
  }),
  listTokens: (userId) => request(`/api/users/${userId}/tokens`),
  createToken: (userId, name) => request(`/api/users/${userId}/tokens`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ name })
  }),
  revokeToken: (userId, tokenId) => request(`/api/users/${userId}/tokens/${tokenId}`, {
    method: 'DELETE'
  }),
  createChannel: (channel) => request('/api/channels', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(channel)
  }),
  updateChannel: (channelId, channel) => request(`/api/channels/${channelId}`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(channel)
  }),
  deleteChannel: (channelId) => request(`/api/channels/${channelId}`, {
    method: 'DELETE'
  }),
  listRules: (channelId, capability) => request(`/api/channels/${channelId}/rules${capability && capability !== 'ALL' ? `?capability=${encodeURIComponent(capability)}` : ''}`),
  createRule: (channelId, rule) => request(`/api/channels/${channelId}/rules`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(rule)
  }),
  updateRule: (channelId, rule) => request(`/api/channels/${channelId}/rules/${rule.id}`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(rule)
  }),
  deleteRule: (channelId, ruleId) => request(`/api/channels/${channelId}/rules/${ruleId}`, {
    method: 'DELETE'
  }),
  setRuleEnabled: (channelId, ruleId, enabled) => request(`/api/channels/${channelId}/rules/${ruleId}/${enabled ? 'enable' : 'disable'}`, {
    method: 'POST'
  }),
  setRulePublic: (channelId, ruleId, publicRule) => request(`/api/channels/${channelId}/rules/${ruleId}/${publicRule ? 'publish' : 'unpublish'}`, {
    method: 'POST'
  }),
  listPublicRules: (channelId) => request(`/api/channels/${channelId}/rules/public`),
  copyPublicRule: (channelId, ruleId) => request(`/api/channels/${channelId}/rules/public/${ruleId}/copy`, {
    method: 'POST'
  }),
  testRule: (channelId, message) => request(`/api/channels/${channelId}/rules/test`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(message)
  }),
  testRuleXml: (channelId, xml) => requestText(`/api/channels/${channelId}/rules/test-xml`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/xml', Accept: 'application/xml' },
    body: xml
  }),
  serializeWireRequest: (channelId, requestXml) => request(`/api/channels/${channelId}/tools/serialize`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ requestXml })
  }),
  deserializeWireResponse: (channelId, responseHex) => request(`/api/channels/${channelId}/tools/deserialize`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ responseHex })
  }),
  buildPosDebugXml: (channelId, payload) => request(`/api/channels/${channelId}/debug/pos/build-xml`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload)
  }),
  sendPosDebug: (channelId, payload) => request(`/api/channels/${channelId}/debug/pos/send`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload)
  }),
  tidInitPosDebug: (channelId, payload) => request(`/api/channels/${channelId}/debug/pos/tid-init`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload)
  }),
  listTestHistory: (userId, channelId) => request(`/api/users/${userId}/channels/${channelId}/test-history`),
  runTestHistory: (userId, channelId, requestXml) => request(`/api/users/${userId}/channels/${channelId}/test-history/run`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ requestXml })
  }),
  deleteTestHistory: (userId, channelId, historyId) => request(`/api/users/${userId}/channels/${channelId}/test-history/${historyId}`, {
    method: 'DELETE'
  }),
  clearTestHistory: (userId, channelId) => request(`/api/users/${userId}/channels/${channelId}/test-history`, {
    method: 'DELETE'
  }),
  listLogs: (channelId) => request(`/api/channels/${channelId}/logs?limit=100`)
};
