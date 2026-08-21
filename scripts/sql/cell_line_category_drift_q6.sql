-- Q6 -- Is anything still minting EFO_0000322 as a cell-line CATEGORY, or is the gap against
-- cab's 2026-05-16 snapshot a matter of counting surface and scope?
--
-- cab measured 21,708 EFO_0000322 / 72,363 CLO_0000031 off the May snapshot; the 2026-08-20
-- census read 28,657 / 102,680 off CHARACTERISTIC on prod. A URI that no source in four
-- repositories writes should not grow 32% in three months, so one of the two readings is
-- measuring something the other is not.
--
-- This splits the EE2C surface three ways at once:
--   uri    which of the two cell-line category URIs
--   shape  GSE-shaped SHORT_NAME vs anything else -- the snapshot's population filter
--   era    whether the experiment predates the snapshot, taken from the FIRST audit event on
--          its trail (the closest thing the schema has to a creation date)
--
-- Reading it: if the "newer than the snapshot" bucket is ~empty, nothing has minted this URI
-- onto a new experiment since May and the gap is surface/scope. If it is not empty, there is a
-- live producer and a bulk retag would run against it.
--
-- 🛑 EE2C is the DENORMALIZED surface -- its totals are much smaller than CHARACTERISTIC's
-- (18,501 / 2,336 vs 102,680 / 28,657 on 2026-08-20). That is expected and is not the subject
-- here: this query is about the era and shape SPLIT, not about the absolute total.
--
-- 🛑 And EE2C rows can be STALE, which is the caveat that bites the CONCLUSION rather than the
-- totals. Its refresh is an upsert, so it cannot fix or delete a non-winner row -- 1,008 survived
-- a full refresh on prod. A stale row puts its experiment in an era bucket it no longer belongs
-- in, in either direction. So a non-empty "newer than the snapshot" bucket is a candidate list to
-- confirm against CHARACTERISTIC, not on its own proof that a live producer exists.
--
-- Read-only. One statement.
SELECT uri,
       shape,
       era,
       COUNT(*)           AS n_annotations,
       COUNT(DISTINCT ee) AS n_experiments
FROM (
  SELECT e.EXPRESSION_EXPERIMENT_FK AS ee,
         e.CATEGORY_URI             AS uri,
         CASE WHEN i.SHORT_NAME LIKE 'GSE%' THEN 'GSE-shaped' ELSE 'other' END AS shape,
         CASE WHEN ( SELECT MIN( ae.`DATE` )
                       FROM AUDIT_EVENT ae
                      WHERE ae.AUDIT_TRAIL_FK = i.AUDIT_TRAIL_FK ) < '2026-05-16'
              THEN '1 predates the snapshot'
              ELSE '2 newer than the snapshot' END AS era
    FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e
    JOIN INVESTIGATION i ON i.ID = e.EXPRESSION_EXPERIMENT_FK
   WHERE e.CATEGORY_URI IN ( 'http://purl.obolibrary.org/obo/CLO_0000031',
                             'http://www.ebi.ac.uk/efo/EFO_0000322' )
) s
GROUP BY uri, shape, era
ORDER BY uri, shape, era;
