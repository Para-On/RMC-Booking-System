-- Align default Maya payment hold with typical checkout duration (was 15 minutes).
UPDATE rate_plan SET hold_ttl_minutes = 30 WHERE hold_ttl_minutes < 30;
