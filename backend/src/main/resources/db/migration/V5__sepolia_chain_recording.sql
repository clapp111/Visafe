UPDATE chain_records SET network = 'stub' WHERE network = 'sepolia';

ALTER TABLE chain_records
    ALTER COLUMN network SET DEFAULT 'stub',
    ADD COLUMN chain_id BIGINT,
    ADD COLUMN contract_address VARCHAR(42),
    ADD COLUMN signed_transaction TEXT,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_chain_records__sepolia_target CHECK (
        (network = 'sepolia' AND chain_id IS NOT NULL AND chain_id > 0
            AND contract_address IS NOT NULL AND contract_address ~ '^0x[0-9a-f]{40}$')
        OR (network <> 'sepolia' AND chain_id IS NULL AND contract_address IS NULL)
    ),
    ADD CONSTRAINT ck_chain_records__signed_transaction CHECK (
        signed_transaction IS NULL OR (network = 'sepolia' AND status IN ('PENDING', 'SENT'))
    );

-- 같은 DB를 쓰는 작업자들이 하나의 기록 지갑으로 동시에 새 nonce를 배정하지 않게 한다.
CREATE UNIQUE INDEX uq_chain_records__sepolia_inflight
    ON chain_records (network) WHERE signed_transaction IS NOT NULL;

CREATE INDEX ix_chain_records__sepolia_pending
    ON chain_records (id) WHERE network = 'sepolia' AND status IN ('PENDING', 'SENT');
