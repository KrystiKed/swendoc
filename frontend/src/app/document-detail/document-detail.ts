import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Doc, Group } from '../api';
import { Header } from '../header';
import { Validation, check, serverErrors } from '../validation';

@Component({
  selector: 'app-document-detail',
  imports: [Header, DatePipe, DecimalPipe, RouterLink],
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
  /** Object URL of the image preview; the file is fetched with the token, so it can't be linked directly. */
  protected readonly previewUrl = signal<string | null>(null);
  private readonly titleRules = toSignal(inject(Validation).rules('title'));

  protected readonly canSave = computed(() => {
    const newTitle = this.title().trim();
    return !!newTitle && newTitle !== this.doc()?.title && !this.saving();
  });
  protected readonly isImage = computed(() => !!this.doc()?.contentType?.startsWith('image/'));

  constructor() {
    this.load();
    this.api.listGroups().subscribe({
      next: (groups) =>
        this.groups.set(
          [...groups].sort((a, b) =>
            a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }),
          ),
        ),
      error: () => this.groups.set([]),
    });
    inject(DestroyRef).onDestroy(() => {
      const url = this.previewUrl();
      if (url) URL.revokeObjectURL(url);
    });
  }

  private load(): void {
    this.api.get(this.id).subscribe({
      next: (doc) => {
        this.doc.set(doc);
        this.title.set(doc.title);
        this.error.set(null);
        if (this.isImage()) {
          this.api
            .content(doc.id)
            .subscribe({
              next: (blob) => this.previewUrl.set(URL.createObjectURL(blob)),
              error: () => undefined,
            });
        }
      },
      error: (e) => {
        if (e?.status === 404 || e?.status === 400) this.notFound.set(true);
        else this.error.set(`Could not load document (${e?.status || 'network error'}).`);
      },
    });
  }

  protected isMember(group: Group): boolean {
    return group.documents.some((d) => d.id === this.id);
  }

  protected save(): void {
    const newTitle = this.title().trim();
    const invalid = check(this.titleRules(), { title: newTitle })['title'];
    this.titleError.set(invalid ?? null);
    if (invalid) return;
    this.saving.set(true);
    this.api.rename(this.id, newTitle).subscribe({
      next: (doc) => {
        this.saving.set(false);
        this.doc.set(doc);
        this.title.set(doc.title);
        this.msg.set({ text: 'Title saved.', error: false });
      },
      error: (e) => {
        this.saving.set(false);
        const fields = serverErrors(e);
        if (fields) this.titleError.set(fields['title'] ?? Object.values(fields)[0]);
        else
          this.msg.set({
            text: `Could not save title (${e?.status || 'network error'}).`,
            error: true,
          });
      },
    });
  }

  protected toggleGroup(group: Group): void {
    const adding = !this.isMember(group);
    this.pendingGroup.set(group.id);
    (adding
      ? this.api.addToGroup(group.id, this.id)
      : this.api.removeFromGroup(group.id, this.id)
    ).subscribe({
      next: (updated) => {
        this.pendingGroup.set(null);
        this.groups.update((list) =>
          list.map((existing) => (existing.id === updated.id ? updated : existing)),
        );
        this.msg.set({
          text: adding ? `Added to group "${group.name}".` : `Removed from group "${group.name}".`,
          error: false,
        });
      },
      error: (e) => {
        this.pendingGroup.set(null);
        this.msg.set({
          text: `Could not update group (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }

  protected download(): void {
    const doc = this.doc();
    if (!doc) return;
    this.api.download(doc).subscribe({
      error: (e) =>
        this.msg.set({ text: `Download failed (${e?.status || 'network error'}).`, error: true }),
    });
  }

  protected remove(): void {
    const doc = this.doc();
    if (!doc || !confirm(`Delete "${doc.title}"?`)) return;
    this.api.delete(doc.id).subscribe({
      next: () => this.router.navigateByUrl('/documents'),
      error: () => this.msg.set({ text: 'Delete failed.', error: true }),
    });
  }
}
