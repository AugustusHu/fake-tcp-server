<template>
  <section v-if="!authReady" class="login-screen">
    <div class="login-card">
      <div class="login-brand">
        <img class="faker-logo-mark" :src="brandLogoUrl" alt="" />
        <div>
          <strong>Faker</strong>
          <span>ISO8583 Mock Console</span>
        </div>
      </div>
      <p>正在恢复登录状态...</p>
    </div>
  </section>

  <section v-else-if="!currentUser" class="login-screen">
    <form class="login-card" @submit.prevent="login">
      <div class="login-brand">
        <img class="faker-logo-mark" :src="brandLogoUrl" alt="" />
        <div>
          <strong>Faker</strong>
          <span>ISO8583 Mock Console</span>
        </div>
      </div>
      <div class="login-copy">
        <h1>登录控制台</h1>
        <p>使用用户名和密码进入规则、渠道和规则验证管理。</p>
      </div>
      <label>
        <span>用户名</span>
        <input v-model="loginForm.username" autocomplete="username" autofocus required />
      </label>
      <label>
        <span>密码</span>
        <input v-model="loginForm.password" autocomplete="current-password" type="password" required />
      </label>
      <div v-if="loginError" class="login-error">{{ loginError }}</div>
      <button class="primary login-submit" type="submit" :disabled="loginLoading">
        <KeyRound :size="16" />
        {{ loginLoading ? '登录中...' : '登录' }}
      </button>
    </form>
  </section>

  <div v-else class="app-shell">
    <header class="app-primary-header">
      <div class="brand app-brand">
        <img class="faker-logo-mark compact" :src="brandLogoUrl" alt="" />
        <div>
          <strong>Faker</strong>
          <span>ISO8583 Workbench</span>
        </div>
      </div>

      <nav class="app-module-nav" aria-label="一级功能">
        <button type="button" :class="{ active: activeModule === 'channel' }" @click="openChannelModule">
          <Server :size="14" />
          渠道配置
        </button>
        <button type="button" :class="{ active: activeModule === 'mock' }" @click="openMockModule">
          <ListChecks :size="14" />
          Mock工具
        </button>
        <button type="button" :class="{ active: activeModule === 'debug' }" @click="openDebugModule">
          <Network :size="14" />
          调试工具
        </button>
        <button type="button" :class="{ active: activeModule === 'system' }" @click="openSystemModule">
          <Users :size="14" />
          系统
        </button>
      </nav>

      <div class="global-actions">
        <button v-if="selectedChannel" class="top-channel-switch" type="button" title="切换渠道" @click="openChannelSwitcher">
          <span>{{ channelDisplayCode(selectedChannel) }}</span>
          <RefreshCw :size="12" />
        </button>
        <button class="top-icon-button info-entry-button" type="button" title="查看使用说明" @click="openInfoCenter">
          <Info :size="14" />
          <span class="visually-hidden">信息</span>
        </button>
        <div class="account-menu" :class="{ open: accountMenuOpen }">
          <button
            class="account-trigger"
            type="button"
            :title="currentUser ? `当前用户：${currentUser.username}` : '当前用户'"
            :aria-expanded="accountMenuOpen"
            @click="accountMenuOpen = !accountMenuOpen"
          >
            <UserCircle :size="14" />
            <span class="account-username">{{ currentUser.username }}</span>
            <ChevronDown :size="12" />
          </button>
          <div v-if="accountMenuOpen" class="account-dropdown" role="menu">
            <button type="button" role="menuitem" @click="openCurrentUserProfile">
              <UserCircle :size="16" />
              个人信息
            </button>
            <button type="button" role="menuitem" @click="logout">
              <LogOut :size="16" />
              退出登录
            </button>
          </div>
        </div>
      </div>
    </header>

    <aside class="secondary-panel">
      <template v-if="activeModule === 'debug'">
        <section class="collection-sidebar" aria-label="Collections">
          <div class="collection-header">
            <div>
              <strong>Collections</strong>
              <span>{{ debugCollections.length }} 组</span>
            </div>
            <button type="button" title="新增 Collection" @click="createDebugCollection">
              <Plus :size="14" />
            </button>
          </div>
          <div class="collection-tree">
            <article v-for="collection in debugCollections" :key="collection.id" class="collection-group">
              <div class="collection-title" :class="{ active: activeDebugCollectionId === collection.id }">
                <div
                  class="collection-select"
                  role="button"
                  tabindex="0"
                  @click="selectDebugCollection(collection.id)"
                  @keydown.enter.prevent="selectDebugCollection(collection.id)"
                >
                  <ChevronRight :size="13" />
                  <input
                    v-if="editingDebugCollectionId === collection.id"
                    v-model="collection.name"
                    class="collection-name-input"
                    autofocus
                    @click.stop
                    @blur="commitDebugCollectionName(collection)"
                    @keydown.enter.prevent="$event.target.blur()"
                  />
                  <span v-else>{{ collection.name }}</span>
                  <small>{{ collection.requests.length }}</small>
                </div>
                <button type="button" title="编辑 Collection 名称" @click.stop="startEditingDebugCollection(collection)">
                  <Pencil :size="12" />
                </button>
                <button type="button" title="删除 Collection" @click.stop="deleteDebugCollection(collection)">
                  <Trash2 :size="12" />
                </button>
              </div>
              <div
                v-for="request in collection.requests"
                :key="request.id"
                class="request-node"
                :class="{ active: activeDebugRequestId === request.id }"
              >
                <div
                  class="request-select"
                  role="button"
                  tabindex="0"
                  @click="loadDebugRequest(collection.id, request.id)"
                  @keydown.enter.prevent="loadDebugRequest(collection.id, request.id)"
                >
                  <span class="method-chip">POS</span>
                  <input
                    v-if="editingDebugRequestId === request.id"
                    v-model="request.name"
                    class="request-name-input"
                    autofocus
                    @click.stop
                    @blur="commitDebugRequestName(request)"
                    @keydown.enter.prevent="$event.target.blur()"
                  />
                  <span v-else>{{ request.name }}</span>
                </div>
                <button type="button" title="编辑 Request 名称" @click.stop="startEditingDebugRequest(request)">
                  <Pencil :size="12" />
                </button>
                <button type="button" title="删除 Request" @click.stop="deleteDebugRequest(collection, request)">
                  <Trash2 :size="12" />
                </button>
              </div>
              <button class="request-node add-request-node" type="button" @click="newDebugRequestInCollection(collection.id)">
                <Plus :size="13" />
                New Request
              </button>
            </article>
          </div>
        </section>

        <section v-if="selectedChannel" class="environment-panel" aria-label="当前环境">
          <div class="panel-kicker">Environment</div>
          <div class="environment-control-row">
            <select class="environment-selector" v-model="activeEnvironmentId" @change="selectDebugEnvironment(activeEnvironmentId)">
              <option v-for="environment in debugEnvironments" :key="environment.id" :value="environment.id">
                {{ environment.name }}
              </option>
            </select>
            <button type="button" title="新建环境" @click="createDebugEnvironment">
              <Plus :size="13" />
            </button>
            <button type="button" title="编辑当前环境" :disabled="!activeEnvironment" @click="editActiveDebugEnvironment">
              <Pencil :size="13" />
            </button>
          </div>
          <div class="environment-summary">
            <span>{{ environmentVariableCount }} variables · {{ debugChannelCode }}</span>
          </div>
          <div class="environment-variable-groups">
            <section v-for="group in environmentVariableGroups" :key="group.title" class="environment-variable-group">
              <header>
                <span>{{ group.title }}</span>
                <small>{{ group.items.length }}</small>
              </header>
              <div class="environment-variable-list">
                <div v-for="variable in group.items" :key="`${group.title}-${variable.key}`">
                  <code>{{ variable.key }}</code>
                  <span>{{ variable.value || '-' }}</span>
                </div>
              </div>
            </section>
          </div>
        </section>

      </template>

      <template v-else>
      <div class="module-context">
        <strong>{{ moduleTitle }}</strong>
        <span>{{ moduleSubtitle }}</span>
      </div>

      <nav class="side-menu" aria-label="模块菜单">
        <template v-if="activeModule === 'channel'">
          <button class="menu-button" :class="{ active: tab === 'channel-new' }" @click="openChannelSettings">
            <Server :size="17" />
            <span>
              <strong>渠道配置</strong>
              <small>全局数据</small>
            </span>
          </button>
          <button class="menu-button" :class="{ active: tab === 'keys' }" @click="openKeys">
            <KeyRound :size="17" />
            <span>
              <strong>Key设置</strong>
              <small>全局数据</small>
            </span>
          </button>
          <button class="menu-button" :class="{ active: tab === 'packager-preview' }" @click="openPackagerPreview">
            <Cpu :size="17" />
            <span>
              <strong>Packager预览</strong>
              <small>字段定义</small>
            </span>
          </button>
        </template>

        <template v-else-if="activeModule === 'mock'">
          <button class="menu-button" :class="{ active: tab === 'rules' }" @click="openRules">
            <ListChecks :size="17" />
            <span>
              <strong>规则管理</strong>
              <small>用户数据</small>
            </span>
          </button>
          <button class="menu-button" :class="{ active: isValidationTab }" @click="openTest">
            <FlaskConical :size="17" />
            <span>
              <strong>规则验证</strong>
              <small>用户数据</small>
            </span>
          </button>
          <button class="menu-button" :class="{ active: tab === 'public-rules' }" @click="openPublicRules">
            <Share2 :size="17" />
            <span>
              <strong>公开规则</strong>
              <small>全局共享</small>
            </span>
          </button>
        </template>

        <template v-else>
          <button class="menu-button" :class="{ active: tab === 'user-detail' && selectedUser?.id === currentUser?.id }" @click="openCurrentUserProfile">
            <UserCircle :size="17" />
            <span>
              <strong>个人信息</strong>
              <small>MCP Token</small>
            </span>
          </button>
          <button v-if="isAdminUser" class="menu-button" :class="{ active: tab === 'users' }" @click="openUsers">
            <Users :size="17" />
            <span>
              <strong>用户管理</strong>
              <small>用户列表</small>
            </span>
          </button>
          <button class="menu-button" @click="openInfoCenter">
            <Info :size="17" />
            <span>
              <strong>信息中心</strong>
              <small>MCP / ISO8583</small>
            </span>
          </button>
        </template>
      </nav>
      </template>

    </aside>

    <main class="workspace" v-if="selectedChannel || tab === 'channel-new' || isUserTab">
      <header class="topbar">
        <div>
          <h1 class="page-heading">
            <span>{{ pageTitle }}</span>
            <small v-if="pageScopeLabel" class="scope-pill" :class="pageScopeClass">{{ pageScopeLabel }}</small>
          </h1>
          <p>{{ pageSubtitle }}</p>
        </div>
      </header>

      <section v-if="tab === 'rules' && ruleView === 'list'" class="single-panel rule-list-page">
        <div class="workbench-toolbar rule-list-toolbar">
          <div class="toolbar-meta">
            <strong>{{ selectedCapabilityFilter === 'ALL' ? '全部能力' : capabilityLabel(selectedCapabilityFilter) }}</strong>
            <span>{{ filteredRules.length }} 条 · {{ enabledFilteredRuleCount }}/{{ filteredRules.length }} 已启用</span>
          </div>
          <button class="primary" type="button" @click="newRule">
            <Plus :size="17" />
            新增规则
          </button>
        </div>

        <div class="capability-filter" aria-label="按能力筛选规则">
          <button type="button" :class="{ active: selectedCapabilityFilter === 'ALL' }" @click="selectedCapabilityFilter = 'ALL'">
            全部
          </button>
          <button
            v-for="capability in capabilityOptions"
            :key="capability.value"
            type="button"
            :class="{ active: selectedCapabilityFilter === capability.value }"
            @click="selectedCapabilityFilter = capability.value"
          >
            {{ capability.label }}
          </button>
        </div>

        <div class="data-list rule-data-list">
          <div class="data-list-header rule-data-row">
            <span>规则</span>
            <span>能力</span>
            <span>条件与结果</span>
            <span>操作</span>
          </div>
          <article
            v-for="rule in filteredRules"
            :key="rule.id"
            class="data-list-row rule-data-row"
            :class="{ selected: editingRule?.id === rule.id }"
            @click="editRule(rule)"
          >
            <div class="rule-name-cell">
              <div class="rule-title">
                <span class="status-dot" :class="{ on: rule.enabled }"></span>
                <strong>{{ rule.name }}</strong>
              </div>
              <div class="rule-meta">
                <code>#{{ rule.id }}</code>
                <code>P{{ rule.priority }}</code>
                <code>{{ rule.matchMode }}</code>
                <code>{{ rule.action.type }}</code>
                <code v-if="rule.publicRule">PUBLIC</code>
              </div>
            </div>
            <div class="rule-capability-cell">
              <span class="capability-badge capability-badge-standalone">{{ capabilityLabel(rule.capability) }}</span>
            </div>
            <div class="rule-flow">
              <div class="rule-flow-part">
                <strong>if</strong>
                <em>
                  <span
                    v-for="(segment, index) in isoFieldSegments(ruleIfSummary(rule))"
                    :key="`if-${rule.id}-${index}`"
                    :class="{ 'iso-field-token': segment.field }"
                  >{{ segment.text }}</span>
                </em>
              </div>
              <div class="rule-flow-arrow">then</div>
              <div class="rule-flow-part then-part">
                <strong class="iso-field-token">DE39</strong>
                <em>{{ ruleThenSummary(rule) }}</em>
              </div>
            </div>
            <div class="row-actions">
              <button
                class="status-toggle"
                :class="rule.enabled ? 'is-active' : 'is-inactive'"
                :title="rule.enabled ? '点击停用' : '点击启用'"
                :aria-label="rule.enabled ? '点击停用' : '点击启用'"
                :aria-pressed="rule.enabled"
                @click.stop="toggleRule(rule)"
              >
                <Power :size="16" />
              </button>
              <button title="复制" @click.stop="duplicateRule(rule)">
                <Copy :size="16" />
              </button>
              <button :title="rule.publicRule ? '取消公开' : '公开'" @click.stop="togglePublicRule(rule)">
                <Share2 :size="16" :class="{ muted: !rule.publicRule }" />
              </button>
              <button title="删除" @click.stop="removeRule(rule)">
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
          <div v-if="filteredRules.length === 0" class="empty-state">
            <ListChecks :size="22" />
            <strong>暂无规则</strong>
            <span>为 {{ channelDisplayCode(selectedChannel) }} 新增{{ selectedCapabilityFilter === 'ALL' ? '' : capabilityLabel(selectedCapabilityFilter) }}规则。</span>
          </div>
        </div>
      </section>

      <section v-if="tab === 'rules' && ruleView === 'detail'" class="rule-detail-page">
        <form class="editor rule-detail-editor" @submit.prevent="saveRule">
          <div class="editor-header">
            <div>
              <h2>{{ editingRule.id ? '规则详情' : '新增规则' }}</h2>
              <p>{{ editingRule.id ? `#${editingRule.id}` : '草稿' }}</p>
            </div>
            <div class="topbar-actions">
              <button class="ghost" type="button" title="返回列表" @click="backToRuleList">
                <ArrowLeft :size="16" />
                返回
              </button>
              <button class="primary" type="submit">
                <Save :size="16" />
                保存
              </button>
            </div>
          </div>

          <div class="form-nav">
            <button type="button" :class="{ active: ruleSection === 'match' }" @click="ruleSection = 'match'">匹配</button>
            <button type="button" :class="{ active: ruleSection === 'behavior' }" @click="ruleSection = 'behavior'">动作与响应</button>
          </div>

          <div v-if="ruleSection === 'match'" class="form-section">
            <div class="rule-meta-strip">
              <label class="rule-name-field">
                <span>规则名称</span>
                <input v-model="editingRule.name" required />
              </label>
              <label class="rule-priority-field">
                <span>优先级</span>
                <input type="number" v-model.number="editingRule.priority" />
              </label>
              <label class="switch-field compact-switch">
                <input type="checkbox" v-model="editingRule.enabled" />
                <span>启用</span>
              </label>
            </div>

            <div class="rule-work-panel trigger-panel">
              <div class="rule-panel-title">
                <strong>交易匹配</strong>
                <span>能力前提始终 AND，字段值允许按渠道调整。</span>
              </div>
              <div class="trigger-grid">
                <label>
                  <span>交易类型</span>
                  <select :value="editingRule.capability" @change="setRuleCapability($event.target.value)">
                    <option v-for="capability in capabilityOptions" :key="capability.value" :value="capability.value">
                      {{ capability.label }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>请求 MTI</span>
                  <input
                    :value="systemConditionInputValue('0')"
                    maxlength="4"
                    placeholder="例如 0200"
                    @input="setSystemConditionValue('0', $event.target.value)"
                  />
                </label>
                <label>
                  <span>DE3 Process Code</span>
                  <input
                    :value="systemConditionInputValue('3')"
                    maxlength="6"
                    placeholder="例如 000000"
                    @input="setSystemConditionValue('3', $event.target.value)"
                  />
                </label>
              </div>
            </div>

            <div class="rule-work-panel filters-panel">
              <div class="rule-panel-title">
                <strong>过滤条件</strong>
                <div class="compact-choice-group" role="group" aria-label="条件关系">
                  <span>满足</span>
                  <button type="button" :class="{ active: editingRule.matchMode === 'ALL' }" @click="editingRule.matchMode = 'ALL'">全部 ALL</button>
                  <button type="button" :class="{ active: editingRule.matchMode === 'ANY' }" @click="editingRule.matchMode = 'ANY'">任一 ANY</button>
                </div>
              </div>
              <div class="rule-table filter-table">
                <div class="rule-table-head">
                  <span>域</span>
                  <span>类型</span>
                  <span>条件</span>
                  <span>目标值</span>
                  <span>操作</span>
                </div>
                <div class="rule-table-row condition-line" v-for="(condition, index) in editingRule.conditions" :key="index">
                  <label class="field-ref-input">
                    <input placeholder="3 或 DE3" v-model="condition.field" @blur="normalizeConditionField(condition)" />
                    <small v-if="condition.field">{{ formatFieldLabel(condition.field) }}</small>
                  </label>
                  <div class="field-type-chip" :class="conditionFieldValueType(condition).toLowerCase()">
                    <strong>{{ conditionFieldTypeLabel(condition) }}</strong>
                    <span>{{ conditionFieldHint(condition) }}</span>
                  </div>
                  <label class="operator-field">
                    <select v-model="condition.operator" @change="ensureConditionOperator(condition)">
                      <option v-for="op in conditionOperatorOptions(condition)" :key="op.value" :value="op.value">
                        {{ op.label }}
                      </option>
                    </select>
                  </label>
                  <label v-if="conditionNeedsValue(condition.operator)" class="condition-value-field">
                    <input
                      :inputmode="conditionInputMode(condition)"
                      :placeholder="conditionValuePlaceholder(condition)"
                      v-model="condition.value"
                    />
                  </label>
                  <div v-else class="linked-hint condition-value-field">
                    无需填写
                  </div>
                  <button type="button" title="移除" @click="editingRule.conditions.splice(index, 1)">
                    <X :size="16" />
                  </button>
                </div>
                <div v-if="editingRule.conditions.length === 0" class="rule-table-empty">
                  未设置附加过滤条件，仅按能力前提匹配。
                </div>
              </div>
              <div class="rule-panel-footer">
                <button type="button" class="secondary" @click="addCondition">
                  <Plus :size="16" />
                  添加过滤字段
                </button>
                <span>数值字段会先转换为数字再比较，例如 DE4 的 000020000 按 20000 判断。</span>
              </div>
            </div>
          </div>

          <div v-if="ruleSection === 'behavior'" class="form-section">
            <div class="rule-work-panel action-panel">
              <div class="rule-panel-title">
                <strong>动作</strong>
                <span>{{ actionSummary }}</span>
              </div>
              <div class="action-editor-grid">
                <label>
                  <span>命中动作</span>
                  <select v-model="editingRule.action.type">
                    <option v-for="action in actionOptions" :key="action.type" :value="action.type">
                      {{ action.label }}
                    </option>
                  </select>
                </label>
                <label v-if="actionUsesDelay">
                  <span>延迟毫秒</span>
                  <input type="number" v-model.number="editingRule.action.delayMs" />
                </label>
                <label v-if="actionReturnsResponse">
                  <span>响应 MTI</span>
                  <input v-model="editingRule.response.mti" maxlength="4" placeholder="例如 0210" />
                </label>
              </div>
              <div v-if="!actionReturnsResponse" class="notice muted-notice compact-notice">
                <AlertTriangle :size="17" />
                <span>当前动作不会返回 ISO 报文，因此不需要配置响应字段。</span>
              </div>
            </div>

            <div v-if="actionReturnsResponse" class="rule-work-panel response-panel">
              <div class="rule-panel-title">
                <strong>响应字段</strong>
                <div class="segmented-control compact-segmented">
                  <button type="button" :class="{ active: responseInputMode === 'fields' }" @click="responseInputMode = 'fields'">键值对</button>
                  <button type="button" :class="{ active: responseInputMode === 'xml' }" @click="responseInputMode = 'xml'">XML 导入</button>
                </div>
              </div>
              <div v-if="responseInputMode === 'xml'" class="response-import">
                <CodeEditor class="xml-import-textarea" language="xml" v-model="responseXmlImport" placeholder="<isomsg>...</isomsg>" />
                <div class="form-line">
                  <button type="button" class="secondary" @click="importResponseXml">
                    <Upload :size="16" />
                    导入 XML
                  </button>
                  <label class="file-picker compact-picker">
                    <Upload :size="16" />
                    <span>上传 XML</span>
                    <input type="file" accept=".xml,text/xml" @change="readResponseXmlFile" />
                  </label>
                </div>
              </div>
              <div v-if="responseInputMode === 'fields'" class="rule-table response-table">
                <div class="rule-table-head">
                  <span>域</span>
                  <span>返回值</span>
                  <span>操作</span>
                </div>
                <div class="rule-table-row kv-line" v-for="field in responseRows" :key="field.key">
                  <label class="field-ref-input">
                    <input placeholder="39 或 DE39" v-model="field.key" @change="normalizeResponseRow(field)" />
                    <small v-if="field.key">{{ formatFieldLabel(field.key) }}</small>
                  </label>
                  <input placeholder="字段值" v-model="field.value" @input="syncResponseFields" />
                  <button type="button" title="移除" @click="removeResponseRow(field)">
                    <X :size="16" />
                  </button>
                </div>
                <div v-if="responseRows.length === 0" class="rule-table-empty">
                  未设置响应覆盖字段，默认沿用请求字段。
                </div>
              </div>
              <button v-if="responseInputMode === 'fields'" type="button" class="secondary" @click="addResponseRow">
                <Plus :size="16" />
                添加响应字段
              </button>
            </div>
          </div>
        </form>
      </section>

      <section v-if="tab === 'keys'" class="key-settings-page single-panel">
        <div class="workbench-toolbar key-settings-toolbar">
          <div class="toolbar-meta">
            <strong>Key 设置</strong>
            <span>{{ channelDisplayCode(selectedChannel) }} · 全局配置 · 保存后热生效</span>
          </div>
          <button class="primary" type="button" :disabled="keyLoading" @click="saveKeySettings">
            <Save :size="17" />
            保存
          </button>
        </div>

        <div class="key-settings-grid">
          <section class="form-card span-2">
            <div class="form-card-header">
              <strong>明文密钥 <small class="effect-badge hot">热生效</small></strong>
              <span>允许配置人员直接维护明文 HEX；TPK 先入库保留，TSK 用于 MAC 计算。</span>
            </div>
            <div class="field-grid compact-grid">
              <label>
                <span>TPK 明文</span>
                <input v-model="keySettings.tpkPlain" spellcheck="false" placeholder="16、32 或 48 位 HEX，可为空" />
              </label>
              <label>
                <span>TSK 明文</span>
                <input v-model="keySettings.tskPlain" spellcheck="false" placeholder="16、32 或 48 位 HEX；为空则不启用 MAC" />
              </label>
            </div>
            <div class="choice-card-grid mac-algorithm-grid" role="radiogroup" aria-label="MAC 算法">
              <button
                  v-for="option in macAlgorithmOptions"
                  :key="option.value"
                  type="button"
                  class="choice-card"
                  :class="{ active: keySettings.macAlgorithm === option.value }"
                  @click="keySettings.macAlgorithm = option.value"
                >
                <strong>{{ option.label }}</strong>
                <span>{{ option.description }}</span>
              </button>
            </div>
          </section>

          <section class="form-card">
            <div class="form-card-header">
              <strong>MAC 字段判定 <small class="effect-badge hot">热生效</small></strong>
              <span>不需要人工选择；系统会按请求 bitmap、XML bitmap hint 和交易能力自动判断。</span>
            </div>
            <div class="mac-field-policy-list">
              <div>
                <strong>Bitmap 优先</strong>
                <span>请求已经带 DE64 或 DE128 时，验签和响应签名沿用实际 MAC 域。</span>
              </div>
              <div>
                <strong>默认兜底</strong>
                <span>参数下载、Callhome 默认 DE64；其它非 TID 初始化交易默认 DE128。</span>
              </div>
              <div>
                <strong>SHA-256 Field128</strong>
                <span>启用后强制使用 DE128；缺失时先补 64 个 0，pack 后裁掉最后 64 bytes 再计算。</span>
              </div>
            </div>
          </section>

          <section class="form-card span-2">
            <div class="form-card-header">
              <strong>测试交易参数 <small class="effect-badge hot">热生效</small></strong>
              <span>用于真实第三方测试环境联调时快速复用，不参与当前 Mock 规则匹配。</span>
            </div>
            <div class="field-grid compact-grid">
              <label>
                <span>测试 TID / DE41</span>
                <input v-model="keySettings.testTid" maxlength="8" placeholder="例如 TERM0001" />
              </label>
              <label>
                <span>测试 PAN / DE2</span>
                <input v-model="keySettings.testPan" inputmode="numeric" maxlength="19" placeholder="测试卡号" />
              </label>
              <label>
                <span>卡号有效期 / DE14</span>
                <input v-model="keySettings.testDe14" inputmode="numeric" maxlength="4" placeholder="YYMM，例如 3002" />
              </label>
              <label>
                <span>测试 PIN 信息</span>
                <input v-model="keySettings.testPin" placeholder="PIN 或 PIN Block 信息" />
              </label>
              <label>
                <span>DE42 商户号</span>
                <input v-model="keySettings.testDe42" maxlength="15" placeholder="15 位以内" />
              </label>
              <label>
                <span>DE18 MCC</span>
                <input v-model="keySettings.testDe18" inputmode="numeric" maxlength="4" placeholder="例如 6012" />
              </label>
              <label>
                <span>DE43 商户名称/地址</span>
                <input v-model="keySettings.testDe43" maxlength="40" placeholder="40 位以内" />
              </label>
              <label>
                <span>DE49 币种</span>
                <input v-model="keySettings.testDe49" inputmode="numeric" maxlength="3" placeholder="例如 566" />
              </label>
            </div>
          </section>
        </div>

        <div class="mac-policy-panel">
          <strong>默认 MAC 策略</strong>
          <span class="tooltip-trigger" tabindex="0" data-tooltip="TSK 为空：不处理 MAC。TSK 有值：TID-TMK / TID-TPK / TID-TSK 不需要 MAC；其它能力需要 MAC。ANSI X9.19 按 bitmap/能力判定 MAC 域；SHA-256 Field128 固定使用 DE128。">?</span>
        </div>
      </section>

      <section v-if="tab === 'test'" class="validation-page">
        <div class="validation-header page-card">
          <div>
            <h2>一次性验证</h2>
            <p>直接粘贴 XML 后运行验证；每次运行会进入最近 30 条历史记录。</p>
          </div>
          <div class="validation-actions">
            <button class="secondary" type="button" @click="openValidationHistory">
              <ListChecks :size="16" />
              查看历史记录
            </button>
          </div>
        </div>

        <div class="data-scope-callout user">
          <strong>用户数据</strong>
          <span>规则验证历史只保存当前登录用户最近 30 条；序列化/反序列化工具使用全局渠道和 Key 配置。</span>
        </div>

        <section class="validation-workspace validation-single page-card">
          <div class="validation-toolbar">
            <div>
              <h2>请求与响应</h2>
              <span class="tooltip-trigger" tabindex="0" data-tooltip="未填写的响应字段默认沿用请求；MTI 默认自动配对，适合快速验证规则调整。">?</span>
            </div>
            <div class="validation-actions">
              <button type="button" class="primary" @click="runTest">
                <Play :size="16" />
                运行
              </button>
            </div>
          </div>

          <div class="validation-panels">
            <label class="validation-pane">
              <span>请求报文 XML</span>
              <CodeEditor language="xml" v-model="testXml" />
            </label>
            <div class="validation-pane">
              <div class="validation-pane-title">
                <span>验证结果</span>
              </div>
              <CodeBlock language="xml" :code="testResult" placeholder="运行后显示响应 XML。" />
            </div>
          </div>
        </section>

        <section class="validation-tool-grid">
          <div class="page-card validation-tool-card">
            <div class="validation-toolbar">
              <div>
                <h2>请求序列化工具</h2>
                <span class="tooltip-trigger" tabindex="0" data-tooltip="将左侧 XML 序列化为完整 TCP Frame HEX；配置 TSK 后会按 bitmap/能力自动补 MAC。">?</span>
              </div>
              <button type="button" class="secondary" @click="serializeWireRequest">
                <RefreshCw :size="16" />
                序列化
              </button>
            </div>
            <div class="wire-result-grid" v-if="wirePreview">
              <div class="wire-summary">
                <code>{{ wirePreview.macRequired ? `MAC ${formatFieldLabel(wirePreview.macField)}=${wirePreview.macValue || '-'}` : 'MAC skipped' }}</code>
                <span>{{ wirePreview.macRequired ? '已使用 TSK 按当前 MAC 算法签名。' : '当前请求不需要 MAC，或未配置 TSK。' }}</span>
              </div>
              <div class="wire-send-hint">
                <strong>发送提示</strong>
                <span>{{ wirePreview.sendHint || `将完整 Frame HEX 转为二进制后，通过任意 TCP 客户端发送到 ${wirePreview.targetHost || '127.0.0.1'}:${wirePreview.targetPort || selectedChannel.port}。` }}</span>
              </div>
              <label>
                <span>完整 Frame HEX（可发送）</span>
                <textarea class="hex-textarea" v-model="wirePreview.frameHex" spellcheck="false"></textarea>
              </label>
              <div class="wire-hex-grid">
                <label>
                  <span>Payload HEX（去掉长度前缀）</span>
                  <textarea class="hex-textarea compact-hex-textarea" v-model="wirePreview.payloadHex" spellcheck="false"></textarea>
                </label>
                <label>
                  <span>ISO Body HEX（去掉 Header/TPDU）</span>
                  <textarea class="hex-textarea compact-hex-textarea" v-model="wirePreview.isoHex" spellcheck="false"></textarea>
                </label>
              </div>
              <label>
                <span>补齐 MAC 后的 XML</span>
                <CodeEditor class="xml-preview-textarea" language="xml" v-model="wirePreview.requestXml" />
              </label>
            </div>
            <div v-else class="empty-inline">序列化后展示完整 Frame HEX、Payload HEX 和发送提示。</div>
          </div>

          <div class="page-card validation-tool-card">
            <div class="validation-toolbar">
              <div>
                <h2>响应反序列化工具</h2>
                <span class="tooltip-trigger" tabindex="0" data-tooltip="粘贴收到的响应 HEX，按当前渠道 Framing、Header/TPDU 和 Packager 转回 XML。">?</span>
              </div>
              <button type="button" class="secondary" @click="deserializeWireResponse">
                <Upload :size="16" />
                反序列化
              </button>
            </div>
            <label>
              <span>响应 Frame HEX 或 Payload HEX</span>
              <textarea class="hex-textarea" v-model="responseHexInput" spellcheck="false" placeholder="粘贴收到的响应 HEX；可以是完整 Frame，也可以是去掉长度前缀后的 Payload"></textarea>
            </label>
            <label>
              <span>解析结果 XML</span>
              <CodeEditor class="xml-preview-textarea" language="xml" :model-value="parsedResponseXml" placeholder="解析后显示 XML。" readonly />
            </label>
          </div>
        </section>
      </section>

      <section v-if="tab === 'test-history'" class="validation-page">
        <div class="validation-header page-card">
          <div>
            <h2>历史记录</h2>
            <p>{{ channelDisplayCode(selectedChannel) }} 最近 30 条规则验证记录，新的运行会自动排在最前面。</p>
          </div>
          <div class="validation-actions">
            <button class="ghost" type="button" @click="openTest">
              <ArrowLeft :size="16" />
              一次性验证
            </button>
            <button class="secondary" type="button" @click="loadTestHistory">
              <RefreshCw :size="16" />
              刷新
            </button>
            <button class="secondary" type="button" :disabled="testHistory.length === 0" @click="clearTestHistory">
              <Trash2 :size="16" />
              清空历史
            </button>
          </div>
        </div>

        <div class="data-scope-callout user">
          <strong>用户数据</strong>
          <span>这里只展示当前登录用户的规则验证历史，不会混入其他用户记录。</span>
        </div>

        <section class="single-panel test-history-list-page">
          <div class="data-list test-history-data-list">
            <div class="data-list-header test-history-data-row">
              <span>时间</span>
              <span>结果</span>
              <span>命中规则</span>
              <span>操作</span>
            </div>
            <article
              v-for="history in testHistory"
              :key="history.id"
              class="data-list-row test-history-data-row"
              @click="openTestHistoryDetail(history)"
            >
              <div class="rule-name-cell">
                <strong>{{ formatDateTime(history.createdAt) }}</strong>
                <small>#{{ history.id }} · {{ channelDisplayCode(selectedChannel) }}</small>
              </div>
              <span class="history-status" :class="{ failed: !history.success }">
                {{ history.success ? '成功' : '失败' }}
              </span>
              <span>{{ historyRuleLabel(history) }}</span>
              <div class="row-actions">
                <button type="button" title="查看详情" @click.stop="openTestHistoryDetail(history)">
                  <Eye :size="16" />
                </button>
                <button type="button" title="载入到一次性验证" @click.stop="loadTestHistoryEntry(history)">
                  <Upload :size="16" />
                </button>
                <button type="button" title="删除历史" @click.stop="deleteTestHistory(history)">
                  <Trash2 :size="16" />
                </button>
              </div>
            </article>
            <div v-if="testHistory.length === 0" class="empty-state">
              <ListChecks :size="22" />
              <strong>暂无历史记录</strong>
              <span>运行一次规则验证后，会自动保存到这里。</span>
            </div>
          </div>
        </section>
      </section>

      <section v-if="tab === 'test-history-detail'" class="validation-page">
        <section class="editor test-history-detail-page">
          <div class="editor-header">
            <div>
              <h2>历史详情</h2>
              <p>{{ selectedTestHistory ? `${formatDateTime(selectedTestHistory.createdAt)} · ${historyRuleLabel(selectedTestHistory)}` : '未选择历史记录' }}</p>
            </div>
            <div class="topbar-actions">
              <button class="ghost" type="button" @click="openValidationHistory">
                <ArrowLeft :size="16" />
                返回列表
              </button>
              <button class="secondary" type="button" :disabled="!selectedTestHistory" @click="loadTestHistoryEntry(selectedTestHistory)">
                <Upload :size="16" />
                载入验证
              </button>
              <button class="secondary" type="button" :disabled="!selectedTestHistory" @click="deleteTestHistory(selectedTestHistory)">
                <Trash2 :size="16" />
                删除
              </button>
            </div>
          </div>

          <div class="detail-summary-grid" v-if="selectedTestHistory">
            <div class="detail-summary-item">
              <span>渠道</span>
              <strong>{{ channelDisplayCode(selectedChannel) }}</strong>
            </div>
            <div class="detail-summary-item">
              <span>验证结果</span>
              <strong>{{ selectedTestHistory.success ? '成功' : '失败' }}</strong>
            </div>
            <div class="detail-summary-item">
              <span>命中规则</span>
              <strong>{{ historyRuleLabel(selectedTestHistory) }}</strong>
            </div>
          </div>

          <div class="form-section" v-if="selectedTestHistory">
            <div class="section-title">
              <h2>请求与响应</h2>
              <span class="tooltip-trigger" tabindex="0" data-tooltip="载入会把请求 XML 放回一次性验证页，方便基于历史重新调整。">?</span>
            </div>
            <div class="validation-panels history-detail-panels">
              <div class="validation-pane">
                <div class="validation-pane-title">
                  <span>请求 XML</span>
                </div>
                <CodeBlock language="xml" :code="selectedTestHistory.requestXml" />
              </div>
              <div class="validation-pane">
                <div class="validation-pane-title">
                  <span>{{ selectedTestHistory.success ? '响应 XML' : '错误信息' }}</span>
                </div>
                <CodeBlock :language="selectedTestHistory.success ? 'xml' : 'text'" :code="selectedTestHistory.success ? selectedTestHistory.responseXml : selectedTestHistory.errorMessage" />
              </div>
            </div>
          </div>
        </section>
      </section>

      <section v-if="tab === 'debug-pos'" class="debug-page debug-pos-page">
        <div v-if="!debugTargetConfigured" class="notice inline-notice">
          <AlertTriangle :size="17" />
          <span v-pre>TCP 地址栏还没有有效 IP:Port；可直接填写，也可使用 {{target.ip}}:{{target.port}}。</span>
        </div>

        <div class="debug-command-bar">
          <div class="debug-target-urlbar" aria-label="TCP 目标地址">
            <span class="debug-protocol-chip">TCP</span>
            <div class="debug-target-input-shell">
              <input
                v-model="debugTargetInput"
                :placeholder="'{{target.ip}}:{{target.port}}'"
                spellcheck="false"
                :title="debugResolvedTargetAddress ? `解析为 ${debugResolvedTargetAddress}` : '支持 Environment 变量，例如 {{target.ip}}:{{target.port}}'"
                @blur="normalizeDebugTargetInput"
              />
            </div>
            <button class="primary" type="button" :disabled="debugLoading || !debugTargetConfigured" @click="runPosDebug">
              <Play :size="16" />
              发送
            </button>
          </div>
          <div class="debug-command-actions">
            <div class="debug-toolbar-buttons" aria-label="Debug 工具">
              <button class="secondary" type="button" @click="openDebugToolModal('xml')">
                <RefreshCw :size="15" />
                XML 生成
              </button>
              <button class="secondary" type="button" @click="openDebugToolModal('tid')">
                <KeyRound :size="15" />
                TID 初始化
              </button>
              <button class="secondary" type="button" @click="openSmartReplacePreview">
                <RefreshCw :size="15" />
                智能替换
              </button>
              <button class="secondary" type="button" @click="saveCurrentDebugRequestToCollection">
                <Save :size="15" />
                保存请求
              </button>
            </div>
            <button class="secondary debug-channel-edit" type="button" :title="`切换 Environment：${activeEnvironmentName}`" @click="openEnvironmentSwitcher">
              <RefreshCw :size="15" />
              环境
            </button>
          </div>
        </div>

        <div class="debug-workbench debug-console-workbench">
          <section class="debug-panel debug-message-panel">
            <section class="debug-message-section debug-request-panel" :class="{ collapsed: collapsedDebugPanels.request }">
              <div class="debug-panel-header compact-debug-header">
                <button class="panel-collapse-button" type="button" @click="toggleDebugPanel('request')">
                  <ChevronRight v-if="collapsedDebugPanels.request" :size="14" />
                  <ChevronDown v-else :size="14" />
                  <span>请求</span>
                </button>
                <div class="segmented-control compact-segmented">
                  <button type="button" :class="{ active: debugMessageView === 'xml' }" @click="debugMessageView = 'xml'">XML</button>
                  <button type="button" :class="{ active: debugMessageView === 'ascii' }" @click="debugMessageView = 'ascii'">原始 ASCII</button>
                </div>
              </div>

              <div v-show="!collapsedDebugPanels.request" class="debug-panel-body">
                <label v-if="debugMessageView === 'xml'" class="debug-xml-pane">
                  <CodeEditor language="xml" v-model="debugRequest.requestXml" placeholder="<isomsg>...</isomsg>" />
                </label>
                <CodeBlock
                  v-else
                  language="text"
                  :code="debugRequestAscii"
                  placeholder="发送后展示原始请求 ASCII；二进制不可见字符会显示为 ."
                />
              </div>
            </section>

            <section class="debug-message-section debug-response-panel" :class="{ collapsed: collapsedDebugPanels.response }">
              <div class="debug-panel-header compact-debug-header">
                <button class="panel-collapse-button" type="button" @click="toggleDebugPanel('response')">
                  <ChevronRight v-if="collapsedDebugPanels.response" :size="14" />
                  <ChevronDown v-else :size="14" />
                  <span>响应</span>
                </button>
                <div class="debug-header-actions">
                  <span v-if="debugResult" class="debug-header-meta">{{ responseCodeSummary }}</span>
                  <span v-if="debugResult" class="history-status" :class="{ failed: !debugResult.success }">
                    {{ debugResult.success ? '成功' : '失败' }}
                  </span>
                  <div class="segmented-control compact-segmented">
                    <button type="button" :class="{ active: debugMessageView === 'xml' }" @click="debugMessageView = 'xml'">XML</button>
                    <button type="button" :class="{ active: debugMessageView === 'ascii' }" @click="debugMessageView = 'ascii'">原始 ASCII</button>
                  </div>
                </div>
              </div>

              <div v-show="!collapsedDebugPanels.response" class="debug-panel-body">
                <CodeBlock
                  :language="debugMessageView === 'xml' ? 'xml' : 'text'"
                  :code="debugMessageView === 'xml' ? debugResponseXml : debugResponseAscii"
                  :placeholder="debugMessageView === 'xml' ? '发送后显示响应 XML。' : '发送后显示原始响应 ASCII；二进制不可见字符会显示为 .'"
                />

                <section v-if="debugResult && debugResultKind !== 'tid'" class="debug-result-panel debug-result-embedded">
                  <div class="data-list debug-step-list">
                    <div class="data-list-header debug-step-row">
                      <span>步骤</span>
                      <span>结果</span>
                      <span>DE39</span>
                      <span>MAC</span>
                      <span>操作</span>
                    </div>
                    <article v-for="step in debugResult.steps" :key="`${step.capability}-${step.label}`" class="data-list-row debug-step-row">
                      <div class="rule-name-cell">
                        <strong>{{ step.label || capabilityLabel(step.capability) }}</strong>
                        <small>{{ step.targetIp }}:{{ step.targetPort }} · {{ step.durationMs }}ms</small>
                      </div>
                      <span class="history-status" :class="{ failed: !step.success }">{{ step.success ? '成功' : '失败' }}</span>
                      <code>{{ step.responseCode || '-' }}</code>
                      <span>{{ step.responseMacValid === null || step.responseMacValid === undefined ? '未验证' : (step.responseMacValid ? '通过' : '失败') }}</span>
                      <div class="row-actions">
                        <button type="button" title="载入请求 XML" @click="loadDebugStepRequest(step)">
                          <Upload :size="16" />
                        </button>
                        <button type="button" title="复制请求 HEX" @click="copyDebugStepHex(step)">
                          <Copy :size="16" />
                        </button>
                      </div>
                    </article>
                  </div>
                </section>
              </div>
            </section>
          </section>

          <section class="debug-panel debug-history-side-panel" :class="{ collapsed: collapsedDebugPanels.history }">
            <div class="debug-history-header compact-debug-header">
              <button class="panel-collapse-button" type="button" @click="toggleDebugPanel('history')">
                <ChevronRight v-if="collapsedDebugPanels.history" :size="14" />
                <ChevronDown v-else :size="14" />
                <span>历史</span>
              </button>
              <button class="ghost" type="button" :disabled="debugHistory.length === 0" @click="clearDebugHistory">
                <Trash2 :size="15" />
                清空
              </button>
            </div>
            <div v-show="!collapsedDebugPanels.history" class="debug-history-body">
              <div v-if="debugHistory.length === 0" class="empty-inline">暂无记录</div>
              <div v-else class="debug-history-list">
                <article v-for="item in debugHistory" :key="item.id" class="debug-history-item" @click="loadDebugHistoryItem(item)">
                  <div>
                    <strong>{{ item.title }}</strong>
                    <span>{{ formatDateTime(item.createdAt) }} · {{ item.responseCode || '-' }}</span>
                  </div>
                  <span class="history-status" :class="{ failed: !item.success }">{{ item.success ? '成功' : '失败' }}</span>
                  <button type="button" title="删除历史" @click.stop="deleteDebugHistoryItem(item)">
                    <Trash2 :size="15" />
                  </button>
                </article>
              </div>
            </div>
          </section>

          <section class="debug-panel debug-console-panel" :class="{ collapsed: collapsedDebugPanels.console }">
            <div class="debug-panel-header compact-debug-header">
              <button class="panel-collapse-button" type="button" @click="toggleDebugPanel('console')">
                <ChevronRight v-if="collapsedDebugPanels.console" :size="14" />
                <ChevronDown v-else :size="14" />
                <span>Console</span>
              </button>
              <div class="debug-console-actions">
                <button class="ghost" type="button" :disabled="!debugConsoleEntries.length" @click="copyDebugConsole">
                  <Copy :size="15" />
                  复制
                </button>
                <button class="ghost" type="button" :disabled="!debugConsoleEntries.length" @click="clearDebugConsole">
                  <Trash2 :size="15" />
                  清空
                </button>
              </div>
            </div>
            <pre v-show="!collapsedDebugPanels.console" ref="debugConsoleOutputRef" class="debug-console-output">{{ debugConsoleText || 'Console idle. 点击发送后显示完整执行日志。' }}</pre>
          </section>
        </div>
      </section>

      <section v-if="tab === 'debug-http'" class="debug-page">
        <section class="debug-panel http-debug-placeholder">
          <div class="debug-panel-header">
            <div>
              <h2>HTTP Debug</h2>
            </div>
          </div>
        </section>
      </section>

      <section v-if="tab === 'public-rules'" class="single-panel">
        <div class="panel-header">
          <div>
            <h2>公开规则</h2>
            <p>复制共享规则到 {{ channelDisplayCode(selectedChannel) }}。</p>
          </div>
          <button class="secondary" @click="loadPublicRules">
            <RefreshCw :size="16" />
            刷新
          </button>
        </div>

        <div class="data-scope-callout global">
          <strong>全局数据</strong>
          <span>公开规则是全局共享库；复制公开规则时会创建当前用户自己的规则副本。</span>
        </div>

        <div class="data-list public-rule-data-list">
          <div class="data-list-header public-rule-data-row">
            <span>规则</span>
            <span>条件与结果</span>
            <span>操作</span>
          </div>
          <article v-for="rule in publicRules" :key="rule.id" class="data-list-row public-rule-data-row">
            <div class="rule-name-cell">
              <div class="rule-title">
                <span class="status-dot on"></span>
                <strong>{{ rule.name }}</strong>
              </div>
              <small>{{ rule.channelId }} · 作者 {{ ruleOwnerName(rule) }} · 更新 {{ formatDateTime(rule.updatedAt) }}</small>
              <div class="rule-meta">
                <code>P{{ rule.priority }}</code>
                <span class="capability-badge">{{ capabilityLabel(rule.capability) }}</span>
                <code>{{ rule.action.type }}</code>
              </div>
            </div>
            <div class="rule-flow">
              <div class="rule-flow-part">
                <strong>if</strong>
                <em>{{ ruleIfSummary(rule) }}</em>
              </div>
              <div class="rule-flow-arrow">then</div>
              <div class="rule-flow-part then-part">
                <strong>DE39</strong>
                <em>{{ ruleThenSummary(rule) }}</em>
              </div>
            </div>
            <div class="row-actions">
              <button title="复制到当前渠道" @click="copyPublicRule(rule)">
                <Copy :size="16" />
              </button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="tab === 'users' && isAdminUser" class="users-page">
        <div class="users-toolbar page-card">
          <div>
            <h2>用户列表</h2>
            <p>用户名即登录和归属标识；新用户默认密码为 123456。</p>
          </div>
          <button class="primary" type="button" @click="openCreateUserModal">
            <Plus :size="16" />
            新建用户
          </button>
        </div>
        <div v-if="isUsernameDuplicate" class="notice inline-notice">
          <AlertTriangle :size="17" />
          <span>用户名已存在，请换一个用户名。</span>
        </div>

        <div class="data-list user-data-list">
          <div class="data-list-header user-data-row">
            <span>用户</span>
            <span>创建时间</span>
            <span>操作</span>
          </div>
          <article
              v-for="user in users"
              :key="user.id"
              class="data-list-row user-data-row"
              @click="openUserDetail(user)"
            >
            <div class="user-table-cell">
              <span class="user-avatar">{{ userDisplayName(user).slice(0, 1).toUpperCase() }}</span>
              <span>
                <strong>{{ user.username }}</strong>
                <small>用户 ID {{ user.id }}</small>
              </span>
            </div>
            <span>{{ formatDateTime(user.createdAt) }}</span>
            <div class="row-actions">
              <button type="button" title="查看用户" @click.stop="openUserDetail(user)">
                <Users :size="16" />
              </button>
            </div>
          </article>
          <div v-if="users.length === 0" class="empty-state">
            <Users :size="22" />
            <strong>暂无用户</strong>
            <span>新建用户后可进入详情页创建 MCP Token。</span>
          </div>
        </div>
      </section>

      <section v-if="tab === 'user-detail' && canViewSelectedUserDetail" class="user-detail-page">
        <section class="editor user-detail-editor">
          <div class="editor-header">
            <div>
              <h2>{{ selectedUser?.username || '用户详情' }}</h2>
              <p>用户 ID {{ selectedUser?.id || '-' }}</p>
            </div>
            <div class="topbar-actions">
              <button class="ghost" type="button" @click="leaveUserDetail">
                <ArrowLeft :size="16" />
                {{ isAdminUser ? '返回列表' : '返回规则' }}
              </button>
              <button class="primary" type="button" :disabled="!selectedUser || tokenLimitReached" @click="openCreateTokenModal">
                <KeyRound :size="16" />
                创建 Token
              </button>
            </div>
          </div>

          <div class="detail-summary-grid">
            <div class="detail-summary-item">
              <span>用户名</span>
              <strong>{{ selectedUser?.username || '-' }}</strong>
            </div>
            <div class="detail-summary-item">
              <span>创建时间</span>
              <strong>{{ formatDateTime(selectedUser?.createdAt) }}</strong>
            </div>
            <div class="detail-summary-item">
              <span>有效 MCP Token</span>
              <strong>{{ activeTokenCount }}/3</strong>
            </div>
          </div>

          <div v-if="tokenLimitReached" class="notice inline-notice muted-notice">
            <AlertTriangle :size="17" />
            <span>每个用户最多保留 3 个有效 MCP Token；吊销一个后才能继续创建。</span>
          </div>

          <div class="form-section">
            <div class="section-title">
              <h2>MCP Token</h2>
              <span class="tooltip-trigger" tabindex="0" data-tooltip="Token 只属于当前用户；用于 AI 或 MCP 客户端调用本服务。">?</span>
            </div>
            <div class="mcp-endpoint-panel">
              <div class="mcp-endpoint-main">
                <span>HTTP MCP 完整访问路径</span>
                <code>POST {{ mcpEndpointUrl }}</code>
                <small>当前页面同源代理路径，推荐给本地 AI/MCP 客户端使用。</small>
              </div>
              <div>
                <span>鉴权方式</span>
                <code>Authorization: Bearer mcp_...</code>
              </div>
              <div>
                <span>开放能力</span>
                <strong>渠道配置、Key设置、规则管理、公开规则复制、规则 XML 验证、验证历史、POS Debug</strong>
              </div>
            </div>
            <div class="data-list token-data-list">
              <div class="data-list-header token-data-row">
                <span>Token</span>
                <span>状态</span>
                <span>创建时间</span>
                <span>操作</span>
              </div>
              <article v-for="token in activeMcpTokens" :key="token.id" class="data-list-row token-data-row">
                <div class="token-value-cell">
                  <strong>{{ token.name }}</strong>
                  <button type="button" class="token-copy-button" title="点击复制 Token" @click="copyMcpToken(token)">
                    <Copy :size="13" />
                    <span>{{ maskSecret(token.token) }}</span>
                  </button>
                </div>
                <span>
                  <code>ACTIVE</code>
                </span>
                <span>{{ formatDateTime(token.createdAt) }}</span>
                <div class="row-actions">
                  <button type="button" title="吊销 Token" @click="revokeToken(token)">
                    <Trash2 :size="16" />
                  </button>
                </div>
              </article>
              <div v-if="activeMcpTokens.length === 0" class="empty-state">
                <KeyRound :size="22" />
                <strong>暂无 MCP Token</strong>
                <span>点击右上角“创建 Token”为当前用户生成调用凭据。</span>
              </div>
            </div>
          </div>
        </section>
      </section>

      <section v-if="tab === 'channel-new'" class="channel-create-page">
        <form class="editor channel-create-editor" @submit.prevent="saveChannel">
          <div class="workbench-toolbar channel-editor-toolbar">
            <div class="toolbar-meta">
              <strong>{{ isEditingChannel ? '编辑渠道' : '新增渠道' }}</strong>
              <span>{{ channelEditor.channelCode || '草稿' }} · 全局配置 · 运行时字段变更需重启</span>
            </div>
            <div class="toolbar-actions">
              <button class="ghost" type="button" title="返回规则" @click="openRules">
                <ArrowLeft :size="16" />
                返回
              </button>
              <button class="primary" type="submit" :disabled="isChannelCodeDuplicate || isChannelPortDuplicate || isChannelPackagerDuplicate">
                <Save :size="16" />
                保存
              </button>
            </div>
          </div>

          <div v-if="restartNotice" class="notice">
            <AlertTriangle :size="17" />
            <span>{{ restartNotice }}</span>
          </div>

          <div class="form-nav">
            <button type="button" :class="{ active: channelSection === 'basic' }" @click="channelSection = 'basic'">基础</button>
            <button type="button" :class="{ active: channelSection === 'framing' }" @click="channelSection = 'framing'">Framing</button>
            <button type="button" :class="{ active: channelSection === 'header' }" @click="channelSection = 'header'">Header</button>
            <button type="button" :class="{ active: channelSection === 'packager' }" @click="channelSection = 'packager'">Packager</button>
          </div>

          <div v-if="channelSection === 'basic'" class="form-section">
            <div class="section-title">
              <h2>基础信息</h2>
              <span class="tooltip-trigger" tabindex="0" :data-tooltip="isEditingChannel ? '编辑渠道配置是全局变更；只有运行时配置变更才需要重启。' : '新增渠道需要重启创建 TCP listener；更新时只有运行时配置变更才需要重启。'">?</span>
            </div>
            <div class="form-card-grid">
              <div class="form-card span-2">
                <div class="form-card-header">
                  <strong>渠道身份</strong>
                  <span>只填写渠道 Code；内部 ID 由后台保存时自动生成。</span>
                </div>
                <div class="field-grid compact-grid">
                  <label class="wide-field">
                    <span>渠道 Code <small class="effect-badge hot">热生效</small></span>
                    <input v-model="channelEditor.channelCode" required placeholder="填写真实渠道 code，不要使用示例值" />
                  </label>
                  <label class="switch-field">
                    <input type="checkbox" v-model="channelEditor.enabled" />
                    <span>启用 <small class="effect-badge restart">需重启</small></span>
                  </label>
                </div>
                <div v-if="isChannelCodeDuplicate" class="notice inline-notice">
                  <AlertTriangle :size="17" />
                  <span>渠道 Code 已存在，请换一个唯一的 code。</span>
                </div>
              </div>

              <div class="form-card span-2">
                <div class="form-card-header">
                  <strong>第三方测试环境 <small class="effect-badge hot">必填</small></strong>
                  <span>真实上游/收单测试环境参数；Debug 工具和环境默认变量会优先读取这里。</span>
                </div>
                <div class="field-grid compact-grid">
                  <label>
                    <span>测试环境 IP</span>
                    <input v-model="channelEditor.thirdPartyTestIp" required placeholder="例如 10.10.10.20" />
                  </label>
                  <label>
                    <span>测试环境端口</span>
                    <input type="number" min="1" max="65535" required v-model.number="channelEditor.thirdPartyTestPort" placeholder="1-65535" />
                  </label>
                  <label class="switch-field">
                    <input type="checkbox" v-model="channelEditor.thirdPartyTlsEnabled" />
                    <span>TLS <small class="effect-badge hot">热生效</small></span>
                  </label>
                  <label>
                    <span>CTMK1</span>
                    <input v-model="channelEditor.ctmk1" required spellcheck="false" placeholder="HEX 密钥分量" />
                  </label>
                  <label>
                    <span>CTMK2</span>
                    <input v-model="channelEditor.ctmk2" required spellcheck="false" placeholder="HEX 密钥分量" />
                  </label>
                </div>
              </div>

              <div class="form-card span-2">
                <div class="form-card-header">
                  <strong>Mock 测试环境 <small class="effect-badge restart">端口/TLS 需重启</small></strong>
                  <span>Faker 自己的 Mock TCP 监听环境；CTMK 和 TLS 默认复用第三方测试环境，也可以单独覆盖。</span>
                </div>
                <div class="field-grid compact-grid">
                  <label>
                    <span>监听地址</span>
                    <input v-model="channelEditor.host" />
                  </label>
                  <label>
                    <span>Mock TCP 端口</span>
                    <input type="number" min="14400" max="14700" v-model.number="channelEditor.port" placeholder="14400-14700，系统推荐后确认" />
                  </label>
                  <div class="wide-field">
                    <span class="field-label">Mock TLS</span>
                    <div class="segmented-control compact-segmented">
                      <button type="button" :class="{ active: channelEditor.mockTlsEnabled === null }" @click="channelEditor.mockTlsEnabled = null">
                        复用第三方
                      </button>
                      <button type="button" :class="{ active: channelEditor.mockTlsEnabled === true }" @click="channelEditor.mockTlsEnabled = true">
                        启用
                      </button>
                      <button type="button" :class="{ active: channelEditor.mockTlsEnabled === false }" @click="channelEditor.mockTlsEnabled = false">
                        关闭
                      </button>
                    </div>
                  </div>
                  <label>
                    <span>Mock CTMK1</span>
                    <input v-model="channelEditor.mockCtmk1" spellcheck="false" placeholder="留空复用第三方 CTMK1" />
                  </label>
                  <label>
                    <span>Mock CTMK2</span>
                    <input v-model="channelEditor.mockCtmk2" spellcheck="false" placeholder="留空复用第三方 CTMK2" />
                  </label>
                </div>
                <div v-if="!isEditingChannel && channelEditor.port" class="notice inline-notice muted-notice">
                  <Info :size="17" />
                  <span>已推荐当前未被配置占用的端口 {{ channelEditor.port }}；如需改动，请选择 14400-14700 内的空闲端口。</span>
                </div>
                <div v-if="isChannelPortDuplicate" class="notice inline-notice">
                  <AlertTriangle :size="17" />
                  <span>监听端口已被其他渠道占用，请换一个 14400-14700 范围内的空闲端口。</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="channelSection === 'framing'" class="form-section">
            <div class="section-title">
              <h2>Framing</h2>
              <span class="tooltip-trigger" tabindex="0" data-tooltip="Framing 决定如何从 TCP 字节流中切出一条完整 ISO8583 报文，变更后需重启后端。">?</span>
            </div>
            <div class="form-card-grid">
              <div class="form-card">
                <div class="form-card-header">
                  <strong>长度前缀 <small class="effect-badge restart">需重启</small></strong>
                  <span>常见为 2 字节二进制长度</span>
                </div>
                <div class="choice-card-grid">
                  <button type="button" class="choice-card" :class="{ active: channelEditor.framingType === 'BINARY_2' }" @click="channelEditor.framingType = 'BINARY_2'">
                    <strong>二进制 2 字节</strong>
                    <span>适合绝大多数 ISO8583 直连渠道</span>
                    <code>BINARY_2</code>
                  </button>
                  <button type="button" class="choice-card" :class="{ active: channelEditor.framingType === 'ASCII_4' }" @click="channelEditor.framingType = 'ASCII_4'">
                    <strong>ASCII 4 位长度</strong>
                    <span>长度以 4 个 ASCII 数字表示</span>
                    <code>ASCII_4</code>
                  </button>
                </div>
              </div>

              <div class="form-card">
                <div class="form-card-header">
                  <strong>长度解释 <small class="effect-badge restart">需重启</small></strong>
                  <span>和对端协议保持一致</span>
                </div>
                <div class="choice-group" v-if="channelEditor.framingType === 'BINARY_2'">
                  <button type="button" :class="{ active: channelEditor.byteOrder === 'BIG_ENDIAN' }" @click="channelEditor.byteOrder = 'BIG_ENDIAN'">
                    <strong>大端</strong>
                    <small>BIG_ENDIAN</small>
                  </button>
                  <button type="button" :class="{ active: channelEditor.byteOrder === 'LITTLE_ENDIAN' }" @click="channelEditor.byteOrder = 'LITTLE_ENDIAN'">
                    <strong>小端</strong>
                    <small>LITTLE_ENDIAN</small>
                  </button>
                </div>
                <div class="choice-group">
                  <button type="button" :class="{ active: channelEditor.lengthIncludes === 'PAYLOAD' }" @click="channelEditor.lengthIncludes = 'PAYLOAD'">
                    <strong>仅报文体</strong>
                    <small>PAYLOAD</small>
                  </button>
                  <button type="button" :class="{ active: channelEditor.lengthIncludes === 'PAYLOAD_PLUS_LENGTH' }" @click="channelEditor.lengthIncludes = 'PAYLOAD_PLUS_LENGTH'">
                    <strong>含长度头</strong>
                    <small>PAYLOAD_PLUS_LENGTH</small>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="channelSection === 'header'" class="form-section">
            <div class="section-title">
              <h2>Header / TPDU</h2>
              <span class="tooltip-trigger" tabindex="0" data-tooltip="仅当前置 TPDU/Header 字节时需要配置；变更后需重启后端。">?</span>
            </div>
            <div class="form-card-grid">
              <div class="form-card">
                <div class="form-card-header">
                  <strong>Header 开关 <small class="effect-badge restart">需重启</small></strong>
                  <span>请求载荷前是否存在额外字节</span>
                </div>
                <label class="switch-field strong-switch">
                  <input type="checkbox" v-model="channelEditor.headerEnabled" />
                  <span>{{ channelEditor.headerEnabled ? '已启用 Header/TPDU' : '不处理 Header/TPDU' }}</span>
                </label>
              </div>

              <div class="form-card" v-if="channelEditor.headerEnabled">
                <div class="form-card-header">
                  <strong>响应处理 <small class="effect-badge restart">需重启</small></strong>
                  <span>返回报文时 Header 如何生成</span>
                </div>
                <div class="field-grid compact-grid">
                  <label>
                    <span>长度</span>
                    <input type="number" min="0" v-model.number="channelEditor.headerLength" />
                  </label>
                </div>
                <div class="choice-group">
                  <button type="button" :class="{ active: channelEditor.headerResponseMode === 'NONE' }" @click="channelEditor.headerResponseMode = 'NONE'">
                    <strong>不返回</strong>
                    <small>NONE</small>
                  </button>
                  <button type="button" :class="{ active: channelEditor.headerResponseMode === 'ECHO' }" @click="channelEditor.headerResponseMode = 'ECHO'">
                    <strong>回显请求</strong>
                    <small>ECHO</small>
                  </button>
                  <button type="button" :class="{ active: channelEditor.headerResponseMode === 'FIXED' }" @click="channelEditor.headerResponseMode = 'FIXED'">
                    <strong>固定值</strong>
                    <small>FIXED</small>
                  </button>
                </div>
                <label v-if="channelEditor.headerResponseMode === 'FIXED'" class="wide-field">
                  <span>固定 HEX</span>
                  <input v-model="channelEditor.headerFixedValueHex" />
                </label>
              </div>
            </div>
          </div>

          <div v-if="channelSection === 'packager'" class="form-section">
            <div class="section-title with-actions">
              <div>
                <h2>Packager</h2>
                <span class="tooltip-trigger" tabindex="0" data-tooltip="Packager 是渠道方言，创建渠道时必须独立确认；不可偷懒复用其它渠道配置。自定义实现必须继承 ISOBasePackager。">?</span>
              </div>
              <button class="secondary" type="button" @click="openDraftPackagerPreview">
                <Eye :size="16" />
                预览字段
              </button>
            </div>
            <div class="form-card-grid">
              <div class="form-card span-2">
                <div class="form-card-header">
                  <strong>方言来源 <small class="effect-badge restart">需重启</small></strong>
                  <span>决定 ISO8583 字段如何 pack/unpack；每个渠道必须使用自己的正确方言</span>
                </div>
                <div class="choice-card-grid">
                  <button type="button" class="choice-card" :class="{ active: channelEditor.packagerType === 'XML' }" @click="setPackagerType('XML')">
                    <strong>XML 方言</strong>
                    <span>上传或粘贴 jPOS XML packager</span>
                    <code>XML</code>
                  </button>
                  <button type="button" class="choice-card" :class="{ active: channelEditor.packagerType === 'CLASS' }" @click="setPackagerType('CLASS')">
                    <strong>jPOS 类名</strong>
                    <span>使用已存在的 Packager 类</span>
                    <code>CLASS</code>
                  </button>
                  <button type="button" class="choice-card" :class="{ active: channelEditor.packagerType === 'CUSTOM' }" @click="setPackagerType('CUSTOM')">
                    <strong>自定义实现</strong>
                    <span>粘贴 Java 源码或填写类名</span>
                    <code>CUSTOM</code>
                  </button>
                </div>
              </div>

              <div class="form-card">
                <div class="form-card-header">
                  <strong>配置方式 <small class="effect-badge restart">需重启</small></strong>
                  <span>推荐选择可直接入库的内容模式</span>
                </div>
                <div class="choice-group">
                  <button v-if="channelEditor.packagerType === 'XML'" type="button" :class="{ active: channelEditor.packagerConfigMode === 'XML_CONTENT' }" @click="channelEditor.packagerConfigMode = 'XML_CONTENT'">
                    <strong>粘贴/上传 XML</strong>
                    <small>推荐</small>
                  </button>
                  <button v-if="channelEditor.packagerType === 'XML'" type="button" :class="{ active: channelEditor.packagerConfigMode === 'XML_FILE' }" @click="channelEditor.packagerConfigMode = 'XML_FILE'">
                    <strong>文件名</strong>
                    <small>不推荐</small>
                  </button>
                  <button v-if="channelEditor.packagerType !== 'XML'" type="button" :class="{ active: channelEditor.packagerConfigMode === 'CLASS_NAME' }" @click="channelEditor.packagerConfigMode = 'CLASS_NAME'">
                    <strong>类名</strong>
                    <small>{{ channelEditor.packagerType === 'CLASS' ? 'jPOS' : '不推荐' }}</small>
                  </button>
                  <button v-if="channelEditor.packagerType === 'CUSTOM'" type="button" :class="{ active: channelEditor.packagerConfigMode === 'JAVA_SOURCE' }" @click="channelEditor.packagerConfigMode = 'JAVA_SOURCE'">
                    <strong>Java 源码</strong>
                    <small>推荐</small>
                  </button>
                </div>
                <label v-if="channelEditor.packagerConfigMode === 'XML_FILE'">
                  <span>文件名</span>
                  <input v-model="channelEditor.packagerFileName" placeholder="resources/packager/*.xml" />
                </label>
                <label v-if="channelEditor.packagerConfigMode === 'CLASS_NAME'" class="wide-field">
                  <span>类名</span>
                  <input v-model="channelEditor.packagerClassName" required placeholder="填写该渠道真实 Packager 类名" />
                </label>
                <label>
                  <span>未命中响应码</span>
                  <input v-model="channelEditor.noMatchResponseCode" maxlength="2" />
                </label>
              </div>
            </div>
            <div v-if="isChannelPackagerDuplicate" class="notice inline-notice">
              <AlertTriangle :size="17" />
              <span>当前 Packager 配置已被其它渠道使用。每个渠道必须单独确认方言，请勿直接复用。</span>
            </div>
            <div v-if="channelEditor.packagerType === 'XML'" class="packager-box">
              <div class="notice" v-if="channelEditor.packagerConfigMode === 'XML_FILE'">
                <AlertTriangle :size="17" />
                <span>文件名模式需要重启前手工把 XML 放到 resources/packager/，推荐使用内容模式。</span>
              </div>
              <label v-if="channelEditor.packagerConfigMode === 'XML_CONTENT'" class="wide-field">
                <span>XML 内容</span>
                <textarea class="packager-textarea" v-model="channelEditor.packagerContent"></textarea>
              </label>
              <label v-if="channelEditor.packagerConfigMode === 'XML_CONTENT'" class="file-picker">
                <Upload :size="16" />
                <span>上传 XML</span>
                <input type="file" accept=".xml,text/xml" @change="readPackagerFile($event, 'xml')" />
              </label>
            </div>
            <div v-if="channelEditor.packagerType === 'CUSTOM'" class="packager-box">
              <div class="notice" v-if="channelEditor.packagerConfigMode === 'CLASS_NAME'">
                <AlertTriangle :size="17" />
                <span>类名模式需要重启前手工改代码，推荐使用 Java 源码模式。</span>
              </div>
              <label v-if="channelEditor.packagerConfigMode === 'JAVA_SOURCE'" class="wide-field">
                <span>Java 源码</span>
                <textarea class="packager-textarea" v-model="channelEditor.packagerContent"></textarea>
              </label>
              <label v-if="channelEditor.packagerConfigMode === 'JAVA_SOURCE'" class="file-picker">
                <Upload :size="16" />
                <span>上传 Java</span>
                <input type="file" accept=".java,text/x-java-source,text/plain" @change="readPackagerFile($event, 'java')" />
              </label>
              <div v-if="channelEditor.packagerConfigMode === 'JAVA_SOURCE' && channelEditor.packagerContent" class="packager-source-preview">
                <span>Java 高亮预览</span>
                <CodeBlock language="java" :code="channelEditor.packagerContent" />
              </div>
            </div>
            <section v-if="draftPackagerPreviewVisible" class="packager-preview-inline">
              <div class="section-title with-actions">
                <div>
                  <h2>字段预览</h2>
                  <span class="tooltip-trigger" tabindex="0" data-tooltip="当前预览基于表单草稿生成，仅用于确认字段定义；保存后才会入库并在重启后影响运行时。">?</span>
                </div>
                <button class="secondary" type="button" @click="draftPackagerPreviewVisible = false">
                  <X :size="16" />
                  收起
                </button>
              </div>
              <div v-if="packagerPreviewLoading" class="empty-inline">正在读取 Packager 字段定义...</div>
              <div v-else-if="packagerPreviewError" class="notice">
                <AlertTriangle :size="17" />
                <span>{{ packagerPreviewError }}</span>
              </div>
              <div v-else-if="packagerPreview" class="packager-preview-content">
                <div class="packager-preview-summary">
                  <code>{{ packagerPreview.packagerClass }}</code>
                  <span>{{ packagerPreview.fields.length }} 个字段</span>
                  <span v-if="packagerPreview.description">{{ packagerPreview.description }}</span>
                </div>
                <div v-if="packagerPreview.fields.length === 0" class="empty-inline">
                  当前 Packager 没有暴露可预览的字段定义。
                </div>
                <div v-else class="packager-table-wrap">
                  <table class="packager-table">
                    <thead>
                      <tr>
                        <th>字段</th>
                        <th>取值类型</th>
                        <th>类型</th>
                        <th>长度</th>
                        <th>最大打包长度</th>
                        <th>描述</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="field in packagerPreview.fields" :key="field.id">
                        <td><code>{{ formatFieldLabel(field.id) }}</code></td>
                        <td><span class="field-type-pill" :class="fieldValueType(field).toLowerCase()">{{ fieldTypeLabel(fieldValueType(field)) }}</span></td>
                        <td>{{ field.type }}</td>
                        <td>{{ field.length }}</td>
                        <td>{{ field.maxPackedLength }}</td>
                        <td>{{ field.description || '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          </div>
        </form>
      </section>

      <section v-if="tab === 'packager-preview'" class="packager-preview-page single-panel">
        <div class="workbench-toolbar packager-preview-toolbar">
          <div class="toolbar-meta">
            <strong>Packager 预览</strong>
            <span>{{ packagerPreview?.channelName || channelDisplayCode(selectedChannel) }} · {{ packagerPreview?.packagerClass || shortPackager }}</span>
          </div>
          <button class="secondary" type="button" @click="openPackagerPreview">
            <RefreshCw :size="16" />
            刷新
          </button>
        </div>

        <div v-if="packagerPreviewLoading" class="empty-inline">正在读取 Packager 字段定义...</div>
        <div v-else-if="packagerPreviewError" class="notice">
          <AlertTriangle :size="17" />
          <span>{{ packagerPreviewError }}</span>
        </div>
        <div v-else-if="packagerPreview" class="packager-preview-content">
          <div class="packager-preview-summary">
            <code>{{ packagerPreview.packagerClass }}</code>
            <span>{{ packagerPreview.fields.length }} 个字段</span>
            <span v-if="packagerPreview.description">{{ packagerPreview.description }}</span>
          </div>
          <div v-if="packagerPreview.fields.length === 0" class="empty-inline">
            当前 Packager 没有暴露可预览的字段定义。
          </div>
          <div v-else class="packager-table-wrap">
            <table class="packager-table">
              <thead>
                <tr>
                  <th>字段</th>
                  <th>取值类型</th>
                  <th>类型</th>
                  <th>长度</th>
                  <th>最大打包长度</th>
                  <th>描述</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="field in packagerPreview.fields" :key="field.id">
                  <td><code>{{ formatFieldLabel(field.id) }}</code></td>
                  <td><span class="field-type-pill" :class="fieldValueType(field).toLowerCase()">{{ fieldTypeLabel(fieldValueType(field)) }}</span></td>
                  <td>{{ field.type }}</td>
                  <td>{{ field.length }}</td>
                  <td>{{ field.maxPackedLength }}</td>
                  <td>{{ field.description || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </main>

    <div v-if="infoCenterOpen" class="modal-backdrop" @click.self="closeInfoCenter">
      <section class="modal info-center-modal" role="dialog" aria-modal="true" aria-labelledby="info-center-title">
        <header class="modal-header">
          <div>
            <h2 id="info-center-title">信息中心</h2>
            <p>MCP 接入、返回码和 ISO8583 结构速查。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeInfoCenter">
            <X :size="16" />
          </button>
        </header>

        <div class="info-center-content">
          <nav class="info-tabs" aria-label="信息中心分类">
            <button type="button" :class="{ active: infoCenterTab === 'mcp' }" @click="infoCenterTab = 'mcp'">
              MCP 使用
            </button>
            <button type="button" :class="{ active: infoCenterTab === 'codes' }" @click="infoCenterTab = 'codes'">
              0-99 返回码
            </button>
            <button type="button" :class="{ active: infoCenterTab === 'iso8583' }" @click="infoCenterTab = 'iso8583'">
              ISO8583 结构
            </button>
          </nav>

          <section v-if="infoCenterTab === 'mcp'" class="info-panel">
            <div class="info-callout">
              <strong>鉴权</strong>
              <span>每个用户使用自己的 MCP Token 调用；Token 可在用户详情页创建，最多保留 3 个有效 Token。</span>
            </div>
            <div class="info-section-grid">
              <article class="info-card">
                <strong>Endpoint</strong>
                <code>POST {{ mcpEndpointUrl }}</code>
                <span>请求头使用 <code>Authorization: Bearer mcp_...</code>；后端直连地址为 <code>{{ backendMcpEndpointUrl }}</code>。</span>
              </article>
              <article class="info-card">
                <strong>协议形态</strong>
                <span>JSON-RPC 风格，支持 <code>initialize</code>、<code>tools/list</code>、<code>tools/call</code>。</span>
              </article>
            </div>
            <div class="info-code-grid">
              <div class="info-code-panel">
                <strong>Bash</strong>
                <CodeBlock language="bash" :code="mcpCurlExample" />
              </div>
              <div class="info-code-panel">
                <strong>JSON</strong>
                <CodeBlock language="json" :code="mcpJsonExample" />
              </div>
            </div>
            <div class="info-tool-list">
              <div v-for="group in mcpCapabilityGroups" :key="group.title">
                <strong>{{ group.title }}</strong>
                <span>{{ group.tools.join('、') }}</span>
              </div>
            </div>
            <div class="info-note">
              <strong>数据边界</strong>
              <span>渠道、Key 配置和公开规则是全局数据；规则管理和规则验证历史绑定 Token 所属用户。</span>
            </div>
          </section>

          <section v-if="infoCenterTab === 'codes'" class="info-panel">
            <div class="info-callout warning">
              <strong>说明</strong>
              <span>DE39 含义在不同渠道、卡组织和主机规范中可能被覆盖；这里提供常见 00-99 约定，生产联调以渠道文档为准。</span>
            </div>
            <div class="response-code-table-wrap">
              <table class="response-code-table">
                <thead>
                  <tr>
                    <th>码</th>
                    <th>含义</th>
                    <th>处理建议</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="code in responseCodeReferences" :key="code.code">
                    <td><code>{{ code.code }}</code></td>
                    <td>{{ code.label }}</td>
                    <td>{{ code.note }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-if="infoCenterTab === 'iso8583'" class="info-panel">
            <div class="iso-structure-grid">
              <article v-for="item in isoStructureItems" :key="item.title" class="info-card">
                <strong>{{ item.title }}</strong>
                <span>{{ item.description }}</span>
              </article>
            </div>
            <div class="info-two-column">
              <div class="info-note">
                <strong>常见字段</strong>
                <span>MTI 表示消息类型；DE3 表示处理码；DE4 表示金额；DE11 表示流水号；DE39 表示响应码；DE41/DE42 表示终端和商户；DE64/DE128 通常用于 MAC。</span>
              </div>
              <div class="info-note">
                <strong>本系统处理</strong>
                <span>字段号输入 <code>3</code> 或 <code>DE3</code> 都会归一为 DE3；响应未配置字段默认沿用请求，MTI 默认自动配对。</span>
              </div>
            </div>
            <div class="info-note">
              <strong>报文边界</strong>
              <span>TCP Frame 通常在 ISO Body 外层增加长度前缀；部分渠道还会在 ISO Body 前增加 TPDU/Header。本系统按渠道配置处理 Framing 和 Header。</span>
            </div>
          </section>
        </div>
      </section>
    </div>

    <div v-if="debugToolModal" class="modal-backdrop" @click.self="closeDebugToolModal">
      <section class="modal debug-tool-modal" role="dialog" aria-modal="true" :aria-labelledby="`debug-${debugToolModal}-title`">
        <header class="modal-header">
          <div>
            <h2 :id="`debug-${debugToolModal}-title`">{{ debugToolModal === 'xml' ? 'XML 生成' : 'TID 初始化' }}</h2>
            <p>{{ debugToolModal === 'xml' ? '按能力和当前 Environment 生成 ISO8583 XML；取不到值的必填域会保留空值。' : '一次性执行 TID-TMK / TID-TPK / TID-TSK 初始化。' }}</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeDebugToolModal">
            <X :size="16" />
          </button>
        </header>

        <div v-if="debugToolModal === 'xml'" class="debug-modal-body">
          <div class="field-grid compact-grid debug-small-fields">
            <label>
              <span>交易模板</span>
              <select v-model="debugRequest.capability">
                <option v-for="capability in capabilityOptions" :key="capability.value" :value="capability.value">
                  {{ capability.label }} · {{ capability.requestMti }} / {{ capability.processCode }}
                </option>
              </select>
            </label>
          </div>

          <div class="debug-note xml-generate-note">
            <strong>自动填充</strong>
            <span>MTI、DE3、STAN、时间戳、RRN、环境变量 DE2/DE14/DE41/DE52/SN、动态参数 DE18/DE42/DE43/DE49；手册 mandatory 字段缺值时输出 <code>""</code>。</span>
            <span>当前 Environment：TID <code>{{ debugEnvironmentTid || '-' }}</code> · SN <code>{{ debugEnvironmentSn || '-' }}</code></span>
          </div>

          <div class="debug-field-table modal-field-table">
            <div class="debug-field-row debug-field-head">
              <span>覆盖字段</span>
              <span>覆盖值</span>
              <span></span>
            </div>
            <div class="debug-field-row" v-for="(row, index) in debugFieldRows" :key="index">
              <input v-model="row.key" placeholder="3 或 DE3" @blur="row.key = canonicalFieldKey(row.key)" />
              <input v-model="row.value" placeholder="字段值" />
              <button type="button" title="移除字段" @click="removeDebugFieldRow(index)">
                <X :size="15" />
              </button>
            </div>
          </div>
        </div>

        <div v-else class="debug-modal-body">
          <div class="debug-note xml-generate-note">
            <strong>使用 Environment</strong>
            <span>TID <code>{{ debugEnvironmentTid || '-' }}</code> · SN <code>{{ debugEnvironmentSn || '-' }}</code></span>
          </div>
          <div class="field-grid compact-grid">
            <label class="switch-field">
              <input type="checkbox" v-model="debugTidInit.saveKey" />
              <span>尝试保存 Key</span>
            </label>
          </div>
          <div class="debug-note">
            <strong>业务边界</strong>
            <span>初始化只需要 TID 和 SN，不需要 PAN/PIN，默认不处理 MAC。</span>
          </div>
        </div>

        <footer class="modal-footer">
          <button type="button" class="secondary" @click="closeDebugToolModal">取消</button>
          <button v-if="debugToolModal === 'xml'" type="button" class="secondary" @click="addDebugFieldRow">
            <Plus :size="16" />
            添加字段
          </button>
          <button v-if="debugToolModal === 'xml'" type="button" class="primary" @click="generateDebugXmlFromModal">
            <RefreshCw :size="16" />
            生成 XML
          </button>
          <button v-else type="button" class="primary" :disabled="debugLoading || !debugTargetConfigured" @click="runTidInitFromModal">
            <Play :size="16" />
            执行初始化
          </button>
        </footer>
      </section>
    </div>

    <div v-if="environmentSwitcherOpen" class="modal-backdrop" @click.self="closeEnvironmentSwitcher">
      <section class="modal compact-modal environment-switch-modal" role="dialog" aria-modal="true" aria-labelledby="environment-switch-title">
        <header class="modal-header">
          <div>
            <h2 id="environment-switch-title">切换 Environment</h2>
            <p>只切换当前请求使用的环境变量集合。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeEnvironmentSwitcher">
            <X :size="16" />
          </button>
        </header>

        <div class="environment-switch-list">
          <button
            v-for="environment in debugEnvironments"
            :key="environment.id"
            type="button"
            class="environment-switch-item"
            :class="{ active: activeEnvironmentId === environment.id }"
            @click="chooseDebugEnvironment(environment.id)"
          >
            <span class="status-dot"></span>
            <span>
              <strong>{{ environment.name }}</strong>
              <small>{{ environmentChannelLabel(environment) }} · {{ environment.targetIp || '-' }}:{{ environment.targetPort || '-' }}</small>
            </span>
            <code v-if="activeEnvironmentId === environment.id">CURRENT</code>
          </button>
          <div v-if="debugEnvironments.length === 0" class="empty-inline">暂无 Environment</div>
        </div>
      </section>
    </div>

    <div v-if="smartReplaceOpen" class="modal-backdrop" @click.self="closeSmartReplacePreview">
      <section class="modal smart-replace-modal" role="dialog" aria-modal="true" aria-labelledby="smart-replace-title">
        <header class="modal-header">
          <div>
            <h2 id="smart-replace-title">智能替换</h2>
            <p>选择本次要替换的字段组，确认后才会写回请求 XML；空值字段会自动跳过。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeSmartReplacePreview">
            <X :size="16" />
          </button>
        </header>

        <div class="smart-replace-body">
          <section class="smart-replace-scope">
            <header>
              <div>
                <strong>替换范围</strong>
                <span>{{ smartReplaceSelectedCount ? smartReplaceSelectedFields : '未选择替换内容' }}</span>
              </div>
              <small>{{ smartReplaceChanges.length }} 个字段有变化</small>
            </header>
            <div class="smart-replace-scope-options">
              <label v-for="option in smartReplaceScopeOptions" :key="option.key" class="smart-replace-scope-option">
                <input
                  v-model="smartReplaceSelection[option.key]"
                  type="checkbox"
                  @change="refreshSmartReplacePreview"
                />
                <span>
                  <strong>{{ option.label }}</strong>
                  <small>{{ option.fields }}</small>
                  <em>{{ option.description }}</em>
                </span>
              </label>
            </div>
          </section>

          <div v-if="!smartReplaceSelectedCount" class="empty-inline smart-replace-empty">
            请选择至少一种替换内容。
          </div>
          <div v-else-if="!smartReplaceChanges.length" class="empty-inline smart-replace-empty">
            当前选择没有产生差异。可能是字段值已经一致，或者当前 Environment 没有对应变量。
          </div>

          <div v-if="smartReplaceChanges.length" class="smart-replace-table" role="table" aria-label="字段替换差异">
            <div class="smart-replace-row smart-replace-head" role="row">
              <span>字段</span>
              <span>来源</span>
              <span>替换前</span>
              <span>替换后</span>
            </div>
            <div v-for="change in smartReplaceChanges" :key="change.field" class="smart-replace-row" role="row">
              <span class="iso-field-chip">{{ change.label }}</span>
              <span>{{ change.source }}</span>
              <code :class="{ muted: change.created }">{{ change.beforeDisplay }}</code>
              <code>{{ change.after }}</code>
            </div>
          </div>

          <section class="smart-replace-code">
            <header>
              <strong>替换后 XML</strong>
              <span>{{ smartReplaceChanges.length }} 个字段将被更新</span>
            </header>
            <CodeBlock language="xml" :code="smartReplaceXmlDraft" />
          </section>
        </div>

        <footer class="modal-footer">
          <button type="button" class="secondary" @click="closeSmartReplacePreview">取消</button>
          <button type="button" class="primary" :disabled="!smartReplaceChanges.length" @click="applySmartReplace">
            <CheckCircle2 :size="16" />
            Apply
          </button>
        </footer>
      </section>
    </div>

    <div class="toast-stack" aria-live="polite" aria-atomic="false">
      <article v-for="toast in toasts" :key="toast.id" class="toast-message" :class="toast.type">
        <CheckCircle2 v-if="toast.type === 'success'" :size="17" />
        <AlertTriangle v-else-if="toast.type === 'warning'" :size="17" />
        <XCircle v-else-if="toast.type === 'error'" :size="17" />
        <Info v-else :size="17" />
        <span>{{ toast.message }}</span>
        <button type="button" title="关闭提示" @click="removeToast(toast.id)">
          <X :size="14" />
        </button>
      </article>
    </div>

    <div v-if="environmentEditorOpen" class="modal-backdrop" @click.self="closeEnvironmentEditor">
      <form class="modal compact-modal environment-modal" role="dialog" aria-modal="true" aria-labelledby="environment-editor-title" @submit.prevent="saveDebugEnvironment">
        <header class="modal-header">
          <div>
            <h2 id="environment-editor-title">{{ environmentEditorMode === 'create' ? '新建 Environment' : '编辑 Environment' }}</h2>
            <p>Environment 是用户自己的变量集合；渠道只作为 Packager / Framing / Header 的协议模板。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeEnvironmentEditor">
            <X :size="16" />
          </button>
        </header>
        <div class="modal-form-body environment-form-body">
          <section class="environment-editor-section">
            <h3>基本信息</h3>
            <div class="field-grid compact-grid">
              <label>
                <span>环境名称</span>
                <input v-model="environmentEditor.name" required placeholder="例如 GH-GTB UAT" />
              </label>
              <label>
                <span>协议隧道</span>
                <input :value="environmentChannelLabel(environmentEditor)" readonly title="协议隧道由创建环境时绑定，不允许在环境内修改。" />
              </label>
            </div>
          </section>

          <section class="environment-editor-section">
            <div class="environment-section-heading">
              <h3>环境配置</h3>
              <button
                type="button"
                class="icon-mini-button"
                title="拉取当前协议渠道的第三方测试环境配置"
                @click="pullChannelTestEnvironmentIntoEditor"
              >
                <RefreshCw :size="13" />
              </button>
            </div>
            <div class="field-grid compact-grid">
              <label>
                <span>目标 IP</span>
                <input v-model="environmentEditor.targetIp" placeholder="第三方测试环境 IP" />
              </label>
              <label>
                <span>目标 Port</span>
                <input v-model="environmentEditor.targetPort" inputmode="numeric" placeholder="第三方测试环境端口" />
              </label>
              <label>
                <span>MAC 域</span>
                <select v-model="environmentEditor.macField">
                  <option value="">按 bitmap/能力自动</option>
                  <option value="64">DE64</option>
                  <option value="128">DE128</option>
                </select>
              </label>
              <label>
                <span>默认 MAC 算法</span>
                <select v-model="environmentEditor.macAlgorithm">
                  <option v-for="option in macAlgorithmOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>PIN 算法</span>
                <select v-model="environmentEditor.pinAlgorithm">
                  <option v-for="option in pinAlgorithmOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
          </section>

          <section class="environment-editor-section">
            <div class="environment-section-heading">
              <h3>环境变量</h3>
              <div class="environment-variable-toolbar">
                <input v-model="environmentVariableSearch" placeholder="Search variables" />
                <button type="button" class="secondary mini-button" @click="addEnvironmentVariableRow">
                  <Plus :size="13" />
                  Add
                </button>
              </div>
            </div>
            <div class="environment-variable-editor">
              <div class="environment-variable-editor-header">
                <span>Variable</span>
                <span>Value</span>
                <span>Description</span>
                <span></span>
              </div>
              <div
                v-for="variable in filteredEnvironmentVariableEditorRows"
                :key="variable.id"
                class="environment-variable-editor-row"
              >
                <input v-model="variable.key" placeholder="variable_name" />
                <input v-model="variable.value" :type="variable.sensitive ? 'password' : 'text'" placeholder="value" />
                <input v-model="variable.description" placeholder="description" />
                <button type="button" title="删除变量" @click="removeEnvironmentVariableRow(variable.id)">
                  <Trash2 :size="14" />
                </button>
              </div>
              <div v-if="filteredEnvironmentVariableEditorRows.length === 0" class="empty-inline">No variables.</div>
            </div>
          </section>

          <section class="environment-editor-section">
            <h3>动态密钥</h3>
            <div class="field-grid compact-grid">
              <label>
                <span>TMK 明文</span>
                <input v-model="environmentEditor.tmkPlain" spellcheck="false" placeholder="16、32 或 48 位 HEX" />
              </label>
              <label>
                <span>TPK 明文</span>
                <input v-model="environmentEditor.tpkPlain" spellcheck="false" placeholder="16、32 或 48 位 HEX" />
              </label>
              <label>
                <span>TSK 明文</span>
                <input v-model="environmentEditor.tskPlain" spellcheck="false" placeholder="16、32 或 48 位 HEX" />
              </label>
            </div>
          </section>

          <section class="environment-editor-section">
            <h3>动态参数</h3>
            <div class="field-grid compact-grid">
              <label>
                <span>DE18 / MCC</span>
                <input v-model="environmentEditor.testDe18" inputmode="numeric" maxlength="4" />
              </label>
              <label>
                <span>DE42 / 商户号</span>
                <input v-model="environmentEditor.testDe42" maxlength="15" />
              </label>
              <label>
                <span>DE43 / 商户名称地址</span>
                <input v-model="environmentEditor.testDe43" maxlength="40" />
              </label>
              <label>
                <span>DE49 / 币种</span>
                <input v-model="environmentEditor.testDe49" inputmode="numeric" maxlength="3" />
              </label>
            </div>
          </section>
        </div>
        <footer class="modal-footer">
          <button
            v-if="environmentEditorMode === 'edit'"
            type="button"
            class="secondary danger-text"
            :disabled="debugEnvironments.length <= 1"
            @click="deleteDebugEnvironment"
          >
            <Trash2 :size="16" />
            删除
          </button>
          <span class="modal-footer-spacer"></span>
          <button type="button" class="secondary" @click="closeEnvironmentEditor">取消</button>
          <button type="submit" class="primary">
            <Save :size="16" />
            保存
          </button>
        </footer>
      </form>
    </div>

    <div v-if="channelSwitchOpen" class="modal-backdrop" @click.self="closeChannelSwitcher">
      <section class="modal compact-modal channel-switch-modal" role="dialog" aria-modal="true" aria-labelledby="channel-switch-title">
        <header class="modal-header">
          <div>
            <h2 id="channel-switch-title">切换渠道</h2>
            <p>选择当前工作上下文，规则、验证、Debug 会切到对应渠道。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeChannelSwitcher">
            <X :size="16" />
          </button>
        </header>
        <section v-if="selectedChannel" class="channel-mock-summary" aria-label="当前渠道 Mock 环境">
          <div class="channel-mock-summary-title">
            <strong>{{ channelDisplayCode(selectedChannel) }}</strong>
            <span>Mock 环境</span>
          </div>
          <div class="channel-mock-grid">
            <button type="button" @click="copyChannelMockValue('监听 IP', channelMockHost(selectedChannel))">
              <span>监听 IP</span>
              <code>{{ channelMockHost(selectedChannel) }}</code>
            </button>
            <button type="button" @click="copyChannelMockValue('监听端口', channelMockPort(selectedChannel))">
              <span>监听端口</span>
              <code>{{ channelMockPort(selectedChannel) }}</code>
            </button>
            <button type="button" :disabled="!channelMockCtmk1(selectedChannel)" @click="copyChannelMockValue('CTMK1', channelMockCtmk1(selectedChannel))">
              <span>CTMK1</span>
              <code>{{ channelMockCtmk1(selectedChannel) || '-' }}</code>
            </button>
            <button type="button" :disabled="!channelMockCtmk2(selectedChannel)" @click="copyChannelMockValue('CTMK2', channelMockCtmk2(selectedChannel))">
              <span>CTMK2</span>
              <code>{{ channelMockCtmk2(selectedChannel) || '-' }}</code>
            </button>
          </div>
        </section>
        <div class="channel-switch-list">
          <button
            v-for="channel in channels"
            :key="channel.id"
            type="button"
            class="channel-switch-item"
            :class="{ active: selectedChannel?.id === channel.id }"
            @click="chooseChannel(channel.id)"
          >
            <span class="status-dot" :class="{ on: channel.enabled !== false }"></span>
            <span>
              <strong>{{ channelDisplayCode(channel) }}</strong>
              <small>{{ channelMockTlsLabel(channel) }} {{ channelMockHost(channel) }}:{{ channelMockPort(channel) }} · {{ channel.packager || channel.config?.packager || '-' }}</small>
            </span>
            <code v-if="selectedChannel?.id === channel.id">CURRENT</code>
          </button>
          <div v-if="channels.length === 0" class="empty-inline">暂无可用渠道。</div>
        </div>
        <footer class="modal-footer">
          <button type="button" class="secondary" @click="createChannelFromSwitcher">
            <Plus :size="16" />
            新增渠道
          </button>
          <button type="button" class="secondary" :disabled="!selectedChannel" @click="editChannelFromSwitcher">
            <Pencil :size="16" />
            编辑当前
          </button>
        </footer>
      </section>
    </div>

    <div v-if="createUserOpen" class="modal-backdrop" @click.self="closeCreateUserModal">
      <form class="modal compact-modal" role="dialog" aria-modal="true" aria-labelledby="create-user-title" @submit.prevent="createUser">
        <header class="modal-header">
          <div>
            <h2 id="create-user-title">新建用户</h2>
            <p>默认密码为 123456；用户名不允许重复。</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeCreateUserModal">
            <X :size="16" />
          </button>
        </header>
        <div class="modal-form-body">
          <label>
            <span>用户名</span>
            <input v-model="newUser.username" required placeholder="例如 ops-user" />
          </label>
          <div v-if="isUsernameDuplicate" class="notice inline-notice">
            <AlertTriangle :size="17" />
            <span>用户名已存在，请换一个用户名。</span>
          </div>
        </div>
        <footer class="modal-footer">
          <button type="button" class="secondary" @click="closeCreateUserModal">取消</button>
          <button type="submit" class="primary" :disabled="isUsernameDuplicate">
            <Plus :size="16" />
            创建
          </button>
        </footer>
      </form>
    </div>

    <div v-if="createTokenOpen" class="modal-backdrop" @click.self="closeCreateTokenModal">
      <form class="modal compact-modal" role="dialog" aria-modal="true" aria-labelledby="create-token-title" @submit.prevent="createToken">
        <header class="modal-header">
          <div>
            <h2 id="create-token-title">创建 MCP Token</h2>
            <p>{{ selectedUser?.username || '-' }} · 有效 Token {{ activeTokenCount }}/3</p>
          </div>
          <button class="ghost" type="button" title="关闭" @click="closeCreateTokenModal">
            <X :size="16" />
          </button>
        </header>
        <div class="modal-form-body">
          <label>
            <span>Token 名称</span>
            <input v-model="newTokenName" required placeholder="例如 AI Agent" />
          </label>
          <div v-if="tokenLimitReached" class="notice inline-notice muted-notice">
            <AlertTriangle :size="17" />
            <span>每个用户最多保留 3 个有效 MCP Token。</span>
          </div>
        </div>
        <footer class="modal-footer">
          <button type="button" class="secondary" @click="closeCreateTokenModal">取消</button>
          <button type="submit" class="primary" :disabled="!selectedUser || tokenLimitReached">
            <KeyRound :size="16" />
            创建
          </button>
        </footer>
      </form>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Copy,
  Cpu,
  Eye,
  FlaskConical,
  Info,
  KeyRound,
  ListChecks,
  LogOut,
  Network,
  Pencil,
  Play,
  Plus,
  Power,
  RefreshCw,
  Save,
  Server,
  Share2,
  Trash2,
  Upload,
  UserCircle,
  Users,
  X,
  XCircle
} from 'lucide-vue-next';
import { api } from './api/client';
import CodeBlock from './components/CodeBlock.vue';
import CodeEditor from './components/CodeEditor.vue';

const capabilityOptions = [
  { value: 'DEBIT', label: '扣款', requestMti: '0200', processCode: '000000', responseMti: '0210' },
  { value: 'BALANCE_QUERY', label: '余额查询', requestMti: '0100', processCode: '310000', responseMti: '0110' },
  { value: 'PRE_AUTH', label: '预授权', requestMti: '0100', processCode: '600000', responseMti: '0110' },
  { value: 'PRE_AUTH_COMPLETION', label: '预授权完成', requestMti: '0220', processCode: '610000', responseMti: '0230' },
  { value: 'REVERSAL', label: '冲正', requestMti: '0400', processCode: '000000', responseMti: '0410' },
  { value: 'REFUND', label: '退款', requestMti: '0200', processCode: '200000', responseMti: '0210' },
  { value: 'TID_INIT_9A', label: 'TID-TMK', requestMti: '0800', processCode: '9A0000', responseMti: '0810' },
  { value: 'TID_INIT_9G', label: 'TID-TPK', requestMti: '0800', processCode: '9G0000', responseMti: '0810' },
  { value: 'TID_INIT_9B', label: 'TID-TSK', requestMti: '0800', processCode: '9B0000', responseMti: '0810' },
  { value: 'PARAMETER_DOWNLOAD', label: '参数下载', requestMti: '0800', processCode: '9C0000', responseMti: '0810' },
  { value: 'CALLHOME', label: 'Callhome', requestMti: '0800', processCode: '9D0000', responseMti: '0810' }
];
const capabilityAliases = {
  TID_INIT: 'TID_INIT_9A',
  TID_INIT_9C: 'TID_INIT_9B'
};

const mcpCapabilityGroups = [
  {
    title: '渠道能力',
    tools: ['channel_list', 'channel_get', 'channel_create', 'channel_update', 'channel_packager_preview']
  },
  {
    title: 'Key能力',
    tools: ['key_get', 'key_update']
  },
  {
    title: '规则能力',
    tools: ['rule_capabilities', 'rule_list', 'rule_create', 'rule_update', 'rule_set_enabled', 'rule_delete']
  },
  {
    title: '验证能力',
    tools: ['rule_validate_xml', 'rule_validation_history', 'public_rule_list', 'public_rule_copy']
  },
  {
    title: 'POS Debug能力',
    tools: ['debug_pos_build_xml', 'debug_pos_send', 'debug_pos_tid_init']
  }
];

const mcpCurlExample = `curl -X POST http://127.0.0.1:18080/mcp \\
  -H "Authorization: Bearer mcp_xxx" \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'`;

const mcpJsonExample = `{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "rule_capabilities",
    "arguments": {
      "channelCode": "GH-GTB"
    }
  }
}`;

const responseCodeReferences = [
  { code: '00', label: '批准或成功', note: '正常返回，交易完成。' },
  { code: '01', label: '请持卡人与发卡行联系', note: '建议提示客户联系发卡行。' },
  { code: '02', label: '请联系发卡行特殊处理', note: '需要发卡行人工或特殊授权。' },
  { code: '03', label: '无效商户', note: '检查 DE42、商户配置或渠道入网状态。' },
  { code: '04', label: '没收卡', note: '线下场景可能要求吞卡或保留卡。' },
  { code: '05', label: '拒绝', note: '常见通用拒绝码，需结合渠道日志定位。' },
  { code: '06', label: '错误', note: '发卡方或系统处理异常。' },
  { code: '07', label: '没收卡并特殊处理', note: '高风险拒绝，按渠道风控流程处理。' },
  { code: '08', label: '批准，需身份识别', note: '部分场景表示授权成功但需额外识别。' },
  { code: '09', label: '请求处理中', note: '可按渠道规则查询或重试。' },
  { code: '10', label: '部分批准', note: '金额或授权只部分成功。' },
  { code: '11', label: 'VIP 批准', note: '成功类响应，具体含义按渠道文档。' },
  { code: '12', label: '无效交易', note: '检查 MTI、DE3、交易能力或渠道开关。' },
  { code: '13', label: '无效金额', note: '检查 DE4 格式、币种和限额。' },
  { code: '14', label: '无效卡号', note: '检查 DE2/PAN、磁道或卡 BIN。' },
  { code: '15', label: '无此发卡行', note: '路由或 BIN 未识别。' },
  { code: '16', label: '批准并更新第三磁道', note: '少见成功类响应，以渠道规范为准。' },
  { code: '17', label: '客户取消', note: '终端或用户主动取消。' },
  { code: '18', label: '客户争议', note: '常见争议或拒绝类占位。' },
  { code: '19', label: '重新交易', note: '建议重新发起或按渠道规则冲正。' },
  { code: '20', label: '无效响应', note: '主机返回内容不符合预期。' },
  { code: '21', label: '无操作记录', note: '原交易不存在或无法定位。' },
  { code: '22', label: '疑似故障', note: '系统或主机侧异常。' },
  { code: '23', label: '不可接受的交易费', note: '费用字段或计费配置不匹配。' },
  { code: '24', label: '文件更新不支持', note: '参数或文件更新类交易受限。' },
  { code: '25', label: '找不到原始交易', note: '冲正、退款或完成类交易常见。' },
  { code: '26', label: '重复文件记录', note: '文件或批处理记录重复。' },
  { code: '27', label: '文件更新字段错误', note: '检查参数下载或文件更新字段。' },
  { code: '28', label: '文件锁定', note: '主机文件或参数表暂不可用。' },
  { code: '29', label: '文件更新失败', note: '主机侧更新未完成。' },
  { code: '30', label: '格式错误', note: '检查 Packager、长度、bitmap 和字段格式。' },
  { code: '31', label: '银行不支持', note: '收单或发卡机构不支持该交易。' },
  { code: '32', label: '部分完成', note: '批处理或文件类交易可能出现。' },
  { code: '33', label: '卡过期', note: '检查 DE14 或卡状态。' },
  { code: '34', label: '疑似欺诈', note: '风控拒绝，高风险处理。' },
  { code: '35', label: '联系收单方', note: '需联系收单机构或服务商。' },
  { code: '36', label: '受限卡', note: '卡状态受限或不能用于该交易。' },
  { code: '37', label: '联系收单安全部门', note: '安全或风控异常。' },
  { code: '38', label: 'PIN 尝试次数超限', note: '持卡人 PIN 错误次数过多。' },
  { code: '39', label: '无贷记账户', note: '账户类型或账户状态不支持。' },
  { code: '40', label: '请求功能不支持', note: '交易能力未开通或 DE3 不支持。' },
  { code: '41', label: '挂失卡', note: '卡已挂失。' },
  { code: '42', label: '无账户', note: '借记/账户不存在或不可用。' },
  { code: '43', label: '被盗卡', note: '高风险卡状态。' },
  { code: '44', label: '无投资账户', note: '账户类型不适用。' },
  { code: '45', label: '预留', note: '不同渠道可能自定义。' },
  { code: '46', label: '预留', note: '不同渠道可能自定义。' },
  { code: '47', label: '预留', note: '不同渠道可能自定义。' },
  { code: '48', label: '预留', note: '不同渠道可能自定义。' },
  { code: '49', label: '预留', note: '不同渠道可能自定义。' },
  { code: '50', label: '预留', note: '不同渠道可能自定义。' },
  { code: '51', label: '余额不足', note: '常见扣款失败。' },
  { code: '52', label: '无支票账户', note: '账户类型不支持。' },
  { code: '53', label: '无储蓄账户', note: '账户类型不支持。' },
  { code: '54', label: '卡已过期', note: '检查卡有效期和卡状态。' },
  { code: '55', label: 'PIN 错误', note: '检查 PIN Block、密钥和密码。' },
  { code: '56', label: '无卡记录', note: '卡未建档或路由不可识别。' },
  { code: '57', label: '持卡人不允许此交易', note: '卡产品或账户权限不支持。' },
  { code: '58', label: '终端不允许此交易', note: '商户、终端或渠道权限不支持。' },
  { code: '59', label: '疑似欺诈', note: '风控拒绝。' },
  { code: '60', label: '联系收单行', note: '收单机构处理异常或需人工处理。' },
  { code: '61', label: '超过取款限额', note: '金额超限或日限额超限。' },
  { code: '62', label: '受限卡', note: '卡片受限。' },
  { code: '63', label: '安全违规', note: 'MAC、PIN、密钥或安全策略异常。' },
  { code: '64', label: '原交易金额不正确', note: '完成、冲正或退款金额与原交易不符。' },
  { code: '65', label: '超过取款次数限制', note: '次数限制命中。' },
  { code: '66', label: '联系收单安全部门', note: '安全或风控异常。' },
  { code: '67', label: '强制没收卡', note: '高风险卡状态。' },
  { code: '68', label: '响应超时', note: '可查询、冲正或按渠道超时流程处理。' },
  { code: '69', label: '预留', note: '不同渠道可能自定义。' },
  { code: '70', label: '预留', note: '不同渠道可能自定义。' },
  { code: '71', label: '预留', note: '不同渠道可能自定义。' },
  { code: '72', label: '预留', note: '不同渠道可能自定义。' },
  { code: '73', label: '预留', note: '不同渠道可能自定义。' },
  { code: '74', label: '预留', note: '不同渠道可能自定义。' },
  { code: '75', label: 'PIN 尝试次数超限', note: '常见与 38 类似，按渠道解释。' },
  { code: '76', label: '找不到原始交易', note: '冲正、退款、完成类交易常见。' },
  { code: '77', label: '原交易与当前交易不一致', note: '检查 RRN、STAN、金额和日期。' },
  { code: '78', label: '账户不存在或未激活', note: '卡或账户未激活。' },
  { code: '79', label: '生命周期状态不正确', note: '卡或账户状态不允许当前交易。' },
  { code: '80', label: '日期错误', note: '检查 DE7、DE12、DE13 或清算日期。' },
  { code: '81', label: '加密错误', note: 'PIN/MAC/密钥计算异常。' },
  { code: '82', label: 'CVV/CVC 错误', note: '卡验证值失败。' },
  { code: '83', label: 'PIN 验证失败', note: '密码校验异常。' },
  { code: '84', label: '生命周期状态异常', note: '卡状态不允许当前交易。' },
  { code: '85', label: '无理由拒绝或余额查询成功', note: '部分渠道余额查询成功用 85，需看规范。' },
  { code: '86', label: 'PIN 验证不可用', note: '发卡方或安全服务不可用。' },
  { code: '87', label: '金额或限额相关拒绝', note: '常见渠道自定义。' },
  { code: '88', label: '加密校验失败', note: '密钥、MAC、PIN 或安全域异常。' },
  { code: '89', label: '终端认证失败', note: '终端、商户或密钥认证异常。' },
  { code: '90', label: '日终处理中', note: '主机切日或批处理，稍后重试。' },
  { code: '91', label: '发卡方不可达', note: '路由、网络或发卡主机不可用。' },
  { code: '92', label: '路由不可用', note: '金融机构或渠道路由找不到。' },
  { code: '93', label: '交易不能完成', note: '法律、风控或账户限制。' },
  { code: '94', label: '重复交易', note: 'STAN/RRN 或幂等命中。' },
  { code: '95', label: '对账差错', note: '批结算或对账状态异常。' },
  { code: '96', label: '系统故障', note: '主机或处理系统异常，常用默认异常码。' },
  { code: '97', label: '预留或终端异常', note: '不同渠道可能自定义。' },
  { code: '98', label: '预留或超时异常', note: '不同渠道可能自定义。' },
  { code: '99', label: '预留或通用错误', note: '不同渠道可能自定义。' }
];

const isoStructureItems = [
  { title: 'Frame', description: 'TCP 传输层边界，常见为 2 字节长度前缀，用来告诉接收方读取多少字节。' },
  { title: 'TPDU/Header', description: '渠道头，一般位于 ISO Body 前，用于网络路由、版本或接入标识。是否存在、长度和返回方式按渠道配置。' },
  { title: 'MTI', description: '4 位消息类型，例如 0200 请求、0210 响应、0400 冲正请求、0410 冲正响应。' },
  { title: 'Bitmap', description: '字段存在性位图。主位图覆盖 1-64，二级位图覆盖 65-128。DE64/DE128 的 MAC 选择应优先看 bitmap。' },
  { title: 'Data Elements', description: '业务字段集合，例如 DE3 处理码、DE4 金额、DE11 流水号、DE39 响应码、DE55 IC 数据。' },
  { title: 'MAC', description: '报文鉴别码。参数下载和 Callhome 常用 DE64，其它交易常用 DE128，但最终以报文 bitmap 和渠道规范为准。' }
];

const authReady = ref(false);
const currentUser = ref(null);
const accountMenuOpen = ref(false);
const channelSwitchOpen = ref(false);
const loginForm = ref({ username: 'admin', password: '' });
const loginLoading = ref(false);
const loginError = ref('');
const channels = ref([]);
const selectedChannel = ref(null);
const users = ref([]);
const selectedUser = ref(null);
const mcpTokens = ref([]);
const newUser = ref({ username: '' });
const newTokenName = ref('AI Agent');
const createUserOpen = ref(false);
const createTokenOpen = ref(false);
const rules = ref([]);
const publicRules = ref([]);
const fieldCatalog = ref([]);
const editingRule = ref(emptyRule());
const channelEditor = ref(emptyChannel());
const ruleView = ref('list');
const ruleSection = ref('match');
const selectedCapabilityFilter = ref('ALL');
const channelSection = ref('basic');
const channelMode = ref('create');
const responseRows = ref([]);
const responseXmlImport = ref('');
const responseInputMode = ref('fields');
const testHistory = ref([]);
const selectedTestHistoryId = ref(null);
const tab = ref('rules');
const restartNotice = ref('');
const testXml = ref(`<isomsg>
  <mti id="MTI" value="0200"/>
  <field id="0" value="0200"/>
  <field id="3" value="000000"/>
  <field id="4" value="000000010000"/>
  <field id="11" value="123456"/>
  <field id="41" value="TERM0001"/>
</isomsg>`);
const testResult = ref('');
const keySettings = ref(emptyKeySettings());
const keyLoading = ref(false);
const debugRequest = ref(emptyDebugRequest());
const debugTidInit = ref(emptyDebugTidInit());
const debugFieldRows = ref(defaultDebugFieldRows());
const debugResult = ref(null);
const debugResultKind = ref('');
const debugLoading = ref(false);
const debugConsoleOutputRef = ref(null);
const debugMessageView = ref('xml');
const debugToolModal = ref('');
const debugHistory = ref([]);
const debugConsoleEntries = ref([]);
const collapsedDebugPanels = ref({
  request: false,
  response: false,
  history: false,
  console: false
});
const debugCollections = ref([]);
const activeDebugCollectionId = ref('');
const activeDebugRequestId = ref('');
const editingDebugCollectionId = ref('');
const editingDebugRequestId = ref('');
const debugEnvironments = ref([]);
const activeEnvironmentId = ref('');
const debugTargetInput = ref('');
const environmentSwitcherOpen = ref(false);
const environmentEditorOpen = ref(false);
const environmentEditorMode = ref('create');
const environmentEditor = ref(emptyDebugEnvironment());
const environmentVariableEditorRows = ref([]);
const environmentVariableSearch = ref('');
const smartReplaceOpen = ref(false);
const smartReplaceSelection = ref(defaultSmartReplaceSelection());
const smartReplaceContext = ref(null);
const smartReplaceChanges = ref([]);
const smartReplaceXmlDraft = ref('');
const wirePreview = ref(null);
const responseHexInput = ref('');
const parsedResponseXml = ref('');
const infoCenterOpen = ref(false);
const infoCenterTab = ref('mcp');
const packagerPreviewLoading = ref(false);
const packagerPreviewError = ref('');
const packagerPreview = ref(null);
const draftPackagerPreviewVisible = ref(false);
const toasts = ref([]);
let toastSequence = 0;

const smartReplaceScopeOptions = [
  {
    key: 'dynamicParameters',
    label: '动态参数',
    fields: 'DE18 / DE42 / DE43 / DE49',
    description: '来自参数下载后沉淀的 MCC、商户、币种等字段。'
  },
  {
    key: 'environmentVariables',
    label: '环境变量',
    fields: 'DE2 / DE14 / DE41 / DE52',
    description: '来自当前 Environment 的测试卡、终端、PIN Block 等变量。'
  },
  {
    key: 'stan',
    label: 'STAN',
    fields: 'DE11',
    description: '生成 6 位随机流水号。'
  },
  {
    key: 'timestamps',
    label: '时间戳',
    fields: 'DE7 / DE12 / DE13',
    description: '使用当前时间填充交易传输时间、本地时间和本地日期。'
  },
  {
    key: 'rrn',
    label: 'RRN',
    fields: 'DE37',
    description: '使用当前年月日时分秒生成 12 位检索参考号。'
  }
];

const operatorLabels = {
  EQ: '= 等于',
  NE: '!= 不等于',
  EXISTS: '存在',
  NOT_EXISTS: '不存在',
  CONTAINS: '包含',
  NOT_CONTAINS: '不包含',
  GT: '> 大于',
  GTE: '>= 大于等于',
  LT: '< 小于',
  LTE: '<= 小于等于',
  REGEX: '正则匹配',
  IN: '在列表中'
};
const operatorsByValueType = {
  NUMERIC: ['EQ', 'NE', 'EXISTS', 'NOT_EXISTS', 'GT', 'GTE', 'LT', 'LTE', 'IN'],
  TEXT: ['EQ', 'NE', 'EXISTS', 'NOT_EXISTS', 'CONTAINS', 'NOT_CONTAINS', 'REGEX', 'IN'],
  BINARY: ['EQ', 'NE', 'EXISTS', 'NOT_EXISTS', 'IN'],
  UNKNOWN: ['EQ', 'NE', 'EXISTS', 'NOT_EXISTS', 'CONTAINS', 'NOT_CONTAINS', 'REGEX', 'IN']
};
const actionOptions = [
  { type: 'RESPOND', label: '立即响应', description: '命中后直接返回配置字段' },
  { type: 'DELAY_RESPOND', label: '延迟响应', description: '等待指定毫秒后再返回' },
  { type: 'TIMEOUT', label: '模拟超时', description: '保持连接但不返回报文' },
  { type: 'DISCONNECT', label: '模拟断连', description: '命中后立即关闭连接' }
];
const pinAlgorithmOptions = [
  { value: 'NONE', label: '不处理 PIN', description: '不生成或覆盖 DE52。' },
  { value: 'ISO0_3DES_ECB', label: 'ISO-0 PIN Block', description: '使用明文 TPK 生成 ISO-0 PIN Block，并写入 DE52。' }
];
const macAlgorithmOptions = [
  {
    value: 'ANSI_X9_19',
    label: 'ANSI X9.19',
    description: '传统 Retail MAC；MAC 域按 bitmap/能力自动判定。'
  },
  {
    value: 'SHA256_FIELD128_TRIM64',
    label: 'SHA-256 Field128',
    description: 'DE128 补 0 后 pack，裁掉最后 64 bytes，计算 SHA-256(hex2byte(TSK)+trimmedPacked)。'
  }
];
const filteredRules = computed(() => selectedCapabilityFilter.value === 'ALL'
  ? rules.value
  : rules.value.filter((rule) => rule.capability === selectedCapabilityFilter.value));
const enabledFilteredRuleCount = computed(() => filteredRules.value.filter((rule) => rule.enabled).length);
const isValidationTab = computed(() => ['test', 'test-history', 'test-history-detail'].includes(tab.value));
const isDebugTab = computed(() => ['debug-pos', 'debug-http'].includes(tab.value));
const isUserTab = computed(() => ['users', 'user-detail'].includes(tab.value));
const activeModule = computed(() => {
  if (['channel-new', 'keys', 'packager-preview'].includes(tab.value)) return 'channel';
  if (isDebugTab.value) return 'debug';
  if (isUserTab.value) return 'system';
  return 'mock';
});
const moduleTitle = computed(() => {
  const titles = {
    channel: '渠道配置',
    mock: 'Mock工具',
    debug: '调试工具',
    system: '系统'
  };
  return titles[activeModule.value] || 'Mock工具';
});
const moduleSubtitle = computed(() => {
  const subtitles = {
    channel: '渠道、Packager、Key',
    mock: '规则、验证、公开复用',
    debug: '真实环境联调',
    system: '用户、Token、说明'
  };
  return subtitles[activeModule.value] || '';
});
const selectedUserInitial = computed(() => userDisplayName(selectedUser.value).slice(0, 1).toUpperCase() || '-');
const currentUserInitial = computed(() => userDisplayName(currentUser.value).slice(0, 1).toUpperCase() || '-');
const isAdminUser = computed(() => Boolean(currentUser.value?.admin) || currentUser.value?.username?.toLowerCase() === 'admin');
const canViewSelectedUserDetail = computed(() => canAccessUserProfile(selectedUser.value));
const isUsernameDuplicate = computed(() => {
  const username = newUser.value.username.trim();
  return Boolean(username) && users.value.some((user) => user.username === username);
});
const activeMcpTokens = computed(() => mcpTokens.value.filter((token) => !token.revoked));
const activeTokenCount = computed(() => activeMcpTokens.value.length);
const tokenLimitReached = computed(() => activeTokenCount.value >= 3);
const appBasePath = (import.meta.env.BASE_URL || '/').replace(/\/$/, '');
const publicAsset = (path) => `${appBasePath || ''}/${path.replace(/^\//, '')}`;
const brandLogoUrl = publicAsset('favicon.svg?v=2');
const mcpEndpointUrl = computed(() => `${window.location.origin}${appBasePath}/mcp`);
const backendMcpEndpointUrl = computed(() => `${window.location.protocol}//${window.location.hostname}:18080/mcp`);
const isChannelCodeDuplicate = computed(() => {
  const code = (channelEditor.value.channelCode || '').trim();
  return Boolean(code) && channels.value.some((channel) => channelDisplayCode(channel) === code && channel.id !== channelEditor.value.id);
});
const isChannelPortDuplicate = computed(() => {
  const port = Number(channelEditor.value.port);
  return Number.isInteger(port) && channels.value.some((channel) => Number(channel.port) === port && channel.id !== channelEditor.value.id);
});
const isChannelPackagerDuplicate = computed(() => {
  const fingerprint = channelPackagerFingerprint(channelEditor.value);
  if (!fingerprint) return false;
  return channels.value.some((channel) => channel.id !== channelEditor.value.id
    && channelPackagerFingerprint(channel.config || channel) === fingerprint);
});
const isEditingChannel = computed(() => channelMode.value === 'edit');
const fieldCatalogById = computed(() => {
  const fields = {};
  for (const field of fieldCatalog.value) {
    fields[canonicalFieldKey(field.id)] = normalizeFieldDefinition(field);
  }
  return fields;
});
const shortPackager = computed(() => {
  const value = selectedChannel.value?.packager || '';
  return value.split('.').pop() || value;
});
const actionUsesDelay = computed(() => ['DELAY_RESPOND', 'TIMEOUT'].includes(editingRule.value.action.type));
const actionReturnsResponse = computed(() => !['TIMEOUT', 'DISCONNECT'].includes(editingRule.value.action.type));
const actionSummary = computed(() => {
  if (editingRule.value.action.type === 'DELAY_RESPOND') return '命中后等待指定毫秒，再返回配置的响应。';
  if (editingRule.value.action.type === 'TIMEOUT') return '命中后保持连接，不返回响应报文。';
  if (editingRule.value.action.type === 'DISCONNECT') return '命中后立即断开连接。';
  return '命中后立即返回配置的响应。';
});
const pageTitle = computed(() => {
  if (tab.value === 'channel-new') return isEditingChannel.value ? '编辑渠道' : '新增渠道';
  if (tab.value === 'rules') return ruleView.value === 'detail' ? '规则详情' : '规则管理';
  if (tab.value === 'test') return '规则验证';
  if (tab.value === 'test-history') return '历史记录';
  if (tab.value === 'test-history-detail') return '历史详情';
  if (tab.value === 'debug-pos') return 'POS Debug';
  if (tab.value === 'debug-http') return 'HTTP Debug';
  if (tab.value === 'keys') return 'Key设置';
  if (tab.value === 'public-rules') return '公开规则';
  if (tab.value === 'users') return '用户管理';
  if (tab.value === 'user-detail') return selectedUser.value?.username || '用户详情';
  return selectedChannel.value ? channelDisplayCode(selectedChannel.value) : 'Faker';
});
const pageScopeLabel = computed(() => {
  if (['rules', 'test', 'test-history', 'test-history-detail'].includes(tab.value)) return '用户数据';
  if (['channel-new', 'keys', 'public-rules'].includes(tab.value)) return '全局数据';
  if (isDebugTab.value) return '联调工具';
  return '';
});
const pageScopeClass = computed(() => pageScopeLabel.value === '用户数据' ? 'scope-user' : 'scope-global');
const pageSubtitle = computed(() => {
  if (tab.value === 'users') return '用户、MCP Token 和 AI 调用身份';
  if (tab.value === 'user-detail') return `MCP Token 管理 · 有效 ${activeTokenCount.value}/3`;
  if (tab.value === 'channel-new') return isEditingChannel.value
    ? '全局渠道配置；运行时字段变更需重启'
    : '全局渠道配置；运行时配置需重启，资料字段热生效';
  if (!selectedChannel.value) return '';
  if (tab.value === 'rules') {
    const visibleRules = ruleView.value === 'list' ? filteredRules.value : rules.value;
    const enabledVisibleRules = visibleRules.filter((rule) => rule.enabled).length;
    const scope = selectedCapabilityFilter.value === 'ALL' ? '规则按优先级匹配' : `${capabilityLabel(selectedCapabilityFilter.value)}规则`;
    return `${scope} · ${enabledVisibleRules}/${visibleRules.length} 已启用`;
  }
  if (tab.value === 'test') return '当前用户的一次性验证；历史记录只保存当前用户最近 30 条';
  if (tab.value === 'test-history') return `当前用户最近 ${testHistory.value.length}/30 条验证记录`;
  if (tab.value === 'test-history-detail') return '当前用户历史；可载入重新验证';
  if (tab.value === 'debug-pos') return '真实第三方测试环境联调；请求、响应、MAC 验证和 Console 日志在同一工作台完成';
  if (tab.value === 'debug-http') return 'HTTP 类接口调试后续扩展，当前先保留模块入口';
  if (tab.value === 'keys') return '全局 Key 配置；对当前渠道下所有用户生效';
  if (tab.value === 'public-rules') return '全局共享规则库；复制后进入当前用户规则';
  return `${channelDisplayCode(selectedChannel.value)} · ${selectedChannel.value.framing} · ${selectedChannel.value.packager}`;
});
const selectedTestHistory = computed(() => testHistory.value.find((history) => history.id === selectedTestHistoryId.value));
const activeEnvironment = computed(() => debugEnvironments.value.find((environment) => environment.id === activeEnvironmentId.value) || null);
const debugProtocolChannel = computed(() => {
  const channelId = activeEnvironment.value?.channelId;
  return channels.value.find((channel) => channel.id === channelId)
    || selectedChannel.value
    || channels.value[0]
    || null;
});
const debugChannelId = computed(() => debugProtocolChannel.value?.id || selectedChannel.value?.id || '');
const debugChannelCode = computed(() => channelDisplayCode(debugProtocolChannel.value));
const debugShortPackager = computed(() => {
  const value = debugProtocolChannel.value?.packager || debugProtocolChannel.value?.config?.packager || '';
  return value.split('.').pop() || value || '-';
});
const debugTransportProtocol = computed(() => (debugProtocolChannel.value?.thirdPartyTlsEnabled || debugProtocolChannel.value?.config?.thirdPartyTlsEnabled) ? 'TLS' : 'TCP');
const debugTargetVariableMap = computed(() => buildDebugTargetVariableMap(activeEnvironment.value));
const debugResolvedTargetAddress = computed(() => resolveDebugTargetVariables(debugTargetInput.value));
const debugResolvedTarget = computed(() => parseDebugTargetAddress(debugResolvedTargetAddress.value));
const debugTargetConfigured = computed(() => Boolean(debugResolvedTarget.value.ip && debugResolvedTarget.value.port));
const activeEnvironmentName = computed(() => activeEnvironment.value?.name || 'No Environment');
const environmentVariables = computed(() => {
  const environment = activeEnvironment.value || {};
  return environmentVariableGroups.value.flatMap((group) => group.items);
});
const environmentVariableGroups = computed(() => {
  const environment = activeEnvironment.value || {};
  const customVariables = normalizeEnvironmentVariables(environment.variables);
  return [
    {
      title: '基本信息',
      items: [
        { key: 'env.name', value: activeEnvironmentName.value },
        { key: 'protocol.channel', value: debugChannelCode.value },
        { key: 'channel.packager', value: debugShortPackager.value }
      ]
    },
    {
      title: '环境配置',
      items: [
        { key: 'target.ip', value: environment.targetIp || '' },
        { key: 'target.port', value: environment.targetPort || '' },
        { key: 'mac.field', value: environment.macField || 'auto' },
        { key: 'mac.algorithm', value: environment.macAlgorithm || 'ANSI_X9_19' },
        { key: 'pin.algorithm', value: environment.pinAlgorithm || 'NONE' }
      ]
    },
    {
      title: '环境变量',
      items: [
        { key: 'DE41', value: environment.testTid || '' },
        { key: 'SN', value: environment.testSn || '' },
        { key: 'DE2', value: environment.testPan || '' },
        { key: 'DE14', value: environment.testDe14 || '' },
        { key: 'DE52', value: environment.testDe52 ? maskSecret(environment.testDe52) : '' },
        ...(customVariables.length
          ? customVariables.map((variable) => ({ key: variable.key, value: variable.value }))
          : [{ key: 'custom', value: '' }])
      ]
    },
    {
      title: '动态密钥',
      items: [
        { key: 'key.tmk', value: environment.tmkPlain ? maskSecret(environment.tmkPlain) : '' },
        { key: 'key.tpk', value: environment.tpkPlain ? maskSecret(environment.tpkPlain) : '' },
        { key: 'key.tsk', value: environment.tskPlain ? maskSecret(environment.tskPlain) : '' }
      ]
    },
    {
      title: '动态参数',
      items: [
        { key: 'param.de18', value: environment.testDe18 || '' },
        { key: 'param.de42', value: environment.testDe42 || '' },
        { key: 'param.de43', value: environment.testDe43 || '' },
        { key: 'param.de49', value: environment.testDe49 || '' }
      ]
    }
  ];
});
const environmentVariableCount = computed(() => environmentVariables.value.filter((item) => item.value !== null && item.value !== undefined && String(item.value) !== '').length);
const filteredEnvironmentVariableEditorRows = computed(() => {
  const search = environmentVariableSearch.value.trim().toLowerCase();
  if (!search) return environmentVariableEditorRows.value;
  return environmentVariableEditorRows.value.filter((row) => [
    row.key,
    row.value,
    row.description
  ].some((value) => String(value || '').toLowerCase().includes(search)));
});
const smartReplaceSelectedCount = computed(() => smartReplaceScopeOptions
  .filter((option) => smartReplaceSelection.value[option.key])
  .length);
const smartReplaceSelectedFields = computed(() => smartReplaceScopeOptions
  .filter((option) => smartReplaceSelection.value[option.key])
  .map((option) => option.fields)
  .join(' · '));
const activeDebugCollection = computed(() => debugCollections.value.find((collection) => collection.id === activeDebugCollectionId.value));
const activeDebugRequest = computed(() => activeDebugCollection.value?.requests.find((request) => request.id === activeDebugRequestId.value));
const debugSelectedCapability = computed(() => capabilityByValue(debugRequest.value.capability));
const debugEnvironmentTid = computed(() => activeEnvironment.value?.testTid || '');
const debugEnvironmentSn = computed(() => activeEnvironment.value?.testSn || '');
const debugEnvironmentMacAlgorithm = computed(() => activeEnvironment.value?.macAlgorithm || 'ANSI_X9_19');
const debugEnvironmentPinAlgorithm = computed(() => activeEnvironment.value?.pinAlgorithm || 'NONE');
const debugRequiresPin = computed(() => debugEnvironmentPinAlgorithm.value === 'ISO0_3DES_ECB');
const debugCanSaveTidConfig = computed(() => debugRequest.value.capability === 'PARAMETER_DOWNLOAD');
const debugTargetSummary = computed(() => {
  const target = debugTargetConfigured.value
    ? debugResolvedTargetAddress.value
    : '未配置目标环境';
  return `${target} · ${debugChannelCode.value} · ${debugShortPackager.value}`;
});
const debugSteps = computed(() => debugResult.value?.steps || []);
const debugRequestAscii = computed(() => debugSteps.value.length
  ? debugSteps.value.map((step) => formatStepAscii(step, 'request')).join('\n\n')
  : '');
const debugResponseAscii = computed(() => debugSteps.value.length
  ? debugSteps.value.map((step) => formatStepAscii(step, 'response')).join('\n\n')
  : '');
const debugResponseXml = computed(() => {
  if (!debugSteps.value.length) return '';
  return debugSteps.value.map((step) => {
    const label = step.label || capabilityLabel(step.capability);
    const body = step.responseXml || step.errorMessage || '无响应内容';
    return `<!-- ${label} -->\n${body}`;
  }).join('\n\n');
});
const responseCodeSummary = computed(() => {
  if (!debugSteps.value.length) return '等待请求';
  const codes = debugSteps.value
    .map((step) => step.responseCode)
    .filter(Boolean);
  return codes.length ? `DE39 ${codes.join(' / ')}` : (debugResult.value?.success ? '请求完成' : '请求失败');
});
const debugConsoleText = computed(() => debugConsoleEntries.value.map((entry) => entry.text).join('\n'));

function channelDisplayCode(channel) {
  return channel?.channelCode || channel?.code || channel?.name || channel?.id || '-';
}

function channelConfigValue(channel, key) {
  return channel?.config?.[key] ?? channel?.[key] ?? '';
}

function channelMockHost(channel) {
  return channelConfigValue(channel, 'host') || '0.0.0.0';
}

function channelMockPort(channel) {
  return channelConfigValue(channel, 'port') || '-';
}

function channelMockTlsEnabled(channel) {
  const value = channelConfigValue(channel, 'mockTlsEnabled');
  return value === null || value === undefined || value === ''
    ? true
    : Boolean(value);
}

function channelMockTlsLabel(channel) {
  return channelMockTlsEnabled(channel) ? 'TLS' : 'TCP';
}

function channelMockCtmk1(channel) {
  return channelConfigValue(channel, 'mockCtmk1') || channelConfigValue(channel, 'ctmk1') || '';
}

function channelMockCtmk2(channel) {
  return channelConfigValue(channel, 'mockCtmk2') || channelConfigValue(channel, 'ctmk2') || '';
}

function environmentChannelLabel(environment) {
  const channel = channels.value.find((item) => item.id === environment?.channelId)
    || selectedChannel.value
    || channels.value[0]
    || {};
  const packager = channel.packager || channel.config?.packager || '-';
  return `${channelDisplayCode(channel)} · ${packager}`;
}

function userDisplayName(user) {
  return user?.username || '';
}

function canAccessUserProfile(user) {
  if (!currentUser.value || !user) {
    return false;
  }
  return isAdminUser.value || user.id === currentUser.value.id;
}

function maskSecret(value) {
  if (!value) return '-';
  if (value.length <= 14) return `${value.slice(0, 4)}...${value.slice(-4)}`;
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
});

function formatDateTime(value, fallback = '-') {
  if (!value) return fallback;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;
  return dateTimeFormatter.format(date);
}

function ruleOwnerName(rule) {
  if (rule?.ownerUsername) return rule.ownerUsername;
  const owner = users.value.find((user) => user.id === rule?.ownerUserId);
  if (owner) return owner.username;
  if (!rule?.id && currentUser.value?.username) return currentUser.value.username;
  return rule?.ownerUserId ? `用户 #${rule.ownerUserId}` : '-';
}

onMounted(async () => {
  if (!api.hasAuthToken()) {
    authReady.value = true;
    return;
  }
  try {
    currentUser.value = await api.me();
    await loadAppData();
  } catch (error) {
    api.clearAuthToken();
    currentUser.value = null;
  } finally {
    authReady.value = true;
  }
});

watch([tab, isAdminUser, selectedUser], () => {
  if (!currentUser.value) {
    return;
  }
  if (tab.value === 'users' && !isAdminUser.value) {
    tab.value = 'rules';
    ruleView.value = 'list';
    return;
  }
  if (tab.value === 'user-detail' && !canAccessUserProfile(selectedUser.value)) {
    selectedUser.value = currentUser.value;
    mcpTokens.value = [];
    loadTokens().catch(() => null);
  }
});

async function login() {
  loginError.value = '';
  loginLoading.value = true;
  try {
    const result = await api.login(loginForm.value);
    api.setAuthToken(result.token);
    currentUser.value = result.user;
    loginForm.value.password = '';
    await loadAppData();
  } catch (error) {
    loginError.value = error.message || '登录失败';
  } finally {
    loginLoading.value = false;
  }
}

async function logout() {
  accountMenuOpen.value = false;
  await api.logout().catch(() => null);
  api.clearAuthToken();
  currentUser.value = null;
  channelSwitchOpen.value = false;
  selectedUser.value = null;
  mcpTokens.value = [];
  channels.value = [];
  selectedChannel.value = null;
  rules.value = [];
  testHistory.value = [];
  selectedTestHistoryId.value = null;
  keySettings.value = emptyKeySettings();
  debugRequest.value = emptyDebugRequest();
  debugTidInit.value = emptyDebugTidInit();
  debugFieldRows.value = defaultDebugFieldRows();
  debugResult.value = null;
  debugResultKind.value = '';
  debugHistory.value = [];
  debugConsoleEntries.value = [];
  debugCollections.value = [];
  activeDebugCollectionId.value = '';
  activeDebugRequestId.value = '';
  debugEnvironments.value = [];
  activeEnvironmentId.value = '';
  debugTargetInput.value = '';
  environmentSwitcherOpen.value = false;
  environmentEditorOpen.value = false;
  environmentEditor.value = emptyDebugEnvironment();
  debugToolModal.value = '';
  smartReplaceOpen.value = false;
  smartReplaceChanges.value = [];
  smartReplaceXmlDraft.value = '';
  wirePreview.value = null;
  parsedResponseXml.value = '';
  tab.value = 'rules';
  ruleView.value = 'list';
}

async function loadAppData() {
  if (isAdminUser.value) {
    await loadUsers();
  } else {
    users.value = currentUser.value ? [currentUser.value] : [];
  }
  channels.value = await api.listChannels();
  if (channels.value.length > 0) {
    const preferredChannel = channels.value.find((channel) => channel.id === currentUser.value?.lastChannelId)
      || channels.value[0];
    await selectChannel(preferredChannel, { persist: preferredChannel.id !== currentUser.value?.lastChannelId });
  }
}

async function selectChannel(channel, options = {}) {
  const keepCurrentTab = options.keepCurrentTab === true;
  const previousTab = tab.value;
  const previousRuleView = ruleView.value;
  selectedChannel.value = channel;
  if (options.persist !== false) {
    await saveLastChannelPreference(channel.id);
  }
  restartNotice.value = '';
  if (!keepCurrentTab) {
    tab.value = 'rules';
    ruleView.value = 'list';
  }
  await loadFieldCatalog();
  await loadRules();
  testHistory.value = [];
  selectedTestHistoryId.value = null;
  debugResult.value = null;
  debugResultKind.value = '';
  debugHistory.value = [];
  debugConsoleEntries.value = [];
  debugRequest.value = emptyDebugRequest();
  debugTidInit.value = emptyDebugTidInit();
  debugFieldRows.value = defaultDebugFieldRows();
  debugCollections.value = [];
  activeDebugCollectionId.value = '';
  activeDebugRequestId.value = '';
  debugTargetInput.value = '';
  debugToolModal.value = '';
  environmentSwitcherOpen.value = false;
  smartReplaceOpen.value = false;
  smartReplaceChanges.value = [];
  smartReplaceXmlDraft.value = '';

  if (!keepCurrentTab) {
    return;
  }

  if (previousTab === 'rules') {
    tab.value = 'rules';
    ruleView.value = 'list';
    return;
  }

  ruleView.value = previousRuleView;
  if (previousTab === 'channel-new') {
    editCurrentChannel();
    return;
  }
  if (previousTab === 'keys') {
    await openKeys();
    return;
  }
  if (previousTab === 'packager-preview') {
    await openPackagerPreview();
    return;
  }
  if (previousTab === 'debug-pos') {
    await openPosDebug();
    return;
  }
  if (previousTab === 'test-history' || previousTab === 'test-history-detail') {
    await openValidationHistory();
    return;
  }
  tab.value = previousTab;
}

async function selectChannelById(channelId) {
  const channel = channels.value.find((item) => item.id === channelId);
  if (channel) {
    await selectChannel(channel, { keepCurrentTab: true });
  }
}

function openChannelSwitcher() {
  channelSwitchOpen.value = true;
}

function closeChannelSwitcher() {
  channelSwitchOpen.value = false;
}

async function chooseChannel(channelId) {
  await selectChannelById(channelId);
  closeChannelSwitcher();
}

function createChannelFromSwitcher() {
  closeChannelSwitcher();
  newChannel();
}

function editChannelFromSwitcher() {
  closeChannelSwitcher();
  editCurrentChannel();
}

async function saveLastChannelPreference(channelId) {
  if (!currentUser.value || !channelId) {
    return;
  }
  if (currentUser.value.lastChannelId === channelId) {
    return;
  }
  try {
    currentUser.value = await api.saveLastChannel(channelId);
    const current = users.value.find((user) => user.id === currentUser.value.id);
    if (current) {
      current.lastChannelId = currentUser.value.lastChannelId;
    }
  } catch (error) {
    showToast(error.message || '保存上次使用渠道失败', 'warning');
  }
}

async function openPackagerPreview() {
  if (!selectedChannel.value) return;
  tab.value = 'packager-preview';
  ruleView.value = 'list';
  draftPackagerPreviewVisible.value = false;
  packagerPreviewLoading.value = true;
  packagerPreviewError.value = '';
  packagerPreview.value = null;
  try {
    packagerPreview.value = await api.previewPackager(selectedChannel.value.id);
    fieldCatalog.value = packagerPreview.value.fields || [];
  } catch (error) {
    packagerPreviewError.value = error.message || '读取 Packager 失败';
  } finally {
    packagerPreviewLoading.value = false;
  }
}

async function loadFieldCatalog() {
  if (!selectedChannel.value) {
    fieldCatalog.value = [];
    return;
  }
  try {
    const preview = await api.previewPackager(selectedChannel.value.id);
    fieldCatalog.value = preview.fields || [];
    ensureRuleConditionOperators(editingRule.value);
  } catch (error) {
    fieldCatalog.value = [];
  }
}

async function openDraftPackagerPreview() {
  normalizePackagerMode();
  draftPackagerPreviewVisible.value = true;
  packagerPreviewLoading.value = true;
  packagerPreviewError.value = '';
  packagerPreview.value = null;
  try {
    packagerPreview.value = await api.previewPackagerDraft(channelEditor.value);
  } catch (error) {
    packagerPreviewError.value = error.message || '读取 Packager 失败';
  } finally {
    packagerPreviewLoading.value = false;
  }
}

function openInfoCenter() {
  accountMenuOpen.value = false;
  infoCenterOpen.value = true;
}

function closeInfoCenter() {
  infoCenterOpen.value = false;
}

async function loadRules() {
  rules.value = await api.listRules(selectedChannel.value.id);
}

async function loadUsers() {
  if (!isAdminUser.value) {
    users.value = currentUser.value ? [currentUser.value] : [];
    if (selectedUser.value && selectedUser.value.id !== currentUser.value?.id) {
      selectedUser.value = null;
    }
    return;
  }
  users.value = await api.listUsers();
  const current = users.value.find((item) => item.id === currentUser.value?.id);
  if (current) {
    currentUser.value = current;
  }
  if (selectedUser.value) {
    selectedUser.value = users.value.find((item) => item.id === selectedUser.value.id) || null;
  }
}

async function selectUser(user) {
  if (!canAccessUserProfile(user)) {
    showToast('只能查看当前用户自己的个人信息。', 'warning');
    return false;
  }
  selectedUser.value = user;
  await loadTokens();
  return true;
}

async function openUserDetail(user) {
  const selected = await selectUser(user);
  if (!selected) {
    return;
  }
  tab.value = 'user-detail';
}

function leaveUserDetail() {
  if (isAdminUser.value) {
    openUsers();
    return;
  }
  openRules();
}

async function openCurrentUserProfile() {
  if (!currentUser.value) return;
  accountMenuOpen.value = false;
  const user = users.value.find((item) => item.id === currentUser.value.id) || currentUser.value;
  await openUserDetail(user);
}

function openCreateUserModal() {
  if (!isAdminUser.value) {
    showToast('只有 admin 可以创建用户。', 'warning');
    return;
  }
  newUser.value = { username: '' };
  createUserOpen.value = true;
}

function closeCreateUserModal() {
  createUserOpen.value = false;
}

async function createUser() {
  if (!isAdminUser.value) {
    showToast('只有 admin 可以创建用户。', 'warning');
    return;
  }
  if (!newUser.value.username) return;
  const username = newUser.value.username.trim();
  if (!username) return;
  if (isUsernameDuplicate.value) {
    showToast('用户名已存在，请换一个用户名。', 'warning');
    return;
  }
  try {
    const created = await api.createUser({ username });
    newUser.value = { username: '' };
    createUserOpen.value = false;
    await loadUsers();
    await openUserDetail(created);
    showToast('用户已创建。', 'success');
  } catch (error) {
    showToast(error.message || '创建用户失败', 'error');
  }
}

async function loadTokens() {
  if (!selectedUser.value || !canAccessUserProfile(selectedUser.value)) {
    mcpTokens.value = [];
    return;
  }
  mcpTokens.value = await api.listTokens(selectedUser.value.id);
}

function openCreateTokenModal() {
  if (!selectedUser.value || !canAccessUserProfile(selectedUser.value)) {
    showToast('只能为当前用户创建 MCP Token。', 'warning');
    return;
  }
  newTokenName.value = 'AI Agent';
  createTokenOpen.value = true;
}

function closeCreateTokenModal() {
  createTokenOpen.value = false;
}

async function createToken() {
  if (!selectedUser.value || !canAccessUserProfile(selectedUser.value)) {
    showToast('只能为当前用户创建 MCP Token。', 'warning');
    return;
  }
  if (tokenLimitReached.value) {
    showToast('每个用户最多只能创建 3 个有效 MCP Token。', 'warning');
    return;
  }
  try {
    const created = await api.createToken(selectedUser.value.id, newTokenName.value);
    newTokenName.value = 'AI Agent';
    createTokenOpen.value = false;
    await loadTokens();
    try {
      await copyTextToClipboard(created.token);
      showToast('Token 已创建并复制到剪贴板。', 'success');
    } catch (copyError) {
      showToast('Token 已创建；点击列表中的 Token 可再次复制。', 'warning');
    }
  } catch (error) {
    showToast(error.message || '创建 MCP Token 失败', 'error');
  }
}

async function revokeToken(token) {
  if (!selectedUser.value || !canAccessUserProfile(selectedUser.value)) {
    showToast('只能吊销当前用户自己的 MCP Token。', 'warning');
    return;
  }
  try {
    await api.revokeToken(selectedUser.value.id, token.id);
    await loadTokens();
    showToast('Token 已吊销。', 'success');
  } catch (error) {
    showToast(error.message || '吊销 Token 失败', 'error');
  }
}

async function copyMcpToken(token) {
  if (!token || token.revoked) return;
  try {
    await copyTextToClipboard(token.token);
    showToast('MCP Token 已复制到剪贴板。', 'success');
  } catch (error) {
    showToast('复制失败，请检查浏览器剪贴板权限。', 'error');
  }
}

async function copyChannelMockValue(label, value) {
  if (!value || value === '-') {
    showToast(`${label} 为空，无法复制。`, 'error');
    return;
  }
  try {
    await copyTextToClipboard(String(value));
    showToast(`${label} 已复制。`, 'success');
  } catch {
    showToast(`${label} 复制失败，请检查浏览器剪贴板权限。`, 'error');
  }
}

async function copyTextToClipboard(value) {
  if (!value) {
    throw new Error('empty clipboard value');
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const input = document.createElement('textarea');
  input.value = value;
  input.setAttribute('readonly', 'readonly');
  input.style.position = 'fixed';
  input.style.left = '-9999px';
  document.body.appendChild(input);
  input.select();
  const copied = document.execCommand('copy');
  document.body.removeChild(input);
  if (!copied) {
    throw new Error('clipboard copy failed');
  }
}

function showToast(message, type = 'info') {
  if (!message) return;
  const id = ++toastSequence;
  toasts.value = [...toasts.value, { id, message, type }].slice(-4);
  window.setTimeout(() => removeToast(id), type === 'error' ? 5200 : 3600);
}

function removeToast(id) {
  toasts.value = toasts.value.filter((toast) => toast.id !== id);
}

function defaultCapability() {
  return capabilityOptions[0];
}

function capabilityByValue(value) {
  const normalizedValue = capabilityAliases[value] || value;
  return capabilityOptions.find((capability) => capability.value === normalizedValue) || defaultCapability();
}

function capabilityLabel(value) {
  return capabilityByValue(value).label;
}

function capabilitySystemConditions(value) {
  const capability = capabilityByValue(value);
  return [
    { field: '0', operator: 'EQ', value: capability.requestMti },
    { field: '3', operator: 'EQ', value: capability.processCode }
  ];
}

function ensureSystemConditions(rule, capability, reset = false) {
  const defaults = capabilitySystemConditions(capability.value);
  const existing = Array.isArray(rule.systemConditions) ? rule.systemConditions : [];
  const nextConditions = defaults.map((defaultCondition) => {
    const current = reset
      ? null
      : existing.find((condition) => canonicalFieldKey(condition?.field) === defaultCondition.field);
    return {
      field: defaultCondition.field,
      operator: 'EQ',
      value: current?.value || defaultCondition.value
    };
  });
  const unchanged = existing.length === nextConditions.length && nextConditions.every((nextCondition) => {
    const current = existing.find((condition) => canonicalFieldKey(condition?.field) === nextCondition.field);
    return current?.operator === nextCondition.operator && current?.value === nextCondition.value;
  });
  if (reset || !unchanged) {
    rule.systemConditions = nextConditions;
  }
}

function systemConditionByField(field) {
  const capability = capabilityByValue(editingRule.value.capability);
  ensureSystemConditions(editingRule.value, capability);
  return editingRule.value.systemConditions.find((condition) => canonicalFieldKey(condition.field) === canonicalFieldKey(field));
}

function systemConditionInputValue(field) {
  return systemConditionByField(field)?.value || '';
}

function setSystemConditionValue(field, value) {
  const condition = systemConditionByField(field);
  if (condition) {
    condition.value = value;
  }
}

function ensureRuleCapability(rule) {
  const capability = capabilityByValue(rule.capability);
  rule.capability = capability.value;
  ensureSystemConditions(rule, capability);
  rule.conditions = rule.conditions || [];
  rule.action = rule.action || { type: 'RESPOND', delayMs: 0 };
  rule.response = rule.response || { mti: capability.responseMti, fields: {} };
  rule.response.fields = rule.response.fields || {};
  if (!rule.response.mti) {
    rule.response.mti = capability.responseMti;
  }
}

function setRuleCapability(value) {
  const capability = capabilityByValue(value);
  editingRule.value.capability = capability.value;
  ensureSystemConditions(editingRule.value, capability, true);
  editingRule.value.response = editingRule.value.response || { fields: {} };
  editingRule.value.response.mti = capability.responseMti;
}

function emptyRule() {
  const capability = defaultCapability();
  return {
    name: '新增规则',
    description: '',
    enabled: true,
    capability: capability.value,
    priority: 100,
    matchMode: 'ALL',
    systemConditions: capabilitySystemConditions(capability.value),
    conditions: [],
    action: { type: 'RESPOND', delayMs: 0 },
    response: {
      mti: capability.responseMti,
      fields: {
        '39': { type: 'FIXED', value: '00' }
      }
    }
  };
}

function emptyChannel() {
  return {
    id: '',
    channelCode: '',
    name: '',
    enabled: true,
    host: '0.0.0.0',
    port: null,
    framingType: 'BINARY_2',
    byteOrder: 'BIG_ENDIAN',
    lengthIncludes: 'PAYLOAD',
    headerEnabled: false,
    headerLength: 0,
    headerResponseMode: 'NONE',
    headerFixedValueHex: '',
    packagerType: 'CLASS',
    packagerConfigMode: 'CLASS_NAME',
    packagerLocation: '',
    packagerFileName: '',
    packagerContent: '',
    packagerClassName: '',
    noMatchResponseCode: '96',
    thirdPartyTestIp: '',
    thirdPartyTestPort: null,
    thirdPartyTlsEnabled: false,
    ctmk1: '',
    ctmk2: '',
    mockTlsEnabled: null,
    mockCtmk1: '',
    mockCtmk2: ''
  };
}

function emptyKeySettings() {
  return {
    channelId: '',
    tpkPlain: '',
    tskPlain: '',
    macField: '128',
    macAlgorithm: 'ANSI_X9_19',
    testTid: '',
    testPan: '',
    testPin: '',
    testDe14: '',
    testDe42: '',
    testDe18: '',
    testDe43: '',
    testDe49: ''
  };
}

function emptyDebugRequest() {
  return {
    capability: 'DEBIT',
    tid: '',
    sn: '',
    requestXml: '',
    pin: '',
    saveTidConfig: false
  };
}

function emptyDebugTidInit() {
  return {
    tid: '',
    sn: '',
    saveKey: false
  };
}

function emptyDebugEnvironment() {
  return {
    id: '',
    name: '',
    channelId: '',
    targetIp: '',
    targetPort: '',
    tmkPlain: '',
    tpkPlain: '',
    tskPlain: '',
    macField: '',
    macAlgorithm: 'ANSI_X9_19',
    pinAlgorithm: 'NONE',
    testTid: '',
    testSn: '',
    testPan: '',
    testDe14: '',
    testDe52: '',
    testDe42: '',
    testDe18: '',
    testDe43: '',
    testDe49: '',
    variables: [],
    createdAt: '',
    updatedAt: ''
  };
}

function defaultSmartReplaceSelection() {
  return {
    dynamicParameters: true,
    environmentVariables: true,
    stan: true,
    timestamps: true,
    rrn: true
  };
}

function defaultDebugFieldRows() {
  return [];
}

function newRule() {
  editingRule.value = emptyRule();
  editingRule.value.ownerUserId = currentUser.value?.id || 1;
  editingRule.value.ownerUsername = currentUser.value?.username || '';
  hydrateResponseRows();
  ruleSection.value = 'match';
  tab.value = 'rules';
  ruleView.value = 'detail';
}

function editRule(rule) {
  editingRule.value = JSON.parse(JSON.stringify(rule));
  ensureRuleCapability(editingRule.value);
  ensureRuleConditionOperators(editingRule.value);
  hydrateResponseRows();
  ruleSection.value = 'match';
  tab.value = 'rules';
  ruleView.value = 'detail';
}

function duplicateRule(rule) {
  const clone = JSON.parse(JSON.stringify(rule));
  delete clone.id;
  delete clone.createdAt;
  delete clone.updatedAt;
  clone.name = `${clone.name} 副本`;
  clone.ownerUserId = currentUser.value?.id || clone.ownerUserId || 1;
  clone.ownerUsername = currentUser.value?.username || clone.ownerUsername || '';
  clone.publicRule = false;
  ensureRuleCapability(clone);
  ensureRuleConditionOperators(clone);
  editingRule.value = clone;
  hydrateResponseRows();
  ruleSection.value = 'match';
  tab.value = 'rules';
  ruleView.value = 'detail';
}

async function saveRule() {
  normalizeEditingRuleFields();
  if (!editingRule.value.id || !editingRule.value.ownerUserId) {
    editingRule.value.ownerUserId = currentUser.value?.id || 1;
    editingRule.value.ownerUsername = currentUser.value?.username || '';
  }
  const isUpdate = Boolean(editingRule.value.id);
  try {
    if (isUpdate) {
      await api.updateRule(selectedChannel.value.id, editingRule.value);
    } else {
      await api.createRule(selectedChannel.value.id, editingRule.value);
    }
    await loadRules();
    ruleView.value = 'list';
    showToast(isUpdate ? '规则已保存。' : '规则已创建。', 'success');
  } catch (error) {
    showToast(error.message || '保存规则失败', 'error');
  }
}

function normalizeEditingRuleFields() {
  ensureRuleCapability(editingRule.value);
  for (const condition of editingRule.value.conditions || []) {
    normalizeConditionField(condition);
  }
  for (const row of responseRows.value) {
    row.key = canonicalFieldKey(row.key);
  }
  syncResponseFields();
}

function openRules() {
  tab.value = 'rules';
  ruleView.value = 'list';
}

function openChannelModule() {
  openChannelSettings();
}

function openMockModule() {
  openRules();
}

function openDebugModule() {
  openPosDebug();
}

function openSystemModule() {
  if (isAdminUser.value) {
    openUsers();
    return;
  }
  openCurrentUserProfile();
}

function openChannelSettings() {
  if (selectedChannel.value) {
    editCurrentChannel();
    return;
  }
  newChannel();
}

function backToRuleList() {
  ruleView.value = 'list';
}

function openTest() {
  tab.value = 'test';
}

async function openPosDebug() {
  tab.value = 'debug-pos';
  await loadKeySettings();
  await loadDebugEnvironments();
  hydrateDebugDefaults();
  await loadDebugCollections();
  await loadDebugHistory();
}

async function openValidationHistory() {
  tab.value = 'test-history';
  await loadTestHistory();
}

function backToSingleValidation() {
  openTest();
}

function openTestHistoryDetail(history) {
  selectedTestHistoryId.value = history.id;
  tab.value = 'test-history-detail';
}

async function openPublicRules() {
  tab.value = 'public-rules';
  await loadPublicRules();
}

async function openKeys() {
  tab.value = 'keys';
  await loadKeySettings();
}

async function openUsers() {
  if (!isAdminUser.value) {
    showToast('只有 admin 可以访问用户管理。', 'warning');
    return;
  }
  tab.value = 'users';
  await loadUsers();
}

async function loadKeySettings() {
  if (!selectedChannel.value) return;
  keyLoading.value = true;
  try {
    keySettings.value = { ...emptyKeySettings(), ...(await api.getKeySettings(selectedChannel.value.id)) };
  } catch (error) {
    showToast(error.message || '读取 Key 设置失败', 'error');
  } finally {
    keyLoading.value = false;
  }
}

async function saveKeySettings() {
  if (!selectedChannel.value) return;
  keyLoading.value = true;
  try {
    keySettings.value = await api.saveKeySettings(selectedChannel.value.id, keySettings.value);
    showToast('Key 设置已保存。', 'success');
  } catch (error) {
    showToast(error.message || '保存 Key 设置失败', 'error');
  } finally {
    keyLoading.value = false;
  }
}

function hydrateDebugDefaults() {
  const settings = activeEnvironment.value || keySettings.value || {};
  if (!debugRequest.value.tid && settings.testTid) {
    debugRequest.value.tid = settings.testTid;
  }
  if (!debugRequest.value.sn && settings.testSn) {
    debugRequest.value.sn = settings.testSn;
  }
  if (!debugTidInit.value.tid && settings.testTid) {
    debugTidInit.value.tid = settings.testTid;
  }
  if (!debugTidInit.value.sn && settings.testSn) {
    debugTidInit.value.sn = settings.testSn;
  }
}

function activeEnvironmentStorageKey() {
  return `fakerActiveDebugEnvironment:${currentUser.value?.id || 'anonymous'}`;
}

async function loadDebugEnvironments() {
  if (!currentUser.value) return;
  try {
    debugEnvironments.value = (await api.listDebugEnvironments(currentUser.value.id))
      .map(normalizeDebugEnvironment)
      .filter((environment) => environment.id);
    localStorage.removeItem(`fakerDebugEnvironments:${currentUser.value.id}`);
  } catch (error) {
    showToast(error.message || '读取 Environment 失败', 'error');
    debugEnvironments.value = [];
  }
  if (debugEnvironments.value.length === 0) {
    try {
      const created = await api.createDebugEnvironment(currentUser.value.id, defaultDebugEnvironment());
      debugEnvironments.value = [normalizeDebugEnvironment(created)];
    } catch (error) {
      showToast(error.message || '创建默认 Environment 失败', 'error');
      return;
    }
  }
  const storedActiveId = localStorage.getItem(activeEnvironmentStorageKey()) || '';
  activeEnvironmentId.value = debugEnvironments.value.some((environment) => environment.id === activeEnvironmentId.value)
    ? activeEnvironmentId.value
    : (debugEnvironments.value.some((environment) => environment.id === storedActiveId)
      ? storedActiveId
      : debugEnvironments.value[0].id);
  applyActiveEnvironmentDefaults();
  syncDebugTargetInput(true);
}

function persistDebugEnvironments() {
  if (activeEnvironmentId.value) {
    localStorage.setItem(activeEnvironmentStorageKey(), activeEnvironmentId.value);
  }
}

function defaultDebugEnvironment() {
  const now = new Date().toISOString();
  const channel = selectedChannel.value || channels.value[0] || {};
  const settings = keySettings.value || {};
  return normalizeDebugEnvironment({
    id: uniqueId('env'),
    name: `${channelDisplayCode(channel)} Test`,
    channelId: channel.id || '',
    targetIp: channel.thirdPartyTestIp || channel.config?.thirdPartyTestIp || '',
    targetPort: channel.thirdPartyTestPort ?? channel.config?.thirdPartyTestPort ?? '',
    tmkPlain: '',
    tpkPlain: settings.tpkPlain || '',
    tskPlain: settings.tskPlain || '',
    macField: settings.macField || '',
    macAlgorithm: settings.macAlgorithm || 'ANSI_X9_19',
    pinAlgorithm: 'NONE',
    testTid: settings.testTid || '',
    testSn: '',
    testPan: settings.testPan || '',
    testDe14: settings.testDe14 || '',
    testDe52: settings.testPin || '',
    testDe42: settings.testDe42 || '',
    testDe18: settings.testDe18 || '',
    testDe43: settings.testDe43 || '',
    testDe49: settings.testDe49 || '',
    variables: [],
    createdAt: now,
    updatedAt: now
  });
}

function normalizeDebugEnvironment(environment) {
  const base = { ...emptyDebugEnvironment(), ...(environment || {}) };
  const now = new Date().toISOString();
  base.id = base.id || uniqueId('env');
  base.name = String(base.name || 'Untitled Environment').trim();
  base.channelId = base.channelId || selectedChannel.value?.id || channels.value[0]?.id || '';
  base.targetIp = String(base.targetIp || '').trim();
  base.targetPort = normalizePort(base.targetPort) || '';
  base.tmkPlain = String(base.tmkPlain || '').trim();
  base.tpkPlain = String(base.tpkPlain || '').trim();
  base.tskPlain = String(base.tskPlain || '').trim();
  base.macField = String(base.macField || '').trim();
  base.macAlgorithm = base.macAlgorithm || 'ANSI_X9_19';
  base.pinAlgorithm = base.pinAlgorithm || 'NONE';
  base.testTid = String(base.testTid || '').trim();
  base.testSn = String(base.testSn || '').trim();
  base.testPan = String(base.testPan || '').trim();
  base.testDe14 = String(base.testDe14 || '').trim();
  base.testDe52 = String(base.testDe52 || base.testPin || '').trim();
  base.testDe42 = String(base.testDe42 || '').trim();
  base.testDe18 = String(base.testDe18 || '').trim();
  base.testDe43 = String(base.testDe43 || '').trim();
  base.testDe49 = String(base.testDe49 || '').trim();
  base.variables = normalizeEnvironmentVariables(base.variables);
  base.createdAt = base.createdAt || now;
  base.updatedAt = base.updatedAt || now;
  return base;
}

function normalizeEnvironmentVariables(variables) {
  if (!Array.isArray(variables)) return [];
  return variables
    .map((variable) => ({
      key: String(variable?.key || '').trim(),
      value: String(variable?.value || '').trim(),
      description: String(variable?.description || '').trim(),
      sensitive: Boolean(variable?.sensitive)
    }))
    .filter((variable) => variable.key);
}

function environmentVariableMap(environment) {
  return normalizeEnvironmentVariables(environment?.variables).reduce((result, variable) => {
    result[variable.key] = variable.value;
    return result;
  }, {});
}

const standardEnvironmentVariableSpecs = [
  { key: 'DE41', field: 'testTid', description: 'TID / terminal id' },
  { key: 'SN', field: 'testSn', description: 'DE62 Tag01 / terminal serial number' },
  { key: 'DE2', field: 'testPan', description: 'PAN / test card number' },
  { key: 'DE14', field: 'testDe14', description: 'Card expiry, YYMM' },
  { key: 'DE52', field: 'testDe52', description: 'PIN Block', sensitive: true }
];

function hydrateEnvironmentVariableEditorRows(environment) {
  const normalized = normalizeDebugEnvironment(environment);
  const rows = standardEnvironmentVariableSpecs.map((spec) => ({
    id: uniqueId('var'),
    key: spec.key,
    value: normalized[spec.field] || '',
    description: spec.description,
    sensitive: Boolean(spec.sensitive)
  }));
  rows.push(...normalizeEnvironmentVariables(normalized.variables).map((variable) => ({
    id: uniqueId('var'),
    key: variable.key,
    value: variable.value,
    description: variable.description || '',
    sensitive: Boolean(variable.sensitive)
  })));
  environmentVariableEditorRows.value = rows;
  environmentVariableSearch.value = '';
}

function applyEnvironmentVariableEditorRows(environment) {
  const next = { ...environment };
  const customVariables = [];
  for (const spec of standardEnvironmentVariableSpecs) {
    next[spec.field] = '';
  }
  for (const variable of normalizeEnvironmentVariables(environmentVariableEditorRows.value)) {
    const standardSpec = standardEnvironmentVariableSpecForKey(variable.key);
    if (standardSpec) {
      next[standardSpec.field] = variable.value || '';
    } else {
      customVariables.push(variable);
    }
  }
  next.variables = customVariables;
  return next;
}

function standardEnvironmentVariableSpecForKey(key) {
  const normalizedKey = String(key || '').trim().toLowerCase();
  if (!normalizedKey) return null;
  const aliasMap = {
    de41: 'DE41',
    tid: 'DE41',
    testtid: 'DE41',
    sn: 'SN',
    testsn: 'SN',
    de2: 'DE2',
    pan: 'DE2',
    testpan: 'DE2',
    de14: 'DE14',
    cardexpiry: 'DE14',
    testde14: 'DE14',
    de52: 'DE52',
    pinblock: 'DE52',
    testde52: 'DE52'
  };
  const canonicalKey = aliasMap[normalizedKey] || key;
  return standardEnvironmentVariableSpecs.find((spec) => spec.key.toLowerCase() === String(canonicalKey).toLowerCase()) || null;
}

function buildDebugTargetVariableMap(environment) {
  const source = environment || {};
  const map = {};
  const add = (key, value, override = false) => {
    const name = String(key || '').trim();
    if (!name || value === null || value === undefined) return;
    const text = String(value).trim();
    if (!text) return;
    if (!override && Object.prototype.hasOwnProperty.call(map, name)) return;
    map[name] = text;
  };
  const targetPort = normalizePort(source.targetPort);
  const targetAddress = source.targetIp && targetPort ? `${source.targetIp}:${targetPort}` : '';

  add('target', targetAddress);
  add('target.ip', source.targetIp);
  add('target.port', targetPort);
  add('host', source.targetIp);
  add('ip', source.targetIp);
  add('port', targetPort);
  add('env.name', source.name || activeEnvironmentName.value);
  add('protocol.channel', debugChannelCode.value);
  add('channel.packager', debugShortPackager.value);

  add('DE2', source.testPan);
  add('PAN', source.testPan);
  add('DE14', source.testDe14);
  add('cardExpiry', source.testDe14);
  add('DE18', source.testDe18);
  add('MCC', source.testDe18);
  add('DE41', source.testTid);
  add('TID', source.testTid);
  add('testTid', source.testTid);
  add('SN', source.testSn);
  add('testSn', source.testSn);
  add('DE42', source.testDe42);
  add('merchantId', source.testDe42);
  add('DE43', source.testDe43);
  add('merchantName', source.testDe43);
  add('DE49', source.testDe49);
  add('currency', source.testDe49);

  for (const variable of normalizeEnvironmentVariables(source.variables)) {
    add(variable.key, variable.value);
  }
  return map;
}

function resolveDebugTargetVariable(name) {
  const key = String(name || '').trim();
  if (!key) return null;
  const map = debugTargetVariableMap.value;
  if (Object.prototype.hasOwnProperty.call(map, key)) {
    return { key, value: map[key] };
  }
  const lowerKey = key.toLowerCase();
  const matchingKey = Object.keys(map).find((item) => item.toLowerCase() === lowerKey);
  return matchingKey ? { key: matchingKey, value: map[matchingKey] } : null;
}

function resolveDebugTargetVariables(value) {
  return String(value || '').trim().replace(/\{\{\s*([^{}]+?)\s*\}\}/g, (token, name) => {
    const resolved = resolveDebugTargetVariable(name);
    return resolved?.value || token;
  });
}

function parseDebugTargetAddress(value) {
  const target = String(value || '').trim().replace(/^tcp:\/\//i, '');
  const separatorIndex = target.lastIndexOf(':');
  if (separatorIndex <= 0 || separatorIndex === target.length - 1) {
    return { ip: '', port: null };
  }
  const ip = target.slice(0, separatorIndex).trim();
  const port = normalizePort(target.slice(separatorIndex + 1).trim());
  return { ip, port };
}

function syncDebugTargetInput(force = false) {
  if (!force && debugTargetInput.value.trim()) return;
  debugTargetInput.value = '{{target.ip}}:{{target.port}}';
}

function normalizeDebugTargetInput() {
  debugTargetInput.value = debugTargetInput.value.trim();
  syncDebugTargetInput(false);
}

function normalizePort(value) {
  if (value === null || value === undefined || value === '') return null;
  const port = Number(value);
  return Number.isInteger(port) && port > 0 && port <= 65535 ? port : null;
}

function uniqueId(prefix) {
  const random = Math.random().toString(36).slice(2, 8);
  return `${prefix}-${Date.now().toString(36)}-${random}`;
}

function createDebugEnvironment() {
  const source = activeEnvironment.value || defaultDebugEnvironment();
  environmentEditorMode.value = 'create';
  environmentEditor.value = {
    ...normalizeDebugEnvironment(source),
    id: '',
    name: `${source.name || 'Environment'} Copy`,
    createdAt: '',
    updatedAt: ''
  };
  hydrateEnvironmentVariableEditorRows(environmentEditor.value);
  environmentEditorOpen.value = true;
}

function editActiveDebugEnvironment() {
  if (!activeEnvironment.value) {
    createDebugEnvironment();
    return;
  }
  environmentEditorMode.value = 'edit';
  environmentEditor.value = { ...normalizeDebugEnvironment(activeEnvironment.value) };
  hydrateEnvironmentVariableEditorRows(environmentEditor.value);
  environmentEditorOpen.value = true;
}

function pullChannelTestEnvironmentIntoEditor() {
  const channel = channels.value.find((item) => item.id === environmentEditor.value.channelId) || debugProtocolChannel.value;
  if (!channel) {
    showToast('没有找到当前 Environment 绑定的协议渠道。', 'warning');
    return;
  }
  const config = channel.config || {};
  const targetIp = channel.thirdPartyTestIp || config.thirdPartyTestIp || '';
  const targetPort = channel.thirdPartyTestPort ?? config.thirdPartyTestPort ?? '';
  if (!targetIp && !targetPort) {
    showToast('当前渠道没有第三方测试环境配置。', 'warning');
    return;
  }
  environmentEditor.value.targetIp = targetIp;
  environmentEditor.value.targetPort = targetPort || '';
  showToast(`已拉取 ${channelDisplayCode(channel)} 的测试环境配置。`, 'success');
}

function closeEnvironmentEditor() {
  environmentEditorOpen.value = false;
  environmentEditor.value = emptyDebugEnvironment();
  environmentVariableEditorRows.value = [];
  environmentVariableSearch.value = '';
}

function addEnvironmentVariableRow() {
  environmentVariableEditorRows.value.push({
    id: uniqueId('var'),
    key: '',
    value: '',
    description: '',
    sensitive: false
  });
}

function removeEnvironmentVariableRow(rowId) {
  environmentVariableEditorRows.value = environmentVariableEditorRows.value.filter((row) => row.id !== rowId);
}

async function saveDebugEnvironment() {
  const normalized = normalizeDebugEnvironment({
    ...applyEnvironmentVariableEditorRows(environmentEditor.value),
    id: environmentEditorMode.value === 'create' ? '' : environmentEditor.value.id,
    updatedAt: new Date().toISOString()
  });
  if (!normalized.name) {
    showToast('环境名称不能为空。', 'warning');
    return;
  }
  if (!normalized.channelId) {
    showToast('Environment 必须选择协议渠道。', 'warning');
    return;
  }
  if (environmentEditor.value.targetPort && !normalizePort(environmentEditor.value.targetPort)) {
    showToast('目标 Port 必须在 1 到 65535 之间。', 'warning');
    return;
  }
  try {
    const saved = environmentEditorMode.value === 'create'
      ? await api.createDebugEnvironment(currentUser.value.id, normalized)
      : await api.updateDebugEnvironment(currentUser.value.id, normalized);
    const next = normalizeDebugEnvironment(saved);
    const index = debugEnvironments.value.findIndex((environment) => environment.id === next.id);
    if (index >= 0) {
      debugEnvironments.value.splice(index, 1, next);
    } else {
      debugEnvironments.value.push(next);
    }
    activeEnvironmentId.value = next.id;
  } catch (error) {
    showToast(error.message || '保存 Environment 失败', 'error');
    return;
  }
  persistDebugEnvironments();
  closeEnvironmentEditor();
  applyActiveEnvironmentDefaults();
  syncDebugTargetInput(true);
  await loadDebugCollections();
  await loadDebugHistory();
  showToast('Environment 已保存。', 'success');
}

async function deleteDebugEnvironment() {
  if (!activeEnvironment.value || debugEnvironments.value.length <= 1) return;
  const id = activeEnvironment.value.id;
  try {
    await api.deleteDebugEnvironment(currentUser.value.id, id);
  } catch (error) {
    showToast(error.message || '删除 Environment 失败', 'error');
    return;
  }
  debugEnvironments.value = debugEnvironments.value.filter((environment) => environment.id !== id);
  activeEnvironmentId.value = debugEnvironments.value[0]?.id || '';
  persistDebugEnvironments();
  closeEnvironmentEditor();
  applyActiveEnvironmentDefaults();
  syncDebugTargetInput(true);
  await loadDebugCollections();
  await loadDebugHistory();
  showToast('Environment 已删除。', 'success');
}

async function selectDebugEnvironment(environmentId) {
  if (!debugEnvironments.value.some((environment) => environment.id === environmentId)) return;
  activeEnvironmentId.value = environmentId;
  persistDebugEnvironments();
  debugResult.value = null;
  debugResultKind.value = '';
  applyActiveEnvironmentDefaults();
  syncDebugTargetInput(true);
  await loadDebugCollections();
  await loadDebugHistory();
}

function applyActiveEnvironmentDefaults() {
  const environment = activeEnvironment.value;
  if (!environment) return;
  debugRequest.value.tid = environment.testTid || '';
  debugRequest.value.sn = environment.testSn || '';
  debugTidInit.value.tid = environment.testTid || '';
  debugTidInit.value.sn = environment.testSn || '';
  hydrateDebugDefaults();
}

function upsertDebugField(key, value) {
  if (!value) return;
  const canonical = canonicalFieldKey(key);
  const existing = debugFieldRows.value.find((row) => canonicalFieldKey(row.key) === canonical);
  if (existing) {
    existing.value = value;
    return;
  }
  debugFieldRows.value.push({ key: canonical, value });
}

function debugFieldsMap() {
  const fields = {};
  for (const row of debugFieldRows.value) {
    const key = canonicalFieldKey(row.key);
    if (key && row.value !== null && row.value !== undefined) {
      fields[key] = String(row.value);
    }
  }
  return fields;
}

function debugEnvironmentFieldsMap() {
  const environment = activeEnvironment.value || {};
  const fields = {
    '2': environment.testPan || '',
    '14': environment.testDe14 || '',
    '41': debugEnvironmentTid.value || '',
    SN: debugEnvironmentSn.value || '',
    '52': environment.testDe52 || ''
  };
  for (const variable of normalizeEnvironmentVariables(environment.variables)) {
    const key = canonicalFieldKey(variable.key);
    if (key && key !== '0' && /^\d+$/.test(key)) {
      fields[key] = variable.value || '';
    }
  }
  return fields;
}

function debugDynamicFieldsMap() {
  const environment = activeEnvironment.value || {};
  return {
    '18': environment.testDe18 || '',
    '42': environment.testDe42 || '',
    '43': environment.testDe43 || '',
    '49': environment.testDe49 || ''
  };
}

function addDebugFieldRow() {
  debugFieldRows.value.push({ key: '', value: '' });
}

function removeDebugFieldRow(index) {
  debugFieldRows.value.splice(index, 1);
}

function openDebugToolModal(tool) {
  debugToolModal.value = tool;
}

function closeDebugToolModal() {
  debugToolModal.value = '';
}

function openEnvironmentSwitcher() {
  environmentSwitcherOpen.value = true;
}

function closeEnvironmentSwitcher() {
  environmentSwitcherOpen.value = false;
}

async function chooseDebugEnvironment(environmentId) {
  await selectDebugEnvironment(environmentId);
  closeEnvironmentSwitcher();
}

function toggleDebugPanel(panel) {
  collapsedDebugPanels.value = {
    ...collapsedDebugPanels.value,
    [panel]: !collapsedDebugPanels.value[panel]
  };
}

async function loadDebugCollections() {
  if (!currentUser.value || !activeEnvironmentId.value) {
    debugCollections.value = [];
    return;
  }
  try {
    const collections = await api.listDebugCollections(currentUser.value.id, activeEnvironmentId.value);
    debugCollections.value = Array.isArray(collections) && collections.length
      ? collections.map(normalizeDebugCollection)
      : [];
    localStorage.removeItem(`fakerDebugCollections:${currentUser.value.id}:${activeEnvironmentId.value}`);
  } catch (error) {
    debugCollections.value = [];
    showToast(error.message || '读取 Collection 失败', 'error');
  }
  if (debugCollections.value.length === 0) {
    try {
      const created = await api.createDebugCollection(currentUser.value.id, defaultDebugCollections()[0]);
      debugCollections.value = [normalizeDebugCollection(created)];
    } catch (error) {
      showToast(error.message || '创建默认 Collection 失败', 'error');
      return;
    }
  }
  const firstCollection = debugCollections.value[0];
  activeDebugCollectionId.value = activeDebugCollectionId.value && debugCollections.value.some((item) => item.id === activeDebugCollectionId.value)
    ? activeDebugCollectionId.value
    : (firstCollection?.id || '');
  const activeCollection = debugCollections.value.find((item) => item.id === activeDebugCollectionId.value);
  activeDebugRequestId.value = activeDebugRequestId.value && activeCollection?.requests.some((item) => item.id === activeDebugRequestId.value)
    ? activeDebugRequestId.value
    : (activeCollection?.requests[0]?.id || '');
}

function defaultDebugCollections() {
  return [{
    id: `col-${Date.now()}`,
    environmentId: activeEnvironmentId.value,
    name: `${activeEnvironmentName.value} Collection`,
    requests: [{
      id: `req-${Date.now()}`,
      name: 'Current Request',
      capability: debugRequest.value.capability,
      requestXml: debugRequest.value.requestXml,
      pin: debugRequest.value.pin,
      saveTidConfig: debugRequest.value.saveTidConfig
    }]
  }];
}

function normalizeDebugCollection(collection) {
  return {
    id: collection?.id || uniqueId('col'),
    environmentId: collection?.environmentId || activeEnvironmentId.value,
    name: String(collection?.name || 'Untitled Collection').trim(),
    requests: Array.isArray(collection?.requests) ? collection.requests.map(normalizeDebugCollectionRequest) : []
  };
}

function normalizeDebugCollectionRequest(request) {
  return {
    id: request?.id || uniqueId('req'),
    name: String(request?.name || 'Untitled Request').trim(),
    capability: request?.capability || 'DEBIT',
    requestXml: request?.requestXml || '',
    pin: request?.pin || '',
    saveTidConfig: Boolean(request?.saveTidConfig)
  };
}

async function persistDebugCollection(collection) {
  if (!currentUser.value || !collection) return null;
  const normalized = normalizeDebugCollection({ ...collection, environmentId: activeEnvironmentId.value });
  const saved = await api.updateDebugCollection(currentUser.value.id, normalized);
  const index = debugCollections.value.findIndex((item) => item.id === saved.id);
  if (index >= 0) {
    debugCollections.value.splice(index, 1, normalizeDebugCollection(saved));
  }
  return saved;
}

async function createDebugCollection() {
  const collection = {
    id: `col-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    environmentId: activeEnvironmentId.value,
    name: `Collection ${debugCollections.value.length + 1}`,
    requests: []
  };
  try {
    const saved = normalizeDebugCollection(await api.createDebugCollection(currentUser.value.id, collection));
    debugCollections.value.push(saved);
    activeDebugCollectionId.value = saved.id;
    activeDebugRequestId.value = '';
  } catch (error) {
    showToast(error.message || '创建 Collection 失败', 'error');
  }
}

function startEditingDebugCollection(collection) {
  editingDebugCollectionId.value = collection.id;
}

async function commitDebugCollectionName(collection) {
  collection.name = String(collection.name || '').trim() || 'Untitled Collection';
  editingDebugCollectionId.value = '';
  try {
    await persistDebugCollection(collection);
  } catch (error) {
    showToast(error.message || '保存 Collection 失败', 'error');
  }
}

async function deleteDebugCollection(collection) {
  if (!collection) return;
  if (!window.confirm(`删除 Collection "${collection.name}"？`)) return;
  try {
    await api.deleteDebugCollection(currentUser.value.id, collection.id);
    debugCollections.value = debugCollections.value.filter((item) => item.id !== collection.id);
    if (activeDebugCollectionId.value === collection.id) {
      const nextCollection = debugCollections.value[0];
      activeDebugCollectionId.value = nextCollection?.id || '';
      activeDebugRequestId.value = nextCollection?.requests[0]?.id || '';
      if (nextCollection?.requests[0]) {
        loadDebugRequest(nextCollection.id, nextCollection.requests[0].id);
      } else {
        debugRequest.value = emptyDebugRequest();
        debugResult.value = null;
        debugResultKind.value = '';
      }
    }
  } catch (error) {
    showToast(error.message || '删除 Collection 失败', 'error');
  } finally {
    editingDebugCollectionId.value = '';
  }
}

function selectDebugCollection(collectionId) {
  activeDebugCollectionId.value = collectionId;
  const collection = debugCollections.value.find((item) => item.id === collectionId);
  activeDebugRequestId.value = collection?.requests[0]?.id || '';
  if (collection?.requests[0]) {
    loadDebugRequest(collection.id, collection.requests[0].id);
  } else {
    debugRequest.value = emptyDebugRequest();
    debugResult.value = null;
    debugResultKind.value = '';
  }
}

async function newDebugRequestInCollection(collectionId = activeDebugCollectionId.value) {
  const collection = debugCollections.value.find((item) => item.id === collectionId) || await ensureDebugCollection();
  const request = debugRequestSnapshot(`Request ${collection.requests.length + 1}`);
  request.requestXml = '';
  collection.requests.push(request);
  activeDebugCollectionId.value = collection.id;
  activeDebugRequestId.value = request.id;
  debugRequest.value = { ...emptyDebugRequest(), capability: request.capability };
  debugResult.value = null;
  debugResultKind.value = '';
  try {
    await persistDebugCollection(collection);
  } catch (error) {
    showToast(error.message || '保存 Request 失败', 'error');
  }
}

function startEditingDebugRequest(request) {
  editingDebugRequestId.value = request.id;
}

async function commitDebugRequestName(request) {
  request.name = String(request.name || '').trim() || 'Untitled Request';
  editingDebugRequestId.value = '';
  const collection = debugCollections.value.find((item) => item.requests.some((candidate) => candidate.id === request.id));
  try {
    await persistDebugCollection(collection);
  } catch (error) {
    showToast(error.message || '保存 Request 失败', 'error');
  }
}

async function deleteDebugRequest(collection, request) {
  if (!collection || !request) return;
  if (!window.confirm(`删除 Request "${request.name}"？`)) return;
  collection.requests = collection.requests.filter((item) => item.id !== request.id);
  if (activeDebugRequestId.value === request.id) {
    const nextRequest = collection.requests[0];
    activeDebugRequestId.value = nextRequest?.id || '';
    if (nextRequest) {
      loadDebugRequest(collection.id, nextRequest.id);
    } else {
      debugRequest.value = emptyDebugRequest();
      debugResult.value = null;
      debugResultKind.value = '';
    }
  }
  editingDebugCollectionId.value = '';
  editingDebugRequestId.value = '';
  try {
    await persistDebugCollection(collection);
  } catch (error) {
    showToast(error.message || '删除 Request 失败', 'error');
  }
}

async function saveCurrentDebugRequestToCollection() {
  const collection = activeDebugCollection.value || await ensureDebugCollection();
  const existing = collection.requests.find((item) => item.id === activeDebugRequestId.value);
  const snapshot = debugRequestSnapshot(existing?.name || capabilityLabel(debugRequest.value.capability));
  if (existing) {
    Object.assign(existing, snapshot, { id: existing.id, name: existing.name });
  } else {
    collection.requests.push(snapshot);
    activeDebugRequestId.value = snapshot.id;
  }
  activeDebugCollectionId.value = collection.id;
  try {
    await persistDebugCollection(collection);
    showToast('请求已保存到 Collection。', 'success');
  } catch (error) {
    showToast(error.message || '保存 Request 失败', 'error');
  }
}

async function ensureDebugCollection() {
  if (activeDebugCollection.value) return activeDebugCollection.value;
  if (debugCollections.value.length === 0) {
    const created = await api.createDebugCollection(currentUser.value.id, defaultDebugCollections()[0]);
    debugCollections.value = [normalizeDebugCollection(created)];
  }
  activeDebugCollectionId.value = debugCollections.value[0]?.id || '';
  return activeDebugCollection.value;
}

function debugRequestSnapshot(name) {
  return {
    id: `req-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: name || capabilityLabel(debugRequest.value.capability),
    capability: debugRequest.value.capability,
    requestXml: debugRequest.value.requestXml,
    pin: debugRequest.value.pin,
    saveTidConfig: debugRequest.value.saveTidConfig
  };
}

function loadDebugRequest(collectionId, requestId) {
  const collection = debugCollections.value.find((item) => item.id === collectionId);
  const request = collection?.requests.find((item) => item.id === requestId);
  if (!request) return;
  activeDebugCollectionId.value = collectionId;
  activeDebugRequestId.value = requestId;
  debugRequest.value = {
    ...emptyDebugRequest(),
    capability: request.capability || 'DEBIT',
    requestXml: request.requestXml || '',
    pin: request.pin || '',
    saveTidConfig: Boolean(request.saveTidConfig)
  };
  debugResult.value = null;
  debugResultKind.value = '';
  debugMessageView.value = 'xml';
  tab.value = 'debug-pos';
}

async function loadDebugHistory() {
  if (!currentUser.value || !activeEnvironmentId.value) {
    debugHistory.value = [];
    return;
  }
  try {
    debugHistory.value = await api.listDebugHistory(currentUser.value.id, activeEnvironmentId.value);
    localStorage.removeItem(`fakeTcpDebugHistory:${currentUser.value.id}:${activeEnvironmentId.value}`);
  } catch (error) {
    debugHistory.value = [];
    showToast(error.message || '读取请求历史失败', 'error');
  }
}

async function saveDebugHistory(kind) {
  if (!debugChannelId.value || !debugResult.value) return;
  const firstStep = debugSteps.value[0] || {};
  const item = {
    environmentId: activeEnvironmentId.value,
    kind,
    title: kind === 'tid' ? 'TID 初始化' : capabilityLabel(debugRequest.value.capability),
    capability: debugRequest.value.capability,
    success: Boolean(debugResult.value.success),
    responseCode: debugSteps.value.map((step) => step.responseCode).filter(Boolean).join('/') || '',
    requestXml: kind === 'tid' ? (firstStep.signedRequestXml || firstStep.requestXml || debugRequest.value.requestXml) : debugRequest.value.requestXml,
    result: debugResult.value
  };
  try {
    const saved = await api.createDebugHistory(currentUser.value.id, item);
    debugHistory.value = [saved, ...debugHistory.value].slice(0, 30);
  } catch (error) {
    appendDebugConsole(error.message || '保存请求历史失败', 'warn');
  }
}

function loadDebugHistoryItem(item) {
  if (!item) return;
  debugRequest.value.capability = item.capability || debugRequest.value.capability;
  debugRequest.value.requestXml = item.requestXml || '';
  debugResult.value = item.result || null;
  debugResultKind.value = item.kind || '';
  debugMessageView.value = 'xml';
  appendDebugConsole(`Loaded history: ${item.title || '-'} · ${item.responseCode || '-'}`);
  appendDebugResultConsole(debugResult.value);
  showToast('已载入请求历史。', 'success');
}

async function deleteDebugHistoryItem(item) {
  try {
    await api.deleteDebugHistory(currentUser.value.id, activeEnvironmentId.value, item.id);
    debugHistory.value = debugHistory.value.filter((history) => history.id !== item.id);
  } catch (error) {
    showToast(error.message || '删除请求历史失败', 'error');
  }
}

async function clearDebugHistory() {
  try {
    await api.clearDebugHistory(currentUser.value.id, activeEnvironmentId.value);
    debugHistory.value = [];
  } catch (error) {
    showToast(error.message || '清空请求历史失败', 'error');
  }
}

function appendDebugConsole(text, level = 'info') {
  const timestamp = new Date().toLocaleTimeString('zh-CN', { hour12: false });
  const prefix = level === 'error' ? 'ERROR' : level === 'warn' ? 'WARN ' : 'INFO ';
  debugConsoleEntries.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    text: `[${timestamp}] ${prefix} ${text}`
  });
  if (debugConsoleEntries.value.length > 300) {
    debugConsoleEntries.value = debugConsoleEntries.value.slice(-300);
  }
  scrollDebugConsoleToBottom();
}

function appendDebugConsoleBlock(title, body) {
  if (!body) return;
  debugConsoleEntries.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    text: `\n--- ${title} ---\n${body}`
  });
  scrollDebugConsoleToBottom();
}

async function appendDebugResultConsole(result = debugResult.value) {
  if (!result) return;
  appendDebugConsole(`Result ${result.success ? 'SUCCESS' : 'FAILED'} · ${result.targetIp || '-'}:${result.targetPort || '-'} · ${result.channelCode || debugChannelCode.value}`);
  await waitForConsoleFrame();
  for (const [index, step] of (result.steps || []).entries()) {
    appendDebugConsole(`Step ${index + 1} ${step.label || capabilityLabel(step.capability)} · ${step.success ? 'SUCCESS' : 'FAILED'} · ${step.durationMs ?? '-'}ms · DE39 ${step.responseCode || '-'}`);
    for (const line of step.logs || []) {
      appendDebugConsole(`  ${line}`);
    }
    appendDebugConsole(`MAC ${step.responseMacValid === null || step.responseMacValid === undefined ? 'not verified' : (step.responseMacValid ? 'valid' : 'invalid')}`);
    if (step.errorMessage) {
      appendDebugConsole(step.errorMessage, 'error');
    }
    await waitForConsoleFrame();
    appendDebugConsoleBlock(`STEP ${index + 1} REQUEST XML`, step.signedRequestXml || step.requestXml);
    appendDebugConsoleBlock(`STEP ${index + 1} REQUEST HEX`, step.requestHex);
    await waitForConsoleFrame();
    appendDebugConsoleBlock(`STEP ${index + 1} RESPONSE XML`, step.responseXml);
    appendDebugConsoleBlock(`STEP ${index + 1} RESPONSE HEX`, step.responseHex);
    await waitForConsoleFrame();
  }
}

function waitForConsoleFrame() {
  return new Promise((resolve) => requestAnimationFrame(resolve));
}

function scrollDebugConsoleToBottom() {
  requestAnimationFrame(() => {
    const output = debugConsoleOutputRef.value;
    if (output) {
      output.scrollTop = output.scrollHeight;
    }
  });
}

function clearDebugConsole() {
  debugConsoleEntries.value = [];
}

async function copyDebugConsole() {
  try {
    await copyTextToClipboard(debugConsoleText.value);
    showToast('Console 日志已复制。', 'success');
  } catch (error) {
    showToast('复制失败，请检查剪贴板权限。', 'error');
  }
}

function formatStepAscii(step, direction) {
  const label = step.label || capabilityLabel(step.capability);
  const hex = direction === 'request' ? step.requestHex : step.responseHex;
  return `# ${label}\n${hexToAscii(hex) || '无原始报文'}`;
}

function hexToAscii(hex) {
  const compact = String(hex || '').replace(/[^0-9a-f]/gi, '');
  if (!compact) return '';
  const chars = [];
  for (let index = 0; index < compact.length; index += 2) {
    const byte = Number.parseInt(compact.slice(index, index + 2), 16);
    if (Number.isNaN(byte)) continue;
    chars.push(byte >= 32 && byte <= 126 ? String.fromCharCode(byte) : '.');
  }
  return chars.join('');
}

function openSmartReplacePreview() {
  const sourceXml = debugRequest.value.requestXml?.trim();
  if (!sourceXml) {
    showToast('请先粘贴或生成 XML 请求报文。', 'warning');
    return;
  }
  smartReplaceSelection.value = normalizeSmartReplaceSelection(smartReplaceSelection.value);
  smartReplaceContext.value = createSmartReplaceContext();
  try {
    refreshSmartReplacePreview();
    smartReplaceOpen.value = true;
  } catch (error) {
    showToast(error.message || '智能替换失败，请检查 XML 格式。', 'error');
  }
}

function closeSmartReplacePreview() {
  smartReplaceOpen.value = false;
  smartReplaceContext.value = null;
  smartReplaceChanges.value = [];
  smartReplaceXmlDraft.value = '';
}

function refreshSmartReplacePreview() {
  const sourceXml = debugRequest.value.requestXml?.trim();
  if (!sourceXml) {
    smartReplaceChanges.value = [];
    smartReplaceXmlDraft.value = '';
    return;
  }
  if (!smartReplaceContext.value) {
    smartReplaceContext.value = createSmartReplaceContext();
  }
  const preview = buildSmartReplacePreview(sourceXml, smartReplaceSelection.value, smartReplaceContext.value);
  smartReplaceChanges.value = preview.changes;
  smartReplaceXmlDraft.value = preview.xml;
}

function applySmartReplace() {
  if (!smartReplaceXmlDraft.value || !smartReplaceChanges.value.length) {
    closeSmartReplacePreview();
    return;
  }
  debugRequest.value.requestXml = smartReplaceXmlDraft.value;
  debugMessageView.value = 'xml';
  closeSmartReplacePreview();
  showToast('智能替换已应用到请求 XML。', 'success');
}

function buildSmartReplacePreview(sourceXml, selection = defaultSmartReplaceSelection(), context = createSmartReplaceContext()) {
  const xmlDocument = new DOMParser().parseFromString(sourceXml, 'application/xml');
  const error = xmlDocument.querySelector('parsererror');
  if (error) {
    throw new Error('XML 格式不正确，无法智能替换。');
  }
  const root = xmlDocument.querySelector('isomsg') || xmlDocument.documentElement;
  if (!root) {
    throw new Error('没有找到 isomsg 根节点。');
  }

  const changes = [];
  for (const replacement of smartReplaceItems(selection, context)) {
    const fieldElement = findIsoFieldElement(root, replacement.field);
    const before = fieldElement?.getAttribute('value') ?? '';
    if (fieldElement && before === replacement.after) {
      continue;
    }
    const target = fieldElement || xmlDocument.createElement('field');
    if (!fieldElement) {
      target.setAttribute('id', replacement.field);
      root.appendChild(target);
    }
    target.setAttribute('value', replacement.after);
    changes.push({
      ...replacement,
      before,
      beforeDisplay: fieldElement ? (before || '(空值)') : '未设置',
      created: !fieldElement
    });
  }

  return {
    changes,
    xml: formatIsoXmlRoot(root)
  };
}

function smartReplaceItems(selection, context) {
  const normalizedSelection = normalizeSmartReplaceSelection(selection);
  return [
    ...(normalizedSelection.dynamicParameters ? smartDynamicParameterItems() : []),
    ...(normalizedSelection.environmentVariables ? smartEnvironmentVariableItems() : []),
    ...(normalizedSelection.stan ? smartStanItems(context) : []),
    ...(normalizedSelection.timestamps ? smartTimestampItems(context) : []),
    ...(normalizedSelection.rrn ? smartRrnItems(context) : [])
  ].filter((item) => hasSmartReplaceValue(item.after));
}

function smartEnvironmentVariableItems() {
  return [
    { field: '2', label: 'DE2', source: '环境变量 / PAN', after: smartConfigValue('testPan', 'DE2', 'de2') },
    { field: '14', label: 'DE14', source: '环境变量 / 卡号有效期', after: smartConfigValue('testDe14', 'DE14', 'de14', 'cardExpiry') },
    { field: '41', label: 'DE41', source: '环境变量 / TID', after: smartConfigValue('testTid', 'DE41', 'de41') },
    { field: '52', label: 'DE52', source: '环境变量 / PIN Block', after: smartConfigValue('testDe52', 'DE52', 'de52') }
  ];
}

function smartDynamicParameterItems() {
  return [
    { field: '18', label: 'DE18', source: '动态参数 / MCC', after: smartConfigValue('testDe18', 'DE18', 'de18') },
    { field: '42', label: 'DE42', source: '动态参数 / 商户号', after: smartConfigValue('testDe42', 'DE42', 'de42') },
    { field: '43', label: 'DE43', source: '动态参数 / 商户名称地址', after: smartConfigValue('testDe43', 'DE43', 'de43') },
    { field: '49', label: 'DE49', source: '动态参数 / 币种', after: smartConfigValue('testDe49', 'DE49', 'de49') }
  ];
}

function smartConfigValue(...keys) {
  const sources = [
    environmentVariableMap(activeEnvironment.value),
    activeEnvironment.value || {},
    keySettings.value || {},
    selectedChannel.value?.config || {},
    selectedChannel.value || {}
  ];
  for (const source of sources) {
    for (const key of keys) {
      if (hasSmartReplaceValue(source[key])) {
        return String(source[key]).trim();
      }
    }
  }
  return '';
}

function smartStanItems(context) {
  return [
    { field: '11', label: 'DE11', source: 'STAN / 6 位随机流水', after: context.stan }
  ];
}

function smartTimestampItems(context) {
  const { month, day, hour, minute, second } = smartTimeParts(context.now);
  return [
    { field: '7', label: 'DE7', source: '时间戳 / MMDDhhmmss', after: `${month}${day}${hour}${minute}${second}` },
    { field: '12', label: 'DE12', source: '时间戳 / hhmmss', after: `${hour}${minute}${second}` },
    { field: '13', label: 'DE13', source: '时间戳 / MMDD', after: `${month}${day}` }
  ];
}

function smartRrnItems(context) {
  const { year, month, day, hour, minute, second } = smartTimeParts(context.now);
  return [
    { field: '37', label: 'DE37', source: 'RRN / YYMMDDhhmmss', after: `${year}${month}${day}${hour}${minute}${second}` }
  ];
}

function createSmartReplaceContext() {
  return {
    now: new Date(),
    stan: randomSixDigits()
  };
}

function smartTimeParts(now) {
  const month = pad2(now.getMonth() + 1);
  const day = pad2(now.getDate());
  const hour = pad2(now.getHours());
  const minute = pad2(now.getMinutes());
  const second = pad2(now.getSeconds());
  const year = String(now.getFullYear()).slice(-2);
  return { year, month, day, hour, minute, second };
}

function normalizeSmartReplaceSelection(selection) {
  return {
    ...defaultSmartReplaceSelection(),
    ...(selection || {})
  };
}

function hasSmartReplaceValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== '';
}

function findIsoFieldElement(root, fieldId) {
  const canonical = canonicalFieldKey(fieldId);
  return Array.from(root.querySelectorAll('field'))
    .find((field) => canonicalFieldKey(field.getAttribute('id')) === canonical);
}

function formatIsoXmlRoot(root) {
  const rootName = root.tagName || 'isomsg';
  const rootAttrs = formatXmlAttributes(root);
  const lines = [`<${rootName}${rootAttrs}>`];
  for (const child of Array.from(root.children)) {
    lines.push(`  ${serializeIsoXmlElement(child)}`);
  }
  lines.push(`</${rootName}>`);
  return lines.join('\n');
}

function serializeIsoXmlElement(element) {
  const tagName = element.tagName;
  const attrs = formatXmlAttributes(element);
  const children = Array.from(element.children);
  const text = element.textContent || '';
  if (!children.length && !text.trim()) {
    return `<${tagName}${attrs}/>`;
  }
  if (!children.length) {
    return `<${tagName}${attrs}>${escapeXmlText(text)}</${tagName}>`;
  }
  const childLines = children.map((child) => `  ${serializeIsoXmlElement(child)}`);
  return `<${tagName}${attrs}>\n${childLines.join('\n')}\n</${tagName}>`;
}

function formatXmlAttributes(element) {
  return Array.from(element.attributes || [])
    .map((attr) => ` ${attr.name}="${escapeXmlAttr(attr.value)}"`)
    .join('');
}

function escapeXmlAttr(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

function escapeXmlText(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

function randomSixDigits() {
  return String(Math.floor(Math.random() * 1000000)).padStart(6, '0');
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

async function generateDebugXml(showGeneratedToast = true) {
  if (!debugChannelId.value) return '';
  appendDebugConsole(`Build XML · ${capabilityLabel(debugRequest.value.capability)} · Env ${activeEnvironmentName.value}`);
  try {
    const generated = await api.buildPosDebugXml(debugChannelId.value, {
      environmentId: activeEnvironmentId.value,
      capability: debugRequest.value.capability,
      tid: debugEnvironmentTid.value,
      sn: debugEnvironmentSn.value,
      environmentFields: debugEnvironmentFieldsMap(),
      dynamicFields: debugDynamicFieldsMap(),
      fields: debugFieldsMap()
    });
    debugRequest.value.requestXml = generated.requestXml;
    appendDebugConsoleBlock('GENERATED REQUEST XML', generated.requestXml);
    if (showGeneratedToast) {
      showToast('XML 已生成。', 'success');
    }
    return generated.requestXml;
  } catch (error) {
    appendDebugConsole(error.message || '生成 XML 失败', 'error');
    showToast(error.message || '生成 XML 失败', 'error');
    return '';
  }
}

function debugEnvironmentPayload() {
  const environment = activeEnvironment.value || {};
  const target = debugResolvedTarget.value;
  return {
    environmentId: activeEnvironmentId.value,
    targetIp: target.ip || environment.targetIp || '',
    targetPort: target.port || normalizePort(environment.targetPort),
    macAlgorithm: environment.macAlgorithm || 'ANSI_X9_19',
    pinAlgorithm: environment.pinAlgorithm || 'NONE',
    pan: environment.testPan || '',
    tpkPlain: environment.tpkPlain || '',
    tskPlain: environment.tskPlain || '',
    macField: environment.macField || '',
    de52: environment.testDe52 || ''
  };
}

async function generateDebugXmlFromModal() {
  const generatedXml = await generateDebugXml();
  if (generatedXml) {
    closeDebugToolModal();
  }
}

async function runPosDebug() {
  if (!debugChannelId.value) return;
  if (!debugTargetConfigured.value) {
    appendDebugConsole('TCP 地址栏没有有效 IP:Port，发送已取消。', 'warn');
    showToast('TCP 地址栏没有有效 IP:Port，请填写或使用 Environment 变量。', 'warning');
    return;
  }
  if (!debugRequest.value.requestXml?.trim()) {
    const generatedXml = await generateDebugXml(false);
    if (!generatedXml) return;
  }
  debugLoading.value = true;
  const payload = {
    capability: debugRequest.value.capability,
    requestXml: debugRequest.value.requestXml,
    pin: debugRequest.value.pin,
    saveTidConfig: Boolean(debugRequest.value.saveTidConfig && debugCanSaveTidConfig.value),
    ...debugEnvironmentPayload()
  };
  appendDebugConsole(`Send POS · ${debugTransportProtocol.value} · ${capabilityLabel(payload.capability)} · ${payload.targetIp}:${payload.targetPort} · Env ${activeEnvironmentName.value}`);
  appendDebugConsole(`MAC ${payload.macAlgorithm || '-'} · PIN ${payload.pinAlgorithm || '-'}`);
  appendDebugConsoleBlock('REQUEST XML BEFORE SEND', payload.requestXml);
  try {
    debugResult.value = await api.sendPosDebug(debugChannelId.value, payload);
    debugResultKind.value = 'send';
    debugMessageView.value = 'xml';
    await appendDebugResultConsole(debugResult.value);
    await saveDebugHistory('send');
    showToast(debugResult.value.success ? 'POS Debug 已完成。' : 'POS Debug 返回失败结果，请查看步骤和 Console。', debugResult.value.success ? 'success' : 'warning');
  } catch (error) {
    appendDebugConsole(error.message || 'POS Debug 执行失败', 'error');
    showToast(error.message || 'POS Debug 执行失败', 'error');
  } finally {
    debugLoading.value = false;
  }
}

async function runTidInitDebug(options = {}) {
  if (!debugChannelId.value) return;
  if (!debugTargetConfigured.value) {
    appendDebugConsole('TCP 地址栏没有有效 IP:Port，TID 初始化已取消。', 'warn');
    showToast('TCP 地址栏没有有效 IP:Port，请填写或使用 Environment 变量。', 'warning');
    return false;
  }
  if (!debugEnvironmentTid.value.trim() || !debugEnvironmentSn.value.trim()) {
    appendDebugConsole('TID 初始化缺少 TID 或 SN，执行已取消。', 'warn');
    showToast('TID 初始化需要 Environment 里配置 TID 和 SN。', 'warning');
    return false;
  }
  debugLoading.value = true;
  const payload = {
    tid: debugEnvironmentTid.value,
    sn: debugEnvironmentSn.value,
    saveKey: debugTidInit.value.saveKey,
    ...debugEnvironmentPayload()
  };
  appendDebugConsole(`TID init · ${debugTransportProtocol.value} · ${payload.targetIp}:${payload.targetPort} · TID ${payload.tid} · SN ${payload.sn}`);
  appendDebugConsole('TID init steps · TID-TMK -> TID-TPK -> TID-TSK');
  if (options.closeModalOnStart) {
    closeDebugToolModal();
  }
  try {
    debugResult.value = await api.tidInitPosDebug(debugChannelId.value, payload);
    debugResultKind.value = 'tid';
    debugRequest.value.tid = debugEnvironmentTid.value;
    debugRequest.value.sn = debugEnvironmentSn.value;
    debugMessageView.value = 'xml';
    await appendDebugResultConsole(debugResult.value);
    await saveDebugHistory('tid');
    showToast(debugResult.value.success ? 'TID 初始化三步已执行。' : 'TID 初始化存在失败步骤，请查看步骤和 Console。', debugResult.value.success ? 'success' : 'warning');
    return true;
  } catch (error) {
    appendDebugConsole(error.message || 'TID 初始化失败', 'error');
    showToast(error.message || 'TID 初始化失败', 'error');
    return false;
  } finally {
    debugLoading.value = false;
  }
}

async function runTidInitFromModal() {
  await runTidInitDebug({ closeModalOnStart: true });
}

function loadDebugStepRequest(step) {
  if (!step) return;
  debugRequest.value.capability = step.capability || debugRequest.value.capability;
  debugRequest.value.requestXml = step.signedRequestXml || step.requestXml || '';
  showToast('已载入该步骤请求 XML。', 'success');
}

async function copyDebugStepHex(step) {
  if (!step?.requestHex) {
    showToast('当前步骤没有可复制的请求 HEX。', 'warning');
    return;
  }
  try {
    await copyTextToClipboard(step.requestHex);
    showToast('请求 Frame HEX 已复制。', 'success');
  } catch (error) {
    showToast('复制失败，请检查剪贴板权限。', 'error');
  }
}

function newChannel() {
  channelEditor.value = { ...emptyChannel(), port: nextAvailablePort() };
  channelMode.value = 'create';
  channelSection.value = 'basic';
  tab.value = 'channel-new';
  restartNotice.value = '';
}

function editCurrentChannel() {
  if (!selectedChannel.value) return;
  const config = JSON.parse(JSON.stringify(selectedChannel.value.config || {}));
  const channelCode = channelDisplayCode(selectedChannel.value);
  channelEditor.value = {
    ...emptyChannel(),
    ...config,
    id: selectedChannel.value.id,
    channelCode,
    name: channelCode,
    thirdPartyTestIp: config.thirdPartyTestIp || selectedChannel.value.thirdPartyTestIp || '',
    thirdPartyTestPort: config.thirdPartyTestPort ?? selectedChannel.value.thirdPartyTestPort ?? null,
    thirdPartyTlsEnabled: Boolean(config.thirdPartyTlsEnabled ?? selectedChannel.value.thirdPartyTlsEnabled),
    mockTlsEnabled: config.mockTlsEnabled ?? selectedChannel.value.mockTlsEnabled ?? null,
    mockCtmk1: config.mockCtmk1 || '',
    mockCtmk2: config.mockCtmk2 || ''
  };
  normalizePackagerMode();
  channelMode.value = 'edit';
  channelSection.value = 'basic';
  tab.value = 'channel-new';
  restartNotice.value = selectedChannel.value.source === 'application.yml'
    ? '当前渠道来自 application.yml；保存后会生成数据库覆盖配置。运行时字段变更仍需重启后端。'
    : '';
}

function nextChannelCode() {
  let index = channels.value.length + 1;
  let code = `channel-${index}`;
  while (channels.value.some((channel) => channelDisplayCode(channel) === code)) {
    index += 1;
    code = `channel-${index}`;
  }
  return code;
}

function nextAvailablePort() {
  const usedPorts = new Set(channels.value.map((channel) => Number(channel.port)).filter(Number.isInteger));
  for (let port = 14400; port <= 14700; port += 1) {
    if (!usedPorts.has(port)) {
      return port;
    }
  }
  return 14400;
}

function channelPackagerFingerprint(channel) {
  if (!channel) return '';
  const parts = [
    channel.packagerType,
    channel.packagerConfigMode,
    channel.packagerLocation,
    channel.packagerFileName,
    channel.packagerContent,
    channel.packagerClassName
  ].map((part) => (part || '').trim());
  const hasConcreteConfig = parts.slice(2).some(Boolean);
  return hasConcreteConfig ? parts.join('|') : '';
}

function hasExplicitPackagerConfig(channel) {
  normalizePackagerMode();
  if (channel.packagerType === 'XML' && channel.packagerConfigMode === 'XML_CONTENT') {
    return Boolean((channel.packagerContent || '').trim());
  }
  if (channel.packagerType === 'XML' && channel.packagerConfigMode === 'XML_FILE') {
    return Boolean((channel.packagerFileName || channel.packagerLocation || '').trim());
  }
  if (channel.packagerType === 'CUSTOM' && channel.packagerConfigMode === 'JAVA_SOURCE') {
    return Boolean((channel.packagerContent || '').trim());
  }
  return Boolean((channel.packagerClassName || '').trim());
}

async function saveChannel() {
  normalizePackagerMode();
  restartNotice.value = '';
  const channelCode = (channelEditor.value.channelCode || '').trim();
  if (!channelCode) {
    showToast('渠道 Code 不能为空。', 'warning');
    return;
  }
  const port = Number(channelEditor.value.port);
  if (!Number.isInteger(port)) {
    showToast('监听端口不能为空。系统会推荐端口，也可以手工选择空闲端口。', 'warning');
    return;
  }
  if (port < 14400 || port > 14700) {
    showToast('端口必须在 14400 到 14700 之间。', 'warning');
    return;
  }
  if (isChannelPortDuplicate.value) {
    showToast('监听端口已被其他渠道占用，请换一个空闲端口。', 'warning');
    return;
  }
  if (isChannelCodeDuplicate.value) {
    showToast('渠道 Code 已存在，请换一个唯一的 code。', 'warning');
    return;
  }
  if (!hasExplicitPackagerConfig(channelEditor.value)) {
    channelSection.value = 'packager';
    showToast('Packager 必须填写该渠道真实方言，不能使用空配置或默认占位。', 'warning');
    return;
  }
  if (isChannelPackagerDuplicate.value) {
    channelSection.value = 'packager';
    showToast('当前 Packager 已被其它渠道使用，请为当前渠道配置独立方言。', 'warning');
    return;
  }
  const thirdPartyTestIp = (channelEditor.value.thirdPartyTestIp || '').trim();
  if (!thirdPartyTestIp) {
    showToast('第三方测试环境 IP 必填。', 'warning');
    return;
  }
  const thirdPartyTestPort = channelEditor.value.thirdPartyTestPort === '' ? null : channelEditor.value.thirdPartyTestPort;
  if (thirdPartyTestPort === null || thirdPartyTestPort === undefined) {
    showToast('第三方测试环境端口必填。', 'warning');
    return;
  }
  if (Number(thirdPartyTestPort) < 1 || Number(thirdPartyTestPort) > 65535) {
    showToast('第三方测试环境端口必须在 1 到 65535 之间。', 'warning');
    return;
  }
  if (!(channelEditor.value.ctmk1 || '').trim()) {
    showToast('第三方测试环境 CTMK1 必填。', 'warning');
    return;
  }
  if (!(channelEditor.value.ctmk2 || '').trim()) {
    showToast('第三方测试环境 CTMK2 必填。', 'warning');
    return;
  }
  try {
    const payload = {
      ...channelEditor.value,
      thirdPartyTestIp,
      thirdPartyTestPort,
      channelCode,
      name: channelCode
    };
    const savedChannel = isEditingChannel.value
      ? await api.updateChannel(channelEditor.value.id, payload)
      : await api.createChannel(payload);
    await reloadChannels(savedChannel.id);
    showToast(channelRestartNotice(savedChannel), savedChannel.restartRequired ? 'warning' : 'success');
    tab.value = 'rules';
    ruleView.value = 'list';
  } catch (error) {
    showToast(error.message || '保存渠道失败', 'error');
  }
}

function channelRestartNotice(channel) {
  if (!channel?.restartRequired) {
    return '渠道配置已保存。本次变更为热生效字段，无需重启。';
  }
  const reasons = channel.restartReasons?.length ? `原因：${channel.restartReasons.join('；')}。` : '';
  return `渠道配置已保存。本次变更需要重启后端后生效。${reasons}`;
}

function normalizePackagerMode() {
  const type = channelEditor.value.packagerType;
  const mode = channelEditor.value.packagerConfigMode;
  if (type === 'XML' && !['XML_CONTENT', 'XML_FILE'].includes(mode)) {
    channelEditor.value.packagerConfigMode = 'XML_CONTENT';
  }
  if (type === 'CLASS') {
    channelEditor.value.packagerConfigMode = 'CLASS_NAME';
  }
  if (type === 'CUSTOM' && !['JAVA_SOURCE', 'CLASS_NAME'].includes(mode)) {
    channelEditor.value.packagerConfigMode = 'JAVA_SOURCE';
  }
}

function setPackagerType(type) {
  channelEditor.value.packagerType = type;
  normalizePackagerMode();
}

async function readPackagerFile(event, kind) {
  const file = event.target.files?.[0];
  if (!file) return;
  channelEditor.value.packagerContent = await file.text();
  if (kind === 'xml') {
    channelEditor.value.packagerType = 'XML';
    channelEditor.value.packagerConfigMode = 'XML_CONTENT';
    channelEditor.value.packagerFileName = file.name;
  } else {
    channelEditor.value.packagerType = 'CUSTOM';
    channelEditor.value.packagerConfigMode = 'JAVA_SOURCE';
    channelEditor.value.packagerFileName = file.name;
  }
  event.target.value = '';
}

async function reloadChannels(preferredId) {
  channels.value = await api.listChannels();
  const next = channels.value.find((channel) => channel.id === preferredId) || channels.value[0];
  if (next) {
    selectedChannel.value = next;
    await saveLastChannelPreference(next.id);
    await loadFieldCatalog();
    await loadRules();
  }
}

async function toggleRule(rule) {
  const nextEnabled = !rule.enabled;
  try {
    await api.setRuleEnabled(selectedChannel.value.id, rule.id, nextEnabled);
    await loadRules();
    showToast(nextEnabled ? '规则已启用。' : '规则已停用。', 'success');
  } catch (error) {
    showToast(error.message || '更新规则状态失败', 'error');
  }
}

async function togglePublicRule(rule) {
  const nextPublic = !rule.publicRule;
  try {
    await api.setRulePublic(selectedChannel.value.id, rule.id, nextPublic);
    await loadRules();
    showToast(nextPublic ? '规则已公开。' : '已取消公开。', 'success');
  } catch (error) {
    showToast(error.message || '更新公开状态失败', 'error');
  }
}

async function loadPublicRules() {
  publicRules.value = selectedChannel.value ? await api.listPublicRules(selectedChannel.value.id) : [];
}

async function copyPublicRule(rule) {
  try {
    await api.copyPublicRule(selectedChannel.value.id, rule.id);
    await loadRules();
    tab.value = 'rules';
    showToast('公开规则已复制到当前渠道。', 'success');
  } catch (error) {
    showToast(error.message || '复制公开规则失败', 'error');
  }
}

async function removeRule(rule) {
  try {
    await api.deleteRule(selectedChannel.value.id, rule.id);
    await loadRules();
    showToast('规则已删除。', 'success');
  } catch (error) {
    showToast(error.message || '删除规则失败', 'error');
  }
}

function normalizeFieldDefinition(field) {
  return {
    ...field,
    valueType: fieldValueType(field)
  };
}

function fieldValueType(field) {
  if (field?.valueType) return field.valueType;
  const type = String(field?.type || '').toUpperCase();
  if (type.includes('BINARY') || type.includes('BITMAP')) return 'BINARY';
  if (type.includes('NUM') || type.includes('AMOUNT')) return 'NUMERIC';
  if (type.includes('CHAR') || type.includes('STRING')) return 'TEXT';
  return 'UNKNOWN';
}

function fieldTypeLabel(valueType) {
  const labels = {
    NUMERIC: '数值',
    TEXT: '文本',
    BINARY: '二进制',
    UNKNOWN: '未知'
  };
  return labels[valueType] || labels.UNKNOWN;
}

function conditionFieldDefinition(condition) {
  const key = canonicalFieldKey(condition?.field);
  return key ? fieldCatalogById.value[key] : null;
}

function conditionFieldValueType(condition) {
  return conditionFieldDefinition(condition)?.valueType || 'UNKNOWN';
}

function conditionFieldTypeLabel(condition) {
  return fieldTypeLabel(conditionFieldValueType(condition));
}

function conditionFieldHint(condition) {
  const key = canonicalFieldKey(condition?.field);
  if (!key) return '输入字段号后识别';
  const field = conditionFieldDefinition(condition);
  if (!field) return '当前 Packager 未定义';
  const parts = [field.type, field.length ? `${field.length} 位` : '', field.description].filter(Boolean);
  return parts.join(' · ');
}

function conditionOperatorOptions(condition) {
  const valueType = conditionFieldValueType(condition);
  return (operatorsByValueType[valueType] || operatorsByValueType.UNKNOWN).map((operator) => ({
    value: operator,
    label: operatorLabels[operator] || operator
  }));
}

function ensureConditionOperator(condition) {
  const allowed = conditionOperatorOptions(condition).map((operator) => operator.value);
  if (!allowed.includes(condition.operator)) {
    condition.operator = allowed[0] || 'EQ';
  }
  if (!conditionNeedsValue(condition.operator)) {
    condition.value = '';
  }
}

function ensureRuleConditionOperators(rule) {
  for (const condition of rule?.conditions || []) {
    ensureConditionOperator(condition);
  }
}

function conditionValueLabel(condition) {
  if (condition.operator === 'IN') return '列表值';
  if (conditionFieldValueType(condition) === 'NUMERIC') return '数值';
  return '匹配值';
}

function conditionValuePlaceholder(condition) {
  if (condition.operator === 'IN') return '用逗号分隔多个值';
  if (conditionFieldValueType(condition) === 'NUMERIC') return '例如 20000';
  if (conditionFieldValueType(condition) === 'BINARY') return 'HEX 或原始值';
  return '匹配值';
}

function conditionInputMode(condition) {
  return conditionFieldValueType(condition) === 'NUMERIC' ? 'decimal' : undefined;
}

function addCondition() {
  editingRule.value.conditions.push({ field: '', operator: 'EQ', value: '' });
}

function normalizeConditionField(condition) {
  condition.field = canonicalFieldKey(condition.field);
  ensureConditionOperator(condition);
}

function conditionNeedsValue(operator) {
  return !['EXISTS', 'NOT_EXISTS'].includes(operator);
}

function hydrateResponseRows() {
  responseRows.value = Object.entries(editingRule.value.response.fields || {}).map(([key, value]) => ({
    key: canonicalFieldKey(key),
    value: typeof value === 'string' ? value : value.value || value.sourceField || value.pattern || ''
  }));
  responseXmlImport.value = '';
  responseInputMode.value = 'fields';
}

function normalizeResponseRow(row) {
  row.key = canonicalFieldKey(row.key);
  syncResponseFields();
}

function syncResponseFields() {
  const fields = {};
  for (const row of responseRows.value) {
    const key = canonicalFieldKey(row.key);
    if (!key) continue;
    fields[key] = { type: 'FIXED', value: row.value || '' };
  }
  editingRule.value.response.fields = fields;
}

function addResponseRow() {
  responseRows.value.push({ key: '', value: '' });
}

function removeResponseRow(field) {
  responseRows.value = responseRows.value.filter((item) => item !== field);
  syncResponseFields();
}

async function runTest() {
  if (!currentUser.value || !selectedChannel.value) return;
  try {
    const history = await api.runTestHistory(currentUser.value.id, selectedChannel.value.id, testXml.value);
    selectedTestHistoryId.value = history.id;
    testResult.value = history.success
      ? (history.responseXml || '验证完成，但响应为空。')
      : `ERROR ${history.errorMessage || '验证失败'}`;
    await loadTestHistory();
  } catch (error) {
    testResult.value = `ERROR ${error.message || '验证失败'}`;
    showToast(error.message || '验证失败', 'error');
  }
}

async function serializeWireRequest() {
  if (!selectedChannel.value) return;
  wirePreview.value = null;
  try {
    wirePreview.value = await api.serializeWireRequest(selectedChannel.value.id, testXml.value);
  } catch (error) {
    showToast(error.message || '序列化 TCP 请求失败', 'error');
  }
}

async function deserializeWireResponse() {
  if (!selectedChannel.value) return;
  parsedResponseXml.value = '';
  try {
    const parsed = await api.deserializeWireResponse(selectedChannel.value.id, responseHexInput.value);
    parsedResponseXml.value = parsed.responseXml || '';
  } catch (error) {
    showToast(error.message || '反序列化响应失败', 'error');
  }
}

async function loadTestHistory() {
  if (!currentUser.value || !selectedChannel.value) {
    testHistory.value = [];
    selectedTestHistoryId.value = null;
    return;
  }
  try {
    testHistory.value = await api.listTestHistory(currentUser.value.id, selectedChannel.value.id);
    if (selectedTestHistoryId.value && !testHistory.value.some((history) => history.id === selectedTestHistoryId.value)) {
      selectedTestHistoryId.value = null;
    }
  } catch (error) {
    showToast(error.message || '读取历史记录失败', 'error');
  }
}

async function deleteTestHistory(history) {
  if (!currentUser.value || !selectedChannel.value || !history) return;
  try {
    await api.deleteTestHistory(currentUser.value.id, selectedChannel.value.id, history.id);
    if (selectedTestHistoryId.value === history.id) {
      selectedTestHistoryId.value = null;
      tab.value = 'test-history';
    }
    await loadTestHistory();
    showToast('历史记录已删除。', 'success');
  } catch (error) {
    showToast(error.message || '删除历史记录失败', 'error');
  }
}

async function clearTestHistory() {
  if (!currentUser.value || !selectedChannel.value || testHistory.value.length === 0) return;
  if (!window.confirm('确认清空当前渠道的验证历史？')) return;
  try {
    await api.clearTestHistory(currentUser.value.id, selectedChannel.value.id);
    selectedTestHistoryId.value = null;
    testHistory.value = [];
    tab.value = 'test-history';
    showToast('历史记录已清空。', 'success');
  } catch (error) {
    showToast(error.message || '清空历史记录失败', 'error');
  }
}

function loadTestHistoryEntry(history) {
  if (!history) return;
  testXml.value = history.requestXml || testXml.value;
  tab.value = 'test';
  testResult.value = history.success ? (history.responseXml || '') : `ERROR ${history.errorMessage || '验证失败'}`;
}

function historyRuleLabel(history) {
  if (!history) return '-';
  if (!history.success) return '验证失败';
  if (!history.matched) return '未命中';
  return history.ruleName || (history.ruleId ? `#${history.ruleId}` : '已命中');
}

async function readResponseXmlFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  responseXmlImport.value = await file.text();
  importResponseXml();
  event.target.value = '';
}

function importResponseXml() {
  const parsed = parseIsoXml(responseXmlImport.value);
  if (parsed.mti) {
    editingRule.value.response.mti = parsed.mti;
  }
  responseRows.value = Object.entries(parsed.fields).map(([key, value]) => ({ key, value }));
  responseInputMode.value = 'fields';
  syncResponseFields();
}

function parseIsoXml(xml) {
  const document = new DOMParser().parseFromString(xml, 'application/xml');
  const error = document.querySelector('parsererror');
  if (error) {
    throw new Error('ISO XML 格式不正确。');
  }
  const result = { mti: '', fields: {} };
  const mti = document.querySelector('mti');
  if (mti?.getAttribute('value')) {
    result.mti = mti.getAttribute('value');
  }
  for (const field of document.querySelectorAll('field')) {
    const id = field.getAttribute('id');
    if (!id || id.toLowerCase() === 'bitmap') continue;
    const value = field.getAttribute('value') || '';
    if (id === '0' || id.toUpperCase() === 'MTI') {
      if (!result.mti) result.mti = value;
      continue;
    }
    result.fields[canonicalFieldKey(id)] = value;
  }
  return result;
}

function conditionSummary(rule) {
  return rule.conditions.map((item) => `${formatFieldLabel(item.field)} ${item.operator} ${item.value || ''}`).join(` ${rule.matchMode} `);
}

function ruleIfSummary(rule) {
  const conditions = (rule.conditions || []).slice(0, 5).map(formatCondition);
  if ((rule.conditions || []).length > 5) {
    conditions.push('...');
  }
  if (conditions.length === 0) return '无附加条件';
  return conditions.join(rule.matchMode === 'ANY' ? ' or ' : ' and ');
}

function isoFieldSegments(text) {
  const value = String(text || '');
  const segments = [];
  const pattern = /\bDE\d{1,3}\b/g;
  let lastIndex = 0;
  for (const match of value.matchAll(pattern)) {
    if (match.index > lastIndex) {
      segments.push({ text: value.slice(lastIndex, match.index), field: false });
    }
    segments.push({ text: match[0], field: true });
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < value.length) {
    segments.push({ text: value.slice(lastIndex), field: false });
  }
  return segments.length ? segments : [{ text: value, field: false }];
}

function formatCondition(condition) {
  const field = formatFieldLabel(condition.field);
  const value = condition.value || '';
  const operatorMap = {
    EQ: '=',
    NE: '!=',
    GT: '>',
    GTE: '>=',
    LT: '<',
    LTE: '<=',
    CONTAINS: ' contains ',
    NOT_CONTAINS: ' not contains ',
    REGEX: ' =~ ',
    IN: ' in '
  };
  if (condition.operator === 'EXISTS') return `${field} exists`;
  if (condition.operator === 'NOT_EXISTS') return `${field} not exists`;
  const operator = operatorMap[condition.operator] || ` ${condition.operator} `;
  return `${field}${operator}${value}`;
}

function ruleThenSummary(rule) {
  return responseCode(rule) || '-';
}

function responseCode(rule) {
  const fields = rule.response?.fields || {};
  const field = fields['39'] || fields.DE39;
  return typeof field === 'string' ? field : field?.value;
}

function canonicalFieldKey(value) {
  if (value === null || value === undefined) return '';
  const raw = String(value).trim();
  if (!raw) return '';
  if (raw.toUpperCase() === 'MTI') return '0';
  const deMatch = raw.match(/^DE\s*(\d+)$/i);
  if (deMatch) return stripLeadingZeros(deMatch[1]);
  if (/^\d+$/.test(raw)) return stripLeadingZeros(raw);
  return raw;
}

function stripLeadingZeros(value) {
  return String(value).replace(/^0+(?!$)/, '');
}

function formatFieldLabel(value) {
  const key = canonicalFieldKey(value);
  if (!key) return '-';
  if (key === '0') return 'MTI';
  if (/^\d+$/.test(key)) return `DE${key}`;
  return key;
}

</script>
