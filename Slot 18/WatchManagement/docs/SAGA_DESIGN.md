# Saga design — Watch Management

MSWatch is the orchestrator, MSBrand owns the brand/watch assignment, and each service commits only to its own database. HTTP calls across services are not covered by a local `@Transactional`; Saga state and log entries use independent `REQUIRES_NEW` transactions.

## State lifecycle

`PENDING → EXECUTING → COMPLETED`; failures move through `FAILED → COMPENSATING → COMPENSATED`. If compensation fails after three attempts, the state stays `COMPENSATING` for manual recovery.

## CREATE_WATCH

1. Insert the watch in DBWatch; compensate by deleting that watch.
2. Assign the watch to its brand in DBBrand; compensate by unassigning it.

Compensate in reverse order.

## UPDATE_WATCH

1. Save the new watch values in DBWatch; compensate by restoring the before snapshot.
2. Assign it to the new brand in DBBrand; compensate by assigning the old brand again.

## DELETE_WATCH

1. Unassign the watch from DBBrand; compensate by assigning it back to the previous brand.
2. Delete the watch from DBWatch. This is the pivot because its generated identity cannot be restored reliably.

Brand assign/unassign operations are idempotent so retrying compensation is safe. The pivot is last so a failed remote operation can still be compensated. Saga gives eventual consistency and does not provide isolation across databases.
