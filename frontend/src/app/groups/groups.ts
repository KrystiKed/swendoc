import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Group } from '../api';
import { Validation, check, hasErrors, serverErrors } from '../validation';

@Component({
  selector: 'app-groups',
  imports: [DatePipe, RouterLink],
  templateUrl: './groups.html',
})
export class Groups {
  protected readonly api = inject(Api);
  private router = inject(Router);

  protected readonly groups = signal<Group[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly name = signal('');
  protected readonly creating = signal(false);
  protected readonly createMsg = signal<{ text: string; error: boolean } | null>(null);
  protected readonly nameError = signal<string | null>(null);
  private readonly rules = toSignal(inject(Validation).rules('group'));
  protected readonly showHelp = signal(false);
  protected readonly now = signal(new Date());

  protected readonly sorted = computed(() =>
    [...this.groups()].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })),
  );
  protected readonly canCreate = computed(() => !!this.name().trim() && !this.creating());

  constructor() {
    this.load();
    const timer = setInterval(() => this.now.set(new Date()), 30000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected load(): void {
    this.loading.set(true);
    this.api.listGroups().subscribe({
      next: g => {
        this.groups.set(g);
        this.error.set(null);
        this.loading.set(false);
      },
      error: e => {
        this.loading.set(false);
        if (e?.status === 401) return this.expired();
        this.error.set(`Could not load groups (${e?.status || 'network error'}).`);
      },
    });
  }

  protected create(): void {
    const n = this.name().trim();
    const errors = check(this.rules(), { name: n });
    this.nameError.set(errors['name'] ?? null);
    if (hasErrors(errors)) return;
    this.creating.set(true);
    this.api.createGroup(n).subscribe({
      next: g => {
        this.creating.set(false);
        this.name.set('');
        this.nameError.set(null);
        this.createMsg.set({ text: `Group "${g.name}" created.`, error: false });
        this.load();
      },
      error: e => {
        this.creating.set(false);
        if (e?.status === 401) return this.expired();
        const fields = serverErrors(e);
        if (fields) return this.nameError.set(fields['name'] ?? Object.values(fields)[0]);
        this.createMsg.set({ text: `Could not create group (${e?.status || 'network error'}).`, error: true });
      },
    });
  }

  protected remove(g: Group): void {
    if (!confirm(`Delete group "${g.name}"? The documents themselves are kept.`)) return;
    this.api.deleteGroup(g.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed.'),
    });
  }

  /** The backend rejected the token (unknown or expired session): log in again. */
  private expired(): void {
    this.api.logout();
    this.router.navigateByUrl('/login');
  }

  protected logout(): void {
    this.api.logout();
    this.router.navigateByUrl('/login');
  }
}
