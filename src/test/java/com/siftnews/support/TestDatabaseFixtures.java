package com.siftnews.support;

import jakarta.persistence.EntityManager;

/** FK가 활성화된 통합 테스트에서 부모 행을 명시적으로 준비하는 fixture. */
public final class TestDatabaseFixtures {

    private TestDatabaseFixtures() {}

    public static void source(EntityManager entityManager, long id) {
        entityManager.createNativeQuery("""
                INSERT INTO source (id, created_at, updated_at, name, type, url, lang, category, active)
                VALUES (:id, now(), now(), :name, 'RSS', :url, 'en', 'DEV', true)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("name", "Fixture source " + id)
                .setParameter("url", "https://fixture.example.com/rss/" + id)
                .executeUpdate();
    }

    public static void topic(EntityManager entityManager, long id) {
        entityManager.createNativeQuery("""
                INSERT INTO topic (id, created_at, updated_at, name, slug, lang_scope,
                    include_keywords, exclude_keywords, keyword_weights, source_categories,
                    recency_half_life_hours, max_items, score_threshold, active)
                VALUES (:id, now(), now(), :name, :slug, 'ko,en',
                    '[]'::jsonb, '[]'::jsonb, '{}'::jsonb, '["DEV"]'::jsonb,
                    24, 10, 0.0, true)
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("name", "Fixture topic " + id)
                .setParameter("slug", "fixture-" + id)
                .executeUpdate();
    }

    public static void article(EntityManager entityManager, long id, long sourceId) {
        entityManager.createNativeQuery("""
                INSERT INTO article (id, created_at, updated_at, source_id, url, normalized_url,
                    title, body, lang, category)
                VALUES (:id, now(), now(), :sourceId, :url, :url, 'Fixture article', 'Fixture body', 'en', 'DEV')
                ON CONFLICT (id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("sourceId", sourceId)
                .setParameter("url", "https://fixture.example.com/article/" + id)
                .executeUpdate();
    }
}
