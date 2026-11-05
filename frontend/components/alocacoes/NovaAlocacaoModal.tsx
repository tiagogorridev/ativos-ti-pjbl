"use client";

import { useState, FormEvent } from "react";
import { X, Save } from "lucide-react";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import Select from "@/components/ui/Select";
import type { Colaborador } from "@/lib/types";

/** Formulário de nova alocação: patrimônio do ativo e colaborador ativo que vai recebê-lo. */
export default function NovaAlocacaoModal({
  colaboradores,
  onClose,
  onSubmit,
}: {
  colaboradores: Colaborador[];
  onClose: () => void;
  onSubmit: (numeroPatrimonio: string, colaboradorId: number) => Promise<void>;
}) {
  const ativos = colaboradores.filter((c) => c.situacao === "Ativo");
  const [patrimonio, setPatrimonio] = useState("");
  const [colaboradorId, setColaboradorId] = useState<string>(ativos[0] ? String(ativos[0].id) : "");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (!patrimonio.trim() || !colaboradorId) {
      setErro("Informe o número de patrimônio e o colaborador.");
      return;
    }
    setSalvando(true);
    try {
      await onSubmit(patrimonio.trim(), Number(colaboradorId));
    } catch (err) {
      // Ex.: 409 quando o ativo já está alocado ou não está "Em estoque".
      setErro(err instanceof Error ? err.message : "Não foi possível registrar a alocação.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center px-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
        <div className="flex items-start justify-between mb-5">
          <h2 className="text-base font-semibold text-[var(--navy)]">Nova alocação</h2>
          <button onClick={onClose} className="text-[var(--text-muted)] hover:text-[var(--text)]" aria-label="Fechar">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="grid gap-3">
          <label className="text-xs text-[var(--text-muted)]">
            Número de patrimônio
            <Input
              className="w-full mt-1"
              value={patrimonio}
              onChange={(e) => setPatrimonio(e.target.value)}
              placeholder="PAT-0003"
              required
            />
          </label>

          <label className="text-xs text-[var(--text-muted)]">
            Colaborador
            <Select className="w-full mt-1" value={colaboradorId} onChange={(e) => setColaboradorId(e.target.value)}>
              {ativos.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome} — {c.setor}
                </option>
              ))}
            </Select>
          </label>

          {erro && (
            <p className="text-xs text-[var(--danger)] bg-red-50 border border-red-100 rounded-lg px-3 py-2">{erro}</p>
          )}

          <div className="flex justify-end gap-2 mt-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" icon={<Save size={15} />} disabled={salvando}>
              {salvando ? "Salvando…" : "Alocar"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
