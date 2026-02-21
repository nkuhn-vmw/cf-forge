import { X } from 'lucide-react'
import { useNotificationStore } from '../store/notifications.ts'

export function Notifications() {
  const { notifications, removeNotification } = useNotificationStore()

  if (notifications.length === 0) return null

  return (
    <div className="toast-container">
      {notifications.map((n) => (
        <div key={n.id} className={`toast toast-${n.type}`}>
          <span>{n.message}</span>
          <button onClick={() => removeNotification(n.id)} className="toast-dismiss">
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  )
}
