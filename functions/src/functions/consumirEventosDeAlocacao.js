const { app } = require("@azure/functions");
const { lerEvento, registroDeAuditoria } = require("../dominio/auditoria");

// Gatilho de fila do Azure Service Bus: consome os eventos AtivoAlocado e AtivoDevolvido
// publicados pelo BFF e registra a trilha de auditoria (log da Function).
// "connection" é o NOME (prefixo) da configuração da conexão, não a conexão em si: no Azure,
// ServiceBusConnection__fullyQualifiedNamespace (identidade gerenciada); local, ServiceBusConnection.
app.serviceBusQueue("consumirEventosDeAlocacao", {
  connection: "ServiceBusConnection",
  queueName: "%SERVICEBUS_FILA%",
  handler: (mensagem, context) => {
    const evento = lerEvento(mensagem);
    const registro = registroDeAuditoria(evento, context.triggerMetadata?.messageId);
    context.log(registro.mensagem);
    context.log(JSON.stringify(registro));
  },
});
