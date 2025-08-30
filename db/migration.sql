INSERT INTO campaign (
    campaign_id,
    schedule_id,
    total_budget
)
SELECT
    id AS campaign_id,
    id AS schedule_id,
    total_budget
FROM schedule;

INSERT INTO ad_set (
    ad_set_id,
    schedule_id,
    ad_set_start_date,
    ad_set_end_date,
    ad_set_start_time,
    ad_set_end_time,
    ad_set_status,
    daily_budget,
    payment_type,
    unit_cost
)
SELECT
    id AS ad_set_id,
    id AS schedule_id,
    ad_set_start_date,
    ad_set_end_date,
    ad_set_start_time,
    ad_set_end_time,
    ad_set_status,
    daily_budget,
    payment_type,
    unit_cost
FROM schedule;

INSERT INTO creative (
    creative_id,
    schedule_id,
    landing_url,
    creative_status,
    creative_image,
    creative_movie,
    creative_logo,
    copyrighting_title,
    copyrighting_subtitle
)
SELECT
    creative_id,
    id AS schedule_id,
    landing_url,
    creative_status,
    creative_image,
    creative_movie,
    creative_logo,
    copyrighting_title,
    copyrighting_subtitle
FROM schedule;

ALTER TABLE schedule
    DROP COLUMN creative_id,
    DROP COLUMN ad_set_start_date,
    DROP COLUMN ad_set_end_date,
    DROP COLUMN ad_set_start_time,
    DROP COLUMN ad_set_end_time,
    DROP COLUMN total_budget,
    DROP COLUMN daily_budget,
    DROP COLUMN payment_type,
    DROP COLUMN unit_cost,
    DROP COLUMN creative_image,
    DROP COLUMN creative_movie,
    DROP COLUMN creative_logo,
    DROP COLUMN copyrighting_title,
    DROP COLUMN copyrighting_subtitle,
    DROP COLUMN ad_set_status,
    DROP COLUMN creative_status,
    DROP COLUMN landing_url;
