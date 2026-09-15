from app.monitoring import Monitoring


def test_snapshot_starts_empty() -> None:
    monitor = Monitoring()

    snapshot = monitor.snapshot()

    assert snapshot.logical_queries == 0
    assert snapshot.http_requests == 0
    assert snapshot.bytes_received == 0


def test_accumulates_logical_queries_and_http_requests() -> None:
    monitor = Monitoring()
    monitor.record_logical_query()
    monitor.record_logical_query()
    monitor.record_search(http_requests=5, bytes_received=1200)
    monitor.record_search(http_requests=6, bytes_received=900)

    snapshot = monitor.snapshot()

    assert snapshot.logical_queries == 2
    assert snapshot.http_requests == 11
    assert snapshot.bytes_received == 2100


def test_snapshots_are_independent_of_further_updates() -> None:
    monitor = Monitoring()
    before = monitor.snapshot()
    monitor.record_logical_query()
    monitor.record_search(http_requests=1, bytes_received=1)

    assert before.logical_queries == 0
    after = monitor.snapshot()
    assert after.logical_queries == 1
    assert after.http_requests == 1
