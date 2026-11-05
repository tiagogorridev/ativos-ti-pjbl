const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

// Testes de arquitetura: conferem as dependências entre as pastas lendo os require() de cada arquivo.
// src/dominio guarda as regras puras e src/functions só liga essas regras aos gatilhos do Azure.

const SRC = path.join(__dirname, "..", "src");

function arquivos(pasta) {
  return fs.readdirSync(path.join(SRC, pasta)).filter((f) => f.endsWith(".js"));
}

function requires(pasta, arquivo) {
  const codigo = fs.readFileSync(path.join(SRC, pasta, arquivo), "utf8");
  return [...codigo.matchAll(/require\(\s*["']([^"']+)["']\s*\)/g)].map((m) => m[1]);
}

describe("arquitetura das Functions", () => {
  it("o domínio não depende do Azure nem de outros pacotes", () => {
    for (const arquivo of arquivos("dominio")) {
      for (const dep of requires("dominio", arquivo)) {
        assert.ok(dep.startsWith("./"), `${arquivo} não pode depender de "${dep}"`);
      }
    }
  });

  it("o domínio não depende das Functions", () => {
    for (const arquivo of arquivos("dominio")) {
      for (const dep of requires("dominio", arquivo)) {
        assert.ok(!dep.includes("functions"), `${arquivo} não pode depender de "${dep}"`);
      }
    }
  });

  it("cada Function depende só do SDK do Azure e do domínio", () => {
    for (const arquivo of arquivos("functions")) {
      for (const dep of requires("functions", arquivo)) {
        const permitido = dep === "@azure/functions" || dep.startsWith("../dominio/");
        assert.ok(permitido, `${arquivo} não pode depender de "${dep}"`);
      }
    }
  });

  it("cada arquivo registra uma única Function (uma fatia por arquivo)", () => {
    for (const arquivo of arquivos("functions")) {
      const codigo = fs.readFileSync(path.join(SRC, "functions", arquivo), "utf8");
      const registros = codigo.match(/\bapp\.(http|serviceBusQueue|serviceBusTopic|timer|storageQueue)\(/g) ?? [];
      assert.equal(registros.length, 1, `${arquivo} registra ${registros.length} Functions`);
    }
  });
});
