import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

/**
 * Testes de arquitetura: leem os imports de cada arquivo em src/ e falham se o código
 * quebrar a regra de dependência da Clean Architecture ou a independência das fatias verticais.
 */
const raiz = resolve(dirname(fileURLToPath(import.meta.url)), "../../src");

type Camada = "domain" | "application" | "infrastructure" | "api" | "main";

interface Arquivo {
  caminho: string;
  camada: Camada;
  fatia?: string;
  importsInternos: { camada: Camada; fatia?: string; alvo: string }[];
  importsExternos: string[];
}

function listar(pasta: string): string[] {
  return readdirSync(pasta).flatMap((nome) => {
    const completo = join(pasta, nome);
    return statSync(completo).isDirectory() ? listar(completo) : completo.endsWith(".ts") ? [completo] : [];
  });
}

function classificar(caminhoAbsoluto: string): { camada: Camada; fatia?: string } {
  const partes = relative(raiz, caminhoAbsoluto).split(sep);
  if (partes.length === 1) return { camada: "main" };
  const camada = partes[0] as Camada;
  return { camada, fatia: camada === "application" ? partes[1] : undefined };
}

const arquivos: Arquivo[] = listar(raiz).map((caminho) => {
  const codigo = readFileSync(caminho, "utf8");
  const imports = [...codigo.matchAll(/from\s+["']([^"']+)["']/g)].map((m) => m[1] ?? "");
  const internos = imports.filter((i) => i.startsWith("."));
  return {
    caminho: relative(raiz, caminho),
    ...classificar(caminho),
    importsInternos: internos.map((i) => {
      const alvo = resolve(dirname(caminho), i.replace(/\.js$/, ".ts"));
      return { ...classificar(alvo), alvo: relative(raiz, alvo) };
    }),
    importsExternos: imports.filter((i) => !i.startsWith(".")),
  };
});

const PERMITIDAS: Record<Camada, Camada[]> = {
  domain: ["domain"],
  application: ["application", "domain"],
  infrastructure: ["infrastructure", "application", "domain"],
  api: ["api", "application", "domain"],
  main: ["main", "api", "application", "infrastructure", "domain"],
};

function violacoes(regra: (a: Arquivo) => string[]): string[] {
  return arquivos.flatMap(regra);
}

describe("Arquitetura do BFF", () => {
  it("encontra as quatro camadas", () => {
    const camadas = new Set(arquivos.map((a) => a.camada));
    expect([...camadas]).toEqual(expect.arrayContaining(["domain", "application", "infrastructure", "api"]));
  });

  it("dependências só apontam para dentro (Clean Architecture)", () => {
    const erros = violacoes((a) =>
      a.importsInternos
        .filter((i) => !PERMITIDAS[a.camada].includes(i.camada))
        .map((i) => `${a.caminho} (${a.camada}) importa ${i.alvo} (${i.camada})`),
    );
    expect(erros).toEqual([]);
  });

  it("domain e application não usam bibliotecas externas (nem Express, nem Azure)", () => {
    const erros = violacoes((a) =>
      a.camada === "domain" || a.camada === "application"
        ? a.importsExternos.filter((i) => !i.startsWith("node:")).map((i) => `${a.caminho} importa ${i}`)
        : [],
    );
    expect(erros).toEqual([]);
  });

  it("só a infrastructure conhece o Azure Service Bus", () => {
    const erros = violacoes((a) =>
      a.camada !== "infrastructure" && a.importsExternos.some((i) => i.startsWith("@azure/")) ? [a.caminho] : [],
    );
    expect(erros).toEqual([]);
  });

  it("fatias verticais da application não dependem umas das outras", () => {
    const erros = violacoes((a) =>
      a.camada === "application"
        ? a.importsInternos
            .filter((i) => i.camada === "application" && i.fatia !== a.fatia)
            .map((i) => `${a.caminho} (fatia ${a.fatia}) importa ${i.alvo} (fatia ${i.fatia})`)
        : [],
    );
    expect(erros).toEqual([]);
  });
});
