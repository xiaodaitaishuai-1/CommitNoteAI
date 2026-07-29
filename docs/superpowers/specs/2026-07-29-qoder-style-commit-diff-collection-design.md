# Qoder-Style Commit Diff Collection Design

## Goal

Replace CommitNoteAI's file-content comparison collector with a Qoder-aligned,
structured unified-diff collector. Generated commit messages must be based on
the changes selected in the IntelliJ commit workflow, including selected
unversioned files.

This work preserves the existing OpenAI-compatible request flow, generated
message preview, editable replacement, prompt preferences, and post-generation
fact checking.

## Reference Behavior

The supplied Qoder JetBrains plugin's commit-message action uses the following
observable collection behavior:

- Collect included versioned changes and included unversioned files.
- Represent each unversioned file as an added `Change`.
- Exclude binary files and single-line text files longer than 300 characters.
- Generate each text change with IntelliJ's `IdeaTextPatchBuilder` and write it
  as a unified diff.
- Accept at most 50 diff entries and 70,000 total diff characters.
- Stop collecting when either limit is reached.
- Preserve an empty-patch file-name entry.
- When patch construction fails for a new file, construct a unified added-file
  diff with `/dev/null`, `b/<path>`, a hunk header, and `+` content lines.
- Continue when an individual change cannot be read or converted to a patch.

The implementation will reproduce these behaviors with original Kotlin code.
It will not copy Qoder's proprietary implementation.

## Architecture

`CommitChangeCollector` becomes the sole producer of generation evidence.
It receives the selected `Change` values, includes selected unversioned files
where the commit workflow makes them available, filters ineligible inputs, and
uses platform patch APIs to produce unified diff text.

A new diff-oriented snapshot model carries:

- one unified diff or empty-patch description per accepted change;
- the collection limit state and skipped-item summary;
- enough path and change metadata for prompt rendering and deterministic tests.

The prompt builder renders the collected unified diffs and makes truncation
explicit. `ChangedSymbolExtractor`, `CommitChangeFactExtractor`, and
`CommitMessageFactChecker` consume the same diff evidence rather than
independent before/after file snippets. The preview dialog and commit-message
replacement flow remain unchanged.

## Collection Rules

For each selected change, the collector resolves the after revision first and
otherwise the before revision. It skips the change if the resolved file type is
binary or its readable content is one line longer than 300 characters. A read
failure does not fail the full collection.

Each eligible change is converted independently through
`IdeaTextPatchBuilder.buildPatch` and `UnifiedDiffWriter`. The collector adds
the written unified patch when nonblank. If the patch list is empty, it adds a
path-oriented empty-patch entry. For a new file whose patch generation throws,
it reads the after content and constructs a bounded added-file unified diff.

The collector allows up to 50 accepted entries and 70,000 accumulated patch
characters. It performs the capacity check before accepting a generated patch
and stops collecting when either limit is reached. A new-file fallback is also
bounded to the same 70,000-character maximum.

If no usable diff evidence remains after filtering and failures, the generation
action does not call the model and reports that no usable change is selected.

## Failure Handling

Individual VCS read, patch-build, and patch-write failures are isolated to the
affected change. The collector records a non-sensitive skip reason for prompt
and diagnostics but continues with remaining changes. Binary content is not
read or sent. Limits are reported as truncation, never as a claim that the
complete selected change set was analyzed.

## Tests

Unit tests cover:

- unified diff formatting for modified, added, deleted, and renamed files;
- multiple distant hunks retained as separate patch hunks;
- selected unversioned files represented as additions;
- binary and overlong single-line exclusion;
- 50-entry and 70,000-character collection limits;
- empty-patch and new-file fallback output;
- partial failure continuing with other changes;
- symbol and fact extraction from unified diffs;
- prompt rendering of diffs and explicit truncation;
- no-model path when no usable diff remains.

Existing response parsing, sanitization, fact-checking, preview state, and
settings tests remain in place.

## Non-Goals

- Changing model providers, API contracts, or generation output styles.
- Adding an unrelated Git command-line integration.
- Changing the commit preview or replacement interaction.
- Claiming that skipped or truncated files were analyzed.
