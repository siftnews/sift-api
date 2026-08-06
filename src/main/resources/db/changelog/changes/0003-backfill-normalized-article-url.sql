--liquibase formatted sql

--changeset siftnews:0003-backfill-normalized-article-url
-- 기존 규칙이 쿼리 전체를 버렸던 시기에 적재된 행만 대상으로 한다. url 본문의 비추적 쿼리를
-- 이름·값 순서로 안정 정렬해 붙이며, 이미 점유된 키와 충돌하면 기존 행을 보존한다.
WITH candidate AS (
    SELECT id,
           split_part(normalized_url, '?', 1) || COALESCE((
               SELECT '?' || string_agg(parameter, '&' ORDER BY split_part(parameter, '=', 1), parameter)
               FROM unnest(string_to_array(split_part(split_part(url, '?', 2), '#', 1), '&')) AS parameter
               WHERE parameter <> ''
                 AND lower(split_part(parameter, '=', 1)) !~ '^(utm_|at_)'
                 AND lower(split_part(parameter, '=', 1)) NOT IN ('fbclid', 'gclid', 'igshid', 'mc_cid', 'mc_eid', 'ref')
           ), '') AS target_normalized_url
    FROM article
    WHERE position('?' IN url) > 0
)
UPDATE article AS target
SET normalized_url = candidate.target_normalized_url
FROM candidate
WHERE target.id = candidate.id
  AND target.normalized_url <> candidate.target_normalized_url
  AND NOT EXISTS (
      SELECT 1
      FROM article AS occupied
      WHERE occupied.normalized_url = candidate.target_normalized_url
        AND occupied.id <> target.id
  );
