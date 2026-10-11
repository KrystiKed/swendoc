import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Doc, Group } from '../api';
import { Validation, check, serverErrors } from '../validation';

@Component({
  selector: 'app-document-detail',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './document-detail.html',
})
export class DocumentDetail {
  protected readonly api = inject(Api);
  private router = inject(Router);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id')!;

  protected readonly doc = signal<Doc | null>(null);
  protected readonly notFound = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly groups = signal<Group[]>([]);
  protected readonly title = signal('');
  protected readonly titleError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly msg = signal<{ text: string; error: boolean } | null>(null);
  /** Group id whose membership change is in flight, so its checkbox is disabled meanwhile. */
  protected readonly pendingGroup = signal<string | null>(null);
  private readonly titleRules = toSignal(inject(Validation).rules('title'));
  protected readonly now = signal(new Date());

  protected readonly canSave = computed(() => {
    const t = this.title().trim();
    return !!t && t !== this.doc()?.title && !this.saving();
  });
  /** Browsers render images from the attachment URL, so they get a preview; other types don't. */
  protected readonly isImage = computed(() => !!this.doc()?.contentType?.startsWith('image/'));

  constructor() {
    this.load();
    this.api.listGroups().subscribe({
      next: g => this.groups.set([...g].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))),
      error: e => (e?.status === 401 ? this.logout() : this.groups.set([])),
    });
    const timer = setInterval(() => this.now.set(new Date()), 30000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  private load(): void {
    this.api.get(this.id).subscribe({
      next: d => {
        this.doc.set(d);
        this.title.set(d.title);
        this.error.set(null);
      },
      error: e => {
        if (e?.status === 404 || e?.status === 400) this.notFound.set(true);
        else this.error.set(`Could not load document (${e?.status || 'network error'}).`);
      },
    });
  }

  protected isMember(g: Group): boolean {
    return g.documents.some(d => d.id === this.id);
  }

  protected save(): void {
    const t = this.title().trim();
    const invalid = check(this.titleRules(), { title: t })['title'];
    this.titleError.set(invalid ?? null);
    if (invalid) return;
    this.saving.set(true);
    this.api.rename(this.id, t).subscribe({
      next: d => {
        this.saving.set(false);
        this.doc.set(d);
        this.title.set(d.title);
        this.msg.set({ text: 'Title saved.', error: false });
      },
      error: e => {
        this.saving.set(false);
        const fields = serverErrors(e);
        if (fields) this.titleError.set(fields['title'] ?? Object.values(fields)[0]);
        else this.msg.set({ text: `Could not save title (${e?.status || 'network error'}).`, error: true });
      },
    });
  }

  protected toggleGroup(g: Group): void {
    const adding = !this.isMember(g);
    this.pendingGroup.set(g.id);
    (adding ? this.api.addToGroup(g.id, this.id) : this.api.removeFromGroup(g.id, this.id)).subscribe({
      next: updated => {
        this.pendingGroup.set(null);
        this.groups.update(list => list.map(x => (x.id === updated.id ? updated : x)));
        this.msg.set({ text: adding ? `Added to group "${g.name}".` : `Removed from group "${g.name}".`, error: false });
      },
      error: e => {
        this.pendingGroup.set(null);
        this.msg.set({
          text:
            adding && e?.status === 404
              ? 'Only documents you uploaded while logged in can be added to your groups.'
              : `Could not update group (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }

  protected remove(): void {
    const d = this.doc();
    if (!d || !confirm(`Delete "${d.title}"?`)) return;
    this.api.delete(d.id).subscribe({
      next: () => this.router.navigateByUrl('/documents'),
      error: () => this.msg.set({ text: 'Delete failed.', error: true }),
    });
  }

  protected logout(): void {
    this.api.logout();
    this.router.navigateByUrl('/login');
  }
}
