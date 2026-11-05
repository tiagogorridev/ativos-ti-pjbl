"use client";

import { useCallback, useEffect, useState } from "react";
import { ArrowUpFromLine, Plus } from "lucide-react";
import { alocarAtivo, devolverAlocacao, getAlocacoes, getColaboradores } from "@/lib/api";
import type { Alocacao, Colaborador } from "@/lib/types";
import PageHeader from "@/components/common/PageHeader";
import InfoBanner from "@/components/common/InfoBanner";
import { Loading, ErrorMsg } from "@/components/common/DataState";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import AlocacoesTable from "@/components/alocacoes/AlocacoesTable";
import NovaAlocacaoModal from "@/components/alocacoes/NovaAlocacaoModal";

export default function AlocacoesPage() {
  const [alocacoes, setAlocacoes] = useState<Alocacao[] | null>(null);
  const [colaboradores, setColaboradores] = useState<Colaborador[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [modalAberto, setModalAberto] = useState(false);

  const carregar = useCallback(() => {
    getAlocacoes()
      .then(setAlocacoes)
      .catch((err: Error) => setError(err.message));
  }, []);

  useEffect(() => {
    carregar();
    getColaboradores()
      .then(setColaboradores)
      .catch(() => setColaboradores([]));
  }, [carregar]);

  if (error) return <ErrorMsg message={error} />;
  if (!alocacoes) return <Loading />;

  async function devolver(id: number) {
    setAviso(null);
    try {
      // O BFF encerra a alocação (Azure SQL), devolve o ativo ao estoque (MongoDB)
      // e publica o evento AtivoDevolvido no Service Bus.
      await devolverAlocacao(id);
      carregar();
    } catch (err) {
      setAviso(err instanceof Error ? err.message : "Não foi possível registrar a devolução.");
    }
  }

  async function alocar(numeroPatrimonio: string, colaboradorId: number) {
    await alocarAtivo(numeroPatrimonio, colaboradorId);
    setModalAberto(false);
    carregar();
  }

  return (
    <div>
      <PageHeader
        icon={ArrowUpFromLine}
        title="Alocações"
        subtitle="Equipamentos atualmente em posse de colaboradores."
        actions={
          <Button icon={<Plus size={15} />} onClick={() => setModalAberto(true)}>
            Nova Alocação
          </Button>
        }
      />

      <InfoBanner>
        Um ativo só pode estar alocado a um colaborador por vez. Ao registrar
        a devolução, o equipamento volta ao estoque e a movimentação fica no
        histórico.
      </InfoBanner>

      {aviso && (
        <p className="mb-4 text-sm text-[var(--danger)] bg-red-50 border border-red-100 rounded-lg px-3 py-2">{aviso}</p>
      )}

      <Card padded={false}>
        <div className="px-4 py-3 border-b border-[var(--border)] text-sm text-[var(--text-muted)]">
          {alocacoes.length} alocações ativas
        </div>
        <AlocacoesTable itens={alocacoes} onDevolver={devolver} />
      </Card>

      {modalAberto && (
        <NovaAlocacaoModal
          colaboradores={colaboradores}
          onClose={() => setModalAberto(false)}
          onSubmit={alocar}
        />
      )}
    </div>
  );
}
