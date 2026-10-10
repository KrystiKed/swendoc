import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Doc } from '../api';
import { Documents } from './documents';

const doc = (id: string, title: string, type: Doc['documentType']): Doc => ({
  id, title, filename: `${title}.bin`, contentType: null, size: 2048, uploadedAt: '2026-01-02T10:00:00Z', documentType: type,
});

describe('Documents', () => {
  it('renders listed docs and filters by search', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Documents);
    await fixture.whenStable();
    http.expectOne('/docs').flush([doc('1', 'Alpha', 'PDF'), doc('2', 'Beta', null)]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(2);

    const input = el.querySelector<HTMLInputElement>('#search-input')!;
    input.value = 'alp';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(el.querySelectorAll('tbody tr').length).toBe(1);
    expect(el.textContent).toContain('Alpha');
  });

  it('adds a document to the group picked in the Add to Group dialog', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Documents);
    await fixture.whenStable();
    http.expectOne('/docs').flush([doc('1', 'Alpha', null)]);
    const group = { id: 'g1', name: 'Invoices', createdAt: '2026-01-02T10:00:00Z', documents: [] };
    http.expectOne('/groups').flush([group]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;

    [...el.querySelectorAll<HTMLButtonElement>('tbody button')].find(b => b.textContent?.includes('Add to Group'))!.click();
    await fixture.whenStable();
    expect(el.querySelector('[role=dialog]')?.textContent).toContain('Alpha');
    [...el.querySelectorAll<HTMLButtonElement>('[role=dialog] button')].find(b => b.textContent?.trim() === 'Add')!.click();

    const req = http.expectOne(r => r.method === 'PUT' && r.url === '/groups/g1/documents/1');
    req.flush({ ...group, documents: [doc('1', 'Alpha', null)] });
    await fixture.whenStable();
    expect(el.querySelector('[role=dialog]')).toBeNull();
    expect(el.textContent).toContain('"Alpha" added to group "Invoices".');
  });

  it('shows the groups of a document and removes it from the picked one', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Documents);
    await fixture.whenStable();
    const alpha = doc('1', 'Alpha', null);
    http.expectOne('/docs').flush([alpha, doc('2', 'Beta', null)]);
    const group = { id: 'g1', name: 'Invoices', createdAt: '2026-01-02T10:00:00Z', documents: [alpha] };
    http.expectOne('/groups').flush([group]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    const rows = [...el.querySelectorAll('tbody tr')];
    const alphaRow = rows.find(r => r.textContent?.includes('Alpha'))!;
    const betaRow = rows.find(r => r.textContent?.includes('Beta'))!;
    expect(alphaRow.textContent).toContain('Invoices');
    expect(betaRow.textContent).not.toContain('Remove from Group');

    [...alphaRow.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.includes('Remove from Group'))!.click();
    await fixture.whenStable();
    [...el.querySelectorAll<HTMLButtonElement>('[role=dialog] button')].find(b => b.textContent?.trim() === 'Remove')!.click();

    http.expectOne(r => r.method === 'DELETE' && r.url === '/groups/g1/documents/1').flush({ ...group, documents: [] });
    await fixture.whenStable();
    expect(el.textContent).toContain('"Alpha" removed from group "Invoices".');
    expect(alphaRow.textContent).not.toContain('Invoices');
  });
});
