import { ServiceBusClient, type ServiceBusSender } from "@azure/service-bus";
import type { PublicadorDeEventos, Registro } from "../../domain/portas.js";
import type { EventoDeAlocacao } from "../../domain/tipos.js";

/** Publica os eventos na fila do Azure Service Bus (consumida por uma Azure Function). */
export class PublicadorServiceBus implements PublicadorDeEventos {
  private readonly cliente: ServiceBusClient;
  private readonly remetente: ServiceBusSender;

  constructor(cadeiaDeConexao: string, fila: string) {
    this.cliente = new ServiceBusClient(cadeiaDeConexao);
    this.remetente = this.cliente.createSender(fila);
  }

  async publicar(evento: EventoDeAlocacao): Promise<void> {
    await this.remetente.sendMessages({
      body: evento,
      contentType: "application/json",
      subject: evento.tipo,
      messageId: `${evento.tipo}-${evento.alocacaoId}`,
    });
  }

  async fechar(): Promise<void> {
    await this.remetente.close();
    await this.cliente.close();
  }
}

/** Usado quando não há Service Bus configurado (desenvolvimento local): só registra o evento. */
export class PublicadorEmLog implements PublicadorDeEventos {
  constructor(private readonly registro: Registro) {}

  async publicar(evento: EventoDeAlocacao): Promise<void> {
    this.registro.info(`Evento ${evento.tipo} (Service Bus não configurado)`, evento);
  }
}
