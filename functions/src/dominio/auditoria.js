// Regra pura (sem Azure): transforma um evento de alocação em um registro de auditoria.

const EVENTOS_CONHECIDOS = {
  AtivoAlocado: "alocado para",
  AtivoDevolvido: "devolvido por",
};

/** Lê o corpo da mensagem (objeto ou texto JSON) e valida os campos obrigatórios. */
function lerEvento(corpo) {
  const evento = typeof corpo === "string" ? JSON.parse(corpo) : corpo;
  if (!evento || typeof evento !== "object") {
    throw new Error("Mensagem sem corpo JSON.");
  }
  for (const campo of ["tipo", "numeroPatrimonio", "colaborador", "data"]) {
    if (!evento[campo]) {
      throw new Error(`Evento sem o campo '${campo}'.`);
    }
  }
  return evento;
}

function registroDeAuditoria(evento, idDaMensagem) {
  const verbo = EVENTOS_CONHECIDOS[evento.tipo] ?? `(${evento.tipo})`;
  return {
    mensagem: `Auditoria: ativo ${evento.numeroPatrimonio} (${evento.equipamento ?? "equipamento"}) ${verbo} ${evento.colaborador} em ${evento.data}.`,
    tipo: evento.tipo,
    numeroPatrimonio: evento.numeroPatrimonio,
    colaborador: evento.colaborador,
    alocacaoId: evento.alocacaoId ?? null,
    ocorridoEm: evento.ocorridoEm ?? null,
    idDaMensagem: idDaMensagem ?? null,
  };
}

module.exports = { lerEvento, registroDeAuditoria };
