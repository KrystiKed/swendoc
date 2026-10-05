import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

export type DocumentType = 'WORD' | 'PDF' | 'EXCEL';

/** Mirrors DocumentResponse.java. */
export interface Doc {
  id: string;
  title: string;
  filename: string;
  contentType: string | null;
  size: number;
  uploadedAt: string;
  documentType: DocumentType | null;
}

/** Mirrors SessionResponse.java, plus the username the backend doesn't echo back. */
export interface Session {
  token: string;
  userId: string;
  expiresAt: string;
  username: string;
}

const SESSION_KEY = 'windoc.session';

function readSession(): Session | null {
  try {
    const s = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null') as Session | null;
    return s && new Date(s.expiresAt) > new Date() ? s : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);

  readonly session = signal<Session | null>(readSession());

  // --- users / session ---

  login(username: string, password: string): Observable<Session> {
    return this.http.post<Omit<Session, 'username'>>('/session', { username, password }).pipe(
      map(s => ({ ...s, username })),
      tap(s => {
        try {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(s));
        } catch {
          // storage blocked: session lives only in memory
        }
        this.session.set(s);
      }),
    );
  }

  register(username: string, password: string): Observable<unknown> {
    return this.http.post('/users', { username, password });
  }

  logout(): void {
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {}
    this.session.set(null);
  }

  // --- documents ---

  list(): Observable<Doc[]> {
    return this.http.get<Doc[]>('/docs');
  }

  /** Server-side grouping: GET /documents/group/{word|pdf|excel}. */
  listByType(type: DocumentType): Observable<Doc[]> {
    return this.http.get<Doc[]>(`/documents/group/${type.toLowerCase()}`);
  }

  upload(title: string, file: File): Observable<Doc> {
    const form = new FormData();
    form.append('title', title);
    form.append('file', file);
    return this.http.post<Doc>('/docs', form);
  }

  rename(id: string, title: string): Observable<Doc> {
    return this.http.put<Doc>(`/docs/${id}`, { title });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/docs/${id}`);
  }

  /** Plain link; the backend sets Content-Disposition: attachment. */
  contentUrl(id: string): string {
    return `/docs/${id}/content`;
  }
}
