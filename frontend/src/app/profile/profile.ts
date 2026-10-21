import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Group, User } from '../api';
import { Header } from '../header';
import { FieldErrors, Validation, check, hasErrors, serverErrors } from '../validation';

@Component({
  selector: 'app-profile',
  imports: [Header, DatePipe, RouterLink],
  templateUrl: './profile.html',
})
export class Profile {
  protected readonly api = inject(Api);

  protected readonly user = signal<User | null>(null);
  protected readonly groups = signal<Group[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly current = signal('');
  protected readonly next = signal('');
  protected readonly confirm = signal('');
  protected readonly errors = signal<FieldErrors>({});
  protected readonly saving = signal(false);
  protected readonly msg = signal<{ text: string; error: boolean } | null>(null);
  private readonly rules = toSignal(inject(Validation).rules('password'));

  /** Distinct documents across all groups; one document can sit in several. */
  protected readonly groupedDocs = computed(
    () => new Set(this.groups().flatMap((group) => group.documents.map((d) => d.id))).size,
  );
  protected readonly canSave = computed(
    () => !!this.current() && !!this.next() && !!this.confirm() && !this.saving(),
  );

  constructor() {
    this.api.me().subscribe({
      next: (user) => this.user.set(user),
      error: (e) => this.error.set(`Could not load profile (${e?.status || 'network error'}).`),
    });
    this.api
      .listGroups()
      .subscribe({ next: (groups) => this.groups.set(groups), error: () => this.groups.set([]) });
  }

  protected changePassword(): void {
    const errors = check(this.rules(), {
      currentPassword: this.current(),
      newPassword: this.next(),
    });
    // the only rule the server can't publish: both new-password fields must agree
    if (!errors['newPassword'] && this.next() !== this.confirm())
      errors['confirm'] = 'The passwords do not match.';
    this.errors.set(errors);
    this.msg.set(null);
    if (hasErrors(errors)) return;
    this.saving.set(true);
    this.api.changePassword(this.current(), this.next()).subscribe({
      next: () => {
        this.saving.set(false);
        this.current.set('');
        this.next.set('');
        this.confirm.set('');
        this.msg.set({ text: 'Password changed.', error: false });
      },
      error: (e) => {
        this.saving.set(false);
        if (e?.status === 403)
          return this.errors.set({ currentPassword: 'The current password is wrong.' });
        const fields = serverErrors(e);
        if (fields) return this.errors.set(fields);
        this.msg.set({
          text: `Could not change password (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }
}
