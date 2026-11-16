-- PostgreSQL: data.sql resets the counters (ALTER ... RESTART WITH) on every startup.
-- With a persistent database that leaves the IDs below the existing ones; here they are moved to the real maximum.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM categories), false);
SELECT setval(pg_get_serial_sequence('knowledge_articles', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM knowledge_articles), false);
SELECT setval(pg_get_serial_sequence('incidents', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM incidents), false);
SELECT setval(pg_get_serial_sequence('ai_queries', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM ai_queries), false);
SELECT setval(pg_get_serial_sequence('ai_responses', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM ai_responses), false);
SELECT setval(pg_get_serial_sequence('response_sources', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM response_sources), false);
SELECT setval(pg_get_serial_sequence('ratings', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM ratings), false);
