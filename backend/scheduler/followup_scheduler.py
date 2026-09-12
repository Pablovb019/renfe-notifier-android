import logging
import time
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

from backend.renfe.checker import RenfeChecker
from backend.db import database

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    level=logging.INFO)
logger = logging.getLogger(__name__)

class FollowupScheduler:
    def __init__(self):
        self.checker = RenfeChecker()
        self.scheduler = BackgroundScheduler()
        self.scheduler.add_job(
            self.check_followups,
            trigger=IntervalTrigger(seconds=30),
            id='followup_check_job',
            name='Check followups every 30 seconds',
            replace_existing=True,
        )
        self._running = False

    def start(self):
        if not self._running:
            self.scheduler.start()
            self._running = True
            logger.info("Followup scheduler started.")

    def shutdown(self):
        if self._running:
            self.scheduler.shutdown()
            self._running = False
            logger.info("Followup scheduler shut down.")

    def check_followups(self):
        """Job that runs every 30 seconds to check all active followups."""
        try:
            followups = database.get_active_followups()
            logger.info(f"Checking {len(followups)} active followups.")
            for followup in followups:
                self._process_followup(followup)
        except Exception as e:
            logger.exception("Error in followup check job: %s", e)

    def _process_followup(self, followup):
        """Check a single followup and update its state."""
        fid = followup['id']
        logger.debug(f"Processing followup {fid}: {followup['origin']} -> {followup['destination']} on {followup['travel_date']}")
        try:
            available, trains = self.checker.check_trip(
                followup['origin'],
                followup['destination'],
                followup['travel_date'],
                None,  # dat_ret not used for simplicity
                bool(followup['plaza_h'])
            )
            # Increment consult count
            database.increment_consult_count(fid)
            if available is not None and trains is not None:
                # Update availability in DB
                database.set_followup_available(fid, available)
                logger.info(f"Followup {fid} available={available}, trains found={len(trains)}")
                # TODO: If available and previously not available, send FCM notification (later hit)
            else:
                logger.warning(f"Followup {fid} check failed or no data.")
        except Exception as e:
            logger.exception("Error processing followup %s: %s", fid, e)

def main():
    """Entry point to run the scheduler."""
    # Initialize DB
    database.init_db()
    scheduler = FollowupScheduler()
    scheduler.start()
    try:
        # Keep the main thread alive
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down scheduler...")
        scheduler.shutdown()

if __name__ == "__main__":
    main()