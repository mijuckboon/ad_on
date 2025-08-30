-- Campaign VO 테이블
CREATE TABLE campaign (
                          schedule_id BIGINT NOT NULL COMMENT 'Schedule 참조',
                          campaign_id BIGINT NOT NULL COMMENT '캠페인 ID',
                          total_budget BIGINT NOT NULL COMMENT '총 예산'
);

-- AdSet VO 테이블
CREATE TABLE ad_set (
                        schedule_id       BIGINT NOT NULL COMMENT 'Schedule 참조',
                        ad_set_id         BIGINT NOT NULL COMMENT '광고 세트 ID',
                        ad_set_start_date DATE NOT NULL COMMENT '세트 시작일',
                        ad_set_end_date   DATE NOT NULL COMMENT '세트 종료일',
                        ad_set_start_time TIME NOT NULL COMMENT '세트 시작 시간',
                        ad_set_end_time   TIME NOT NULL COMMENT '세트 종료 시간',
                        ad_set_status     VARCHAR(10) NOT NULL COMMENT '세트 상태',
                        daily_budget      BIGINT NOT NULL COMMENT '일 예산',
                        payment_type      VARCHAR(10) NOT NULL COMMENT '입찰 전략',
                        unit_cost         BIGINT NOT NULL DEFAULT 0 COMMENT '기준 입찰액',
                        CHECK (ad_set_status IN ('ON','OFF')),
                        CHECK (payment_type IN ('CPM','CPC','CPA')),
                        CHECK (daily_budget >= 0),
                        CHECK (unit_cost >= 0)
);

-- Creative VO 테이블
CREATE TABLE creative (
                          schedule_id            BIGINT NOT NULL COMMENT 'Schedule 참조',
                          creative_id            BIGINT NOT NULL COMMENT '소재 ID',
                          landing_url            VARCHAR(2000) NOT NULL COMMENT 'URL',
                          creative_status        VARCHAR(10) NOT NULL COMMENT '소재 상태',
                          creative_image         VARCHAR(2000) NULL COMMENT '소재 이미지',
                          creative_movie         VARCHAR(2000) NULL COMMENT '소재 영상',
                          creative_logo          VARCHAR(2000) NULL COMMENT '소재 로고',
                          copyrighting_title     VARCHAR(255) NULL COMMENT '카피라이팅 타이틀',
                          copyrighting_subtitle  VARCHAR(255) NULL COMMENT '카피라이팅 서브타이틀',
                          CHECK (creative_status IN ('ON','OFF'))
);