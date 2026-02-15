export type AuthFetch = (url: string, init?: RequestInit) => Promise<Response>

export type RegisterRequest = {
  login: string
  password: string
  email: string
  displayName: string
}

export const authApi = {
  
  async register(data: RegisterRequest): Promise<void> {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      const errorText = await response.text()
      let errorMessage = errorText

      try {
        const errorJson = JSON.parse(errorText)
        if (errorJson.message) {
          errorMessage = errorJson.message
        }
      } catch {
      }

      throw new Error(errorMessage || 'Registration failed')
    }
  },
}