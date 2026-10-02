package vault.ui

// Curated validation messages only; never display provider exceptions or IDs.
internal fun reportInputError(message: String?): String? = when (message) {
    "invalid CPF check digits" -> "Confira os dígitos do CPF informado ou deixe o campo vazio."
    "invalid email" -> "Confira o e-mail informado ou deixe o campo vazio."
    "phone too long" -> "Use até 40 caracteres no telefone."
    "invalid inspection date: use YYYY-MM-DD", "invalid consultation date" -> "Informe uma data válida no formato AAAA-MM-DD."
    "complete professional date, summary and references" -> "Preencha data da vistoria, resumo e referências no relatório profissional."
    "professional applicability acknowledgement required" -> "Confirme as atribuições, o registro da ART/RRT e a revisão das exigências aplicáveis."
    "provide consultation source and date together" -> "Informe fonte e data da consulta juntas, ou deixe ambas vazias."
    "complete professional report details" -> "Complete nome, título, registro, finalidade, escopo, métodos, limitações, análise, conclusões e referência da ART/RRT."
    "ART/RRT document required" -> "Anexe o PDF da ART/RRT correspondente ao serviço."
    "council and ART/RRT mismatch" -> "Selecione CREA com ART ou CAU com RRT."
    "invalid council state" -> "Informe a sigla válida da UF do conselho."
    "author acknowledgement required" -> "Confirme a autoria e a revisão dos dados antes do lacre."
    "describe the disagreement" -> "Descreva o ponto de discordância antes de registrar."
    "explicit declaration intent required" -> "Confirme sua intenção antes de registrar a declaração."
    "generate a new report version for declarations" -> "Gere uma nova versão do PDF para registrar declarações; a versão anterior será preservada."
    "report key unavailable" -> "A chave original deste relatório não está disponível para uma nova assinatura."
    "declaration limit reached" -> "Este relatório já contém o limite de 32 declarações."
    "declaration payload too large" -> "Reduza a observação para registrar a declaração."
    "invalid A1 file size", "file too large" -> "O arquivo excede o tamanho permitido para esta importação."
    "A1 file or password invalid" -> "Não foi possível abrir o certificado A1. Confira o arquivo e a senha."
    "select a PKCS12 with exactly one private key" -> "Selecione um certificado A1 que contenha somente uma chave privada."
    "RSA key too weak or too large", "only P-256 supported", "unsupported certificate key" -> "O certificado usa uma chave não suportada. Use RSA de 2048 a 8192 bits ou P-256."
    "certificate not usable for signing", "end-entity certificate required" -> "Este certificado não permite a assinatura do titular."
    "certificate outside device validity period" -> "O certificado está fora do período de validade segundo o relógio deste dispositivo."
    else -> null
}
