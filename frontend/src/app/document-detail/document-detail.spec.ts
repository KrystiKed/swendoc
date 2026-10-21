import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { DocumentDetail } from './document-detail';

const doc = { id: '1', title: 'Invoice', filename: 'invoice.pdf', contentType: 'application/pdf', size: 2048, uploadedAt: '2026-01-02T10:00:00Z', documentType: null };
const titleRules = { title: [{ constraint: 'NotBlank', attributes: {}, message: 'Please fill in this field.' }] };

function setup() {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
    ],
  });
  return { http: TestBed.inject(HttpTestingController), fixture: TestBed.createComponent(DocumentDetail) };
}

describe('DocumentDetail', () => {
  it('shows the document, validates and saves a new title, toggles a group', async () => {
    const { http, fixture } = setup();
    await fixture.whenStable();
    http.expectOne('/api/validation/title').flush(titleRules);
    http.expectOne('/api/docs/1').flush(doc);
    http.expectOne('/api/groups').flush([{ id: 'g1', name: 'Invoices', createdAt: '2026-01-01T00:00:00Z', documents: [] }]);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')!.textContent).toContain('Invoice');
    expect(el.textContent).toContain('2,048 bytes');

    const input = el.querySelector<HTMLInputElement>('#title-input')!;
    input.value = '   ';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(el.querySelector('#title-error')!.textContent).toContain('Please fill in this field.');
    http.expectNone(r => r.method === 'PUT');

    input.value = ' Invoice 2026 ';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    const put = http.expectOne(r => r.method === 'PUT' && r.url === '/api/docs/1');
    expect(put.request.body).toEqual({ title: 'Invoice 2026' });
    put.flush({ ...doc, title: 'Invoice 2026' });
    await fixture.whenStable();
    expect(el.querySelector('h1')!.textContent).toContain('Invoice 2026');

    el.querySelector<HTMLInputElement>('#groups-section input[type=checkbox]')!.click();
    http.expectOne(r => r.method === 'PUT' && r.url === '/api/groups/g1/documents/1')
      .flush({ id: 'g1', name: 'Invoices', createdAt: '2026-01-01T00:00:00Z', documents: [doc] });
    await fixture.whenStable();
    expect(el.querySelector<HTMLInputElement>('#groups-section input[type=checkbox]')!.checked).toBe(true);
    expect(el.textContent).toContain('Added to group "Invoices".');
  });

  it('shows a not-found state for an unknown id', async () => {
    const { http, fixture } = setup();
    await fixture.whenStable();
    http.expectOne('/api/docs/1').flush(null, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Document not found');
  });
});
