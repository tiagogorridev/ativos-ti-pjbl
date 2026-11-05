// Regra de negócio pura (sem Azure): depreciação linear dos equipamentos de informática.
// Vida útil de 5 anos (taxa de 20% ao ano, a usada para equipamentos de informática) e valor
// residual zero. Ativo baixado já não tem valor contábil.

const VIDA_UTIL_ANOS = 5;

function arredondar(valor) {
  return Math.round(valor * 100) / 100;
}

/** Meses completos entre a data de aquisição e a data de referência. */
function mesesDecorridos(dataAquisicao, referencia) {
  const inicio = new Date(`${dataAquisicao}T00:00:00Z`);
  if (Number.isNaN(inicio.getTime())) {
    throw new Error(`Data de aquisição inválida: '${dataAquisicao}'. Use o formato AAAA-MM-DD.`);
  }
  let meses = (referencia.getUTCFullYear() - inicio.getUTCFullYear()) * 12 + (referencia.getUTCMonth() - inicio.getUTCMonth());
  if (referencia.getUTCDate() < inicio.getUTCDate()) meses -= 1;
  return Math.max(0, meses);
}

function depreciarAtivo(ativo, referencia) {
  const valorAquisicao = Number(ativo.valorAquisicao);
  if (!Number.isFinite(valorAquisicao) || valorAquisicao < 0) {
    throw new Error(`Valor de aquisição inválido para o ativo ${ativo.numeroPatrimonio}.`);
  }
  const vidaUtilMeses = VIDA_UTIL_ANOS * 12;
  const fracao = ativo.status === "Baixado" ? 1 : Math.min(mesesDecorridos(ativo.dataAquisicao, referencia) / vidaUtilMeses, 1);
  const depreciacaoAcumulada = arredondar(valorAquisicao * fracao);
  return {
    numeroPatrimonio: ativo.numeroPatrimonio,
    valorAquisicao: arredondar(valorAquisicao),
    valorAtual: arredondar(valorAquisicao - depreciacaoAcumulada),
    depreciacaoAcumulada,
    percentualDepreciado: arredondar(fracao * 100),
    vidaUtilAnos: VIDA_UTIL_ANOS,
  };
}

/** Calcula a depreciação de cada ativo e os totais do parque. */
function calcularDepreciacao(ativos, referencia = new Date()) {
  if (!Array.isArray(ativos)) {
    throw new Error("Envie { \"ativos\": [ ... ] } com a lista de ativos.");
  }
  const itens = ativos.map((ativo) => depreciarAtivo(ativo, referencia));
  const soma = (campo) => arredondar(itens.reduce((total, item) => total + item[campo], 0));
  return {
    calculadoEm: referencia.toISOString(),
    valorAquisicaoTotal: soma("valorAquisicao"),
    valorAtualTotal: soma("valorAtual"),
    depreciacaoTotal: soma("depreciacaoAcumulada"),
    itens,
  };
}

module.exports = { calcularDepreciacao, mesesDecorridos, VIDA_UTIL_ANOS };
