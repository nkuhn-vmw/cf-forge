import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useNotificationStore, notify } from './notifications'

describe('notifications store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useNotificationStore.setState({ notifications: [] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('adds a notification', () => {
    notify.success('File saved')
    const state = useNotificationStore.getState()
    expect(state.notifications).toHaveLength(1)
    expect(state.notifications[0].type).toBe('success')
    expect(state.notifications[0].message).toBe('File saved')
  })

  it('supports success, error, and info types', () => {
    notify.success('ok')
    notify.error('fail')
    notify.info('info')
    const state = useNotificationStore.getState()
    expect(state.notifications).toHaveLength(3)
    expect(state.notifications.map((n) => n.type)).toEqual(['success', 'error', 'info'])
  })

  it('auto-removes after 4 seconds', () => {
    notify.success('temporary')
    expect(useNotificationStore.getState().notifications).toHaveLength(1)

    vi.advanceTimersByTime(3999)
    expect(useNotificationStore.getState().notifications).toHaveLength(1)

    vi.advanceTimersByTime(1)
    expect(useNotificationStore.getState().notifications).toHaveLength(0)
  })

  it('removes a specific notification manually', () => {
    notify.success('first')
    notify.error('second')
    const state = useNotificationStore.getState()
    expect(state.notifications).toHaveLength(2)

    state.removeNotification(state.notifications[0].id)
    expect(useNotificationStore.getState().notifications).toHaveLength(1)
    expect(useNotificationStore.getState().notifications[0].message).toBe('second')
  })

  it('each notification gets a unique id', () => {
    notify.info('a')
    notify.info('b')
    const ids = useNotificationStore.getState().notifications.map((n) => n.id)
    expect(ids[0]).not.toBe(ids[1])
  })
})
