import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay } from 'rxjs';

/**
 * Client-side validation for user experience only: the rules and their (localized) messages come
 * from GET /api/validation/{form}, i.e. from the Bean Validation annotations the server enforces anyway.
 * Nothing here is a second copy of a rule.
 */

/** Mirrors ValidationRulesController.Rule. */
export interface Rule {
  constraint: string;
  attributes: Record<string, unknown>;
  message: string;
}

export type FormRules = Record<string, Rule[]>;

/** Field name -> first message to show for it. */
export type FieldErrors = Record<string, string>;

export type Form = 'credentials' | 'upload' | 'title' | 'group' | 'password';

const num = (value: unknown, fallback: number) => (typeof value === 'number' ? value : fallback);
const length = (value: unknown) =>
  typeof value === 'string' || Array.isArray(value) ? value.length : 0;

/** One check per Bean Validation constraint name; a constraint missing here is left to the server. */
const CHECKS: Record<string, (value: unknown, a: Record<string, unknown>) => boolean> = {
  NotNull: (v) => v != null,
  NotEmpty: (v) => v != null && length(v) > 0,
  NotBlank: (v) => typeof v === 'string' && v.trim().length > 0,
  Size: (v, a) =>
    v == null || (length(v) >= num(a['min'], 0) && length(v) <= num(a['max'], Infinity)),
  Pattern: (v, a) => v == null || new RegExp(`^(?:${a['regexp']})$`).test(String(v)),
  FileSize: (v, a) =>
    !(v instanceof Blob) || (v.size > 0 && v.size <= num(a['maxMb'], Infinity) * 1024 * 1024),
};

/**
 * Checks values against rules (only the given fields, default all); the server lists "required"
 * rules first, so they win per field.
 */
export function check(
  rules: FormRules | undefined,
  values: Record<string, unknown>,
  fields?: string[],
): FieldErrors {
  const errors: FieldErrors = {};
  for (const [field, fieldRules] of Object.entries(rules ?? {})) {
    if (fields && !fields.includes(field)) continue;
    const failed = fieldRules.find(
      (r) => CHECKS[r.constraint] && !CHECKS[r.constraint](values[field], r.attributes),
    );
    if (failed) errors[field] = failed.message;
  }
  return errors;
}

/** The field errors of a 400 from the server ({"errors":[{field, message}]}), or null for any other error. */
export function serverErrors(error: unknown): FieldErrors | null {
  if (
    !(error instanceof HttpErrorResponse) ||
    error.status !== 400 ||
    !Array.isArray(error.error?.errors)
  )
    return null;
  const errors: FieldErrors = {};
  for (const { field, message } of error.error.errors as { field: string; message: string }[]) {
    errors[field] ??= message;
  }
  return errors;
}

export const hasErrors = (errors: FieldErrors) => Object.keys(errors).length > 0;

@Injectable({ providedIn: 'root' })
export class Validation {
  private http = inject(HttpClient);
  private cache = new Map<Form, Observable<FormRules>>();

  /** Rules in the browser's language (Accept-Language); empty if unreachable, the server still validates. */
  rules(form: Form): Observable<FormRules> {
    let rules = this.cache.get(form);
    if (!rules) {
      rules = this.http.get<FormRules>(`/api/validation/${form}`).pipe(
        catchError(() => of({})),
        shareReplay(1),
      );
      this.cache.set(form, rules);
    }
    return rules;
  }
}
