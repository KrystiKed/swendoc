import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { Api, Doc, DocumentType, Group } from '../api';
import { FieldErrors, Validation, check, hasErrors, serverErrors } from '../validation';

/** 'ALL' shows every document, otherwise the id of one of the user's groups. */
const ALL = 'ALL';
type SortKey = 'title' | 'size' | 'uploadedAt';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-documents',
  imports: [DatePipe, RouterLink],
  templateUrl: './documents.html',
})
export class Documents {
  protected readonly api = inject(Api);
  private router = inject(Router);

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
  protected readonly fileHint = computed(() => this.uploadRules()?.['file']?.find(r => r.constraint === 'FileSize')?.message);
  protected readonly dragging = signal(false);
  protected readonly showHelp = signal(false);
  protected readonly now = signal(new Date());

  protected readonly pageSize = PAGE_SIZE;

  protected readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    let list = this.docs();
    if (q) list = list.filter(d => d.title.toLowerCase().includes(q) || d.filename.toLowerCase().includes(q));
    const key = this.sortKey();
    const dir = this.sortAsc() ? 1 : -1;
    return [...list].sort((a, b) => {
      const c =
        key === 'size'
          ? a.size - b.size
          : key === 'uploadedAt'
            ? Date.parse(a.uploadedAt) - Date.parse(b.uploadedAt)
            : a.title.localeCompare(b.title, undefined, { sensitivity: 'base' });
      return c * dir;
    });
  });
  protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE_SIZE)));
  protected readonly pageIdx = computed(() => Math.min(this.page(), this.pageCount() - 1));
  protected readonly rows = computed(() => {
    const s = this.pageIdx() * PAGE_SIZE;
    return this.filtered().slice(s, s + PAGE_SIZE);
  });
  protected readonly pages = computed(() => Array.from({ length: this.pageCount() }, (_, i) => i));
  protected readonly rangeStart = computed(() => (this.filtered().length ? this.pageIdx() * PAGE_SIZE + 1 : 0));
  protected readonly rangeEnd = computed(() => Math.min((this.pageIdx() + 1) * PAGE_SIZE, this.filtered().length));
  protected readonly allSelected = computed(
    () => this.rows().length > 0 && this.rows().every(d => this.selected().has(d.id)),
  );
  protected readonly volumeMb = computed(() => (this.docs().reduce((s, d) => s + d.size, 0) / 1048576).toFixed(1));
  protected readonly canUpload = computed(() => !!this.file() && !!this.title().trim() && !this.uploading());

  constructor() {
    this.load();
    this.loadGroups();
    const timer = setInterval(() => this.now.set(new Date()), 30000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected load(): void {
    const g = this.group();
    const req = g === ALL ? this.api.list() : this.api.getGroup(g).pipe(map(group => group.documents));
    this.loading.set(true);
    req.subscribe({
      next: d => {
        this.docs.set(d);
        this.error.set(null);
        this.loading.set(false);
        const ids = new Set(d.map(x => x.id));
        this.selected.update(s => new Set([...s].filter(id => ids.has(id))));
      },
      error: e => {
        this.error.set(`Could not load documents (${e?.status || 'network error'}).`);
        this.loading.set(false);
      },
    });
  }

  /** Fills the View Group dropdown; without groups it just offers All Documents. */
  private loadGroups(): void {
    this.api.listGroups().subscribe({
      next: g => this.groups.set([...g].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))),
      error: () => this.groups.set([]),
    });
  }

  protected setGroup(v: string): void {
    this.group.set(v);
    this.page.set(0);
    this.load();
  }

  protected setSearch(v: string): void {
    this.search.set(v);
    this.page.set(0);
  }

  protected sortBy(key: SortKey): void {
    if (this.sortKey() === key) this.sortAsc.update(a => !a);
    else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected arrow(key: SortKey): string {
    return this.sortKey() === key ? (this.sortAsc() ? '▲' : '▼') : '';
  }

  protected toggle(id: string): void {
    this.selected.update(s => {
      const n = new Set(s);
      if (!n.delete(id)) n.add(id);
      return n;
    });
  }

  protected toggleAll(): void {
    const all = this.allSelected();
    this.selected.update(s => {
      const n = new Set(s);
      for (const d of this.rows()) all ? n.delete(d.id) : n.add(d.id);
      return n;
    });
  }

  protected downloadSelected(): void {
    for (const d of this.docs().filter(x => this.selected().has(x.id))) {
      const a = document.createElement('a');
      a.href = this.api.contentUrl(d.id);
      a.download = d.filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
    }
  }

  protected rename(d: Doc): void {
    const t = prompt('New title:', d.title)?.trim();
    if (t == null || t === d.title) return;
    const invalid = check(this.titleRules(), { title: t })['title'];
    if (invalid) {
      alert(invalid);
      return;
    }
    this.api.rename(d.id, t).subscribe({
      next: () => this.load(),
      error: e => {
        const fields = serverErrors(e);
        if (fields) alert(fields['title'] ?? Object.values(fields)[0]);
        else this.error.set('Rename failed.');
      },
    });
  }

  protected remove(d: Doc): void {
    if (!confirm(`Delete "${d.title}"?`)) return;
    this.api.delete(d.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed.'),
    });
  }

  // --- add to group ---

  /** The user's groups that contain the document, for the Groups column. */
  protected groupsOf(d: Doc): Group[] {
    return this.groups().filter(g => g.documents.some(x => x.id === d.id));
  }

  /** Groups offered in the dialog: ones without the document to add, ones with it to remove. */
  protected readonly dialogGroups = computed(() => {
    const d = this.adding();
    if (!d) return [];
    const member = (g: Group) => g.documents.some(x => x.id === d.id);
    return this.groups().filter(g => (this.dialogMode() === 'add' ? !member(g) : member(g)));
  });

  protected openGroupDialog(d: Doc, mode: 'add' | 'remove'): void {
    this.dialogMode.set(mode);
    this.adding.set(d);
    // when viewing one group, removing defaults to that group
    const current = this.dialogGroups().find(g => g.id === this.group());
    this.targetGroup.set((mode === 'remove' && current ? current : this.dialogGroups()[0])?.id ?? '');
    this.addMsg.set(null);
  }

  protected closeAddToGroup(): void {
    this.adding.set(null);
    this.addMsg.set(null);
  }

  protected submitGroupDialog(): void {
    const d = this.adding();
    const groupId = this.targetGroup();
    if (!d || !groupId) return;
    const adding = this.dialogMode() === 'add';
    this.addPending.set(true);
    (adding ? this.api.addToGroup(groupId, d.id) : this.api.removeFromGroup(groupId, d.id)).subscribe({
      next: g => {
        this.addPending.set(false);
        this.adding.set(null);
        this.addMsg.set({
          text: adding ? `"${d.title}" added to group "${g.name}".` : `"${d.title}" removed from group "${g.name}".`,
          error: false,
        });
        this.groups.update(list => list.map(x => (x.id === g.id ? g : x)));
        if (this.group() === g.id) this.load();
      },
      error: e => {
        this.addPending.set(false);
        this.addMsg.set({
          text:
            adding && e?.status === 404
              ? 'Only documents you uploaded while logged in can be added to your groups.'
              : `Could not ${adding ? 'add to' : 'remove from'} group (${e?.status || 'network error'}).`,
          error: true,
        });
      },
    });
  }

  // --- upload ---

  protected pick(f: File | null | undefined): void {
    if (!f) return;
    this.uploadMsg.set(null);
    // check the file right away (empty, too large) instead of waiting for Upload File
    const errors = check(this.uploadRules(), { file: f }, ['file']);
    this.uploadErrors.set(errors);
    if (hasErrors(errors)) {
      this.file.set(null);
      return;
    }
    this.file.set(f);
    this.title.set(f.name.replace(/\.[^.]*$/, '') || f.name);
  }

  protected onDrop(e: DragEvent): void {
    e.preventDefault();
    this.dragging.set(false);
    this.pick(e.dataTransfer?.files?.[0]);
  }

  protected upload(): void {
    const f = this.file();
    const t = this.title().trim();
    const errors = check(this.uploadRules(), { title: t, file: f });
    this.uploadErrors.set(errors);
    if (hasErrors(errors) || !f) return;
    this.uploading.set(true);
    this.uploadMsg.set({ text: 'Uploading...', error: false });
    this.api.upload(t, f).subscribe({
      next: () => {
        this.uploading.set(false);
        this.file.set(null);
        this.title.set('');
        this.uploadErrors.set({});
        this.uploadMsg.set({ text: 'Upload complete.', error: false });
        this.load();
      },
      error: e => {
        this.uploading.set(false);
        const fields = serverErrors(e);
        if (fields) this.uploadErrors.set(fields);
        this.uploadMsg.set(fields ? null : { text: `Upload failed (${e?.status || 'network error'}).`, error: true });
      },
    });
  }

  // --- navigation ---

  protected goto(id: string, focusId?: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (focusId) document.getElementById(focusId)?.focus();
  }

  protected logout(): void {
    this.api.logout();
    this.router.navigateByUrl('/login');
  }

  // --- presentation helpers ---

  protected icon(t: DocumentType | null): { name: string; cls: string } {
    switch (t) {
      case 'PDF': return { name: 'picture_as_pdf', cls: 'text-red-700' };
      case 'WORD': return { name: 'description', cls: 'text-blue-800' };
      case 'EXCEL': return { name: 'table_chart', cls: 'text-emerald-700' };
      default: return { name: 'draft', cls: 'text-gray-600' };
    }
  }


  protected formatSize(n: number): string {
    return n < 1024 ? `${n} B` : `${Math.round(n / 1024).toLocaleString('en-US')} KB`;
  }
}
