import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Groups } from './groups';

describe('Groups', () => {
  it('lists groups and creates a new one with the typed name', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Groups);
    await fixture.whenStable();
    http.expectOne('/groups').flush([{ id: '1', name: 'Invoices', createdAt: '2026-01-02T10:00:00Z', documents: [] }]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(1);
    expect(el.textContent).toContain('Invoices');

    const input = el.querySelector<HTMLInputElement>('#group-name-input')!;
    input.value = '  Contracts  ';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();

    const create = http.expectOne(r => r.method === 'POST' && r.url === '/groups');
    expect(create.request.body).toEqual({ name: 'Contracts' });
    create.flush({ id: '2', name: 'Contracts', createdAt: '2026-01-03T10:00:00Z', documents: [] });
    http.expectOne('/groups').flush([]);
    await fixture.whenStable();
    expect(el.textContent).toContain('Group "Contracts" created.');
  });
});
