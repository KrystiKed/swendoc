import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  it('shows an error on 401', async () => {
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(Login);
    const el: HTMLElement = fixture.nativeElement;
    const http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();

    const set = (id: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>('#' + id)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    set('username-input', 'alice');
    set('password-input', 'wrongpass1');
    el.querySelector('form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne('/session');
    expect(req.request.body).toEqual({ username: 'alice', password: 'wrongpass1' });
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.textContent).toContain('Invalid user name or password.');
  });
});
