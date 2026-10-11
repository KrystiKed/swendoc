import { inject } from '@angular/core';
import { Router, Routes } from '@angular/router';
import { Api } from './api';
import { Login } from './login/login';
import { Documents } from './documents/documents';
import { Groups } from './groups/groups';
import { DocumentDetail } from './document-detail/document-detail';
import { Profile } from './profile/profile';

// ponytail: client-side guard only; /docs has no server-side auth yet
const loggedIn = () => !!inject(Api).session() || inject(Router).parseUrl('/login');

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'documents', component: Documents, canActivate: [loggedIn] },
  { path: 'documents/:id', component: DocumentDetail, canActivate: [loggedIn] },
  { path: 'profile', component: Profile, canActivate: [loggedIn] },
  // not "groups": that path is the backend API and the dev proxy / nginx forward it
  { path: 'my-groups', component: Groups, canActivate: [loggedIn] },
  { path: '**', redirectTo: 'documents' },
];
