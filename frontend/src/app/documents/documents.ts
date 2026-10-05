import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api, Doc, DocumentType } from '../api';

type Group = 'ALL' | DocumentType | 'OTHER';
type SortKey = 'title' | 'size' | 'uploadedAt';

const PAGE_SIZE = 10;
const MAX_BYTES = 50 * 1024 * 1024;

@Component({
  selector: 'app-documents',
  imports: [DatePipe],
  templateUrl: './documents.html',
})
export class Documents {
  protected readonly api = inject(Api);
  private router = inject(Router);

  protected readonly docs = signal<Doc[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly group = signal<Group>('ALL');
  protected readonly sortKey = signal<SortKey>('uploadedAt');
  protected readonly sortAsc = signal(false);
  protected readonly page = signal(0);
  protected readonly selected = signal<ReadonlySet<string>>(new Set());
  protected readonly title = signal('');
  protected readonly file = signal<File | null>(null);
  protected readonly uploading = signal(false);
  protected readonly uploadMsg = signal<{ text: string; error: boolean } | null>(null);
  protected readonly dragging = signal(false);
  protected readonly showHelp = signal(false);
  protected readonly now = signal(new Date());

  protected readonly pageSize = PAGE_SIZE;

  protected readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    let list = this.docs();
    if (this.group() === 'OTHER') list = list.filter(d => d.documentType === null);
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
    const timer = setInterval(() => this.now.set(new Date()), 30000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected load(): void {
    const g = this.group();
    const req = g === 'WORD' || g === 'PDF' || g === 'EXCEL' ? this.api.listByType(g) : this.api.list();
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

  protected setGroup(v: string): void {
    this.group.set(v as Group);
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
    if (!t || t === d.title) return;
    this.api.rename(d.id, t).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Rename failed.'),
    });
  }

  protected remove(d: Doc): void {
    if (!confirm(`Delete "${d.title}"?`)) return;
    this.api.delete(d.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed.'),
    });
  }

  // --- upload ---

  protected pick(f: File | null | undefined): void {
    if (!f) return;
    if (f.size > MAX_BYTES) {
      this.file.set(null);
      this.uploadMsg.set({ text: `"${f.name}" exceeds the 50MB limit.`, error: true });
      return;
    }
    this.file.set(f);
    this.uploadMsg.set(null);
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
    if (!f || !t) return;
    this.uploading.set(true);
    this.uploadMsg.set({ text: 'Uploading...', error: false });
    this.api.upload(t, f).subscribe({
      next: () => {
        this.uploading.set(false);
        this.file.set(null);
        this.title.set('');
        this.uploadMsg.set({ text: 'Upload complete.', error: false });
        this.load();
      },
      error: e => {
        this.uploading.set(false);
        this.uploadMsg.set({ text: `Upload failed (${e?.status || 'network error'}).`, error: true });
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

  protected groupLabel(t: DocumentType | null): string {
    return t === 'PDF' ? 'Adobe PDF' : t === 'WORD' ? 'Word Document' : t === 'EXCEL' ? 'Excel Workbook' : 'Other';
  }

  protected formatSize(n: number): string {
    return n < 1024 ? `${n} B` : `${Math.round(n / 1024).toLocaleString('en-US')} KB`;
  }
}
