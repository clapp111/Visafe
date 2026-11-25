# Visafe 스마트 컨트랙트

`ContractAnchorRegistry`는 Spring에서 계산한 확정·종료 해시를 저장한다. 배포 지갑이 초기 관리자와 기록 지갑이 된다. 관리자는 `setWriter(address)`로 기록 지갑을 교체할 수 있다.

| ABI | 동작 |
| --- | --- |
| `record(bytes32,uint8,bytes32)` | 키, 기록 종류, 해시를 받는다. `0`은 확정, `1`은 종료다. 동일 기록의 재요청은 기존 해시를 유지하며 새 이벤트를 발행하지 않는다. |
| `getRecordHash(bytes32,uint8)` | 키와 기록 종류에 해당하는 해시를 반환한다. 기록이 없으면 `bytes32(0)`이다. |
| `RecordWritten(bytes32,uint8,bytes32,address)` | 첫 기록의 키, 기록 종류, 해시, 기록 지갑 주소를 남긴다. 키와 기록 종류는 로그 검색에 사용할 수 있다. |

## 로컬 검증

`contracts/`에서 실행한다. 테스트는 Ignition 모듈로 로컬 EVM에 레지스트리를 배포한다.

```sh
npm ci
npm run build
npm run typecheck
npm test
```

## Sepolia 배포

1. GitHub의 `sepolia` 환경을 만들고 배포 브랜치를 `dev`로 제한한다. 환경 Secrets에 `SEPOLIA_RPC_URL`과 `SEPOLIA_DEPLOYER_PRIVATE_KEY`를 등록한다. 지갑에는 수수료를 낼 Sepolia 테스트 ETH가 필요하다.
2. 워크플로가 `dev`에 병합되면 **Actions → Contracts → Run workflow**에서 `dev`를 선택한다. 최초 배포 시 `resume_run_id`를 비워 둔다. 이전 실행의 배포를 이어갈 때는 `sepolia-deployment` 아티팩트가 있는 실행 ID를 입력한다.
3. 배포 실행의 `sepolia-deployment` 아티팩트를 보관한다. `deployed_addresses.json`에 배포 주소가 있고, `journal.jsonl`에 트랜잭션 이력이 있다. `artifacts/`의 JSON에서 ABI와 바이트코드를 가져와 [#21](https://github.com/clapp111/Visafe/issues/21)에 넘긴다.

Sepolia의 체인 ID는 `11155111`이다. 배포 주소와 트랜잭션은 [Sepolia Etherscan](https://sepolia.etherscan.io/)에서 조회한다.
