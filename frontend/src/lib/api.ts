/** BE의 DTO와 일치하는 FE 타입. API 변경 시 양쪽 타입을 함께 수정한다. */
export type Member = { id: number; email: string; nickname: string };
export type AccountType = "CHECKING" | "SAVINGS" | "CASH";
export type Account = {
  id: number;
  memberId: number;
  name: string;
  type: AccountType;
  balance: number;
  currency: string;
};

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...options?.headers },
    cache: "no-store",
    credentials: "same-origin",
  });

  if (!response.ok) {
    // BE가 제공하는 {message}를 우선 사용하고, 예상치 못한 응답은 기본 오류로 처리한다.
    const body = await response.json().catch(() => null) as { message?: string } | null;
    if (response.status === 401 && !path.startsWith("/auth/")) {
      window.dispatchEvent(new Event("assetflow:unauthorized"));
    }
    throw new ApiError(response.status, body?.message ?? `요청 실패 (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function mutate<T>(path: string, method: "POST" | "PATCH", body?: unknown): Promise<T> {
  // JWT 쿠키는 브라우저가 보내고 변경 요청에는 서버가 발급한 CSRF 토큰을 추가한다.
  const csrf = await request<{ token: string; headerName: string }>("/auth/csrf");
  return request<T>(path, {
    method,
    headers: { [csrf.headerName]: csrf.token },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export const assetApi = {
  updateNickname: (nickname: string) => mutate<Member>("/members/me", "PATCH", { nickname }),
  register: (email: string, nickname: string, password: string) =>
    mutate<Member>("/auth/register", "POST", { email, nickname, password }),
  login: (email: string, password: string) => mutate<Member>("/auth/login", "POST", { email, password }),
  logout: () => mutate<void>("/auth/logout", "POST"),
  me: () => request<Member>("/auth/me"),
  listAccounts: () => request<Account[]>("/accounts"),
  createAccount: (name: string, type: AccountType, openingBalance: number) =>
    mutate<Account>("/accounts", "POST", { name, type, openingBalance, currency: "KRW" }),
  renameAccount: (accountId: number, name: string) => mutate<Account>(`/accounts/${accountId}`, "PATCH", { name }),
};
