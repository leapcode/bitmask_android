package pgpverify

import (
	"os"
	"strings"

	"golang.org/x/crypto/openpgp"
)

// PgpVerifier - exported struct used for file verification
type PgpVerifier struct {
	Logger Logger
}

// Logger - logging interface
type Logger interface {
	Log(msg string)
}

// Verify checks if a file was signed with the correct pgp key
// using a PEM formatted signature and a corresponding public key
func (pgpv *PgpVerifier) Verify(signature string, publicKey string, targetPath string) bool {
	keyRingReader := strings.NewReader(publicKey)
	signatureReader := strings.NewReader(signature)

	verificationTarget, err := os.Open(targetPath)
	if err != nil {
		pgpv.Logger.Log("Open verification target: " + err.Error())
		return false
	}

	keyring, err := openpgp.ReadArmoredKeyRing(keyRingReader)
	if err != nil {
		pgpv.Logger.Log("Read Armored Key Ring: " + err.Error())
		return false
	}
	_, err = openpgp.CheckArmoredDetachedSignature(keyring, verificationTarget, signatureReader)
	if err != nil {
		pgpv.Logger.Log("Verification failed: " + err.Error())
		return false
	}
	pgpv.Logger.Log("Successfully verified: entity.Identities")
	return true
}

