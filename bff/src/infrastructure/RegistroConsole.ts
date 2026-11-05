import type { Registro } from "../domain/portas.js";

export class RegistroConsole implements Registro {
  info(mensagem: string, extra?: unknown): void {
    console.log(`[bff] ${mensagem}`, extra ?? "");
  }

  aviso(mensagem: string, extra?: unknown): void {
    console.warn(`[bff] ${mensagem}`, extra ?? "");
  }
}
