# Frontend (Microfrontend) — Next.js

Parte do **Sistema de Controle de Ativos de Informática** (PJBL — PUCPR). Interface web do sistema
(SPA em React com Next.js), publicada no **Azure Static Web Apps**. O frontend fala **só com o BFF**,
sempre passando pelo **API Gateway**; ele nunca chama os microsserviços diretamente.

- **Site publicado:** https://kind-bush-0c7a5e010.5.azurestaticapps.net
- **Swagger (BFF e microsserviços):** https://kind-bush-0c7a5e010.5.azurestaticapps.net/swagger/

## Alunos

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Arquitetura

```
Navegador → Frontend (Static Web Apps) → API Gateway → BFF → Microsserviços / Azure Function
```

O código é organizado por módulo funcional (base para a divisão em microfrontends):

| Pasta | Conteúdo |
|---|---|
| `app/` | Uma rota por tela: dashboard (`/`), `/ativos`, `/colaboradores`, `/alocacoes`, `/manutencoes`, `/relatorios`, `/usuarios` |
| `components/<módulo>/` | Componentes de cada tela |
| `components/ui` e `components/common` | Componentes compartilhados (design system) |
| `components/layout` e `lib/AuthContext.tsx` | Shell da aplicação: menu, cabeçalho e sessão |
| `lib/api.ts` | Cliente HTTP: todas as chamadas vão ao API Gateway com o cabeçalho `X-API-Key` |
| `public/swagger/` | Página com o Swagger do BFF e dos microsserviços |

## Tecnologias

Next.js 16 (export estático), React 19, TypeScript, Tailwind CSS, lucide-react, Azure Static Web Apps.

## Como rodar localmente

```bash
npm install
npm run dev            # http://localhost:3000, usa NEXT_PUBLIC_API_BASE_URL (padrão /api)
npm run dev:backend    # encaminha /api/* ao API Gateway local (http://localhost:8080)
```

Build de produção (gera a pasta `out/`, publicada no Static Web Apps):

```bash
NEXT_PUBLIC_API_BASE_URL=https://<gateway>/api NEXT_PUBLIC_API_KEY=<chave do gateway> npm run build
```

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Endereço do API Gateway com `/api` (padrão: `/api`) |
| `NEXT_PUBLIC_API_KEY` | Chave enviada no cabeçalho `X-API-Key` (fica no JavaScript do site; identifica o cliente, não é senha) |
| `GATEWAY_URL` | Só em desenvolvimento: liga o proxy do Next.js para o gateway |
