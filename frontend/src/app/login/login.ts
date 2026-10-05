import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { Api } from '../api';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private api = inject(Api);
  private router = inject(Router);

  private userInput = viewChild.required<ElementRef<HTMLInputElement>>('userInput');

  protected username = signal('');
  protected password = signal('');
  protected pending = signal(false);
  protected error = signal('');
  protected showHelp = signal(false);

  ngOnInit(): void {
    if (this.api.session()) {
      this.router.navigateByUrl('/documents');
    }
  }

  protected logOn(): void {
    this.run(() => this.api.login(this.username(), this.password()));
  }

  protected register(): void {
    const [username, password] = [this.username(), this.password()];
    this.run(() =>
      this.api.register(username, password).pipe(switchMap(() => this.api.login(username, password))),
    );
  }

  protected cancel(): void {
    this.username.set('');
    this.password.set('');
    this.error.set('');
    this.userInput().nativeElement.focus();
  }

  private run(call: () => Observable<unknown>): void {
    if (this.pending()) return;
    this.pending.set(true);
    this.error.set('');
    call().subscribe({
      next: () => this.router.navigateByUrl('/documents'),
      error: (e: unknown) => {
        this.pending.set(false);
        this.error.set(this.message(e));
      },
      complete: () => this.pending.set(false),
    });
  }

  private message(e: unknown): string {
    const status = e instanceof HttpErrorResponse ? e.status : 0;
    switch (status) {
      case 400:
        return 'Password must be 8–128 characters.';
      case 401:
        return 'Invalid user name or password.';
      case 409:
        return 'User name already taken.';
      default:
        return 'Server unreachable.';
    }
  }
}
