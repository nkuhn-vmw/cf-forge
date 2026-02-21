import { create } from 'zustand'

type NotificationType = 'success' | 'error' | 'info'

interface Notification {
  id: string
  type: NotificationType
  message: string
}

interface NotificationState {
  notifications: Notification[]
  addNotification: (type: NotificationType, message: string) => void
  removeNotification: (id: string) => void
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  addNotification: (type, message) => {
    const id = crypto.randomUUID()
    set((state) => ({
      notifications: [...state.notifications, { id, type, message }],
    }))
    setTimeout(() => {
      set((state) => ({
        notifications: state.notifications.filter((n) => n.id !== id),
      }))
    }, 4000)
  },
  removeNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),
}))

// Convenience functions callable from anywhere (no hook needed)
export const notify = {
  success: (message: string) => useNotificationStore.getState().addNotification('success', message),
  error: (message: string) => useNotificationStore.getState().addNotification('error', message),
  info: (message: string) => useNotificationStore.getState().addNotification('info', message),
}
