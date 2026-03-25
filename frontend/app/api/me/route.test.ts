import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const getServerSessionMock = vi.fn();

vi.mock("next-auth", () => ({
  getServerSession: getServerSessionMock
}));

vi.mock("@/lib/auth", () => ({
  authOptions: {}
}));

describe("GET /api/me", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.stubEnv("BACKEND_INTERNAL_URL", "http://backend:8080");
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("returns 401 when no session access token exists", async () => {
    getServerSessionMock.mockResolvedValue(null);

    const { GET } = await import("./route");
    const response = await GET();

    expect(response.status).toBe(401);
    await expect(response.json()).resolves.toEqual({ error: "Not authenticated" });
  });

  it("forwards the backend response body on success", async () => {
    getServerSessionMock.mockResolvedValue({ accessToken: "access-token" });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ nickname: "drummer" }), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        })
      )
    );

    const { GET } = await import("./route");
    const response = await GET();

    expect(fetch).toHaveBeenCalledWith(
      "http://backend:8080/api/private/me",
      expect.objectContaining({
        method: "GET",
        headers: expect.objectContaining({
          Authorization: "Bearer access-token",
          Accept: "application/json"
        })
      })
    );
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({ nickname: "drummer" });
  });

  it("maps backend failures to the frontend route response", async () => {
    getServerSessionMock.mockResolvedValue({ accessToken: "access-token" });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: "USER_PROFILE_NOT_FOUND" }), {
          status: 404,
          headers: { "Content-Type": "application/json" }
        })
      )
    );

    const { GET } = await import("./route");
    const response = await GET();

    expect(response.status).toBe(404);
    await expect(response.json()).resolves.toEqual({
      error: "Backend request failed",
      details: { code: "USER_PROFILE_NOT_FOUND" }
    });
  });
});
