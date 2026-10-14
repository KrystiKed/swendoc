import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Profile } from './profile';

const rules = {
  currentPassword: [{ constraint: 'NotBlank', attributes: {}, message: 'Please fill in this field.' }],
  newPassword: [
    { constraint: 'NotBlank', attributes: {}, message: 'Please fill in this field.' },
    { constraint: 'Size', attributes: { min: 8, max: 128 }, message: 'Must be between 8 and 128 characters long.' },
  ],
};

function type(el: HTMLElement, id: string, value: string) {
  const input = el.querySelector<HTMLInputElement>(id)!;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

describe('Profile', () => {
  it('shows the account and validates the password change', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Profile);
    await fixture.whenStable();
    http.expectOne('/api/validation/password').flush(rules);
    http.expectOne('/api/users/me').flush({ id: 'u1', username: 'alice', createdAt: '2026-01-02T10:00:00Z' });
    http.expectOne('/api/groups').flush([
      { id: 'g1', name: 'A', createdAt: '', documents: [{ id: 'd1' }, { id: 'd2' }] },
      { id: 'g2', name: 'B', createdAt: '', documents: [{ id: 'd1' }] },
    ]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')!.textContent).toContain('alice');
    expect(el.querySelector('#group-count')!.textContent).toBe('2');

    type(el, '#current-password', 'old-password');
    type(el, '#new-password', 'short');
    type(el, '#confirm-password', 'short');
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
    expect(el.querySelector('#new-password-error')!.textContent).toContain('between 8 and 128');

    type(el, '#new-password', 'new-password');
    type(el, '#confirm-password', 'other-password');
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
    expect(el.querySelector('#confirm-password-error')!.textContent).toContain('do not match');
    http.expectNone('/api/users/me/password');

    type(el, '#confirm-password', 'new-password');
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    const put = http.expectOne('/api/users/me/password');
    expect(put.request.body).toEqual({ currentPassword: 'old-password', newPassword: 'new-password' });
    put.flush(null, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();
    expect(el.querySelector('#current-password-error')!.textContent).toContain('current password is wrong');
  });
});
