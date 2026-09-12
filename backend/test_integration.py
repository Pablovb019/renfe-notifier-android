import os
import sys
sys.path.insert(0, os.path.dirname(__file__))

from db import database
from renfe.checker import RenfeChecker
import datetime

def test_database_and_checker():
    # Initialize DB
    database.init_db()
    print("Database initialized.")

    # Add a followup
    userid = "test_user"
    origin = "MADRID (TODAS)"
    destination = "BARCELONA SANTS"
    travel_date = (datetime.datetime.now() + datetime.timedelta(days=1)).strftime("%Y-%m-%d")
    plaza_h = False
    watch_all = False
    fid = database.add_followup(userid, origin, destination, travel_date, plaza_h, watch_all)
    print(f"Added followup with ID {fid}")

    # Retrieve active followups
    active = database.get_active_followups()
    print(f"Active followups: {len(active)}")
    for f in active:
        print(f"  ID {f['id']}: {f['origin']} -> {f['destination']} on {f['travel_date']}")

    # Check the trip using the checker
    checker = RenfeChecker()
    print(f"Checking trip {origin} -> {destination} on {travel_date}...")
    available, trains = checker.check_trip(origin, destination, travel_date, None, plaza_h)
    print(f"Check result: available={available}, trains count={len(trains) if trains else 0}")
    if trains:
        for t in trains[:3]:  # Show first 3 trains
            print(f"  Train: {t.get('TREN', '?')} {t.get('SALIDA', '?')}-{t.get('LLEGADA', '?')} disponible={t.get('DISPONIBLE', False)}")

    # Update followup based on result
    if available is not None:
        database.set_followup_available(fid, available)
        print(f"Updated followup {fid} availability to {available}")

    # Clean up: delete the followup
    database.delete_followup(fid)
    print(f"Deleted followup {fid}")

    print("Integration test completed successfully.")

if __name__ == "__main__":
    test_database_and_checker()