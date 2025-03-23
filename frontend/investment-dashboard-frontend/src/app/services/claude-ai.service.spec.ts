import { TestBed } from '@angular/core/testing';

import { ClaudeAiService } from './claude-ai.service';

describe('ClaudeAiService', () => {
  let service: ClaudeAiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClaudeAiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
