# Habimetria

**Cada detalhe, documentado.** Vistorias e documentação de imóveis com captura fotográfica, lacre e relatórios verificáveis.

![Identidade visual Habimetria e exemplo conceitual da interface](assets/identidade-habimetria.png)

*A imagem apresenta a identidade visual e uma proposta de interface; não representa uma captura atual do aplicativo.*

## Recursos

- **Vistorias no celular:** cadastre imóveis, nomeie cômodos, fotografe categorias e acompanhe a cobertura de cada ambiente.
- **Registro de detalhes:** adicione problemas por opções predefinidas e observações; revise as fotos antes de lacrar.
- **Lacre e integridade:** a vistoria lacrada é imutável. O aplicativo calcula hashes e gera um manifesto para verificação; novos registros seguem como ocorrências.
- **Relatórios e compartilhamento:** gere PDF com fotos e QR, compartilhe pelo sistema e exporte ou importe pacotes verificáveis em ZIP.
- **Trabalho local:** imóveis, capturas, lacre, PDF, comparação e verificação no dispositivo funcionam sem conta ou assinatura. Os horários locais são declarações do dispositivo, não horários certificados pelo servidor.
- **Painel web e API:** área autenticada para administração e acesso autorizado a imóveis, vistorias e relatórios armazenados no servidor.
- **Conta e segurança:** cadastro e verificação de e-mail, recuperação de senha e sessões de dispositivos com rotação e revogação estão implementados na base de serviços. As telas de conta no app ainda estão planejadas.
- **Serviços futuros:** planos mensais para recursos hospedados, compras pelas lojas, backup e sincronização estão em desenvolvimento. Não há preço ou assinatura disponível para contratação neste repositório.

## Acesso e downloads

| Plataforma | Situação e acesso |
| --- | --- |
| Android | Código e build de desenvolvimento disponíveis. [Instruções de compilação](app/README.md#local-verification-linux). Não há release publicado neste repositório. |
| iOS | Código compartilhado e fontes do aplicativo disponíveis; o projeto Xcode e a validação em iPhone ainda são necessários. [Orientações para iOS](app/iosApp/README.md). |
| Web | O painel é executado junto ao servidor; consulte a [configuração e as permissões](docs/04-backend/web_panel.md). Nenhum endereço público de produção está informado. |
| Código-fonte | [Baixar código](https://github.com/AlexandreZanata/Habimetria/archive/refs/heads/main.zip) ou consultar [releases](https://github.com/AlexandreZanata/Habimetria/releases). O desenvolvimento atual está na branch `dev`. |
| Google Play e App Store | Publicação ainda não confirmada; links de instalação serão adicionados quando estiverem disponíveis. |

## Interface e identidade

![Monograma e nome Habimetria](docs/05-mobile/assets/habimetria-logo.svg)

A interface usa o monograma H, verde pinho, marfim e tipografia Manrope. O fluxo prioriza fotos, informações claras e uso em dispositivos móveis. A imagem acima é um recurso de marca; capturas reais de telas poderão ser adicionadas após uma revisão que evite expor dados de imóveis ou usuários.

## Executar o projeto

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up --build
# API: http://localhost:8080/healthz
```

Para compilar e testar o aplicativo, consulte o [guia do app](app/README.md). O painel web requer configuração de autenticação e permissões: veja o [guia do painel](docs/04-backend/web_panel.md). As configurações de desenvolvimento não representam uma implantação de produção.

## Arquitetura e documentação

- `app/` — aplicativo Kotlin Multiplatform e interface Compose.
- `backend/` — API, painel web, serviços Go e migrações.
- `infra/` — infraestrutura de desenvolvimento e testes.
- `docs/` — contratos de produto, arquitetura e planejamento em inglês.

Consulte o [índice de documentação](docs/README.md), o [plano comercial](docs/06-roadmap/commercial/README.md) e o [progresso das etapas](docs/06-roadmap/commercial/progress.md). A direção comercial mantém as funções locais gratuitas e prevê mensalidades para serviços hospedados; o plano ainda está em execução.

Alterações seguem de `dev` para `main` por pull request, com os checks `backend` e `mobile-linux` exigidos pela [proteção da branch](https://github.com/AlexandreZanata/Habimetria/rules/26970165).

## Licença

Distribuído sob a [Habimetria Visible Attribution License 1.0](LICENSE). O uso, inclusive comercial, requer a atribuição visível ao sistema e ao criador. Componentes de terceiros mantêm suas próprias licenças e avisos.
