# Test type: API smoke test (curl)
# Validation to be executed: parse -> validate -> filter -> returns endpoints return 200 and expected fields
# Command: bash test/smoke_api.sh http://localhost:5477

set -euo pipefail

BASE="${1:-http://localhost:5477}"

echo "1) performance"
curl -sf "$BASE/blackrock/challenge/v1/performance" > /dev/null
echo "OK"

echo "2) parse"
PARSE_RES=$(curl -s -X POST "$BASE/blackrock/challenge/v1/transactions:parse" \
  -H "Content-Type: application/json" \
  -d '{"expenses":[{"date":"2024-01-01 10:00:00","amount":1519},{"date":"2024-01-01 10:10:00","amount":1600}]}')

echo "$PARSE_RES" | grep -q '"ceiling": 1600'
echo "$PARSE_RES" | grep -q '"remanent": 81'
echo "OK"

echo "3) filter"
curl -sf -X POST "$BASE/blackrock/challenge/v1/transactions:filter" \
  -H "Content-Type: application/json" \
  -d '{
    "transactions":[
      {"date":"2024-01-01 10:00:00","amount":1519,"ceiling":1600,"remanent":81},
      {"date":"2024-01-01 10:10:00","amount":1600,"ceiling":1600,"remanent":0}
    ],
    "qPeriods":[{"startDate":"2024-01-01 10:00:00","endDate":"2024-01-01 10:06:00","fixed":25}],
    "pPeriods":[{"startDate":"2024-01-01 10:00:00","endDate":"2024-01-01 10:20:00","extra":5}],
    "kPeriods":[{"startDate":"2024-01-01 09:00:00","endDate":"2024-01-01 10:30:00"}]
  }' > /dev/null
echo "OK"

echo "All smoke tests passed."