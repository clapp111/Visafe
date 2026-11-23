import axios, { isAxiosError, type InternalAxiosRequestConfig } from "axios";
import { getApiErrorCode } from "./error";
import type { components } from "./schema";

type SessionRequest = InternalAxiosRequestConfig & {
  sessionVersion?: number;
  retried?: boolean;
};

const options = {
  baseURL: "/api/v1",
  allowAbsoluteUrls: false,
};

export const api = axios.create(options);
export const authApi = axios.create(options);

let accessToken: string | null = null;
let sessionVersion = 0;
let refresh: { version: number; request: Promise<void> } | undefined;
let logout: Promise<void> | undefined;
let onSessionExpired: (() => void) | undefined;

export function setAccessToken(token: string | null) {
  accessToken = token;
  sessionVersion += 1;
}

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
  return () => {
    if (onSessionExpired === handler) {
      onSessionExpired = undefined;
    }
  };
}

export function reissueAccessToken(): Promise<void> {
  const version = sessionVersion;
  if (refresh?.version === version) {
    return refresh.request;
  }

  const request = authApi
    .post<components["schemas"]["TokenResponse"]>("/auth/reissue")
    .then(({ data }) => {
      if (version !== sessionVersion || !data?.accessToken) {
        throw new Error("Unable to restore the access token");
      }

      accessToken = data.accessToken;
    })
    .finally(() => {
      if (refresh?.version === version) {
        refresh = undefined;
      }
    });

  refresh = { version, request };
  return request;
}

export function logoutSession(): Promise<void> {
  if (logout) {
    return logout;
  }

  // Let any pending refresh cookie arrive before revoking it.
  logout = Promise.resolve(refresh?.request)
    .catch(() => undefined)
    .then(async () => {
      await authApi.post("/auth/logout");
      setAccessToken(null);
    })
    .finally(() => {
      logout = undefined;
    });
  return logout;
}

api.interceptors.request.use(
  (config: SessionRequest) => {
    config.sessionVersion = sessionVersion;
    if (accessToken) {
      config.headers.set("Authorization", `Bearer ${accessToken}`);
    }

    return config;
  },
  undefined,
  { synchronous: true },
);

api.interceptors.response.use(undefined, async (error: unknown) => {
  if (
    !isAxiosError(error) ||
    error.response?.status !== 401 ||
    getApiErrorCode(error) !== "JWT_EXPIRED"
  ) {
    throw error;
  }

  const request: SessionRequest | undefined = error.config;
  const version = sessionVersion;
  if (
    !request ||
    request.retried ||
    request.sessionVersion !== version ||
    !accessToken ||
    logout
  ) {
    throw error;
  }

  request.retried = true;
  try {
    if (request.headers.get("Authorization") === `Bearer ${accessToken}`) {
      await reissueAccessToken();
    }
  } catch (refreshError) {
    if (version === sessionVersion && !logout) {
      setAccessToken(null);
      onSessionExpired?.();
    }
    throw refreshError;
  }

  if (version !== sessionVersion || logout) {
    throw error;
  }

  return api.request(request);
});
