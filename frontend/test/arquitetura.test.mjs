import { describe, it } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Testes de arquitetura do Frontend: leem os imports de cada arquivo e conferem as fronteiras
// entre os módulos de tela (futuros microfrontends), o Design System e o cliente de API.

const RAIZ = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const MODULOS = ["alocacoes", "ativos", "colaboradores", "dashboard", "manutencoes", "relatorios", "usuarios"];

function listar(pasta) {
  const dir = path.join(RAIZ, pasta);
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const rel = path.join(pasta, e.name);
    if (e.isDirectory()) return listar(rel);
    return /\.(ts|tsx)$/.test(e.name) ? [rel] : [];
  });
}

function ler(arquivo) {
  return fs.readFileSync(path.join(RAIZ, arquivo), "utf8");
}

function imports(arquivo) {
  return [...ler(arquivo).matchAll(/from\s+["']([^"']+)["']/g)].map((m) => m[1]);
}

const codigo = [...listar("app"), ...listar("components"), ...listar("lib")];

describe("arquitetura do Frontend", () => {
  it("só lib/api.ts faz chamadas HTTP (o site fala só com o BFF, pelo API Gateway)", () => {
    for (const arquivo of codigo) {
      if (arquivo === path.join("lib", "api.ts")) continue;
      assert.ok(!/\bfetch\(/.test(ler(arquivo)), `${arquivo} não pode chamar fetch diretamente`);
    }
  });

  it("nenhum arquivo aponta para os microsserviços diretamente", () => {
    for (const arquivo of codigo) {
      assert.ok(!/ms-ativos|ms-alocacoes|localhost:808[1-9]/.test(ler(arquivo)), `${arquivo} cita um microsserviço`);
    }
  });

  it("um módulo de tela não importa componentes de outro módulo", () => {
    for (const modulo of MODULOS) {
      for (const arquivo of listar(path.join("components", modulo))) {
        for (const dep of imports(arquivo)) {
          const outro = MODULOS.find((m) => m !== modulo && dep.startsWith(`@/components/${m}/`));
          assert.equal(outro, undefined, `${arquivo} não pode importar "${dep}"`);
        }
      }
    }
  });

  it("componentes não chamam a API: só as páginas em app/ usam lib/api", () => {
    for (const arquivo of listar("components")) {
      assert.ok(!imports(arquivo).includes("@/lib/api"), `${arquivo} não pode importar "@/lib/api"`);
    }
  });

  it("o Design System (components/ui) não depende de telas, layout nem da API", () => {
    for (const arquivo of listar(path.join("components", "ui"))) {
      for (const dep of imports(arquivo)) {
        const proibido = (dep.startsWith("@/components/") && !dep.startsWith("@/components/ui/")) || dep === "@/lib/api";
        assert.ok(!proibido, `${arquivo} não pode importar "${dep}"`);
      }
    }
  });

  it("lib/ não depende de componentes nem de páginas", () => {
    for (const arquivo of listar("lib")) {
      for (const dep of imports(arquivo)) {
        assert.ok(!dep.startsWith("@/components/") && !dep.startsWith("@/app/"), `${arquivo} não pode importar "${dep}"`);
      }
    }
  });
});
