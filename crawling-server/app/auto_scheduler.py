"""
대학 공지사항 자동 크롤링 스케줄러
college_code의 크롤러들을 자동으로 등록하고 관리
"""

import logging
from typing import Dict, List, Any
from celery import current_app
from celery.schedules import crontab

from college_crawlers import get_college_crawler

logger = logging.getLogger(__name__)


class CollegeAutoScheduler:
    """대학 공지사항 자동 스케줄러"""

    def __init__(self):
        self.crawler = get_college_crawler()
        self.job_configs = self._get_job_configs()

    def _get_job_configs(self) -> List[Dict[str, Any]]:
        """크롤링 작업 설정 목록 (간소화 버전)"""
        return [
            {
                "name": "college-봉사-공지사항-크롤링",
                "category": "volunteer",
                "schedule_cron": "0 */2 * * *",  # 2시간마다
            },
            {
                "name": "college-취업-공지사항-크롤링",
                "category": "job",
                "schedule_cron": "0 */3 * * *",  # 3시간마다
            },
            {
                "name": "college-장학금-공지사항-크롤링",
                "category": "scholarship",
                "schedule_cron": "0 */4 * * *",  # 4시간마다
            },
            {
                "name": "college-일반행사/채용-크롤링",
                "category": "general_events",
                "schedule_cron": "0 */6 * * *",  # 6시간마다
            },
            {
                "name": "college-교육시험-크롤링",
                "category": "educational_test",
                "schedule_cron": "0 */6 * * *",  # 6시간마다
            },
            {
                "name": "college-등록금납부-크롤링",
                "category": "tuition_payment",
                "schedule_cron": "0 */8 * * *",  # 8시간마다
            },
            {
                "name": "college-학점-크롤링",
                "category": "academic_credit",
                "schedule_cron": "0 */8 * * *",  # 8시간마다
            },
            {
                "name": "college-학위-크롤링",
                "category": "degree",
                "schedule_cron": "0 */8 * * *",  # 8시간마다
            },
        ]

    def update_celery_beat_schedule(self):
        """Celery Beat 스케줄 업데이트"""
        logger.info("Updating Celery Beat schedule...")

        app = current_app
        schedule = {}

        for config in self.job_configs:
            # cron 문자열 파싱 (분 시 일 월 요일)
            cron_parts = config["schedule_cron"].split()
            if len(cron_parts) == 5:
                schedule[config["name"]] = {
                    "task": "tasks.run_college_crawler",
                    "schedule": crontab(
                        minute=cron_parts[0],
                        hour=cron_parts[1],
                        day_of_month=cron_parts[2],
                        month_of_year=cron_parts[3],
                        day_of_week=cron_parts[4],
                    ),
                    "args": (config["category"],),
                }
                logger.info(f"Added schedule: {config['name']} - {config['schedule_cron']}")

        # Celery Beat 설정 업데이트
        app.conf.beat_schedule = schedule
        logger.info("Celery Beat schedule updated successfully")


def init_college_scheduler() -> bool:
    """대학 크롤링 스케줄러 초기화"""
    try:
        scheduler = CollegeAutoScheduler()
        scheduler.update_celery_beat_schedule()
        logger.info(f"College scheduler initialized")
        return True
    except Exception as e:
        logger.error(f"Failed to initialize college scheduler: {e}")
        return False
