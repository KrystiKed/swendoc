import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Doc, DocumentType, Group } from '../api';
import { Header } from '../header';
import { FieldErrors, Validation, check, hasErrors, serverErrors } from '../validation';

/** 'ALL' shows every document, otherwise the id of one of the user's groups. */
const ALL = 'ALL';
type SortKey = 'title' | 'size' | 'uploadedAt';

const PAGE_SIZE = 10;

const byName = (a: Group, b: Group) =>
  a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });

@Component({
  selector: 'app-documents',
  imports: [Header, DatePipe, RouterLink],
  templateUrl: './documents.html',
})
export class Documents {
  protected readonly api = inject(Api);

  protected readonly docs = signal<Doc[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly groups = signal<Group[]>([]);
  /** The document whose Add to Group dialog is open, if any. */
  protected readonly adding = signal<Doc | null>(null);
  protected readonly targetGroup = signal('');
  protected readonly dialogMode = signal<'add' | 'remove'>('add');
  protected readonly addMsg = signal<{ text: string; error: boolean } | null>(null);
  protected readonly addPending = signal(false);
  protected readonly group = signal<string>(ALL);
  protected readonly sortKey = signal<SortKey>('uploadedAt');
  protected readonly sortAsc = signal(false);
  protected readonly page = signal(0);
  protected readonly selected = signal<ReadonlySet<string>>(new Set());
  protected readonly title = signal('');
  protected readonly file = signal<File | null>(null);
  protected readonly uploading = signal(false);
  protected readonly uploadMsg = signal<{ text: string; error: boolean } | null>(null);
  protected readonly uploadErrors = signal<FieldErrors>({});
  private readonly uploadRules = toSignal(inject(Validation).rules('upload'));
  private readonly titleRules = toSignal(inject(Validation).rules('title'));
  /** The server's file rule as hint text, e.g. "... at most 50 MB." */
  protected readonly fileHint = computed(
    () => this.uploadRules()?.['file']?.find((r) => r.constraint === 'FileSize')?.message,
  );
  protected readonly dragging = signal(false);
  protected readonly showHelp = signal(false);

  protected readonly pageSize = PAGE_SIZE;

  protected readonly filtered = computed(() => {
    const query = this.search().trim().toLowerCase();
    let list = this.docs();
    if (query)
      list = list.filter(
        (d) => d.title.toLowerCase().includes(query) || d.filename.toLowerCase().includes(query),
      );
    const key = this.sortKey();
    const direction = this.sortAsc() ? 1 : -1;
    return [...list].sort((a, b) => {
      const comparison =
        key === 'size'
          ? a.size - b.size
          : key === 'uploadedAt'
            ? Date.parse(a.uploadedAt) - Date.parse(b.uploadedAt)
            : a.title.localeCompare(b.title, undefined, { sensitivity: 'base' });
      return comparison * direction;
    });
  });
  protected readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.filtered().length / PAGE_SIZE)),
  );
  protected readonly pageIdx = computed(() => Math.min(this.page(), this.pageCount() - 1));
  protected readonly rows = computed(() => {
    const start = this.pageIdx() * PAGE_SIZE;
    return this.filtered().slice(start, start + PAGE_SIZE);
  });
  protected readonly pages = computed(() => Array.from({ length: this.pageCount() }, (_, i) => i));
  protected readonly rangeStart = computed(() =>
    this.filtered().length ? this.pageIdx() * PAGE_SIZE + 1 : 0,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.pageIdx() + 1) * PAGE_SIZE, this.filtered().length),
  );
  protected readonly allSelected = computed(
    () => this.rows().length > 0 && this.rows().every((d) => this.selected().has(d.id)),
  );
  protected readonly volumeMb = computed(() =>
    (this.docs().reduce((sum, d) => sum + d.size, 0) / 1048576).toFixed(1),
  );
  protected readonly canUpload = computed(
    () => !!this.file() && !!this.title().trim() && !this.uploading(),
  );

  constructor() {
    this.load();
    this.loadGroups();
  }

  protected load(): void {
    const groupId = this.group();
    const request =
      groupId === ALL
        ? this.api.list()
        : this.api.getGroup(groupId).pipe(map((group) => group.documents));
    this.loading.set(true);
    request.subscribe({
      next: (docs) => {
        this.docs.set(docs);
        this.error.set(null);
        this.loading.set(false);
        const ids = new Set(docs.map((d) => d.id));
        this.selected.update((selection) => new Set([...selection].filter((id) => ids.has(id))));
      },
      error: (e) => {
        this.error.set(`Could not load documents (${e?.status || 'network error'}).`);
        this.loading.set(false);
      },
    });
  }

  /** Fills the View Group dropdown; without groups it just offers All Documents. */
  private loadGroups(): void {
    this.api.listGroups().subscribe({
      next: (groups) => this.groups.set([...groups].sort(byName)),
      error: () => this.groups.set([]),
    });
  }

  protected setGroup(groupId: string): void {
    this.group.set(groupId);
    this.page.set(0);
    this.load();
  }

  protected setSearch(query: string): void {
    this.search.set(query);
    this.page.set(0);
  }

  protected sortBy(key: SortKey): void {
    if (this.sortKey() === key) this.sortAsc.update((ascending) => !ascending);
    else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected arrow(key: SortKey): string {
    return this.sortKey() === key ? (this.sortAsc() ? '▲' : '▼') : '';
  }

  protected toggle(id: string): void {
    this.selected.update((selection) => {
      const next = new Set(selection);
      if (!next.delete(id)) next.add(id);
      return next;
    });
  }

  protected toggleAll(): void {
    const all = this.allSelected();
    this.selected.update((selection) => {
      const next = new Set(selection);
      for (const doc of this.rows()) all ? next.delete(doc.id) : next.add(doc.id);
      return next;
    });
  }

  protected download(doc: Doc): void {
    this.api
      .download(doc)
      .subscribe({
        error: (e) => this.error.set(`Download failed (${e?.status || 'network error'}).`),
      });
  }

  protected downloadSelected(): void {
    for (const doc of this.docs().filter((d) => this.selected().has(d.id))) this.download(doc);
  }

  protected rename(doc: Doc): void {
    const newTitle = prompt('New title:', doc.title)?.trim();
    if (newTitle == null || newTitle === doc.title) return;
    const invalid = check(this.titleRules(), { title: newTitle })['title'];
    if (invalid) {
      alert(invalid);
      return;
    }
    this.api.rename(doc.id, newTitle).subscribe({
      next: () => this.load(),
      error: (e) => {
        const fields = serverErrors(e);
        if (fields) alert(fields['title'] ?? Object.values(fields)[0]);
        else this.error.set('Rename failed.');
      },
    });
  }

  protected remove(doc: Doc): void {
    if (!confirm(`Delete "${doc.title}"?`)) return;
    this.api.delete(doc.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed.'),
    });
  }

  // --- add to group ---

  /** The user's groups that contain the document, for the Groups column. */
  protected groupsOf(doc: Doc): Group[] {
    return this.groups().filter((group) => group.documents.some((d) => d.id === doc.id));
  }

  /** Groups offered in the dialog: ones without the document to add, ones with it to remove. */
  protected readonly dialogGroups = computed(() => {
    const doc = this.adding();
    if (!doc) return [];
    const isMember = (group: Group) => group.documents.some((d) => d.id === doc.id);
    return this.groups().filter((group) =>
      this.dialogMode() === 'add' ? !isMember(group) : isMember(group),
    );
  });

  protected openGroupDialog(doc: Doc, mode: 'add' | 'remove'): void {
    this.dialogMode.set(mode);
    this.adding.set(doc);
    // when viewing one group, removing defaults to that group
    const current = this.dialogGroups().find((group) => group.id === this.group());
    this.targetGroup.set(
      (mode === 'remove' && current ? current : this.dialogGroups()[0])?.id ?? '',
    );
    this.addMsg.set(null);
  }

  protected closeAddToGroup(): void {
    this.adding.set(null);
    this.addMsg.set(null);
  }

  protected submitGroupDialog(): void {
    const doc = this.adding();
    const groupId = this.targetGroup();
    if (!doc || !groupId) return;
    const adding = this.dialogMode() === 'add';
    this.addPending.set(true);
    (adding
      ? this.api.addToGroup(groupId, doc.id)
      : this.api.removeFromGroup(groupId, doc.id)
    ).subscribe({
      next: (updated) => {
        this.addPending.set(false);
        this.adding.set(null);
        this.addMsg.set({
          text: adding
            ? `"${doc.title}" added to group "${updated.name}".`
            : `"${doc.title}" removed from group "${updated.name}".`,
          error: false,
        });
        this.groups.update((list) =>
          list.map((group) => (group.id === updated.id ? updated : group)),
        );
        if (this.group() === updated.id) this.load();
      },
      error: (e) => {
        this.addPending.set(false);
        this.addMsg.set({
          text: `Could not ${adding ? 'add to' : 'remove from'} group (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }

  // --- upload ---

  protected pick(file: File | null | undefined): void {
    if (!file) return;
    this.uploadMsg.set(null);
    // check the file right away (empty, too large) instead of waiting for Upload File
    const errors = check(this.uploadRules(), { file }, ['file']);
    this.uploadErrors.set(errors);
    if (hasErrors(errors)) {
      this.file.set(null);
      return;
    }
    this.file.set(file);
    this.title.set(file.name.replace(/\.[^.]*$/, '') || file.name);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.pick(event.dataTransfer?.files?.[0]);
  }

  protected upload(): void {
    const file = this.file();
    const title = this.title().trim();
    const errors = check(this.uploadRules(), { title, file });
    this.uploadErrors.set(errors);
    if (hasErrors(errors) || !file) return;
    this.uploading.set(true);
    this.uploadMsg.set({ text: 'Uploading...', error: false });
    this.api.upload(title, file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.file.set(null);
        this.title.set('');
        this.uploadErrors.set({});
        this.uploadMsg.set({ text: 'Upload complete.', error: false });
        this.load();
      },
      error: (e) => {
        this.uploading.set(false);
        const fields = serverErrors(e);
        if (fields) this.uploadErrors.set(fields);
        this.uploadMsg.set(
          fields ? null : { text: `Upload failed (${e?.status || 'network error'}).`, error: true },
        );
      },
    });
  }

  // --- navigation ---

  protected goto(sectionId: string, focusId?: string): void {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (focusId) document.getElementById(focusId)?.focus();
  }

  // --- presentation helpers ---

  protected icon(type: DocumentType | null): { name: string; cls: string } {
    switch (type) {
      case 'PDF':
        return { name: 'picture_as_pdf', cls: 'text-red-700' };
      case 'WORD':
        return { name: 'description', cls: 'text-blue-800' };
      case 'EXCEL':
        return { name: 'table_chart', cls: 'text-emerald-700' };
      default:
        return { name: 'draft', cls: 'text-gray-600' };
    }
  }

  protected formatSize(bytes: number): string {
    return bytes < 1024 ? `${bytes} B` : `${Math.round(bytes / 1024).toLocaleString('en-US')} KB`;
  }
}
