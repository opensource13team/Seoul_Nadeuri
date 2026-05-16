# Backend setup

이 프로젝트의 백엔드는 `worker.js`에 있는 Cloudflare Worker입니다. Android 앱은 나중에 Worker의 `/api/all-places` 엔드포인트를 호출하게 만들면 됩니다.

## 필요한 것

- Node.js
- Cloudflare 계정
- Wrangler CLI
- 서울 열린데이터광장 API 키
- Cloudflare KV namespace 2개: production, preview

## 최초 준비

```bash
npm install
npx wrangler login
```

Windows PowerShell에서 `npm.ps1` 실행 정책 오류가 나면 아래처럼 `.cmd`를 붙여 실행합니다.

```powershell
npm.cmd install
npx.cmd wrangler login
```

Cloudflare KV namespace를 만든 뒤 `wrangler.toml`의 값을 바꿔주세요.

```toml
[[kv_namespaces]]
binding = "SEOUL_CACHE"
id = "production namespace id"
preview_id = "preview namespace id"
```

로컬 개발용 환경변수 파일을 만듭니다.

```bash
cp .dev.vars.example .dev.vars
```

`.dev.vars` 안에 실제 서울시 API 키를 넣습니다.

운영 배포용 secret은 따로 등록합니다.

```bash
npx wrangler secret put SEOUL_API_KEY
```

## 실행

```bash
npm run dev
```

PowerShell 실행 정책 오류가 나면:

```powershell
npm.cmd run dev
```

확인할 API:

```text
GET http://localhost:8787/api/all-places
```

초기에는 KV 캐시가 비어 있을 수 있습니다. 이 경우 `503`이 정상입니다. scheduled job이 데이터를 수집하거나, Wrangler에서 scheduled 이벤트를 테스트한 뒤 다시 확인하면 됩니다.

## 배포

```bash
npm run deploy
```

배포 후 Android 앱에서는 Worker 배포 URL의 `/api/all-places`를 호출하면 됩니다.
