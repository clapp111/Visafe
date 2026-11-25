import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

export default buildModule("ContractAnchorRegistryModule", (module) => {
  const registry = module.contract("ContractAnchorRegistry");

  return { registry };
});
