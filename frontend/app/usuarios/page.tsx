"use client";

import { ShieldCheck } from "lucide-react";
import PageHeader from "@/components/common/PageHeader";
import InfoBanner from "@/components/common/InfoBanner";
import PermissoesMatrix from "@/components/usuarios/PermissoesMatrix";

export default function UsuariosPage() {
  return (
    <div>
      <PageHeader
        icon={ShieldCheck}
        title="Usuários e Permissões"
        subtitle="O que cada perfil pode fazer no sistema."
      />

      <InfoBanner>
        As contas vêm do diretório corporativo (Microsoft Entra ID), e o
        perfil de cada pessoa chega no token de login. Por isso o sistema não
        guarda cadastro nem senha de usuários: aqui fica apenas a matriz de
        permissões por perfil.
      </InfoBanner>

      <PermissoesMatrix />
    </div>
  );
}
