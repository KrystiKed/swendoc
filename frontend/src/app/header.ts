import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Api } from './api';

/** The page header of every logged-in page; page-specific nav buttons (Upload, Help) are projected in. */
@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="bg-win-bg border-b-2 border-win-dark shadow-sm">
      <div
        class="bg-gradient-to-r from-win-navy to-win-navy-gradient text-white px-4 py-2 flex items-center justify-between"
      >
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 flex items-center justify-center shrink-0">
            <img alt="WinDoc logo" class="w-8 h-8" src="logo.svg" />
          </div>
          <div class="flex flex-col">
            <span class="font-bold text-sm tracking-wide">WinDoc Web DMS</span>
            <span class="text-[11px] text-gray-200">Enterprise Shared Document Repository</span>
          </div>
        </div>
        <div class="flex items-center gap-3 text-xs">
          <a
            routerLink="/profile"
            class="flex items-center gap-1.5 font-mono text-[11px] bg-black/25 px-2.5 py-1 border border-white/20 text-white no-underline hover:bg-black/40"
          >
            <span class="material-symbols-outlined text-[14px] text-teal-300">account_circle</span>
            <span
              >USER: <strong class="text-white">{{ api.session()?.username }}</strong></span
            >
          </a>
          <button
            type="button"
            class="text-gray-200 hover:text-white underline font-semibold text-[11px] cursor-pointer"
            (click)="api.logout()"
          >
            Log Out
          </button>
        </div>
      </div>
      <nav class="px-3 py-1 flex items-center gap-1 border-t border-win-light text-xs">
        @for (link of links; track link.path) {
          <a
            [routerLink]="link.path"
            routerLinkActive="font-semibold !text-black bg-[#e2ded6]"
            ariaCurrentWhenActive="page"
            class="win-btn px-3 py-1 text-gray-800 hover:text-black flex items-center gap-1.5 no-underline"
          >
            <span class="material-symbols-outlined text-[15px]" [class]="link.iconClass">{{
              link.icon
            }}</span
            ><span>{{ link.label }}</span>
          </a>
        }
        <ng-content />
      </nav>
    </header>
  `,
})
export class Header {
  protected readonly api = inject(Api);

  protected readonly links = [
    { path: '/documents', label: 'Documents', icon: 'folder_shared', iconClass: 'text-win-navy' },
    { path: '/groups', label: 'Groups', icon: 'category', iconClass: 'text-gray-700' },
    { path: '/profile', label: 'Profile', icon: 'badge', iconClass: 'text-win-teal' },
  ];
}
