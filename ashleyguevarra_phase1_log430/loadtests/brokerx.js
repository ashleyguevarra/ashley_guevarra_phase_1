import http from 'k6/http';
import { check, sleep } from 'k6';
import encoding from 'k6/encoding';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 25 },
    { duration: '30s', target: 50 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8082';
const AUTH = 'Basic ' + encoding.b64encode('admin:admin');

const CID_A = __ENV.CID_A;
const CID_B = __ENV.CID_B;
const AID_SRC = __ENV.AID_SRC;
const AID_DST = __ENV.AID_DST;

export default function () {
  const commonHeaders = {
    Authorization: AUTH,
    'X-Customer-Id': CID_A,
  };

  const r = Math.random();

  if (r < 0.7) {
    const res = http.get(`${BASE_URL}/accounts/${AID_SRC}/balance`, {
      headers: commonHeaders,
    });
    check(res, {
      'balance status is 200': (x) => x.status === 200,
    });
  } else if (r < 0.9) {
    const res = http.get(`${BASE_URL}/accounts/${AID_SRC}/ledger?page=0&size=10`, {
      headers: commonHeaders,
    });
    check(res, {
      'ledger status is 200': (x) => x.status === 200,
    });
  } else {
    const key = `k6-${__VU}-${__ITER}-${Date.now()}`;
    const body = JSON.stringify({
      fromAccountId: AID_SRC,
      toAccountId: AID_DST,
      amountCents: 1,
    });

    const res = http.post(`${BASE_URL}/transfers`, body, {
      headers: {
        ...commonHeaders,
        'Idempotency-Key': key,
        'Content-Type': 'application/json',
      },
    });

    check(res, {
      'transfer status is 200': (x) => x.status === 200,
    });
  }

  sleep(1);
}