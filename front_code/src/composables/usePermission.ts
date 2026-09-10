import { useUserStore } from '@/stores/user'

/**
 * 权限检查 composable
 */
export function usePermission() {
  const userStore = useUserStore()

  function hasPermission(code: string): boolean {
    return userStore.hasPermission(code)
  }

  return { hasPermission }
}
