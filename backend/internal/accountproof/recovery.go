package accountproof

func (k *Key) IssueRecovery(user, operation string) (digest, ciphertext []byte) {
	return k.issue("password_recovery", user, operation)
}

func (k *Key) OpenRecovery(user, operation string, envelope []byte) (string, error) {
	return k.open("password_recovery", user, operation, envelope)
}
