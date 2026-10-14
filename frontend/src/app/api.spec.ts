import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Api, authInterceptor } from './api';

function setup() {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: 'login', children: [] }]),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const api = TestBed.inject(Api);
  const http = TestBed.inject(HttpTestingController);
  const router = TestBed.inject(Router);
  return { api, http, router };
}

function logIn(api: Api, http: HttpTestingController, expiresInMs = 3_600_000) {
  api.login(' alice ', 'password123').subscribe();
  http.expectOne('/api/session').flush({ token: 't1', userId: 'u1', expiresAt: new Date(Date.now() + expiresInMs).toISOString() });
}

describe('Api', () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.useRealTimers();
  });

  it('sends the token with every request and ends the session on any 401', async () => {
    const { api, http, router } = setup();
    logIn(api, http);
    expect(api.session()?.username).toBe('alice');

    api.list().subscribe({ error: () => undefined });
    const request = http.expectOne('/api/docs');
    expect(request.request.headers.get('Authorization')).toBe('Bearer t1');
    request.flush(null, { status: 401, statusText: 'Unauthorized' });
    await new Promise(resolve => setTimeout(resolve));

    expect(api.session()).toBeNull();
    expect(router.url).toBe('/login');
  });

  it('logs out on the server, not only in the browser', () => {
    const { api, http } = setup();
    logIn(api, http);

    api.logout();

    const request = http.expectOne(r => r.method === 'DELETE' && r.url === '/api/session');
    expect(request.request.headers.get('Authorization')).toBe('Bearer t1');
    expect(api.session()).toBeNull();
  });

  it('ends the session when it expires, without waiting for a 401', () => {
    vi.useFakeTimers();
    const { api, http } = setup();
    logIn(api, http, 1000);

    vi.advanceTimersByTime(1001);

    expect(api.session()).toBeNull();
  });
});
