import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { Api } from '../api';
import { FieldErrors, Validation, check, hasErrors, serverErrors } from '../validation';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private api = inject(Api);
  private router = inject(Router);
  private rules = toSignal(inject(Validation).rules('credentials'));

  private userInput = viewChild.required<ElementRef<HTMLInputElement>>('userInput');

  protected username = signal('');
  protected password = signal('');
  protected pending = signal(false);
  protected error = signal('');
  protected fieldErrors = signal<FieldErrors>({});
  protected showHelp = signal(false);

  /** The server's password length rule, e.g. "Must be between 8 and 128 characters long." */
  protected passwordHint = computed(() => this.rules()?.['password']?.find(r => r.constraint === 'Size')?.message);

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
    this.fieldErrors.set({});
    this.userInput().nativeElement.focus();
  }

  private run(call: () => Observable<unknown>): void {
    if (this.pending()) return;
    this.error.set('');
    const errors = check(this.rules(), { username: this.username(), password: this.password() });
    this.fieldErrors.set(errors);
    if (hasErrors(errors)) return;
    this.pending.set(true);
    call().subscribe({
      next: () => this.router.navigateByUrl('/documents'),
      error: (e: unknown) => {
        this.pending.set(false);
        const fields = serverErrors(e);
        if (fields) this.fieldErrors.set(fields);
        else this.error.set(this.message(e));
      },
      complete: () => this.pending.set(false),
    });
  }

  private message(e: unknown): string {
    const status = e instanceof HttpErrorResponse ? e.status : 0;
    switch (status) {
      case 401:
        return 'Invalid user name or password.';
      case 409:
        return 'User name already taken.';
      default:
        return 'Server unreachable.';
    }
  }
}
