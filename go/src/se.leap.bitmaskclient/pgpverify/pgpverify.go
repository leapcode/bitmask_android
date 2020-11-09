package pgpverify

import (
	"os"
	"strings"

	"golang.org/x/crypto/openpgp"
)

// PgpVerifier - exported struct used for file verification
type PgpVerifier struct {
	//Signature string
	//Target    string
	//PublicKey string
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

/*func main() {
	keyRingReader, err := os.Open("public_leap.asc")
	if err != nil {
		fmt.Println(err)
		return
	}

	signature, err := os.Open("RiseupVPN_release_1.0.5.apk.sig")
	if err != nil {
		fmt.Println(err)
		return
	}

	verificationTarget, err := os.Open("RiseupVPN_release_1.0.5.apk")
	if err != nil {
		fmt.Println(err)
		return
	}

	keyring, err := openpgp.ReadArmoredKeyRing(keyRingReader)
	if err != nil {
		fmt.Println("Read Armored Key Ring: " + err.Error())
		return
	}
	entity, err := openpgp.CheckArmoredDetachedSignature(keyring, verificationTarget, signature)
	if err != nil {
		fmt.Println("Check Detached Signature: " + err.Error())
		return
	} else {
		fmt.Println("successfully verified")
	}

	fmt.Println(entity.Identities)
}*/
