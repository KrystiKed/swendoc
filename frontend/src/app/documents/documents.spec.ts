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
});
