import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, tap, throwError } from 'rxjs';

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

/** Mirrors DocumentGroupResponse.java. */
export interface Group {
  id: string;
  name: string;
  createdAt: string;
  documents: Doc[];
}

/** Mirrors UserResponse.java. */
export interface User {
  id: string;
  username: string;
  createdAt: string;
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
    const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null') as Session | null;
    return stored && new Date(stored.expiresAt) > new Date() ? stored : null;
  } catch {
    return null;
  }
}

/** Saves a downloaded blob under the given file name. */
function saveAs(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url));
}

/**
 * Sends the session token with every request and handles every 401 the same way: the session is
 * gone (expired, logged out elsewhere), so it ends here too and the user logs in again.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const api = inject(Api);
  const token = api.session()?.token;
  const authorized = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authorized).pipe(
    catchError((error: unknown) => {
      // without a token a 401 is just a failed login, which the login form reports itself
      if (token && error instanceof HttpErrorResponse && error.status === 401) api.endSession();
      return throwError(() => error);
    }),
  );
};

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  private router = inject(Router);
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly session = signal<Session | null>(null);

  constructor() {
    this.setSession(readSession());
  }

  // --- users / session ---

  login(username: string, password: string): Observable<Session> {
    return this.http.post<Omit<Session, 'username'>>('/api/session', { username, password }).pipe(
      // the server ignores surrounding whitespace, so the header shows the name as stored
      map(response => ({ ...response, username: username.trim() })),
      tap(session => {
        try {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
        } catch {
          // storage blocked: session lives only in memory
        }
        this.setSession(session);
      }),
    );
  }

  register(username: string, password: string): Observable<unknown> {
    return this.http.post('/api/users', { username, password });
  }

  me(): Observable<User> {
    return this.http.get<User>('/api/users/me');
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/users/me/password', { currentPassword, newPassword });
  }

  /** Invalidates the token on the server, then ends the session here. */
  logout(): void {
    if (this.session()) {
      // a failure means the token is already invalid or the server is down: nothing left to end there
      this.http.delete('/api/session').subscribe({ error: () => undefined });
    }
    this.endSession();
  }

  /** Forgets the session locally and goes to the login page (logout, expiry, or any 401). */
  endSession(): void {
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {}
    this.setSession(null);
    this.router.navigateByUrl('/login');
  }

  private setSession(session: Session | null): void {
    clearTimeout(this.expiryTimer);
    this.session.set(session);
    if (session) {
      this.expiryTimer = setTimeout(() => this.endSession(), Date.parse(session.expiresAt) - Date.now());
    }
  }

  // --- documents (always the logged-in user's own) ---

  list(): Observable<Doc[]> {
    return this.http.get<Doc[]>('/api/docs');
  }

  get(id: string): Observable<Doc> {
    return this.http.get<Doc>(`/api/docs/${id}`);
  }

  upload(title: string, file: File): Observable<Doc> {
    const form = new FormData();
    form.append('title', title);
    form.append('file', file);
    return this.http.post<Doc>('/api/docs', form);
  }

  rename(id: string, title: string): Observable<Doc> {
    return this.http.put<Doc>(`/api/docs/${id}`, { title });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/docs/${id}`);
  }

  /** The file itself; fetched instead of linked because a plain link cannot send the token. */
  content(id: string): Observable<Blob> {
    return this.http.get(`/api/docs/${id}/content`, { responseType: 'blob' });
  }

  download(doc: Doc): Observable<Blob> {
    return this.content(doc.id).pipe(tap(blob => saveAs(blob, doc.filename)));
  }

  // --- groups (always the logged-in user's own) ---

  listGroups(): Observable<Group[]> {
    return this.http.get<Group[]>('/api/groups');
  }

  getGroup(id: string): Observable<Group> {
    return this.http.get<Group>(`/api/groups/${id}`);
  }

  createGroup(name: string): Observable<Group> {
    return this.http.post<Group>('/api/groups', { name });
  }

  /** Only works for documents the logged-in user uploaded; anything else is a 404. */
  addToGroup(groupId: string, docId: string): Observable<Group> {
    return this.http.put<Group>(`/api/groups/${groupId}/documents/${docId}`, null);
  }

  removeFromGroup(groupId: string, docId: string): Observable<Group> {
    return this.http.delete<Group>(`/api/groups/${groupId}/documents/${docId}`);
  }

  deleteGroup(id: string): Observable<void> {
    return this.http.delete<void>(`/api/groups/${id}`);
  }
}
