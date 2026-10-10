import { HttpErrorResponse } from '@angular/common/http';
import { FormRules, check, serverErrors } from './validation';

const rules: FormRules = {
  title: [
    { constraint: 'NotBlank', attributes: {}, message: 'required' },
    { constraint: 'Size', attributes: { max: 5 }, message: 'too long' },
  ],
  file: [
    { constraint: 'NotNull', attributes: {}, message: 'choose a file' },
    { constraint: 'FileSize', attributes: { maxMb: 1 }, message: 'bad size' },
  ],
  other: [{ constraint: 'SomethingServerOnly', attributes: {}, message: 'never shown' }],
};

describe('validation', () => {
  it('reports the first failing rule per field and skips unknown constraints', () => {
    expect(check(rules, { title: '   ', file: null })).toEqual({ title: 'required', file: 'choose a file' });
    expect(check(rules, { title: 'abcdef', file: new Blob([]) })).toEqual({ title: 'too long', file: 'bad size' });
    expect(check(rules, { title: 'ok', file: new Blob(['x']) })).toEqual({});
  });

  it('can limit the check to some fields', () => {
    expect(check(rules, { file: new Blob(['x']) }, ['file'])).toEqual({});
  });

  it('reads field errors from a server 400 and ignores other errors', () => {
    const body = { errors: [{ field: 'title', constraint: 'Size', message: 'zu lang' }] };
    expect(serverErrors(new HttpErrorResponse({ status: 400, error: body }))).toEqual({ title: 'zu lang' });
    expect(serverErrors(new HttpErrorResponse({ status: 500, error: body }))).toBeNull();
  });
});
