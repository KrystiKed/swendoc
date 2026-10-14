import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Group } from '../api';
import { Header } from '../header';
import { Validation, check, hasErrors, serverErrors } from '../validation';

@Component({
  selector: 'app-groups',
  imports: [Header, DatePipe],
  templateUrl: './groups.html',
})
export class Groups {
  protected readonly api = inject(Api);

  protected readonly groups = signal<Group[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly name = signal('');
  protected readonly creating = signal(false);
  protected readonly createMsg = signal<{ text: string; error: boolean } | null>(null);
  protected readonly nameError = signal<string | null>(null);
  private readonly rules = toSignal(inject(Validation).rules('group'));
  protected readonly showHelp = signal(false);

  protected readonly sorted = computed(() =>
    [...this.groups()].sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }),
    ),
  );
  protected readonly canCreate = computed(() => !!this.name().trim() && !this.creating());

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api.listGroups().subscribe({
      next: (groups) => {
        this.groups.set(groups);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(`Could not load groups (${e?.status || 'network error'}).`);
      },
    });
  }

  protected create(): void {
    const name = this.name().trim();
    const errors = check(this.rules(), { name });
    this.nameError.set(errors['name'] ?? null);
    if (hasErrors(errors)) return;
    this.creating.set(true);
    this.api.createGroup(name).subscribe({
      next: (created) => {
        this.creating.set(false);
        this.name.set('');
        this.nameError.set(null);
        this.createMsg.set({ text: `Group "${created.name}" created.`, error: false });
        this.load();
      },
      error: (e) => {
        this.creating.set(false);
        const fields = serverErrors(e);
        if (fields) return this.nameError.set(fields['name'] ?? Object.values(fields)[0]);
        this.createMsg.set({
          text: `Could not create group (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }

  protected remove(group: Group): void {
    if (!confirm(`Delete group "${group.name}"? The documents themselves are kept.`)) return;
    this.api.deleteGroup(group.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed.'),
    });
  }
}
