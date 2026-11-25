import hardhatIgnitionViem from "@nomicfoundation/hardhat-ignition-viem";
import hardhatNodeTestRunner from "@nomicfoundation/hardhat-node-test-runner";
import { configVariable, defineConfig } from "hardhat/config";

const compiler = {
  version: "0.8.35",
  settings: {
    evmVersion: "cancun",
    optimizer: { enabled: true, runs: 200 },
  },
};

export default defineConfig({
  plugins: [hardhatIgnitionViem, hardhatNodeTestRunner],
  solidity: {
    profiles: { default: compiler, production: compiler },
  },
  networks: {
    sepolia: {
      type: "http",
      chainType: "l1",
      chainId: 11155111,
      url: configVariable("SEPOLIA_RPC_URL"),
      accounts: [configVariable("SEPOLIA_DEPLOYER_PRIVATE_KEY")],
    },
  },
});
