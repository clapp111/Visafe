import assert from "node:assert/strict";
import { test } from "node:test";
import { network } from "hardhat";
import { getAddress, keccak256, parseEventLogs, toHex, zeroAddress, zeroHash } from "viem";
import ContractAnchorRegistryModule from "../ignition/modules/ContractAnchorRegistry.js";

const CONFIRM = 0;
const TERMINATE = 1;
const anchorKey = keccak256(toHex("visafe-test-contract"));
const otherAnchorKey = keccak256(toHex("visafe-other-contract"));
const confirmationHash = keccak256(toHex("visafe-confirmation"));
const terminationHash = keccak256(toHex("visafe-termination"));
const differentHash = keccak256(toHex("visafe-different-record"));

async function deployRegistry() {
  const { ignition, viem } = await network.create();
  const { registry } = await ignition.deploy(ContractAnchorRegistryModule);
  const [deployer, otherAccount] = await viem.getWalletClients();
  const publicClient = await viem.getPublicClient();

  return { registry, deployer, otherAccount, publicClient };
}

test("sets the deployer as admin and initial writer", async () => {
  const { registry, deployer } = await deployRegistry();

  assert.equal(await registry.read.admin(), getAddress(deployer.account.address));
  assert.equal(await registry.read.writer(), getAddress(deployer.account.address));
  assert.equal(await registry.read.getRecordHash([anchorKey, CONFIRM]), zeroHash);
  assert.equal(await registry.read.getRecordHash([anchorKey, TERMINATE]), zeroHash);
});

test("records both hashes under one anchor and emits the record details", async () => {
  const { registry, deployer, publicClient } = await deployRegistry();

  const confirmation = await registry.write.record([anchorKey, CONFIRM, confirmationHash]);
  const termination = await registry.write.record([anchorKey, TERMINATE, terminationHash]);

  assert.equal(await registry.read.getRecordHash([anchorKey, CONFIRM]), confirmationHash);
  assert.equal(await registry.read.getRecordHash([anchorKey, TERMINATE]), terminationHash);

  for (const [hash, recordType, recordHash] of [
    [confirmation, CONFIRM, confirmationHash],
    [termination, TERMINATE, terminationHash],
  ] as const) {
    const receipt = await publicClient.waitForTransactionReceipt({ hash });
    const events = parseEventLogs({
      abi: registry.abi,
      eventName: "RecordWritten",
      logs: receipt.logs,
    });
    assert.equal(events.length, 1);
    assert.deepEqual(events[0].args, {
      anchorKey,
      recordType,
      recordHash,
      writer: getAddress(deployer.account.address),
    });
  }
});

test("rejects termination before confirmation of the same anchor", async () => {
  const { registry } = await deployRegistry();
  await registry.write.record([otherAnchorKey, CONFIRM, confirmationHash]);

  await assert.rejects(
    registry.write.record([anchorKey, TERMINATE, terminationHash]),
    /ConfirmationRequired/,
  );
  assert.equal(await registry.read.getRecordHash([anchorKey, TERMINATE]), zeroHash);
});

test("accepts an identical retry without emitting another record event", async () => {
  const { registry, publicClient } = await deployRegistry();
  await registry.write.record([anchorKey, CONFIRM, confirmationHash]);
  await registry.write.record([anchorKey, TERMINATE, terminationHash]);

  for (const [recordType, recordHash] of [
    [CONFIRM, confirmationHash],
    [TERMINATE, terminationHash],
  ] as const) {
    const hash = await registry.write.record([anchorKey, recordType, recordHash]);
    const receipt = await publicClient.waitForTransactionReceipt({ hash });
    assert.equal(receipt.status, "success");
    assert.equal(receipt.logs.length, 0);
    assert.equal(await registry.read.getRecordHash([anchorKey, recordType]), recordHash);
  }
});

test("rejects attempts to replace a confirmation or termination hash", async () => {
  const { registry } = await deployRegistry();
  await registry.write.record([anchorKey, CONFIRM, confirmationHash]);
  await registry.write.record([anchorKey, TERMINATE, terminationHash]);

  for (const [recordType, recordHash] of [
    [CONFIRM, confirmationHash],
    [TERMINATE, terminationHash],
  ] as const) {
    await assert.rejects(
      registry.write.record([anchorKey, recordType, differentHash]),
      /RecordAlreadyWritten/,
    );
    assert.equal(await registry.read.getRecordHash([anchorKey, recordType]), recordHash);
  }
});

test("rejects writes from a wallet other than the writer", async () => {
  const { registry, otherAccount } = await deployRegistry();

  await assert.rejects(
    registry.write.record([anchorKey, CONFIRM, confirmationHash], {
      account: otherAccount.account,
    }),
    /UnauthorizedWriter/,
  );
});

test("rejects zero keys, zero hashes and unknown record types", async () => {
  const { registry } = await deployRegistry();

  await assert.rejects(registry.write.record([zeroHash, CONFIRM, confirmationHash]), /InvalidRecord/);
  await assert.rejects(registry.write.record([anchorKey, CONFIRM, zeroHash]), /InvalidRecord/);
  await assert.rejects(registry.write.record([anchorKey, 2, confirmationHash]), /revert/i);
});

test("allows only the admin to rotate the writer and preserves existing records", async () => {
  const { registry, deployer, otherAccount, publicClient } = await deployRegistry();
  await registry.write.record([anchorKey, CONFIRM, confirmationHash]);

  await assert.rejects(
    registry.write.setWriter([otherAccount.account.address], { account: otherAccount.account }),
    /UnauthorizedAdmin/,
  );
  await assert.rejects(registry.write.setWriter([zeroAddress]), /InvalidWriter/);

  const hash = await registry.write.setWriter([otherAccount.account.address]);
  const receipt = await publicClient.waitForTransactionReceipt({ hash });
  const events = parseEventLogs({
    abi: registry.abi,
    eventName: "WriterChanged",
    logs: receipt.logs,
  });
  assert.equal(events.length, 1);
  assert.deepEqual(events[0].args, {
    previousWriter: getAddress(deployer.account.address),
    newWriter: getAddress(otherAccount.account.address),
  });

  await assert.rejects(
    registry.write.record([anchorKey, TERMINATE, terminationHash]),
    /UnauthorizedWriter/,
  );
  await registry.write.record([anchorKey, TERMINATE, terminationHash], {
    account: otherAccount.account,
  });
  assert.equal(await registry.read.getRecordHash([anchorKey, CONFIRM]), confirmationHash);
  assert.equal(await registry.read.getRecordHash([anchorKey, TERMINATE]), terminationHash);
});
