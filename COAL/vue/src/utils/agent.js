/**
 * Agent SSE 连接管理与消息解析工具
 * 用于前端与 AgentChatController 的 SSE 流式通信
 */

const BASE_URL = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'

/**
 * 创建 SSE 连接并发送消息
 * @param {string} message - 用户输入的消息
 * @param {object} callbacks - 回调函数集合
 * @param {string} sessionId - 可选，恢复已有会话
 * @returns {AbortController} - 用于取消请求
 */
export function sendMessage(message, callbacks, sessionId = null) {
  const controller = new AbortController()
  const token = JSON.parse(localStorage.getItem('xm-user') || '{}').token || ''

  fetch(`${BASE_URL}/api/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token
    },
    body: JSON.stringify({
      message,
      sessionId,
      token
    }),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      callbacks.onError?.(`请求失败: ${response.status}`)
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = ''    // 跟踪当前 SSE event: 行

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        // 跟踪 event: 行
        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const rawData = line.substring(5).trim()
          dispatchSSE(currentEvent, rawData, callbacks)
          currentEvent = ''  // 用完即清
        }
      }
    }
    callbacks.onDone?.()
  }).catch(err => {
    if (err.name === 'AbortError') return
    callbacks.onError?.(err.message)
  })

  return controller
}

/**
 * 根据 SSE event 类型分发到对应回调。
 */
function dispatchSSE(eventType, data, callbacks) {
  switch (eventType) {
    case 'thought':
      callbacks.onThought?.(data)
      break
    case 'tool_call':
      try {
        callbacks.onToolCall?.(JSON.parse(data))
      } catch (e) {
        callbacks.onToolCall?.({ tool: data, params: {} })
      }
      break
    case 'tool_trace':
      callbacks.onToolTrace?.(data)
      break
    case 'message':
      callbacks.onMessage?.(data)
      break
    case 'session':
      callbacks.onSession?.(data)
      break
    case 'done':
      callbacks.onDone?.()
      break
    case 'error':
      callbacks.onError?.(data)
      break
    default:
      // 无 event: 行或未知类型，兼容旧格式：data 直接就是内容
      if (data === 'completed') {
        callbacks.onDone?.()
      } else if (data.startsWith('处理请求时出错') || data.startsWith('未登录')) {
        callbacks.onError?.(data)
      } else {
        callbacks.onMessage?.(data)
      }
  }
}

/**
 * 提交反馈
 */
export async function submitFeedback(sessionId, feedback) {
  const token = JSON.parse(localStorage.getItem('xm-user') || '{}').token || ''
  const response = await fetch(`${BASE_URL}/api/agent/feedback`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token
    },
    body: JSON.stringify({ sessionId, feedback })
  })
  return response.json()
}

/**
 * 清除会话
 */
export async function clearSession(sessionId) {
  const token = JSON.parse(localStorage.getItem('xm-user') || '{}').token || ''
  const response = await fetch(`${BASE_URL}/api/agent/clear-session`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token
    },
    body: JSON.stringify({ sessionId })
  })
  return response.json()
}
