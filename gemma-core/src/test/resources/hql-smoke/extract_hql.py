#!/usr/bin/env python3
"""
Walk gemma-core/src/main/java, extract every static HQL string passed to
session.createQuery(...) calls, write to hql-manifest.json next to this
script.

The output is consumed by HqlSmokeIT — a parameterized parse-only test that
asserts each HQL string is accepted by the HB6 SQM translator against an
empty H2 schema.

Extraction rules:
- Match `createQuery(` followed by a sequence of string-literal concatenations
  (`"..." + "..." + ...`).
- Reject the entry if any non-literal token appears in the argument
  expression (variable concatenation = dynamic HQL, not safely extractable
  here). These are reported but skipped.
- Capture file:line so the test failure points back to the source.

Re-run when HQL changes:
    python3 gemma-core/src/test/resources/hql-smoke/extract_hql.py
"""

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[5]
SRC = REPO_ROOT / "gemma-core/src/main/java"
MANIFEST = Path(__file__).resolve().parent / "hql-manifest.json"

# Match `createQuery(` (possibly preceded by chained calls) and capture the
# argument list up to the matching closing paren. We handle string-literal
# concatenations only; if a non-literal token appears we skip.
CREATE_QUERY_RE = re.compile(r'\bcreateQuery\s*\(', re.MULTILINE)

# Match a Java string literal: opening quote, optional escapes, closing quote.
STRING_LIT_RE = re.compile(r'"((?:\\.|[^"\\])*)"')

# Match the parameter binding pattern: ".setParameter*"; we don't fully parse
# but we do collect which `:name` placeholders the query needs.
PARAM_PLACEHOLDER_RE = re.compile(r':([A-Za-z_][A-Za-z0-9_]*)')

def parse_string_concat(src, start):
    """
    Starting at offset `start` inside `src`, parse a sequence of
    string literals joined by `+`, returning (combined_string, end_offset)
    or None if a non-literal token is encountered.
    """
    pos = start
    parts = []
    while pos < len(src):
        # Skip whitespace + comments
        while pos < len(src) and src[pos] in ' \t\r\n':
            pos += 1
        # Skip // comments
        if pos + 1 < len(src) and src[pos:pos+2] == '//':
            eol = src.find('\n', pos)
            pos = eol + 1 if eol != -1 else len(src)
            continue
        # Skip /* */ comments
        if pos + 1 < len(src) and src[pos:pos+2] == '/*':
            end = src.find('*/', pos + 2)
            pos = end + 2 if end != -1 else len(src)
            continue
        if pos >= len(src):
            break
        c = src[pos]
        if c == '"':
            m = STRING_LIT_RE.match(src, pos)
            if not m:
                return None
            # Unescape Java string literal (just the common cases)
            literal = m.group(1)
            literal = literal.replace('\\"', '"').replace('\\\\', '\\').replace('\\n', '\n').replace('\\t', '\t')
            parts.append(literal)
            pos = m.end()
            continue
        if c == '+':
            pos += 1
            continue
        # A `,` or `)` ends the argument list (createQuery(hql, Class) form
        # accepts a second arg; we'll let the caller stop at the closing
        # comma for the literal-only first arg).
        if c in ',)':
            if parts:
                return (''.join(parts), pos)
            return None
        # Any other character → non-literal token → bail
        return None
    return None


def main():
    entries = []
    skipped = []
    for fp in sorted(SRC.rglob("*.java")):
        rel = fp.relative_to(REPO_ROOT).as_posix()
        text = fp.read_text()
        for m in CREATE_QUERY_RE.finditer(text):
            arg_start = m.end()
            parsed = parse_string_concat(text, arg_start)
            line = text.count('\n', 0, m.start()) + 1
            if parsed is None:
                skipped.append({"file": rel, "line": line, "reason": "non-literal argument"})
                continue
            hql_str, _ = parsed
            placeholders = sorted(set(PARAM_PLACEHOLDER_RE.findall(hql_str)))
            # The `:` prefix on `(:ee)` or `(:ee_)` can over-capture from
            # parameter-list syntax. Keep distinct placeholder names only.
            entries.append({
                "file": rel,
                "line": line,
                "hql": hql_str,
                "params": placeholders,
            })

    # De-dup identical (hql, params) tuples — same query appearing in two
    # files is rare but we don't want to double-test.
    seen = set()
    unique = []
    for e in entries:
        key = (e["hql"], tuple(e["params"]))
        if key in seen:
            continue
        seen.add(key)
        unique.append(e)

    out = {
        "extracted_at": "2026-05-25",
        "extractor_version": 1,
        "total_callsites": len(entries),
        "unique_queries": len(unique),
        "skipped_dynamic": len(skipped),
        "queries": unique,
        "skipped_summary": skipped[:50],  # first 50 only; full list noisy
    }
    MANIFEST.write_text(json.dumps(out, indent=2))
    print(f"wrote {MANIFEST} — {len(unique)} unique queries, {len(skipped)} dynamic-skipped")

if __name__ == "__main__":
    sys.exit(main() or 0)
