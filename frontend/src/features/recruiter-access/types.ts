export type AccessState = {
  enabled: boolean
  authenticated: boolean
  requestInvitationEnabled: boolean
  captchaSiteKey: string | null
  captchaAction: string | null
  expiresAt: string | null
  isChecking: boolean
}

export const INITIAL_ACCESS_STATE: AccessState = {
  enabled: false,
  authenticated: false,
  requestInvitationEnabled: false,
  captchaSiteKey: null,
  captchaAction: null,
  expiresAt: null,
  isChecking: true,
}
