"use client";

import { useEffect, useMemo, useState } from "react";
import { Wrench, Plus } from "lucide-react";
import { getManutencoes } from "@/lib/api";
import type { Manutencao } from "@/lib/types";
import { formatCurrency } from "@/lib/format";
import PageHeader from "@/components/common/PageHeader";
import StatCard from "@/components/common/StatCard";
import { Loading, ErrorMsg } from "@/components/common/DataState";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import ManutencoesTable from "@/components/manutencoes/ManutencoesTable";

const DIAS_RECENTE = 45; // aproximação apenas para o indicador mock

export default function ManutencoesPage() {
  const [manutencoes, setManutencoes] = useState<Manutencao[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busca, setBusca] = useState("");

  useEffect(() => {
    getManutencoes()
      .then(setManutencoes)
      .catch((err: Error) => setError(err.message));
  }, []);

  const filtradas = useMemo(() => {
    if (!manutencoes) return [];
    const termo = busca.trim().toLowerCase();
    if (!termo) return manutencoes;
    return manutencoes.filter(
      (m) =>
        m.numeroPatrimonio.toLowerCase().includes(termo) ||
        m.equipamento.toLowerCase().includes(termo)
    );
  }, [manutencoes, busca]);

  if (error) return <ErrorMsg message={error} />;
  if (!manutencoes) return <Loading />;

  const custoTotal = manutencoes.reduce((t, m) => t + m.custo, 0);
  const custoMedio = manutencoes.length ? custoTotal / manutencoes.length : 0;
  const emManutencaoAgora = new Set(
    manutencoes
      .filter((m) => {
        const dias = (Date.now() - new Date(m.data).getTime()) / 86_400_000;
        return dias < DIAS_RECENTE;
      })
      .map((m) => m.numeroPatrimonio)
  ).size;

  return (
    <div>
      <PageHeader
        icon={Wrench}
        title="Manutenções"
        subtitle="Serviços realizados nos equipamentos e o custo de cada um."
        actions={<Button icon={<Plus size={15} />}>Registrar Manutenção</Button>}
      />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatCard
          label="Ativos com manutenção recente"
          value={emManutencaoAgora}
          hint={`Últimos ${DIAS_RECENTE} dias`}
          valueClassName="text-[var(--warning)]"
        />
        <StatCard label="Custo total registrado" value={formatCurrency(custoTotal)} />
        <StatCard label="Custo médio por serviço" value={formatCurrency(custoMedio)} />
      </div>

      <Card padded={false}>
        <div className="flex items-center justify-between gap-3 p-4 flex-wrap border-b border-[var(--border)]">
          <Input
            type="text"
            placeholder="Pesquisar por patrimônio ou equipamento…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="flex-1 min-w-[240px]"
          />
          <span className="text-sm text-[var(--text-muted)]">
            {filtradas.length} registros
          </span>
        </div>
        <ManutencoesTable itens={filtradas} />
      </Card>
    </div>
  );
}
