"use client";

import { useEffect, useState } from "react";
import { LayoutDashboard } from "lucide-react";
import { getDashboard } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import type { DashboardData } from "@/lib/types";
import PageHeader from "@/components/common/PageHeader";
import StatCard from "@/components/common/StatCard";
import { Loading, ErrorMsg } from "@/components/common/DataState";
import StatusBreakdown from "@/components/dashboard/StatusBreakdown";
import CustoPorMesChart from "@/components/dashboard/CustoPorMesChart";
import MovimentacoesTable from "@/components/dashboard/MovimentacoesTable";

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard()
      .then(setData)
      .catch((err: Error) => setError(err.message));
  }, []);

  if (error) return <ErrorMsg message={error} />;
  if (!data) return <Loading />;

  const statusList = [
    { label: "Em uso" as const, valor: data.emUso },
    { label: "Em estoque" as const, valor: data.emEstoque },
    { label: "Em manutenção" as const, valor: data.emManutencao },
    { label: "Baixado" as const, valor: data.baixados },
  ];

  return (
    <div>
      <PageHeader
        icon={LayoutDashboard}
        title="Dashboard"
        subtitle="Indicadores consolidados do parque de TI."
      />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Total de ativos" value={data.totalAtivos} hint="Cadastrados no MongoDB Atlas" />
        <StatCard
          label="Em uso"
          value={data.emUso}
          hint={`${Math.round((data.emUso / data.totalAtivos) * 100)}% do parque`}
          valueClassName="text-[var(--success)]"
        />
        <StatCard
          label="Em manutenção"
          value={data.emManutencao}
          hint="Ver detalhes em Manutenções"
          valueClassName="text-[var(--warning)]"
        />
        <StatCard
          label="Custo de manutenção (ano)"
          value={formatCurrency(data.custoManutencaoAcumulado)}
          hint="Soma das manutenções registradas"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-6">
        <StatusBreakdown items={statusList} />
        <CustoPorMesChart dados={data.custoManutencaoPorMes ?? []} />
      </div>

      <MovimentacoesTable itens={data.ultimasMovimentacoes ?? []} />
    </div>
  );
}
