import { apiClient } from '@/lib/axios'
import type { PageResponse } from '@/types/api.types'
import type { NotificationResponse } from '@/types/notification.types'

export const notificationService = {
  list: (page = 0, size = 20): Promise<PageResponse<NotificationResponse>> =>
    apiClient.get('/notifications', { params: { page, size } }).then((r) => r.data.data),

  markRead: (id: string): Promise<void> =>
    apiClient.patch(`/notifications/${id}/read`).then(() => undefined),
}
