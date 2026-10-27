const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
export class ApiError extends Error {
    status;
    constructor(message, status) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}
export async function request(path, options = {}) {
    const headers = new Headers();
    if (options.body !== undefined)
        headers.set('Content-Type', 'application/json');
    if (options.token)
        headers.set('Authorization', `Bearer ${options.token}`);
    let response;
    try {
        response = await fetch(`${API_URL}${path}`, {
            method: options.method ?? 'GET',
            headers,
            body: options.body === undefined ? undefined : JSON.stringify(options.body),
        });
    }
    catch {
        throw new ApiError('Não foi possível conectar à API. Confira se ela está iniciada.', 0);
    }
    if (response.status === 204)
        return undefined;
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        throw new ApiError(data?.message ?? `A solicitação falhou (${response.status}).`, response.status);
    }
    return data;
}
