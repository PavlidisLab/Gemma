#!/usr/bin/env python3
"""
Project the legacy gsec ACL store onto phase2's canonical Spring Security ACL store.

WHY THIS EXISTS
---------------
During dual-version operation the deployed 1.32.x and phase2 share one database, but
each maintains its own ACL tables:

    1.32.x (authoritative)  ACLOBJECTIDENTITY / ACLENTRY / ACLSID
    phase2  (derived)       acl_object_identity / acl_entry / acl_sid / acl_class

1.32.x has no mapping for the canonical tables, so every entity it creates, deletes or
re-permissions leaves the canonical store stale. The canonical store was populated once
by the schema migration (~2026-05-15) and received nothing afterwards.

`lintAcls` is NOT a substitute. It enforces *existence* and *default* ACEs
(GROUP_ADMIN -> ADMINISTRATION, GROUP_AGENT -> READ) and grants anonymous READ only for
the classes in AclLinterServiceImpl.shouldBePublic = {ExternalDatabase, Protocol}. It
therefore cannot replicate per-dataset public/private status or user-specific grants.
As of 2026-08-08, 186 experiments were public on prod but private in canonical, which no
amount of linting would fix.

Direction is one-way by design: legacy -> canonical. Prod is authoritative for the whole
dual-version window. This script never writes to the legacy store and never touches any
entity table.

RETIREMENT
----------
Delete this script at cutover, once 1.32.x is no longer running against this database.
Run it one final time as part of the cutover, or ~186+ currently-public datasets
disappear from the public site on day one.

CREDENTIALS
-----------
Resolved through MySQL's own login-path store (~/.mylogin.cnf) -- never a .env file,
never a flag. Create one with:

    mysql_config_editor set --login-path=gemd-ro --host=... --user=... --password

Use a read-only login-path for dry runs. --apply refuses to run under a login-path whose
name ends in '-ro'.

USAGE
-----
    scripts/acl_sync.py                              # dry run, all classes
    scripts/acl_sync.py --class ...ExpressionExperiment
    scripts/acl_sync.py --login-path gemd-rw --apply
    scripts/acl_sync.py --apply --skip-aces          # identities/parents only, fast
"""

import argparse
import subprocess
import sys
import time

CANONICAL_TABLES = ("acl_class", "acl_sid", "acl_object_identity", "acl_entry")

# Legacy ACLSID is a single-table hierarchy: class='PrincipalSid' carries PRINCIPAL,
# class='GrantedAuthoritySid' carries GRANTED_AUTHORITY. Canonical acl_sid flattens that
# into (principal BIT, sid VARCHAR). This expression is the mapping, reused everywhere.
LEGACY_SID_NAME = "COALESCE(ls.PRINCIPAL, ls.GRANTED_AUTHORITY)"
LEGACY_SID_IS_PRINCIPAL = "(ls.class = 'PrincipalSid')"


def run_sql(login_path, database, sql, tabular=False):
    """Execute SQL through the mysql client. Returns stdout (TSV unless tabular)."""
    cmd = ["mysql", f"--login-path={login_path}", "-A", database, "-e", sql]
    if tabular:
        cmd.insert(-2, "-t")
    else:
        cmd[-3:-3] = ["-N", "-B"]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        sys.exit(f"SQL failed:\n{proc.stderr.strip()}\n--- statement ---\n{sql}")
    return proc.stdout


def scalar(login_path, database, sql):
    out = run_sql(login_path, database, sql).strip()
    return int(out.split("\n")[0]) if out else 0


def class_filter(alias, klass):
    """Restrict a legacy-side query to one class, if requested."""
    return f" AND {alias}.OBJECT_CLASS = '{klass}'" if klass else ""


# --------------------------------------------------------------------------------------
# Step definitions. Each has a COUNT form (dry run) and a mutation form (--apply).
# Order matters: classes -> sids -> identities -> parents -> ACEs. Identities are inserted
# with parent_object NULL first because a parent's canonical row may not exist yet; the
# parent pass then resolves every edge by (class, entity id) rather than by legacy row id.
# --------------------------------------------------------------------------------------

def step_classes(klass):
    where = f"WHERE NOT EXISTS (SELECT 1 FROM acl_class c WHERE c.class = o.OBJECT_CLASS)" \
            + (f" AND o.OBJECT_CLASS = '{klass}'" if klass else "")
    count = f"SELECT COUNT(*) FROM (SELECT DISTINCT o.OBJECT_CLASS FROM ACLOBJECTIDENTITY o {where}) t"
    # ORDER BY: binlog_format=STATEMENT on prod. INSERT..SELECT into an AUTO_INCREMENT table
    # must have a deterministic row order or a replica can assign ids differently.
    apply_ = f"INSERT INTO acl_class (class) SELECT DISTINCT o.OBJECT_CLASS FROM ACLOBJECTIDENTITY o {where} ORDER BY o.OBJECT_CLASS"
    return "acl_class rows to create", count, apply_


def step_sids(_klass):
    # SIDs are global, never class-scoped.
    missing = f"""
        FROM ACLSID ls
        WHERE NOT EXISTS (
            SELECT 1 FROM acl_sid k
             WHERE k.sid = {LEGACY_SID_NAME}
               AND k.principal = {LEGACY_SID_IS_PRINCIPAL})
    """
    count = f"SELECT COUNT(*) FROM (SELECT DISTINCT {LEGACY_SID_NAME} nm, {LEGACY_SID_IS_PRINCIPAL} pr {missing}) t"
    apply_ = f"INSERT INTO acl_sid (principal, sid) SELECT DISTINCT {LEGACY_SID_IS_PRINCIPAL}, {LEGACY_SID_NAME} {missing} ORDER BY 2, 1"
    return "acl_sid rows to create", count, apply_


def step_identities(klass):
    body = f"""
        FROM ACLOBJECTIDENTITY o
        JOIN acl_class c ON c.class = o.OBJECT_CLASS
        WHERE NOT EXISTS (
            SELECT 1 FROM acl_object_identity oi
             WHERE oi.object_id_class = c.id
               AND oi.object_id_identity = o.OBJECT_ID)
        {class_filter('o', klass)}
    """
    count = f"SELECT COUNT(*) {body}"
    # owner_sid is resolved through the legacy owner; entries_inheriting is copied verbatim
    # so the canonical row reproduces prod's inheritance flag rather than a default.
    apply_ = f"""
        INSERT INTO acl_object_identity
               (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
        SELECT c.id, o.OBJECT_ID, NULL,
               (SELECT k.id FROM acl_sid k JOIN ACLSID ls ON ls.ID = o.OWNER_SID_FK
                 WHERE k.sid = {LEGACY_SID_NAME} AND k.principal = {LEGACY_SID_IS_PRINCIPAL} LIMIT 1),
               o.ENTRIES_INHERITING
        {body}
        ORDER BY c.id, o.OBJECT_ID
    """
    return "acl_object_identity rows to create", count, apply_


def step_parents(klass):
    # Resolve parent edges by (class, entity id) on both sides. Also repairs edges that
    # drifted, not just NULLs.
    joins = f"""
        FROM acl_object_identity oi
        JOIN acl_class c   ON c.id = oi.object_id_class
        JOIN ACLOBJECTIDENTITY o  ON o.OBJECT_CLASS = c.class AND o.OBJECT_ID = oi.object_id_identity
        JOIN ACLOBJECTIDENTITY po ON po.ID = o.PARENT_OBJECT_FK
        JOIN acl_class pc  ON pc.class = po.OBJECT_CLASS
        JOIN acl_object_identity poi ON poi.object_id_class = pc.id
                                    AND poi.object_id_identity = po.OBJECT_ID
        WHERE o.PARENT_OBJECT_FK IS NOT NULL
          AND (oi.parent_object IS NULL OR oi.parent_object <> poi.id)
        {class_filter('o', klass)}
    """
    count = f"SELECT COUNT(*) {joins}"
    apply_ = f"UPDATE acl_object_identity oi JOIN acl_class c ON c.id = oi.object_id_class " \
             f"JOIN ACLOBJECTIDENTITY o ON o.OBJECT_CLASS = c.class AND o.OBJECT_ID = oi.object_id_identity " \
             f"JOIN ACLOBJECTIDENTITY po ON po.ID = o.PARENT_OBJECT_FK " \
             f"JOIN acl_class pc ON pc.class = po.OBJECT_CLASS " \
             f"JOIN acl_object_identity poi ON poi.object_id_class = pc.id AND poi.object_id_identity = po.OBJECT_ID " \
             f"SET oi.parent_object = poi.id " \
             f"WHERE o.PARENT_OBJECT_FK IS NOT NULL AND (oi.parent_object IS NULL OR oi.parent_object <> poi.id)" \
             + class_filter('o', klass)
    return "parent edges to set/repair", count, apply_


# ACE reconciliation ---------------------------------------------------------------------
# An identity's ACEs are replaced wholesale rather than diffed row by row, because
# ace_order has a UNIQUE(acl_object_identity, ace_order) constraint and must stay a
# contiguous 0-based sequence. Partial inserts would collide or leave gaps.
#
# Legacy ACEs with a NULL OBJECTIDENTITY_FK are skipped: there were 3,134,396 such rows on
# 2026-08-08, detached from any identity (the FK is nullable). They are inert.

def assert_no_deny_aces(login_path, database):
    """
    ACEs are renumbered on sync, so relative order is not preserved. That is safe only
    while every ACE is a grant: DefaultPermissionGrantingStrategy returns on first match,
    and with no denies the match outcome is order-independent. Both stores held zero deny
    ACEs on 2026-08-08. If one ever appears, renumbering could flip an authorization
    decision, so refuse to run rather than guess.
    """
    n = scalar(login_path, database,
               "SELECT (SELECT COUNT(*) FROM ACLENTRY WHERE GRANTING = 0)"
               " + (SELECT COUNT(*) FROM acl_entry WHERE granting = 0)")
    if n:
        sys.exit(f"aborting: {n} deny ACE(s) present. This tool renumbers ace_order, which is "
                 f"only order-safe when every ACE is a grant. Reconcile denies by hand.")


def _differing_identities_sql(klass):
    """
    Identities whose canonical ACE *set* differs from legacy, compared as distinct
    (sid, principal, mask, granting) tuples in both directions.

    Deliberately set-based, not count-based: legacy carries duplicate ACE rows on ~104
    identities (e.g. EE 1 has IS_AUTHENTICATED_ANONYMOUSLY twice, both at ACE_ORDER 2)
    which the original migration correctly collapsed. A count comparison would flag every
    one of those as drift forever, and copying them back would violate canonical's
    UNIQUE(acl_object_identity, ace_order).
    """
    return f"""
        SELECT oi.id AS canon_id
          FROM acl_object_identity oi
          JOIN acl_class c ON c.id = oi.object_id_class
          JOIN ACLOBJECTIDENTITY o ON o.OBJECT_CLASS = c.class AND o.OBJECT_ID = oi.object_id_identity
         WHERE 1=1 {class_filter('o', klass)}
           AND (
             EXISTS (
               SELECT 1 FROM ACLENTRY le JOIN ACLSID ls ON ls.ID = le.SID_FK
                WHERE le.OBJECTIDENTITY_FK = o.ID
                  AND NOT EXISTS (
                    SELECT 1 FROM acl_entry ke JOIN acl_sid ks ON ks.id = ke.sid
                     WHERE ke.acl_object_identity = oi.id
                       AND ks.sid = {LEGACY_SID_NAME}
                       AND ks.principal = {LEGACY_SID_IS_PRINCIPAL}
                       AND ke.mask = le.MASK
                       AND ke.granting = le.GRANTING))
             OR EXISTS (
               SELECT 1 FROM acl_entry ke JOIN acl_sid ks ON ks.id = ke.sid
                WHERE ke.acl_object_identity = oi.id
                  AND NOT EXISTS (
                    SELECT 1 FROM ACLENTRY le2 JOIN ACLSID ls2 ON ls2.ID = le2.SID_FK
                     WHERE le2.OBJECTIDENTITY_FK = o.ID
                       AND COALESCE(ls2.PRINCIPAL, ls2.GRANTED_AUTHORITY) = ks.sid
                       AND (ls2.class = 'PrincipalSid') = ks.principal
                       AND le2.MASK = ke.mask
                       AND le2.GRANTING = ke.granting))
           )
    """


def sync_aces(login_path, database, klass, apply_):
    """
    Reconcile ACEs for identities whose set differs, by replacing them wholesale.

    Wholesale replacement rather than row-level patching, because ace_order is subject to
    UNIQUE(acl_object_identity, ace_order) and must stay a contiguous 0-based run;
    incremental inserts would collide or leave gaps. The replacement set is deduplicated
    and renumbered, so legacy's duplicate rows are dropped rather than propagated.

    ace_order is derived with a correlated COUNT rather than a window function (MySQL 5.7)
    or a session variable (evaluation order is not contractual). Identities carry at most
    7 ACEs, so the quadratic term is irrelevant. The scratch set is materialised twice
    because MySQL cannot open one TEMPORARY table twice in a single statement.
    """
    diff_sql = _differing_identities_sql(klass)
    n = scalar(login_path, database, f"SELECT COUNT(*) FROM ({diff_sql}) d")
    if not apply_ or n == 0:
        return n
    run_sql(login_path, database, f"""
        DROP TEMPORARY TABLE IF EXISTS acl_sync_targets;
        DROP TEMPORARY TABLE IF EXISTS acl_sync_aces;
        DROP TEMPORARY TABLE IF EXISTS acl_sync_aces_b;

        CREATE TEMPORARY TABLE acl_sync_targets (canon_id BIGINT PRIMARY KEY) ENGINE=MEMORY;
        INSERT INTO acl_sync_targets (canon_id) {diff_sql};

        CREATE TEMPORARY TABLE acl_sync_aces (
            canon_id BIGINT, sid_id BIGINT, mask INT, granting TINYINT, ord INT,
            KEY k (canon_id)) ENGINE=MEMORY;
        INSERT INTO acl_sync_aces (canon_id, sid_id, mask, granting, ord)
        SELECT oi.id, k.id, le.MASK, le.GRANTING, MIN(le.ACE_ORDER)
          FROM acl_sync_targets t
          JOIN acl_object_identity oi ON oi.id = t.canon_id
          JOIN acl_class c ON c.id = oi.object_id_class
          JOIN ACLOBJECTIDENTITY o ON o.OBJECT_CLASS = c.class AND o.OBJECT_ID = oi.object_id_identity
          JOIN ACLENTRY le ON le.OBJECTIDENTITY_FK = o.ID
          JOIN ACLSID ls ON ls.ID = le.SID_FK
          JOIN acl_sid k ON k.sid = {LEGACY_SID_NAME} AND k.principal = {LEGACY_SID_IS_PRINCIPAL}
         GROUP BY oi.id, k.id, le.MASK, le.GRANTING;

        CREATE TEMPORARY TABLE acl_sync_aces_b LIKE acl_sync_aces;
        INSERT INTO acl_sync_aces_b SELECT * FROM acl_sync_aces;

        DELETE ke FROM acl_entry ke JOIN acl_sync_targets t ON t.canon_id = ke.acl_object_identity;

        INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
        SELECT a.canon_id,
               (SELECT COUNT(*) FROM acl_sync_aces_b b
                 WHERE b.canon_id = a.canon_id
                   AND (b.ord < a.ord OR (b.ord = a.ord AND b.sid_id < a.sid_id))),
               a.sid_id, a.mask, a.granting, 0, 0
          FROM acl_sync_aces a
         ORDER BY a.canon_id, a.ord, a.sid_id;

        DROP TEMPORARY TABLE acl_sync_targets;
        DROP TEMPORARY TABLE acl_sync_aces;
        DROP TEMPORARY TABLE acl_sync_aces_b;
    """)
    return n


def report_extras(login_path, database, klass):
    """Canonical identities with no legacy counterpart. Reported, never deleted."""
    return scalar(login_path, database, f"""
        SELECT COUNT(*)
          FROM acl_object_identity oi
          JOIN acl_class c ON c.id = oi.object_id_class
         WHERE NOT EXISTS (
            SELECT 1 FROM ACLOBJECTIDENTITY o
             WHERE o.OBJECT_CLASS = c.class AND o.OBJECT_ID = oi.object_id_identity)
           {f"AND c.class = '{klass}'" if klass else ""}
    """)


def main():
    p = argparse.ArgumentParser(description="Sync legacy gsec ACLs into the canonical Spring Security ACL tables.")
    p.add_argument("--login-path", default="gemd-ro", help="mysql_config_editor login-path (default: gemd-ro)")
    p.add_argument("--database", default="gemd")
    p.add_argument("--class", dest="klass", default=None, help="restrict to one fully-qualified securable class")
    p.add_argument("--apply", action="store_true", help="write changes (default is a dry run)")
    p.add_argument("--skip-aces", action="store_true", help="skip ACE reconciliation (the slow phase)")
    args = p.parse_args()

    if args.apply and args.login_path.endswith("-ro"):
        sys.exit(f"refusing to --apply through read-only login-path '{args.login_path}'; "
                 f"pass --login-path with write access")

    if not args.skip_aces:
        assert_no_deny_aces(args.login_path, args.database)

    mode = "APPLY" if args.apply else "DRY RUN"
    print(f"acl_sync: legacy -> canonical  [{mode}]  db={args.database} "
          f"login-path={args.login_path} class={args.klass or 'ALL'}\n")

    results = []
    for builder in (step_classes, step_sids, step_identities, step_parents):
        label, count_sql, apply_sql = builder(args.klass)
        t0 = time.time()
        n = scalar(args.login_path, args.database, count_sql)
        if args.apply and n:
            run_sql(args.login_path, args.database, apply_sql)
        results.append((label, n, time.time() - t0))
        print(f"  {label:<38} {n:>9,}  ({results[-1][2]:.1f}s)")

    # NOTE: on a dry run the ACE figure counts only identities that already exist in
    # canonical. Identities reported above as "to create" contribute no ACE diff yet,
    # because the comparison joins through acl_object_identity. After --apply creates
    # them, a second run will report (and fill) their ACEs. Two passes are therefore
    # expected on a database with missing identities; the second is idempotent.
    if args.skip_aces:
        print(f"  {'identities with differing ACEs':<38} {'skipped':>9}")
    else:
        t0 = time.time()
        n = sync_aces(args.login_path, args.database, args.klass, args.apply)
        results.append(("identities with differing ACEs", n, time.time() - t0))
        print(f"  {'identities with differing ACEs':<38} {n:>9,}  ({time.time() - t0:.1f}s)")

    extras = report_extras(args.login_path, args.database, args.klass)
    print(f"  {'canonical-only identities (report)':<38} {extras:>9,}")

    total = sum(n for _, n, _ in results)
    print()
    if args.apply:
        print(f"Applied {total:,} changes.")
        print("The webapp caches ACLs in memory (SpringCacheBasedAclCache). Flush it:")
        print("    curl -X DELETE -H 'Authorization: Bearer $TOKEN' <base>/admin/caches")
        if not args.skip_aces:
            print("Run again: newly created identities only acquire their ACEs on a second pass.")
    elif total:
        print(f"{total:,} changes pending. Re-run with a write login-path and --apply.")
        print("Expect two --apply passes: the first creates identities, the second fills their ACEs.")
    else:
        print("Canonical store is in sync with legacy.")

    if extras:
        print(f"\n{extras:,} canonical identities have no legacy counterpart. Not deleted by this "
              f"tool -- they are either entities deleted on prod (use `lintAcls --apply-fixes` to "
              f"remove dangling ones) or entities created by phase2 itself.")


if __name__ == "__main__":
    main()
