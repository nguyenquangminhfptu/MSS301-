# Commit theo TODO

Thư mục `slot13/saga-demo`; tên commit theo mẫu được yêu cầu.
Mỗi TODO có một commit riêng; số TODO được ghi trong commit body.

| TODO | Commit | Message |
|---|---|---|
| TODO-0 | `f7dbb46` | `build(saga): add docker compose for mysql, sqlserver, mongodb` |
| TODO-1 | `310febe` | `feat(inventory): add product/reservation documents and data seeder` |
| TODO-2 | `1038f6c` | `feat(inventory): add reserve and release with mongo transaction` |
| TODO-3 | `53ab684` | `feat(payment): add wallet/payment entities and idempotent data.sql` |
| TODO-4 | `e955c66` | `feat(payment): add pay and refund with idempotent handling` |
| TODO-5 | `a7a1c30` | `feat(order): add order/saga-log entities, status enums and dtos` |
| TODO-6 | `0a4db8f` | `feat(order): add order service with per-step local transactions` |
| TODO-7 | `f682520` | `feat(order): add rest clients for inventory and payment services` |
| TODO-8 | `9063e55` | `feat(order): add saga orchestrator with lifo compensation and retry` |
| TODO-9 | `66f53f6` | `feat(order): add order controller with saga-logs endpoint` |
| TODO-10 | `fb8d9fe` | `build(saga): add dockerfiles and services to docker compose` |
| TODO-11 | xem lệnh bên dưới | `test(postman): add saga collection and outage tests` |

```sh
git log --oneline --grep="test(postman): add saga collection and outage tests"
git log --reverse --format="%h %s%n%b" 4430324..HEAD -- slot13
```

Chỉ 11 commit Slot 13 chưa push được chỉnh tên/path theo ảnh mẫu.
Các commit trước đó và thay đổi có sẵn ở Slot khác/Assignment 1 được giữ nguyên.
